resource "aws_route53_zone" "slingtv-sports-cloud-k8s-local" {
  name = "sports-cloud.com"
  comment = "slingtv-sports-cloud.k8s.local Internal zone"
  vpc_id = "${aws_vpc.slingtv-sports-cloud-k8s-local.id}"
  vpc_region = "${var.aws_region}"
}

resource "aws_route53_record" "slingtv-sports-cloud-k8s-local-artifact-server" {
  zone_id = "${aws_route53_zone.slingtv-sports-cloud-k8s-local.id}"
  name    = "artifact-server.sports-cloud.com"
  type    = "A"
  ttl     = "300"
  records = ["100.68.16.196"]
}

resource "aws_route53_record" "slingtv-sports-cloud-k8s-local-docker-registry-server" {
  zone_id = "${aws_route53_zone.slingtv-sports-cloud-k8s-local.id}"
  name    = "registry.sports-cloud.com"
  type    = "A"
  ttl     = "300"
  records = ["100.69.212.32"]
}