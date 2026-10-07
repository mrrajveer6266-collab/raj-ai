async function search(query) {
  return {
    provider: 'test',
    query,
    results: [
      {
        title: `Test result for ${query}`,
        url: 'https://example.com',
        snippet: 'RAJ AI search provider system is working.'
      }
    ]
  };
}

module.exports = { search };
