import com.datastax.driver.core.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by Jianhong Li on 9/28/15.
 */
public class Main {
  private static Session session;

  public static List<DataRow> readDatabase (String type) {
    List<DataRow> dataMatrix = new ArrayList<>();

    String cqlStatement = "SELECT features, label FROM data WHERE type = '%s'";
    ResultSet result = session.execute(
        String.format(cqlStatement, type));
    for (Row row : result) {
      List<Double> features = row.getList("features", Double.class);
      int label = row.getInt("label");
      DataRow dataRow = new DataRow(features, label);
      dataMatrix.add(dataRow);
    }
    return dataMatrix;
  }

  public static void validate(RandomForest randomForest)
      throws IOException {
    List<DataRow> dataMatrix = readDatabase("testing");
    int correctCount = 0;
    for (DataRow row: dataMatrix) {
      if (randomForest.test(row.features) == row.label)
        correctCount++;
    }
    System.out.printf("Correctness: %f%%\n",
        (double)correctCount / dataMatrix.size() * 100);
  }

  public static void serialize(RandomForest randomForest, String rfKey) {
    try {
      String cqlStatement =
          "INSERT INTO random_forest (rf_key, forest) VALUES (?, ?)";
      BoundStatement boundStatement = session.prepare(cqlStatement).bind();
      ByteBuffer blob = ByteBuffer.wrap(RandomForest.serialize(randomForest));
      session.execute(boundStatement.bind(rfKey, blob));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static RandomForest deserialize(String rfKey)
      throws IOException, ClassNotFoundException {
    String cqlStatement =
        "SELECT forest FROM random_forest WHERE rf_key = '%s'";
    ResultSet resultSet = session.execute(String.format(cqlStatement, rfKey));
    ByteBuffer blob = resultSet.one().getBytes("forest");
    byte[] bytes = new byte[blob.remaining()];
    blob.get(bytes);

    return RandomForest.deserialize(bytes);
  }

  public static void main (String[] args)
      throws IOException, ClassNotFoundException {
    // Read data
    String rfKey = "testRun1";
    int N = 50;

    try (Cluster cluster =
             Cluster.builder().addContactPoint("localhost").build()) {
      session = cluster.connect("big_data_analytics");
      List<DataRow> dataMatrix = readDatabase("training");

      // Construct Decision tree with training data set
      RandomForest randomForest = new RandomForest(N);
      randomForest.train(dataMatrix);

      serialize(randomForest, rfKey);
      randomForest = deserialize(rfKey);

      // Validate test data
      validate(randomForest);
    }
  }
}
