#!/bin/sh
# eg: PlayerStatsDataMuncher
NAME=$1
# eg: com.slingmedia.sportscloud.offline.batch.impl.MetaDataMuncher
CLASS_NAME=$2
# eg: PLAYERSTATS meta_batch player_stats
ARGS=$3
# eg: http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/all-spark-jobs.jar
$JAR_FILE=$4

echo "Running job $NAME from jarfile $JAR_FILE for class $CLASS_NAME with args $ARGS "
/opt/spark/dist --submit-args="--name $NAME \
--master mesos://zk://master.mesos:2181/mesos
--class $CLASS_NAME \
--driver-memory 4G \
--executor-memory 4G \
--total-executor-cores 4 \
--conf spark.es.index.auto.create=false \
--conf spark.es.resource=sports-cloud/game_schedule  \
--conf spark.es.nodes=localhost  \
--conf spark.es.net.http.auth.user=elastic  \
--conf spark.es.net.http.auth.pass=changeme  \
--conf spark.es.write.operation=upsert  \
--conf spark.default.parallelism=4 \
--conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
--packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.1 \
$JAR_FILE $ARGS"