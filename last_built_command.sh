##-- Build command (RUN ONLY ONCE) ---
CORPORATE_CIDR_BLOCKS='\"182.71.244.110\/32\", \"75.62.122.254\/32\"' \
BASE_PRIVATE_HOSTED_DOMAIN="sports-cloud.com" \
ARTIFACT_SERVER_NAME="artifact-server" \
DOCKER_REGISTRY_NAME="registry" \
CLUSTER_NAME="slingtv-sports-cloud.k8s.local" \
./build.sh slingtv-sports-cloud

##-- Following command can be run as and when scale up , scale down is required --
cd slingtv-sports-cloud

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