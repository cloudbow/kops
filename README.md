Running the project
==================

# How to run 

#

Install docker & do the following. 
NOTE: MAKE SURE THE RM COMMANDS ARE RUN OURSIDE YOUR PROJECT REPO
USER_DOCKER="root"
git clone  https://githsports.slingbox.com/scm/sapi/slingtv-proxy-api.git
git checkout develop
sudo mkdir -p /data/apps/content-matcher/feeds
sudo mkdir -p /data/platforms/solr
sudo mkdir -p $USER_DOCKER/.m2/repository
sudo mkdir -p $USER_DOCKER/.ivy2
sudo chown -R $USER_DOCKER:  /data/apps/content-matcher/feeds
sudo chown -R $USER_DOCKER:  /data/platforms/solr
sudo chown -R $USER_DOCKER: /root/.m2
sudo chown -R $USER_DOCKER: /root/.ivy2
cd slingtv-proxy-api
make docker-build
rm -rf micro-content-matcher sports-cloud-schedulers sports-cloud-parsers && cp -r ../micro-content-matcher . && cp -r ../sports-cloud-schedulers . && cp -r ../sports-cloud-parsers . && make sports-cloud-no-network
make sports-cloud-no-network

# Increase syctl virtual memory
sysctl -w vm.max_map_count=262144
# Run elastic 
make elastic