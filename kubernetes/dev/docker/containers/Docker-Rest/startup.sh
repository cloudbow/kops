#!/usr/bin/env sh
# Starting kafka zookeeper
# start rest layer also
java ${JAVA_OPTS}  -jar /jars/sports-cloud-rest-server.jar localhost 9080 
