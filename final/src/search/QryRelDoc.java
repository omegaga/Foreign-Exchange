/**
 * Created by Jianhong Li on 11/26/15.
 */
public class QryRelDoc {
  public String externalDocId;
  public int relevance;

  public QryRelDoc(String externalDocId, int relevance) {
    this.externalDocId = externalDocId;
    this.relevance = relevance;
  }
}
