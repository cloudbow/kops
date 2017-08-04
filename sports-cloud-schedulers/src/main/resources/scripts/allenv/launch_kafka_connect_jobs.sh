#!/bin/sh
standAloneConfig=$1
connectConfig=$2
logFile=$3

echo "Params $standAloneConfig $connectConfig $logFile"

runCheck="$(fuser ${logFile})"
echo "Is it running $runCheck"
errorCheck="$(grep 'java.net.ConnectException: Connection refused' ${logFile})"
echo "Is connect exception there? $errorCheck"

if [ "$runCheck" == "" ]
then
    echo "The kafka job is not running"
    echo "Creating kafka instance"
    "$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
fi


if [ "$errorCheck" != "" ]
then
    echo "The kafka job is stuck.Restarting it"
    "$(fuser -k  ${logFile})"
	echo "Creating kafka instance"
    "$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
fi
