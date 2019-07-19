## Running application on Spark in standalone cluster mode

### Spark configuration

The below instructions expect **SPARK_HOME** environment variable to be defined in the environment and point to the Spark installation directory, 
for example */opt/spark-2.4.3-bin-without-hadoop*

* **Master node**
  * Slave nodes are defined in *$SPARK_HOME/conf/slaves*
```
ip-172-31-36-93
ip-172-31-38-214
ip-172-31-45-170
```
* **On each node**
  * Configuration is provided in *$SPARK_HOME/conf/spark-env.sh*
    * **HADOOP_CONF_DIR** - points to the Hadoop directory with configuration files
    * **SPARK_DIST_CLASSPATH** - adds additional Hadoop libraries to be available to Spark. If any other libraries need to be added and then need to be available 
      universally for all Spark applications, they could be added here too.
    * **SPARK_MASTER_HOST** - points to the *Master* IP
    * **SPARK_LOCAL_IP** - is **not set** on any nodes - setting this variable explicitly to any value, whether it is IP representing localhost or eth0 interface
      causes problems as exwecutors are not able to connect back to the driver - having this variable not set enables Spark to manage these IPs on its own better
```
export HADOOP_CONF_DIR=/opt/hadoop-3.2.0/etc/hadoop
export SPARK_DIST_CLASSPATH=$(/opt/hadoop-3.2.0/bin/hadoop classpath)
export SPARK_MASTER_HOST=172.31.36.93
```
    
##### Starting up

* **On Master node**
```
$SPARK_HOME/sbin/start-master.sh
$SPARK_HOME/sbin/start-slaves.sh spark://172.31.36.93:7077
```

##### Shutting down
* **On Master node**
```
$SPARK_HOME/sbin/stop-slaves.sh
$SPARK_HOME/sbin/stop-master.sh
```

### Submitting application to Spark

* **run-spark-standalone.sh**
  * puts the application jar file onto HDFS to be available for Spark (*fat* jar might need to be used here as described in Build section)
  * submits the application to Spark 
  * uses *cluster* mode of deployment, which means the Spark driver is executed on one of the Spark cluster nodes
```
hdfs dfs -put -f scala-spark-example_2.11-0.1.jar /scala-spark-example_2.11-0.1.jar

$SPARK_HOME/bin/spark-submit 
    --class example.Main 
    --master spark://172.31.36.93:7077 
    --deploy-mode cluster 
    --executor-memory 1G 
    --total-executor-cores 3 
    hdfs:///scala-spark-example_2.11-0.1.jar 
    /Very-Short-Story.txt
```

### Notes
* It was appeared it was not actually necessary to set **SPARK_LOCAL_IP** in *spark-env.sh* on each worker hosts. Setting it to any explicit value
  caused executors not being able to connect back to driver, or driver not being able to pass work to executors:
  * enables each worker to bind to a port on the correct local IP
  * shows correct IP addresses on master Web UI
  * enables each executor to connect to driver via correct IP
    * when **SPARK_LOCAL_IP** was set as *127.0.0.1*, then workers were not able to connect to *127.0.0.1:<port>*, 
      unless executor was running on the same host as driver - in this particular scenario application seemed to run correctly as it finished
      the problem was that the only executor used was the one running on the same host as the driver