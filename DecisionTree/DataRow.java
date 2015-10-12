import java.util.List;

/**
 * Created by Jianhong Li on 9/28/15.
 */
public class DataRow {
  DataRow(List<Double> features, int label) {
    this.features = features;
    this.label = label;
  }

  @Override
  public boolean equals(Object obj) {
    if (getClass() != obj.getClass())
      return false;
    DataRow other = (DataRow) obj;
    for (int i = 0; i < features.size(); ++i)
      if (this.features.get(i).equals(other.features.get(i)))
        return false;
    return true;
  }

  public List<Double> features;
  public int label;
}
