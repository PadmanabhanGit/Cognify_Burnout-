// This service simulates what would normally be a Google Play Scraper
// In production, you would 'npm install google-play-scraper'

const appDictionary = {
  "com.mangazone.app": "Entertainment",
  "com.android.chrome": "Productivity",
  "com.microsoft.emmx": "Productivity", // Edge
  "com.opera.browser": "Productivity",
  "com.brave.browser": "Productivity",
  "com.duckduckgo.mobile.android": "Productivity",
  "com.viber.voip": "Social Media",
  "com.discord": "Social Media",
  "com.zhiliaoapp.musically": "Social Media", // TikTok
  "com.spotify.music": "Entertainment",
  "com.chess": "Gaming",
  "com.tencent.ig": "Gaming", // PUBG
  "com.dts.freefireth": "Gaming",
  "com.mojang.minecraftpe": "Gaming"
};

/**
 * Resolves a category for an unknown package name
 * @param {string} packageName
 * @returns {string} category
 */
async function resolveCategory(packageName) {
  // 1. Check our known dictionary
  if (appDictionary[packageName]) {
    return appDictionary[packageName];
  }

  // 2. Logic to "guess" based on common package patterns if not found
  const pkg = packageName.toLowerCase();
  if (pkg.includes('game') || pkg.includes('rpg') || pkg.includes('moba')) return "Gaming";
  if (pkg.includes('video') || pkg.includes('music') || pkg.includes('player') || pkg.includes('stream')) return "Entertainment";
  if (pkg.includes('chat') || pkg.includes('messenger') || pkg.includes('social')) return "Social Media";
  if (pkg.includes('office') || pkg.includes('edit') || pkg.includes('browser') || pkg.includes('tool')) return "Productivity";

  // 3. In a real app, this is where you'd call the Scraper API
  // let results = await gplay.app({appId: packageName});
  // return results.genre;

  return "Others";
}

module.exports = { resolveCategory };
