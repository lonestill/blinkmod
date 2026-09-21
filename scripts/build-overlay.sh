#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -z "${JAVA_HOME:-}" ] && [ -d "/opt/homebrew/opt/openjdk@17" ] && export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"

ANDROID_JAR="${ANDROID_JAR:-/opt/homebrew/share/android-commandlinetools/platforms/android-35/android.jar}"
D8="${D8:-/opt/homebrew/share/android-commandlinetools/build-tools/35.0.0/d8}"
APKTOOL="${APKTOOL:-/Users/a123/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu/android-build-tools/apktool.jar}"

: "${ANDROID_JAR:?Set ANDROID_JAR to android.jar}"
: "${D8:?Set D8 to the d8 executable}"
: "${APKTOOL:?Set APKTOOL to apktool.jar}"
OUT="${OUT:-$ROOT/build}"
mkdir -p "$OUT/classes" "$OUT/java-dex" "$OUT/bridge-project/smali/com/blinkmap/mod" "$OUT/merged"
cp "$ROOT/src/main/smali/com/blinkmap/mod/WorkBridge.smali" "$OUT/bridge-project/smali/com/blinkmap/mod/"
cat > "$OUT/bridge-project/apktool.yml" <<YML
version: 3.0.3
apkFileName: bridge.apk
usesFramework:
  ids:
  - 1
sdkInfo:
  minSdkVersion: 32
doNotCompress:
- dex
YML
cat > "$OUT/bridge-project/AndroidManifest.xml" <<XML
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="app.blinkmod"><uses-sdk android:minSdkVersion="32" /></manifest>
XML
if [ -n "${ECJ:-}" ] && [ -f "${ECJ:-}" ]; then
    java -jar "$ECJ" -source 1.8 -target 1.8 -cp "$ANDROID_JAR" -d "$OUT/classes" $(find "$ROOT/src/main/java" -name '*.java' | sort)
else
    JAVAC_BIN="${JAVAC:-/opt/homebrew/opt/openjdk@17/bin/javac}"
    "$JAVAC_BIN" --release 8 -cp "$ANDROID_JAR" -d "$OUT/classes" $(find "$ROOT/src/main/java" -name '*.java' | sort)
fi
"$D8" --lib "$ANDROID_JAR" --min-api 32 --output "$OUT/java-dex" $(find "$OUT/classes" -name '*.class' | sort)
java -jar "$APKTOOL" b "$OUT/bridge-project" -o "$OUT/bridge.apk"
unzip -p "$OUT/bridge.apk" classes.dex > "$OUT/bridge.dex"
"$D8" --lib "$ANDROID_JAR" --min-api 32 --output "$OUT/merged" "$OUT/java-dex/classes.dex" "$OUT/bridge.dex"
cp "$OUT/merged/classes.dex" "$OUT/blinkmod-overlay.dex"
echo "Built $OUT/blinkmod-overlay.dex"
