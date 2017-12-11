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



## Run this on the Jenkins server
build-and-push-builder-docker:
	@$(current_path)/dev/scripts/application/build-builder.sh $(current_path)/dev


all-external-tools-once:
	@$(current_path)/dev/scripts/application/external-tools-exactly-once.sh $(current_path)/dev

## Create external tools required
## Create private docker registry
all-external-tools:
	@$(current_path)/dev/scripts/application/external-tools.sh $(current_path)/dev

# Following actions needs to be done whenever any change in image happens to any of the docker images
# Build Docker images for the required applicatons

## This will burn all required docker things into the corresponding docker directores
add-docker-artifacts-from-project:
	@$(current_path)/dev/scripts/application/burn-project-artifacts-for-docker.sh $(current_path)/dev

# Following action is needed to push all docker contianers
build-and-push-all-docker-containers: $(build-rest-layer-dev)
	@$(current_path)/dev/scripts/application/push-all-docker-containers.sh $(current_path)/dev


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
