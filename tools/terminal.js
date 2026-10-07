const { execFile } = require('child_process');

const ALLOWED_COMMANDS = new Set([
  'pwd',
  'ls',
  'whoami',
  'date',
  'node',
  'npm'
]);

function runTerminal(command, args = []) {
  return new Promise((resolve, reject) => {
    if (!ALLOWED_COMMANDS.has(command)) {
      return reject(
        new Error(`Command "${command}" अभी allowed नहीं है।`)
      );
    }

    execFile(command, args, {
      cwd: process.env.HOME,
      timeout: 10000,
      maxBuffer: 1024 * 1024
    }, (error, stdout, stderr) => {
      if (error) {
        return reject(new Error(stderr || error.message));
      }

      resolve((stdout || stderr || '').trim());
    });
  });
}

module.exports = { runTerminal };
