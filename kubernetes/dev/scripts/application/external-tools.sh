#!/usr/bin/env sh
BASE_PATH=$1

### START Enable Kube registry service ###
## Clear first the kube registry services
kubectl delete deploy/kube-registry-v0 svc/kube-registry
## Apply the configuration
kubectl apply -f $BASE_PATH/config/external-tools.yml
### END Enable Kube registry service ###

### Wait until regisy is available
kubectl rollout status deployments/kube-registry-v0
kubectl rollout status svc/kube-registry

### START Builder docker container ####
$BASE_PATH/scripts/application/build-builder.sh $BASE_PATH
### END  build of Builder docker container ####


### Create Empty Jenkins ###