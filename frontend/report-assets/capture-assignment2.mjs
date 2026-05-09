import { chromium } from 'playwright';
import fs from 'node:fs';

const base = 'http://127.0.0.1:5173';
const outDir = new URL('.', import.meta.url).pathname;

const browser = await chromium.launch({ headless: true });

// 1) Login page
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.screenshot({ path: `${outDir}/01-login-page.png`, fullPage: true });
  await page.close();
}

// 2) Login form validation
{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.getByRole('button', { name: '安全登录' }).click();
  await page.waitForTimeout(600);
  await page.screenshot({ path: `${outDir}/02-login-validation.png`, fullPage: true });
  await page.close();
}

// 3) Main page after login (mock backend API)
{
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  await context.addInitScript(() => {
    localStorage.setItem('token', 'assignment_token_demo');
    localStorage.setItem('user_role', 'USER');
    localStorage.setItem('userInfo', JSON.stringify({
      id: 10001,
      nickname: '作业演示用户',
      role: 'USER',
      avatar: ''
    }));
  });

  await context.route('http://localhost:8080/**', async (route) => {
    const url = new URL(route.request().url());
    const path = url.pathname;
    let data = null;

    if (path === '/product/list') {
      data = [
        {
          id: 1,
          name: '二手机械键盘',
          description: '九成新，手感优秀，支持面交测试',
          price: 129,
          originalPrice: 399,
          image: '',
          sellerAddress: '一食堂门口',
          sellerName: '张同学',
          sellerId: 20001,
          sellerAvatar: ''
        },
        {
          id: 2,
          name: '大学物理教材',
          description: '笔记完整，适合期末复习',
          price: 25,
          originalPrice: 68,
          image: '',
          sellerAddress: '图书馆南门',
          sellerName: '李同学',
          sellerId: 20002,
          sellerAvatar: ''
        }
      ];
    } else if (path.endsWith('/unread-count')) {
      data = 2;
    } else if (path === '/cart/list') {
      data = [];
    } else {
      data = {};
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ code: 200, msg: 'ok', data })
    });
  });

  const page = await context.newPage();
  await page.goto(`${base}/`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(800);
  await page.screenshot({ path: `${outDir}/03-main-page-after-login.png`, fullPage: true });
  await context.close();
}

await browser.close();

const files = fs.readdirSync(outDir).filter((x) => x.endsWith('.png')).sort();
for (const f of files) console.log(f);
