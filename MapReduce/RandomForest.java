import sun.jvm.hotspot.jdi.ArrayReferenceImpl;

import java.io.*;
import java.util.*;

/**
 * Created by Jianhong Li on 10/11/15.
 */
public class RandomForest implements Serializable {
  // public List<DataRow> dataMatrix;
  public List<TreeNode> forest;
  public int N;
  public List<Double> accuracy;

  public RandomForest(int N) {
    this.N = N;
    forest = new ArrayList<>();
    accuracy = new ArrayList<>();
  }

  public RandomForest() {
    forest = new ArrayList<>();
    accuracy = new ArrayList<>();
  }

  /**
   * Train the random forest, only work in single node mode
   * @param dataMatrix matrix containing training data
   */
  public void train(List<DataRow> dataMatrix) {
    // this.dataMatrix = dataMatrix;
    int featureCount = dataMatrix.get(0).features.size();
    int sampleCount = dataMatrix.size();
    int selectedSampleCount = (int) Math.ceil(sampleCount * 2.0 / 3.0);
    int selectedFeatureCount = (int) Math.ceil(Math.sqrt(featureCount));

    Set<Integer> allFeatures = new HashSet<>();
    for (int i = 0; i < featureCount; ++i)
      allFeatures.add(i);

    // Randomly select samples
    Collections.shuffle(dataMatrix);
    List<DataRow> selectedSamples = new ArrayList<>(
        dataMatrix.subList(0, selectedSampleCount));

    for (int i = 0; i < N; ++i) {
      Set<Integer> selectedFeatures = selectRandomFeatures(allFeatures,
          selectedFeatureCount);
      TreeNode root = TreeNode.grow(selectedSamples, selectedFeatures);
      forest.add(root);

      // Record performance statistics
      int correctCount = eval(dataMatrix, selectedSampleCount);
      accuracy.add((double)correctCount / (sampleCount - selectedSampleCount));
      System.out.printf("Grow %dth tree, internal correctness: %f%%\n",
          i,  accuracy.get(i) * 100);
    }
  }


  /**
   * Select random features from a feature set
   * @param allFeatures a set containing all features
   * @param selectedFeatureCount the size of generated subset of features
   * @return a subset of features
   */
  public static Set<Integer> selectRandomFeatures(Set<Integer> allFeatures,
                                                  int selectedFeatureCount) {

    // Randomly select features
    List<Integer> tmpList = new LinkedList<>(allFeatures);
    Collections.shuffle(tmpList);
    return new HashSet<>(tmpList.subList(0, selectedFeatureCount));
  }


  public int eval(List<DataRow> dataMatrix, int selectedSampleCount) {
    int correctCount = 0;
    int sampleCount = dataMatrix.size();
    for (int j = selectedSampleCount; j < sampleCount; ++j) {
      if (this.test(dataMatrix.get(j).features) == dataMatrix.get(j).label)
        correctCount++;
    }
    return correctCount;
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

  public static byte[] serialize(RandomForest randomForest) throws IOException {
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    try (ObjectOutputStream o = new ObjectOutputStream(b)) {
      o.writeObject(randomForest);
      return b.toByteArray();
    } finally {
      b.close();
    }
  }

  public static RandomForest deserialize(byte[] bytes) throws
      IOException, ClassNotFoundException {
    ByteArrayInputStream b = new ByteArrayInputStream(bytes);

    try (ObjectInputStream o = new ObjectInputStream(b)) {
      return (RandomForest) o.readObject();
    } finally {
      b.close();
    }
  }

  public static boolean getSampleTrue() {
    return Math.random() > (1.0 / 3);
  }
}
