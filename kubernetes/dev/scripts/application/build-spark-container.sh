#!/usr/bin/env sh
## Add connector jar
BASE_PATH=$1
TAG_SPARK_BASE=spark-base
TAG_SPARK_CLUSTER=spark:latest
TAG_ZEPPELIN=zeppelin:latest
## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000


$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-base "$TAG_SPARK_BASE" "$DOCKER_ID"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark "$TAG_SPARK_CLUSTER" "$DOCKER_ID"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/zeppelin "$TAG_ZEPPELIN" "$DOCKER_ID"