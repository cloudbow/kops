#!/bin/sh

ps -aef | grep "com.slingmedia.sportscloud.offline.streaming.impl.LiveDataMuncher" | grep -v grep > /dev/null

if [ $? != 0 ]
then
	echo "Submitting LiveDataMuncher job"
	$SPARK_HOME/bin/spark-submit --name LiveDataMucher --class com.slingmedia.sportscloud.offline.streaming.impl.LiveDataMuncher --master local[8] --driver-java-options -Dlog4j.configuration=file:/spark-log4j-config/log4j-driver.properties --driver-memory 7G --executor-memory 7G --total-executor-cores 4 --conf spark.executor.extraJavaOptions=-Dlog4j.configuration=file:/spark-log4j-config/log4j-executor.properties --packages org.apache.spark:spark-sql-kafka-0-10_2.11:2.1.1 --jars /project/micro-content-matcher/non-transitive/spark-solr-3.0.2.jar /project/micro-content-matcher/target/scala-2.11/micro-container-matcher-assembly-0.1.0.jar live_info live_info localhost:9983  >/var/log/sports-cloud-streaming-jobs/sc-live-stream-job.log 2>&1
else
	echo "LiveDataMuncher job already running"
fi