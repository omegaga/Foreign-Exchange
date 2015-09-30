import javax.xml.crypto.Data;
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
    for (DataRow row: dataMatrix) {
      labelCount[row.label]++;
    }

    int sampleCount = dataMatrix.size();

    // Calculate the entropy
    double score = 0;
    for (int i = 0; i < 2; ++i) {
      double p = ((double)labelCount[i]) / sampleCount;
      score += -p * (Math.log(p) / Math.log(2));
    }

    this.entropy = score;

    // Update label
    this.label = -1;
    if (this.remainFeatures.size() == 0) {
      this.label = (labelCount[1] > labelCount[0]) ? 1 : 0;
    } else if (labelCount[0] == 0) {
      this.label = 1;
    } else if (labelCount[1] == 0) {
      this.label = 0;
    }
  }

  /**
   * Construct the tree using ID3
   */
  public void construct() {
    if (label != -1)
      return;
    findThreshold();
    splitTree();
    leftChild.construct();
    rightChild.construct();
  }

  /**
   * Find the feature that maximizes information gain. For each feature, scan
   * through all possible threshold to find the largest information gain.
   */
  public void findThreshold() {
    double maxScore = Double.MIN_VALUE;

    for (int feature: remainFeatures) {
      int[] totalLabelCount = new int[2];
      int[] currentLabelCount = new int[2];
      int sampleCount = dataMatrix.size();

      List<FeatureLabelTuple> featureList = new ArrayList<>();

      // Pre-processing
      for (int i = 0; i < sampleCount; ++i) {
        DataRow row = dataMatrix.get(i);
        FeatureLabelTuple tuple = new FeatureLabelTuple(row.features.get(feature),
            row.label);
        featureList.add(tuple);
        totalLabelCount[row.label]++;
      }

      Collections.sort(featureList);

      currentLabelCount[dataMatrix.get(0).label]++;
      for (int threshold = 1; threshold < sampleCount; ++threshold) {
        if (featureList.get(threshold).label !=
            featureList.get(threshold - 1).label) {
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
          score += aboveScore *
              ((double) sampleCount - threshold) / sampleCount;

          score = entropy - score;
          if (score > maxScore) {
            maxScore = score;
            selectedFeature = feature;
            selectedThreshold = featureList.get(threshold).feature;
          }
        }
        currentLabelCount[dataMatrix.get(0).label]++;
      }
    }
  }

  /**
   * Split the tree with selected feature and threshold
   */
  public void splitTree() {
    List<DataRow> leftData =
        this.dataMatrix.stream()
            .filter(x -> x.features.get(selectedFeature) < selectedThreshold)
            .collect(Collectors.toList());
    Set<Integer> leftRemainFeatures = new HashSet<>(this.remainFeatures);
    leftRemainFeatures.remove(selectedFeature);
    this.leftChild = new TreeNode(leftRemainFeatures, leftData);

    List<DataRow> rightData =
        this.dataMatrix.stream()
            .filter(x -> x.features.get(selectedFeature) >= selectedThreshold)
            .collect(Collectors.toList());
    Set<Integer> rightRemainFeatures = new HashSet<>(this.remainFeatures);
    rightRemainFeatures.remove(selectedFeature);
    this.rightChild = new TreeNode(rightRemainFeatures, rightData);
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
