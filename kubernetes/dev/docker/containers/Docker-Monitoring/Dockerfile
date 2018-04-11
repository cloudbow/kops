FROM alpine
ADD scripts /scripts
ADD config /config
RUN apk update && \
    apk add bash && \
    apk add make && \
    apk add wget && \
    apk add curl && \
    apk add strace && \
    apk add git && \
    apk add logrotate && \
    apk add --update coreutils && rm -rf /var/cache/apk/*