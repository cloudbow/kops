#!/usr/bin/env sh
BASE_PATH=$1
DOCKER_ID=registry.sports-cloud.com:5000
TAG_REST=sports-cloud-rest


cd /tmp
cp -rf $BASE_PATH/../../sports-cloud-rest-server .
cd sports-cloud-rest-server
mvn clean package
echo "Creating the jars directory"
mkdir -p $BASE_PATH/docker/containers/Docker-Rest/jars
echo "Copying to jars directory"
cp /tmp/sports-cloud-rest-server/target/sports-cloud-rest-server*.jar $BASE_PATH/docker/containers/Docker-Rest/jars/sports-cloud-rest-server.jar

cd $BASE_PATH/docker/containers/Docker-Rest
$BASE_PATH/scripts/docker/build-docker-container.sh  $BASE_PATH/docker/containers/Docker-Rest "$TAG_REST" "$DOCKER_ID"