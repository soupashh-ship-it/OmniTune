import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';

const basedir = 'C:\\Users\\soupa\\server';

// start static server
const server = http.createServer((req, res) => {
  const filePath = path.join(basedir, req.url === '/' ? '/stream_overlay.html' : req.url);
  if (!filePath.startsWith(basedir)) { res.writeHead(403); res.end(); return; }
  try {
    const content = fs.readFileSync(filePath);
    const ext = path.extname(filePath);
    const types = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.png': 'image/png' };
    res.writeHead(200, { 'Content-Type': types[ext] || 'application/octet-stream' });
    res.end(content);
  } catch { res.writeHead(404); res.end(); }
});
server.listen(9877, '127.0.0.1', async () => {
  console.log('Server on 9877');

  const browser = await chromium.launch({ headless: true, executablePath: 'C:\\Users\\soupa\\AppData\\Local\\ms-playwright\\b\\browser@c346f673e2d5f03811305df6560f50d2\\chrome-win64\\chrome.exe' });
  const ctx = await browser.newContext({
    viewport: { width: 1920, height: 1080 },
    recordVideo: { dir: basedir, size: { width: 1920, height: 1080 } }
  });
  const page = await ctx.newPage();
  await page.goto('http://127.0.0.1:9877/stream_overlay.html', { waitUntil: 'networkidle' });
  await page.waitForTimeout(15000);
  const vpath = await page.video().path();
  await ctx.close();
  await browser.close();
  server.close();
  console.log('Done:', vpath);
});
