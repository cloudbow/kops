#!/usr/bin/env sh
# Starting kafka zookeeper
# start rest layer also
java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=elasticsearch.default.svc.cluster.local -DindexingPort=9200 -jar /jars/sports-cloud-rest-server.jar localhost 9080 
