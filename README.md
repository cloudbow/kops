Running the project
==================

NOTE: DONT RUN THE COMMANDS HERE WITHOUT LOOKING AT last_built_command.sh . 

OTHERWISE YOU MIGHT END UP IN TEARING DOWN THE CLUSTER OR REDUCING ITS CAPACITY 

ALSO UPDATE THE LAST BUILT COMMAND ONCE YOU UPDATE THE TERRAFORM IMAGE

# How to Run

## Old Sports Cloud Cluster

Currently the script is used only to generate newer clusters. The old cluster is not generated using this generate mechanism described here.

For old cluster we still use terraform . Use the following command

TF_VAR_aws_node_instance_nos=14 TF_VAR_aws_master_instance_nos=1 terraform apply



## Generate the configuration

Note: CLUSTER_NAME SHOULD END WITH .k8s.local
NOTE: Need to be in bash to run the following

```
OUTPUT_DIR="slingtv-sports-cloud"
CLUSTER_NAME="slingtv-sports-cloud.k8s.local" ./build.sh ${OUTPUT_DIR}
cd ${OUTPUT_DIR}
```
## Run the terraform file
NOTE: DONT RUN THIS COMMAND AS IT IS A TEMPLATE. USE THE last_built_command.sh
```
terraform init
TF_VAR_aws_node_instance_nos=<num>  TF_VAR_aws_master_instance_nos=<num> TF_VAR_aws_public_key_pem_path="<key-file>" terraform plan
TF_VAR_aws_node_instance_nos=<num>  TF_VAR_aws_master_instance_nos=<num> TF_VAR_aws_public_key_pem_path="<key-file>" terraform apply
```


## Uses

Currently the script is used only to generate newer clusters. The old cluster is not generated using this generate mechanism described here.
