#!/usr/bin/env sh
curl -X POST  -H "Content-Type:application/json" --data @$1 http://m1.dcos:8080/v2/groups