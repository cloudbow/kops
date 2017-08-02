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


# Create topic mlb_meta
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic content_match
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic live_info
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic meta_batch

# Add retention policy topic mlb meta
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic content_match --config retention.ms=8640000
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic live_info --config retention.ms=8640000
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic meta_batch --config retention.ms=8640000

# building and starting kafka connect
cd /project/sports-cloud-parsers
$SBT_HOME/bin/sbt clean assembly
cp -f target/scala-2.11/kafka-schedule-parser-assembly-0.1.0.jar $KAFKA_HOME/libs
mkdir /var/log/sports-cloud-kafka-jobs
nohup $KAFKA_HOME/bin/connect-standalone.sh /project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-content-match.properties /project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-connect-content-match.properties &> /var/log/sports-cloud-kafka-jobs/cs-content-match-kafka-connect.log &
nohup $KAFKA_HOME/bin/connect-standalone.sh /project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-meta-batch.properties /project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-meta-batch.properties  &> /var/log/sports-cloud-kafka-jobs/cs-meta-batch-kafka-connect.log &
nohup $KAFKA_HOME/bin/connect-standalone.sh /project/sports-cloud-parsers/src/main/resources/kafka-standalone/cs-live-info.properties /project/sports-cloud-parsers/src/main/resources/kafka-connect/ftp-live-scores.properties  &> /var/log/sports-cloud-kafka-jobs/cs-live-info-kafka-connect.log &

# Start solr 
$SOLR_HOME/bin/solr start -cloud -p 8983 -s "/data/solr/cloud/node1/solr" -force
$SOLR_HOME/bin/solr create -c game_schedule  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c live_info  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c team_standings  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c player_stats  -d data_driven_schema_configs -force
$SOLR_HOME/bin/solr create -c scoring_plays  -d data_driven_schema_configs -force


# Building content matcher offline batch job
cd /project/micro-content-matcher
$SBT_HOME/bin/sbt clean assembly 
# Building sports cloud scheduler
cd /project/sports-cloud-schedulers
$SBT_HOME/bin/sbt clean assembly 
java -DsparkHomeLoc=$SPARK_HOME -DsparkExtraJars=/project/micro-content-matcher/non-transitive/spark-solr-3.0.2.jar  -DcmsHost=cbd46b77 -DsportsCloudBatchJarLoc=/project/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar -jar target/scala-2.12/sports-cloud-schedulers-assembly-0.1.0.jar 