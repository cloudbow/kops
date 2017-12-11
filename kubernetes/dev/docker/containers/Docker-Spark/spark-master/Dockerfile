FROM registry.sports-cloud.com:5000/spark-base
ADD log4j.properties /opt/spark/conf/log4j.properties
ADD spark-defaults.conf /opt/spark/conf/spark-defaults.conf
ADD start-master /
ENV PATH $PATH:/opt/spark/bin
ENV LD_LIBRARY_PATH ${LD_LIBRARY_PATH}:/opt/hadoop/lib/native
ENV SPARK_LOCAL_DIRS /spark/work
ENV SPARK_WORKER_DIR /spark/work

