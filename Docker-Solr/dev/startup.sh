#!/usr/bin/env sh
# Starting solr cloud
$SOLR_HOME/bin/solr -e cloud -noprompt
$SOLR_HOME/bin/solr create -c game_schedule  -d data_driven_schema_configs
$SOLR_HOME/bin/solr create -c live_info  -d data_driven_schema_configs
$SOLR_HOME/bin/solr create -c team_standings  -d data_driven_schema_configs
$SOLR_HOME/bin/solr create -c player_stats  -d data_driven_schema_configs