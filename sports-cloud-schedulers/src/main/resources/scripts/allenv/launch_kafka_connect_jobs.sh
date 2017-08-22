#!/bin/sh
standAloneConfig=$1
connectConfig=$2
logFile=$3

echo "Params $standAloneConfig $connectConfig $logFile"

runCheck="$(fuser ${logFile})"
echo "Is it running $runCheck"
errorCheck="$(grep 'java.net.ConnectException: Connection refused' ${logFile})"
errorCheck1="$(grep 'org.apache.commons.net.io.CopyStreamException: IOException caught while copying' ${logFile})"
errorCheck2="$(grep 'java.lang.OutOfMemoryError: Java heap space' ${logFile})"

echo "Is connect exception there? $errorCheck"

if [ "$runCheck" == "" ]
then
    echo "The kafka job is not running"
    echo "Creating kafka instance"
    "$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
fi


if [ "$errorCheck" != "" ] || [ "$errorCheck1" != "" ] || [ "$errorCheck2" != "" ]
then
    echo "The kafka job is stuck.Restarting it"
    "$(fuser -k  ${logFile})"
	echo "Creating kafka instance"
    "$(/project/sports-cloud-schedulers/src/main/resources/scripts/allenv/kafka-connect-job.sh ${standAloneConfig} ${connectConfig} ${logFile})"
fi
