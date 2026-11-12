import { beforeEach, describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { Booking, MeetingType } from '../api/generated';
import {
  apiCreateMeetingType,
  apiDeleteMeetingType,
  apiListBookings,
  apiListMeetingTypes,
  apiUpdateMeetingType,
} from '../api/generated';
import AdminPage from './Admin';

vi.mock('../api/generated', () => ({
  apiCreateMeetingType: vi.fn(),
  apiDeleteMeetingType: vi.fn(),
  apiListBookings: vi.fn(),
  apiListMeetingTypes: vi.fn(),
  apiUpdateMeetingType: vi.fn(),
}));

const meetingTypes: MeetingType[] = [
  {
    id: 'meeting-15',
    name: 'Встреча 15 минут',
    description: 'Короткий тип события для быстрого слота',
    durationMinutes: 15,
  },
];

const bookings: Booking[] = [
  {
    id: 'booking-1',
    meetingTypeId: 'meeting-15',
    meetingTypeName: 'Встреча 15 минут',
    date: '2026-11-13',
    startTime: '09:00:00',
    endTime: '09:15:00',
    guestName: 'Иван Гость',
    guestAvatar: '🦊',
  },
];

function renderAdminPage() {
  return render(
    <MemoryRouter initialEntries={['/admin']}>
      <AdminPage />
    </MemoryRouter>,
  );
}

beforeEach(() => {
  localStorage.clear();
  vi.clearAllMocks();
  vi.mocked(apiListMeetingTypes).mockResolvedValue({ data: meetingTypes, error: undefined });
  vi.mocked(apiListBookings).mockResolvedValue({ data: bookings, error: undefined });
  vi.mocked(apiCreateMeetingType).mockResolvedValue({
    data: {
      id: 'meeting-45',
      name: 'Встреча 45 минут',
      description: 'Длинное обсуждение',
      durationMinutes: 45,
    },
    error: undefined,
  });
  vi.mocked(apiUpdateMeetingType).mockResolvedValue({
    data: { ...meetingTypes[0], name: 'Встреча 20 минут', durationMinutes: 20 },
    error: undefined,
  });
  vi.mocked(apiDeleteMeetingType).mockResolvedValue({ data: undefined, error: undefined });
});

describe('админка', () => {
  it('показывает типы встреч и предстоящие записи', async () => {
    renderAdminPage();

    expect((await screen.findAllByText('Встреча 15 минут')).length).toBeGreaterThan(0);
    expect(screen.getByText('Иван Гость')).toBeInTheDocument();
    expect(screen.getByText('2026-11-13')).toBeInTheDocument();
  });

  it('создаёт тип встречи через SDK и перечитывает список', async () => {
    renderAdminPage();

    fireEvent.change(await screen.findByLabelText('Название'), {
      target: { value: 'Встреча 45 минут' },
    });
    fireEvent.change(screen.getByLabelText('Описание'), {
      target: { value: 'Длинное обсуждение' },
    });
    fireEvent.change(screen.getByLabelText('Длительность, мин'), { target: { value: '45' } });
    fireEvent.click(screen.getByRole('button', { name: 'Добавить тип' }));

    await waitFor(() =>
      expect(apiCreateMeetingType).toHaveBeenCalledWith({
        body: {
          name: 'Встреча 45 минут',
          description: 'Длинное обсуждение',
          durationMinutes: 45,
        },
      }),
    );
    await waitFor(() => expect(apiListMeetingTypes).toHaveBeenCalledTimes(2));
  });

  it('редактирует тип встречи через SDK', async () => {
    renderAdminPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Редактировать' }));
    fireEvent.change(screen.getByLabelText('Название'), {
      target: { value: 'Встреча 20 минут' },
    });
    fireEvent.change(screen.getByLabelText('Длительность, мин'), { target: { value: '20' } });
    fireEvent.click(screen.getByRole('button', { name: 'Сохранить тип' }));

    await waitFor(() =>
      expect(apiUpdateMeetingType).toHaveBeenCalledWith({
        path: { id: 'meeting-15' },
        body: {
          name: 'Встреча 20 минут',
          description: 'Короткий тип события для быстрого слота',
          durationMinutes: 20,
        },
      }),
    );
    await waitFor(() => expect(apiListMeetingTypes).toHaveBeenCalledTimes(2));
  });

  it('удаляет тип встречи через SDK', async () => {
    renderAdminPage();

    fireEvent.click(await screen.findByRole('button', { name: 'Удалить' }));

    await waitFor(() =>
      expect(apiDeleteMeetingType).toHaveBeenCalledWith({ path: { id: 'meeting-15' } }),
    );
    await waitFor(() => expect(apiListMeetingTypes).toHaveBeenCalledTimes(2));
  });
});
