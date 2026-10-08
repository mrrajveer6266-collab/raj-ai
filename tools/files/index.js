const fs = require('fs');
const path = require('path');

const { analyzeFile } = require('./analyzer');

const BASE_DIR = path.resolve(process.env.RAJ_AI_FILES_DIR || path.join(process.env.HOME || '.', 'raj-ai-files'));

function safePath(relativePath = '.') {
  const target = path.resolve(BASE_DIR, String(relativePath || '.'));
  if (target !== BASE_DIR && !target.startsWith(BASE_DIR + path.sep)) {
    throw new Error('इस location तक access allowed नहीं है।');
  }
  return target;
}

async function listFiles(relativePath = '.') {
  const dir = safePath(relativePath);
  const entries = await fs.promises.readdir(dir, { withFileTypes: true });

  return entries.map(entry => ({
    name: entry.name,
    type: entry.isDirectory() ? 'folder' : 'file'
  }));
}

async function readText(relativePath) {
  const file = safePath(relativePath);
  const stat = await fs.promises.stat(file);

  if (!stat.isFile()) {
    throw new Error('यह file नहीं है।');
  }

  return fs.promises.readFile(file, 'utf8');
}

async function writeText(relativePath, content = '') {
  const file = safePath(relativePath);
  await fs.promises.mkdir(path.dirname(file), { recursive: true });
  await fs.promises.writeFile(file, String(content), 'utf8');

  return `File बनाई गई: ${relativePath}`;
}

module.exports = {
  BASE_DIR,
  listFiles,
  readText,
  writeText,
  analyzeFile
};
