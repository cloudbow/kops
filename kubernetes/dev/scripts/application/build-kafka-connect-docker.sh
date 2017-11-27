#!/usr/bin/env sh
## Add connector jar
BASE_PATH=$1
TAG_KAFKA_CONNECT=sc-cp-connect
## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000



## Add the sports cloud parsers
cd /tmp
cp -rf $BASE_PATH/../../sports-cloud-parsers  .
cd sports-cloud-parsers
### Add the eneco ftp jar to maven
$MAVEN_HOME/bin/mvn install:install-file -DgroupId=com.eneco  \
-DartifactId=kafka-connect-ftp  \
-Dversion=0.0.0-unspecified  \
-Dfile=/tmp/sports-cloud-parsers/libs/kafka-connect-ftp-0.1.7-8-kafka-0.10.2.0.jar  \
-Dpackaging=jar \
-DgeneratePom=true

## Build and generate the jar and copy to the image
sbt clean assembly
mkdir -p $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars
cp target/scala-2.11/kafka-schedule-parser-assembly-*.jar $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars/kafka-schedule-parser-assembly.jar


cd $BASE_PATH/docker/containers/Docker-ConfluentConnect
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ConfluentConnect "$TAG_KAFKA_CONNECT" "$DOCKER_ID"