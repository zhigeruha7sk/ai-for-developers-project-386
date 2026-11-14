import { defineConfig, devices } from '@playwright/test';

const FRONTEND_URL = 'http://127.0.0.1:5173';
const BACKEND_URL = 'http://127.0.0.1:8080';

/**
 * Браузерные сценарии идут против настоящих backend и frontend: тесты не
 * подменяют ответы API бронирования. Серверы поднимаются автоматически, если
 * ещё не запущены (локально `mise run dev` можно переиспользовать).
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  // Один воркер: сценарии делят in-memory состояние одного backend.
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: FRONTEND_URL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'mise run dev:backend',
      cwd: '..',
      url: `${BACKEND_URL}/api/health`,
      reuseExistingServer: !process.env.CI,
      timeout: 180_000,
    },
    {
      command: 'mise run dev:frontend',
      cwd: '..',
      url: FRONTEND_URL,
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
    },
  ],
});
