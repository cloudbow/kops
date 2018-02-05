#!/bin/sh
echo "All args : $@"
scala -classpath /deploy-scheduled-jobs/libs/kafka-schedule-rest-parsers-assembly.jar $2 $3