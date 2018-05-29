#!/usr/bin/env sh
TAG=$1
echo "Pushing tag $TAG"
docker push $TAG