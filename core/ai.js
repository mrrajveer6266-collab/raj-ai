require('dotenv').config();

const SYSTEM_PROMPT = `
You are RAJ AI, a smart Hindi/Hinglish personal AI assistant.

Answer naturally and accurately.
Use the user's language.
Do not claim to have performed an action unless a connected tool actually performed it.
If you don't know something, clearly say so instead of inventing facts.
`;

async function callGroq(message) {
  const r = await fetch('https://api.groq.com/openai/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${process.env.GROQ_API_KEY}`
    },
    body: JSON.stringify({
      model: 'openai/gpt-oss-120b',
      messages: [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: message }
      ],
      temperature: 0.7
    })
  });

  const data = await r.json();

  if (!r.ok) {
    throw new Error(data?.error?.message || `Groq ${r.status}`);
  }

  return data.choices?.[0]?.message?.content;
}

async function callGemini(message) {
  const model = 'gemini-2.5-flash';

  const r = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': process.env.GEMINI_API_KEY
      },
      body: JSON.stringify({
        systemInstruction: {
          parts: [{ text: SYSTEM_PROMPT }]
        },
        contents: [
          {
            role: 'user',
            parts: [{ text: message }]
          }
        ],
        generationConfig: {
          temperature: 0.7
        }
      })
    }
  );

  const data = await r.json();

  if (!r.ok) {
    throw new Error(data?.error?.message || `Gemini ${r.status}`);
  }

  return data.candidates?.[0]?.content?.parts
    ?.map(p => p.text || '')
    .join('')
    .trim();
}

async function callOpenRouter(message) {
  const r = await fetch(
    'https://openrouter.ai/api/v1/chat/completions',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${process.env.OPENROUTER_API_KEY}`,
        'HTTP-Referer': 'http://localhost',
        'X-Title': 'RAJ AI'
      },
      body: JSON.stringify({
        model: 'openrouter/free',
        messages: [
          { role: 'system', content: SYSTEM_PROMPT },
          { role: 'user', content: message }
        ],
        temperature: 0.7
      })
    }
  );

  const data = await r.json();

  if (!r.ok) {
    throw new Error(data?.error?.message || `OpenRouter ${r.status}`);
  }

  return data.choices?.[0]?.message?.content;
}

async function askAI(message) {
  const providers = [
    ['Groq', callGroq],
    ['Gemini', callGemini],
    ['OpenRouter', callOpenRouter]
  ];

  const errors = [];

  for (const [name, call] of providers) {
    try {
      console.log(`⚡ ${name} को try कर रहा हूँ...`);

      const answer = await call(message);

      if (answer) {
        console.log(`✅ जवाब ${name} से आया`);
        return answer;
      }

      errors.push(`${name}: empty response`);
    } catch (error) {
      console.log(`⚠️ ${name} उपलब्ध नहीं है`);
      errors.push(`${name}: ${error.message}`);
    }
  }

  throw new Error(
    'कोई भी AI provider जवाब नहीं दे पाया।\n' +
    errors.join('\n')
  );
}

module.exports = { askAI };
