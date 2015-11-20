import java.util.Calendar

import com.datastax.driver.core.Row
import org.apache.spark.ml.{PipelineModel, Pipeline}
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.tree.model.RandomForestModel

import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.util.MLUtils
import com.datastax.spark.connector._
import org.apache.spark.ml.classification.{RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}


/**
  * Created by Jianhong Li on 11/20/15.
  */
object SparkRandomForestApp {
  def main(args: Array[String]) {
    // Initialize Cassandra
    val CassandraHost = "127.0.0.1"
    val CassandraKeyspace = "big_data_analytics"

    val conf = new SparkConf(true)
      .set("spark.cassandra.connection.host", CassandraHost)
      .set("spark.cleaner.ttl", "3600")
      .setAppName("ScalaRandomForestApp")
      .setMaster("local")

    lazy val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    // Retrieve training data
    val trainingData =
      sc.cassandraTable(CassandraKeyspace, "training_data")
        .map{row =>
          LabeledPoint(
            row.getDouble("label"),
            Vectors.dense(
              row.getString("features").split(" ")
                .map{s : String => s.toDouble}))}

    val trainingDataFrame = sqlContext.createDataFrame(trainingData)

    // Initialize indexer
    val indexer = new StringIndexer()
      .setInputCol("label")
      .setOutputCol("directionLabel")

    // Initialize random forest
    val numTrees = 100
    val randomForest = new RandomForestClassifier()
      .setNumTrees(numTrees)
      .setLabelCol("directionLabel")
      .setPredictionCol("predictedLabel")

    val pipeline = new Pipeline()
      .setStages(Array(indexer, randomForest))

    // Train the model with training data
    val model = pipeline.fit(trainingDataFrame)

    // Retrieve testing data
    val testingData =
      sc.cassandraTable("big_data_analytics", "testing_data")
        .map{row =>
          LabeledPoint(
            row.getDouble("label"),
            Vectors.dense(
              row.getString("features").split(" ")
                .map{s : String => s.toDouble}))}

    val testingDataFrame = sqlContext.createDataFrame(testingData)

    // Use the model to predict testing data
    val predictions = model.transform(testingDataFrame)

    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("indexedLabel")
    val accuracy = evaluator.evaluate(predictions)
    println("Accuracy = " + (1.0 - accuracy))

    // Persist the random forest model
    val rfModel = model.stages(2).asInstanceOf[RandomForestClassificationModel]
    val collection = sc.parallelize(Seq(
      (Calendar.getInstance().getTime, rfModel)))
    collection.saveToCassandra(CassandraKeyspace, "random_forest",
      SomeColumns("time", "model"))
  }
}
