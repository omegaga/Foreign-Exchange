import sun.jvm.hotspot.jdi.ArrayReferenceImpl;

import java.io.Serializable;
import java.util.*;

/**
 * Created by Jianhong Li on 10/11/15.
 */
public class RandomForest implements Serializable {
  public List<DataRow> dataMatrix;
  public List<TreeNode> forest;
  public int N;
  public List<Double> accuracy;

  public RandomForest(int N) {
    this.N = N;
    forest = new ArrayList<>();
    accuracy = new ArrayList<>();
  }

  /**
   * Train the random forest
   * @param dataMatrix matrix containing training data
   */
  public void train(List<DataRow> dataMatrix) {
    this.dataMatrix = dataMatrix;
    int featureCount = dataMatrix.get(0).features.size();
    int sampleCount = dataMatrix.size();
    int selectedFeatureCount = (int) Math.ceil(Math.sqrt(featureCount));
    int selectedSampleCount = (int) Math.ceil(sampleCount * 2.0 / 3.0);

    Set<Integer> allFeatures = new HashSet<>();
    for (int i = 0; i < featureCount; ++i)
      allFeatures.add(i);

    for (int i = 0; i < N; ++i) {
      // Randomly select features
      List<Integer> tmpList = new LinkedList<>(allFeatures);
      Collections.shuffle(tmpList);
      Set<Integer> selectedFeatures = new HashSet<>(
          tmpList.subList(0, selectedFeatureCount));

      // Randomly select samples
      Collections.shuffle(dataMatrix);
      List<DataRow> selectedSamples = new ArrayList<>(
          dataMatrix.subList(0, selectedSampleCount));

      // Grow a decision tree
      TreeNode root = new TreeNode(selectedFeatures, selectedSamples);
      root.construct();
      forest.add(root);

      // Record performance statistics
      int correctCount = 0;
      for (int j = selectedSampleCount; j < sampleCount; ++j) {
        if (this.test(dataMatrix.get(j).features) == dataMatrix.get(j).label)
          correctCount++;
      }
      accuracy.add((double)correctCount / (sampleCount - selectedSampleCount));
      System.out.printf("Grow %dth tree, internal correctness: %f%%\n",
          i,  accuracy.get(i) * 100);
    }
  }

  /**
   * Test data
   * @param features feature of test data
   * @return predicted label
   */
  public int test(List<Double> features) {
    int[] voteCount = new int[2];
    for (TreeNode root : forest)
      voteCount[root.test(features)]++;

    if (voteCount[1] != voteCount[0]) {
      return voteCount[0] > voteCount[1] ? 0 : 1;
    } else {
      // Break tie by randomly choose a label
      return Math.random() < 0.5 ? 0 : 1;
    }
  }
}
