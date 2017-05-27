## kops create

Create a resource by command line, filename or stdin.

### Synopsis


Create a resource:
  
  * cluster  
  * instancegroup  
  * secret  
  * federation  

Create a cluster, instancegroup or secret using command line flags or YAML cluster spec. Clusters and instancegroups can be created using the YAML cluster spec.

```
kops create -f FILENAME
```

### Examples

```
  # Create a cluster using a cluser spec file
  kops create -f my-cluster.yaml
  
  # Create a cluster in AWS
  kops create cluster --name=kubernetes-cluster.example.com \
  --state=s3://kops-state-1234 --zones=eu-west-1a \
  --node-count=2 --node-size=t2.micro --master-size=t2.micro \
  --dns-zone=example.com
  
  # Create an instancegroup for the k8s-cluster.example.com cluster.
  kops create ig --name=k8s-cluster.example.com node-example \
  --role node --subnet 172.16.32.1/24
  
  # Create an new ssh public key called admin.
  kops create secret sshpublickey admin -i ~/.ssh/id_rsa.pub \
  --name k8s-cluster.example.com --state s3://example.com
```

### Options

```
  -f, --filename stringSlice   Filename to use to create the resource
```

### Options inherited from parent commands

```
      --alsologtostderr                  log to standard error as well as files
      --config string                    config file (default is $HOME/.kops.yaml)
      --log_backtrace_at traceLocation   when logging hits line file:N, emit a stack trace (default :0)
      --log_dir string                   If non-empty, write log files in this directory
      --logtostderr                      log to standard error instead of files (default false)
      --name string                      Name of cluster
      --state string                     Location of state storage
      --stderrthreshold severity         logs at or above this threshold go to stderr (default 2)
  -v, --v Level                          log level for V logs
      --vmodule moduleSpec               comma-separated list of pattern=N settings for file-filtered logging
```

### SEE ALSO
* [kops](kops.md)	 - kops is Kubernetes ops.
* [kops create cluster](kops_create_cluster.md)	 - Create a Kubernetes cluster.
* [kops create instancegroup](kops_create_instancegroup.md)	 - Create an instancegroup.
* [kops create secret](kops_create_secret.md)	 - Create a secret.
