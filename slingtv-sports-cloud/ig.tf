resource "aws_internet_gateway" "slingtv-sports-cloud-k8s-local" {
  vpc_id = "${aws_vpc.slingtv-sports-cloud-k8s-local.id}"

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "slingtv-sports-cloud.k8s.local"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
  }
}
