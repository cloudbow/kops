resource "aws_vpc" "sports-cloud-k8s-local" {
  cidr_block           = "172.20.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_vpc_dhcp_options" "sports-cloud-k8s-local" {
  domain_name         = "us-east-2.compute.internal"
  domain_name_servers = ["AmazonProvidedDNS"]

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_vpc_dhcp_options_association" "sports-cloud-k8s-local" {
  vpc_id          = "${aws_vpc.sports-cloud-k8s-local.id}"
  dhcp_options_id = "${aws_vpc_dhcp_options.sports-cloud-k8s-local.id}"
}