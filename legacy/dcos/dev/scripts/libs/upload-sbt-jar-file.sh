#!/usr/bin/env sh
BASE_PATH=$1
# eg: 2.12
SCALA_VERSION=$2
# eg: sports-cloud-dcos-schedulers-assembly-0.1.0.jar
JAR_FILE_NAME=$3
# eg: slingtv/sports-cloud/$JAR_FILE_NAME
UPLOAD_PATH=$4
echo "building from $BASE_PATH and uploading to path $UPLOAD_PATH"
cd $BASE_PATH
sbt clean assembly
echo "uploading file $BASE_PATH/target/scala-$SCALA_VERSION/$JAR_FILE_NAME to path $UPLOAD_PATH"
curl --upload-file $BASE_PATH/target/scala-$SCALA_VERSION/$JAR_FILE_NAME http://p1.dcos:9082/artifacts/$UPLOAD_PATH

curl -v -o /tmp/o.y  http://p1.dcos:9082/artifacts/$UPLOAD_PATH