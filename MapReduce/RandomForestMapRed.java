import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.cassandra.hadoop.ColumnFamilyInputFormat;
import org.apache.cassandra.hadoop.ConfigHelper;
import org.apache.cassandra.db.Column;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by Jianhong Li on 11/10/15.
 */
public class RandomForestMapRed extends Configured implements Tool {
  private static final int TREE_NUM = 20;
  private static final String INITIAL_ADDRESS = "localhost";
  private static final String PORT = "9160";
  private static final String KEYSPACE = "big_data_analytics";
  private static final String COLUMN_FAMILY = "training";
  private static final int SAMPLE_COUNT = 10000;

  public static class RandomForestMapper

      extends Mapper<ByteBuffer, SortedMap<ByteBuffer, Column>, Text, IntWritable> {

    public void map(ByteBuffer key, SortedMap<ByteBuffer, Column> columns, Context context)
        throws IOException, InterruptedException {

      List<DataRow> dataMatrix = new ArrayList<>();
      for (Column column : columns.values()) {
        if (!RandomForest.getSampleTrue())
          continue;
        String value = ByteBufferUtil.string(column.value());
        String[] parts = value.split(",");
        List<Double> features = new ArrayList<>();
        for (int i = 0; i < parts.length - 1; i++) {
            features.add(Double.parseDouble(parts[i]));
        }
        Integer label = Integer.parseInt(parts[parts.length - 1]);
        DataRow row = new DataRow(features, label);
        dataMatrix.add(row);
      }
      int featureCount = dataMatrix.get(0).features.size();
      int selectedFeatureCount = (int) Math.ceil(Math.sqrt(featureCount));
      Set<Integer> allFeatures = new HashSet<>();
      for (int i = 0; i < featureCount; ++i)
        allFeatures.add(i);

      Set<Integer> selectedFeatures = RandomForest.selectRandomFeatures(
          allFeatures, selectedFeatureCount);

      TreeNode node = TreeNode.grow(dataMatrix, selectedFeatures);

      Gson gson = new GsonBuilder().create();
      Text serialized = new Text(gson.toJson(node));

      context.write(serialized, new IntWritable(1));
    }
  }

  public static class RandomForestReducer
      extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
      RandomForest randomForest = new RandomForest();
      Gson gson = new Gson();

      for (Text tree : values) {
        randomForest.forest.add(gson.fromJson(tree.toString(), TreeNode.class));
      }

      context.write(key, new Text(gson.toJson(randomForest)));
		}
  }

  @Override
  public int run(String[] strings) throws Exception {
    // Set configuration
    Configuration conf = new Configuration();
    Job job = new Job(getConf(), "RandomForestMapRed");
    job.setJarByClass(RandomForestMapRed.class);

    // Set mapper
    job.setMapperClass(RandomForestMapper.class);
    job.setMapOutputKeyClass(Text.class);


    // Set reducer
    job.setReducerClass(RandomForestReducer.class);
    job.setNumReduceTasks(1);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);

    // Set Cassandra
    job.setInputFormatClass(ColumnFamilyInputFormat.class);

    ConfigHelper.setInputRpcPort(job.getConfiguration(), PORT);
    ConfigHelper.setInputInitialAddress(job.getConfiguration(), INITIAL_ADDRESS);
    ConfigHelper.setInputPartitioner(job.getConfiguration(),
        "Murmur3Partitioner");
    ConfigHelper.setInputColumnFamily(job.getConfiguration(), KEYSPACE, COLUMN_FAMILY);

    SliceRange range = new SliceRange()
        .setStart(ByteBufferUtil.EMPTY_BYTE_BUFFER)
        .setFinish(ByteBufferUtil.EMPTY_BYTE_BUFFER)
        .setCount(SAMPLE_COUNT);
    SlicePredicate predicate = new SlicePredicate()
        .setSlice_range(range);
    ConfigHelper.setInputSlicePredicate(job.getConfiguration(), predicate);
    job.waitForCompletion(true);

    return 0;
  }
  public static void main(String[] args) throws Exception {
    ToolRunner.run(new Configuration(), new RandomForestMapRed(), args);
    System.exit(0);
  }
}
