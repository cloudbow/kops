#!/usr/bin/env sh
dcos marathon app remove $1
sleep 20;
dcos marathon app add $2