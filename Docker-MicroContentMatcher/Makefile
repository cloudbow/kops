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
	elastic sports-cloud-no-network docker-network-inspect
	


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

elastic:
	@docker exec -u elasticsearch:elasticsearch \
                     -d \
                     -t sports-cloud-no-network \
                     /elastic-base/scripts/elastic-start.sh
		
sports-cloud-no-network:
	@docker run --rm \
		--name=$@ \
            -p 9080:9080 \
	    -p 8983:8983 \
	    -p 4040:4040 \
		-v /root/.m2:/root/.m2 \
	 	-v /root/.ivy2:/root/.ivy2 \
	 	-v /data/apps/content-matcher/feeds:/data/feeds \
	 	-v /data/platforms/elastic:/data/elastic \
		-v /var/log:/var/log \
		-v $(current_path)/sports-cloud-schedulers/src/main/resources:/spark-log4j-config \
		-v $(current_path):/project \
		-it --cap-add=SYS_PTRACE --security-opt=apparmor:unconfined $(TAG)

