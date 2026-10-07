function cleanAIResponse(text) {
  if (!text) return '';

  let output = String(text);

  output = output.replace(/\r\n/g, '\n');
  output = output.replace(/\*\*\*/g, '');
  output = output.replace(/\*\*/g, '');
  output = output.replace(/__/g, '');
  output = output.replace(/_/g, '');
  output = output.replace(/~~/g, '');
  output = output.replace(/^#{1,6}\s*/gm, '');
  output = output.replace(/^\s*[-*_]{3,}\s*$/gm, '');
  output = output.replace(/^\s*[-*_]\s+/gm, '');
  output = output.replace(/\s+([,.;!?])/g, '$1');

  output = output
    .split('\n')
    .map(line => line.trim())
    .filter((line, index, lines) => {
      if (line !== '') return true;
      return index > 0 && index < lines.length - 1;
    })
    .join('\n');

  return output.trim();
}

module.exports = { cleanAIResponse };
