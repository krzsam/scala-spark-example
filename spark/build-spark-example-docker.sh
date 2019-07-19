rm -rf work
mkdir work
mkdir work/hadoop
cp -R $SPARK_HOME/* work
cp -R $HADOOP_INSTALL/* work/hadoop
cp -f entrypoint.sh work/kubernetes/dockerfiles/spark/
cp -f spark-submit work/bin/

cd work
$SPARK_HOME/bin/docker-image-tool.sh -r krzsam -t spark-docker -f ../Dockerfile build
$SPARK_HOME/bin/docker-image-tool.sh -r krzsam -t spark-docker -f ../Dockerfile push
cd ..
