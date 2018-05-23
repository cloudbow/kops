## Command to geenrate the docker compose file

## Input is defined in INPUT_SERVER_EP env variable which define the rest layer host
## Input can be one of rest, loader, all

set -xe

## Just add the environment variables that is required here. 
## A list can be create by appending TF_VAR_<varname> from variables.tf
 

###
cd descriptors
terraform init
TF_VAR_aws_node_instance_nos=6 TF_VAR_aws_master_instance_nos=1 terraform apply