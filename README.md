# scala-spark-example
Example Spark application



startup Spark in standalone mode
node1
$SPARK_HOME/sbin/start-master.sh
$SPARK_HOME/sbin/start-slaves.sh spark://172.31.36.93:7077
Spark console http://localhost:8080/




hdfs dfs -put -f scala-spark-example_2.11-0.1.jar /scala-spark-example_2.11-0.1.jar

$SPARK_HOME/bin/spark-submit 
    --class example.Main 
    --master spark://172.31.36.93:7077 
    --deploy-mode cluster 
    --executor-memory 1G 
    --total-executor-cores 4     
    hdfs:///scala-spark-example_2.12-0.1.jar 
    /Very-Short-Story.txt

node1
$SPARK_HOME/sbin/stop-slaves.sh
$SPARK_HOME/sbin/stop-master.sh




Observations - standalone Spark cluster:
* removed setting of SPARK_LOCAL_IP in spark-env.sh on each worker hosts
** this enables each worker to bind to a port on the correct local IP
** this shows correct IP addresses on master Web UI
** this enables each worker to connect to driver via correct IP
*** when SPARK_LOCAL_IP was set as 127.0.0.1, then workers were not able to connect to 127.0.0.1:<port>, unless worker was running on the same host as driver
* 0.0.0.0 has to be passed for "spark.driver.bindAddress" configuration parameter