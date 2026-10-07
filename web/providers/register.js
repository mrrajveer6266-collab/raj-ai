const { registerProvider } = require('./index');
const googleNews = require('./google-news');

registerProvider('google-news', googleNews);

module.exports = {
  registered: ['google-news']
};
