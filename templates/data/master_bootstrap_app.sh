#!/bin/bash

## This script will be moved to the s3 after replacing. Its here so that templates gets replaced.
## Initial bootstrap commands telnet, jq, stern

apt-get update
apt-get install telnet
apt-get install jq
wget https://github.com/wercker/stern/releases/download/1.5.1/stern_linux_amd64
mv stern_linux_amd64 /opt/stern
ln -s /opt/stern /usr/bin/stern
chmod +x /usr/bin/stern
##kubectl install
curl -LO https://storage.googleapis.com/kubernetes-release/release/$(curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt)/bin/linux/amd64/kubectl && \
chmod +x ./kubectl && \
sudo mv ./kubectl /usr/bin/kubectl
#----helm start------#
## Install helm
wget https://storage.googleapis.com/kubernetes-helm/helm-v2.8.1-linux-amd64.tar.gz
tar -xvf helm-v2.8.1-linux-amd64.tar.gz
cd linux-amd64
mv helm /usr/bin
## Wait for api server to be accessibe and then do helm init
echo "Waiting for port 8080..."
while ! nc -z localhost 8080; do   
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "Api server accessible"
echo "Waiting for kube-system"
while ! kubectl get namespace | grep kube-system | grep Active; do   
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "Kube system alive"
helm init

#----alias start------#
echo '
alias ke="kubectl edit $@"
alias kg="kubectl get $@"
alias kgcm="kubectl get cm $@"
alias kgss="kubectl get statefulsets $@"
alias kgpvc="kubectl get pvc $@"
alias kgpv="kubectl get pv $@"
alias kge="kubectl get events --tail 300 $@"
alias kdrc="kubectl delete rc $@"
alias kgi="kubectl get ingress $@"
alias krs="kubectl rollout status $@"
alias kgss="kubectl get statefulsets $@"
alias kds="kubectl delete service $@"
alias kdj="kubectl delete job $@"
alias ka="kubectl apply -f $@"
alias kdi="kubectl describe ing"
alias kgj="kubectl get jobs"
alias kgc="kubectl get cronjobs"
alias kgs="kubectl get svc"
alias kgd="kubectl get deployments $@"
alias kr="kubectl run -it --image $@"
alias kdd="kubectl delete deployments $@"
alias kdp="kubectl delete po $@"
alias kdcm="kubectl delete cm $@"
alias kdepvc="kubectl delete pvc $@"
alias kdpv="kubectl describe pv $@"
alias kdpv="kubectl delete pv $@"
alias kdpf="kubectl delete po --force --grace-period=0 $@"
alias kdpo="kubectl describe po $@"
alias kgpo="kubectl get po  --sort-by=.status.startTime  $@"
alias kpow="kubectl get po -w  $@"
alias keb="kubectl exec -it $@"
alias kl="kubectl logs $@"
alias klf="kubectl logs --tail 100 $@"
alias kd="kubectl describe $@"
alias drb="docker run -it $@"
alias drmi="docker rmi $@"
alias kall="kubectl get svc,deployments,pods,cronjob,jobs,rs,configmap,pdb,statefulset,daemonset,pvc,sc"
alias kdpo="kubectl describe po $@"
alias kdc="kubectl describe cronjobs $@"' > ~/.bashrc
#----alias end---------#

kubectl rollout status  deployment/tiller-deploy -n kube-system
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-rule --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
kubectl patch deploy --namespace kube-system tiller-deploy -p '{"spec":{"template":{"spec":{"serviceAccount":"tiller"}}}}' 
#------helm end----#
## Generate tls secret for kube registry self signed
openssl req -newkey rsa:4096 -nodes -sha256 -keyout domain.key -x509 -days 3650 -out domain.crt -subj "/C=US/ST=NY/L=NYC/O=SlingMedia/OU=Backend/CN=#docker_registry_host#"
## Get domain.crt
## Get domain.key
echo "Waiting for aws command"
while ! which /usr/local/bin/aws; do   
  sleep 0.1 # wait for 1/10 of the second before check again
done
echo "aws command available"

/usr/local/bin/aws s3 cp s3://k8s-state-#cluster_name_hyphenated#/#cluster_name#/non-kops-docker-state/domain.crt domain.crt
/usr/local/bin/aws s3 cp s3://k8s-state-#cluster_name_hyphenated#/#cluster_name#/non-kops-docker-state/domain.key domain.key

kubectl create secret generic registry-tls-secret --from-file=domain.crt=domain.crt --from-file=domain.key=domain.key
cp domain.crt ca.crt
## Create registry

### Following is done only on master using this script use ansible or other tools to copy this to other nodes
### This assumes that the directory at which openssl is running is same as what is used to run below commands
echo "Copying certificates to master ONLY !! Copying to others has to be done some other way"
mkdir -p /etc/docker/certs.d/#docker_registry_host#:5000
cp -rf ca.crt /etc/docker/certs.d/#docker_registry_host#:5000



### Create kubernetes pod for docker registry
## Apply the configuration
echo '# --------------------- Storage class for aws ------------- #
kind: StorageClass
apiVersion: storage.k8s.io/v1
metadata:
  name: slow
  namespace: default
provisioner: kubernetes.io/aws-ebs
reclaimPolicy: Retain
parameters:
  type: gp2
  zone: us-east-2a
---
# --------------------- Persistent Volume claim for 20Gi for registry ------------- #
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: image-claim
  namespace: default
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 60Gi
  storageClassName: slow
---
# --------------------- Kube Registry Deployment ------------- #
apiVersion: apps/v1beta2 # for versions before 1.8.0 use apps/v1beta1
kind: Deployment
metadata:
  name: kube-registry-v0
  namespace: default
  labels:
    k8s-app: kube-registry
    version: v0
spec:
  replicas: 1
  selector:
    matchLabels:
      k8s-app: kube-registry
  template:
    metadata:
      labels:
        k8s-app: kube-registry
        version: v0
    spec:
      containers:
        - name: registry
          image: registry:2
          resources:
            requests:
              cpu: 100m
              memory: 100Mi
          env:
          - name: REGISTRY_HTTP_ADDR
            value: :5000
          - name: REGISTRY_STORAGE_FILESYSTEM_ROOTDIRECTORY
            value: /var/lib/registry
          - name: REGISTRY_HTTP_TLS_CERTIFICATE
            value: /certs/domain.crt
          - name: REGISTRY_HTTP_TLS_KEY
            value: /certs/domain.key
          volumeMounts:
          - name: image-store
            mountPath: /var/lib/registry
          - name: cert-dir
            mountPath: /certs
          ports:
          - containerPort: 5000
            name: registry
            protocol: TCP
      volumes:
      - name: image-store
        persistentVolumeClaim:
          claimName: image-claim
      - name: cert-dir
        secret:
          secretName: registry-tls-secret
---
# --------------------- Kube Registry service ------------- #
apiVersion: v1
kind: Service
metadata:
  name: kube-registry
  namespace: default
  labels:
    k8s-app: kube-registry
#    kubernetes.io/cluster-service: "true"
    kubernetes.io/name: "KubeRegistry"
spec:
  selector:
    k8s-app: kube-registry
  clusterIP: 100.69.212.32
  type: NodePort
  ports:
  - name: registry
    port: 5000
    protocol: TCP' > /tmp/external-tools.yml
kubectl apply -f /tmp/external-tools.yml
### END Enable Kube registry service ###

### Wait until regisy is available
kubectl rollout status deployments/kube-registry-v0


## Start building builder docker for scala 2.11.8 and push it to docker registry
build-builder-image() 
{ 
  mkdir /tmp/builder-docker
  REQ_SCALA_VERSION=$1
  BUILDER_TAG=$2
  echo "FROM anapsix/alpine-java:8_jdk
ENV SCALA_VERSION=${REQ_SCALA_VERSION}" >> /tmp/builder-docker/Dockerfile
  echo 'ENV SCALA_HOME=/usr/share/scala \
    SBT_VERSION=0.13.11 \
    SBT_HOME=/usr/local/sbt \
    MAVEN_VERSION=3.3.9 \
    MAVEN_HOME=/usr/lib/maven
RUN apk update && \
    apk add bash && \
    apk add make && \
    apk add wget && \
    apk add curl && \
    apk add strace && \
    apk add git && \
    apk add zip && \
    apk add --update coreutils && rm -rf /var/cache/apk/* && \
    apk add --no-cache --virtual=.build-dependencies wget ca-certificates && \
    apk add --no-cache bash && \
    cd "/tmp" && \
    wget -q http://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz -O - | tar xzf - && \
    mv /tmp/apache-maven-$MAVEN_VERSION /usr/lib/maven && \
    curl -sL "http://dl.bintray.com/sbt/native-packages/sbt/$SBT_VERSION/sbt-$SBT_VERSION.tgz" | gunzip | tar -x -C /usr/local && \
    echo -ne "- with sbt $SBT_VERSION\n" >> /root/.built && \
    wget "https://downloads.typesafe.com/scala/${SCALA_VERSION}/scala-${SCALA_VERSION}.tgz" && \
    tar xzf "scala-${SCALA_VERSION}.tgz" && \
    mkdir "${SCALA_HOME}" && \
    rm "/tmp/scala-${SCALA_VERSION}/bin/"*.bat && \
    mv "/tmp/scala-${SCALA_VERSION}/bin" "/tmp/scala-${SCALA_VERSION}/lib" "${SCALA_HOME}" && \
    ln -s "${SCALA_HOME}/bin/"* "/usr/bin/" && \
    apk del .build-dependencies
ENV PATH="$MAVEN_HOME/bin:$SBT_HOME/bin:$SCALA_HOME/bin:$PATH"' >> /tmp/builder-docker/Dockerfile
  ## Set the build context
  cd /tmp/builder-docker
  TAG="${BUILDER_TAG}"
  DOCKER_HUB_USER="#docker_registry_host#:5000"
  docker build --rm --tag=$DOCKER_HUB_USER/$TAG .
  docker push $DOCKER_HUB_USER/$TAG
  rm -rf /tmp/builder-docker
}

build-builder-image "2.11.8" "builder-docker:scala_2.11"
build-builder-image "2.12.2" "builder-docker:scala_2.12"

