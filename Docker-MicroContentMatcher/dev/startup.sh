#!/usr/bin/env sh
# Starting kafka zookeeper
nohup $KAFKA_HOME/bin/zookeeper-server-start.sh $KAFKA_HOME/config/zookeeper.properties &> /var/log/kafka-zookeeper.log &
# Starting kafka server
nohup $KAFKA_HOME/bin/kafka-server-start.sh $KAFKA_HOME/config/server.properties &> /var/log/kafka.log &
sleep 20s
# Install eneco in local m2 repo
$MAVEN_HOME/bin/mvn install:install-file -DgroupId=com.eneco  \
-DartifactId=kafka-connect-ftp  \
-Dversion=0.0.0-unspecified  \
-Dfile=/project/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar  \
-Dpackaging=jar \
-DgeneratePom=true

# copy kafka eneco jar to kafka libs
cp /project/sports-cloud-parsers/libs/kafka-connect-ftp-0.0.0-unspecified-jar-with-dependencies.jar $KAFKA_HOME/libs
cp /project/sports-cloud-parsers/libs/scala-xml_2.11-1.0.2.jar $KAFKA_HOME/libs

# Create topic mlb_meta
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic content_match
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic live_info
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic meta_batch

# Add retention policy topic mlb meta
# Setting up for 9hrs
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic content_match --config retention.ms=36000000
# Retain live data for 30min
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic live_info --config retention.ms=9000000
# Retain for 9hrs
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic meta_batch --config retention.ms=36000000

# building and starting kafka connect
cd /project/sports-cloud-parsers
$SBT_HOME/bin/sbt clean package
cp /project/sports-cloud-parsers/target/scala-2.11/kafka-schedule-parser_2.11-0.1.0.jar $KAFKA_HOME/libs
mkdir /var/log/sports-cloud-kafka-jobs

# Kafka connect jobs started from server shell scripts

mkdir -p /var/log/sports-cloud-streaming-jobs
mkdir -p /var/log/sports-cloud-batch-jobs
# Building content matcher offline batch job
cd /project/micro-content-matcher
$SBT_HOME/bin/sbt clean assembly 

# Start solr 
$SOLR_HOME/bin/solr start -cloud -p 8983 -s "/data/solr/cloud/node1/solr" -force
$SOLR_HOME/bin/solr create -c game_schedule  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c live_info  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c team_standings  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c player_stats  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c scoring_events  -d data_driven_schema_configs -force

# Building sports cloud scheduler
cd /project/sports-cloud-schedulers
$SBT_HOME/bin/sbt clean assembly 
java -DsparkHomeLoc=$SPARK_HOME -DzkHost=localhost:2181 -DsparkExtraJars=/project/micro-content-matcher/non-transitive/spark-solr-3.0.2.jar  -DcmsSummaryUrl=cms/publish3/domain/summary/1.json -DcmsHost=cbd46b77 -DsportsCloudBatchJarLoc=/project/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar -jar /project/sports-cloud-schedulers/target/scala-2.12/sports-cloud-schedulers-assembly-0.1.0.jar 