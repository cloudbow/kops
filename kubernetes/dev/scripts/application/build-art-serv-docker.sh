#!/usr/bin/env sh
BASE_PATH=$1
DOCKER_ID=registry.sports-cloud.com:5000
TAG_ARTIFACT_SERVER=artifact-server

$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-ArtifactsServer "$TAG_ARTIFACT_SERVER" "$DOCKER_ID"