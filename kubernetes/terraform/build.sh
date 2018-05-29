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

CLUSTER_NAME_HYPHENATED=`echo ${CLUSTER_NAME} | gsed 's/\.k8s\.local/-k8s-local/g'`
while read -r line; do
	gsed -i "s/#cluster_name_hyphenated#/${CLUSTER_NAME_HYPHENATED}/g" $line
done <<< "$FILES"

CLUSTER_NAME_HYPHENATED=`echo ${CLUSTER_NAME} | gsed 's/\.k8s\.local/-k8s-local/g'`
while read -r line; do
	gsed -i "s/#cluster_name_trimmed_elb#/$(printf $CLUSTER_NAME_HYPHENATED|cut -c 1-28)/g" $line
done <<< "$FILES"



rename 's/#cluster_name#/'$CLUSTER_NAME'/g' data/*.*

