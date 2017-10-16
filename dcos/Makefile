# Docker configuration
#

TAG=sports-cloud-dcos
NETWORK=sports-cloud-dcos-network

TAG_REST=sports-cloud-dcos-rest
TAG_ARTIFACT_SERVER=artifact-server

ARTIFACT_SERVER_NODE=172.17.0.3

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
	docker-build docker-network-create  \
	install-dcos-packages build-rest-layer-dev push-rest-docker-dev submit-rest-layer-dev docker-network-inspect
	


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


# Run Docker container
#

install-dcos-packages-dev:
	@$(current_path)/dev/Docker-Dcos/scripts/install-dcos-packages.sh $(current_path)/dev/Docker-Dcos

build-rest-layer-dev:
	@$(current_path)/dev/scripts/build-docker-container.sh $(current_path)/dev/Docker-Rest $(TAG_REST) arungeorge09

push-rest-docker-dev:
	@$(current_path)/dev/scripts/push-docker-container.sh $(current_path)/dev/Docker-Rest  arungeorge09/$(TAG_REST):latest

submit-rest-layer-dev:
	@$(current_path)/dev/scripts/submit-docker-container.sh sports-cloud-rest $(current_path)/dev/Docker-Rest/config/sports-cloud-rest.json


build-artser-layer-dev:
	@$(current_path)/dev/scripts/build-docker-container.sh $(current_path)/dev/Docker-ArtifactsServer $(TAG_ARTIFACT_SERVER) arungeorge09

push-artser-docker-dev:
	@$(current_path)/dev/scripts/push-docker-container.sh $(current_path)/dev/Docker-ArtifactsServer  arungeorge09/$(TAG_ARTIFACT_SERVER):latest

submit-artser-layer-dev:
	@$(current_path)/dev/Docker-ArtifactsServer/scripts/submit-docker-container.sh $(current_path) artifact-server $(current_path)/dev/Docker-ArtifactsServer/config/artifact-server.json  '.constraints += [["hostname","CLUSTER","'"$(ARTIFACT_SERVER_NODE)"'"]] | .container.portMappings[0] |= .+ {"labels": { "VIP_0": "/artifact-server:9082" }}'


