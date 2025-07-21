import org.apache.spark.sql.SparkSession
import java.io.{File, PrintWriter}
import scala.sys.process._
import scala.util.{Failure, Success, Try}
import scala.io.Source

object RMLBatchRunner {

  /** Helper function to inject filename into mapping copy */
  def patchMappingTemplate(templatePath: String, outputPath: String, inputFileName: String): Unit = {
    val content = Source.fromFile(templatePath).mkString
    val patched = content.replace("rml:source \"input.xml\"", s"""rml:source "$inputFileName"""")
    val out = new PrintWriter(outputPath)
    out.write(patched)
    out.close()
  }

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
        val fileName = file.getName
        val outputFile = new File(outDir, fileName.replaceAll("\\.xml$", ".trig"))
        val patchedMapping = new File(outDir, fileName.replaceAll("\\.xml$", ".patched.ttl"))

        // Use full path for rml:source
        patchMappingTemplate(rmlMapping, patchedMapping.getAbsolutePath, file.getAbsolutePath)

        val cmd = Seq(
          "java", "-Xmx8g", "-jar", rmlMapperJar,
          "-m", patchedMapping.getAbsolutePath,
          "-o", outputFile.getAbsolutePath,
          "-s", "trig"
        )

        println(s"[INFO] [${Thread.currentThread().getName}] Running RMLMapper for ${fileName}")
        Try(cmd.!!) match {
          case Success(_) =>
            println(s"[SUCCESS] ${fileName} → ${outputFile.getName}")
          case Failure(e) =>
            println(s"[ERROR] Failed to process ${fileName}")
            e.printStackTrace()
        }

        // patchedMapping.delete() // optional
      }
      Iterator.empty
    }.count()

    println(s"[DONE] All files processed in parallel using patched RML mappings.")
  }
}
