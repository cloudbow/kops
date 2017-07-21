# Docker configuration
#

TAG=sports-cloud-v2
NETWORK=sports-cloud-v2-network

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
	sports-cloud-no-network docker-network-inspect
	


# Create Docker image
#
# To distinguish java docker immage from other it need to have
# unique tag, that is provided as argument to build command.
docker-build:
	@docker build --tag=$(TAG) ./dev

docker-network-create:
	@docker network create --driver bridge $(NETWORK)

docker-network-inspect:
	docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}'


# Run Docker container
#
# To launch docker container for cloned java project it directory
# name should be providaded to make as build target. Example:
# $ make sport_api
#
# Container will have git credentials and configuration files of host
# user mounted inside container user directory. As well as, user Gradle 
# directory mounted, so it would not need to re-download all packages
# each time container instance running.
# Projects files would be mounted under "/project".
		
sports-cloud-no-network:
	@docker run --rm \
		--name=$@ \
		-v /root/.m2:/root/.m2 \
	 	-v /root/.ivy2:/root/.ivy2 \
	 	-v /data/apps/content-matcher/feeds:/data/feeds \
		-v /var/log:/var/log \
		-v $(current_path):/project \
		-it $(TAG)

