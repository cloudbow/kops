#!/usr/bin/env sh
BASE_PATH=$1

## Assumes you have installed jq . Please install jq otherwisee
## Run this on the machine where cluster is created
## This is required to be there for elb to auto detect the subnets to use when creating ingress controller
## This needs to be done after cluster is created and before any application is run
aws ec2 describe-subnets | jq -r -c '.Subnets[] | .SubnetId'| while read i; do 
  echo "Adding tag kubernetes.io/cluster/sports-cloud.k8s.local,Value=shared for subnet $i"
  aws ec2 create-tags --resources $i --tags Key=kubernetes.io/cluster/sports-cloud.k8s.local,Value=shared
  echo "Adding tag Key=kubernetes.io/role/alb-ingress,Value= for subnet $i"
  aws ec2 create-tags --resources $i --tags Key=kubernetes.io/role/alb-ingress,Value=
done

### Add private hosted zone for vpc
VPC_ID=`aws ec2 describe-vpcs |jq -r '[.Vpcs[]] | .[] | select(.Tags[].Key | contains("KubernetesCluster")) | .VpcId'`
aws route53 create-hosted-zone --name sports-cloud.com --vpc VPCRegion=`aws configure get region`,VPCId=$VPC_ID --caller-reference `date +%F%T` --hosted-zone-config Comment="Sports Cloud Internal zone"

### Add A record for artifact server and registry inside kubernetes
ZONE_ID=`aws route53 list-hosted-zones | jq -r '[.HostedZones[]] | .[] | select(.Name | contains("sports-cloud.com.")) | .Id' | cut -d'/' -f3`
aws route53 change-resource-record-sets --hosted-zone-id $ZONE_ID --change-batch file://$BASE_PATH/scripts/aws/config/artifact-server-record.json
aws route53 change-resource-record-sets --hosted-zone-id $ZONE_ID --change-batch file://$BASE_PATH/scripts/aws/config/registry-server-record.json