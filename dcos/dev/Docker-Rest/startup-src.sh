#!/usr/bin/env sh
# Starting kafka zookeeper
# start rest layer also
git clone https://arun.george:sling%40123@githsports.slingbox.com/scm/sapi/slingtv-proxy-api.git
cd slingtv-proxy-api
git checkout feature/dcos_local
cd sports-cloud-rest-server
$MAVEN_HOME/bin/mvn package -DskipTests
mkdir -p /var/log/sports-cloud-rest-server
nohup java -Dtarget-host-to-proxy=http://93a256a7.cdn.cms.movetv.com -DindexingHost=api.elastic.marathon.l4lb.thisdcos.directory -DindexingPort=9200 -jar target/sports-cloud-rest-server.jar localhost 9080 >/var/log/sports-cloud-rest-server/app.log 2>&1 &
tail -f /var/log/sports-cloud-rest-server/app.log
