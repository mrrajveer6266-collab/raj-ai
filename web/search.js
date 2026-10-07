const https = require('https');

function searchWeb(query) {
  return new Promise((resolve, reject) => {
    const url =
      'https://www.google.com/search?q=' +
      encodeURIComponent(query);

    const request = https.get(
      url,
      {
        headers: {
          'User-Agent':
            'Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36'
        },
        timeout: 10000
      },
      response => {
        let data = '';

        response.on('data', chunk => {
          data += chunk;
        });

        response.on('end', () => {
          resolve(data);
        });
      }
    );

    request.on('error', reject);
    request.on('timeout', () => {
      request.destroy();
      reject(new Error('Web search timeout'));
    });
  });
}

module.exports = { searchWeb };
