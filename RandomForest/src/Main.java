import java.io.*;
import java.util.*;

/**
 * Created by Jianhong Li on 9/28/15.
 */
public class Main {
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

  public static void validate(RandomForest randomForest, String testingFilename)
      throws IOException {
    List<DataRow> dataMatrix = readFile(testingFilename);
    int correctCount = 0;
    for (DataRow row: dataMatrix) {
      if (randomForest.test(row.features) == row.label)
        correctCount++;
    }
    System.out.printf("Correctness: %f%%\n",
        (double)correctCount / dataMatrix.size() * 100);
  }

  public static void serialize(RandomForest randomForest, String filename) {
    ObjectOutputStream objectOutputStream;
    try {
      objectOutputStream = new ObjectOutputStream(
          new FileOutputStream(filename));
      objectOutputStream.writeObject(randomForest);
      objectOutputStream.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main (String[] args) throws IOException {
    // Read data
    String trainingFilename = "training";
    List<DataRow> dataMatrix = readFile(trainingFilename);

    // Construct Decision tree with training data set
    int N = 500;
    RandomForest randomForest = new RandomForest(N);
    randomForest.train(dataMatrix);

    // Validate test data
    String testingFilename = "testing";
    validate(randomForest, testingFilename);

    String serializeFilename = "randomForest";
    serialize(randomForest, serializeFilename);
  }
}
