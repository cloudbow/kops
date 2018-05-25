variable "aws_region" {
  description = "The AWS region to create things in."
  default     = "us-east-2"
}


variable "aws_access_key" {
  default = ""
  description = "the user aws access key"
}
variable "aws_secret_key" {
  default = ""
  description = "the user aws secret key"
}


variable "aws_master_instance_type" {
  default = "c4.large"
  description = "the master instance type"
}


variable "aws_node_instance_type" {
  default = "m4.large"
  description = "the node instance type"
}

variable "aws_master_instance_ebs_size" {
  default = "64"
  description = "the master ebs size"
}


variable "aws_node_instance_ebs_size" {
  default = "128"
  description = "the node ebs size"
}

variable "aws_disk_type" {
  default = "gp2"
  description = "the node ebs size"
}


variable "aws_image_id_per_region" {
  type = "map"
  default = {
    us-east-2 = "ami-a45064c1"
  }
  description = "Image id for aws"
}