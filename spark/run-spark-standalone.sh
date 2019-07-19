hdfs dfs -put -f scala-spark-example_2.11-0.1.jar /scala-spark-example_2.11-0.1.jar
$SPARK_HOME/bin/spark-submit --class example.Main --master spark://172.31.36.93:7077 --deploy-mode cluster --executor-memory 1G --total-executor-cores 3 hdfs:///scala-spark-example_2.11-0.1.jar /Very-Short-Story.txt
