resource "aws_ebs_volume" "a-etcd-events-slingtv-sports-cloud-k8s-local" {
  availability_zone = "us-east-2a"
  size              = 20
  type              = "gp2"
  encrypted         = false

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "a.etcd-events.slingtv-sports-cloud.k8s.local"
    "k8s.io/etcd/events"                           = "a/a"
    "k8s.io/role/master"                           = "1"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_ebs_volume" "a-etcd-main-slingtv-sports-cloud-k8s-local" {
  availability_zone = "us-east-2a"
  size              = 20
  type              = "gp2"
  encrypted         = false

  tags = {
    KubernetesCluster                              = "slingtv-sports-cloud.k8s.local"
    Name                                           = "a.etcd-main.slingtv-sports-cloud.k8s.local"
    "k8s.io/etcd/main"                             = "a/a"
    "k8s.io/role/master"                           = "1"
    "kubernetes.io/cluster/slingtv-sports-cloud.k8s.local" = "owned"
  }
}