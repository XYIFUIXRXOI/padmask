#!/usr/bin/env bash
# Build PadMask APK with a local toolchain (no Gradle, no third-party module code).
set -euo pipefail

ROOT=/root/workspace/padmask
SDK=/root/workspace/.sdk
mkdir -p "$SDK"

dl() { # url dest
  if [ -s "$2" ]; then echo "cached $2"; return 0; fi
  python3 - "$1" "$2" <<'PY'
import sys, urllib.request
url, dst = sys.argv[1], sys.argv[2]
urllib.request.urlretrieve(url, dst)
print("downloaded", dst)
PY
}

echo "== toolchain =="
if ! command -v javac >/dev/null 2>&1; then
  apt-get update -qq
  DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-17-jdk-headless unzip zip >/dev/null
fi
javac -version

dl "https://dl.google.com/android/repository/build-tools_r34-linux.zip" "$SDK/bt.zip"
if [ ! -s "$SDK/pf.zip" ]; then
  dl "https://dl.google.com/android/repository/platform-33_r03.zip" "$SDK/pf.zip" \
    || dl "https://dl.google.com/android/repository/platform-33_r02.zip" "$SDK/pf.zip"
fi
rm -rf "$SDK/bt" "$SDK/pf"; mkdir -p "$SDK/bt" "$SDK/pf"
unzip -q -o "$SDK/bt.zip" -d "$SDK/bt"
unzip -q -o "$SDK/pf.zip" -d "$SDK/pf"

BTOOLS=$(dirname "$(find "$SDK/bt" -name aapt2 -type f | head -1)")
ANDROIDJAR=$(find "$SDK/pf" -name android.jar | head -1)
echo "build-tools: $BTOOLS"
echo "android.jar: $ANDROIDJAR"

echo "== compile =="
cd "$ROOT"
rm -rf build; mkdir -p build/stub build/classes build/dex
javac -source 8 -target 8 -nowarn -d build/stub $(find stub -name '*.java')
javac -source 8 -target 8 -nowarn -classpath "build/stub:$ANDROIDJAR" \
      -d build/classes $(find src -name '*.java')

echo "== dex =="
"$BTOOLS/d8" --release --min-api 29 --lib "$ANDROIDJAR" --output build/dex \
      $(find build/classes -name '*.class')

echo "== package =="
"$BTOOLS/aapt" package -f -M AndroidManifest.xml -I "$ANDROIDJAR" -A assets -F build/app.unsigned.apk
python3 - <<'PY'
import zipfile
z = zipfile.ZipFile('/root/workspace/padmask/build/app.unsigned.apk', 'a')
z.write('/root/workspace/padmask/build/dex/classes.dex', 'classes.dex')
z.close()
PY
"$BTOOLS/zipalign" -f 4 build/app.unsigned.apk build/app.aligned.apk

echo "== sign =="
[ -f build/padspoof.jks ] || keytool -genkeypair -keystore build/padspoof.jks -alias padspoof \
  -dname "CN=PadMask, O=DSH" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass padspoof -keypass padspoof -noprompt >/dev/null 2>&1
"$BTOOLS/apksigner" sign --ks build/padspoof.jks --ks-pass pass:padspoof \
  --key-pass pass:padspoof --out build/PadMask-1.0.apk build/app.aligned.apk
"$BTOOLS/apksigner" verify --print-certs build/PadMask-1.0.apk | head -4

mkdir -p /sdcard/Download/DSH-Go
cp -f build/PadMask-1.0.apk /sdcard/Download/DSH-Go/PadMask-1.0.apk
ls -l /sdcard/Download/DSH-Go/PadMask-1.0.apk
echo "DONE"
