#!/usr/bin/env sh
BASE_PATH=$1
UPLOAD_PATH=$2
cd $BASE_PATH
cp -rf ../sports-cloud-dcos-schedulers .
cp -rf ../sports-cloud-parsers .
cp -rf ../micro-content-matcher .
cp -rf ../sports-cloud-rest-server .
sudo mkdir -p /var/log
sudo mkdir -p /var/log/sports-cloud/elastic
sudo mkdir -p /data/apps/sports-cloud/spark/log4j-config
sudo mkdir -p /data/apps/sports-cloud/artifacts
sudo mkdir -p /data/apps/sports-cloud/kafka/connect/libs
sudo mkdir -p /data/apps/sports-cloud/elastic/data
## 
### Create Kafka topics
$BASE_PATH/dev/deploy-scheduled-jobs/scripts/kafka/create-kafka-topics.sh    

# Create and copy executable jar to libs folder
cd $BASE_PATH/sports-cloud-dcos-schedulers
sbt clean assembly
mkdir $BASE_PATH/dev/deploy-scheduled-jobs/libs
cp target/scala-*/sports-cloud-dcos-schedulers-assembly-*.jar $BASE_PATH/dev/deploy-scheduled-jobs/libs/sports-cloud-dcos-schedulers-assembly.jar

echo "uploading jar file"
$BASE_PATH/dev/scripts/libs/zip-and-upload-artifacts.sh /tmp/scheduled-job-scripts.tar.gz slingtv/sports-cloud/scheduled-job-scripts.tar.gz $BASE_PATH/dev/deploy-scheduled-jobs


cd $BASE_PATH/sports-cloud-rest-server
mvn clean package
$BASE_PATH/dev/scripts/libs/upload-file.sh $BASE_PATH/sports-cloud-rest-server/target/sports-cloud-rest-server*.jar slingtv/sports-cloud/sports-cloud-rest-server.jar

cd $BASE_PATH/sports-cloud-parsers
sbt clean assembly
# denormalize the versions
cp target/scala-2.11/kafka-schedule-parser-assembly-*.jar /data/apps/sports-cloud/kafka/connect/libs/kafka-schedule-parser-assembly.jar

## At this point copy all the paths using ansible

echo "Upload path is $UPLOAD_PATH"
$BASE_PATH/dev/scripts/libs/upload-sbt-jar-file.sh "$BASE_PATH/micro-content-matcher" "2.11" "micro-container-matcher-assembly-*.jar" "$UPLOAD_PATH"




## Dcoker registry
# Make the following into non interactive
#openssl req -newkey rsa:4096 -nodes -sha256 -keyout domain.key -x509 -days 365 -out domain.crt
# Copy it to all the nodes
#cp genconf/serve/domain.crt /etc/docker/certs.d/registry.marathon.l4lb.thisdcos.directory:5000

