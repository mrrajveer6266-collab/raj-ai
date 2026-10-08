#!/data/data/com.termux/files/usr/bin/bash

set -u

PROJECT="$HOME/raj-ai"
SDK="$HOME/android-sdk"
ANDROID_JAR="$SDK/platforms/android-35/android.jar"
BUILD_TOOLS="$SDK/build-tools/34.0.0"
MANUAL="$PROJECT/android/manual-build"
SRC="$PROJECT/android/app/src/main/java"
RES="$PROJECT/android/app/src/main/res"
MANIFEST="$PROJECT/android/app/src/main/AndroidManifest.xml"
CLASSES="$MANUAL/classes"
DEX="$MANUAL/dex"
APK_UNSIGNED="$MANUAL/RAJ-AI-unsigned.apk"
APK_SIGNED="$MANUAL/RAJ-AI-signed.apk"
KEYSTORE="$MANUAL/raj-ai-debug.keystore"
DOWNLOADS="$HOME/storage/downloads"
FINAL="$DOWNLOADS/RAJ-AI.apk"

cd "$PROJECT" || exit 1

echo "📱 RAJ AI APK BUILD"
echo "==================="

for f in "$ANDROID_JAR" "$MANIFEST"; do
    if [ ! -e "$f" ]; then
        echo "❌ Required file missing: $f"
        exit 1
    fi
done

AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
APKSIGNER="$BUILD_TOOLS/apksigner"

for tool in "$AAPT2" "$D8" "$APKSIGNER"; do
    if [ ! -x "$tool" ]; then
        echo "❌ Build tool missing: $tool"
        exit 1
    fi
done

rm -rf "$CLASSES" "$DEX" "$MANUAL/gen"
mkdir -p "$CLASSES" "$DEX" "$MANUAL/gen"

echo "🧹 Build directory cleaned."

echo "☕ Compiling Java..."

JAVA_FILES=()

while IFS= read -r file; do
    JAVA_FILES+=("$file")
done < <(
    find "$SRC" -type f -name "*.java" \
        ! -name "MainActivity_backup_*.java" \
        -print
)

if [ "${#JAVA_FILES[@]}" -eq 0 ]; then
    echo "❌ No Java source files found."
    exit 1
fi

javac \
    -encoding UTF-8 \
    -source 8 \
    -target 8 \
    -cp "$ANDROID_JAR" \
    -d "$CLASSES" \
    "${JAVA_FILES[@]}"

if [ "$?" -ne 0 ]; then
    echo "❌ Java compilation failed."
    exit 1
fi

echo "✅ Java compiled."

echo "⚙️ Creating resources..."

"$AAPT2" compile \
    --dir "$RES" \
    -o "$MANUAL/resources.zip"

if [ "$?" -ne 0 ]; then
    echo "❌ Resource compilation failed."
    exit 1
fi

echo "🔗 Linking APK..."

"$AAPT2" link \
    -o "$APK_UNSIGNED" \
    --manifest "$MANIFEST" \
    -I "$ANDROID_JAR" \
    --auto-add-overlay \
    --min-sdk-version 23 \
    --target-sdk-version 35 \
    --version-code 1 \
    --version-name 1.0 \
    "$MANUAL/resources.zip"

if [ "$?" -ne 0 ]; then
    echo "❌ APK resource linking failed."
    exit 1
fi

echo "✅ APK linked."

echo "📦 Creating DEX..."

CLASS_JAR="$MANUAL/classes.jar"
rm -f "$CLASS_JAR"

(
    cd "$CLASSES" &&
    jar cf "$CLASS_JAR" .
)

"$D8" \
    --min-api 23 \
    --output "$DEX" \
    "$CLASS_JAR"

if [ "$?" -ne 0 ]; then
    echo "❌ DEX creation failed."
    exit 1
fi

echo "📥 Adding classes.dex..."

cd "$DEX" || exit 1

if [ ! -f classes.dex ]; then
    echo "❌ classes.dex नहीं मिला."
    exit 1
fi

cp classes.dex "$MANUAL/classes.dex"

cd "$PROJECT" || exit 1

(
    cd "$MANUAL" &&
    zip -q -u "$(basename "$APK_UNSIGNED")" classes.dex
)

if [ "$?" -ne 0 ]; then
    echo "❌ classes.dex APK में add नहीं हो पाया."
    exit 1
fi

echo "🔐 Signing APK..."

if [ ! -f "$KEYSTORE" ]; then
    echo "🔑 Creating signing keystore..."

    keytool \
        -genkeypair \
        -keystore "$KEYSTORE" \
        -storepass android \
        -keypass android \
        -alias rajai \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=RAJ AI, OU=RAJ AI, O=RAJ AI, C=IN"
fi

rm -f "$APK_SIGNED"

"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-key-alias rajai \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$APK_SIGNED" \
    "$APK_UNSIGNED"

if [ "$?" -ne 0 ]; then
    echo "❌ APK signing failed."
    exit 1
fi

echo "✅ APK signed."

echo "🔎 Verifying APK..."

"$APKSIGNER" verify --verbose "$APK_SIGNED"

if [ "$?" -ne 0 ]; then
    echo "❌ APK verification failed."
    exit 1
fi

mkdir -p "$DOWNLOADS"
cp "$APK_SIGNED" "$FINAL"

if [ ! -f "$FINAL" ]; then
    echo "❌ Final APK copy failed."
    exit 1
fi

echo
echo "==================="
echo "🎉 APK BUILD COMPLETE"
echo "📱 $FINAL"
echo "==================="
