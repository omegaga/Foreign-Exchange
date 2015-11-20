name := "Spark Random Forest"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "1.5.2",
  "com.datastax.spark" % "spark-cassandra-connector_2.11" % "1.4.0"
  )
