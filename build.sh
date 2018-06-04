## Command to geenrate the docker compose file

## Input is defined in INPUT_SERVER_EP env variable which define the rest layer host
## Input can be one of rest, loader, all

set -xe
### Assumptions

### Assumes that the data dir contains the public key in the format required
### aws_key_pair_kubernetes.{{CLUSTER_NAME}}-{{FINGER_PRINT_WITHOUT_COLON}}_public_key


## Just add the environment variables that is required here. 
## A list can be create by appending TF_VAR_<varname> from variables.tf

## Don't continue if clustername is not given
if [ -z "${CLUSTER_NAME}" ]
then
    echo "ClusterName is required"
    exit 1
else
    echo "Using clustername ${CLUSTER_NAME}"
fi

## Don't continue if clustername is not given
if [ -z "$1" ]
then
    echo "Please specify the directory to generate the final output"
    echo "The terraform state maintenance will be your responsibility"
    echo "Generally terraform state will be updated in this directory"
    exit 1
else
    echo "Using directory $1"
fi



unameOut="$(uname -s)"
case "${unameOut}" in
    Linux*)     MACHINE=Linux;;
    Darwin*)    MACHINE=Mac;;
    CYGWIN*)    MACHINE=Cygwin;;
    MINGW*)     MACHINE=MinGw;;
    *)          MACHINE="UNKNOWN:${unameOut}"
esac
echo ${MACHINE}

if [ "$MACHINE" == "Mac" ]
then
	GSED_HELP=`gsed --help`
	if [ -z "${GSED_HELP}" ]
	then
	    echo "Install gsed from brew"
	    exit 1
	else
	    echo "Using gsed"
	fi	
else
	alias gsed="sed $@"
	echo "Aliasing sed to gsed"
fi

RENAME_CMD=`which rename`
if [ -z "${RENAME_CMD}" ]
then
    echo "Install rename from brew"
    exit 1
else
    echo "Using rename"
fi

###
GEN_DIR=$1
if [ -z "${GEN_DIR}" ]
then
    echo "Provide the output directory as commandline arg."
    exit 1
else
    echo "Using GEN_DIR ${GEN_DIR}"
fi  

### Delete old files and copy newer one
### Dont change this to rm -rf 
rm -rf "$GEN_DIR/data"


mkdir -p $GEN_DIR/data
cp -r templates/* $GEN_DIR
cd $GEN_DIR



## Replace all cluster name with correct values
FILES=`find . -type f \( ! -iname "terraform.tfstate*"  \)`
while read -r line; do
	gsed -i "s/#cluster_name#/${CLUSTER_NAME}/g" $line
done <<< "$FILES"

## Replace all cluster name hyphenated with correct values
CLUSTER_NAME_HYPHENATED=`echo ${CLUSTER_NAME} | gsed 's/\.k8s\.local/-k8s-local/g'`
while read -r line; do
	gsed -i "s/#cluster_name_hyphenated#/${CLUSTER_NAME_HYPHENATED}/g" $line
done <<< "$FILES"

## Replace all clustername trimmed format
CLUSTER_NAME_HYPHENATED_TRIMMED="$(printf $CLUSTER_NAME_HYPHENATED|cut -c 1-28)"
while read -r line; do
	gsed -i "s/#cluster_name_trimmed_elb#/${CLUSTER_NAME_HYPHENATED_TRIMMED}/g" $line
done <<< "$FILES"

## Replace default region
DEFAULT_REGION="us-east-2"
while read -r line; do
    gsed -i "s/#default_region#/${DEFAULT_REGION}/g" $line
done <<< "$FILES"


## Replace default region
DEFAULT_REGION="us-east-2"
while read -r line; do
    gsed -i "s/#default_region#/${DEFAULT_REGION}/g" $line
done <<< "$FILES"

## Replace all nod einstance types
NODE_INSTANCE_TYPE="m4.large"
while read -r line; do
    gsed -i "s/#default_node_instance_type#/${NODE_INSTANCE_TYPE}/g" $line
done <<< "$FILES"

## Replace all nod einstance types
MASTER_INSTANCE_TYPE="c4.large"
while read -r line; do
    gsed -i "s/#default_master_instance_type#/${MASTER_INSTANCE_TYPE}/g" $line
done <<< "$FILES"



rename 's/#cluster_name#/'$CLUSTER_NAME'/g' data/*.*




## Create S3 state store and generate and update config to S3.
S3_BUCKET=k8s-state-${CLUSTER_NAME_HYPHENATED}
export KOPS_STATE_STORE=s3://${S3_BUCKET}
if aws s3 ls "s3://${S3_BUCKET}" 2>&1 | grep -q 'NoSuchBucket'
then
    aws s3api create-bucket \
        --bucket ${S3_BUCKET} \
        --region ${DEFAULT_REGION}
fi

AVAILABILITY_ZONES="us-east-2a,us-east-2b"


#DEFAULT_NODE_INSTANCE_TYPE#
FIRST_NODE_COUNT=3
KUBERNETES_VERSION="1.8.7"
kops create cluster \
    --zones ${AVAILABILITY_ZONES} \
    --node-size "${NODE_INSTANCE_TYPE}" \
    --master-size "${MASTER_INSTANCE_TYPE}" \
    --kubernetes-version=1.8.7 \
    --node-count=${FIRST_NODE_COUNT} \
    ${CLUSTER_NAME}

kops update cluster ${CLUSTER_NAME} --yes \
--out=/tmp/terraform-"$(date +%F%T)" \
--target=terraform






