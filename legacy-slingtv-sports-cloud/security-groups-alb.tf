resource "aws_security_group" "alb-sports-cloud-k8s-local" {
  name        = "alb-sports-cloud.k8s.local"
  vpc_id      = "${aws_vpc.sports-cloud-k8s-local.id}"
  description = "Security group for alb"
  tags = {
    "ManagedBy"                              = "alb-ingress"
    "Name"                                   = "alb-sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_security_group_rule" "alb-sports-cloud-k8s-local-80-rule" {
  type              = "ingress"
  security_group_id = "${aws_security_group.alb-sports-cloud-k8s-local.id}"
  from_port         = 80
  to_port           = 80
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "alb-sports-cloud-k8s-local-corporate-rule" {
  type              = "ingress"
  security_group_id = "${aws_security_group.alb-sports-cloud-k8s-local.id}"
  from_port         = 0
  to_port           = 65535
  protocol          = "tcp"
  cidr_blocks       = ["182.71.244.110/32", "75.62.122.254/32"]
}

resource "aws_security_group_rule" "alb-sports-cloud-k8s-egress" {
  type              = "egress"
  security_group_id = "${aws_security_group.alb-sports-cloud-k8s-local.id}"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}