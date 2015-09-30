/**
 * Created by Jianhong Li on 9/29/15.
 */
public class FeatureLabelTuple implements Comparable<FeatureLabelTuple>{
  public Double feature;
  public Integer label;
  public FeatureLabelTuple(Double feature, Integer label) {
    this.feature = feature;
    this.label = label;
  }
  public int compareTo(FeatureLabelTuple o) {
    return this.feature.compareTo(o.feature);
  }

}
