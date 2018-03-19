#!/bin/bash
set -x
### Modfied for high performance
export KAFKA_BROKER_ID=${HOSTNAME##*-}
/etc/confluent/docker/run