FROM anapsix/alpine-java

RUN apk update && apk upgrade
RUN apk add --no-cache bash gawk sed grep bc coreutils

ENV SOLR_VERSION 5.2.1
ENV SOLR solr-$SOLR_VERSION
ENV SOLR_USER solr

RUN export DEBIAN_FRONTEND=noninteractive && \
  apk update && \
  apk add lsof procps && \
  addgroup -S $SOLR_USER && \
  adduser -S -G $SOLR_USER $SOLR_USER && \
  mkdir -p /opt && \
  wget  -O /opt/$SOLR.tgz http://archive.apache.org/dist/lucene/solr/$SOLR_VERSION/$SOLR.tgz && \
  tar -C /opt --extract --file /opt/$SOLR.tgz && \
  rm /opt/$SOLR.tgz && \
  ln -s /opt/$SOLR /opt/solr && \
  chown -R $SOLR_USER:$SOLR_USER /opt/solr /opt/$SOLR

EXPOSE 8983
WORKDIR /opt/solr
USER $SOLR_USER
ENTRYPOINT ["/bin/sh", "-c", "/opt/solr/bin/solr -f"]
