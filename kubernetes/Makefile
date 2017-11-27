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
	install-k8s-packages build-rest-layer-dev push-rest-docker-dev submit-rest-layer-dev docker-network-inspect


TAG=sports-cloud-k8s
NETWORK=sports-cloud-k8s-network



# Run Docker container
#

## Folloiwng are for building all custom docker containers

build-rest-layer-docker-dev:
	@$(current_path)/dev/scripts/application/build-rest-docker.sh $(current_path)/dev

build-art-serv-layer-docker-dev:
	@$(current_path)/dev/scripts/application/build-art-serv-docker.sh $(current_path)/dev

build-kafka-connect-docker-dev:
	@$(current_path)/dev/scripts/application/build-kafka-connect-docker.sh $(current_path)/dev

build-spark-cluster-docker-dev:
	@$(current_path)/dev/scripts/application/build-spark-container.sh $(current_path)/dev

build-spark-job-docker-dev:
	@$(current_path)/dev/scripts/application/build-spark-job-container.sh $(current_path)/dev

build-sc-job-scheduler-docker-dev:
	@$(current_path)/dev/scripts/application/build-job-scheduler-container.sh $(current_path)/dev

push-all-docker-containers: $(build-rest-layer-dev)
	@$(current_path)/dev/scripts/application/push-all-docker-containers.sh $(current_path)/dev

create-alb-attributes:
	@$(current_path)/dev/scripts/aws/post-create-config.sh $(current_path)/dev

create-docker-credentials:
	@$(current_path)/dev/scripts/application/create-docker-credentials.sh
all-external-tools:
	@$(current_path)/dev/scripts/application/external-tools.sh $(current_path)/dev
## Use this to submit all serivces
all-services:
	@$(current_path)/dev/scripts/k8s/deploy-services-group.sh  $(current_path)/dev/config/all-services.yml
## Use this to submit all the jobs	
all-jobs:
	@$(current_path)/dev/scripts/k8s/deploy-services-group.sh  $(current_path)/dev/config/all-jobs.yml
## Use this to test a single srevce	
services-placeholder:
	@$(current_path)/dev/scripts/k8s/deploy-services-group.sh  $(current_path)/dev/config/all-services-placeholder.yml
## Use this to test a single job
jobs-placeholder:
	@$(current_path)/dev/scripts/k8s/deploy-services-group.sh $(current_path)/dev/config/job-placeholder.yml
	
kube-dashboard:
	@$(current_path)/dev/scripts/k8s/deploy-services-group.sh $(current_path)/dev/config/kube-dashboard.yml

