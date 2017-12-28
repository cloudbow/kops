#!/bin/bash

# Copyright 2015 The Kubernetes Authors All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

echo "=== Launching Zeppelin under Docker ==="
function join { local IFS="$1"; shift; echo "$*"; }
IFS=', ' read -r -a masterList <<< "$MASTER"
cnt=${#masterList[@]}
masterListPrepended=()
for ((i=0;i<cnt;i++)); do
    masterListPrepended[i]="spark://${masterList[i]}"
done 
echo "Created prepended master list"
printf "\nexport MASTER=%s" `join , ${masterListPrepended[@]}` >>  /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport SPARK_HOME=%s" $SPARK_HOME >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_HOME=%s" $ZEPPELIN_HOME >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_JAVA_OPTS=%s" $ZEPPELIN_JAVA_OPTS >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport CLASSPATH=%s" $CLASSPATH >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_NOTEBOOK_DIR=%s" $ZEPPELIN_NOTEBOOK_DIR >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_MEM=%s" $ZEPPELIN_MEM >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_PORT=%s" $ZEPPELIN_PORT >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport PYTHONPATH=%s" $PYTHONPATH >> /opt/zeppelin/conf/zeppelin-env.sh
printf "\nexport ZEPPELIN_CONF_DIR=%s" $ZEPPELIN_CONF_DIR >> /opt/zeppelin/conf/zeppelin-env.sh

/opt/zeppelin/bin/zeppelin.sh "${ZEPPELIN_CONF_DIR}"
sleep 10000