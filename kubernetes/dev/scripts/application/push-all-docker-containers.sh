#!/usr/bin/env sh
## All the tags for all the images
TAG_REST=sports-cloud-rest
TAG_JOB_SCHEDULER=sc-job-scheduler
TAG_ARTIFACT_SERVER=artifact-server
TAG_KAFKA_CONNECT=sc-cp-connect
TAG_SPARK_BASE=spark-base
TAG_SPARK_MASTER=spark-master
TAG_SPARK_WORKER=spark-worker
TAG_ZEPPELIN=zeppelin
TAG_SPARK_JOB=spark-job

## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000


BASE_PATH=$1


echo "Build REST Layer"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Rest "$TAG_REST" "$DOCKER_ID"

echo "Build artifact server"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ArtifactsServer "$TAG_ARTIFACT_SERVER" "$DOCKER_ID"

echo "Build kafka connect"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ConfluentConnect "$TAG_KAFKA_CONNECT" "$DOCKER_ID"

echo "Building spark cluster"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-base "$TAG_SPARK_BASE" "$DOCKER_ID"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-master "$TAG_SPARK_MASTER" "$DOCKER_ID"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/spark-worker "$TAG_SPARK_WORKER" "$DOCKER_ID"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-SparkJob "$TAG_SPARK_JOB" "$DOCKER_ID"

echo "Building zeppelin"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Spark/zeppelin "$TAG_ZEPPELIN" "$DOCKER_ID"

echo "Building job scheduler"
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ScheduledJob "$TAG_JOB_SCHEDULER" "$DOCKER_ID"



echo "Pushing rest layer"
$BASE_PATH/scripts/docker/push-docker-container.sh $DOCKER_ID/$TAG_REST
echo "Pushing scheduled job"
$BASE_PATH/scripts/docker/push-docker-container.sh  $DOCKER_ID/$TAG_JOB_SCHEDULER
echo "Pusing artifact server"
$BASE_PATH/scripts/docker/push-docker-container.sh  $DOCKER_ID/$TAG_ARTIFACT_SERVER
echo "Pushing kafka connect jobs"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_KAFKA_CONNECT
echo "Getting spark base"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_BASE
echo "PUshing spark master docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_MASTER
echo "PUshing spark worker docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_WORKER
echo "Pushing zeppelin docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_ZEPPELIN
echo "Pushing spark job docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_JOB
