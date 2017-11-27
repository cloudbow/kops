#!/usr/bin/env sh
BASE_PATH=$1
## Clear first the kube registry services
kubectl delete deploy/kube-registry-v0 svc/kube-registry  --namespace=kube-system
## Apply the configuration
kubectl apply -f $BASE_PATH/config/external-tools.yml