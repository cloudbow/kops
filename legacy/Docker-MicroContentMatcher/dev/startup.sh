#!/usr/bin/env sh
# Starting confluent
$CONFLUENT_HOME/bin/confluent start zookeeper
$CONFLUENT_HOME/bin/confluent start kafka
CONFLUENT_BASE=/confluent-base
LOG_DIR=/var/log/kafka-connect-log
BASE_DIR=/project

# Create plugins dir to copy all connect codes
PLUGINS_DIR=/usr/local/share/kafka/plugins
# Create plugins directory
mkdir -p $PLUGINS_DIR
mkdir -p $LOG_DIR

chown -R elasticsearch:elasticsearch /var/log/elasticsearch && \
chown -R elasticsearch:elasticsearch /data/elastic && \
chown -R elasticsearch:elasticsearch /elastic
# Install eneco in local m2 repo
$MAVEN_HOME/bin/mvn install:install-file -DgroupId=com.eneco  \
-DartifactId=kafka-connect-ftp  \
-Dversion=0.0.0-unspecified  \
-Dfile=$BASE_DIR/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar  \
-Dpackaging=jar \
-DgeneratePom=true

# copy kafka eneco jar to kafka libs
cp $BASE_DIR/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar $PLUGINS_DIR
cp $BASE_DIR/sports-cloud-parsers/libs/scala-xml_2.11-1.0.2.jar $PLUGINS_DIR

# building and starting kafka connect
cd $BASE_DIR/sports-cloud-parsers
$SBT_HOME/bin/sbt clean assembly
cp $BASE_DIR/sports-cloud-parsers/target/scala-2.11/kafka-schedule-parser-assembly-0.1.0.jar $PLUGINS_DIR
mkdir /var/log/sports-cloud-kafka-jobs


DEFAULT_REPLICATION_FACTOR=1
MINIMUM_NUM_PARTITIONS=1
# config.storage.topic=connect-configs
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-configs --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-configs --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-configs --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --config cleanup.policy=compact

# offset.storage.topic=connect-offsets
OFFSET_PARTITIONS=50
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-offsets --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $OFFSET_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-offsets --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $OFFSET_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-offsets --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $OFFSET_PARTITIONS --config cleanup.policy=compact

# status.storage.topic=connect-status
TOPIC_PARTITIONS=10
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-cm-status --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $TOPIC_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-live-info-status --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $TOPIC_PARTITIONS --config cleanup.policy=compact
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --topic connect-meta-batch-status --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $TOPIC_PARTITIONS --config cleanup.policy=compact


## Start worker for content match
nohup $CONFLUENT_HOME/bin/connect-distributed $BASE_DIR/sports-cloud-parsers/src/main/resources/kafka-workers/cs-content-match.properties &>$LOG_DIR/content_match.log &
## Start worker for meta batch
nohup $CONFLUENT_HOME/bin/connect-distributed $BASE_DIR/sports-cloud-parsers/src/main/resources/kafka-workers/cs-live-info.properties &>$LOG_DIR/live_info.log &
## Start worker for live info
nohup $CONFLUENT_HOME/bin/connect-distributed $BASE_DIR/sports-cloud-parsers/src/main/resources/kafka-workers/cs-meta-batch.properties &>$LOG_DIR/meta_batch.log &




# Create topic mlb_meta
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --topic content_match
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --topic live_info
$CONFLUENT_HOME/bin/kafka-topics --create --zookeeper localhost:2181 --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS --topic meta_batch

# Add retention policy topic mlb meta
# Setting up for 9hrs
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic content_match --config retention.ms=10800000
# Retain live data for 2.5 hrs
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic live_info --config retention.ms=9000000
# Retain for 4hrs
$CONFLUENT_HOME/bin/kafka-topics --zookeeper localhost:2181 --alter --topic meta_batch --config retention.ms=10800000



# Kafka connect jobs started from server shell scripts

mkdir -p /var/log/sports-cloud-streaming-jobs
mkdir -p /var/log/sports-cloud-batch-jobs
# Building content matcher offline batch job
cd $BASE_DIR/micro-content-matcher
$SBT_HOME/bin/sbt clean assembly 

# start rest layer also
cd $BASE_DIR/sports-cloud-rest-server
$MAVEN_HOME/bin/mvn package -DskipTests
nohup java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=localhost -DindexingPort=9200 -jar target/sports-cloud-rest-server.jar localhost 9080  &> /var/log/sports-cloud-rest.log &

# Building sports cloud scheduler
cd $BASE_DIR/sports-cloud-schedulers
$SBT_HOME/bin/sbt clean assembly 
java -DsparkHomeLoc=$SPARK_HOME -DzkHost=localhost:2181 -DsparkExtraJars=$BASE_DIR/micro-content-matcher/non-transitive/spark-solr-3.0.2.jar  -DcmsSummaryUrl=cms/publish3/domain/summary/1.json -DcmsHost=cbd46b77 -DsportsCloudBatchJarLoc=$BASE_DIR/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar -jar $BASE_DIR/sports-cloud-schedulers/target/scala-2.12/sports-cloud-schedulers-assembly-0.1.0.jar  ncaaf