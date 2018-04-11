FROM nginx
ADD startup.sh /root/startup.sh
COPY artifact-server.conf /etc/nginx/conf.d/default.conf
COPY healthcheck.json /usr/share/nginx/html
ENTRYPOINT /root/startup.sh