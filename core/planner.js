const { askAI } = require('./ai');
const { listTools } = require('../tools');

const PLANNER_PROMPT = `
You are the action planner for RAJ AI.

Your job is to select exactly ONE tool for the user's request.

Available tools:
{{TOOLS}}

Rules:
- If a tool can directly perform the requested action, choose that tool.
- If no available tool is needed, choose "ai".
- Return ONLY the exact tool name or "ai".
- Do not explain anything.
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
    return 'web_search';
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
  const choice = result.trim().toLowerCase();

  const allowed = new Set([
    ...tools.map(t => t.name),
    'ai'
  ]);

  return allowed.has(choice) ? choice : 'ai';
}

module.exports = { planAction };
