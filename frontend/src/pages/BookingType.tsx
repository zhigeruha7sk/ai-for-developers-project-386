import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

// Simple type for meeting type
type MeetingType = {
  id: string;
  name: string;
  durationMinutes: number;
};

export default function BookingTypePage() {
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([]);
  const [state, setState] = useState<'loading' | 'ready' | 'missing'>('loading');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    // Use Vite proxy: /api -> http://localhost:8080
    fetch('/api/meeting-types')
      .then((response) => {
        if (!response.ok) throw new Error('Network response was not ok');
        return response.json();
      })
      .then((data) => {
        console.log('API data:', data);
        setMeetingTypes(data ?? []);
        setState('ready');
        setError(null);
      })
      .catch((err) => {
        console.error('Failed to fetch meeting types:', err);
        setState('missing');
        setError('Не удалось загрузить типы встреч');
      });
  }, []);

  if (state === 'loading') return <p>Загружаем…</p>;
  if (state === 'missing')
    return (
      <div className="types-page">
        <h1>Типы встреч недоступны</h1>
        <p>{error ?? 'Произошла ошибка при загрузке типов встреч.'}</p>
        <button
          onClick={() => {
            setState('loading');
            setError(null);
          }}
          className="button"
        >
          Попробовать снова
        </button>
        <Link to="/" className="button">
          Назад домой
        </Link>
      </div>
    );

  if (meetingTypes.length === 0)
    return (
      <div className="types-page">
        <h1>Типы встреч недоступны</h1>
        <p>Типы встреч не найдены в ответе сервера. Попробуйте нажать "Попробовать снова".</p>
        <button onClick={() => setState('loading')} className="button">
          Попробовать снова
        </button>
        <Link to="/" className="button">
          Назад домой
        </Link>
      </div>
    );

  return (
    <div className="types-page">
      <h1 className="page-heading">Выберите тип события</h1>

      <ul className="meeting-types">
        {meetingTypes.map((t) => (
          <li key={t.id} className="meeting-type" onClick={() => navigate(`/booking/${t.id}`)}>
            <span className="meeting-type-name">{t.name}</span>
            <span className="meeting-type-duration">{t.durationMinutes} мин</span>
          </li>
        ))}
      </ul>

      <div style={{ marginTop: '24px' }}>
        <Link to="/" className="button">
          Назад домой
        </Link>
      </div>
    </div>
  );
}
