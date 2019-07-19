# scala-spark-example
Example Spark application to count word occurrences in the provided file located on HDFS

## Application structure

#### Building

In this version the application is packaged into jar using standard sbt package command, which will produce a jar containing only application specific classes - 
this is fine in situation when the application uses only libraries provided by either Spark or Hadoop.

In situation when other libraries might be used, the *sbt-assembly* plugin is included - in this case, application needs to be packaged into _fat_ jar
using plugin's assembly command - also, this jar will then need to be put on HDFS and provided to Spark to be run. Care needs to be taken
in *sbt* dependency configuration to mark all libraries already available within Spark or Hadoop as provided, so they will not be unnecessarily
included withing *fat* jar.

#### Functionality

The application reads file provided as parameter and located on HDFS, and then count words in it and outputs the words and their counts 
in an alphabetically sorted way to the output file. 

#### Note
* 0.0.0.0 has to be passed as value for *spark.driver.bindAddress* configuration parameter from the application

## [Running application on Spark standalone cluster](https://github.com/krzsam/scala-spark-example/tree/master/README-spark-standalone.md)

## [Running application on Spark via Kubernetes](https://github.com/krzsam/scala-spark-example/tree/master/README-spark-k8s.md)

## Links
* [Hadoop/HDFS](https://hadoop.apache.org/): 3.2.0
  * [HDFS FS shell reference](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/FileSystemShell.html)
    * for latest version it is recommended to use *hdfs* instead of *hadoop* command
* [Spark](https://spark.apache.org/docs/latest/index.html): 2.4.3
  * this install **included** Scala 2.11.12
  * this install **did not include** Hadoop and instead used jars from the above separate Hadoop installation
  * [Submitting applications in Spark](https://spark.apache.org/docs/latest/submitting-applications.html)
  * [Running Spark on K8s](https://spark.apache.org/docs/latest/running-on-kubernetes.html)
  * [Spark configuration parameters](https://spark.apache.org/docs/latest/configuration.html)
* Docker
  * [Dockerfile reference](https://docs.docker.com/engine/reference/builder/)
  * [Docker Docs](https://docs.docker.com/get-started/)
  * [Spark Docker repository](https://cloud.docker.com/u/krzsam/repository/docker/krzsam/spark)
* Kubernetes
  * [Kubernetes Deployment Docs](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/)
  * [Kubernetes Service Docs](https://kubernetes.io/docs/concepts/services-networking/service/)
* [sbt-assembly](https://github.com/sbt/sbt-assembly): 0.14.9
* Scala: 2.11.12  (this exact version as it was the version included in Spark)
* Infrastructure
  * AWS, 3 nodes _t3a.xlarge_ (4 processors, 16GB memory)
  * For simplicity, all network traffic on all TCP and UDP ports is enabled in between each of the nodes
    * ip-172-31-36-93 : k8s _Master_ (also serves as _Worker_ node), Spark standalone _Master_ and _Slave_ node
    * ip-172-31-38-214 : k8s _Worker_ node, Spark standalone _Slave_ node
    * ip-172-31-45-170 : k8s _Worker_ node, Spark standalone _Slave_ node
    