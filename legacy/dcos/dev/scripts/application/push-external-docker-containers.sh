#!/usr/bin/env sh

# eg: confluentinc/cp-kafka-connect:3.3.0
ORIG_TAG=$1
# eg: registry.marathon.l4lb.thisdcos.directory:5000/cp-kafka-connect:3.3.0
NEW_TAG=$2
docker pull "$ORIG_TAG"
docker tag "$ORIG_TAG" "$NEW_TAG"
docker push "$NEW_TAG"