import { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { apiListMeetingTypes } from '../api/generated';
import type { MeetingType } from '../api/generated';

export default function BookingTypePage() {
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([]);
  const [state, setState] = useState<'loading' | 'ready' | 'missing'>('loading');
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  const load = useCallback(async () => {
    setState('loading');
    setError(null);
    const result = await apiListMeetingTypes();
    if (result.error) {
      setMeetingTypes([]);
      setState('missing');
      setError('Не удалось загрузить типы встреч');
      return;
    }
    setMeetingTypes(result.data ?? []);
    setState('ready');
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  if (state === 'loading') return <p>Загружаем…</p>;
  if (state === 'missing')
    return (
      <div className="types-page">
        <h1>Типы встреч недоступны</h1>
        <p>{error ?? 'Произошла ошибка при загрузке типов встреч.'}</p>
        <button onClick={() => void load()} className="button">
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
        <p>Типы встреч не найдены в ответе сервера. Попробуйте нажать «Попробовать снова».</p>
        <button onClick={() => void load()} className="button">
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
