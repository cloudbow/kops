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
$KAFKA_HOME/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic mlb_meta
# Add retention policy topic mlb meta
$KAFKA_HOME/bin/kafka-topics.sh --zookeeper localhost:2181 --alter --topic mlb_meta --config retention.ms=8640000
# building and starting kafka connect
cd /project/sports-cloud-parsers
$SBT_HOME/bin/sbt assembly
cp -f target/scala-2.11/kafka-schedule-parser-assembly-0.1.0.jar $KAFKA_HOME/libs
nohup $KAFKA_HOME/bin/connect-standalone.sh $KAFKA_HOME/config/connect-standalone.properties /project/sports-cloud-parsers/src/main/resources/kafka-connect.properties &> /var/log/kafka-connect.log &
# Building content matcher offline batch job
cd /project/micro-content-matcher
$SBT_HOME/bin/sbt assembly 
# Building sports cloud scheduler
cd /project/sports-cloud-schedulers
$SBT_HOME/bin/sbt assembly 
java -DsparkHomeLoc=$SPARK_HOME -DcmsHost=cbd46b77 -DsportsCloudBatchJarLoc=/project/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar -jar target/scala-2.12/sports-cloud-schedulers-assembly-0.1.0.jar 