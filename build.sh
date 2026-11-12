#!/usr/bin/env bash
# Build PadMask APK on-device (arm64): system aapt + Java d8/apksigner from build-tools.
set -euo pipefail

ROOT=/root/workspace/padmask
SDK=/root/workspace/.sdk
BT="$SDK/bt/android-14"
ANDROIDJAR="$SDK/pf/android-13/android.jar"
AAPT=/usr/bin/aapt
OUT=/sdcard/Download/DSH-Go

cd "$ROOT"
[ -x "$AAPT" ] || { echo "缺少 aapt，请先 apt-get install -y aapt"; exit 1; }
[ -f "$ANDROIDJAR" ] || { echo "缺少 android.jar: $ANDROIDJAR"; exit 1; }

echo "== compile =="
rm -rf build; mkdir -p build/stub build/classes build/dex
javac -source 8 -target 8 -nowarn -d build/stub $(find stub -name '*.java')
javac -source 8 -target 8 -nowarn -classpath "build/stub:$ANDROIDJAR" \
      -d build/classes $(find src -name '*.java')

echo "== dex (d8) =="
java -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 --release --min-api 29 \
     --lib "$ANDROIDJAR" --output build/dex $(find build/classes -name '*.class')

echo "== package (aapt) =="
"$AAPT" package -f -M AndroidManifest.xml -I "$ANDROIDJAR" -A assets -F build/app.unsigned.apk
python3 - <<'PY'
import zipfile
p = '/root/workspace/padmask/build/app.unsigned.apk'
z = zipfile.ZipFile(p, 'a')
z.write('/root/workspace/padmask/build/dex/classes.dex', 'classes.dex')
z.close()
print("classes.dex added")
PY

echo "== sign =="
[ -f build/padmask.jks ] || keytool -genkeypair -keystore build/padmask.jks -alias padmask \
  -dname "CN=PadMask, O=DSH" -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass padmask -keypass padmask -noprompt >/dev/null 2>&1
java -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool sign \
  --ks build/padmask.jks --ks-pass pass:padmask --key-pass pass:padmask \
  --out build/PadMask-1.0.apk build/app.unsigned.apk

echo "== verify =="
java -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool verify \
  --print-certs build/PadMask-1.0.apk | head -5

mkdir -p "$OUT"
cp -f build/PadMask-1.0.apk "$OUT/PadMask-1.0.apk"
ls -lh "$OUT/PadMask-1.0.apk"
echo "DONE"
