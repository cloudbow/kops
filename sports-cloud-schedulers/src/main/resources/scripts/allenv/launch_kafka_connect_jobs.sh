#!/bin/bash
topicName=$1
start=$2
stop=$3
zkHost=$4
retention=$5
standAloneConfig=$6
connectConfig=$7
logFile=$8



echo "Params $topicName $start $stop $standAloneConfig $connectConfig $logFile"





function restartOnError() {
    runCheck="$(fuser ${logFile})"
    echo "Is it running $runCheck"
    errorCheck0="$(grep 'java.net.ConnectException: Connection refused' ${logFile})"
    errorCheck1="$(grep 'org.apache.commons.net.io.CopyStreamException: IOException caught while copying' ${logFile})"
    errorCheck2="$(grep 'java.lang.OutOfMemoryError: Java heap space' ${logFile})"
    errorCheck3="$(grep 'java.net.NoRouteToHostException: No route to host' ${logFile})"
    if [ "$runCheck" == "" ]
    then
        echo "The kafka job $topicName is not running"
        echo "Creating kafka instance for $topicName"
        kr="$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
	echo "return code $kr"
    fi
    
    
    if [ "$errorCheck0" != "" ] || [ "$errorCheck1" != "" ] || [ "$errorCheck2" != "" ] || [ "$errorCheck3" != "" ]
    then
        echo "The kafka job $topicName is stuck.Restarting it"
        "$(fuser -k  ${logFile})"
        echo "Creating kafka instance for $topicName"
        kr="$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
	echo "Command exited with code $kr"
    fi
}

if [ "$topicName" == "live_info" ]
then
    restartOnError
else
   currUTCHour="$(date -u +%-H)"
   if [ $currUTCHour -ge $start ] && [ $currUTCHour -le $stop ]
   then
       restartOnError
   else
      runCheck="$(fuser ${logFile})"
      if [ "$runCheck" == "" ]
      then
         echo "The kafka job $topicName is not running out of interval"
      else
         echo "Killing kafka job $topicName as it is out of time "
         ret="$(kill -9 ${runCheck})"
	 ret1="$(${KAFKA_HOME}/bin/kafka-topics.sh --zookeeper ${zkHost} --alter --topic ${topicName}  --config retention.ms=${retention})"
      fi
   fi
fi




