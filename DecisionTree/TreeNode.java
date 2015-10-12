import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Jianhong Li on 9/28/15.
 */

public class TreeNode {
  public TreeNode leftChild;
  public TreeNode rightChild;
  public Set<Integer> remainFeatures;
  public List<DataRow> dataMatrix;
  public Integer label;
  public Double entropy;
  public Integer selectedFeature;
  public Double selectedThreshold;
  public Double maxIG;

  public TreeNode(Set<Integer> remainFeatures, List<DataRow> dataMatrix) {
    this.remainFeatures = remainFeatures;
    this.dataMatrix = dataMatrix;
    initValues();
  }

  /**
   * Initialize values in a TreeNode instance
   */
  private void initValues() {
    int[] labelCount = new int[2];
    Boolean flag = true;
    DataRow lastRow = null;
    for (DataRow row: dataMatrix) {
      labelCount[row.label]++;
      if (lastRow != null) {
        if (row != lastRow)
          flag = false;
      } else {
        lastRow = row;
      }
    }

    int sampleCount = dataMatrix.size();

    // Calculate the entropy
    double score = 0;
    for (int i = 0; i < 2; ++i) {
      double p = ((double)labelCount[i]) / sampleCount;
      if (p == 0)
        score += 0;
      else
        score += -p * (Math.log(p) / Math.log(2));
    }

    this.entropy = score;

    // Update label
    this.label = -1;
    if (labelCount[0] == 0) {
      this.label = 1;
    } else if (labelCount[1] == 0) {
      this.label = 0;
    }
    if (flag) {
      if (labelCount[1] != labelCount[0]) {
        this.label = labelCount[0] > labelCount[1] ? 0 : 1;
      } else {
        this.label = Math.random() < 0.5 ? 0 : 1;
      }

    }
  }

  /**
   * Construct the tree using ID3
   */
  public void construct() {
    if (label != -1)
      return;
    maxIG = findThreshold();
    splitTree();
    leftChild.construct();
    rightChild.construct();
  }

  /**
   * Find the feature that maximizes information gain. For each feature, scan
   * through all possible threshold to find the largest information gain.
   */
  public double findThreshold() {
    double maxScore = Double.NEGATIVE_INFINITY;
    int sampleCount = dataMatrix.size();

    int[] totalLabelCount = new int[2];
    for (DataRow row : dataMatrix)
      totalLabelCount[row.label]++;

    for (int feature: remainFeatures) {
      int[] currentLabelCount = new int[2];

      List<FeatureLabelTuple> featureList = new ArrayList<>();
      // Pre-processing
      for (DataRow row : dataMatrix) {
        FeatureLabelTuple tuple =
            new FeatureLabelTuple(row.features.get(feature), row.label);
        featureList.add(tuple);
      }

      Collections.sort(featureList);

      for (int threshold = 1; threshold < sampleCount; ++threshold) {
        currentLabelCount[featureList.get(threshold - 1).label]++;
        if (featureList.get(threshold).feature.equals(
            featureList.get(threshold - 1).feature))
          continue;
        // Calculate information gain
        double score = 0;

        // Score below threshold
        double belowScore = 0;
        for (int i = 0; i < 2; ++i) {
          double p = ((double) currentLabelCount[i]) / threshold;
          if (p == 0)
            belowScore += 0;
          else
            belowScore += -p * (Math.log(p) / Math.log(2));
        }
        score += belowScore * ((double) threshold) / sampleCount;

        // Score above threshold
        double aboveScore = 0;
        for (int i = 0; i < 2; ++i) {
          double p = ((double) (totalLabelCount[i] - currentLabelCount[i]))
              / (sampleCount - threshold);
          if (p == 0)
            aboveScore += 0;
          else
            aboveScore += -p * (Math.log(p) / Math.log(2));
        }
        score += aboveScore * ((double) sampleCount - threshold) / sampleCount;

        score = entropy - score;
        if (score > maxScore) {
          maxScore = score;
          selectedFeature = feature;
          selectedThreshold = featureList.get(threshold).feature;
        }
      }
    }
    return maxScore;
  }

  /**
   * Split the tree with selected feature and threshold
   */
  public Boolean splitTree() {
    List<DataRow> leftData =
        this.dataMatrix.stream()
            .filter(x -> x.features.get(selectedFeature) < selectedThreshold)
            .collect(Collectors.toList());

    List<DataRow> rightData =
        this.dataMatrix.stream()
            .filter(x -> x.features.get(selectedFeature) >= selectedThreshold)
            .collect(Collectors.toList());

    Set<Integer> leftRemainFeatures = new HashSet<>(this.remainFeatures);

    this.leftChild = new TreeNode(leftRemainFeatures, leftData);
    Set<Integer> rightRemainFeatures = new HashSet<>(this.remainFeatures);

    this.rightChild = new TreeNode(rightRemainFeatures, rightData);

    return true;
  }

  /**
   * Estimate label using the trained model
   * @param features Feature list for estimation
   * @return estimated label
   */
  public int test(List<Double> features) {
    if (leftChild == null)
      return label;
    if (features.get(selectedFeature) < selectedThreshold)
      return leftChild.test(features);
    else
      return rightChild.test(features);

  }
}
