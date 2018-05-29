#!/usr/bin/env sh
BASE_PATH=$1
DOCKER_ID=registry.sports-cloud.com:5000
TAG_BUILDER=builder-docker

$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Builder "$TAG_BUILDER" "$DOCKER_ID"
$BASE_PATH/scripts/docker/push-docker-container.sh  $DOCKER_ID/$TAG_BUILDER