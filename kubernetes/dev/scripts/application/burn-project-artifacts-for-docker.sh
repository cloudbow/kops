#!/usr/bin/env sh
BASE_PATH=$1
DOCKER_ID=registry.sports-cloud.com:5000
TAG_REST=sports-cloud-rest


## START Build rest artifacts ##

cd /tmp
cp -rf $BASE_PATH/../../sports-cloud-rest-server .
cd sports-cloud-rest-server
mvn clean package
echo "Creating the jars directory"
mkdir -p $BASE_PATH/docker/containers/Docker-Rest/jars
echo "Copying to jars directory"
cp /tmp/sports-cloud-rest-server/target/sports-cloud-rest-server*.jar $BASE_PATH/docker/containers/Docker-Rest/jars/sports-cloud-rest-server.jar

## END Build rest artifacts ##

## START Build artifact server ## 
## No extra dynamic project attributes

## END Build artifact serer

## START Kafka connect build artifacts ##
TAG_KAFKA_CONNECT=sc-cp-connect

### Add the sports cloud parsers
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

### Build and generate the jar and copy to the image
sbt clean assembly
mkdir -p $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars
cp target/scala-2.11/kafka-schedule-parser-assembly-*.jar $BASE_PATH/docker/containers/Docker-ConfluentConnect/jars/kafka-schedule-parser-assembly.jar

## END kafka connect build artifacts

## START Spark worker jars ###
### Add the sports cloud contentmatcher jar
cd /tmp
cp -rf $BASE_PATH/../../micro-content-matcher  .
cd micro-content-matcher
sbt clean assembly
mkdir -p $BASE_PATH/docker/containers/Docker-Spark/spark-worker/jars
cp /tmp/micro-content-matcher/target/scala-*/micro-container-*.jar $BASE_PATH/docker/containers/Docker-Spark/spark-worker/jars
## END Spark worker jars ###


## START ScheduledJob build artifacts ##
### Add the sports cloud parsers
cd /tmp
cp -rf $BASE_PATH/../../sports-cloud-k8s-schedulers  .
cd sports-cloud-k8s-schedulers
sbt clean assembly
### Add the jar to the docker image
mkdir -p $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs
cp target/scala-*/sports-cloud-*-schedulers-assembly-*.jar $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs/sports-cloud-schedulers-assembly.jar
## END ScheduledJob build artifacts ##


