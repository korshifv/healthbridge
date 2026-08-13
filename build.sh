#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ANDROID_PROJECT="$ROOT/android"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" ]]; then
  for candidate in /opt/android-sdk "$HOME/Android/Sdk"; do
    if [[ -d "$candidate" ]]; then
      SDK="$candidate"
      break
    fi
  done
fi

if [[ -z "$SDK" || ! -d "$SDK" ]]; then
  echo "Android SDK not found." >&2
  echo "Set ANDROID_HOME (or ANDROID_SDK_ROOT), e.g.:" >&2
  echo "  export ANDROID_HOME=/opt/android-sdk" >&2
  exit 1
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "Gradle not found in PATH (need Gradle 9.5+)." >&2
  exit 1
fi

printf 'sdk.dir=%s\n' "$SDK" > "$ANDROID_PROJECT/local.properties"

echo "Android SDK: $SDK"
gradle -p "$ANDROID_PROJECT" assembleDebug

echo
echo "APK: $ANDROID_PROJECT/app/build/outputs/apk/debug/app-debug.apk"
