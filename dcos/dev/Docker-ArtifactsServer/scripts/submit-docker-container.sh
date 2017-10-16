#!/usr/bin/env sh
# Run parent script
$1/dev/scripts/submit-docker-container.sh $2 $3
$1/dev/scripts/update-service-config.sh $2 "$4"