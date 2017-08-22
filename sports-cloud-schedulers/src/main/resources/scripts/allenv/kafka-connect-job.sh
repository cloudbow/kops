#!/bin/sh
nohup $KAFKA_HOME/bin/connect-standalone.sh $1 $2 >$3 2>&1 &
