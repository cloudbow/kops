#!/bin/bash
/elasticsearch/bin/elasticsearch-plugin install -b https://distfiles.compuscene.net/elasticsearch/elasticsearch-prometheus-exporter-$ELASTIC_SEARCH_VERSION.0.zip
/run.sh