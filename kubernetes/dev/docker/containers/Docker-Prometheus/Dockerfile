FROM alpine
ENV PROMETHEUS_VERSION=2.2.1 
ADD scripts /scripts
RUN apk update && \
	apk add --no-cache sed && \
    apk add --no-cache coreutils && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    apk add --no-cache bash && \
    cd "/tmp" && \
    wget -q https://github.com/prometheus/prometheus/releases/download/v$PROMETHEUS_VERSION/prometheus-$PROMETHEUS_VERSION.linux-amd64.tar.gz -O - | tar xzf - && \
    mkdir /opt && \
    mv /tmp/prometheus-$PROMETHEUS_VERSION.linux-amd64 /opt/prometheus-$PROMETHEUS_VERSION.linux-amd64 && \
    ln -s /opt/prometheus-$PROMETHEUS_VERSION.linux-amd64 /opt/prometheus && \
    apk del .build-dependencies
ENTRYPOINT ["/scripts/start-prometheus"]