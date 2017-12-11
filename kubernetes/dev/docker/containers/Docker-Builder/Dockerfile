FROM anapsix/alpine-java:8_jdk
ENV SCALA_VERSION=2.12.2 \
    SCALA_HOME=/usr/share/scala \
    SBT_VERSION=0.13.11 \
    SBT_HOME=/usr/local/sbt \
    MAVEN_VERSION=3.3.9 \
    MAVEN_HOME=/usr/lib/maven
RUN apk update && \
    apk add bash && \ 
    apk add wget && \
    apk add curl && \
    apk add strace && \
    apk add git && \
    apk add --update coreutils && rm -rf /var/cache/apk/* && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    apk add --no-cache bash && \
    cd "/tmp" && \
    wget -q http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz -O - | tar xzf - && \
    mv /tmp/apache-maven-$MAVEN_VERSION /usr/lib/maven && \
    curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    apk del .build-dependencies
ENV PATH="$MAVEN_HOME/bin:$SBT_HOME/bin:$SCALA_HOME/bin:$PATH"