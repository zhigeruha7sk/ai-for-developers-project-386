import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { apiCreateBooking, apiListMeetingTypes, apiListSlots } from '../api/generated';
import type { MeetingType, Slot } from '../api/generated';
import { useGuest } from '../context/GuestContext';

/** Окно записи — ближайшие 14 календарных дней (как на бэкенде). */
const WINDOW_DAYS = 14;

const WEEKDAYS_SHORT = ['пн', 'вт', 'ср', 'чт', 'пт', 'сб', 'вс'];
const WEEKDAYS_FULL = [
  'воскресенье',
  'понедельник',
  'вторник',
  'среда',
  'четверг',
  'пятница',
  'суббота',
];
const MONTHS_GENITIVE = [
  'января',
  'февраля',
  'марта',
  'апреля',
  'мая',
  'июня',
  'июля',
  'августа',
  'сентября',
  'октября',
  'ноября',
  'декабря',
];
const MONTHS_NOMINATIVE = [
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

/** Сегодняшняя дата в часовом поясе владельца (Asia/Almaty). */
function todayInAlmaty(): Date {
  const iso = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Almaty',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function toIso(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

function fromIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function addDays(date: Date, days: number): Date {
  const copy = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  copy.setDate(copy.getDate() + days);
  return copy;
}

/** «вторник, 31 марта» — как в спецификации. */
function formatSelectedDate(iso: string): string {
  const date = fromIso(iso);
  return `${WEEKDAYS_FULL[date.getDay()]}, ${date.getDate()} ${MONTHS_GENITIVE[date.getMonth()]}`;
}

export default function BookingPage() {
  const { guest, refresh } = useGuest();
  const navigate = useNavigate();
  const { typeId } = useParams<{ typeId: string }>();

  // ── Все хуки объявлены безусловно, до любых ранних возвратов ──
  const [meetingType, setMeetingType] = useState<MeetingType | null>(null);
  const [typeState, setTypeState] = useState<'loading' | 'ready' | 'missing'>('loading');
  const [selectedDate, setSelectedDate] = useState(() => toIso(todayInAlmaty()));
  const [viewMonth, setViewMonth] = useState(() => {
    const today = todayInAlmaty();
    return { year: today.getFullYear(), month: today.getMonth() };
  });
  const [slots, setSlots] = useState<Slot[]>([]);
  const [slotsState, setSlotsState] = useState<'loading' | 'ready' | 'error'>('loading');
  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  // ── Тип встречи ────────────────────────────────────────────────
  useEffect(() => {
    if (!typeId) return;
    let active = true;

    apiListMeetingTypes().then((result) => {
      if (!active) return;
      if (result.error && !result.data) {
        setTypeState('missing');
        return;
      }
      const found = (result.data ?? []).find((t) => t.id === typeId) ?? null;
      setMeetingType(found);
      setTypeState(found ? 'ready' : 'missing');
    });

    return () => {
      active = false;
    };
  }, [typeId]);

  // ── Слоты выбранной даты ───────────────────────────────────────
  useEffect(() => {
    if (!typeId) return;
    let active = true;

    setSlotsState('loading');
    apiListSlots({ query: { meetingTypeId: typeId, date: selectedDate } }).then((result) => {
      if (!active) return;
      if (result.error && !result.data) {
        setSlotsState('error');
        return;
      }
      setSlots(result.data ?? []);
      setSlotsState('ready');
    });

    return () => {
      active = false;
    };
  }, [typeId, selectedDate]);

  // ── После успешной записи возвращаемся на главную ──────────────
  useEffect(() => {
    if (!success) return;
    const timer = setTimeout(() => navigate('/'), 2500);
    return () => clearTimeout(timer);
  }, [success, navigate]);

  const today = todayInAlmaty();
  const windowStart = today;
  const windowEnd = addDays(today, WINDOW_DAYS - 1);

  const selectDate = (iso: string) => {
    setSelectedDate(iso);
    setSelectedSlot(null);
  };

  const shiftMonth = (delta: number) => {
    setViewMonth((current) => {
      const next = new Date(current.year, current.month + delta, 1);
      return { year: next.getFullYear(), month: next.getMonth() };
    });
  };

  const book = async () => {
    if (!typeId || !selectedSlot) return;
    setSubmitting(true);
    setError(null);
    const result = await apiCreateBooking({
      body: {
        meetingTypeId: typeId,
        date: selectedDate,
        startTime: selectedSlot,
        guestName: guest.name || 'Гость',
        guestAvatar: guest.avatar,
      },
    });
    setSubmitting(false);
    if (result.error) {
      const status = result.response?.status;
      if (status === 409) setError('Это время уже занято. Выберите другой слот.');
      else if (status === 400) setError('Не удалось записаться: проверьте выбранные дату и время.');
      else setError('Не удалось записаться. Попробуйте ещё раз.');
      return;
    }
    // По спецификации: после успешной записи клиент меняется.
    refresh();
    setSuccess(true);
  };

  // ── Ранние возвраты только после всех хуков ────────────────────
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

  // ── Сетка календаря (пн–вс) ────────────────────────────────────
  const firstOfMonth = new Date(viewMonth.year, viewMonth.month, 1);
  const leadingBlanks = (firstOfMonth.getDay() + 6) % 7;
  const daysInMonth = new Date(viewMonth.year, viewMonth.month + 1, 0).getDate();
  const dayCells: Array<number | null> = [
    ...Array.from({ length: leadingBlanks }, () => null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1),
  ];

  return (
    <div className="booking-page">
      {/* Нижний блок, 5% высоты — название выбранного типа */}
      <div className="booking-strip">{meetingType.name}</div>

      {/* Три блока по 30% ширины и 60% высоты */}
      <div className="booking-blocks">
        {/* Блок 1 — гость, тип встречи, выбранная дата и время */}
        <section className="booking-panel">
          <span className="guest-badge">
            <span className="avatar" aria-hidden="true">
              {guest.avatar}
            </span>
            <span className="guest-name">{guest.name || 'Гость'}</span>
            <span className="guest-role">Гость</span>
          </span>

          <h2 className="booking-type-name">{meetingType.name}</h2>
          <p className="booking-type-desc">{meetingType.description}</p>

          <div className="summary-block">
            <span className="summary-label">Выбранная дата</span>
            <span className="summary-value">{formatSelectedDate(selectedDate)}</span>
          </div>
          <div className="summary-block">
            <span className="summary-label">Выбранное время</span>
            <span className="summary-value">
              {selectedSlot ? selectedSlot.slice(0, 5) : 'Время не выбрано'}
            </span>
          </div>
        </section>

        {/* Блок 2 — календарь */}
        <section className="booking-panel">
          <h2 className="panel-title">Календарь</h2>
          <div className="month-nav">
            <button
              type="button"
              className="icon-button"
              aria-label="Предыдущий месяц"
              onClick={() => shiftMonth(-1)}
            >
              ←
            </button>
            <button
              type="button"
              className="icon-button"
              aria-label="Следующий месяц"
              onClick={() => shiftMonth(1)}
            >
              →
            </button>
          </div>
          <p className="month-label">
            {MONTHS_NOMINATIVE[viewMonth.month]} {viewMonth.year} г.
          </p>

          <div className="calendar-grid">
            {WEEKDAYS_SHORT.map((weekday) => (
              <span key={weekday} className="weekday">
                {weekday}
              </span>
            ))}
            {dayCells.map((day, index) => {
              if (day === null) return <span key={`blank-${index}`} className="day-empty" />;
              const date = new Date(viewMonth.year, viewMonth.month, day);
              const iso = toIso(date);
              const disabled = date < windowStart || date > windowEnd;
              const isSelected = iso === selectedDate;
              return (
                <button
                  key={iso}
                  type="button"
                  className={`day${isSelected ? ' day-selected' : ''}${disabled ? ' day-disabled' : ''}`}
                  disabled={disabled}
                  onClick={() => selectDate(iso)}
                >
                  {day}
                </button>
              );
            })}
          </div>
        </section>

        {/* Блок 3 — статус слотов и кнопки */}
        <section className="booking-panel">
          <h2 className="panel-title">Статус слотов</h2>

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

      {success && (
        <div className="success-overlay" role="status">
          <div className="success-modal">Вы успешно записаны! Возвращаемся на главную…</div>
        </div>
      )}
    </div>
  );
}
