import Dependencies._

ThisBuild / scalaVersion     := "2.12.17"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

// Uncomment and modify if needed for Windows
// ThisBuild / javaHome := Some(file("C:\\Program Files\\Java\\jdk-11.0.17"))

lazy val root = (project in file("."))
  .settings(
    name := "pg2rdf",

    libraryDependencies ++= Seq(
      munit % Test,                  // Unit testing
      sparkCore,                      // Spark Core
      sparkSql,                       // Spark SQL
      sparkMllib,                     // Spark MLlib
      "org.apache.spark" %% "spark-hive" % sparkVer,
      "org.neo4j.driver" % "neo4j-java-driver" % "4.4.10",
      "org.neo4j" % "neo4j-connector-apache-spark_2.12" % "4.1.4", // Consider updating to 4.1.6
      "org.scala-lang" % "scala-library" % scalaVer
    ),

    // Add Neo4j Maven repository to resolve the connector
    resolvers += Resolver.mavenCentral,


    // Ensure the application forks when running to use the Java options
    Compile / run / fork := true,
    Compile / run / javaOptions ++= Seq(
        "-Xmx128G", // Set max heap size
        "-Dspark.executor.memory=32G",
        "-Dspark.driver.memory=32G",
        "-Dspark.driver.total.executor.cores=96",
        "-Dspark.executor.cores=8",
        "-Dspark.driver.cores=3",
        "-Dspark.executor.instances=10",
        "-Dspark.yarn.executor.memoryOverhead=8G",
        "-Dspark.driver.maxResultSize=16G"
    ) 

  )

import sbtassembly.AssemblyPlugin.autoImport._

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}