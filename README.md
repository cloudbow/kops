Running the project
==================

NOTE: DONT RUN THE COMMANDS HERE WITHOUT LOOKING AT last_built_command.sh . 

OTHERWISE YOU MIGHT END UP IN TEARING DOWN THE CLUSTER OR REDUCING ITS CAPACITY 

ALSO UPDATE THE last_built_command.sh ONCE YOU UPDATE THE TERRAFORM IMAGE

# How to Run



## Generate the configuration

Note: CLUSTER_NAME SHOULD END WITH .k8s.local
NOTE: Need to be in bash to run the following

```
OUTPUT_DIR="slingtv-sports-cloud"
CORPORATE_CIDR_BLOCKS='"182.71.244.110/32", "75.62.122.254/32"' \
BASE_PRIVATE_HOSTED_DOMAIN="sports-cloud.com" \
ARTIFACT_SERVER_NAME="artifact-server" \
DOCKER_REGISTRY_NAME="registry" \
CLUSTER_NAME="slingtv-sports-cloud.k8s.local" \
./build.sh ${OUTPUT_DIR}

```
## Run the terraform file
NOTE: DONT RUN THIS COMMAND AS IT IS A TEMPLATE. USE THE last_built_command.sh
```
cd ${OUTPUT_DIR}
terraform init
TF_VAR_aws_node_instance_type="m4.xlarge" \
TF_VAR_aws_node_instance_nos=4  \
TF_VAR_aws_master_instance_nos=1 \
TF_VAR_aws_public_key_pem_path="/Volumes/Data/Documents/backend/projects/docs/sports cloud/aws_key_pair_kubernetes.sports-cloud-k8s-local-881fbb3c10201b00cf7efbd888c878db_public_key" \
terraform plan

TF_VAR_aws_node_instance_type="m4.xlarge" \
TF_VAR_aws_node_instance_nos=4  \
TF_VAR_aws_master_instance_nos=1 \
TF_VAR_aws_public_key_pem_path="/Volumes/Data/Documents/backend/projects/docs/sports cloud/aws_key_pair_kubernetes.sports-cloud-k8s-local-881fbb3c10201b00cf7efbd888c878db_public_key" \
terraform apply

```


## Uses

Currently the script is used only to generate newer clusters. The old cluster is not generated using this generate mechanism described here.
