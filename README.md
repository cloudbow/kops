Running the project
==================

NOTE: DONT ATTEMPT TO CHANGE THE NO OF NODES OR ANY PARAMETER IN TERRAFORM APPLY


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

```
terraform init
TF_VAR_aws_node_instance_nos=3  TF_VAR_aws_master_instance_nos=1 TF_VAR_aws_public_key_pem_path="/Volumes/Data/Documents/backend/projects/docs/sports cloud/aws_key_pair_kubernetes.sports-cloud-k8s-local-881fbb3c10201b00cf7efbd888c878db_public_key" terraform apply
```


## Uses

Currently the script is used only to generate newer clusters. The old cluster is not generated using this generate mechanism described here.
