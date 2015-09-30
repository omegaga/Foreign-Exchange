import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by Jianhong Li on 9/28/15.
 */
public class DecisionTree {
  public static List<DataRow> readFile (String filename)
      throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    List<DataRow> dataMatrix = new ArrayList<>();

    try {
      String line = br.readLine();

      while (line != null) {
        String[] parts = line.split(" ");
        List<Double> features = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; ++i) {
          features.add(Double.parseDouble(parts[i]));
        }
        int label = Integer.parseInt(parts[parts.length - 1]);

        DataRow row = new DataRow(features, label);
        dataMatrix.add(row);
        line = br.readLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      br.close();
    }
    return dataMatrix;
  }

  public static void validate(TreeNode root, String testingFilename)
      throws IOException {
    List<DataRow> dataMatrix = readFile(testingFilename);
    int correctCount = 0;
    for (DataRow row: dataMatrix) {
      if (root.test(row.features) == row.label)
        correctCount++;
    }
    System.out.printf("Correctness: %f%%\n",
        (double)correctCount / dataMatrix.size() * 100);
  }

  public static void main (String[] args) throws IOException {
    // Read data
    String trainingFilename = "training.txt";
    List<DataRow> dataMatrix = readFile(trainingFilename);

    // Construct Decision tree with training data set
    int featureCount = dataMatrix.get(0).features.size();
    Set<Integer> remainFeatures = new HashSet<>();
    for (int i = 0; i < featureCount; ++i)
      remainFeatures.add(i);
    TreeNode root = new TreeNode(remainFeatures, dataMatrix);
    root.construct();

    // Validate test data
    String testingFilename = "testing.txt";
    validate(root, testingFilename);
  }
}
