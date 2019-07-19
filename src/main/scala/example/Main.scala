package example

import java.net.URI
import java.nio.charset.StandardCharsets
import java.util

import org.apache.hadoop.fs.{FSDataOutputStream, FileSystem, Path}
import org.apache.spark.sql.{DataFrame, Dataset, Row, SparkSession}
import org.apache.spark.storage.StorageLevel
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConverters._

object Main {
  private val LOG: Logger = LoggerFactory.getLogger( this.getClass )

  def writeResultsToFile(fileName: String, wordCounts: Dataset[(String, Long)], sparkSession: SparkSession, hadoopURI: Option[String] ) = {
    val columns = wordCounts.columns mkString ","
    LOG.info( s"wordCounts columns: ${columns}")

    val renamed: DataFrame = wordCounts.withColumnRenamed( "count(1)", "count" )
    renamed.persist( StorageLevel.MEMORY_AND_DISK )

    val now = System.currentTimeMillis()
    val prefix = if( fileName.startsWith( "hdfs") ) "" else "hdfs://"
    val newFile = new Path( prefix + fileName + s".${now}.out" )
    val hadoop = sparkSession.sparkContext.hadoopConfiguration
    LOG.info( s"Hadoop configuration is: ${hadoop}" )

    val variables = System.getenv.asScala

    variables.foreach{
      (pair) =>
        LOG.info( s"${pair._1} -> ${pair._2}")
    }

    val fs = hadoopURI match {
      case Some( uri ) => FileSystem.get( new URI(uri), hadoop )
      case None => FileSystem.get( hadoop )
    }

    LOG.info( s"From URI: ${hadoopURI} got filesystem: ${fs} , ${fs.getStatus} , ${fs.getCanonicalServiceName} , ${fs.getUri}" )

    val outputStream: FSDataOutputStream = fs.create( newFile )

    val iterator: util.Iterator[Row] = renamed.orderBy( "value").toLocalIterator
    outputStream.write( s"word,count\n".getBytes(StandardCharsets.UTF_8) )
    while( iterator.hasNext ) {
      val row = iterator.next()
      outputStream.write( s"${row.get( 0 )},${row.get( 1 )}\n".getBytes(StandardCharsets.UTF_8) )
    }

    outputStream.close()
  }

  def main(args: Array[String]): Unit = {
    val parameters = args.mkString(",")
    val version = 0.15
    LOG.info( s"Version: ${version} Parameters passed: ${parameters}")
    if( args.size <1 || args.size > 2 )
    {
      LOG.error( "Need to provide parameters: <HDFS://file>  [<HADOOP-URI>]")
      System.exit( 1 )
    }

    val fileName = args(0)
    val hadoopURI = if( args.size == 2 ) Some( args(1) ) else None

    // https://spark.apache.org/docs/latest/configuration.html
    val sparkSession: SparkSession = SparkSession.builder().appName( "scala-spark-example" )
      .config( "spark.driver.bindAddress", "0.0.0.0" )
      .getOrCreate()

    import sparkSession.implicits._
    val text: Dataset[String] = sparkSession.read.textFile( fileName )
    text.persist( StorageLevel.MEMORY_AND_DISK )
    val separator = "[ \\,\\.\\-\t\r\n]+"
    val wordCounts = text.flatMap( _ split separator ).groupByKey(identity).count()
    wordCounts.persist( StorageLevel.MEMORY_AND_DISK )

    writeResultsToFile( fileName, wordCounts, sparkSession, hadoopURI )

    sparkSession.stop()
  }
}
