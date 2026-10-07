require('dotenv').config();

const readline = require('readline');
const { askAI } = require('./core/ai');
const { planAction } = require('./core/planner');
const { runTool } = require('./tools');

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout,
  prompt: 'RAJ AI > '
});

async function handleCommand(command) {
  console.log('🧭 Agent Planner: request समझ रहा हूँ...');

  let action;

  try {
    action = await planAction(command);
    console.log(`🎯 Action: ${action}`);
  } catch (error) {
    console.log(`⚠️ Planner उपलब्ध नहीं है: ${error.message}`);
    action = 'ai';
  }

  if (action !== 'ai') {
    console.log(`🛠️ Tool: ${action}`);

    try {
      const result = await runTool(action);

      console.log('');
      console.log(`📋 Result:\n${result || '(कोई output नहीं)'}`);
      console.log('');
    } catch (error) {
      console.log(`❌ Tool Error: ${error.message}`);
      console.log('');
    }

    return;
  }

  console.log('🧠 AI से जवाब ले रहा हूँ...');

  try {
    const answer = await askAI(command);

    console.log('');
    console.log(`🤖 RAJ AI: ${answer}`);
    console.log('');
  } catch (error) {
    console.log('');
    console.log(`❌ AI Error: ${error.message}`);
    console.log('');
  }
}

console.log('');
console.log('🤖 RAJ AI Agent');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('🧠 Multi-AI Brain: ONLINE');
console.log('🧭 Agent Planner: ONLINE');
console.log('🛠️ Unified Tool Engine: ONLINE');
console.log('🔐 Safe Terminal Mode: ON');
console.log('━━━━━━━━━━━━━━━━━━━━━━━━');
console.log('Type "exit" to quit.');
console.log('');

rl.prompt();

rl.on('line', async (input) => {
  const command = input.trim();

  if (!command) {
    rl.prompt();
    return;
  }

  if (command.toLowerCase() === 'exit') {
    console.log('👋 RAJ AI बंद हो रहा है...');
    rl.close();
    return;
  }

  await handleCommand(command);
  rl.prompt();
});

rl.on('close', () => {
  process.exit(0);
});
