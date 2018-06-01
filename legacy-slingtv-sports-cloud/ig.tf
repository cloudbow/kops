resource "aws_internet_gateway" "sports-cloud-k8s-local" {
  vpc_id = "${aws_vpc.sports-cloud-k8s-local.id}"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}
