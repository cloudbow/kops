Running the project
==================

## How to get started

### Install a kubernetes cluster

### Run the application

Login to kubernetes node

Install helm

Check out the project

Go to the kubernetes/charts directory and run the following 

helm upgrade  --install  -f ../../config/allenv/values.yaml sc-apps-dev -f ../../config/dev/k8s/per-env-values.yaml -f ../../config/dev/k8s/overridden-values.yaml repo/slingtv-sports-cloud-apps


