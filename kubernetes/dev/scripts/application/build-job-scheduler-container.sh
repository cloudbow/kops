#!/usr/bin/env sh
BASE_PATH=$1
DOCKER_ID=registry.sports-cloud.com:5000
TAG_JOB_SCHEDULER=sc-job-scheduler

## Add the sports cloud parsers
cd /tmp
cp -rf $BASE_PATH/../../sports-cloud-k8s-schedulers  .
cd sports-cloud-k8s-schedulers
sbt clean assembly
### Add the jar to the docker image
mkdir -p $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs
cp target/scala-*/sports-cloud-*-schedulers-assembly-*.jar $BASE_PATH/docker/containers/Docker-ScheduledJob/deploy-scheduled-jobs/libs/sports-cloud-schedulers-assembly.jar

$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ScheduledJob "$TAG_JOB_SCHEDULER" "$DOCKER_ID"