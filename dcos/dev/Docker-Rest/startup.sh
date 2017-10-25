#!/usr/bin/env sh
# Starting kafka zookeeper
# start rest layer also
curl -o /sports-cloud-rest-server.jar  http://artifact-server.marathon.l4lb.thisdcos.directory:9082/artifacts/slingtv/sports-cloud/sports-cloud-rest-server.jar
mkdir -p /var/log/sports-cloud-rest-server
java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=data.elastic.l4lb.thisdcos.directory -DindexingPort=9200 -jar /sports-cloud-rest-server.jar localhost 9080 
