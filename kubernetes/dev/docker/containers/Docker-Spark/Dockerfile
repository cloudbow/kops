FROM registry.sports-cloud.com:5000/spark-base

RUN mkdir -p /root/.ivy2/jars
COPY spark-libs/*.jar  /root/.ivy2/jars/
COPY spark-libs/*.jar /opt/spark/jars/
ADD log4j.properties /opt/spark/conf/log4j.properties
ADD spark-defaults.conf /opt/spark/conf/spark-defaults.conf
ADD start-master /
ADD start-worker /
ENV PATH $PATH:/opt/spark/bin
ENV LD_LIBRARY_PATH ${LD_LIBRARY_PATH}:/opt/hadoop/lib/native
ENV SPARK_LOCAL_DIRS /spark/work
ENV SPARK_WORKER_DIR /spark/work

