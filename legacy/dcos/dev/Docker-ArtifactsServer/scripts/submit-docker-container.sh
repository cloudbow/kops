#!/usr/bin/env sh
# Run parent script
$1/dev/scripts/docker/submit-docker-container.sh $2 $3
if [ -z "$4" ]; then
	echo "Doing no extra config"
else
	echo "Applying configuration"
	$1/dev/scripts/dcos/update-service-config.sh $2 "$4"
fi
