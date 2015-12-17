/*
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.1.
 */

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

/**
 * QryEval is a simple application that reads queries from a file,
 * evaluates them against an index, and writes the results to an
 * output file.  This class contains the main method, a method for
 * reading parameter and query files, initialization methods, a simple
 * query parser, a simple query processor, and methods for reporting
 * results.
 * <p>
 * This software illustrates the architecture for the portion of a
 * search engine that evaluates queries.  It is a guide for class
 * homework assignments, so it emphasizes simplicity over efficiency.
 * Everything could be done more efficiently and elegantly.
 * <p>
 * The {@link Qry} hierarchy implements query evaluation using a
 * 'document at a time' (DaaT) methodology.  Initially it contains an
 * #OR operator for the unranked Boolean retrieval model and a #SYN
 * (synonym) operator for any retrieval model.  It is easily extended
 * to support additional query operators and retrieval models.  See
 * the {@link Qry} class for details.
 * <p>
 * The {@link RetrievalModel} hierarchy stores parameters and
 * information required by different retrieval models.  Retrieval
 * models that need these parameters (e.g., BM25 and Indri) use them
 * very frequently, so the RetrievalModel class emphasizes fast access.
 * <p>
 * The {@link Idx} hierarchy provides access to information in the
 * Lucene index.  It is intended to be simpler than accessing the
 * Lucene index directly.
 * <p>
 * As the search engine becomes more complex, it becomes useful to
 * have a standard approach to representing documents and scores.
 * The {@link ScoreList} class provides this capability.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";

  private static final EnglishAnalyzerConfigurable ANALYZER =
    new EnglishAnalyzerConfigurable(Version.LUCENE_43);
  private static final String[] TEXT_FIELDS =
    { "body", "title", "url", "inlink" };


  //  --------------- Methods ---------------------------------------

  /**
   * @param args The only argument is the parameter file name.
   * @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.
    
    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Configure query lexical processing to match index lexical
    //  processing.  Initialize the index and retrieval model.

    ANALYZER.setLowercase(true);
    ANALYZER.setStopwordRemoval(true);
    ANALYZER.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);

    Idx.initialize (parameters.get ("indexPath"));
    RetrievalModel model = initializeRetrievalModel (parameters);
    String fb = parameters.get("fb");
    String fbInitialRankingFile = parameters.get("fbInitialRankingFile");
    String queryFilePath = parameters.get("queryFilePath");
    String trecEvalOutputPath = parameters.get("trecEvalOutputPath");

    if (model instanceof RetrievalModelLetor) {
      String trainingQueryFilePath = parameters.get("letor:trainingQueryFile");
      String qrelsFilePath = parameters.get("letor:trainingQrelsFile");
      String pageRankFilePath = parameters.get("letor:pageRankFile");
      String trainingFeatureVectorPath =
          parameters.get("letor:trainingFeatureVectorsFile");
      Set<Integer> disableFeatures;
      if (parameters.containsKey("letor:featureDisable")) {
        String disableFeaturesStr = parameters.get("letor:featureDisable");
        String[] tokens = disableFeaturesStr.split(",");
        disableFeatures = new HashSet<>(
            Arrays.asList(tokens).stream()
                .map(Integer::valueOf)
                .collect(Collectors.toList()));
      } else {
        disableFeatures = new HashSet<>();
      }

      extractFeaturesFromResults(trainingQueryFilePath, qrelsFilePath,
          pageRankFilePath, trainingFeatureVectorPath, disableFeatures, model,
          "training");

      String featGenC = parameters.get("letor:svmRankParamC");
      String learnExecPath = parameters.get("letor:svmRankLearnPath");
      String modelOutputFilePath = parameters.get("letor:svmRankModelFile");
      SvmRankExecutor.learn(learnExecPath, featGenC, trainingFeatureVectorPath,
          modelOutputFilePath);

      String testingFeatureVectorPath =
          parameters.get("letor:testingFeatureVectorsFile");

      processQueryFile(queryFilePath, trecEvalOutputPath,
          ((RetrievalModelLetor) model).rBM25);
      extractFeaturesFromResults(queryFilePath, trecEvalOutputPath,
          pageRankFilePath, testingFeatureVectorPath, disableFeatures, model,
          "testing");

      String classifyExecPath = parameters.get("letor:svmRankClassifyPath");
      String testingDocumentScores =
          parameters.get("letor:testingDocumentScores");
      SvmRankExecutor.classify(classifyExecPath, testingFeatureVectorPath,
          modelOutputFilePath, testingDocumentScores);

      printLearnResult(testingFeatureVectorPath, testingDocumentScores,
          trecEvalOutputPath);
    } else {
      if (fb == null) {
        //  Perform experiments.
        processQueryFile(queryFilePath, trecEvalOutputPath, model);
      } else {
        // Expand query

        // Get original query result
        if (!fb.equals("true") || fbInitialRankingFile == null) {
          processQueryFile(queryFilePath, trecEvalOutputPath, model);
          fbInitialRankingFile = trecEvalOutputPath;
        }
        String fbExpansionQueryFile = parameters.get("fbExpansionQueryFile");

        Integer fbDocs = Integer.valueOf(parameters.get("fbDocs"));
        Integer fbTerms = Integer.valueOf(parameters.get("fbTerms"));
        Double fbMu = Double.valueOf(parameters.get("fbMu"));
        Double fbOrigWeight = Double.valueOf(parameters.get("fbOrigWeight"));
        FbModel fbModel = new FbModel(fbDocs, fbTerms, fbMu, fbOrigWeight);

        processQueryExpansion(queryFilePath, trecEvalOutputPath, model,
            fbInitialRankingFile, fbExpansionQueryFile, fbModel);
      }
    }

    //  Clean up.
    
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   * Allocate the retrieval model and initialize it using parameters
   * from the parameter file.
   * @return The initialized retrieval model
   * @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    switch (modelString) {
      case "unrankedboolean":
        model = new RetrievalModelUnrankedBoolean();
        break;
      case "rankedboolean":
        model = new RetrievalModelRankedBoolean();
        break;
      case "bm25": {
        if (!(parameters.containsKey("BM25:k_1") &&
            parameters.containsKey("BM25:b") &&
            parameters.containsKey("BM25:k_3"))) {
          throw new IllegalArgumentException
              ("Required parameters of BM25 model were missing from the parameter file.");
        }
        double k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
        double b = Double.parseDouble(parameters.get("BM25:b"));
        double k_3 = Double.parseDouble(parameters.get("BM25:k_3"));

        model = new RetrievalModelBM25(k_1, b, k_3);
        break;
      }
      case "indri": {
        if (!(parameters.containsKey("Indri:mu") &&
            parameters.containsKey("Indri:lambda"))) {
          throw new IllegalArgumentException
              ("Required parameters of Indri model were missing from the parameter file.");
        }
        double mu = Double.parseDouble(parameters.get("Indri:mu"));
        double lambda = Double.parseDouble(parameters.get("Indri:lambda"));
        model = new RetrievalModelIndri(mu, lambda);
        break;
      }
      case "letor": {
        RetrievalModelLetor rLetor = new RetrievalModelLetor();
        double k_1 = parameters.containsKey("BM25:k_1") ?
            Double.parseDouble(parameters.get("BM25:k_1")) :
            1.2;
        double b = parameters.containsKey("BM25:b") ?
            Double.parseDouble(parameters.get("BM25:b")) :
            0.75;
        double k_3 = parameters.containsKey("BM25:k_3") ?
            Double.parseDouble(parameters.get("BM25:k_3")) :
            0.0;
        double mu = parameters.containsKey("Indri:mu") ?
            Double.parseDouble(parameters.get("Indri:mu")) :
            2500.0;
        double lambda = parameters.containsKey("Indri:lambda") ?
            Double.parseDouble(parameters.get("Indri:lambda")) :
            0.4;
        rLetor.rBM25 = new RetrievalModelBM25(k_1, b, k_3);
        rLetor.rIndri = new RetrievalModelIndri(mu, lambda);
        model = rLetor;
        break;
      }
      default:
        throw new IllegalArgumentException
            ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }

    return model;
  }

  /**
   * Return a query tree that corresponds to the query.
   * 
   * @param qString
   *          A string containing a query.
   * @param model
   *          Retrieval model
   * @throws IOException Error accessing the Lucene index.
   */
  static Qry parseQuery(String qString, RetrievalModel model) throws IOException {

    //  Add a default query operator to every query. This is a tiny
    //  bit of inefficiency, but it allows other code to assume
    //  that the query will return document ids and scores.

    String defaultOp = model.defaultQrySopName ();
    qString = defaultOp + "(" + qString + ")";

    //  Simple query tokenization.  Terms like "near-death" are handled later.

    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;

    //  This is a simple, stack-based parser.  These variables record
    //  the parser's state.
    
    Qry currentOp = null;
    Stack<Qry> opStack = new Stack<>();
    boolean weightExpected = false;
    Stack<Double> weightStack = new Stack<>();
    Pattern nearPat = Pattern.compile("#near/(\\d+)", Pattern.CASE_INSENSITIVE);
    Pattern windowPat = Pattern.compile("#window/(\\d+)", Pattern.CASE_INSENSITIVE);
    Matcher nearMat;
    Matcher windowMat;


    //  Each pass of the loop processes one token. The query operator
    //  on the top of the opStack is also stored in currentOp to
    //  make the code more readable.

    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches("[ ,(\t\n\r]")) {
        continue;
      } else if (token.equals(")")) {	// Finish current query op.

        // If the current query operator is not an argument to another
        // query operator (i.e., the opStack is empty when the current
        // query operator is removed), we're done (assuming correct
        // syntax - see below).

        weightExpected = false;
        opStack.pop();

        if (opStack.empty())
          break;

        // Not done yet.  Add the current operator as an argument to
        // the higher-level operator, and shift processing back to the
        // higher-level operator.

        Qry arg = currentOp;
        currentOp = opStack.peek();
        currentOp.appendArg(arg);

        if (currentOp instanceof QrySopWithWeight) {
          QrySopWithWeight sopWithWeight = (QrySopWithWeight) currentOp;
          weightExpected = true;
          Double weight = weightStack.pop();
          sopWithWeight.appendWeight(weight);
        }

      } else if (token.equalsIgnoreCase("#or")) {
        currentOp = new QrySopOr();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
      } else if (token.equalsIgnoreCase("#and")) {
        currentOp = new QrySopAnd();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wand")) {
        currentOp = new QrySopWand();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
        weightExpected = true;
      } else if (token.equalsIgnoreCase("#sum")) {
        currentOp = new QrySopSum();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wsum")) {
        currentOp = new QrySopWsum();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
        weightExpected = true;
      } else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryIopSyn();
        currentOp.setDisplayName(token);
        opStack.push(currentOp);
      } else {
        nearMat = nearPat.matcher(token);
        windowMat = windowPat.matcher(token);
        if (nearMat.matches()) {
          int distance = Integer.parseInt(nearMat.group(1));
          currentOp = new QryIopNear(distance);
          currentOp.setDisplayName(token);
          opStack.push(currentOp);
        } else if (windowMat.matches()) {
          int distance = Integer.parseInt(windowMat.group(1));
          currentOp = new QryIopWindow(distance);
          currentOp.setDisplayName(token);
          opStack.push(currentOp);
        } else if (weightExpected) {
          weightStack.push(Double.parseDouble(token));
          weightExpected = false;
        } else {

          //  Split the token into a term and a field.

          int delimiter = token.indexOf('.');
          boolean currentOpWithWeight = currentOp instanceof QrySopWithWeight;
          String field;
          String term;

          if (delimiter < 0) {
            field = "body";
            term = token;
          } else {
            field = token.substring(delimiter + 1).toLowerCase();
            term = token.substring(0, delimiter);
          }

          if ((field.compareTo("url") != 0) &&
              (field.compareTo("keywords") != 0) &&
              (field.compareTo("title") != 0) &&
              (field.compareTo("body") != 0) &&
              (field.compareTo("inlink") != 0)) {
            throw new IllegalArgumentException("Error: Unknown field " + token);
          }

          //  Lexical processing, stopwords, stemming.  A loop is used
          //  just in case a term (e.g., "near-death") gets tokenized into
          //  multiple terms (e.g., "near" and "death").

          String t[] = tokenizeQuery(term);

          Double weight = 0.0;
          if (currentOpWithWeight) {
            weight = weightStack.pop();
          }

          for (String aT : t) {

            Qry termOp = new QryIopTerm(aT, field);

            currentOp.appendArg(termOp);
            if (currentOpWithWeight) {
              QrySopWithWeight sopWithWeight = (QrySopWithWeight) currentOp;
              sopWithWeight.appendWeight(weight);
            }
          }
          if (currentOpWithWeight) {
            weightExpected = true;
          }
        }
      }
    }


    //  A broken structured query can leave unprocessed tokens on the opStack,

    if (tokens.hasMoreTokens()) {
      throw new IllegalArgumentException
        ("Error:  Query syntax is incorrect.  " + qString);
    }

    return currentOp;
  }

  /**
   * Remove degenerate nodes produced during query parsing, for
   * example #NEAR/1 (of the) that can't possibly match. It would be
   * better if those nodes weren't produced at all, but that would
   * require a stronger query parser.
   */
  static boolean parseQueryCleanup(Qry q) {

    boolean queryChanged = false;

    // Iterate backwards to prevent problems when args are deleted.

    for (int i = q.args.size() - 1; i >= 0; i--) {

      Qry q_i = q.args.get(i);

      // All operators except TERM operators must have arguments.
      // These nodes could never match.
      
      if (q_i.args.size() == 0 && !(q_i instanceof QryIopTerm)) {
        q.removeArg(i);
        queryChanged = true;
      } else {

        // All operators (except SCORE operators) must have 2 or more
        // arguments. This improves efficiency and readability a bit.
        // However, be careful to stay within the same QrySop / QryIop
        // subclass, otherwise the change might cause a syntax error.


        if (q_i.args.size() == 1 && !(q_i instanceof QrySopScore)) {

          Qry q_i_0 = q_i.args.get(0);

          if (((q_i instanceof QrySop) && (q_i_0 instanceof QrySop)) ||
              ((q_i instanceof QryIop) && (q_i_0 instanceof QryIop))) {
            q.args.set(i, q_i_0);
            queryChanged = true;
          }
        } else {

          // Check the subtree.

          if (parseQueryCleanup(q_i))
            queryChanged = true;
        }
      }
    }

    return queryChanged;
  }

  /**
   * Print a message indicating the amount of memory used. The caller
   * can indicate whether garbage collection should be performed,
   * which slows the program but reduces memory usage.
   * 
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  static Qry preprocessQuery(String query, RetrievalModel model)
      throws IOException {
    // Optimize the query.  Remove query operators (except SCORE
    // operators) that have only 1 argument. This improves efficiency
    // and readability a bit.

    Qry q = parseQuery(query, model);

    if (q.args.size() == 1) {
      Qry q_0 = q.args.get(0);

      if (q_0 instanceof QrySop) {
        q = q_0;
      }
    }


    while (q != null && parseQueryCleanup(q));

    return q;
  }
  /**
   * Process one query.
   * @param qid query ID
   * @param query A string representing the query
   * @param model The retrieval model determines how matching and scoring is done.
   * @param out print stream determines where to print results
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static void processQuery(String qid, String query, RetrievalModel model,
                                PrintStream out)
    throws IOException {


    // Show the query that is evaluated
    Qry q = preprocessQuery(query, model);

    System.out.println("    --> " + q);

    ScoreList r = executeQuery(q, model);

    printResults(qid, r, out);
  }

  public static ScoreList executeQuery(Qry q, RetrievalModel model)
      throws IOException {
    if (q != null) {

      ScoreList r = new ScoreList ();

      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docId = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          r.add (docId, score);
          q.docIteratorAdvancePast (docId);
        }
      }

      return r;
    } else
      return null;
  }

  static void processQueryExpansion(
      String queryFilePath,
      String resultFilePath,
      RetrievalModel model,
      String initialRankingFile, String expansionQueryFile, FbModel fbModel)
      throws Exception {

    BufferedReader queryInput = null;
    PrintStream queryOut = null;
    BufferedReader rankInput = null;
    PrintStream expandOut = null;

    String lastQueryName = null;
    try {
      rankInput = new BufferedReader(new FileReader(initialRankingFile));
      String rLine;
      int cnt = 0;
      List<List<QueryResultItem>> results = new ArrayList<>();
      List<String> queryNames = new ArrayList<>();
      while ((rLine = rankInput.readLine()) != null) {
        String[] tokens = rLine.split(" ");
        String queryName = tokens[0];
        String docName = tokens[2];
        Integer docId = Idx.getInternalDocid(docName);
        Integer rank = Integer.valueOf(tokens[3]);
        Double score = Double.valueOf(tokens[4]);

        if (lastQueryName == null || !queryName.equals(lastQueryName)) {
          results.add(new ArrayList<>());
          queryNames.add(queryName);
          cnt = 0;
        }
        lastQueryName = queryName;
        cnt++;

        if (cnt <= fbModel.getFbDocs())
          results.get(results.size() - 1).add(new QueryResultItem(docId, rank, score));
      }

      queryInput = new BufferedReader(new FileReader(queryFilePath));
      queryOut = new PrintStream(new File(resultFilePath));
      expandOut = new PrintStream(new File(expansionQueryFile));

      String qLine;
      Map<String, String> origQueries = new HashMap<>();
      while ((qLine = queryInput.readLine()) != null) {
        int d = qLine.indexOf(':');
        if (d < 0) {
          throw new IllegalArgumentException
              ("Syntax error:  Missing ':' in query line.");
        }

        String qid = qLine.substring(0, d);
        origQueries.put(qid, qLine.substring(d + 1));
      }
      for (int i = 0; i < results.size(); ++i) {
        executeExpandQuery(queryNames.get(i), results.get(i),
            origQueries.get(queryNames.get(i)), queryOut, model, expandOut, fbModel);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (rankInput != null)
        rankInput.close();
      if (queryOut != null)
        queryOut.close();
      if (queryInput != null)
        queryInput.close();
      if (expandOut != null)
        expandOut.close();
    }
  }

  static void executeExpandQuery(String queryName, List<QueryResultItem> results,
      String origQryStr, PrintStream queryOut, RetrievalModel model,
      PrintStream expandOut, FbModel fbModel)
      throws Exception {
    String qryString = QryExpansion.expandQuery(results, fbModel);
    expandOut.println(queryName + ": " + qryString);
    String defaultOp = model.defaultQrySopName ();
    origQryStr = defaultOp + "(" + origQryStr + ")";

    String combinedQryString = String.format("#wand(%f %s %f %s)",
        fbModel.getFbOrigWeight(),
        origQryStr,
        1 - fbModel.getFbOrigWeight(),
        qryString
        );
    // processQuery(queryName, combinedQryString, model, queryOut);
  }

  /**
   * Process the query file.
   * @param queryFilePath
   * @param resultFilePath
   * @param model
   * @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath,
                               String resultFilePath,
                               RetrievalModel model)
      throws IOException {

    BufferedReader input = null;
    PrintStream out = null;

    try {
      String qLine;

      input = new BufferedReader(new FileReader(queryFilePath));
      out = new PrintStream(new File(resultFilePath));

      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
            ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        processQuery(qid, query, model, out);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null)
        input.close();
      if (out != null) {
        out.flush();
        out.close();
      }
    }
  }

  static Map<String, List<QryRelDoc>> readTrainingQrelsFile(String qrelsFilePath, String type)
      throws IOException {
    Map<String, List<QryRelDoc>> result = new HashMap<>();
    BufferedReader input = new BufferedReader(new FileReader(qrelsFilePath));
    String qLine;
    while ((qLine = input.readLine()) != null) {
      String[] tokens = qLine.split(" ");
      String queryName = tokens[0];
      String externalDocId = tokens[2];
      Integer relevance = type.equals("training") ?
          Integer.valueOf(tokens[3]) :
          0;

      if (!result.containsKey(queryName))
        result.put(queryName, new ArrayList<>());
      result.get(queryName).add(new QryRelDoc(externalDocId, relevance));
    }

    return result;
  }

  static Map<String, Double> readPageRankFile(String pageRankFilePath)
      throws IOException {
    BufferedReader input = new BufferedReader(new FileReader(pageRankFilePath));
    String qLine;
    Map<String, Double> result = new HashMap<>();
    while ((qLine = input.readLine()) != null) {
      String[] tokens = qLine.split("\t");
      String externalDocId = tokens[0];
      Double pageRank = Double.valueOf(tokens[1]);
      result.put(externalDocId, pageRank);
    }
    return result;
  }

  /**
   * Process the training query file.
   * @param queryFilePath A file of training queries.
   * @param qrelsFilePath A file of relevance judgments.
   * @param pageRankFilePath A file of PageRank scores.
   * @param featureVectorPath The file to write feature vectors of the queries
   * @param disabledFeatures A comma-separated list of features to disable for this experiment.
   * @param model Retrieval model.
   * @throws IOException Error accessing the Lucene index.
   */
  static void extractFeaturesFromResults(String queryFilePath,
                                         String qrelsFilePath,
                                         String pageRankFilePath,
                                         String featureVectorPath,
                                         Set<Integer> disabledFeatures,
                                         RetrievalModel model,
                                         String type)
      throws Exception {

    BufferedReader input = null;
    PrintStream out = null;

    try {
      input = new BufferedReader(new FileReader(queryFilePath));
      out = new PrintStream(new File(featureVectorPath));

      //  Each pass of the loop processes one query.

      Map<String, List<QryRelDoc>> relDocsMap =
          readTrainingQrelsFile(qrelsFilePath, type);

      Map<String, Double> pageRanks = readPageRankFile(pageRankFilePath);

      Map<String, List<List<Double>>> features = new HashMap<>();

      String qLine;
      List<String> queryNames = new ArrayList<>();
      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
              ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        Qry q = preprocessQuery(query, model);
        List<QryRelDoc> relDocs = relDocsMap.get(qid);
        for (QryRelDoc relDoc : relDocs) {
          Double pageRank = null;
          if (pageRanks.containsKey(relDoc.externalDocId))
            pageRank = pageRanks.get(relDoc.externalDocId);

          if (!features.containsKey(qid)) {
            features.put(qid, new ArrayList<>());
            queryNames.add(qid);
          }
          List<List<Double>> qryFeatures = features.get(qid);
          qryFeatures.add(QryFeatureUtils.getFeatures(q, relDoc, pageRank,
              (RetrievalModelLetor) model, disabledFeatures));
        }
      }
      for (String qid : queryNames) {
        List<List<Double>> queryFeatures = features.get(qid);
        QryFeatureUtils.normalize(queryFeatures);
        List<QryRelDoc> relDocs = relDocsMap.get(qid);

        for (int i = 0; i < queryFeatures.size(); ++i) {
          out.printf("%d qid:%s", relDocs.get(i).relevance, qid);
          for (int j = 0; j < queryFeatures.get(i).size(); ++j) {
            if (!disabledFeatures.contains(j + 1) &&
                queryFeatures.get(i).get(j) != null) {
              out.printf(" %d:%s", j + 1, queryFeatures.get(i).get(j).toString());
            }
          }
          out.printf(" # %s\n", relDocs.get(i).externalDocId);
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      if (input != null)
        input.close();
      if (out != null) {
        out.flush();
        out.close();
      }
    }
  }

  /**
   * Print the query results.
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @param out
   *          A print stream to write results to file
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryName, ScoreList result, PrintStream out) throws IOException {

    if (result == null)
      return;
    result.sort();
    result.truncate(100);

    System.out.println(queryName + ":  ");
    if (result.size() < 1) {
      System.out.println("\tNo results.");
    } else {
      for (int i = 0; i < result.size(); i++) {
        out.printf("%s Q0 %s %d %.12f fubar\n",
            queryName,
            Idx.getExternalDocid(result.getDocid(i)),
            i+1,
            result.getDocidScore(i));
      }
    }
    System.out.println();
  }

  static void printLearnResult(String testingFeatureVectorPath,
                               String testingDocumentScores,
                               String trecEvalOutputPath)
      throws Exception {
    BufferedReader featVecInput =
        new BufferedReader(new FileReader(testingFeatureVectorPath));
    BufferedReader docScoreInput =
        new BufferedReader(new FileReader(testingDocumentScores));
    String line;
    String scoreLine;
    Pattern docIdPat = Pattern.compile("# (.+)", Pattern.CASE_INSENSITIVE);
    Pattern qidPat = Pattern.compile("qid:(\\d+)");

    List<ScoreList> scoreLists = new ArrayList<>();
    List<String> qids = new ArrayList<>();
    String lastQid = null;
    while ((line = featVecInput.readLine()) != null) {
      Matcher qidMat = qidPat.matcher(line);
      String qid = "";
      if (qidMat.find())
        qid = qidMat.group(1);

      String externalDocId = "";
      Matcher docIdMat = docIdPat.matcher(line);
      if (docIdMat.find())
        externalDocId = docIdMat.group(1);
      int docId = Idx.getInternalDocid(externalDocId);

      if (lastQid == null || !lastQid.equals(qid)) {
        scoreLists.add(new ScoreList());
        qids.add(qid);
        lastQid = qid;
      }
      scoreLine = docScoreInput.readLine();

      scoreLists.get(scoreLists.size() - 1).add(docId, Double.valueOf(scoreLine));
    }
    PrintStream out = new PrintStream(new File(trecEvalOutputPath));
    for (int i = 0; i < scoreLists.size(); ++i) {
      printResults(qids.get(i), scoreLists.get(i), out);
    }
  }
  /**
   * Read the specified parameter file, and confirm that the required
   * parameters are present.  The parameters are returned in a
   * HashMap.  The caller (or its minions) are responsible for
   * processing them.
   * @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }

  /**
   * Given a query string, returns the terms one at a time with stopwords
   * removed and the terms stemmed using the Krovetz stemmer.
   * 
   * Use this method to process raw query terms.
   * 
   * @param query
   *          String containing query
   * @return Array of query tokens
   * @throws IOException Error accessing the Lucene index.
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp =
      ANALYZER.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute =
      tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<>();

    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }

    return tokens.toArray (new String[tokens.size()]);
  }

}
