#!/bin/bash
echo "All args : $@"
SLEEP_TIME=$1
while true; do 
    echo "waked up and doing logrotate"
	logrotate /config/logrotate/rotate.conf
	echo "done and seleeping now"
	sleep $SLEEP_TIME
done
