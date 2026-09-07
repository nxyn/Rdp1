#!/usr/bin/env bash
set -euo pipefail

export ANDROID_HOME="${ANDROID_HOME:-$HOME/Android/Sdk}"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

if [ ! -d "$ANDROID_HOME/platforms/android-35" ]; then
  yes | sdkmanager --licenses >/dev/null || true
  sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
fi

./gradlew assembleDebug --no-daemon
