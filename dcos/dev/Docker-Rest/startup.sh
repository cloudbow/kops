#!/usr/bin/env sh
# Starting kafka zookeeper
# start rest layer also
git clone https://arun.george:sling%40123@githsports.slingbox.com/scm/sapi/slingtv-proxy-api.git
cd slingtv-proxy-api
git checkout feature/dcos_local
cd sports-cloud-rest-server
$MAVEN_HOME/bin/mvn package -DskipTests
java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=localhost -DindexingPort=9200 -jar target/sports-cloud-rest-server.jar localhost 9080
sleep 5h