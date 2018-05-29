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
    echo "Only checking current status"
     
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
      runCheck=
      if [ "$runCheck" == "" ]
      then
         echo "The kafka job $topicName is not running out of interval"
      else
         echo "Killing kafka job $topicName as it is out of time "
         
      fi
   fi
fi




