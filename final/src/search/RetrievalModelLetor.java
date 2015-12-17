/**
 * Created by Jianhong Li on 11/26/15.
 */
public class RetrievalModelLetor extends RetrievalModel {

  public RetrievalModelBM25 rBM25;
  public RetrievalModelIndri rIndri;

  public String defaultQrySopName () {
    return "#and";
  }

}

