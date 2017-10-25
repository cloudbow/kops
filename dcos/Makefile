# Docker configuration
#
# Calculate current path 
mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_path := $(dir $(mkfile_path))

# Instruct "make" to not check files with that names
#
# As "make" is working primary with files, we need to instruct it that
# some target is not an actual target, or it will not execute project
# target as it will check "directory" for existance and exit. But if
# it will be specified in this target as dependency, "make" will
# execute it regardes of files existence.
.PHONY: $(TAG) $(NETWORK) \
	docker-build docker-network-create its  \
	install-dcos-packages build-rest-layer-dev push-rest-docker-dev submit-rest-layer-dev docker-network-inspect


TAG=sports-cloud-dcos
NETWORK=sports-cloud-dcos-network
TAG_REST=sports-cloud-dcos-rest
TAG_ARTIFACT_SERVER=artifact-server
TAG_JOB_SCHEDULER=sc-job-scheduler


DOCKER_ID=registry.marathon.l4lb.thisdcos.directory:5000
ARTIFACT_SERVER_NODE=172.17.0.3
BASE_DOCKER_REGISTRY_DIR=172.17.0.3

# Variables for docker registry
MASTER_CTR:= dcos-docker-master
AGENT_CTR := dcos-docker-agent
PUBLIC_AGENT_CTR := dcos-docker-pubagent
BASE_DOCKER_REGISTRY_DIR := $(current_path)/dev/Docker-Registry
INCLUDE_DIR := $(BASE_DOCKER_REGISTRY_DIR)/include
# Variables for the certs for a registry on the first master node.
CERTS_DIR := $(INCLUDE_DIR)/certs
ROOTCA_CERT := $(CERTS_DIR)/cacert.pem
CLIENT_CSR := $(CERTS_DIR)/client.csr
CLIENT_KEY := $(CERTS_DIR)/client.key
CLIENT_CERT := $(CERTS_DIR)/client.cert
INTERACTIVE := -i
# Variable for the registry host
REGISTRY_HOST := registry.local
MASTERS := 1
AGENTS := 1
PUBLIC_AGENTS := 1

ips: ## Gets the ips for the currently running containers.
	@$(foreach NUM,$(shell [[ $(MASTERS) == 0 ]] || seq 1 1 $(MASTERS)),$(call exit_not_running_container,$(MASTER_CTR),$(NUM)))
	@$(foreach NUM,$(shell [[ $(AGENTS) == 0 ]] || seq 1 1 $(AGENTS)),$(call exit_not_running_container,$(AGENT_CTR),$(NUM)))
	@$(foreach NUM,$(shell [[ $(PUBLIC_AGENTS) == 0 ]] || seq 1 1 $(PUBLIC_AGENTS)),$(call exit_not_running_container,$(PUBLIC_AGENT_CTR),$(NUM)))
	$(foreach NUM,$(shell [[ $(MASTERS) == 0 ]] || seq 1 1 $(MASTERS)),$(call get_master_ips,$(NUM)))
	$(foreach NUM,$(shell [[ $(AGENTS) == 0 ]] || seq 1 1 $(AGENTS)),$(call get_agent_ips,$(NUM)))
	$(foreach NUM,$(shell [[ $(PUBLIC_AGENTS) == 0 ]] || seq 1 1 $(PUBLIC_AGENTS)),$(call get_public_agent_ips,$(NUM)))

###########################
#### Helper Function ####
###########################

IP_CMD := docker inspect --format "{{.NetworkSettings.Networks.bridge.IPAddress}}"
STATE_CMD := docker inspect --format "{{.State.Running}}"

null :=
space := ${null} ${null}
${space} := ${space} # ${ } is a space.
comma := ,
define newline

-
endef

# Define the function to populate the MASTER_IPS variable with the
# corresponding IPs of the DC/OS master containers.
# @param number	  ID of the container.
define get_master_ips
$(eval MASTER_IPS := $(MASTER_IPS) $(shell $(IP_CMD) $(MASTER_CTR)$(1)))
endef

# Define the function to populate the AGENT_IPS variable with the
# corresponding IPs of the DC/OS agent containers.
# @param number	  ID of the container.
define get_agent_ips
$(eval AGENT_IPS := $(AGENT_IPS) $(shell $(IP_CMD) $(AGENT_CTR)$(1)))
endef

define get_public_agent_ips
$(eval PUBLIC_AGENT_IPS := $(PUBLIC_AGENT_IPS) $(shell $(IP_CMD) $(PUBLIC_AGENT_CTR)$(1)))
endef

# Define the function to exit if a container is not running.
# @param name	  First part of the container name.
# @param number	  ID of the container.
define exit_not_running_container
if [[ "$(shell $(STATE_CMD) $(1)$(2))" != "true" ]]; then \
	>&2 echo "$(1)$(2) is not running"; \
	exit 1; \
fi;
endef


$(CERTS_DIR): $(INCLUDE_DIR)
	@mkdir -p $@

$(CERTS_DIR)/openssl-ca.cnf: $(CERTS_DIR)
	@cp $(BASE_DOCKER_REGISTRY_DIR)/configs/certs/openssl-ca.cnf $@

$(ROOTCA_CERT): $(CERTS_DIR)/openssl-ca.cnf
	@openssl req -x509 \
		-config $(CERTS_DIR)/openssl-ca.cnf \
		-newkey rsa:4096 -sha256 \
		-subj "/C=US/ST=California/L=San Francisco/O=Mesosphere/CN=DCOS Test CA" \
		-nodes -out $@ -outform PEM
	@openssl x509 -noout -text -in $@

$(CERTS_DIR)/openssl-server.cnf: $(CERTS_DIR)
	@cp $(BASE_DOCKER_REGISTRY_DIR)/configs/certs/openssl-server.cnf $@
	@echo "DNS.1 = $(REGISTRY_HOST)" >> $@
	@echo "IP.1 = $(firstword $(MASTER_IPS))" >> $@

$(CLIENT_CSR): ips $(CERTS_DIR)/openssl-server.cnf
	@openssl req \
		-config $(CERTS_DIR)/openssl-server.cnf \
		-newkey rsa:2048 -sha256 \
		-subj "/C=US/ST=California/L=San Francisco/O=Mesosphere/CN=$(REGISTRY_HOST)" \
		-nodes -out $@ -outform PEM
	@openssl req -text -noout -verify -in $@

$(CERTS_DIR)/index.txt: $(CERTS_DIR)
	@touch $@

$(CERTS_DIR)/serial.txt: $(CERTS_DIR)
	@echo '01' > $@

$(CLIENT_CERT): $(ROOTCA_CERT) $(CLIENT_CSR) $(CERTS_DIR)/index.txt $(CERTS_DIR)/serial.txt
	@openssl ca -batch \
		-config $(CERTS_DIR)/openssl-ca.cnf \
		-policy signing_policy -extensions signing_req \
		-out $@ -infiles $(CLIENT_CSR)
	@openssl x509 -noout -text -in $@

registry: $(CLIENT_CERT) ## Start a docker registry with certs in the mesos master.
	@docker exec $(INTERACTIVE) $(MASTER_CTR)1 \
		docker run \
		-d --restart=always \
		-p 5000:5000 \
		-v /etc/docker/certs.d:/certs \
		-e REGISTRY_HTTP_TLS_CERTIFICATE=/certs/client.cert \
  		-e REGISTRY_HTTP_TLS_KEY=/certs/client.key \
		--name registry \
  		registry:2
	@$(eval REGISTRY_IP := $(firstword $(MASTER_IPS)):5000)
	@$(call copy_registry_certs,$(REGISTRY_IP))
	@$(call copy_registry_certs,$(REGISTRY_HOST):5000)
	@echo "Your registry is reachable from inside the DC/OS containers at:"
	@echo -e "\t$(REGISTRY_HOST):5000"
	@echo -e "\t$(REGISTRY_IP)"


# Connect filter to update replication factor to 1 since in local we have only one kafka broker
CONNECT_JQ_FILTER='.env *=  { "CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR": "1" , "CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR" : "1" , "CONNECT_STATUS_STORAGE_REPLICATION_FACTOR" : "1" , "CONNECT_PLUGIN_PATH": "/data/kafka/connect/libs" , "CONNECT_KEY_CONVERTER": "org.apache.kafka.connect.json.JsonConverter" , "CONNECT_VALUE_CONVERTER" : "org.apache.kafka.connect.json.JsonConverter" , "CONNECT_KEY_CONVERTER_SCHEMAS_ENABLE" : "true" ,"CONNECT_VALUE_CONVERTER_SCHEMAS_ENABLE" : "true" } | .container |= .+ {"volumes": [{"containerPath": "/data/kafka/connect/libs","hostPath": "/data/apps/sports-cloud/kafka/connect/libs","mode": "RW"}]}'

SPORTS_CLOUD_BATCH_JOB_JAR="all-spark-jobs.jar"
SPORTS_CLOUD_BATCH_JOB_ARTIFACT="slingtv/sports-cloud/$(SPORTS_CLOUD_BATCH_JOB_JAR)"
SCALA_SPARK_JOB_VERSION="2.12"

# Create Docker image
#
# To distinguish java docker immage from other it need to have
# unique tag, that is provided as argument to build command.
docker-build:
	@docker build --tag=$(TAG) - < ./dev/Dockerfilev2

docker-network-create:
	@docker network create --driver bridge $(NETWORK)

docker-network-inspect:
	docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'



install-services:
	@$(current_path)/dev/scripts/dcos/deploy-services-group.sh $(current_path)/dev/Docker-Dcos/config/service-groups/all-services.json
# Run Docker container
#

build-rest-layer-dev:
	@$(current_path)/dev/scripts/docker/build-docker-container.sh $(current_path)/dev/Docker-Rest $(TAG_REST) $(DOCKER_ID)

push-rest-docker-dev:
	@$(current_path)/dev/scripts/docker/push-docker-container.sh $(current_path)/dev/Docker-Rest  $(DOCKER_ID)/$(TAG_REST):latest

submit-rest-layer-dev:
	@$(current_path)/dev/scripts/docker/submit-docker-container.sh sports-cloud-rest $(current_path)/dev/Docker-Dcos/config/sports-cloud-rest.json


build-artser-layer-dev:
	@$(current_path)/dev/scripts/docker/build-docker-container.sh $(current_path)/dev/Docker-ArtifactsServer $(TAG_ARTIFACT_SERVER) $(DOCKER_ID)

push-artser-docker-dev: $(build-artser-layer-dev)
	@$(current_path)/dev/scripts/docker/push-docker-container.sh $(current_path)/dev/Docker-ArtifactsServer  $(DOCKER_ID)/$(TAG_ARTIFACT_SERVER):latest

submit-artser-layer-dev: $(push-artser-docker-dev)
	@$(current_path)/dev/Docker-ArtifactsServer/scripts/submit-docker-container.sh $(current_path) artifact-server $(current_path)/dev//Docker-Dcos/config/artifact-server.json

build-sc-job-scheduler-docker-dev:
	@$(current_path)/dev/scripts/docker/build-docker-container.sh $(current_path)/dev/Docker-ScheduledJob $(TAG_JOB_SCHEDULER) $(DOCKER_ID)

push-sc-job-scheduler-docker-dev: $(build-sc-job-scheduler-docker-dev)
	@$(current_path)/dev/scripts/docker/push-docker-container.sh $(current_path)/dev/Docker-ScheduledJob  $(DOCKER_ID)/$(TAG_JOB_SCHEDULER):latest

push-kafka-cp-connect:
	@$(current_path)/dev/scripts/application/push-external-docker-containers.sh	confluentinc/cp-kafka-connect:3.3.0 registry.marathon.l4lb.thisdcos.directory:5000/cp-kafka-connect:3.3.0

push-mesos-spark:
	@$(current_path)/dev/scripts/application/push-external-docker-containers.sh mesosphere/spark:2.0.1-2.2.0-1-hadoop-2.6 registry.marathon.l4lb.thisdcos.directory:5000/spark:2.0.1-2.2.0-1-hadoop-2.6

install-confluent-connect-dev:
	@$(current_path)/dev/scripts/dcos/install-dcos-packages.sh $(current_path)/dev $(current_path)/dev/Docker-Dcos connect confluent-connect $(CONNECT_JQ_FILTER)

install-confluent-kafka-dev:
	@$(current_path)/dev/scripts/dcos/install-dcos-packages.sh $(current_path)/dev $(current_path)/dev/Docker-Dcos kafka confluent-kafka

install-spark-dev:
	@$(current_path)/dev/scripts/dcos/install-dcos-packages.sh $(current_path)/dev $(current_path)/dev/Docker-Dcos spark spark

install-elastic-dev:
	@$(current_path)/dev/scripts/dcos/install-dcos-packages.sh $(current_path)/dev $(current_path)/dev/Docker-Dcos elastic elastic


install-registry:
	@$(current_path)/dev/scripts/dcos/submit-marathon-job.sh  registry $(current_path)/dev/Docker-Dcos/config/registry.json
	
	
install-marathon-lb:
	@$(current_path)/dev/deploy-scheduled-jobs/scripts/kafka/create-kafka-topics.sh


create-kafka-topics:
	@$(current_path)/dev/deploy-scheduled-jobs/scripts/kafka/create-kafka-topics.sh

create-local-dependecies:
	@$(current_path)/dev/scripts/application/create-host-dependencies.sh $(current_path)  $(SPORTS_CLOUD_BATCH_JOB_ARTIFACT)

schedule-job-download-summary-dev:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh download-summary $(current_path)/dev/Docker-Dcos/config/jobs/download-summary.json	
schedule-job-download-thuuz-dev:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh download-thuuz $(current_path)/dev/Docker-Dcos/config/jobs/download-thuuz.json	
schedule-job-download-schedules-dev:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh download-schedules $(current_path)/dev/Docker-Dcos/config/jobs/download-schedules.json	

schedule-kafka-connect-meta-batch:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh confluent-connect-meta-batch $(current_path)/dev/Docker-Dcos/config/jobs/kafka/confluent-connect-meta_batch.json	

schedule-kafka-connect-content-match:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh confluent-connect-content-match $(current_path)/dev/Docker-Dcos/config/jobs/kafka/confluent-connect-content_match.json	

schedule-kafka-connect-live-info:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh confluent-connect-live-info $(current_path)/dev/Docker-Dcos/config/jobs/kafka/confluent-connect-live_info.json	

schedule-player-stats-batch-job:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh player-stats-mlb $(current_path)/dev/Docker-Dcos/config/jobs/spark/mlb/player-stats.json
	
schedule-team-standings-batch-job:
	@$(current_path)/dev/scripts/dcos/submit-dcos-job.sh player-stats-mlb $(current_path)/dev/Docker-Dcos/config/jobs/spark/mlb/team-standings.json