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


all-external-tools-once:
	@$(current_path)/dev/scripts/application/external-tools-exactly-once.sh $(current_path)/dev

## Create external tools required
## Create private docker registry
all-external-tools:
	@$(current_path)/dev/scripts/application/external-tools.sh $(current_path)/dev

# Following actions needs to be done whenever any change in image happens to any of the docker images
# Build Docker images for the required applicatons



add-docker-artifacts-from-project-for-tag:
	@$(current_path)/dev/scripts/jenkins/burn-project-artifacts-for-docker-tag.sh

build-and-push-docker-container-for-tag:
	@$(current_path)/dev/scripts/jenkins/build-and-push-docker-container-for-tag.sh


