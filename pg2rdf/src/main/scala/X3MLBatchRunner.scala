import org.apache.spark.sql.SparkSession
import java.io.{File}
import scala.sys.process._
import scala.util.{Failure, Success, Try}

object X3MLBatchRunner {

  def runX3MLBatch(
      spark: SparkSession,
      inputFolder: String,
      x3mlMapping: String,
      policyFile: String,
      x3mlEngineJar: String,
      outputFolder: String = "output_trig",
      numPartitions: Int = 4 
  ): Unit = {
    val inputDir = new File(inputFolder)
    val outDir = new File(outputFolder)
    if (!outDir.exists()) {
      println(s"[INFO] Creating output directory: ${outDir.getAbsolutePath}")
      outDir.mkdirs()
    }

    val xmlFiles = Option(inputDir.listFiles()).getOrElse(Array.empty)
      .filter(_.getName.endsWith(".xml"))
      .map(_.getAbsolutePath)

    if (xmlFiles.isEmpty) {
      println(s"[WARN] No XML files found in $inputFolder.")
      return
    }

    val filesRDD = spark.sparkContext.parallelize(xmlFiles, numPartitions)

    filesRDD.mapPartitions { filesIter =>
      filesIter.foreach { filePath =>
        val file = new File(filePath)
        val trigFile = new File(outDir, file.getName.replaceAll("\\.xml$", ".trig"))

        val cmd = Seq(
          "java", "-Xms8g", "-Xmx12g", "-jar", x3mlEngineJar,
          "--input", file.getAbsolutePath,
          "--x3ml", x3mlMapping,
          "--policy", policyFile,
          "--output", trigFile.getAbsolutePath,
          "--format", "application/trig",
          "--reportProgress"
        )

        println(s"[INFO] [${Thread.currentThread().getName}] Running X3ML for ${file.getName}")
        Try(cmd.!!) match {
          case Success(_) =>
            println(s"[SUCCESS] ${file.getName} → ${trigFile.getName}")
          case Failure(e) =>
            println(s"[ERROR] Failed to process ${file.getName}")
            e.printStackTrace()
        }
      }

      Iterator.empty // Δεν επιστρέφουμε τίποτα από τα partitions
    }.count() // Trigger action

    println(s"[DONE] All files processed in parallel using mapPartitions.")
  }
}
