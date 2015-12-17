/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    if (r instanceof RetrievalModelIndri)
      return this.docIteratorHasMatchMin (r);
    else
      return this.docIteratorHasMatchAll (r);
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
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri(r);
    } else {
      throw new IllegalArgumentException
          (r.getClass().getName() + " doesn't support the AND operator.");
    }
  }

  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   * getScore for the RankedBoolean retrieval model.
   * @param r The retrieval model that determines how scores are calculated.
   * @return The document score.
   * @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    double score = Double.MAX_VALUE;
    for (Qry q_i: this.args) {
      QrySop sop_i = (QrySop) q_i;
      double current_score = sop_i.getScore(r);
      // Finding minimum score among parameters
      if (current_score < score)
        score = current_score;
    }
    return score;
  }


  public double getDefaultScore(RetrievalModel r, int docId)
      throws IOException {
    if (!(r instanceof RetrievalModelIndri))
      throw new IllegalArgumentException
          (r.getClass().getName() + " doesn't support the AND operator.");

    double score = 1;
    for (Qry q_i: this.args) {
      QrySop sop_i = (QrySop) q_i;
        score *= Math.pow(
            sop_i.getDefaultScore(r, docId), 1.0 / this.args.size());
    }
    return score;
  }

  /**
   * getScore for the Indri retrieval model.
   * @param r The retrieval model that determines how scores are calculated.
   * @return The document score.
   * @throws IOException Error accessing the Lucene index
   */
  private double getScoreIndri (RetrievalModel r) throws IOException {
    double score = 1;
    for (Qry q_i: this.args) {
      QrySop sop_i = (QrySop) q_i;
      Double sopScore;
      if (!sop_i.docIteratorHasMatchCache() ||
          sop_i.docIteratorGetMatch() != this.docIteratorGetMatch())
        sopScore = sop_i.getDefaultScore(r, this.docIteratorGetMatch());
      else
        sopScore = sop_i.getScore(r);
      score *= Math.pow(sopScore, 1.0 / this.args.size());
    }
    return score;
  }
}
