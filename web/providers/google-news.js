const https = require('https');

function fetchUrl(url) {
  return new Promise((resolve, reject) => {
    const request = https.get(
      url,
      {
        headers: {
          'User-Agent': 'RAJ-AI/1.0'
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
      reject(new Error('Google News request timeout'));
    });
  });
}

function decodeEntities(text) {
  return String(text)
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function cleanText(text) {
  return decodeEntities(
    String(text)
      .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, '$1')
      .replace(/<[^>]+>/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
  );
}

async function search(query, options = {}) {
  const limit = Math.min(Math.max(Number(options.limit) || 8, 1), 20);

  const url =
    'https://news.google.com/rss/search?q=' +
    encodeURIComponent(String(query).trim()) +
    '&hl=en-IN&gl=IN&ceid=IN:en';

  const xml = await fetchUrl(url);

  const results = [];
  const itemPattern = /<item>([\s\S]*?)<\/item>/gi;

  let match;

  while ((match = itemPattern.exec(xml)) && results.length < limit) {
    const item = match[1];

    const titleMatch = item.match(/<title>([\s\S]*?)<\/title>/i);
    const linkMatch = item.match(/<link>([\s\S]*?)<\/link>/i);
    const dateMatch = item.match(/<pubDate>([\s\S]*?)<\/pubDate>/i);
    const sourceMatch = item.match(/<source[^>]*>([\s\S]*?)<\/source>/i);

    if (!titleMatch || !linkMatch) {
      continue;
    }

    results.push({
      title: cleanText(titleMatch[1]),
      source: sourceMatch ? cleanText(sourceMatch[1]) : null,
      url: cleanText(linkMatch[1]),
      publishedAt: dateMatch ? cleanText(dateMatch[1]) : null
    });
  }

  return {
    provider: 'google-news',
    query,
    results
  };
}

module.exports = { search };
