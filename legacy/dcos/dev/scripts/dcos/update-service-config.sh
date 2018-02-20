#!/usr/bin/env sh
# Run parent script
SLEEP_TIME=10
TOTAL_WAIT_TIME=$((SLEEP_TIME*60))
SERVICE_ID=$1
SERVICE_CONFIG_JQ=$2
echo "Applying filter $SERVICE_CONFIG_JQ"
function breakOnStatus {
	COUNTER=0
	# Wait until tasks are in state defined by parameter , break otherwise
	while : ; do
		COUNTER=$((COUNTER+1))
		dcos marathon app show $SERVICE_ID  > /tmp/$SERVICE_ID-run.json
		STATUS=`cat /tmp/$SERVICE_ID-run.json  | jq -r "$1"`
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
# Wait until task runs
breakOnStatus '.tasks[0].state' "TASK_RUNNING"
# Delete version from the app config
cat /tmp/$SERVICE_ID-run.json | jq 'del(.version)' | jq  "$SERVICE_CONFIG_JQ" > /tmp/$SERVICE_ID-temp.json

curl  -H "Content-Type:application/json" -H "Authorization: token=$(dcos config show core.dcos_acs_token)" -X PUT -d @/tmp/$SERVICE_ID-temp.json  http://m1.dcos/service/marathon/v2/apps/$SERVICE_ID\?force\=true\&partialUpdate\=false