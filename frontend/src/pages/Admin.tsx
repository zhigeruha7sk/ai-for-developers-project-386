import { useEffect, useState } from 'react';
import { apiListMeetingTypes, apiListBookings } from '../api/generated';
import type { MeetingType } from '../api/generated';
import { useGuest } from '../context/GuestContext';

export default function AdminPage() {
  const { guest } = useGuest();
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([]);
  const [bookings, setBookings] = useState<
    Array<{
      date: string;
      startTime?: string;
      endTime?: string;
      guestName: string;
      guestAvatar: string;
      meetingTypeName: string;
    }>
  >([]);

  useEffect(() => {
    apiListMeetingTypes()
      .then(({ data }) => setMeetingTypes(data ?? []))
      .catch(() => setMeetingTypes([]));
    apiListBookings()
      .then(({ data }) => setBookings(data ?? []))
      .catch(() => setBookings([]));
  }, []);

  return (
    <div className="admin-page">
      <span className="guest-badge">
        <span className="guest-name">{guest.name || 'Гость'}</span>
      </span>
      <h1 className="page-heading">Админка</h1>
      <p className="page-hint">
        Владелец календаря сможет управлять типами встреч и просматривать записи.
      </p>

      <section className="admin-section">
        <h2>Типы встреч</h2>
        <ul className="meeting-types">
          {meetingTypes.map((t) => (
            <li key={t.id} style={{ margin: '8px 0', listStyle: 'none' }}>
              <span style={{ fontWeight: 600 }}>{t.name}</span>
              <span style={{ marginLeft: 12, color: '#5c657a' }}>— {t.description}</span>
              <span style={{ marginLeft: 24, color: '#3b5bdb' }}>({t.durationMinutes} мин)</span>
            </li>
          ))}
        </ul>
        {meetingTypes.length === 0 && (
          <p className="empty-admin">Типы встреч не найдены. Добавьте их в админке.</p>
        )}
      </section>

      <section className="admin-section" style={{ marginTop: '32px' }}>
        <h2>Предстоящие записи</h2>
        {bookings.length === 0 ? (
          <p className="empty-admin">Записей пока нет.</p>
        ) : (
          <table className="admin-table">
            <thead>
              <tr>
                <th>Дата</th>
                <th>Время</th>
                <th>Гость</th>
                <th>Тип встречи</th>
              </tr>
            </thead>
            <tbody>
              {bookings.map((b, idx) => (
                <tr key={idx}>
                  <td>{b.date}</td>
                  <td>
                    {b.startTime?.slice(0, 5)} – {b.endTime?.slice(0, 5)}
                  </td>
                  <td>
                    <span>{b.guestName}</span>
                    <span>{b.guestAvatar}</span>
                  </td>
                  <td>{b.meetingTypeName}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
