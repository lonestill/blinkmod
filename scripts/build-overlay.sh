#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
[ -z "${JAVA_HOME:-}" ] && [ -d "/opt/homebrew/opt/openjdk@17" ] && export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
[ -n "${JAVA_HOME:-}" ] && export PATH="$JAVA_HOME/bin:$PATH"

FLAVOR="${1:-dev}"
case "$FLAVOR" in
    public|release)
        IS_DEV=false
        echo "==> Configuring flavor: PUBLIC / RELEASE (Safe tourism, hidden Top-1 sniper, hidden Blink Lab)"
        ;;
    *)
        IS_DEV=true
        echo "==> Configuring flavor: DEV (Full access: Top-1 Sniper, 230 countries raid, Blink Lab API)"
        ;;
esac

# Update ModConfig.java IS_DEV_BUILD flag
MOD_CONFIG="$ROOT/src/main/java/com/blinkmap/mod/ModConfig.java"
if [ -f "$MOD_CONFIG" ]; then
    sed -i '' "s/public static final boolean IS_DEV_BUILD = .*/public static final boolean IS_DEV_BUILD = $IS_DEV;/" "$MOD_CONFIG"
fi

ANDROID_JAR="${ANDROID_JAR:-/opt/homebrew/share/android-commandlinetools/platforms/android-35/android.jar}"
D8="${D8:-/opt/homebrew/share/android-commandlinetools/build-tools/35.0.0/d8}"
APKTOOL="${APKTOOL:-/Users/a123/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu/android-build-tools/apktool.jar}"

: "${ANDROID_JAR:?Set ANDROID_JAR to android.jar}"
: "${D8:?Set D8 to the d8 executable}"
: "${APKTOOL:?Set APKTOOL to apktool.jar}"
OUT="${OUT:-$ROOT/build}"
rm -rf "$OUT/classes" "$OUT/java-dex" "$OUT/merged"
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

# Automatic sync into decompiled source for immediate packaging
APK_SRC="/Users/a123/Desktop/AI_Agent/blink_from_gitty/blink_mod_menu/blinkmod_v1.1_src"
if [ -d "$APK_SRC" ]; then
    echo "==> Syncing compiled smali and assets to $APK_SRC..."
    SYNC_TMP="/tmp/overlay_sync_$$"
    rm -rf "$SYNC_TMP" && mkdir -p "$SYNC_TMP"
    cp "$OUT/blinkmod-overlay.dex" "$SYNC_TMP/classes.dex"
    zip -q -j "$SYNC_TMP/overlay.apk" "$SYNC_TMP/classes.dex"
    java -jar "$APKTOOL" d "$SYNC_TMP/overlay.apk" -o "$SYNC_TMP/decoded" -f >/dev/null 2>&1
    cp -r "$SYNC_TMP/decoded/smali/com/blinkmap/mod/"* "$APK_SRC/smali_classes9/com/blinkmap/mod/"
    if [ -d "$ROOT/src/main/assets" ]; then
        cp "$ROOT/src/main/assets/"* "$APK_SRC/assets/"
    fi
    rm -rf "$SYNC_TMP"
    echo "==> Smali and assets successfully synced to blinkmod_v1.1_src!"
fi
