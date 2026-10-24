import { defineConfig, devices } from '@playwright/test';

/** 単体テストだけを実行するとき（--project=unit）は、開発サーバーを起動しない。 */
const unitOnly = process.argv.includes('--project=unit');

/**
 * Playwright の設定。
 *
 * - unit: ブラウザを使わない単体テスト（tests/unit）。
 * - e2e : ブラウザ（Chromium）で画面を操作するテスト（tests/e2e）。
 *         バックエンド（既定 http://localhost:8080）が起動していることが前提。
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'unit',
      testMatch: /unit\/.*\.spec\.ts/,
    },
    {
      name: 'e2e',
      testMatch: /e2e\/.*\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] },
      // E2E は同じバックエンド DB を共有するため、順番に実行する。
      fullyParallel: false,
    },
  ],
  webServer: unitOnly
    ? undefined
    : {
        command: 'npm run dev',
        url: 'http://localhost:3000/login',
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
      },
});
