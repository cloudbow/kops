# Docker image for Json Proxy Server


FROM ubuntu:16.04

# By default root does not have password, to protect us from acidental
# "root" access (e.q. rm -Rf /), lets set root password as "pw", so it
# need to be entered before accesing root shell.
RUN echo 'root:pw' | chpasswd

# Before we start configuring Node.JS software, first we need to
# update Ubuntu package database, and install common software and
# configuration.
RUN apt-get update
RUN apt-get install -y software-properties-common


# Oracle Java virtual machine software is not available in default
# Ubuntu repositorie, so we need to add 3rd partie one. Plus, oracle
# installer is required to explicitely accept it EULA, with we can do by
# separate script, so this installation can be automated
RUN apt-add-repository ppa:webupd8team/java
RUN apt-get update
RUN echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
RUN apt-get install -y oracle-java8-installer

# Install networking tools
RUN apt-get install -y telnet
RUN apt-get install -y curl
RUN apt-get install -y tar
RUN apt-get install -y wget
RUN wget http://redrockdigimark.com/apachemirror/maven/maven-3/3.5.0/binaries/apache-maven-3.5.0-bin.tar.gz
RUN tar -xvf apache-maven-3.5.0-bin.tar.gz

ENV M2_HOME=$HOME/apache-maven-3.5.0
ENV PATH=$M2_HOME/bin:$PATH


# install minimal editors
RUN apt-get install -y vim



# We need to provide dedicated directory to mount project files
# to. This way we can work launch docker image for all Node.JS based
# project with simple script.
RUN mkdir -p /data/config/json-proxy-server
RUN touch /data/config/json-proxy-server/json-path-file.txt

RUN mkdir /project
WORKDIR /project