## Running application on Spark via Kubernetes

### Configuring Kubernetes

* A service account was added to Kubernetes to enable Spark driver application to work, 
  as described [here](https://stackoverflow.com/questions/55684693/submit-spark-application-on-kubernetes-in-cluster-mode-configured-service-acco)
```
kubectl create serviceaccount spark
kubectl create clusterrolebinding spark-role --clusterrole=edit --serviceaccount=default:spark --namespace=default
```

The above account will then later be referenced while submitting the application 
via *--conf spark.kubernetes.authenticate.driver.serviceAccountName=spark* parameter

### Preparing Spark Docker image

Some of below steps were necessary as a standard way to run Spark on Kubernetes, some other were added or some already provided Spark files were modified
to overcome a problem with *slf4j* which surfaced which was already posted (though without an answer) 
[Subject: Hadoop free spark on kubernetes => NoClassDefFound](http://mail-archives.apache.org/mod_mbox/spark-user/201903.mbox/%3C40EF8EC4A9C6ED40BAE0ADEA64DF05DC4679BFCE@DEGAMES02.eso.local%3E)
As a side note, investigation was slightly easier when the Spark docker image was started and could be inspected via below command:
```
docker run --rm -it --entrypoint=/bin/bash spark-docker
```

##### Files used to build Spark docker image:
Important note which might not be explicitly stated in the documentation and may be source of confusion - this image is to contain Spark only
(plus any additional libraries like Hadoop), but **should not** contain the jar file with application code - this will be later provided on HDFS.

* **build-spark-example-docker.sh**
  * adds whole Hadoop installation directory to the work directory
  * overwrites standard *entrypoint.sh* and *spark-submit* with the customized versions (with changes described below)
  * uses customized Dockerfile to include additional Hadoop libraries
* **Dockerfile**
  * contains additional command as below, to include Hadoop libraries in the image
  * contains additional environment variables for Hadoop
```
COPY hadoop /opt/hadoop
...
ENV HADOOP_CONF_DIR /opt/hadoop/etc/hadoop
ENV HADOOP_HOME /opt/hadoop/
ENV HADOOP_INSTALL /opt/hadoop/
```
* **entrypoint.sh**
  * the change here is to modify *SPARK_CLASSPATH* and add additional Hadoop libraries. The libraries are the same as produced by *hadoop classpath* command.
  The paths are modified to reference the sdirectory as included in the image which is */opt/hadoop*
  One important thing to note here is that the environment variable is now exported which will be used in the point below
```
export SPARK_CLASSPATH="$SPARK_CLASSPATH:${SPARK_HOME}/jars/*:...hadoop libraries here..."
```
* **spark-submit**
  * this script was changed to provide the additional libraries on classpath for Spark. The *SPARK_CLASSPATH* has to be exported above to be available correctly here 
```
exec "${SPARK_HOME}"/bin/spark-class -cp ${SPARK_CLASSPATH} org.apache.spark.deploy.SparkSubmit "$@"
```

### Submitting application to Spark

* **run-spark-k8s.sh**
  * puts the jar file onto HDFS to be available for Spark to be used (*fat* jar might need to be used here as described in Build section)
  * submits the application to Spark 
  * to make things work, both the jar file and the file to be used had to be referenced on HDFS with full *hdfs://<host>:<port>/....* path
  * *--conf spark.kubernetes.container.image=krzsam/spark:spark-docker*  parameter specifies Spark docker image as created above
  * *--conf spark.kubernetes.authenticate.driver.serviceAccountName=spark* parameter specifies Spark service account
  * Running on Kubernetes requires passing the HDFS URI so the HDFS Filesystem object can be created correctly
```
hdfs dfs -put -f scala-spark-example_2.11-0.1.jar /scala-spark-example_2.11-0.1.jar

$SPARK_HOME/bin/spark-submit 
    --class example.Main 
    --master k8s://172.31.36.93:6443 
    --deploy-mode cluster 
    --executor-memory 1G 
    --total-executor-cores 3 
    --name scala-spark-example 
    --conf spark.kubernetes.container.image=krzsam/spark:spark-docker 
    --conf spark.kubernetes.authenticate.driver.serviceAccountName=spark 
    hdfs://ip-172-31-36-93:4444/scala-spark-example_2.11-0.1.jar 
    hdfs://ip-172-31-36-93:4444/Very-Short-Story.txt
    hdfs://ip-172-31-36-93:4444/
```

State of the pods being run can be observed as below
```
$ kubectl get pods
NAME                                       READY   STATUS    RESTARTS   AGE
scala-spark-example-1563552560716-driver   1/1     Running   0          34s
scala-spark-example-1563552560716-exec-1   1/1     Running   0          26s
scala-spark-example-1563552560716-exec-2   1/1     Running   0          26s

...

$ kubectl get pods
NAME                                       READY   STATUS        RESTARTS   AGE
scala-spark-example-1563552560716-driver   0/1     Completed     0          41s
scala-spark-example-1563552560716-exec-1   0/1     Terminating   0          33s
scala-spark-example-1563552560716-exec-2   0/1     Terminating   0          33s

...

$ kubectl get pods
NAME                                       READY   STATUS      RESTARTS   AGE
scala-spark-example-1563552560716-driver   0/1     Completed   0          46s
```
### Notes
* The Docker runtime on the test cluster runs as *root* user (which should not be the case in real production environment), 
  but HDFS processes are started as *admin* - as a quick fix to enable the process to write to HDFS, the root folder had its permissions changed to enable
  writes by any user, as below
 ```
 hdfs dfs -chmod a+w /
 ```