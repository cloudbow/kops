#!/bin/sh
curl -o /tmp/scheduled-job-scripts.tar.gz http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/scheduled-job-scripts.tar.gz
cd /tmp
tar -xvf scheduled-job-scripts.tar.gz
cp -r /tmp/deploy-scheduled-jobs/* /deploy-scheduled-jobs
JOB_ID=$1
JOB_SCRIPT="/deploy-scheduled-jobs/scripts/execute-$1.sh $@"
eval "$JOB_SCRIPT"
sleep 180;
