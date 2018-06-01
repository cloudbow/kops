resource "aws_vpc" "slingtv-sports-cloud-k8s-local" {
  cidr_block           = "172.20.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "slingtv-sports-cloud.k8s.local"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_vpc_dhcp_options" "slingtv-sports-cloud-k8s-local" {
  domain_name         = "us-east-2.compute.internal"
  domain_name_servers = ["AmazonProvidedDNS"]

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "slingtv-sports-cloud.k8s.local"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_vpc_dhcp_options_association" "slingtv-sports-cloud-k8s-local" {
  vpc_id          = "${aws_vpc.slingtv-sports-cloud-k8s-local.id}"
  dhcp_options_id = "${aws_vpc_dhcp_options.slingtv-sports-cloud-k8s-local.id}"
}

resource "aws_route" "all-slingtv-sports-cloud-k8s-local" {
  route_table_id         = "${aws_route_table.slingtv-sports-cloud-k8s-local.id}"
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = "${aws_internet_gateway.slingtv-sports-cloud-k8s-local.id}"
}

resource "aws_route_table" "slingtv-sports-cloud-k8s-local" {
  vpc_id = "${aws_vpc.slingtv-sports-cloud-k8s-local.id}"

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "slingtv-sports-cloud.k8s.local"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
    "kubernetes.io/kops/role"                      = "public"
  }
}