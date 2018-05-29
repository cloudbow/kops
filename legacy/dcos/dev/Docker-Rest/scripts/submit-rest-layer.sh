#!/usr/bin/env sh
CONFIG=$2
NAME=$1
dcos marathon app remove $NAME
dcos marathon app add $CONFIG