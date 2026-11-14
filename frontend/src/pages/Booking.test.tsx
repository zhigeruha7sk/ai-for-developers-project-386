import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { Booking, MeetingType, Slot } from '../api/generated';
import { apiCreateBooking, apiListMeetingTypes, apiListSlots } from '../api/generated';
import BookingPage from './Booking';
import BookingTypePage from './BookingType';
import HomePage from './Home';

// Стратегия карты: мок на уровне сгенерированного SDK, backend не поднимается.
vi.mock('../api/generated', () => ({
  apiCreateBooking: vi.fn(),
  apiListMeetingTypes: vi.fn(),
  apiListSlots: vi.fn(),
}));

const meetingType: MeetingType = {
  id: 'type-1',
  name: 'Консультация',
  description: 'Разбор вопросов',
  durationMinutes: 30,
};

const slots: Slot[] = [
  { startTime: '09:00:00', endTime: '09:30:00', available: true },
  { startTime: '09:30:00', endTime: '10:00:00', available: false },
];

const createdBooking: Booking = {
  id: 'booking-1',
  meetingTypeId: 'type-1',
  meetingTypeName: 'Консультация',
  date: '2026-11-10',
  startTime: '09:00:00',
  endTime: '09:30:00',
  guestName: 'Гость',
  guestAvatar: '🦊',
};

function renderBookingPage() {
  return render(
    <MemoryRouter initialEntries={['/booking/type-1']}>
      <Routes>
        <Route path="/booking/:typeId" element={<BookingPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

// Маршруты как в приложении: «/» — главная, «/booking» — выбор типа события.
function renderBookingApp() {
  return render(
    <MemoryRouter initialEntries={['/booking/type-1']}>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/booking" element={<BookingTypePage />} />
        <Route path="/booking/:typeId" element={<BookingPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
  vi.mocked(apiListMeetingTypes).mockResolvedValue({ data: [meetingType], error: undefined });
  vi.mocked(apiListSlots).mockResolvedValue({ data: slots, error: undefined });
});

afterEach(() => {
  vi.useRealTimers();
});

describe('страница бронирования', () => {
  it('успешная бронь: выбор свободного слота и подтверждение показывают состояние успеха', async () => {
    vi.mocked(apiCreateBooking).mockResolvedValue({ data: createdBooking, error: undefined });

    renderBookingPage();

    fireEvent.click(await screen.findByRole('button', { name: /09:00 – 09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(await screen.findByRole('status')).toHaveTextContent('Вы успешно записаны!');

    expect(apiListSlots).toHaveBeenCalledWith({
      query: { meetingTypeId: 'type-1', date: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/) },
    });
    expect(apiCreateBooking).toHaveBeenCalledTimes(1);
    expect(apiCreateBooking).toHaveBeenCalledWith({
      body: {
        meetingTypeId: 'type-1',
        date: expect.stringMatching(/^\d{4}-\d{2}-\d{2}$/),
        startTime: '09:00:00',
        guestName: 'Гость',
        guestAvatar: '🦊',
      },
    });
    expect(localStorage.getItem('calendar-guest')).not.toBeNull();
  });

  it('конфликт: занятый слот отклоняется с понятным сообщением гостю', async () => {
    vi.mocked(apiCreateBooking).mockResolvedValue({
      data: undefined,
      error: { type: 'about:blank', title: 'Conflict', status: 409, detail: 'slot already booked' },
      response: new Response(null, { status: 409 }),
    });

    renderBookingPage();

    fireEvent.click(await screen.findByRole('button', { name: /09:00 – 09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(
      await screen.findByText('Это время уже занято. Выберите другой слот.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.queryByText('Вы успешно записаны! Возвращаемся на главную…')).toBeNull();
  });

  it('занятый слот помечен «Занято» и недоступен для выбора', async () => {
    renderBookingPage();

    const busySlot = await screen.findByRole('button', { name: /09:30 – 10:00/ });
    expect(busySlot).toBeDisabled();
    expect(busySlot).toHaveTextContent('Занято');
    expect(screen.getByRole('button', { name: 'Продолжить' })).toBeDisabled();
  });

  it('неизвестный тип встречи показывает заглушку и ссылку назад', async () => {
    vi.mocked(apiListMeetingTypes).mockResolvedValue({ data: [], error: undefined });

    renderBookingPage();

    expect(await screen.findByText('Тип встречи не найден.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Назад к типам' })).toHaveAttribute('href', '/booking');
  });

  it('ошибка 400 показывает гостю сообщение о дате и времени', async () => {
    vi.mocked(apiCreateBooking).mockResolvedValue({
      data: undefined,
      error: { type: 'about:blank', title: 'Bad Request', status: 400, detail: 'bad request' },
      response: new Response(null, { status: 400 }),
    });

    renderBookingPage();

    fireEvent.click(await screen.findByRole('button', { name: /09:00 – 09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(
      await screen.findByText('Не удалось записаться: проверьте выбранные дату и время.'),
    ).toBeInTheDocument();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('блокирует даты вне окна записи', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.setSystemTime(new Date('2026-11-12T05:00:00Z'));

    try {
      renderBookingPage();

      expect(await screen.findByText('Календарь')).toBeInTheDocument();
      expect(screen.getByRole('button', { name: '11' })).toBeDisabled();
      expect(screen.getByRole('button', { name: '12' })).toBeEnabled();
      expect(screen.getByRole('button', { name: '25' })).toBeEnabled();
      expect(screen.getByRole('button', { name: '26' })).toBeDisabled();
    } finally {
      vi.useRealTimers();
    }
  });

  it('после успешной записи показывает подтверждение и возвращает на главную', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true });
    vi.mocked(apiCreateBooking).mockResolvedValue({ data: createdBooking, error: undefined });

    renderBookingApp();

    fireEvent.click(await screen.findByRole('button', { name: /09:00 – 09:30/ }));
    fireEvent.click(screen.getByRole('button', { name: 'Продолжить' }));

    expect(await screen.findByRole('status')).toHaveTextContent(
      'Вы успешно записаны! Возвращаемся на главную…',
    );
    // Подтверждение видно, гость ещё на странице слотов.
    expect(screen.queryByText('БЫСТРАЯ ЗАПИСЬ НА ЗВОНОК')).toBeNull();

    // Через 2,5 секунды — переход на главную, без реального ожидания.
    await act(async () => {
      await vi.advanceTimersByTimeAsync(2500);
    });

    expect(screen.getByText('БЫСТРАЯ ЗАПИСЬ НА ЗВОНОК')).toBeInTheDocument();
    expect(screen.queryByRole('status')).toBeNull();
  });

  it('кнопка «Назад» со страницы слотов возвращает к выбору типа события', async () => {
    renderBookingApp();

    // Дожидаемся загрузки слотов, чтобы страница была в рабочем состоянии.
    await screen.findByRole('button', { name: /09:00 – 09:30/ });

    fireEvent.click(screen.getByRole('button', { name: 'Назад' }));

    expect(await screen.findByText('Выберите тип события')).toBeInTheDocument();
    expect(screen.queryByText('Статус слотов')).toBeNull();
  });

  it('ошибка загрузки слотов показывает гостю понятный текст', async () => {
    vi.mocked(apiListSlots).mockResolvedValue({
      data: undefined,
      error: { type: 'about:blank', title: 'Server Error', status: 500, detail: 'network down' },
    });

    renderBookingPage();

    expect(await screen.findByText('Не удалось загрузить слоты.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /09:00 – 09:30/ })).toBeNull();
  });
});
