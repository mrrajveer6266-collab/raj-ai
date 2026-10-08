require('dotenv').config();

const http = require('http');
const { runAction } = require('./core/action-runner');

const PORT = Number(process.env.PORT) || 8787;

function sendJson(res, status, data) {
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8'
  });
  res.end(JSON.stringify(data));
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = '';

    req.on('data', chunk => {
      body += chunk;

      if (body.length > 1024 * 1024) {
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

const server = http.createServer(async (req, res) => {
  if (req.method === 'GET' && req.url === '/health') {
    sendJson(res, 200, {
      ok: true,
      service: 'RAJ AI',
      status: 'online'
    });
    return;
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
