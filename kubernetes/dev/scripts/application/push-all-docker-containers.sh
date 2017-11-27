#!/usr/bin/env sh
## All the tags for all the images
TAG_REST=sports-cloud-rest
TAG_JOB_SCHEDULER=sc-job-scheduler
TAG_ARTIFACT_SERVER=artifact-server
TAG_KAFKA_CONNECT=sc-cp-connect
TAG_SPARK_BASE=spark-base
TAG_SPARK_CLUSTER=spark:latest
TAG_ZEPPELIN=zeppelin:latest
TAG_SPARK_JOB=spark-job

## The real docker id to push to
DOCKER_ID=registry.sports-cloud.com:5000


BASE_PATH=$1

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
echo "PUshing spark docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_CLUSTER
echo "Pushing zeppelin docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_ZEPPELIN
echo "Pushing spark job docker"
$BASE_PATH/scripts/docker/push-docker-container.sh	$DOCKER_ID/$TAG_SPARK_JOB
