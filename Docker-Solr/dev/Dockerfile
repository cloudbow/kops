FROM anapsix/alpine-java
ADD startup.sh /root/startup.sh
ENV SOLR_VERSION=6.6.0 \
    SOLR_HOME=/solr
RUN apk update && \
    apk upgrade && \
    apk add curl && \
    apk add --no-cache bash && \
  	cd "/tmp" && \
  	curl http://www-eu.apache.org/dist/lucene/solr/6.6.0/solr-6.6.0.tgz | tar -xzf - && \
  	mv solr-6.6.0 $SOLR_HOME && \
  	chmod +x /root/startup.sh 
EXPOSE 8983
WORKDIR $SOLR_HOME
ENTRYPOINT /root/startup.sh
