const { runTerminal } = require('./terminal');
const {
  listRequiredPermissions,
  getPermissionInfo
} = require('../android/permission-manager');
const { search: searchWeb } = require('../web/search-service');
const {
  remember,
  getMemories
} = require('../core/memory');

const {
  BASE_DIR: FILES_BASE_DIR,
  listFiles,
  readText,
  writeText
} = require('./files');


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

  memory_save: {
    name: 'memory_save',
    description: 'RAJ AI की सुरक्षित local memory में जानकारी save करना',
    category: 'memory',
    confirmation: false,
    run: async (text = '', type = 'note') => {
      return remember(text, type);
    }
  },

  memory_list: {
    name: 'memory_list',
    description: 'RAJ AI की saved memory पढ़ना',
    category: 'memory',
    confirmation: false,
    run: async (limit = 20) => {
      const items = await getMemories(limit);

      if (!items.length) {
        return 'Memory खाली है।';
      }

      return items
        .map(
          (item, index) =>
            `${index + 1}. [${item.type}] ${item.text}`
        )
        .join('\n');
    }
  },

  files_list: {
    name: 'files_list',
    description: 'RAJ AI की सुरक्षित files directory में files और folders दिखाना',
    category: 'files',
    confirmation: false,
    run: async (relativePath = '.') => {
      const items = await listFiles(relativePath);
      if (!items.length) {
        return 'Folder खाली है।';
      }

      return [
        `Files location: ${FILES_BASE_DIR}`,
        ...items.map(item => `${item.type === 'folder' ? '[Folder]' : '[File]'} ${item.name}`)
      ].join('\\n');
    }
  },

  files_read: {
    name: 'files_read',
    description: 'RAJ AI की सुरक्षित files directory से text file पढ़ना',
    category: 'files',
    confirmation: false,
    run: async (relativePath = '') => {
      if (!String(relativePath).trim()) {
        throw new Error('File path जरूरी है।');
      }

      return readText(relativePath);
    }
  },

  files_write: {
    name: 'files_write',
    description: 'RAJ AI की सुरक्षित files directory में text file बनाना या लिखना',
    category: 'files',
    confirmation: true,
    run: async (relativePath = '', content = '') => {
      if (!String(relativePath).trim()) {
        throw new Error('File path जरूरी है।');
      }

      return writeText(relativePath, content);
    }
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
