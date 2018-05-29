#!/bin/sh
JOB_ID=$1
JOB_SCRIPT="/deploy-scheduled-jobs/scripts/execute-$1.sh $@"
eval "$JOB_SCRIPT"