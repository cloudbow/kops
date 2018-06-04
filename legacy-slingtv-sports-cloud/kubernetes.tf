/* 
 The provider selection . Can we externalize this to another file? or generate it? 
*/
provider "aws" {
  region = "${var.aws_region}"
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
}

resource "aws_key_pair" "kubernetes-sports-cloud-k8s-local-881fbb3c10201b00cf7efbd888c878db" {
  key_name   = "kubernetes.sports-cloud.k8s.local-88:1f:bb:3c:10:20:1b:00:cf:7e:fb:d8:88:c8:78:db"
  public_key = "${file("${path.module}/data/aws_key_pair_kubernetes.sports-cloud.k8s.local-881fbb3c10201b00cf7efbd888c878db_public_key")}"
}

terraform = {
  required_version = ">= 0.9.3"
}
