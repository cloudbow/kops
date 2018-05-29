#!/usr/bin/env sh
BASE_PATH=$1

sudo mkdir -p /data/apps/sports-cloud/docker-client/security
sudo mkdir -p /data/apps/sports-cloud/docker/security
sudo mkdir -p /data/apps/sports-cloud/docker/registry
sudo mkdir -p /data/apps/sports-cloud/docker/security/registry.marathon.l4lb.thisdcos.directory:5000


## cd to mount dir

cd /data/apps/sports-cloud/docker-client/security

## Create open ssl keys for docker 
openssl req -newkey rsa:4096 -nodes -sha256 -keyout domain.key -x509 -days 3650 -out domain.crt -subj "/C=US/ST=NY/L=NYC/O=SlingMedia/OU=Backend/CN=registry.marathon.l4lb.thisdcos.directory"

# CHANGE THIS BELOW TO THE /etc/docker/certs.d/registry.marathon.l4lb.thisdcos.directory:5000
cp /data/apps/sports-cloud/docker-client/security/domain.crt /data/apps/sports-cloud/docker/security/registry.marathon.l4lb.thisdcos.directory:5000/ca.crt    


## Copy to local docker 
mkdir -p  /etc/docker/certs.d/registry.marathon.l4lb.thisdcos.directory:5000
cp /data/apps/sports-cloud/docker-client/security/domain.crt /etc/docker/certs.d/registry.marathon.l4lb.thisdcos.directory:5000/ca.crt


$BASE_PATH/scripts/dcos/submit-marathon-job.sh $2 $3

