#!/usr/bin/env sh
mkdir /artifacts
chown -R nginx: /artifacts
nginx
tail -f /dev/null