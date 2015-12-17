
import org.apache.lucene.index.Term;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jianhong Li on 11/26/15.
 */
public class QryFeatureUtils {
  private static final int TOTAL_FEATURES = 18;
  public static List<Double> getFeatures(Qry q,
                                         QryRelDoc relDoc,
                                         Double pageRank,
                                         RetrievalModelLetor model,
                                         Set<Integer> disabledFeatures)
      throws Exception {
    int docId = Idx.getInternalDocid(relDoc.externalDocId);
    List<Double> features = new ArrayList<>();

    Map<String, Integer> qry = qryToMap(q);

    String rawUrl = Idx.getAttribute("rawUrl", docId);
    for (int i = 1; i <= TOTAL_FEATURES; ++i) {
      if (disabledFeatures.contains(i)) {
        features.add(null);
        continue;
      }
      switch (i) {
        case 1:
          // f1: Spam score for d (read from index).
          double spam = Double.valueOf(Idx.getAttribute("score", docId));
          features.add(spam);
          break;

        case 2:
          // f2: Url depth for d(number of '/' in the rawUrl field).
          double depth = rawUrl.chars().filter(ch -> ch == '/').count();
          features.add(depth);
          break;

        case 3:
          // f3: FromWikipedia score for d
          // (1 if the rawUrl contains "wikipedia.org", otherwise 0).
          double fromWiki = rawUrl.contains("wikipedia.org") ? 1.0 : 0.0;
          features.add(fromWiki);
          break;

        case 4:
          // f4: PageRank score for d (read from file).
          features.add(pageRank);
          break;

        // f5 - f7: BM25, Indri and TermOverlap score for <q, d_body>.
        case 5:
          features.add(featureBM25(qry, docId, "body", model.rBM25));
          break;
        case 6:
          features.add(featureIndri(qry, docId, "body", model.rIndri));
          break;
        case 7:
          features.add(featureTermOverlap(qry, docId, "body"));
          break;

        // f8 - f10: BM25, Indri and TermOverlap score for <q, d_title>.
        case 8:
          features.add(featureBM25(qry, docId, "title", model.rBM25));
          break;
        case 9:
          features.add(featureIndri(qry, docId, "title", model.rIndri));
          break;
        case 10:
          features.add(featureTermOverlap(qry, docId, "title"));
          break;

        // f11 - f13: BM25, Indri and TermOverlap score for <q, d_url>.
        case 11:
          features.add(featureBM25(qry, docId, "url", model.rBM25));
          break;
        case 12:
          features.add(featureIndri(qry, docId, "url", model.rIndri));
          break;
        case 13:
          features.add(featureTermOverlap(qry, docId, "url"));
          break;

        // f14 - f16: BM25, Indri and TermOverlap score for <q, d_inlink>.
        case 14:
          features.add(featureBM25(qry, docId, "inlink", model.rBM25));
          break;
        case 15:
          features.add(featureIndri(qry, docId, "inlink", model.rIndri));
          break;
        case 16:
          features.add(featureTermOverlap(qry, docId, "inlink"));
          break;

        case 17:
          // f17: doclen
          features.add((double) Idx.getFieldLength("body", docId));
          break;

        // f18: stop word ratio
        case 18:
          features.add(featureStopWordRatio(docId, "body"));
          break;
      }
    }

    return features;
  }

  private static Map<String, Integer> qryToMap(Qry q) {
    Map<String, Integer> qry = new HashMap<>();

    for (Iterator<Qry> iterator = q.args.iterator(); iterator.hasNext();) {
      Qry q_i = iterator.next();
      QryIopTerm iopTerm;
      if (q_i instanceof QryIopTerm) {
        iopTerm = (QryIopTerm) q_i;
      } else {
        if (!(q_i instanceof QrySopScore))
          continue;
        iopTerm = (QryIopTerm) q_i.args.get(0);
      }

      String term = iopTerm.getTerm();
      if (qry.containsKey(term)) {
        qry.put(term, qry.get(term) + 1);
        iterator.remove();
      }
      else
        qry.put(term, 1);
    }
    return qry;
  }

  public static void normalize(List<List<Double>> features) {
    int docCount = features.size();
    if (docCount == 0)
      return;
    int featureCount = features.get(0).size();
    for (int i = 0; i < featureCount; ++i) {
      Double minFeatureVal = features.get(0).get(i);
      Double maxFeatureVal = minFeatureVal;
      for (List<Double> docFeature : features) {
        Double curFeatureVal = docFeature.get(i);
        if (curFeatureVal == null)
          continue;
        if (minFeatureVal == null) {
          minFeatureVal = curFeatureVal;
          maxFeatureVal = curFeatureVal;
        } else {
          minFeatureVal = Math.min(minFeatureVal, curFeatureVal);
          maxFeatureVal = Math.max(maxFeatureVal, curFeatureVal);
        }
      }
      if (minFeatureVal == null || maxFeatureVal.equals(minFeatureVal)) {
        for (List<Double> docFeature : features) {
          docFeature.set(i, 0.0);
        }
      } else {
        for (List<Double> docFeature : features) {
          Double curFeatureVal = docFeature.get(i);
          Double normFeatureVal = null;
          if (curFeatureVal != null) {
            normFeatureVal = (curFeatureVal - minFeatureVal) /
                (maxFeatureVal - minFeatureVal);
          }
          docFeature.set(i, normFeatureVal);
        }
      }
    }
  }

  public static Double featureBM25(Map<String, Integer> qry, int docId,
                                   String field, RetrievalModelBM25 rBM25)
      throws IOException {
    double k_1 = rBM25.k_1;
    double b = rBM25.b;
    double k_3 = rBM25.k_3;
    double N = Idx.getNumDocs();
    double docLen = Idx.getFieldLength(field, docId);
    double avgDocLen = (double)Idx.getSumOfFieldLengths(field) /
        Idx.getDocCount(field);

    double score = 0.0;
    TermVector vector = new TermVector(docId, field);
    if (vector.stemsLength() == 0)
      return null;
    for (int i = 1; i < vector.stemsLength(); ++i) {
      String term = vector.stemString(i);
      if (qry.containsKey(term)) {
        // Convert parameters to double in advance
        double tf = vector.stemFreq(i);
        double df = vector.stemDf(i);
        double qtf = qry.get(term);

        double rsjWeight = Math.max(0,
            Math.log(N - df + 0.5) - Math.log(df + 0.5));
        double tfWeight = tf / (tf + k_1 * ((1 - b) + b * docLen / avgDocLen));
        double userWeight = (k_3 + 1) * qtf / (k_3 + qtf);
        score += rsjWeight * tfWeight * userWeight;
      }
    }
    return score;
  }

  public static Double featureIndri(Map<String, Integer> qry, int docId,
                                    String field, RetrievalModelIndri rIndri)
      throws IOException {
    Double score = 1.0;

    double mu = rIndri.mu;
    double lambda = rIndri.lambda;
    int termSize = qry.size();

    TermVector vector = new TermVector(docId, field);
    Set<String> missingTerms =
        qry.keySet().stream().collect(Collectors.toSet());

    double dLen = vector.positionsLength();
    double cLen = Idx.getSumOfFieldLengths(field);

    if (vector.stemsLength() == 0)
      return null;
    for (int i = 1; i < vector.stemsLength(); ++i) {
      String term = vector.stemString(i);
      if (qry.containsKey(term)) {

        // Convert parameters to double in advance
        double tf = vector.stemFreq(i);
        double ctf = vector.totalStemFreq(i);

        // Bayesian smoothing
        double bayesian = (tf + mu * (ctf / cLen)) / (dLen + mu);

        // Mixture model smoothing
        double stemScore = (1 - lambda) * bayesian + lambda * (ctf / cLen);

        score *= Math.pow(stemScore, 1.0 / termSize);
        missingTerms.remove(term);
      }
    }
    if (missingTerms.size() == qry.size())
      return 0.0;
    for (String term : missingTerms) {
      // Convert parameters to double in advance
      double tf = 0.0;
      double ctf = Idx.INDEXREADER.totalTermFreq(new Term(field, term));

      // Bayesian smoothing
      double bayesian = (tf + mu * (ctf / cLen)) / (dLen + mu);

      // Mixture model smoothing
      double stemScore = (1 - lambda) * bayesian + lambda * (ctf / cLen);
      score *= Math.pow(stemScore, 1.0 / termSize);
    }
    return score;
  }

  public static Double featureTermOverlap(Map<String, Integer> qry, int docId,
                                          String field) throws IOException {
    TermVector vector = new TermVector(docId, field);
    int cnt = 0;
    if (vector.stemsLength() == 0)
      return null;
    for (int i = 1; i < vector.stemsLength(); ++i) {
      String term = vector.stemString(i);
      if (qry.containsKey(term))
        cnt++;
    }
    return 1.0 * cnt / qry.size();
  }

  public static Double featureStopWordRatio(int docId, String field)
      throws IOException {
    TermVector vector = new TermVector(docId, field);
    if (vector.stemsLength() == 0)
      return null;

    double sum = 0;
    for (int i = 1; i < vector.stemsLength(); ++i) {
      sum += vector.stemFreq(i);
    }

    double docLen = vector.positionsLength();
    return sum / docLen;
  }
}
