#!/usr/bin/env sh
BASE_DIR=$1
TAG=$2
DOCKER_HUB_USER=$3
echo "BASEDIR IS $BASE_DIR and tag is $TAG"
cd $BASE_DIR
docker build --rm --tag=$DOCKER_HUB_USER/$TAG .