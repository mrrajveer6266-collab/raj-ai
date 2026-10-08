#!/data/data/com.termux/files/usr/bin/bash

set -u

PROJECT="$HOME/raj-ai"
BACKUP="$PROJECT/backups/rajfix-$(date +%Y%m%d-%H%M%S)"

cd "$PROJECT" || exit 1

echo "🤖 RAJ FIX शुरू..."
echo "📁 Project: $PROJECT"

mkdir -p "$BACKUP"

echo "💾 Backup बना रहा हूँ..."
cp -a core tools server.js package.json android "$BACKUP/" 2>/dev/null || true

echo "🔍 Git status..."
git status --short 2>/dev/null || true

echo
echo "🧪 JavaScript checks..."

FAILED=0

while IFS= read -r file; do
    if ! node --check "$file" >/dev/null 2>&1; then
        echo "❌ JS ERROR: $file"
        node --check "$file" || true
        FAILED=1
    fi
done < <(find core tools -type f -name "*.js" 2>/dev/null)

if [ -f server.js ]; then
    if ! node --check server.js >/dev/null 2>&1; then
        echo "❌ JS ERROR: server.js"
        node --check server.js || true
        FAILED=1
    fi
fi

echo
echo "🧪 Android Java check..."

if [ -d android/app/src/main/java ]; then
    rm -rf android/manual-build/classes
    mkdir -p android/manual-build/classes

    if ! javac \
        -encoding UTF-8 \
        -source 8 \
        -target 8 \
        -cp "$HOME/android-sdk/platforms/android-35/android.jar" \
        -d android/manual-build/classes \
        $(find android/app/src/main/java -name "*.java" -print) \
        >android/manual-build/rajfix-java.log 2>&1; then

        echo "❌ Android Java ERROR"
        cat android/manual-build/rajfix-java.log
        FAILED=1
    else
        echo "✅ Android Java OK"
    fi
fi

echo
echo "📦 package.json check..."

if [ -f package.json ]; then
    if node -e "JSON.parse(require('fs').readFileSync('package.json','utf8'))" >/dev/null 2>&1; then
        echo "✅ package.json OK"
    else
        echo "❌ package.json ERROR"
        cat package.json
        FAILED=1
    fi
fi

echo
echo "--------------------------------"

if [ "$FAILED" -eq 0 ]; then
    echo
    echo "🤖 Repair engine diagnostic..."
    node tools/auto-fix/repair.js

    if [ "$?" -ne 0 ]; then
        echo "⚠️ Repair engine ने समस्या detect की."
        exit 2
    fi

    echo "✅ सभी diagnostics पास."
    echo "💾 Backup: $BACKUP"
    echo
echo "📱 APK BUILD शुरू..."
echo "===================="

if bash "$PROJECT/tools/auto-fix/build-apk.sh"; then
    echo "✅ APK build सफल"
else
    echo "❌ APK build failed"
    exit 1
fi

echo "===================="
echo "🎉 RAJ FIX + APK BUILD COMPLETE"

echo "🚀 RAJ FIX CHECK COMPLETE"

    echo
    echo "🛠️ Safe AI Auto-Repair check..."
    tools/auto-fix/auto-repair.sh

    AUTO_STATUS=$?

    if [ "$AUTO_STATUS" -ne 0 ]; then
        echo "⚠️ Auto-Repair ने समस्या report की."
        exit "$AUTO_STATUS"
    fi

    echo
    echo "🎉 RAJ FIX COMPLETE"
    exit 0
fi

echo "⚠️ Error मिला है."
echo
echo "❌ Auto-fix ने अभी code को blindly change नहीं किया."
echo "💾 Safe backup: $BACKUP"
echo "🔎 ऊपर दिखाया गया error ही अगला fix target है."

exit 2
