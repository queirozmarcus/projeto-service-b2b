{{/*
Helpers compartilhados para o chart scopeflow-api
*/}}

{{/* Nome completo do chart */}}
{{- define "scopeflow-api.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Nome completo com release */}}
{{- define "scopeflow-api.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/* Labels padrão Kubernetes */}}
{{- define "scopeflow-api.labels" -}}
helm.sh/chart: {{ include "scopeflow-api.chart" . }}
{{ include "scopeflow-api.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/* Selector labels */}}
{{- define "scopeflow-api.selectorLabels" -}}
app.kubernetes.io/name: {{ include "scopeflow-api.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/* Chart label */}}
{{- define "scopeflow-api.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/* Nome da ServiceAccount */}}
{{- define "scopeflow-api.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "scopeflow-api.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}
