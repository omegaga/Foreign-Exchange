/**
 * Created by Jianhong Li on 11/4/15.
 */
public class QueryResultItem {
  private Integer docId;
  private Integer rank;
  private Double score;

  public QueryResultItem(Integer docId, Integer rank, Double score) {
    this.docId = docId;
    this.rank = rank;
    this.score = score;
  }

  public Double getScore() {
    return score;
  }

  public Integer getRank() {
    return rank;
  }

  public Integer getDocId() {
    return docId;
  }
}
