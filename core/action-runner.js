const { planAction } = require('./planner');
const { askAI } = require('./ai');
const { runTool } = require('../tools');

async function runAction(userMessage) {
  if (!userMessage || !String(userMessage).trim()) {
    throw new Error('User message खाली है।');
  }

  const message = String(userMessage).trim();
  const plannedTool = await planAction(message);

  console.log(`🧠 Planner: ${plannedTool}`);

  if (plannedTool === 'ai') {
    return {
      tool: 'ai',
      answer: await askAI(message)
    };
  }

  let toolResult;

  if (plannedTool === 'web_search') {
    const lowerMessage = message.toLowerCase();

    let searchQuery = message;

    if (
      lowerMessage.includes('technology') ||
      lowerMessage.includes('tech') ||
      lowerMessage.includes('ai') ||
      lowerMessage.includes('artificial intelligence') ||
      lowerMessage.includes('टेक्नोलॉजी') ||
      lowerMessage.includes('तकनीकी')
    ) {
      searchQuery = 'India latest technology AI news';
    } else if (
      lowerMessage.includes('news') ||
      lowerMessage.includes('खबर') ||
      lowerMessage.includes('समाचार')
    ) {
      searchQuery = 'India latest news';
    }

    console.log(`🔎 Action Search Query: ${searchQuery}`);

    toolResult = await runTool('web_search', [searchQuery]);
  } else {
    toolResult = await runTool(plannedTool, []);
  }

  const finalPrompt = `
User request:
${message}

Tool used:
${plannedTool}

Live tool result:
${toolResult}

IMPORTANT ACCURACY RULES:
Only use facts that are present in the live tool result.
Do not invent missing facts.
If the user asks for today's news, only treat results published on today's date as today's news.
If a result has an older published date, clearly identify it as older or exclude it when the user specifically requested today's news.
Preserve the source name and published date when available.
If fewer than the requested number of relevant results are available, say so instead of inventing more.
Answer in the user's language.
Write clean plain text without Markdown decoration.
`;

  return {
    tool: plannedTool,
    answer: await askAI(finalPrompt)
  };
}

module.exports = { runAction };
