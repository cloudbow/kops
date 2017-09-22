Running the project
==================

# How to run 

Install docker & do the following. 
NOTE: MAKE SURE THE RM COMMANDS ARE RUN OURSIDE YOUR PROJECT REPO

git clone  https://githsports.slingbox.com/scm/sapi/slingtv-proxy-api.git
sudo mkdir -p /data/apps/content-matcher/feeds
sudo mkdir -p /data/platforms/elastic
sudo chown $USER: -R /data/apps/content-matcher/feeds
sudo chown $USER: -R /data/platforms/elastic
cd Docker-MicroContentMatcher
# Run docker build
make docker-build
# Run actual docker instance
rm -rf micro-content-matcher sports-cloud-schedulers sports-cloud-parsers && cp -r ../micro-content-matcher . && cp -r ../sports-cloud-schedulers . && cp -r ../sports-cloud-parsers . && make sports-cloud-no-network
# Increase syctl virtual memory
sysctl -w vm.max_map_count=262144
# Run elastic 
make elastic