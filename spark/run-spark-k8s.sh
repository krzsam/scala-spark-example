hdfs dfs -put -f scala-spark-example_2.11-0.1.jar /scala-spark-example_2.11-0.1.jar
$SPARK_HOME/bin/spark-submit --class example.Main --master k8s://172.31.36.93:6443 --deploy-mode cluster --executor-memory 1G --total-executor-cores 3 --name scala-spark-example --conf spark.kubernetes.container.image=krzsam/spark:spark-docker --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark hdfs://ip-172-31-36-93:4444/scala-spark-example_2.11-0.1.jar hdfs://ip-172-31-36-93:4444/Very-Short-Story.txt hdfs://ip-172-31-36-93:4444/
