# Running the project in dcos docker (Single Machine)
====================================================
Make sure the configuration of the machine is atleast 10 core 16 GB

## Run the following modified dcos docker to first create a dcos docker instance


Properties modified 

No of private agent nodes changed to 3

Ports exposed on agent nodes for access of dcos console

Volumes added exposing the following directories

/data/apps/sports-cloud/artifacts -- Artifact server location

/data/apps/sports-cloud/kafka/connect/libs -- Parser code for parsing files for kafka

/data/apps/sports-cloud/elastic/data -- Elastic data persistent path


Volumes loaded 

<code>
        -v /var/log:/var/log \
        -v /data/apps/sports-cloud/artifacts:/data/apps/sports-cloud/artifacts \
 		-v /data/apps/sports-cloud/docker/registry:/data/apps/sports-cloud/docker/registry \
        -v /data/apps/sports-cloud/docker-client/security:/data/apps/sports-cloud/docker-client/security \
    	-v /data/apps/sports-cloud/docker/security:/data/apps/sports-cloud/docker/security \
        -v /data/apps/sports-cloud/kafka/connect/libs:/data/apps/sports-cloud/kafka/connect/libs \
        -v /data/apps/sports-cloud/docker/security:/etc/docker/certs.d


</code>

<code>
git clone https://github.com/dcos/dcos-docker

make ./configure --auto && \
make && \
make postflight && \
make clean-hosts && \
make hosts
</code>

## Login to the machine with dcos docker running and do the following 


git clone https://githsports.slingbox.com/scm/sapi/slingtv-proxy-api.git

cd dcos

### Add all required packages.

Build and submit rest layer docker instance
<code>
make build-rest-layer-dev && make push-rest-docker-dev
</code>
Build and submit artifact docker instance
<code>
make build-artser-layer-dev && make push-artser-docker-dev
</code>

Build and submit job scheduler docker instance
<code>
make build-sc-job-scheduler-docker-dev && make push-sc-job-scheduler-docker-dev
</code>

dcos auth login -- Manual step? 

Install kafka, connect, spark  & elastic
<code>
dcos marathon group add ./dev/Docker-Dcos/config/service-groups/all-services.json
</code>
### Create the local dependencies

These include mainly the jar files for batch jobs, rest layer , & script modules zipped and uploaded to the artifact server.

<code>
make create-local-dependecies
</code>

### Create the kafka queues
<code>
make create-kafka-topics
</code>
### Schedule connect jobs

These are connect jobs which parses data from ftp and pushes to kafka queues
<code>
make schedule-kafka-connect-meta-batch && \
make schedule-kafka-connect-content-match && \
make schedule-kafka-connect-live-info
</code>
### Schedule download jobs

These are scheduled jobs which download slingtv, thuuz data in scheduled intervals
<code>
make schedule-job-download-summary-dev && \
make schedule-job-download-thuuz-dev && \
make schedule-job-download-schedules-dev 
</code>
### Schedule Spark jobs
<code>
make schedule-player-stats-batch-job && \
make schedule-team-standings-batch-job 
</code>