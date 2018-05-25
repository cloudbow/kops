resource "aws_route_table_association" "us-east-2a-sports-cloud-k8s-local" {
  subnet_id      = "${aws_subnet.us-east-2a-sports-cloud-k8s-local.id}"
  route_table_id = "${aws_route_table.sports-cloud-k8s-local.id}"
}

resource "aws_route_table_association" "us-east-2b-sports-cloud-k8s-local" {
  subnet_id      = "${aws_subnet.us-east-2b-sports-cloud-k8s-local.id}"
  route_table_id = "${aws_route_table.sports-cloud-k8s-local.id}"
}

resource "aws_subnet" "us-east-2a-sports-cloud-k8s-local" {
  vpc_id            = "${aws_vpc.sports-cloud-k8s-local.id}"
  cidr_block        = "172.20.32.0/19"
  availability_zone = "us-east-2a"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "us-east-2a.sports-cloud.k8s.local"
    SubnetType                                     = "Public"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "shared"
    "kubernetes.io/role/alb-ingress"               = ""
    "kubernetes.io/role/elb"                       = "1"
  }
}

resource "aws_subnet" "us-east-2b-sports-cloud-k8s-local" {
  vpc_id            = "${aws_vpc.sports-cloud-k8s-local.id}"
  cidr_block        = "172.20.64.0/19"
  availability_zone = "us-east-2b"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "us-east-2b.sports-cloud.k8s.local"
    SubnetType                                     = "Public"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "shared"
    "kubernetes.io/role/alb-ingress"               = ""
    "kubernetes.io/role/elb"                       = "1"
  }
}