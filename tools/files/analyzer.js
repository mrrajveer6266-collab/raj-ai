const fs = require('fs');
const path = require('path');
const pdfParse = require('pdf-parse');
const mammoth = require('mammoth');

const BASE_DIR = path.resolve(
  process.env.RAJ_AI_FILES_DIR ||
  path.join(process.env.HOME || '.', 'raj-ai-files')
);

function safePath(relativePath = '.') {
  const target = path.resolve(
    BASE_DIR,
    String(relativePath || '.')
  );

  if (
    target !== BASE_DIR &&
    !target.startsWith(BASE_DIR + path.sep)
  ) {
    throw new Error('इस location तक access allowed नहीं है।');
  }

  return target;
}

function extensionOf(filePath) {
  return path.extname(filePath).toLowerCase();
}

async function analyzeFile(relativePath) {
  const file = safePath(relativePath);

  const stat = await fs.promises.stat(file);

  if (!stat.isFile()) {
    throw new Error('यह file नहीं है।');
  }

  const ext = extensionOf(file);
  const buffer = await fs.promises.readFile(file);

  let text = '';

  if (ext === '.pdf') {
    const result = await pdfParse(buffer);
    text = result.text || '';
  } else if (ext === '.docx') {
    const result = await mammoth.extractRawText({
      buffer
    });
    text = result.value || '';
  } else if (
    ext === '.txt' ||
    ext === '.md' ||
    ext === '.csv' ||
    ext === '.json' ||
    ext === '.log'
  ) {
    text = buffer.toString('utf8');
  } else {
    throw new Error(
      `यह file type अभी supported नहीं है: ${ext || 'unknown'}`
    );
  }

  text = text
    .replace(/\r/g, '')
    .trim();

  const MAX_TEXT = 120000;

  if (text.length > MAX_TEXT) {
    text =
      text.substring(0, MAX_TEXT) +
      '\n\n[File का बाकी content बहुत बड़ा है और सीमित किया गया है।]';
  }

  return {
    file: relativePath,
    type: ext || 'text',
    size: stat.size,
    characters: text.length,
    text
  };
}

module.exports = {
  analyzeFile
};
