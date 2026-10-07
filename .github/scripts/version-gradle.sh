#!/usr/bin/env bash
# Lee o escribe versionName/versionCode de app/build.gradle.kts.
#
#   version-gradle.sh leer <archivo>                  -> "versionName=..." y "versionCode=..."
#   version-gradle.sh escribir <archivo> <name> <code>
#
# <archivo> puede ser "-" para leer de stdin (p. ej. git show origin/dev:app/build.gradle.kts).
set -euo pipefail

error() { echo "❌ $*" >&2; exit 1; }

accion="${1:?falta acción (leer|escribir)}"
archivo="${2:?falta archivo}"

case "$accion" in
  leer)
    contenido=$(cat "$archivo" | tr -d '\r')
    name=$(sed -nE 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' <<<"$contenido" | head -1)
    code=$(sed -nE 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*([0-9]+).*/\1/p' <<<"$contenido" | head -1)
    [[ -n "$name" ]] || error "no se encontró versionName en $archivo"
    [[ -n "$code" ]] || error "no se encontró versionCode en $archivo"
    echo "versionName=$name"
    echo "versionCode=$code"
    ;;
  escribir)
    name="${3:?falta versionName}"
    code="${4:?falta versionCode}"
    sed -i -E \
      -e "s/^([[:space:]]*versionName[[:space:]]*=[[:space:]]*)\"[^\"]*\"/\1\"$name\"/" \
      -e "s/^([[:space:]]*versionCode[[:space:]]*=[[:space:]]*)[0-9]+/\1$code/" \
      "$archivo"
    # Verificar que realmente quedó escrito (un cambio de formato en el .kts no debe pasar en silencio).
    leido=$(bash "$0" leer "$archivo")
    [[ "$leido" == "versionName=$name"$'\n'"versionCode=$code" ]] ||
      error "no se pudo escribir la versión en $archivo (quedó: $leido)"
    ;;
  *)
    error "acción '$accion' inválida (leer|escribir)"
    ;;
esac
