resource "aws_iam_instance_profile" "masters-slingtv-sports-cloud-k8s-local" {
  name = "masters.slingtv-sports-cloud.k8s.local"
  role = "${aws_iam_role.masters-slingtv-sports-cloud-k8s-local.name}"
}

resource "aws_iam_instance_profile" "nodes-slingtv-sports-cloud-k8s-local" {
  name = "nodes.slingtv-sports-cloud.k8s.local"
  role = "${aws_iam_role.nodes-slingtv-sports-cloud-k8s-local.name}"
}

resource "aws_iam_role" "masters-slingtv-sports-cloud-k8s-local" {
  name               = "masters.slingtv-sports-cloud.k8s.local"
  assume_role_policy = "${file("${path.module}/data/aws_iam_role_masters.slingtv-sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role" "nodes-slingtv-sports-cloud-k8s-local" {
  name               = "nodes.slingtv-sports-cloud.k8s.local"
  assume_role_policy = "${file("${path.module}/data/aws_iam_role_nodes.slingtv-sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role_policy" "masters-slingtv-sports-cloud-k8s-local" {
  name   = "masters.slingtv-sports-cloud.k8s.local"
  role   = "${aws_iam_role.masters-slingtv-sports-cloud-k8s-local.name}"
  policy = "${file("${path.module}/data/aws_iam_role_policy_masters.slingtv-sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role_policy" "nodes-slingtv-sports-cloud-k8s-local" {
  name   = "nodes.slingtv-sports-cloud.k8s.local"
  role   = "${aws_iam_role.nodes-slingtv-sports-cloud-k8s-local.name}"
  policy = "${file("${path.module}/data/aws_iam_role_policy_nodes.slingtv-sports-cloud.k8s.local_policy")}"
}