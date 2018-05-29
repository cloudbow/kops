FROM quay.io/pires/docker-elasticsearch-kubernetes:6.0.0
ADD scripts /scripts
ENV ELASTIC_SEARCH_VERSION="6.0.0"
ENTRYPOINT ["/scripts/start-elasticsearch.sh"]