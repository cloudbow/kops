#!/usr/bin/env sh
dcos marathon app remove $3
sleep 20;
dcos package install --options=$2/config/$3.json  --yes $4
if [ -z "$5" ]; then
	echo "Doing no extra config"
else
	echo "Applying configuration"
	$1/scripts/dcos/update-service-config.sh $3 "$5"
fi