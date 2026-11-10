import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { Booking, MeetingType, Slot } from '../api/generated';
import { apiCreateBooking, apiListMeetingTypes, apiListSlots } from '../api/generated';
import BookingPage from './Booking';

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

beforeEach(() => {
  localStorage.clear();
  vi.mocked(apiListMeetingTypes).mockResolvedValue({ data: [meetingType], error: undefined });
  vi.mocked(apiListSlots).mockResolvedValue({ data: slots, error: undefined });
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
  });

  it('конфликт: занятый слот отклоняется с понятным сообщением гостю', async () => {
    vi.mocked(apiCreateBooking).mockResolvedValue({
      data: undefined,
      error: { message: 'slot already booked' },
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
});
