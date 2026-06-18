import { chromium } from 'playwright';

const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({
  viewport: { width: 1920, height: 1080 },
  recordVideo: { dir: 'C:\\Users\\soupa\\server\\', size: { width: 1920, height: 1080 } }
});

const page = await context.newPage();
await page.goto('http://127.0.0.1:9876/stream_overlay.html', { waitUntil: 'networkidle' });
await page.waitForTimeout(15000);

const videoPath = await page.video().path();
await context.close();
await browser.close();

console.log('Video saved at:', videoPath);
