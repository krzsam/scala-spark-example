package example

import org.apache.spark.sql.{Dataset, SparkSession}
import org.apache.spark.storage.StorageLevel
import org.slf4j.{Logger, LoggerFactory}

// https://spark.apache.org/docs/latest/submitting-applications.html
// https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/FileSystemShell.html

/*

# Run on a Spark standalone cluster in cluster deploy mode with supervise
./bin/spark-submit \
  --class org.apache.spark.examples.SparkPi \
  --master spark://207.184.161.138:7077 \
  --deploy-mode cluster \
  --supervise \
  --executor-memory 20G \
  --total-executor-cores 100 \
  /path/to/examples.jar \
  1000

# Run on a Kubernetes cluster in cluster deploy mode
./bin/spark-submit \
  --class org.apache.spark.examples.SparkPi \
  --master k8s://xx.yy.zz.ww:443 \
  --deploy-mode cluster \
  --executor-memory 20G \
  --num-executors 50 \
  http://path/to/examples.jar \
  1000
*/

object Main {
  private val LOG: Logger = LoggerFactory.getLogger( this.getClass )

  def logResults( wordCounts: Dataset[(String, Long)] ) = {
    val counts: Array[(String, Long)] = wordCounts.collect()
    LOG.info( "Word counts: ---------" )
    counts foreach {
      pair =>
        LOG.info( s"\t${pair._1} : ${pair._2}")
    }
    LOG.info( "End ------------------" )
  }

  def writeResultsToFile(fileName: String, wordCounts: Dataset[(String, Long)], sparkSession: SparkSession ) = {
    val columns = wordCounts.columns mkString ","
    LOG.info( s"wordCounts columns: ${columns}")

    // https://data-flair.training/blogs/apache-spark-rdd-vs-dataframe-vs-dataset/
    val renamed = wordCounts.withColumnRenamed( "count(1)", "count" )
    renamed.persist( StorageLevel.MEMORY_AND_DISK )

    val now = System.currentTimeMillis()
    val newFile1 = "hdfs://" + fileName + s".${now}-r-o"
    val newFile2 = "hdfs://" + fileName + s".${now}-o-r"
    renamed.repartition( 1 ).orderBy( "value" ).write.csv( newFile1 )

    // the below stores ordered data in 1 partition
    renamed.orderBy( "value" ).repartition( 1 ).write.csv( newFile2 )
  }

  def main(args: Array[String]): Unit = {
    val parameters = args.mkString(",")
    val version = 0.11
    LOG.info( s"Version: ${version} Parameters passed: ${parameters}")
    if( args.size != 1 )
    {
      LOG.error( "Need to provide parameters: <HDFS://file>")
      System.exit( 1 )
    }

    val fileName = args(0)

    // https://spark.apache.org/docs/latest/configuration.html
    val sparkSession: SparkSession = SparkSession.builder().appName( "scala-spark-example" )
      .config( "spark.driver.bindAddress", "0.0.0.0" )
      .getOrCreate()

    // spark.driver.userClassPathFirst : true
    // spark.executor.userClassPathFirst : true
    // spark.driver.extraClassPath : /usr/share/scala/lib
    // spark.executor.extraClassPath : /usr/share/scala/lib

    import sparkSession.implicits._
    val text: Dataset[String] = sparkSession.read.textFile( fileName )
    text.persist( StorageLevel.MEMORY_AND_DISK )
    val separator = "[ \\,\\.\\-\t\r\n]+"
    val wordCounts = text.flatMap( _ split separator ).groupByKey(identity).count()
    wordCounts.persist( StorageLevel.MEMORY_AND_DISK )

    //logResults( wordCounts )
    writeResultsToFile( fileName, wordCounts, sparkSession )

    sparkSession.stop()
  }
}
