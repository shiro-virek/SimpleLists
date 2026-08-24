#!/usr/bin/env bash
# Compila, instala en el teléfono conectado y abre la app
set -e
cd "$(dirname "$0")"

./gradlew assembleRelease -q

adb install -r app/build/outputs/apk/release/app-release.apk

# Abre la app en el teléfono
adb shell monkey -p com.simplelists.app -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1 || true

echo "Instalado y abierto"
