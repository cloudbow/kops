#!/bin/sh
echo "submitting config $1"
curl -X POST -H "Content-Type: application/json" --data @$1 http://connect.marathon.l4lb.thisdcos.directory:8083/connectors

