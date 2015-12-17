/**
 * Created by Jianhong Li on 11/5/15.
 */
public class FbModel {
  private Integer fbDocs;
  private Integer fbTerms;
  private Double fbMu;
  private Double fbOrigWeight;

  public Integer getFbDocs() {
    return fbDocs;
  }

  public Integer getFbTerms() {
    return fbTerms;
  }

  public Double getFbMu() {
    return fbMu;
  }

  public Double getFbOrigWeight() {
    return fbOrigWeight;
  }

  public FbModel(Integer fbDocs, Integer fbTerms,
                 Double fbMu, Double fbOrigWeight) {
    this.fbDocs = fbDocs;
    this.fbTerms = fbTerms;
    this.fbMu = fbMu;
    this.fbOrigWeight = fbOrigWeight;
  }
}
