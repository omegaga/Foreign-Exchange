import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.*;

/**
 * Created by Jianhong Li on 11/4/15.
 */
public class QryExpansion {
  private static String FIELD = "keywords";

  private static Map<String, Double> getTermScores(
      List<QueryResultItem> queryResults,
      Map<String, Map<Integer, Integer>> terms,
      FbModel fbModel)
      throws Exception {
    Map<String, Double> termScores = new HashMap<>();
    for (Map.Entry<String, Map<Integer, Integer>> entry : terms.entrySet()) {
      String term = entry.getKey();
      double mu = fbModel.getFbMu();
      BytesRef termBytes = new BytesRef (term);
      Term termObj = new Term (FIELD, termBytes);
      double cLen = Idx.getSumOfFieldLengths(FIELD);
      double ctf = Idx.INDEXREADER.totalTermFreq(termObj);
      double ptc = ctf / cLen;

      for (QueryResultItem item : queryResults) {
        double dLen = Idx.getFieldLength(FIELD, item.getDocId());
        int docId = item.getDocId();
        Map<Integer, Integer> tfs = entry.getValue();
//        TermVector vector = new TermVector(item.getDocId(), FIELD);
        if (term.contains(".") || term.contains(","))
          continue;
        if (!termScores.containsKey(term))
          termScores.put(term, 0.0);

        double tf = 0.0;
        if (tfs.containsKey(docId))
          tf = tfs.get(item.getDocId());
        double ptd = (tf + mu * ptc) / (dLen + mu);
        double origScore = item.getScore();

        double score = ptd * origScore * Math.log(1.0 / ptc);
        termScores.put(term, termScores.get(term) + score);
      }
    }
    return termScores;
  }

  private static String generateQry(FbModel fbModel,
                                 Map<String, Double> termScores) {
    List<Map.Entry<String, Double>> termScoreList =
        new ArrayList<>(termScores.entrySet());
    Collections.sort(termScoreList,
        (o1, o2) -> o2.getValue().compareTo(o1.getValue()));
    String qryString = "";
    for (int i = Math.min(fbModel.getFbTerms(), termScoreList.size()) - 1;
         i >= 0;
         --i) {
      qryString += String.format("%.4f", termScoreList.get(i).getValue()) + " ";
      qryString += termScoreList.get(i).getKey();
      if (i > 0)
        qryString += " ";
    }
    return String.format("#WAND( %s )", qryString);
  }

  private static Map<String, Map<Integer, Integer>> extractTerms(
      List<QueryResultItem> queryResults)
      throws IOException {

    Map<String, Map<Integer, Integer>> terms = new HashMap<>();
    for (QueryResultItem item : queryResults) {
      TermVector vector = new TermVector(item.getDocId(), FIELD);
      for (int i = 1; i < vector.stemsLength(); ++i) {
        String term = vector.stemString(i);
        if (term.contains(".") || term.contains(","))
          continue;
        if (!terms.containsKey(term)) {
          terms.put(term, new HashMap<>());
        }
        terms.get(term).put(item.getDocId(), vector.stemFreq(i));
      }
    }
    return terms;
  }

  public static String expandQuery(List<QueryResultItem> queryResults,
                         FbModel fbModel) throws Exception {
    Map<String, Map<Integer, Integer>> terms = extractTerms(queryResults);
    Map<String, Double> termScores = getTermScores(queryResults, terms, fbModel);
    return generateQry(fbModel, termScores);
  }
}
