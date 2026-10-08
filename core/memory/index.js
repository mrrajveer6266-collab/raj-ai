const fs = require('fs');
const path = require('path');

const MEMORY_DIR = path.resolve(
  process.env.RAJ_AI_MEMORY_DIR ||
  path.join(process.env.HOME || '.', 'raj-ai-memory')
);

const MEMORY_FILE = path.join(MEMORY_DIR, 'memory.json');

async function ensureMemory() {
  await fs.promises.mkdir(MEMORY_DIR, { recursive: true });

  try {
    await fs.promises.access(MEMORY_FILE);
  } catch {
    await fs.promises.writeFile(
      MEMORY_FILE,
      JSON.stringify([], null, 2),
      'utf8'
    );
  }
}

async function loadMemory() {
  await ensureMemory();

  const raw = await fs.promises.readFile(MEMORY_FILE, 'utf8');

  try {
    const data = JSON.parse(raw);
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

async function saveMemory(items) {
  await ensureMemory();

  await fs.promises.writeFile(
    MEMORY_FILE,
    JSON.stringify(items, null, 2),
    'utf8'
  );
}

async function remember(text, type = 'note') {
  const value = String(text || '').trim();

  if (!value) {
    throw new Error('Memory text खाली है।');
  }

  const items = await loadMemory();

  items.push({
    id: Date.now().toString(),
    type: String(type || 'note'),
    text: value,
    createdAt: new Date().toISOString()
  });

  await saveMemory(items);

  return 'Memory save हो गई।';
}

async function getMemories(limit = 20) {
  const items = await loadMemory();
  const safeLimit = Math.max(1, Math.min(Number(limit) || 20, 100));

  return items.slice(-safeLimit);
}

module.exports = {
  MEMORY_DIR,
  MEMORY_FILE,
  remember,
  getMemories
};
