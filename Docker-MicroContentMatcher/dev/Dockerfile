FROM anapsix/alpine-java:8_jdk
ADD startupv2.sh /root/startupv2.sh
ADD elastic-base /elastic-base
RUN addgroup elasticsearch
RUN adduser -G elasticsearch -S  elasticsearch 
ENV SCALA_VERSION=2.12.2 \
    SCALA_HOME=/usr/share/scala \
    CONFLUENT_HOME=/confluent \
    SBT_VERSION=0.13.11 \
    SBT_HOME=/usr/local/sbt \
    SPARK_HOME=/spark-2.1.0-bin-hadoop2.7 \
    MAVEN_VERSION=3.3.9 \
    MAVEN_HOME=/usr/lib/maven \
    ELASTIC_VERSION=5.6.0 \
    ELASTIC_HOME=/elastic \
    KAFKA_HEAP_OPTS="-Xms512m -Xmx1g"
RUN apk update && \
    apk add bash && \ 
    apk add wget && \
    apk add curl && \
    apk add strace && \
    apk add screen && \
    apk add --update coreutils && rm -rf /var/cache/apk/* && \
    apk add --update netcat-openbsd && rm -rf /var/cache/apk/* && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    apk add --no-cache bash && \
    cd "/tmp" && \
    wget -q http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz -O - | tar xzf - && \
    mv /tmp/apache-maven-$MAVEN_VERSION /usr/lib/maven && \
    mkdir -p /var/log/sports-cloud-schedulers && \
    mkdir -p /var/log/sports-cloud-batch-job && \
    curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built && \
    curl http://packages.confluent.io/archive/3.3/confluent-oss-3.3.0-2.11.tar.gz | tar -xzf - && \
    mv confluent-3.3.0 $CONFLUENT_HOME && \
    mkdir /tmp/zookeeper && \
    mkdir /tmp/kafka-logs && \
    chmod +x /root/startupv2.sh && \
    chmod +x /elastic-base/scripts/elastic-start.sh && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    apk del .build-dependencies && \   
    wget http://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz && \
    tar -xvf spark-2.1.0-bin-hadoop2.7.tgz && \
    mv spark-2.1.0-bin-hadoop2.7 / && \
    wget https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-5.6.0.tar.gz && \
    tar -xvf elasticsearch-5.6.0.tar.gz && \
    mv elasticsearch-5.6.0 /elastic && \
    cp -f /elastic-base/config/elasticsearch.yml  $ELASTIC_HOME/config/elasticsearch.yml && \
    mkdir -p /var/log/elasticsearch && \
    mkdir -p /data/elastic && \
    chown -R elasticsearch:elasticsearch /var/log/elasticsearch && \
    chown -R elasticsearch:elasticsearch /data/elastic && \
    chown -R elasticsearch:elasticsearch /elastic
EXPOSE 8983
EXPOSE 9080
ENTRYPOINT /root/startupv2.sh