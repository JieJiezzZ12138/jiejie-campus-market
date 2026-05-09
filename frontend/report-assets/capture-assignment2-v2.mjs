import { chromium } from 'playwright';
import fs from 'node:fs';

const base = 'http://127.0.0.1:5173';
const outDir = new URL('.', import.meta.url).pathname;

const browser = await chromium.launch({ headless: true });

const mockApiRoutes = async (context) => {
  await context.route('http://localhost:8080/**', async (route) => {
    const req = route.request();
    const url = new URL(req.url());
    const path = url.pathname;
    const method = req.method().toUpperCase();

    let data = {};

    if (method === 'POST' && path === '/auth/login') {
      const body = req.postDataJSON?.() || {};
      data = {
        token: 'assignment_token_demo',
        userInfo: {
          id: 10001,
          username: body.username || '20230001',
          nickname: '作业演示用户',
          role: 'USER',
          avatar: ''
        }
      };
    } else if (path === '/product/list') {
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
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json; charset=utf-8',
      body: JSON.stringify({ code: 200, msg: 'ok', data })
    });
  });
};

{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.waitForTimeout(500);
  await page.screenshot({ path: `${outDir}/01-login-page.png`, fullPage: true });
  await page.close();
}

{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="学号 / 管理员账号"]', '20230001');
  await page.fill('input[placeholder="登录密码"]', 'Pass1234');
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${outDir}/02-login-filled.png`, fullPage: true });
  await page.close();
}

{
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="学号 / 管理员账号"]', '20230001');
  await page.fill('input[placeholder="登录密码"]', 'Pass1234');
  await page.click('.el-input__icon.el-input__password');
  await page.waitForTimeout(300);
  await page.screenshot({ path: `${outDir}/03-password-visible.png`, fullPage: true });
  await page.close();
}

{
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
  await mockApiRoutes(context);

  const page = await context.newPage();
  await page.goto(`${base}/login`, { waitUntil: 'networkidle' });
  await page.fill('input[placeholder="学号 / 管理员账号"]', '20230001');
  await page.fill('input[placeholder="登录密码"]', 'Pass1234');
  await page.getByRole('button', { name: '安全登录' }).click();
  await page.waitForURL(`${base}/`);
  await page.waitForTimeout(700);
  await page.screenshot({ path: `${outDir}/04-main-page-after-login.png`, fullPage: true });
  await context.close();
}

await browser.close();

const files = fs.readdirSync(outDir)
  .filter((x) => x.startsWith('0') && x.endsWith('.png'))
  .sort();
for (const f of files) console.log(f);
