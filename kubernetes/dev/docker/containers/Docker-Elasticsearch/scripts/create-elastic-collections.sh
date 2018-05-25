#!/bin/bash
curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-team-standings" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 1, 
            "number_of_replicas" : 1 
        }
    }
}'


curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-player-stats" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 1, 
            "number_of_replicas" : 1 
        }
    }
}'

curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-game-schedule" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 5, 
            "number_of_replicas" : 1 
        }
    }
}'

curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-live-info" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 5, 
            "number_of_replicas" : 1 
        }
    }
}'

curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-scoring-events" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 5, 
            "number_of_replicas" : 1 
        }
    }
}'

curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-elastic-search-metrics" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 3, 
            "number_of_replicas" : 1 
        }
    }
}'

curl -XPUT "http://sc-apps-dev-elasticsearch-client.default.svc.cluster.local:9200/sc-player-game-stats" -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 5,
            "number_of_replicas" : 1
        }
    }
}'