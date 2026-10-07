function decodeEntities(text) {
  return text
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>');
}

function stripTags(text) {
  return decodeEntities(
    text
      .replace(/<script[\s\S]*?<\/script>/gi, '')
      .replace(/<style[\s\S]*?<\/style>/gi, '')
      .replace(/<[^>]+>/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
  );
}

function parseSearchResults(html, limit = 8) {
  const results = [];
  const seen = new Set();

  const linkPattern =
    /<a[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>/gi;

  let match;

  while ((match = linkPattern.exec(html)) && results.length < limit) {
    let url = decodeEntities(match[1]);
    const title = stripTags(match[2]);

    if (url.startsWith('/url?q=')) {
      url = url.substring(7).split('&')[0];
    }

    if (!/^https?:\/\//i.test(url)) {
      continue;
    }

    if (
      title.length < 5 ||
      seen.has(url) ||
      /google\.(com|co\.in)/i.test(url) ||
      /accounts\.google/i.test(url)
    ) {
      continue;
    }

    seen.add(url);

    results.push({
      title,
      url
    });
  }

  return results;
}

module.exports = { parseSearchResults };
