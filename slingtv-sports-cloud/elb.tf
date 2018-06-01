resource "aws_elb" "api-slingtv-sports-cloud-k8s-local" {
  name = "api-slingtv-sports-cloud-k8s-loc"

  listener = {
    instance_port     = 443
    instance_protocol = "TCP"
    lb_port           = 443
    lb_protocol       = "TCP"
  }

  security_groups = ["${aws_security_group.api-elb-slingtv-sports-cloud-k8s-local.id}"]
  subnets         = ["${aws_subnet.us-east-2a-slingtv-sports-cloud-k8s-local.id}", "${aws_subnet.us-east-2b-slingtv-sports-cloud-k8s-local.id}"]

  health_check = {
    target              = "SSL:443"
    healthy_threshold   = 2
    unhealthy_threshold = 2
    interval            = 10
    timeout             = 5
  }

  idle_timeout = 300

  tags = {
    KubernetesCluster = "slingtv-sports-cloud.k8s.local"
    Name              = "api.slingtv-sports-cloud.k8s.local"
  }
}