#!/usr/bin/env sh
curl --upload-file $2 http://$1:9082/artifacts/$3
