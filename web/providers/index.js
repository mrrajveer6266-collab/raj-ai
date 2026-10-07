const providers = {};

function registerProvider(name, provider) {
  if (!name || !provider || typeof provider.search !== 'function') {
    throw new Error(`Invalid search provider: ${name}`);
  }

  providers[name] = provider;
}

function getProvider(name) {
  return providers[name] || null;
}

function listProviders() {
  return Object.keys(providers);
}

module.exports = {
  registerProvider,
  getProvider,
  listProviders
};
