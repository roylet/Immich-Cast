// Renders art/banner.html into the app's banner, screensaver preview and README image.
// Usage (from the repo root): node art/render.js
const path = require('path');
const { chromium } = require('playwright');

const root = path.resolve(__dirname, '..');
const res = path.join(root, 'app/src/main/res');
const outputs = [
  [1, `${res}/drawable-xhdpi/banner.png`],
  [1.5, `${res}/drawable-xxhdpi/banner.png`],
  [2, `${res}/drawable-xxxhdpi/banner.png`],
  [3, `${res}/drawable-nodpi/dream_preview.png`],
  [4, `${root}/docs/app-banner.png`],
];

(async () => {
  const browser = await chromium.launch();
  for (const [scale, file] of outputs) {
    const page = await browser.newPage({ viewport: { width: 320, height: 180 }, deviceScaleFactor: scale });
    await page.goto('file://' + path.join(__dirname, 'banner.html'));
    require('fs').mkdirSync(path.dirname(file), { recursive: true });
    await page.screenshot({ path: file });
    console.log(`${file} (${320 * scale}x${180 * scale})`);
    await page.close();
  }
  await browser.close();
})();
