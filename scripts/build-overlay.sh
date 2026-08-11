#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
: "${ANDROID_JAR:?Set ANDROID_JAR to android.jar}"
: "${ECJ:?Set ECJ to ecj.jar}"
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
java -jar "$ECJ" -source 1.8 -target 1.8 -cp "$ANDROID_JAR" -d "$OUT/classes" $(find "$ROOT/src/main/java" -name '*.java' | sort)
"$D8" --lib "$ANDROID_JAR" --min-api 32 --output "$OUT/java-dex" $(find "$OUT/classes" -name '*.class' | sort)
java -jar "$APKTOOL" b "$OUT/bridge-project" -o "$OUT/bridge.apk"
unzip -p "$OUT/bridge.apk" classes.dex > "$OUT/bridge.dex"
"$D8" --lib "$ANDROID_JAR" --min-api 32 --output "$OUT/merged" "$OUT/java-dex/classes.dex" "$OUT/bridge.dex"
cp "$OUT/merged/classes.dex" "$OUT/blinkmod-overlay.dex"
echo "Built $OUT/blinkmod-overlay.dex"
