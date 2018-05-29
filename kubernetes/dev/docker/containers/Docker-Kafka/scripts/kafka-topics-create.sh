#!/bin/bash

CONFIG_NUM_PARTITIONS=1
DEFAULT_REPLICATION_FACTOR=3
MINIMUM_NUM_PARTITIONS=5
OFFSET_PARTITIONS=50
TOPIC_PARTITIONS=10
ZOOKEEPER=$KAFKA_ZOOKEEPER_CONNECT

# config.storage.topic=connect-configs
kafka-topics --if-not-exists --topic connect-sc-configs --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $CONFIG_NUM_PARTITIONS


# offset.storage.topic=connect-offsets
kafka-topics --if-not-exists --topic connect-sc-offsets --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $OFFSET_PARTITIONS  

# status.storage.topic=connect-status
kafka-topics --if-not-exists --topic connect-sc-status --create --zookeeper $ZOOKEEPER  --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $TOPIC_PARTITIONS  


# --create  all mlb topics
kafka-topics --if-not-exists --topic  content_match_mlb --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_mlb  --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_mlb --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# --create all ncaaf topics
kafka-topics --if-not-exists --topic  content_match_ncaaf --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_ncaaf --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_ncaaf --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# Create all ncaab topics
kafka-topics --if-not-exists --topic  content_match_ncaab --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_ncaab --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_ncaab --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS
kafka-topics --if-not-exists --topic  player_game_stats_ncaab --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# Create all nba topics
kafka-topics --if-not-exists --topic  content_match_nba --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_nba --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_nba --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS
kafka-topics --if-not-exists --topic  player_game_stats_nba --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS


# Create all nfl topics
kafka-topics --if-not-exists --topic  content_match_nfl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_nfl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_nfl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# Create all nhl topics
kafka-topics --if-not-exists --topic  content_match_nhl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  live_info_nhl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 
kafka-topics --if-not-exists --topic  meta_batch_nhl --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS

# Create live info soccer topic
kafka-topics --if-not-exists --topic  live_info_soccer --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS 


## Create confluent topic
kafka-topics --if-not-exists --topic  _confluent-monitoring --create --zookeeper $ZOOKEEPER --replication-factor $DEFAULT_REPLICATION_FACTOR --partitions $MINIMUM_NUM_PARTITIONS