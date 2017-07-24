FROM anapsix/alpine-java
ADD startup.sh /root/startup.sh
ADD spark-log4j-config /spark-log4j-config
ENV SCALA_VERSION=2.12.2 \
    SCALA_HOME=/usr/share/scala \
    KAFKA_HOME=/kafka \
    SBT_VERSION=0.13.11 \
    SBT_HOME=/usr/local/sbt \
    SPARK_HOME=/spark-2.1.0-bin-hadoop2.7 \
    MAVEN_VERSION=3.3.9 \
    MAVEN_HOME=/usr/lib/maven	
RUN apk update && \
    apk add bash && \ 
    apk add wget && \
    apk add curl && \
    apk add screen && \
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
    curl http://www-us.apache.org/dist/kafka/0.10.2.0/kafka_2.11-0.10.2.0.tgz | tar -xzf - && \
    mv kafka_2.11-0.10.2.0 $KAFKA_HOME && \
    mkdir /tmp/zookeeper && \
    mkdir /tmp/kafka-logs && \
    chmod +x /root/startup.sh && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    apk del .build-dependencies && \   
    wget http://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz && \
    tar -xvf spark-2.1.0-bin-hadoop2.7.tgz && \
    mv spark-2.1.0-bin-hadoop2.7 /
EXPOSE 9999
ENTRYPOINT /root/startup.sh