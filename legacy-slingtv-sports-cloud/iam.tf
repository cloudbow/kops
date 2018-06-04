resource "aws_iam_instance_profile" "masters-sports-cloud-k8s-local" {
  name = "masters.sports-cloud.k8s.local"
  role = "${aws_iam_role.masters-sports-cloud-k8s-local.name}"
}

resource "aws_iam_instance_profile" "nodes-sports-cloud-k8s-local" {
  name = "nodes.sports-cloud.k8s.local"
  role = "${aws_iam_role.nodes-sports-cloud-k8s-local.name}"
}

resource "aws_iam_role" "masters-sports-cloud-k8s-local" {
  name               = "masters.sports-cloud.k8s.local"
  assume_role_policy = "${file("${path.module}/data/aws_iam_role_masters.sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role" "nodes-sports-cloud-k8s-local" {
  name               = "nodes.sports-cloud.k8s.local"
  assume_role_policy = "${file("${path.module}/data/aws_iam_role_nodes.sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role_policy" "masters-sports-cloud-k8s-local" {
  name   = "masters.sports-cloud.k8s.local"
  role   = "${aws_iam_role.masters-sports-cloud-k8s-local.name}"
  policy = "${file("${path.module}/data/aws_iam_role_policy_masters.sports-cloud.k8s.local_policy")}"
}

resource "aws_iam_role_policy" "nodes-sports-cloud-k8s-local" {
  name   = "nodes.sports-cloud.k8s.local"
  role   = "${aws_iam_role.nodes-sports-cloud-k8s-local.name}"
  policy = "${file("${path.module}/data/aws_iam_role_policy_nodes.sports-cloud.k8s.local_policy")}"
}