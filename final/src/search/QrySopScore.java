/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */

  private int qtf = 1;

  public void setQtf(int qtf) {
    this.qtf = qtf;
  }

  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } else if (r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean(r);
    } else if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25(r);
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri(r);
    } else {
      throw new IllegalArgumentException
              (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }
  
  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   *  getScore for the Ranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {

    QryIop arg = (QryIop) this.args.get(0);
    if (this.docIteratorHasMatchCache())
      return arg.docIteratorGetMatchPosting().tf;
    else
      return 0.0;
  }

  /**
   *  getScore for the BM25 retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreBM25 (RetrievalModel r) throws IOException {
    RetrievalModelBM25 rBM25 = (RetrievalModelBM25) r;
    double k_1 = rBM25.k_1;
    double b = rBM25.b;
    double k_3 = rBM25.k_3;

    QryIop arg = (QryIop) (this.args.get(0));
    String field = arg.getField();
    int docId = this.docIteratorGetMatch();

    // Convert parameters to double in advance
    double tf = arg.docIteratorGetMatchPosting().tf;
    double df = arg.getDf();
    double N = Idx.getNumDocs();
    double docLen = Idx.getFieldLength(field, docId);
    double avgDocLen = (double)Idx.getSumOfFieldLengths(field) /
        Idx.getDocCount(field);

    double rsjWeight = Math.max(0, Math.log(N - df + 0.5) - Math.log(df + 0.5));
    double tfWeight = tf / (tf + k_1 * ((1 - b) + b * docLen / avgDocLen));
    double userWeight = (k_3 + 1) * qtf / (k_3 + qtf);
    return rsjWeight * tfWeight * userWeight;
  }

  /**
   *  getScore for the Indri retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreIndri (RetrievalModel r) throws IOException {
    RetrievalModelIndri rIndri = (RetrievalModelIndri) r;
    int docId = this.docIteratorGetMatch();
    QryIop arg = (QryIop) (this.args.get(0));
    int tf = arg.docIteratorGetMatchPosting().tf;

    return calculateIndriScore(rIndri, tf, arg, docId);
  }

  /**
   *  get default score for the Indri retrieval model for given doc id
   *  @param r The retrieval model that determines how scores are calculated.
   *  @param docId The document id to get score
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getDefaultScore (RetrievalModel r, int docId)
    throws IOException {
    RetrievalModelIndri rIndri = (RetrievalModelIndri) r;
    QryIop arg = (QryIop) (this.args.get(0));

    return calculateIndriScore(rIndri, 0, arg, docId);
  }

  /**
   * Calculating score according to formula of Indri
   * @param r The retrieval model that determines how scores are calculated.
   * @param tf Term frequency
   * @param arg Query object of the argument
   * @param docId Document id
   * @return The document score
   * @throws IOException
   */
  private double calculateIndriScore(
      RetrievalModelIndri r, double tf, QryIop arg, int docId)
      throws IOException {
    double mu = r.mu;
    double lambda = r.lambda;
    String field = arg.getField();
    int ctf = arg.getCtf();

    int dLen = Idx.getFieldLength(field, docId);
    long cLen = Idx.getSumOfFieldLengths(field);

    // Bayesian smoothing
    double bayesian = (tf + mu * ((double) ctf / cLen)) / (dLen + mu);

    // Mixture model smoothing
    return (1 - lambda) * bayesian + lambda * ((double) ctf / cLen);
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
