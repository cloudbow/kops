#!/usr/bin/env sh
## Add connector jar
BASE_PATH=$1
TAG_SPARK_JOB=spark-job
## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000
## Add the sports cloud parsers
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-SparkJob "$TAG_SPARK_JOB" "$DOCKER_ID"

SPORTS_CLOUD_BATCH_JOB_JAR='all-spark-jobs.jar'
SPORTS_CLOUD_BATCH_JOB_ARTIFACT="slingtv/sports-cloud/$SPORTS_CLOUD_BATCH_JOB_JAR"
cd /tmp
cp -rf $BASE_PATH/../../micro-content-matcher .	
$BASE_PATH/scripts/libs/upload-sbt-jar-file.sh '/tmp/micro-content-matcher' '2.11' 'micro-container-matcher-assembly-*.jar' 'artifact-server.sports-cloud.com' "$SPORTS_CLOUD_BATCH_JOB_ARTIFACT"
