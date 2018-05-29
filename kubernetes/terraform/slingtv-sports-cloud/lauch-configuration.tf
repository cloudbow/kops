resource "aws_launch_configuration" "master-us-east-2a-masters-slingtv-sports-cloud-k8s-local" {
  name_prefix                 = "master-us-east-2a.masters.slingtv-sports-cloud.k8s.local-"
  image_id                    = "${lookup(var.aws_image_id_per_region, var.aws_region)}"
  instance_type               = "${var.aws_master_instance_type}"
  key_name                    = "${aws_key_pair.kubernetes-keypair-slingtv-sports-cloud-k8s-local.id}"
  iam_instance_profile        = "${aws_iam_instance_profile.masters-slingtv-sports-cloud-k8s-local.id}"
  security_groups             = ["${aws_security_group.masters-slingtv-sports-cloud-k8s-local.id}"]
  associate_public_ip_address = true
  user_data                   = "${file("${path.module}/data/aws_launch_configuration_master-us-east-2a.masters.slingtv-sports-cloud.k8s.local_user_data")}"

  root_block_device = {
    volume_type           = "${var.aws_disk_type}"
    volume_size           = "${var.aws_master_instance_ebs_size}"
    delete_on_termination = true
  }

  lifecycle = {
    create_before_destroy = true
  }

  enable_monitoring = false
}

resource "aws_launch_configuration" "nodes-slingtv-sports-cloud-k8s-local" {
  name_prefix                 = "nodes.slingtv-sports-cloud.k8s.local-"
  image_id                    = "${lookup(var.aws_image_id_per_region, var.aws_region)}"
  instance_type               = "${var.aws_node_instance_type}"
  key_name                    = "${aws_key_pair.kubernetes-keypair-slingtv-sports-cloud-k8s-local.id}"
  iam_instance_profile        = "${aws_iam_instance_profile.nodes-slingtv-sports-cloud-k8s-local.id}"
  security_groups             = ["${aws_security_group.nodes-slingtv-sports-cloud-k8s-local.id}"]
  associate_public_ip_address = true
  user_data                   = "${file("${path.module}/data/aws_launch_configuration_nodes.slingtv-sports-cloud.k8s.local_user_data")}"

  root_block_device = {
    volume_type           = "${var.aws_disk_type}"
    volume_size           = "${var.aws_node_instance_ebs_size}"
    delete_on_termination = true
  }

  lifecycle = {
    create_before_destroy = true
  }

  enable_monitoring = false
}