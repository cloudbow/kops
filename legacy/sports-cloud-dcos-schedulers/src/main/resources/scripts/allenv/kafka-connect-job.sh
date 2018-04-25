#!/bin/sh
curl -X POST -H "Content-Type: application/json" --data $1 http://connect.marathon.l4lb.thisdcos.directory:8083/connectors
