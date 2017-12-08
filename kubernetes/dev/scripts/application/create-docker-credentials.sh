#!/usr/bin/env sh
kubectl delete secret registry-tls-secret 
openssl req -newkey rsa:4096 -nodes -sha256 -keyout domain.key -x509 -days 3650 -out domain.crt -subj "/C=US/ST=NY/L=NYC/O=SlingMedia/OU=Backend/CN=registry.sports-cloud.com"
kubectl create secret generic registry-tls-secret --from-file=domain.crt=domain.crt --from-file=domain.key=domain.key
cp domain.crt ca.crt

### Following is done only on master using this script use ansible or other tools to copy this to other nodes
### This assumes that the directory at which openssl is running is same as what is used to run below commands
echo "Copying certificates to master ONLY !!"
mkdir -p /etc/docker/certs.d/registry.sports-cloud.com:5000
cp -rf ca.crt /etc/docker/certs.d/registry.sports-cloud.com:5000
