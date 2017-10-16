FROM nginx
RUN mkdir /artifacts && chown -R nginx: /artifacts
COPY artifact-server.conf /etc/nginx/conf.d/default.conf
COPY healthcheck.json /usr/share/nginx/html