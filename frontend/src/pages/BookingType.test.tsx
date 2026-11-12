import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import type { MeetingType } from '../api/generated';
import { apiListMeetingTypes } from '../api/generated';
import BookingTypePage from './BookingType';

vi.mock('../api/generated', () => ({
  apiListMeetingTypes: vi.fn(),
}));

const meetingTypes: MeetingType[] = [
  {
    id: 'meeting-15',
    name: 'Встреча 15 минут',
    description: 'Короткий тип события для быстрого слота',
    durationMinutes: 15,
  },
  {
    id: 'meeting-30',
    name: 'Встреча 30 минут',
    description: 'Базовый тип события для бронирования',
    durationMinutes: 30,
  },
];

function renderBookingTypePage() {
  return render(
    <MemoryRouter initialEntries={['/booking']}>
      <Routes>
        <Route path="/booking" element={<BookingTypePage />} />
        <Route path="/booking/:typeId" element={<div>Страница выбора слота</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe('страница выбора типа события', () => {
  it('загружает типы через SDK и ведёт гостя к календарю', async () => {
    vi.mocked(apiListMeetingTypes).mockResolvedValue({ data: meetingTypes, error: undefined });

    renderBookingTypePage();

    const card = await screen.findByText('Встреча 15 минут');
    expect(apiListMeetingTypes).toHaveBeenCalledTimes(1);

    fireEvent.click(card);

    expect(await screen.findByText('Страница выбора слота')).toBeInTheDocument();
  });

  it('ответ с данными, но с ошибкой считается неудачной загрузкой', async () => {
    // Тип SDK делает data и error взаимоисключающими, но в рантайме возможны оба сразу.
    const degraded = {
      data: meetingTypes,
      error: { message: 'degraded response' },
    } as unknown as Awaited<ReturnType<typeof apiListMeetingTypes>>;
    vi.mocked(apiListMeetingTypes).mockResolvedValue(degraded);

    renderBookingTypePage();

    expect(await screen.findByText('Типы встреч недоступны')).toBeInTheDocument();
  });

  it('после ошибки кнопка «Попробовать снова» повторяет загрузку', async () => {
    vi.mocked(apiListMeetingTypes)
      .mockResolvedValueOnce({ data: undefined, error: { message: 'network down' } })
      .mockResolvedValueOnce({ data: meetingTypes, error: undefined });

    renderBookingTypePage();

    expect(await screen.findByText('Типы встреч недоступны')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: 'Попробовать снова' }));

    expect(await screen.findByText('Встреча 30 минут')).toBeInTheDocument();
    expect(apiListMeetingTypes).toHaveBeenCalledTimes(2);
  });
});
