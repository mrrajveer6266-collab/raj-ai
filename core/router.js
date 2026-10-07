const { askAI } = require('./ai');

const ROUTER_PROMPT = `
You are the tool router for RAJ AI.

Available tools:
1. terminal_pwd
   Use when the user asks for the current working/home folder.

2. terminal_ls
   Use when the user asks what files/folders are present.

3. terminal_date
   Use when the user asks for the current date or time.

4. terminal_whoami
   Use when the user asks for the current system username.

If no tool is needed, return "ai".

Return ONLY one of:
terminal_pwd
terminal_ls
terminal_date
terminal_whoami
ai

Do not explain your choice.
`;

async function routeCommand(userMessage) {
  const result = await askAI(
    ROUTER_PROMPT + '\n\nUser request:\n' + userMessage
  );

  const choice = result.trim().toLowerCase();

  const allowed = new Set([
    'terminal_pwd',
    'terminal_ls',
    'terminal_date',
    'terminal_whoami',
    'ai'
  ]);

  return allowed.has(choice) ? choice : 'ai';
}

module.exports = { routeCommand };
