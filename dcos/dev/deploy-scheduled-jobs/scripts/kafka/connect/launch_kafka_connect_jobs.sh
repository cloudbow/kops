#!/bin/bash
topicName=$1
start=$2
stop=$3
zkHost=$4
retention=$5
standAloneConfig=$6
connectConfig=$7
logFile=$8
connectId=$9


echo "Params $topicName $start $stop $standAloneConfig $connectConfig $logFile"
SLEEP_TIME=10
TOTAL_WAIT_TIME=$((SLEEP_TIME*60))
SERVICE_ID="connect"
SERVICE_CONFIG_JQ=".tasks[0].state"
echo "Applying filter $SERVICE_CONFIG_JQ"
function breakOnStatus {
	COUNTER=0
	# Wait until tasks are in state defined by parameter , break otherwise
	while : ; do
		COUNTER=$((COUNTER+1))
		STATUS=`getValForProp $SERVICE_ID $SERVICE_CONFIG_JQ`
		echo "status after  $SLEEP_TIME sec : $STATUS"
		if [ "$STATUS" == "$2" ]; then
		   break
		fi
		if [ "$COUNTER" -ge "$TOTAL_WAIT_TIME" ]; then
		   echo "failed to get task status"
		   exit 1
		fi
		sleep $SLEEP_TIME
	done
}

function getValForProp {
	TEMPFILE=mktemp
	curl  $1 > $TEMPFILE
	STATUS=`cat $TEMPFILE  | jq -r "$2"`
	rm -rf $TEMPFILE
	echo $STATUS
}




function restartOnError() {
	INIT_STATUS=`getValForProp "http://connect.marathon.l4lb.thisdcos.directory:8083/connectors/$connectId/tasks" ".error_code"`
	if [ "$INIT_STATUS" == "404" ]
    then
    	echo "The kafka job $topicName is not running."
    	/deploy-scheduled-jobs/scripts/kafka/connect/connect-start.sh $connectConfig
    else
       echo "There is a status for this job"     
    fi
    STATUS=`getValForProp "http://connect.marathon.l4lb.thisdcos.directory:8083/connectors/$connectId/status" ".connector.state"`
	echo "Only checking current status -  $STATUS"
}

if [[ "$topicName" == *"live_info"* ]]
then
    restartOnError
else
   currUTCHour="$(date -u +%-H)"
   if [ $currUTCHour -ge $start ] && [ $currUTCHour -le $stop ]
   then
       restartOnError
   else
      STATUS=`getValForProp "http://connect.marathon.l4lb.thisdcos.directory:8083/connectors/$connectId/status" ".connector.state"`
      if [ -z "$STATUS" ]
      then
         echo "The kafka job $topicName is not running out of interval"
      else
         echo "Killing kafka job $topicName as it is out of time "
         curl -X DELETE http://connect.marathon.l4lb.thisdcos.directory:8083/connectors/$connectId
      fi
   fi
fi




