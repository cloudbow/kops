#!/usr/bin/env sh
BASE_DIR=$1
TAG=$2
echo "version= $TAG"
cd $BASE_DIR
docker push $TAG