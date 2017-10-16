#!/usr/bin/env sh
BASE_DIR=$1
dcos package uninstall spark --yes --all
dcos package uninstall confluent-kafka --yes --all
dcos package uninstall elastic --yes --all
sleep 40s
dcos package install --options=$BASE_DIR/config/confluent.json  --yes confluent-kafka
sleep 1m
dcos package install --options=$BASE_DIR/config/spark.json  --yes spark
sleep 1m
dcos package install --options=$BASE_DIR/config/elastic.json  --yes elastic
sleep 1m
dcos package install --options=$BASE_DIR/config/connect.json  --yes confluent-connect

