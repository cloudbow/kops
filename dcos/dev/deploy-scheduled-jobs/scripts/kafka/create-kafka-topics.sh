#!/bin/sh
DEFAULT_REPLICATION_FACTOR=1
MINIMUM_NUM_PARTITIONS=1
# config.storage.topic=connect-configs
dcos confluent-kafka topic create connect-sc-configs --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS


# offset.storage.topic=connect-offsets
OFFSET_PARTITIONS=50
dcos confluent-kafka topic create connect-sc-offsets --replication $DEFAULT_REPLICATION_FACTOR --partitions $OFFSET_PARTITIONS  

# status.storage.topic=connect-status
TOPIC_PARTITIONS=10
dcos confluent-kafka topic create connect-sc-status --replication $DEFAULT_REPLICATION_FACTOR --partitions $TOPIC_PARTITIONS  


# Create topic mlb_meta
dcos confluent-kafka topic create content_match --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
dcos confluent-kafka topic create live_info --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
dcos confluent-kafka topic create meta_batch --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# Create topic ncaaf_meta
dcos confluent-kafka topic create content_match_ncaaf --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
dcos confluent-kafka topic create live_info_ncaaf --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
dcos confluent-kafka topic create meta_batch_ncaaf --replication $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS
