require('dotenv').config();

const {
  getProvider,
  listProviders
} = require('./providers');
require('./providers/register');

function getSearchProvider() {
  const requested = (process.env.SEARCH_PROVIDER || 'auto').toLowerCase();

  if (requested !== 'auto') {
    const provider = getProvider(requested);

    if (!provider) {
      throw new Error(`Search provider "${requested}" उपलब्ध नहीं है।`);
    }

    return provider;
  }

  const available = listProviders();

  if (!available.length) {
    throw new Error('कोई search provider अभी registered नहीं है।');
  }

  return getProvider(available[0]);
}

async function search(query, options = {}) {
  if (!query || !String(query).trim()) {
    throw new Error('Search query खाली है।');
  }

  const provider = getSearchProvider();

  return provider.search(String(query).trim(), options);
}

module.exports = {
  search,
  getSearchProvider
};
