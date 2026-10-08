require('dotenv').config();

const GEMINI_API_KEY = process.env.GEMINI_API_KEY;

if (!GEMINI_API_KEY) {
  throw new Error('GEMINI_API_KEY उपलब्ध नहीं है।');
}

async function generateImage(prompt) {
  const response = await fetch(
    'https://generativelanguage.googleapis.com/v1beta/interactions',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': GEMINI_API_KEY
      },
      body: JSON.stringify({
        model: 'gemini-nano-banana-2.1',
        input: String(prompt || '').trim(),
        response_format: {
          type: 'image',
          mime_type: 'image/jpeg',
          aspect_ratio: '1:1',
          image_size: '1K'
        }
      })
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      data?.error?.message || `Gemini Image ${response.status}`
    );
  }

  const imageData = data?.output_image?.data;

  if (!imageData) {
    throw new Error('Gemini ने image output नहीं दिया।');
  }

  return {
    type: 'image',
    mimeType: 'image/jpeg',
    data: imageData
  };
}

async function generateVideo(prompt) {
  const response = await fetch(
    'https://generativelanguage.googleapis.com/v1beta/models/veo-3.1-generate-preview:predictLongRunning',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': GEMINI_API_KEY
      },
      body: JSON.stringify({
        instances: [
          {
            prompt: String(prompt || '').trim()
          }
        ],
        parameters: {
          aspectRatio: '9:16',
          resolution: '720p',
          numberOfVideos: 1
        }
      })
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      data?.error?.message || `Veo ${response.status}`
    );
  }

  if (!data?.name) {
    throw new Error('Veo ने generation operation नहीं दिया।');
  }

  return {
    type: 'video',
    operation: data.name,
    status: 'processing'
  };
}

async function getVideoOperation(operationName) {
  const response = await fetch(
    `https://generativelanguage.googleapis.com/v1beta/${operationName}`,
    {
      method: 'GET',
      headers: {
        'x-goog-api-key': GEMINI_API_KEY
      }
    }
  );

  const data = await response.json();

  if (!response.ok) {
    throw new Error(
      data?.error?.message || `Veo operation ${response.status}`
    );
  }

  return data;
}

module.exports = {
  generateImage,
  generateVideo,
  getVideoOperation
};
