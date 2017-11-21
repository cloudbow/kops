FROM anapsix/alpine-java:8_jdk
COPY scheduled-job-driver.sh /deploy-scheduled-jobs/
ENV SCALA_VERSION=2.12.2 \
    SCALA_HOME=/usr/share/scala
RUN apk update && \
    apk add bash && \ 
    apk add git && \
    apk add wget && \
    apk add curl && \
    apk add jq && \
    apk add strace && \
    apk add tar && \
    apk add --update coreutils && rm -rf /var/cache/apk/* && \
    apk add --update netcat-openbsd && rm -rf /var/cache/apk/* && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    apk add --no-cache bash && \
    mkdir -p /java-libs && \
    cd "/tmp" && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    chmod +x /deploy-scheduled-jobs/scheduled-job-driver.sh && \
    apk del .build-dependencies
EXPOSE 8983
EXPOSE 9080
