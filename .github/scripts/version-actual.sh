#!/usr/bin/env bash
# Versión publicada más reciente, leída de los tags del repo.
#
# Uso: version-actual.sh <build.gradle.kts>
# Imprime "versionName=..." y "versionCode=...".
#
# publicar-apk.yml crea cada tag anotado con el mensaje "versionCode=N"; la versión
# vigente es la del tag con el versionCode más alto. La versión NO se commitea en
# build.gradle.kts: si todavía no hay ningún tag de ese formato (transición desde
# el esquema viejo), se toma la del archivo recibido.
# Requiere los tags en el clone (checkout con fetch-depth: 0 / git fetch --tags).
set -euo pipefail

archivo="${1:?falta build.gradle.kts}"

ultimo=$(git for-each-ref refs/tags --format='%(refname:short) %(contents:subject)' |
  sed -nE 's/^([^ ]+) versionCode=([0-9]+)$/\2 \1/p' | sort -n | tail -1)

if [ -n "$ultimo" ]; then
  echo "versionName=${ultimo#* }"
  echo "versionCode=${ultimo%% *}"
else
  bash "$(dirname "$0")/version-gradle.sh" leer "$archivo"
fi
