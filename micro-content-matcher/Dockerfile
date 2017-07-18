FROM anapsix/alpine-java
RUN apk update && \
    apk add bash && \ 
    apk add wget && \
    apk add curl && \
    wget http://d3kbcqa49mib13.cloudfront.net/spark-2.1.0-bin-hadoop2.7.tgz && \
    tar -xvf *.tgz && \
    rm *.tgz
EXPOSE 9999
# ENTRYPOINT cd /spark-2.1.0-bin-hadoop2.7/bin && ./spark-shell --driver-memory 4G --executor-memory 4G --packages com.databricks:spark-csv_2.10:1.5.0,joda-time:joda-time:2.9.7,org.xerial:sqlite-jdbc:3.18.0,com.databricks:spark-xml_2.11:0.4.0,org.mongodb.spark:mongo-spark-connector_2.11:2.0.0 -i /scripts/scala/docker/init.scala
ENTRYPOINT cd /spark-2.1.0-bin-hadoop2.7/bin && ./spark-shell --driver-memory 2G --executor-memory 2G --conf "spark.mongodb.input.uri=mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage.event?readPreference=primaryPreferred" --conf "spark.mongodb.output.uri=mongodb://sportapi2:Eo8ahsiera@cqhlsdb02.sling.com:2701/eventstorage.slingtv_schguid_cont_nagra_mapping" --packages com.databricks:spark-csv_2.10:1.5.0,joda-time:joda-time:2.9.7,org.mongodb.spark:mongo-spark-connector_2.11:2.0.0