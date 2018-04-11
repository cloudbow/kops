#!/usr/bin/env sh
FILE_NAME=$1
UPLOAD_PATH=$2
curl --upload-file $FILE_NAME http://p1.dcos:9082/artifacts/$UPLOAD_PATH
curl -v -o /tmp/o.y  http://p1.dcos:9082/artifacts/$UPLOAD_PATH