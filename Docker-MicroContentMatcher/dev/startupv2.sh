#!/usr/bin/env sh
# Starting confluent
$CONFLUENT_HOME/bin/confluent start zookeeper
$CONFLUENT_HOME/bin/confluent start kafka
CONFLUENT_BASE=/confluent-base


# Create plugins dir to copy all connect codes
PLUGINS_DIR=/usr/local/share/kafka/plugins/kafka-connect-ftp
# Create plugins directory
mkdir -p $PLUGIN_DIR
# Install eneco in local m2 repo
$MAVEN_HOME/bin/mvn install:install-file -DgroupId=com.eneco  \
-DartifactId=kafka-connect-ftp  \
-Dversion=0.0.0-unspecified  \
-Dfile=/project/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar  \
-Dpackaging=jar \
-DgeneratePom=true

# copy kafka eneco jar to kafka libs
cp /project/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar $PLUGINS_DIR
cp /project/sports-cloud-parsers/libs/scala-xml_2.11-1.0.2.jar $KAFKA_HOME/libs $PLUGINS_DIR



# config.storage.topic=connect-configs
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-configs --replication-factor 3 --partitions 1 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-configs --replication-factor 3 --partitions 1 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-configs --replication-factor 3 --partitions 1 --config cleanup.policy=compact

# offset.storage.topic=connect-offsets
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-offsets --replication-factor 3 --partitions 50 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-offsets --replication-factor 3 --partitions 50 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-offsets --replication-factor 3 --partitions 50 --config cleanup.policy=compact

# status.storage.topic=connect-status
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-status --replication-factor 3 --partitions 10 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-status --replication-factor 3 --partitions 10 --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-status --replication-factor 3 --partitions 10 --config cleanup.policy=compact


## Start worker for content match
$CONFLUENT_HOME/bin/connect-distributed /project/sports-cloud-parsers/src/main/resources/kafka-workers/cs-content-match.properties
## Start worker for meta batch
$CONFLUENT_HOME/bin/connect-distributed /project/sports-cloud-parsers/src/main/resources/kafka-workers/cs-live-info.properties
## Start worker for live info
$CONFLUENT_HOME/bin/connect-distributed /project/sports-cloud-parsers/src/main/resources/kafka-workers/cs-meta-batch.properties




# Create topic mlb_meta
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic content_match
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic live_info
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic meta_batch

# Add retention policy topic mlb meta
# Setting up for 9hrs
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic content_match --config retention.ms=36000000
# Retain live data for 30min
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic live_info --config retention.ms=9000000
# Retain for 9hrs
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic meta_batch --config retention.ms=36000000

# Start content match tasks
curl -X POST -H "Content-Type: application/json" --data @/project/sports-cloud-parsers/src/main/resources/kafka-connector-tasks/ftp-connect-content-match.json http://localhost:8083/connectors
# Start live info task
curl -X POST -H "Content-Type: application/json" --data @/project/sports-cloud-parsers/src/main/resources/kafka-connector-tasks/ftp-live-scores.json http://localhost:8083/connectors
# Start meta batch task
curl -X POST -H "Content-Type: application/json" --data @/project/sports-cloud-parsers/src/main/resources/kafka-connector-tasks/ftp-meta-batch.json http://localhost:8083/connectors


# building and starting kafka connect
cd /project/sports-cloud-parsers
$SBT_HOME/bin/sbt clean package
cp /project/sports-cloud-parsers/target/scala-2.11/kafka-schedule-parser_2.11-0.1.0.jar $PLUGINS_DIR
mkdir /var/log/sports-cloud-kafka-jobs

# Kafka connect jobs started from server shell scripts

mkdir -p /var/log/sports-cloud-streaming-jobs
mkdir -p /var/log/sports-cloud-batch-jobs
# Building content matcher offline batch job
cd /project/micro-content-matcher
$SBT_HOME/bin/sbt clean assembly 

chown -R elasticsearch:elasticsearch /var/log/elasticsearch
chown -R elasticsearch:elasticsearch /data/elastic
chown -R elasticsearch:elasticsearch /elastic

# start rest layer also
cd /project/sports-cloud-rest-server
$MAVEN_HOME/bin/mvn package -DskipTests
nohup java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=localhost -DindexingPort=9200 -jar target/sports-cloud-rest-server.jar localhost 9080  &> /var/log/sports-cloud-rest.log &

# Building sports cloud scheduler
cd /project/sports-cloud-schedulers
$SBT_HOME/bin/sbt clean assembly 
java -DsparkHomeLoc=$SPARK_HOME -DzkHost=localhost:2181 -DsparkExtraJars=/project/micro-content-matcher/non-transitive/spark-solr-3.0.2.jar  -DcmsSummaryUrl=cms/publish3/domain/summary/1.json -DcmsHost=cbd46b77 -DsportsCloudBatchJarLoc=/project/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar -jar /project/sports-cloud-schedulers/target/scala-2.12/sports-cloud-schedulers-assembly-0.1.0.jar 