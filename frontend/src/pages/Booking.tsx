import { useEffect, useState } from 'react';
import { useNavigate, useParams, Link } from 'react-router-dom';
import { apiListMeetingTypes, apiListSlots, apiCreateBooking } from '../api/generated';
import type { MeetingType, Slot } from '../api/generated';
import { useGuest } from '../context/GuestContext';

export default function BookingPage() {
  const { guest } = useGuest();
  const navigate = useNavigate();
  const { typeId } = useParams<{ typeId: string }>();

  // ── Meeting type ───────────────────────────────────────────────
  const [meetingType, setMeetingType] = useState<MeetingType | null>(null);
  const [typeState, setTypeState] = useState<'loading' | 'ready' | 'missing'>('loading');

  useEffect(() => {
    apiListMeetingTypes()
      .then(({ data }) => {
        const list = data ?? [];
        const found = list.find((t) => t.id === typeId) ?? null;
        setMeetingType(found);
        setTypeState(found ? 'ready' : 'missing');
      })
      .catch(() => {
        setTypeState('missing');
      });
  }, [typeId]);

  if (typeState === 'loading') return <p className="page-hint">Загружаем…</p>;
  if (typeState !== 'ready' || !meetingType)
    return (
      <div className="missing">
        <p>Тип встречи не найден.</p>
        <Link to="/booking" className="button">
          Назад к типам
        </Link>
      </div>
    );

  // ── Slot selection ─────────────────────────────────────────────
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [slotsState, setSlotsState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [slots, setSlots] = useState<Slot[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!typeId) return;
    apiListSlots({ query: { meetingTypeId: typeId, date: '2026-01-15' } })
      .then(({ data }) => {
        setSlots(data ?? []);
        setSlotsState('ready');
      })
      .catch(() => {
        setSlotsState('error');
      });
  }, [typeId]);

  // ── Note & submit ──────────────────────────────────────────────
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    if (!success) return;
    const tid = setTimeout(() => navigate('/'), 2500);
    return () => clearTimeout(tid);
  }, [success, navigate]);

  const book = async () => {
    if (!typeId || !selectedSlot) return;
    setSubmitting(true);
    setError(null);
    const body = {
      meetingTypeId: typeId,
      date: '2026-01-15',
      startTime: selectedSlot,
      guestName: guest.name,
      guestAvatar: guest.avatar,
      note: note.trim() ? note.trim() : undefined,
    };
    const res = await apiCreateBooking({ body });
    setSubmitting(false);
    if (res.error) {
      const status = res.response?.status;
      if (status === 409) setError('Это время уже занято. Выберите другой слот.');
      else if (status === 400) setError('Не удалось записаться: проверьте выбранные дату и время.');
      else setError('Не удалось записаться. Попробуйте ещё раз.');
      return;
    }
    setSuccess(true);
  };

  // ── Render ─────────────────────────────────────────────────────
  if (typeState !== 'ready' || !meetingType)
    return (
      <div className="missing">
        <p>Тип встречи не найден.</p>
        <Link to="/booking" className="button">
          Назад к типам
        </Link>
      </div>
    );

  return (
    <div className="booking-page">
      <div className="booking-topbar">
        <span>Встреча {meetingType.name}</span>
        <small>({meetingType.durationMinutes} мин)</small>
      </div>

      <div className="booking-columns">
        <section className="booking-summary">
          <div className="summary-block">
            <span className="summary-label">Выбранная дата</span>
            <span className="summary-value">15 января 2026</span>
          </div>
          <div className="summary-block">
            <span className="summary-label">Выбранное время</span>
            <span className="summary-value">
              {selectedSlot ? selectedSlot.slice(0, 5) : 'Время не выбрано'}
            </span>
          </div>
          <div className="summary-block">
            <span className="summary-label">Заметка (необязательно)</span>
            <textarea
              className="note-input"
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Что-то полезное для встречи…"
            />
          </div>
        </section>

        <section className="booking-slots">
          {slotsState === 'loading' && <p className="page-hint">Загружаем слоты…</p>}
          {slotsState === 'error' && (
            <p className="page-hint hint-error">Не удалось загрузить слоты.</p>
          )}
          {slotsState === 'ready' && (
            <ul className="slots">
              {slots.map((slot) => (
                <li key={slot.startTime}>
                  <button
                    type="button"
                    className={`slot${!slot.available ? ' slot-busy' : ''}${selectedSlot === slot.startTime ? ' slot-selected' : ''}`}
                    disabled={!slot.available}
                    onClick={() => setSelectedSlot(slot.startTime)}
                  >
                    <span>
                      {slot.startTime.slice(0, 5)} – {slot.endTime.slice(0, 5)}
                    </span>
                    <span className="slot-status">{slot.available ? 'Свободно' : 'Занято'}</span>
                  </button>
                </li>
              ))}
            </ul>
          )}
          {error && <p className="hint-error">{error}</p>}
          <div className="slot-actions">
            <button type="button" className="button" onClick={() => navigate('/booking')}>
              Назад
            </button>
            <button
              type="button"
              className="button button-primary"
              disabled={!selectedSlot || submitting}
              onClick={book}
            >
              {submitting ? 'Записываем…' : 'Продолжить'}
            </button>
          </div>
        </section>
      </div>

      {success && <p className="hint success">Вы успешно записаны! Возвращаемся на главную…</p>}
    </div>
  );
}
