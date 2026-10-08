#!/data/data/com.termux/files/usr/bin/bash

set -u

PROJECT="$HOME/raj-ai"
BACKUP="$PROJECT/backups/auto-repair-$(date +%Y%m%d-%H%M%S)"

cd "$PROJECT" || exit 1

echo "🤖 RAJ AI AUTO-REPAIR"
echo "📁 Project: $PROJECT"
echo

mkdir -p "$BACKUP"

echo "💾 Current state backup..."
cp -a core tools server.js package.json package-lock.json android "$BACKUP/" 2>/dev/null || true

echo "🔎 पहले diagnostics..."
node tools/auto-fix/repair.js > "$BACKUP/before.json" 2>&1 || true
cat "$BACKUP/before.json"

if grep -q '"ok": true' "$BACKUP/before.json"; then
    echo
    echo "✅ अभी कोई repair जरूरी नहीं है."
    echo "💾 Backup: $BACKUP"
    exit 0
fi

echo
echo "🧠 AI repair शुरू..."
node tools/auto-fix/ai-repair.js

REPAIR_STATUS=$?

if [ "$REPAIR_STATUS" -ne 0 ]; then
    echo
    echo "❌ AI repair fail हुआ."
    echo "↩️ Original code सुरक्षित है."
    echo "💾 Backup: $BACKUP"
    exit 2
fi

echo
echo "🧪 Repair के बाद diagnostics..."
node tools/auto-fix/repair.js > "$BACKUP/after.json" 2>&1 || true
cat "$BACKUP/after.json"

if grep -q '"ok": true' "$BACKUP/after.json"; then
    echo
    echo "✅ AI repair सफल रहा."
    echo "💾 Backup: $BACKUP"
    echo "🚀 Repair validated."
    exit 0
fi

echo
echo "❌ AI repair validation में fail हुआ."
echo "↩️ Rollback शुरू..."

rm -rf core tools server.js package.json package-lock.json android

cp -a "$BACKUP/core" .
cp -a "$BACKUP/tools" .
cp -a "$BACKUP/server.js" .
cp -a "$BACKUP/package.json" .
cp -a "$BACKUP/package-lock.json" .
cp -a "$BACKUP/android" .

echo
echo "↩️ Rollback complete."
echo "💾 Backup: $BACKUP"
exit 3
