#!/bin/sh
echo "All args : $@"
scala -classpath /deploy-scheduled-jobs/libs/sports-cloud-schedulers-assembly.jar $2 $3