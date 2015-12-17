/**
 *  Copyright (c) 2015, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *  The WSUM operator for all retrieval models.
 */
public class QrySopWsum extends QrySopWithWeight {
  public void initialize(RetrievalModel r) throws IOException {
    if (r instanceof RetrievalModelBM25) {
      // Initialize query operator and calculate query term frequency
      Map<String, Integer> occurrence = new HashMap<>();

      for (Iterator<Qry> iterator = args.iterator(); iterator.hasNext();) {
        Qry q_i = iterator.next();
        if (!(q_i instanceof QrySopScore))
          continue;
        String argStr = q_i.toString();

        if (occurrence.containsKey(argStr)) {
          occurrence.put(argStr, occurrence.get(argStr) + 1);
          iterator.remove();
        }
        else
          occurrence.put(argStr, 1);
      }

      for (Qry q_i: this.args) {
        String argStr = q_i.toString();

        if (occurrence.containsKey(argStr)) {
          QrySopScore score_i = ((QrySopScore) q_i);
          score_i.setQtf(occurrence.get(argStr));
        }
      }
    }
    super.initialize(r);
  }
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchMin (r);
  }

  public double getScore(RetrievalModel r) throws IOException {
    if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25(r);
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri(r);
    } else {
      throw new IllegalArgumentException
          (r.getClass().getName() + " doesn't support the WSUM operator.");
    }
  }
  public double getDefaultScore(RetrievalModel r, int docId)
      throws IOException {
    if (!(r instanceof RetrievalModelIndri))
      throw new IllegalArgumentException
          (r.getClass().getName() + " doesn't support the WSUM operator.");
    if (! this.docIteratorHasMatch(r))
      return 0.0;
    double score = 0.0;
    for (int i = 0; i < this.args.size(); ++i) {
      QrySop sop_i = (QrySop) this.args.get(i);
      score += this.weights.get(i) * sop_i.getDefaultScore(r, docId);
    }
    score /= this.weightSum;
    return score;
  }

  public double getScoreIndri(RetrievalModel r) throws  IOException {
    if (! this.docIteratorHasMatch(r))
      return 0.0;
    double score = 0.0;
    for (int i = 0; i < this.args.size(); ++i) {
      QrySop sop_i = (QrySop) this.args.get(i);
      Double sopScore;
      if (!sop_i.docIteratorHasMatchCache() ||
          sop_i.docIteratorGetMatch() != this.docIteratorGetMatch())
        sopScore = sop_i.getDefaultScore(r, this.docIteratorGetMatch());
      else
        sopScore = sop_i.getScore(r);
      score += this.weights.get(i) * sopScore;
    }
    score /= this.weightSum;
    return score;
  }

  public double getScoreBM25 (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatch(r))
      return 0.0;
    double score = 0;
    for (int i = 0; i < this.args.size(); ++i) {
      QrySop sop_i = (QrySop) this.args.get(i);
      // Scores of query terms that don't occur in the document are 0, skip them
      if (!sop_i.docIteratorHasMatchCache() ||
          sop_i.docIteratorGetMatch() != this.docIteratorGetMatch())
        continue;

      score += this.weights.get(i) * sop_i.getScore(r);
    }
    score /= this.weightSum;
    return score;
  }
}
