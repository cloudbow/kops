#!/bin/sh
for var in "$@"
do
    jobStatus=$(curl "http://metronome.mesos:9000/v1/jobs/$var/runs" | jq .[0].tasks[0].status -r)
    echo $jobStatus

    if [[ $jobStatus != "TASK_RUNNING" ]]; then
        echo $(curl -XPOST "http://metronome.mesos:9000/v1/jobs/$var/runs")
    fi
done
