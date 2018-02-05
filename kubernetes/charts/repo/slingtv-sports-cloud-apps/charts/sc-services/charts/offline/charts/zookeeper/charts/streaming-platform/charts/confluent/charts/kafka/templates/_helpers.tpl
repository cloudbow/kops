{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}

{{/*
Define here the same zookeeper name as in the original zookeeper
This is to have kafka running in same node as zookeeper
*/}}
{{- define "zookeeper.name" -}}
{{- printf "%s" "zookeeper" -}}
{{- end -}}

{{- define "kafka.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "kafka.fullname" -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
