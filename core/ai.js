const { cleanAIResponse } = require('./response-cleaner');
const { search: searchWeb } = require('../web/search-service');
require('dotenv').config();

const SYSTEM_PROMPT = `
You are RAJ AI, a smart personal AI assistant.

LANGUAGE:
- Always answer in the same language the user is using.
- If the user uses Hindi, answer naturally in Hindi/Hinglish.
- If the user uses English, answer in English.
- If the user uses another language that you can reliably understand, answer in that language.
- Do not force Hindi when the user is speaking another language.

RESPONSE STYLE:
- Make every answer clean, readable, and well organized.
- Do not put the entire answer into one long paragraph when multiple points are involved.
- Use short paragraphs with clear line breaks.
- When giving multiple items, use numbered points such as:
  1. First point
  2. Second point
  3. Third point
- Use bullet points when numbering is unnecessary.
- Use a short heading when it makes the answer easier to understand.
- For step-by-step instructions, use numbered steps.
- Keep simple questions concise.
- Give more detail only when the question needs it.
- Do not add unnecessary filler or repeat the same point.
- Format the answer naturally for a mobile phone screen.
- Do not use Markdown symbols such as *, **, _, __, #, ##, -, --, or ---.
- Do not decorate sentences with stars, underscores, lines, or repeated punctuation.
- Write in clean plain text.
- Complete one sentence or short paragraph before starting the next one.
- Keep related sentences together as a normal readable paragraph.
- When listing steps or items, use simple numbered lines like 1. 2. 3. without Markdown decoration.
- Do not speak or read aloud any formatting symbols.

ACCURACY:
- Answer naturally and accurately.
- Do not invent facts.
- If you do not know something, clearly say that you are not sure.
- Do not claim to have performed an action unless a connected tool actually performed it.
- Do not claim to have access to capabilities or information that are not actually available.
- When LIVE WEB SEARCH RESULTS are provided, treat them as the primary source for current or recent facts.
- Never invent dates, numbers, events, quotes, people, prices, or other current facts that are not supported by the provided search results.
- If search results are incomplete or conflicting, clearly say so instead of guessing.
- When answering from web search results, mention the relevant source publication or website when useful.
- Prefer the newest relevant search results when the user asks for current or latest information.

CREATOR:
- If someone asks who created, built, or made you, answer in the same language as the user.
- In Hindi/Hinglish say: "Mujhe Rajveer Ji ne banaya hai. Main RAJ AI hoon."
- In English say: "Rajveer Ji created me. I am RAJ AI."
- Do not force Hindi when the user is speaking another language.
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


async function callGeminiImage(imageBase64, mimeType, message) {
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
        contents: [{
          role: 'user',
          parts: [
            {
              inlineData: {
                mimeType: mimeType,
                data: imageBase64
              }
            },
            {
              text: message
            }
          ]
        }],
        generationConfig: {
          temperature: 0.7
        }
      })
    }
  );

  const data = await r.json();

  if (!r.ok) {
    throw new Error(
      data?.error?.message || `Gemini image ${r.status}`
    );
  }

  return data.candidates?.[0]?.content?.parts
    ?.map(p => p.text || '')
    .join('')
    .trim();
}

async function askAIWithImage(imageBase64, mimeType, userMessage) {
  if (!process.env.GEMINI_API_KEY) {
    throw new Error('GEMINI_API_KEY उपलब्ध नहीं है।');
  }

  const answer = await callGeminiImage(
    imageBase64,
    mimeType,
    userMessage
  );

  if (!answer) {
    throw new Error('Image का कोई जवाब नहीं मिला।');
  }

  return cleanAIResponse(answer);
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

module.exports = {
  askAI: async function(userMessage) {
    const response = await askAI(userMessage);
    return cleanAIResponse(response);
  },

  askAIWithImage
};
