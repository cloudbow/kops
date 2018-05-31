Running the project
==================

# How to Run

## Generate the configuration

Note: ClusterName should end with .k8s.local
NOTE: Need to be in bash the run the following

CLUSTER_NAME="slingtv-sports-cloud" ./build.sh slingtv-sports-cloud

## Run the terraform file


terraform init
TF_VAR_aws_node_instance_nos=3 TF_VAR_aws_master_instance_nos=1 terraform apply

