#!/bin/sh
echo "submitting config $1"
curl -X POST -H "Content-Type: application/json" --data @$1 http://localhost:8083/connectors