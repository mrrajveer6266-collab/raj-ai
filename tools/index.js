const { runTerminal } = require('./terminal');
const {
  listRequiredPermissions,
  getPermissionInfo
} = require('../android/permission-manager');

const TOOL_REGISTRY = {
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

async function runTool(name) {
  const tool = getTool(name);

  if (!tool) {
    throw new Error(`Tool "${name}" उपलब्ध नहीं है।`);
  }

  return tool.run();
}

module.exports = {
  TOOL_REGISTRY,
  getTool,
  listTools,
  runTool
};
