const { askAI } = require('./ai');
const { listTools } = require('../tools');

const PLANNER_PROMPT = `
You are the action planner for RAJ AI.

Select exactly ONE action for the user's request.

Available tools:
{{TOOLS}}

Return ONLY valid JSON in this exact format:
{"tool":"ai","args":[]}

Rules:
- If no tool is needed, use "ai".
- If a tool directly performs the request, select it.
- For files_list, args must be [relativePath].
- For files_read, args must be [relativePath].
- For files_write, args must be [relativePath, content].
- Never use absolute file paths.
- Never invent tool names.
- Return JSON only.
`;

async function planAction(userMessage) {
  const message = String(userMessage || '').trim().toLowerCase();

  const webTriggers = [
    'latest',
    'live',
    'current',
    'today',
    'news',
    'आज',
    'अभी',
    'ताजा',
    'ताज़ा',
    'करंट',
    'न्यूज़',
    'खबर',
    'समाचार',
    'वेब सर्च',
    'web search',
    'internet पर',
    'internet par',
    'online search'
  ];

  if (webTriggers.some(trigger => message.includes(trigger))) {
    return {
      tool: 'web_search',
      args: [userMessage]
    };
  }

  if (
    message.includes('मेरी files') ||
    message.includes('मेरी फाइल') ||
    message.includes('मेरी फाइलें') ||
    message.includes('files दिखाओ') ||
    message.includes('files दिखा') ||
    message.includes('files list') ||
    message.includes('list files')
  ) {
    return {
      tool: 'files_list',
      args: ['.']
    };
  }

  const readMatch =
    message.match(/([a-zA-Z0-9._/\\-]+)\s+(?:पढ़ो|पढ़|read)\s*$/i) ||
    message.match(/(?:read|पढ़ो|पढ़)\s+([a-zA-Z0-9._/\\-]+)\s*$/i);

  if (readMatch) {
    return {
      tool: 'files_read',
      args: [readMatch[1]]
    };
  }

  const writeMatch =
    message.match(/([a-zA-Z0-9._/\\-]+)\s+(?:में|मे)\s+(.+?)\s+(?:लिखो|लिख|write)\s*$/i);

  if (writeMatch) {
    return {
      tool: 'files_write',
      args: [writeMatch[1], writeMatch[2]]
    };
  }

  const tools = listTools();

  const toolText = tools
    .map(t => `${t.name}: ${t.description}`)
    .join('\n');

  const prompt =
    PLANNER_PROMPT
      .replace('{{TOOLS}}', toolText) +
    `\n\nUser request:\n${userMessage}`;

  const result = await askAI(prompt);

  try {
    const parsed = JSON.parse(result.trim());

    const allowed = new Set([
      ...tools.map(t => t.name),
      'ai'
    ]);

    if (!allowed.has(parsed.tool)) {
      return { tool: 'ai', args: [] };
    }

    if (!Array.isArray(parsed.args)) {
      return { tool: parsed.tool, args: [] };
    }

    return {
      tool: parsed.tool,
      args: parsed.args
    };
  } catch {
    return {
      tool: 'ai',
      args: []
    };
  }
}

module.exports = { planAction };
