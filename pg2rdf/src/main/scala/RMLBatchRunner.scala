import org.apache.spark.sql.SparkSession
import java.io.File
import scala.sys.process._
import scala.util.{Failure, Success, Try}

object RMLBatchRunner {

  def runRMLBatch(
      spark: SparkSession,
      inputFolder: String,
      rmlMapping: String,
      rmlMapperJar: String,
      outputFolder: String = "output_trig_rml",
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
        val outputFile = new File(outDir, file.getName.replaceAll("\\.xml$", ".trig"))

        val cmd = Seq(
          "java", "-Xmx8g", "-jar", rmlMapperJar,
          "-m", rmlMapping,
          "-o", outputFile.getAbsolutePath,
          "-s", "trig",
          "-i", file.getAbsolutePath
        )

        println(s"[INFO] [${Thread.currentThread().getName}] Running RMLMapper for ${file.getName}")
        Try(cmd.!!) match {
          case Success(_) =>
            println(s"[SUCCESS] ${file.getName} → ${outputFile.getName}")
          case Failure(e) =>
            println(s"[ERROR] Failed to process ${file.getName}")
            e.printStackTrace()
        }
      }
      Iterator.empty
    }.count()

    println(s"[DONE] All files processed in parallel using RMLMapper.")
  }
}
