import { expect, test } from '@playwright/test';
import type { Page } from '@playwright/test';

const MEETING_15 = { id: 'meeting-15', name: 'Встреча 15 минут' } as const;
const MEETING_30 = { id: 'meeting-30', name: 'Встреча 30 минут' } as const;

const MONTHS = [
  'январь',
  'февраль',
  'март',
  'апрель',
  'май',
  'июнь',
  'июль',
  'август',
  'сентябрь',
  'октябрь',
  'ноябрь',
  'декабрь',
];

interface TargetDate {
  iso: string;
  day: number;
  monthIndex: number;
  year: number;
}

/** Дата в часовом поясе владельца: окно записи считается от неё. */
function almatyDate(offsetDays: number): TargetDate {
  const moment = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  const iso = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Almaty',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(moment);
  const [year, month, day] = iso.split('-').map(Number);
  return { iso, day, monthIndex: month - 1, year };
}

async function openBooking(page: Page, typeId: string): Promise<void> {
  await page.goto(`/booking/${typeId}`);
  await expect(page.locator('.calendar-grid')).toBeVisible();
}

/** Листает календарь до нужного месяца и выбирает дату, дожидаясь слотов. */
async function selectDate(page: Page, date: TargetDate): Promise<void> {
  for (let attempt = 0; attempt < 3; attempt += 1) {
    const label = (await page.locator('.month-label').innerText()).trim();
    const [monthName, yearPart] = label.split(' ');
    const viewMonth = MONTHS.indexOf(monthName.toLowerCase());
    const viewYear = Number(yearPart);
    const delta = (date.year - viewYear) * 12 + (date.monthIndex - viewMonth);
    if (delta === 0) break;
    const navigation = delta > 0 ? 'Следующий месяц' : 'Предыдущий месяц';
    await page.getByRole('button', { name: navigation }).click();
    await expect(page.locator('.month-label')).not.toHaveText(label);
  }

  await expect(page.locator('.month-label')).toHaveText(
    `${MONTHS[date.monthIndex]} ${date.year} г.`,
  );

  const slotsResponse = page.waitForResponse(
    (response) =>
      response.url().includes('/api/slots') && response.url().includes(`date=${date.iso}`),
  );
  await page
    .locator('.calendar-grid')
    .getByRole('button', { name: String(date.day), exact: true })
    .click();
  await slotsResponse;
  await expect(page.locator('.slots .slot').first()).toBeVisible();
}

/** Нажимает первый свободный слот и возвращает время его начала. */
async function selectFirstFreeSlot(page: Page): Promise<string> {
  const slot = page.locator('.slots .slot:not(.slot-busy)').first();
  await expect(slot).toBeVisible();
  const range = (await slot.locator('span').first().innerText()).trim();
  await slot.click();
  await expect(slot).toHaveClass(/slot-selected/);
  return range.split(/\s*–\s*/)[0];
}

async function confirmBooking(page: Page): Promise<void> {
  await page.getByRole('button', { name: 'Продолжить' }).click();
  await expect(page.locator('.success-modal')).toContainText('Вы успешно записаны');
}

test('гость проходит путь от выбора типа до подтверждения записи', async ({ page }) => {
  const date = almatyDate(1);

  await page.goto('/booking');
  await page.locator('.meeting-type', { hasText: MEETING_30.name }).click();
  await expect(page).toHaveURL(/\/booking\/meeting-30$/);

  await selectDate(page, date);
  const startTime = await selectFirstFreeSlot(page);
  await confirmBooking(page);

  // Владелец видит предстоящую встречу в админке.
  await page.goto('/admin');
  await expect(page.locator('.admin-table')).toBeVisible();
  const row = page.locator('.admin-table tbody tr', { hasText: date.iso });
  await expect(row).toContainText(startTime);
  await expect(row).toContainText(MEETING_30.name);
});

test('при конфликте гость видит понятное сообщение о занятом слоте', async ({ context }) => {
  const date = almatyDate(2);
  const firstGuest = await context.newPage();
  const secondGuest = await context.newPage();

  await openBooking(firstGuest, MEETING_30.id);
  await selectDate(firstGuest, date);
  const firstSlot = await selectFirstFreeSlot(firstGuest);

  // Второй гость открывает тот же слот до того, как первый его занял.
  await openBooking(secondGuest, MEETING_30.id);
  await selectDate(secondGuest, date);
  const secondSlot = await selectFirstFreeSlot(secondGuest);
  expect(secondSlot).toBe(firstSlot);

  await confirmBooking(firstGuest);

  await secondGuest.getByRole('button', { name: 'Продолжить' }).click();
  await expect(secondGuest.locator('.hint-error')).toHaveText(
    'Это время уже занято. Выберите другой слот.',
  );
});

test('занятый слот не предлагается как свободный и не бронируется другим типом', async ({
  page,
  request,
}) => {
  const date = almatyDate(3);

  await openBooking(page, MEETING_30.id);
  await selectDate(page, date);
  const startTime = await selectFirstFreeSlot(page);
  await confirmBooking(page);

  // После записи слот того же типа помечен занятым и недоступен для клика.
  await openBooking(page, MEETING_30.id);
  await selectDate(page, date);
  const busy = page.locator('.slot-busy', { hasText: startTime }).first();
  await expect(busy).toBeDisabled();
  await expect(busy).toContainText('Занято');

  // Для другого типа встречи пересекающийся старт тоже занят.
  await openBooking(page, MEETING_15.id);
  await selectDate(page, date);
  await expect(page.locator('.slot-busy', { hasText: startTime }).first()).toBeDisabled();

  // Повторная запись на это время через настоящий API отклоняется.
  const conflict = await request.post('/api/bookings', {
    data: {
      meetingTypeId: MEETING_15.id,
      date: date.iso,
      startTime,
      guestName: 'Гость E2E',
      guestAvatar: '🦊',
    },
  });
  expect(conflict.status()).toBe(409);
  expect((await conflict.json()).detail).toContain('Слот уже занят');
});
