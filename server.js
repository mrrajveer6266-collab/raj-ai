const crypto = require('crypto');
const path = require('path');
const fs = require('fs');
require('dotenv').config();

const http = require('http');
const { runAction } = require('./core/action-runner');
const { analyzeFile } = require('./tools/files/analyzer');
const { askAI, askAIWithImage } = require('./core/ai');

const PORT = Number(process.env.PORT) || 8787;

function sendJson(res, status, data) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8'
  });
  res.end(JSON.stringify(data));
}

function readBody(req, maxBytes = 10 * 1024 * 1024) {
  return new Promise((resolve, reject) => {
    let body = '';

    req.on('data', chunk => {
      body += chunk;

      if (body.length > maxBytes) {
        reject(new Error('Request बहुत बड़ा है।'));
        req.destroy();
      }
    });

    req.on('end', () => {
      try {
        resolve(JSON.parse(body || '{}'));
      } catch {
        reject(new Error('Invalid JSON'));
      }
    });

    req.on('error', reject);
  });
}


function applyLanguageInstruction(message) {
  const text = String(message || '');

  const hindi =
    /हिंदी\s*(में|मे)|hindi\s*(में|me|mein)|in\s+hindi|answer\s+in\s+hindi/i
      .test(text);

  const english =
    /अंग्रेजी\s*(में|मे)|english\s*(में|मे|me|mein)|in\s+english|answer\s+in\s+english/i
      .test(text);

  if (hindi && !english) {
    return text + "\n\nLANGUAGE OVERRIDE: इस request का पूरा उत्तर केवल हिंदी में दो। User की explicit language instruction को प्राथमिकता दो।";
  }

  if (english && !hindi) {
    return text + "\n\nLANGUAGE OVERRIDE: Answer this request entirely in English. Follow the user's explicit language instruction as the highest priority.";
  }

  return text;
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    sendJson(res, 200, {
      ok: true,
      service: 'RAJ AI',
      status: 'online'
    });
    return;
  }

  
    if (req.method === 'POST' && req.url === '/analyze-file') {
      try {
        const payload = await readBody(req, 10 * 1024 * 1024);

        const fileName = String(payload.fileName || 'uploaded-file').trim();
        const mimeType = String(payload.mimeType || 'application/octet-stream').trim();
        const base64 = String(payload.data || '').trim();
        const question = String(
          payload.question || 'इस file को पढ़कर मुख्य जानकारी और महत्वपूर्ण बातें बताओ।'
        ).trim();

        if (!base64) {
          return sendJson(res, 400, {
            ok: false,
            error: 'File data नहीं मिला।'
          });
        }

        const buffer = Buffer.from(base64, 'base64');

        if (!buffer.length) {
          return sendJson(res, 400, {
            ok: false,
            error: 'Uploaded file खाली है।'
          });
        }

        const MAX_FILE_SIZE = 6 * 1024 * 1024;

        if (buffer.length > MAX_FILE_SIZE) {
          return sendJson(res, 413, {
            ok: false,
            error: 'File बहुत बड़ी है। अभी अधिकतम 6 MB supported है।'
          });
        }

        const safeName = path.basename(fileName).replace(
          /[^a-zA-Z0-9._-]/g,
          '_'
        );

        const ext = path.extname(safeName).toLowerCase();

        const allowed = [
          '.pdf',
          '.docx',
          '.txt',
          '.md',
          '.csv',
          '.json',
          '.log'
        ];

        if (!allowed.includes(ext)) {
          return sendJson(res, 400, {
            ok: false,
            error: 'अभी PDF, DOCX, TXT, MD, CSV, JSON और LOG files supported हैं।'
          });
        }

        const baseDir = path.resolve(
          process.env.RAJ_AI_FILES_DIR ||
          path.join(process.env.HOME || '.', 'raj-ai-files')
        );

        const uploadDir = path.join(baseDir, 'uploads');

        await fs.promises.mkdir(uploadDir, {
          recursive: true
        });

        const tempName =
          `${crypto.randomUUID()}-${safeName}`;

        const tempRelativePath =
          path.join('uploads', tempName);

        const tempFullPath =
          path.join(baseDir, tempRelativePath);

        await fs.promises.writeFile(
          tempFullPath,
          buffer
        );

        try {
          const result = await analyzeFile(tempRelativePath);

          if (!result.text || !result.text.trim()) {
            return sendJson(res, 200, {
              ok: true,
              tool: 'files_analyze',
              answer: 'File पढ़ी गई, लेकिन उसमें readable text नहीं मिला।',
              data: {
                fileName: safeName,
                mimeType,
                characters: 0
              }
            });
          }

          const MAX_AI_TEXT = 60000;
          const fileText =
            result.text.length > MAX_AI_TEXT
              ? result.text.substring(0, MAX_AI_TEXT) +
                '\n\n[बाकी content analysis के लिए सीमित किया गया है।]'
              : result.text;

          const prompt = `
User wants analysis of an uploaded file.

File name:
${safeName}

File type:
${result.type}

User question:
${question}

File content:
${fileText}

Rules:
Answer only from the uploaded file content.
Do not invent facts that are not present.
Answer in the user's language.
Use clean plain text.
Give a useful direct answer.
`;

          const answer = await askAI(prompt);

          return sendJson(res, 200, {
            ok: true,
            tool: 'files_analyze',
            answer: typeof answer === 'string'
              ? answer
              : String(answer || ''),
            data: {
              fileName: safeName,
              mimeType,
              type: result.type,
              size: result.size,
              characters: result.characters
            }
          });
        } finally {
          try {
            await fs.promises.unlink(tempFullPath);
          } catch (_) {}
        }
      } catch (error) {
        console.error('File analysis error:', error);

        return sendJson(res, 500, {
          ok: false,
          error: error.message || 'File analysis failed'
        });
      }
    }

  
  if (req.method === 'POST' && req.url === '/analyze-image') {
    try {
      const payload = await readBody(req, 10 * 1024 * 1024);

      const base64 = String(payload.data || '').trim();
      const mimeType = String(
        payload.mimeType || 'image/jpeg'
      ).trim();
      const question = String(
        payload.question || 'इस image में क्या दिखाई दे रहा है?'
      ).trim();

      if (!base64) {
        return sendJson(res, 400, {
          ok: false,
          error: 'Image data नहीं मिला।'
        });
      }

      const allowedImages = [
        'image/jpeg',
        'image/png',
        'image/webp',
        'image/gif'
      ];

      if (!allowedImages.includes(mimeType)) {
        return sendJson(res, 400, {
          ok: false,
          error: 'अभी JPG, PNG, WEBP और GIF images supported हैं।'
        });
      }

      const buffer = Buffer.from(base64, 'base64');

      if (!buffer.length) {
        return sendJson(res, 400, {
          ok: false,
          error: 'Image खाली है।'
        });
      }

      const MAX_IMAGE_SIZE = 6 * 1024 * 1024;

      if (buffer.length > MAX_IMAGE_SIZE) {
        return sendJson(res, 413, {
          ok: false,
          error: 'Image 6 MB से बड़ी है। अभी अधिकतम 6 MB supported है।'
        });
      }

      const answer = await askAIWithImage(
        base64,
        mimeType,
        question
      );

      return sendJson(res, 200, {
        ok: true,
        tool: 'image_analyze',
        answer: typeof answer === 'string'
          ? answer
          : String(answer || ''),
        data: {
          mimeType,
          size: buffer.length
        }
      });

    } catch (error) {
      console.error('Image analysis error:', error);

      return sendJson(res, 500, {
        ok: false,
        error: error.message || 'Image analysis failed'
      });
    }
  }

  if (req.method === 'POST' && req.url === '/chat') {
    try {
      const body = await readBody(req);
      const message = String(body.message || '').trim();

      if (!message) {
        sendJson(res, 400, {
          ok: false,
          error: 'Message खाली है।'
        });
        return;
      }

      const result = await runAction(message);

      sendJson(res, 200, {
        ok: true,
        tool: result.tool || 'ai',
        answer: typeof result.answer === 'string'
          ? result.answer
          : '',
        data: typeof result.answer === 'object' && result.answer !== null
          ? result.answer
          : null
      });
    } catch (error) {
      console.error('Chat error:', error.message);

      sendJson(res, 500, {
        ok: false,
        error: 'AI जवाब नहीं दे पाया।'
      });
    }

    return;
  }

  sendJson(res, 404, {
    ok: false,
    error: 'Not found'
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`🤖 RAJ AI backend running on port ${PORT}`);
});
