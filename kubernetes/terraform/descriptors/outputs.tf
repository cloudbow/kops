output "cluster_name" {
  value = "sports-cloud.k8s.local"
}

output "master_security_group_ids" {
  value = ["${aws_security_group.masters-sports-cloud-k8s-local.id}"]
}

output "masters_role_arn" {
  value = "${aws_iam_role.masters-sports-cloud-k8s-local.arn}"
}

output "masters_role_name" {
  value = "${aws_iam_role.masters-sports-cloud-k8s-local.name}"
}

output "elb_security_group_ids" {
  value = ["${aws_security_group.nodes-sports-cloud-k8s-local.id}"]
}

output "node_security_group_ids" {
  value = ["${aws_security_group.api-elb-sports-cloud-k8s-local.id}"]
}

output "node_subnet_ids" {
  value = ["${aws_subnet.us-east-2a-sports-cloud-k8s-local.id}", "${aws_subnet.us-east-2b-sports-cloud-k8s-local.id}"]
}

output "nodes_role_arn" {
  value = "${aws_iam_role.nodes-sports-cloud-k8s-local.arn}"
}

output "nodes_role_name" {
  value = "${aws_iam_role.nodes-sports-cloud-k8s-local.name}"
}

output "region" {
  value = "us-east-2"
}

output "vpc_id" {
  value = "${aws_vpc.sports-cloud-k8s-local.id}"
}

