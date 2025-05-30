import org.apache.spark.sql.{DataFrame, SparkSession, Row}
import org.apache.spark.sql.types.{ArrayType, StructType}
import org.apache.spark.sql.functions._
import scala.collection.mutable

object Main { 
  def safeFlattenProps(df: DataFrame): DataFrame = {
    if (df.columns.contains("propsInCluster")) {
      val propsField = df.schema("propsInCluster").dataType
      propsField match {
        case ArrayType(ArrayType(_, _), _) =>
          df.withColumn("propsInCluster", flatten(col("propsInCluster")))
        case _ =>
          df
      }
    } else {
      df
    }
  }

  def safeFlattenPropsPatterns(df: DataFrame): DataFrame = {
    if (df.columns.contains("propertiesInCluster")) {
      val propsField = df.schema("propertiesInCluster").dataType
      propsField match {
        case ArrayType(ArrayType(_, _), _) =>
          df.withColumn("propertiesInCluster", flatten(col("propertiesInCluster")))
        case _ =>
          df
      }
    } else {
      df
    }
  }

  def mergeTwoDFsBySortedLabels(spark: SparkSession, df1: DataFrame, df2: DataFrame): DataFrame = {
    import spark.implicits._

    // Union the two DataFrames
    val unionDF = df1.unionByName(df2)

    // Normalize nested arrays if needed
    val cleanedDF = unionDF
      .withColumn("propertiesInCluster",
        when(size($"propertiesInCluster") > 0, $"propertiesInCluster").otherwise(array(lit("")))
      )
      .withColumn("mandatoryProperties",
        when(size($"mandatoryProperties") > 0, $"mandatoryProperties").otherwise(array(lit("")))
      )
      .withColumn("optionalProperties",
        when(size($"optionalProperties") > 0, $"optionalProperties").otherwise(array(lit("")))
      )

    // Group by sortedLabels and aggregate
    val mergedDF = cleanedDF
      .groupBy($"sortedLabels")
      .agg(
        flatten(collect_set($"nodeIdsInCluster")).as("nodeIdsInCluster"),
        array_distinct(flatten(collect_set($"propertiesInCluster"))).as("propertiesInCluster"),
        array_distinct(flatten(collect_set($"optionalProperties"))).as("optionalProperties"),
        array_distinct(flatten(collect_set($"original_cluster_ids"))).as("original_cluster_ids"),
        array_distinct(aggregate(
          collect_list($"propertiesInCluster"),
          first($"propertiesInCluster", ignoreNulls = true),
          (acc, props) => array_intersect(acc, props)
        )).as("mandatoryProperties")
      )
      .withColumn("merged_cluster_id", concat(lit("merged_by_sorted_labels_"), monotonically_increasing_id()))

    // Ensure schema consistency
    val resultDF = mergedDF.select(
      $"sortedLabels".cast("array<string>"),
      $"nodeIdsInCluster".cast("array<string>"),
      $"propertiesInCluster".cast("array<string>"),
      $"optionalProperties".cast("array<string>"),
      $"original_cluster_ids".cast("array<string>"),
      $"mandatoryProperties".cast("array<string>"),
      $"merged_cluster_id".cast("string")
    )

    println("Schema after merging two DataFrames by sortedLabels:")
    resultDF.printSchema()
    println("Sample after merging:")
    resultDF.show(50)

    resultDF
  }


  def mergeTwoDFsByRelationshipTypes(spark: SparkSession, df1: DataFrame, df2: DataFrame): DataFrame = {
    import spark.implicits._

    // Union the two DataFrames
    val unionDF = df1.union(df2)

    // Clean and prepare the DataFrame
    val cleanedDF = unionDF
      .withColumn("propsInCluster",
        when(size($"propsInCluster") > 0, $"propsInCluster".cast("array<string>"))
          .otherwise(array().cast("array<string>"))
      )
      .withColumn("mandatoryProperties",
        when(size($"propsInCluster") > 0, $"propsInCluster".cast("array<string>"))
          .otherwise(array().cast("array<string>"))
      )

    // Group by relationshipTypes and aggregate
    val mergedDF = cleanedDF
      .groupBy($"relationshipTypes")
      .agg(
        flatten(collect_set($"srcLabels")).as("srcLabels"),
        flatten(collect_set($"dstLabels")).as("dstLabels"),
        array_distinct(flatten(collect_list($"propsInCluster"))).as("propsInCluster"),
        flatten(collect_list($"edgeIdsInCluster")).as("edgeIdsInCluster"),
        array_distinct(aggregate(
          collect_list($"propsInCluster"),
          first($"propsInCluster", ignoreNulls = true),
          (acc, props) => array_intersect(acc, props)
        )).as("mandatoryProperties"),
        array_distinct(flatten(collect_set($"optionalProperties"))).as("optionalProperties"),
        flatten(collect_set($"original_cluster_ids")).as("original_cluster_ids")
      )
      .withColumn("merged_cluster_id", concat(lit("merged_by_reltype_"), monotonically_increasing_id()))

    // Ensure schema consistency
    val resultDF = mergedDF.select(
      $"relationshipTypes".cast("array<string>"),
      $"srcLabels".cast("array<string>"),
      $"dstLabels".cast("array<string>"),
      $"propsInCluster".cast("array<string>"),
      $"edgeIdsInCluster".cast("array<struct<srcId:long,dstId:long>>"),
      $"mandatoryProperties".cast("array<string>"),
      $"optionalProperties".cast("array<string>"),
      $"original_cluster_ids".cast("array<string>"),
      $"merged_cluster_id".cast("string")
    )

    println("Schema after merging two DataFrames by relationshipTypes:")
    resultDF.printSchema()
    println("Sample after merging:")
    resultDF.show(50)

    resultDF
  }
def alignSchemas(df1: DataFrame, df2: DataFrame): (DataFrame, DataFrame) = {
  val df1Cols = df1.columns.toSet
  val df2Cols = df2.columns.toSet

  val allCols = df1Cols union df2Cols

  def addMissingColumns(df: DataFrame, allCols: Set[String]): DataFrame = {
    val currentCols = df.columns.toSet
    val missingCols = allCols.diff(currentCols)
    val dfWithMissingCols = missingCols.foldLeft(df)((acc, colName) => acc.withColumn(colName, lit(null)))
    dfWithMissingCols.select(allCols.toSeq.sorted.map(col): _*)
  }

  val alignedDF1 = addMissingColumns(df1, allCols)
  val alignedDF2 = addMissingColumns(df2, allCols)

  (alignedDF1, alignedDF2)
}


  def main(args: Array[String]): Unit = {
    val clusteringMethod = if (args.length < 1) "LSH" else args(0).toUpperCase()
    //check if clustering incremental
    val incremental = args.length > 1 && args(1).toLowerCase == "incremental"
    if (!Set("LSH", "KMEANS", "BOTH").contains(clusteringMethod)) {
      println(s"Invalid clustering method: $clusteringMethod. Use LSH, KMEANS, or BOTH.")
      System.exit(1)
    }

    val spark = SparkSession.builder()
      .appName("HybridLSHDemo")
      .master("local[*]")
      .config("spark.executor.memory", "16g")
      .config("spark.driver.memory", "16g")
      .config("spark.executor.cores", "4")
      .config("spark.executor.instances", "10")
      .config("spark.yarn.executor.memoryOverhead", "4g")
      .config("spark.driver.maxResultSize", "4g")
      .getOrCreate()

    import spark.implicits._
    val startTime = System.currentTimeMillis() 
    spark.sparkContext.setLogLevel("ERROR")
    
      val nodesDF = DataLoader.loadAllNodes(spark)
      val edgesDF = DataLoader.loadAllRelationships(spark)

      val allNodeProperties = nodesDF.columns.filterNot(Seq("_nodeId", "_labels", "originalLabels").contains).toSet    
      val allEdgeProperties = edgesDF.columns.filterNot(Seq("srcId", "dstId", "srcType", "dstType", "relationshipType").contains).toSet

      val binaryNodesDF = PatternPreprocessing.encodePatterns(spark, nodesDF, allNodeProperties)
      val binaryEdgesDF = PatternPreprocessing.encodeEdgePatterns(spark, edgesDF, allEdgeProperties)  

      if (clusteringMethod == "LSH" || clusteringMethod == "BOTH") {
        val clusteredNodes = LSHClustering.applyLSHNodes(spark, binaryNodesDF)
        val clusteredEdges = LSHClustering.applyLSHEdges(spark, binaryEdgesDF)

        val startClusteringTime = System.currentTimeMillis()
        val mergedPatterns = LSHClustering.mergePatternsByLabel(spark, clusteredNodes)
        val endClusteringTime = System.currentTimeMillis()
        val elapsedClusteringTime = endClusteringTime - startClusteringTime
        val elapsedClusteringTimeInSeconds = elapsedClusteringTime / 1000.0
        println(s"Elapsed time for clustering : $elapsedClusteringTimeInSeconds seconds")
        println(s"Elapsed time for clustering: $elapsedClusteringTime milliseconds")

        val mergedEdgesLabelOnly = LSHClustering.mergeEdgePatternsByEdgeLabel(spark, clusteredEdges)

        // if (!nodesDF.columns.contains("original_label")) {
        //   println("[INFO] Column 'original_label' is missing from the originalNodesDF. Please include it before running evaluation.")
        // }
        // else{
        //   // Evaluation for LSH
        //   println("\n=== Evaluation for LSH Nodes ===")
        //   Evaluation.computeMetricsForNodes(spark, nodesDF, mergedPatterns)
        //   println("\n=== Evaluation for LSH Edges (LABEL MERGED)===")
        //   Evaluation.computeMetricsForEdges(spark, edgesDF, mergedEdgesLabelOnly)

        // }

        val updatedMergedPatterns = InferSchema.inferPropertyTypesFromMerged(nodesDF, mergedPatterns, "LSH Merged Nodes", Seq("mandatoryProperties", "optionalProperties"), "_nodeId")
        val updatedMergedEdges = InferSchema.inferPropertyTypesFromMerged(edgesDF, mergedEdgesLabelOnly, "LSH Merged Edges", Seq("mandatoryProperties", "optionalProperties"), "edgeIdsInCluster")

        println("Updated Merged Patterns LSH with Types:")
        // updatedMergedPatterns.printSchema()
        updatedMergedPatterns.show(100)

        println("Updated Merged Edges LSH with Types:")
        // updatedMergedEdges.printSchema()
        updatedMergedEdges.show(100)

        val updatedMergedEdgesWCardinalities = InferSchema.inferCardinalities(edgesDF, updatedMergedEdges)
        println("Updated Merged Edges LSH with Types and Cardinalities:")
        updatedMergedEdgesWCardinalities.show(5)


        PGSchemaExporterLoose.exportPGSchema(
        updatedMergedPatterns,
        updatedMergedEdgesWCardinalities,
        "pg_schema_output_loose.txt"
        )

        PGSchemaExporterStrict.exportPGSchema(
        updatedMergedPatterns,
        updatedMergedEdgesWCardinalities,
        "pg_schema_output_strict.txt"
        )

        XSDExporter.exportXSD(updatedMergedPatterns, updatedMergedEdgesWCardinalities, "schema_output.xsd")

        val xsdPath = "schema_output.xsd"
        XSDToXMLExporter.exportToXMLFromDataframes(
          spark,
          xsdPath,
          "output_xml",
          updatedMergedPatterns,
          updatedMergedEdgesWCardinalities,
          nodesDF,
          edgesDF,
          splitPerItems = true,
          itemsPerFile = 100
        )
        XSD2X3MLGenerator.generateX3ML(
          xsdPath,
          outputPath = "output_mappings.x3ml"
        )
        X3MLBatchRunner.runX3MLBatch(
          inputFolder = "output_xml",
          x3mlMapping = "output_mappings.x3ml",
          policyFile = "generator-policies.xml",
          x3mlEngineJar = "../../x3ml-engine.jar",
          outputFolder = "output_trig"
        )

    } 
    val endTime = System.currentTimeMillis()
    val elapsedTime = endTime - startTime
    val elapsedTimeInSeconds = elapsedTime / 1000.0
    println(s"Elapsed time for proccesing : $elapsedTimeInSeconds seconds")
    println(s"Elapsed time for proccesing: $elapsedTime milliseconds")
    spark.stop()
  }
}