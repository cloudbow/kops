output "cluster_name" {
  value = "slingtv-sports-cloud.k8s.local"
}

output "master_security_group_ids" {
  value = ["${aws_security_group.masters-slingtv-sports-cloud-k8s-local.id}"]
}

output "masters_role_arn" {
  value = "${aws_iam_role.masters-slingtv-sports-cloud-k8s-local.arn}"
}

output "masters_role_name" {
  value = "${aws_iam_role.masters-slingtv-sports-cloud-k8s-local.name}"
}

output "elb_security_group_ids" {
  value = ["${aws_security_group.nodes-slingtv-sports-cloud-k8s-local.id}"]
}

output "node_security_group_ids" {
  value = ["${aws_security_group.api-elb-slingtv-sports-cloud-k8s-local.id}"]
}

output "node_subnet_ids" {
  value = ["${aws_subnet.us-east-2a-slingtv-sports-cloud-k8s-local.id}", "${aws_subnet.us-east-2b-slingtv-sports-cloud-k8s-local.id}"]
}

output "nodes_role_arn" {
  value = "${aws_iam_role.nodes-slingtv-sports-cloud-k8s-local.arn}"
}

output "nodes_role_name" {
  value = "${aws_iam_role.nodes-slingtv-sports-cloud-k8s-local.name}"
}

output "region" {
  value = "us-east-2"
}

output "vpc_id" {
  value = "${aws_vpc.slingtv-sports-cloud-k8s-local.id}"
}

