#!/usr/bin/env sh
BASE_PATH=$1

### START Enable Kube registry service ###
## Clear first the kube registry services
kubectl delete deploy/kube-registry-v0 svc/kube-registry
## Apply the configuration
kubectl apply -f $BASE_PATH/config/external-tools.yml
### END Enable Kube registry service ###


