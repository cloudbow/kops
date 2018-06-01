resource "aws_autoscaling_attachment" "master-us-east-2a-masters-slingtv-sports-cloud-k8s-local" {
  elb                    = "${aws_elb.api-slingtv-sports-cloud-k8s-local.id}"
  autoscaling_group_name = "${aws_autoscaling_group.master-us-east-2a-masters-slingtv-sports-cloud-k8s-local.id}"
}

resource "aws_autoscaling_group" "master-us-east-2a-masters-slingtv-sports-cloud-k8s-local" {
  name                 = "master-us-east-2a.masters.slingtv-sports-cloud.k8s.local"
  launch_configuration = "${aws_launch_configuration.master-us-east-2a-masters-slingtv-sports-cloud-k8s-local.id}"
  max_size             = "${var.aws_master_instance_nos}"
  min_size             = "${var.aws_master_instance_nos}"
  vpc_zone_identifier  = ["${aws_subnet.us-east-2a-slingtv-sports-cloud-k8s-local.id}"]

  tag = {
    key                 = "KubernetesCluster"
    value               = "slingtv-sports-cloud.k8s.local"
    propagate_at_launch = true
  }

  tag = {
    key                 = "Name"
    value               = "master-us-east-2a.masters.slingtv-sports-cloud.k8s.local"
    propagate_at_launch = true
  }

  tag = {
    key                 = "k8s.io/cluster-autoscaler/node-template/label/kops.k8s.io/instancegroup"
    value               = "master-us-east-2a"
    propagate_at_launch = true
  }

  tag = {
    key                 = "k8s.io/role/master"
    value               = "1"
    propagate_at_launch = true
  }

  metrics_granularity = "1Minute"
  enabled_metrics     = ["GroupDesiredCapacity", "GroupInServiceInstances", "GroupMaxSize", "GroupMinSize", "GroupPendingInstances", "GroupStandbyInstances", "GroupTerminatingInstances", "GroupTotalInstances"]
}

resource "aws_autoscaling_group" "nodes-slingtv-sports-cloud-k8s-local" {
  name                 = "nodes.slingtv-sports-cloud.k8s.local"
  launch_configuration = "${aws_launch_configuration.nodes-slingtv-sports-cloud-k8s-local.id}"
  max_size             = "${var.aws_node_instance_nos}"
  min_size             = "${var.aws_node_instance_nos}"
  vpc_zone_identifier  = ["${aws_subnet.us-east-2a-slingtv-sports-cloud-k8s-local.id}", "${aws_subnet.us-east-2b-slingtv-sports-cloud-k8s-local.id}"]

  tag = {
    key                 = "KubernetesCluster"
    value               = "slingtv-sports-cloud.k8s.local"
    propagate_at_launch = true
  }

  tag = {
    key                 = "Name"
    value               = "nodes.slingtv-sports-cloud.k8s.local"
    propagate_at_launch = true
  }

  tag = {
    key                 = "k8s.io/cluster-autoscaler/node-template/label/kops.k8s.io/instancegroup"
    value               = "nodes"
    propagate_at_launch = true
  }

  tag = {
    key                 = "k8s.io/role/node"
    value               = "1"
    propagate_at_launch = true
  }

  metrics_granularity = "1Minute"
  enabled_metrics     = ["GroupDesiredCapacity", "GroupInServiceInstances", "GroupMaxSize", "GroupMinSize", "GroupPendingInstances", "GroupStandbyInstances", "GroupTerminatingInstances", "GroupTotalInstances"]
}