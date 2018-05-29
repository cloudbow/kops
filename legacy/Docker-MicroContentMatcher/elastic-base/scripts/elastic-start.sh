#!/usr/bin/env sh
# Starting kafka zookeeper
/elastic/bin/elasticsearch -d
sleep 20s;

# Create Elastic search
curl -XPUT 'localhost:9200/sports-cloud?pretty' -H 'Content-Type: application/json' -d'
{
    "settings" : {
        "index" : {
            "number_of_shards" : 3, 
            "number_of_replicas" : 2 
        }
    }
}'
