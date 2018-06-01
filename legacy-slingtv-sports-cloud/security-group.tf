resource "aws_security_group" "api-elb-sports-cloud-k8s-local" {
  name        = "api-elb.sports-cloud.k8s.local"
  vpc_id      = "${aws_vpc.sports-cloud-k8s-local.id}"
  description = "Security group for api ELB"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "api-elb.sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_security_group" "masters-sports-cloud-k8s-local" {
  name        = "masters.sports-cloud.k8s.local"
  vpc_id      = "${aws_vpc.sports-cloud-k8s-local.id}"
  description = "Security group for masters"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "masters.sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_security_group" "nodes-sports-cloud-k8s-local" {
  name        = "nodes.sports-cloud.k8s.local"
  vpc_id      = "${aws_vpc.sports-cloud-k8s-local.id}"
  description = "Security group for nodes"

  tags = {
    KubernetesCluster                              = "sports-cloud.k8s.local"
    Name                                           = "nodes.sports-cloud.k8s.local"
    "kubernetes.io/cluster/sports-cloud.k8s.local" = "owned"
  }
}

resource "aws_security_group_rule" "all-master-to-master" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
}

resource "aws_security_group_rule" "all-master-to-node" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
}

resource "aws_security_group_rule" "all-node-to-node" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port                = 0
  to_port                  = 0
  protocol                 = "-1"
}

resource "aws_security_group_rule" "api-elb-egress" {
  type              = "egress"
  security_group_id = "${aws_security_group.api-elb-sports-cloud-k8s-local.id}"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "https-api-elb-0-0-0-0--0" {
  type              = "ingress"
  security_group_id = "${aws_security_group.api-elb-sports-cloud-k8s-local.id}"
  from_port         = 443
  to_port           = 443
  protocol          = "tcp"
  cidr_blocks       = ["182.71.244.110/32", "75.62.122.254/32"]
}

resource "aws_security_group_rule" "https-elb-to-master" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.api-elb-sports-cloud-k8s-local.id}"
  from_port                = 443
  to_port                  = 443
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "master-egress" {
  type              = "egress"
  security_group_id = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "node-egress" {
  type              = "egress"
  security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port         = 0
  to_port           = 0
  protocol          = "-1"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "node-to-master-tcp-1-2379" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port                = 1
  to_port                  = 2379
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "node-to-master-tcp-2382-4000" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port                = 2382
  to_port                  = 4000
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "node-to-master-tcp-4003-65535" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port                = 4003
  to_port                  = 65535
  protocol                 = "tcp"
}

resource "aws_security_group_rule" "node-to-master-udp-1-65535" {
  type                     = "ingress"
  security_group_id        = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  source_security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port                = 1
  to_port                  = 65535
  protocol                 = "udp"
}

resource "aws_security_group_rule" "ssh-external-to-master-0-0-0-0--0" {
  type              = "ingress"
  security_group_id = "${aws_security_group.masters-sports-cloud-k8s-local.id}"
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}

resource "aws_security_group_rule" "ssh-external-to-node-0-0-0-0--0" {
  type              = "ingress"
  security_group_id = "${aws_security_group.nodes-sports-cloud-k8s-local.id}"
  from_port         = 22
  to_port           = 22
  protocol          = "tcp"
  cidr_blocks       = ["0.0.0.0/0"]
}