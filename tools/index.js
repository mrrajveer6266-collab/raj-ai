const { runTerminal } = require('./terminal');
const {
  listRequiredPermissions,
  getPermissionInfo
} = require('../android/permission-manager');
const { search: searchWeb } = require('../web/search-service');

const TOOL_REGISTRY = {
  web_search: {
    name: 'web_search',
    description: 'Internet पर current information और news search करना',
    category: 'web',
    confirmation: false,
    run: async (query = '') => {
      const cleanQuery = String(query || '').trim();

      console.log(`🌐 Tool Web Search Query: ${cleanQuery}`);

      const result = await searchWeb(cleanQuery, { limit: 8 });

      if (!result.results.length) {
        return 'कोई search result नहीं मिला।';
      }

      return result.results
        .map((item, index) => {
          const title = item.title || 'Untitled';
          const url = item.url || '';
          const publishedAt = item.publishedAt || 'Published date unavailable';

          return [
            `${index + 1}. ${title}`,
            `Source URL: ${url}`,
            `Published: ${publishedAt}`
          ].join('\n');
        })
        .join('\n\n');
    }
  },

  terminal_pwd: {
    name: 'terminal_pwd',
    description: 'Current home/working directory बताना',
    category: 'terminal',
    confirmation: false,
    run: () => runTerminal('pwd', [])
  },

  terminal_ls: {
    name: 'terminal_ls',
    description: 'Current directory की files और folders दिखाना',
    category: 'terminal',
    confirmation: false,
    run: () => runTerminal('ls', ['-la'])
  },

  terminal_date: {
    name: 'terminal_date',
    description: 'Current date और time बताना',
    category: 'terminal',
    confirmation: false,
    run: () => runTerminal('date', [])
  },

  terminal_whoami: {
    name: 'terminal_whoami',
    description: 'Current system username बताना',
    category: 'terminal',
    confirmation: false,
    run: () => runTerminal('whoami', [])
  },

  android_permission_list: {
    name: 'android_permission_list',
    description: 'RAJ AI को Android पर किन permissions की जरूरत है बताना',
    category: 'android',
    confirmation: false,
    run: () => {
      const permissions = listRequiredPermissions();

      return permissions
        .map(
          p =>
            `• ${p.name}\n  Android: ${p.android}\n  काम: ${p.description}`
        )
        .join('\n\n');
    }
  }
};

function getTool(name) {
  return TOOL_REGISTRY[name] || null;
}

function listTools() {
  return Object.values(TOOL_REGISTRY).map(tool => ({
    name: tool.name,
    description: tool.description,
    category: tool.category,
    confirmation: tool.confirmation
  }));
}

async function runTool(name, args = []) {
  const tool = getTool(name);

  if (!tool) {
    throw new Error(`Tool "${name}" उपलब्ध नहीं है।`);
  }

  return tool.run(...args);
}

module.exports = {
  TOOL_REGISTRY,
  getTool,
  listTools,
  runTool
};
