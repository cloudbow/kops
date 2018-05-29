#!/usr/bin/env sh
dcos job kill $1 --all
dcos job remove $1
dcos job add $2