import { useEffect, useState } from 'react';
import { apiHealth, apiListMeetingTypes } from './api/generated';
import type { MeetingType } from './api/generated';

type HealthState = 'loading' | 'up' | 'down';
type MeetingTypesState = 'loading' | 'ready' | 'error';

const healthLabels: Record<HealthState, string> = {
  loading: 'Проверяем соединение…',
  up: 'Сервис доступен',
  down: 'Сервис недоступен',
};

function CalendarLogo() {
  return (
    <svg
      className="logo"
      viewBox="0 0 24 24"
      width="28"
      height="28"
      aria-hidden="true"
      focusable="false"
    >
      <rect
        x="3"
        y="5"
        width="18"
        height="16"
        rx="3"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
      />
      <path d="M3 10h18" stroke="currentColor" strokeWidth="1.8" />
      <path d="M8 3v4M16 3v4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
      <circle cx="8" cy="14" r="1.3" fill="currentColor" />
      <circle cx="12" cy="14" r="1.3" fill="currentColor" />
      <circle cx="16" cy="14" r="1.3" fill="currentColor" />
    </svg>
  );
}

export default function App() {
  const [health, setHealth] = useState<HealthState>('loading');
  const [meetingTypes, setMeetingTypes] = useState<MeetingType[]>([]);
  const [meetingTypesState, setMeetingTypesState] = useState<MeetingTypesState>('loading');
  const [selectedTypeId, setSelectedTypeId] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    apiHealth()
      .then(({ data }) => {
        if (active) {
          setHealth(data?.status === 'UP' ? 'up' : 'down');
        }
      })
      .catch(() => {
        if (active) {
          setHealth('down');
        }
      });

    apiListMeetingTypes()
      .then(({ data }) => {
        if (active) {
          setMeetingTypes(data ?? []);
          setMeetingTypesState('ready');
        }
      })
      .catch(() => {
        if (active) {
          setMeetingTypesState('error');
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const selectedType = meetingTypes.find((type) => type.id === selectedTypeId);

  return (
    <div className="page">
      <header className="header">
        <div className="brand">
          <CalendarLogo />
          <span className="brand-name">Calendar</span>
        </div>
        <nav className="nav">
          <a className="button button-primary" href="#booking">
            Записаться
          </a>
          <a className="button" href="#admin">
            Админка
          </a>
        </nav>
      </header>

      <main className="main">
        <section className="hero">
          <h1 className="hero-title">Календарь звонков</h1>
          <p className="hero-text">
            Выберите удобное время и забронируйте встречу. Свободные слоты показываются по часовому
            поясу Asia/Almaty.
          </p>
          <a className="button button-primary button-large" href="#booking">
            Выбрать время
          </a>
        </section>

        <section className="card" id="booking">
          <h2 className="card-title">Выберите тип события</h2>
          <p className="card-text">
            Нажмите на карточку, чтобы открыть календарь и выбрать удобный слот.
          </p>

          {meetingTypesState === 'loading' && <p className="hint">Загружаем типы встреч…</p>}

          {meetingTypesState === 'error' && (
            <p className="hint hint-error">
              Не удалось загрузить типы встреч. Проверьте, что бэкенд запущен.
            </p>
          )}

          {meetingTypesState === 'ready' && (
            <>
              <ul className="meeting-types">
                {meetingTypes.map((type) => {
                  const selected = type.id === selectedTypeId;

                  return (
                    <li key={type.id}>
                      <button
                        type="button"
                        className={`meeting-type${selected ? ' meeting-type-selected' : ''}`}
                        aria-pressed={selected}
                        onClick={() => setSelectedTypeId(type.id)}
                      >
                        <span className="meeting-type-name">{type.name}</span>
                        <span className="meeting-type-duration">{type.durationMinutes} мин</span>
                        <span className="meeting-type-description">{type.description}</span>
                      </button>
                    </li>
                  );
                })}
              </ul>
              <p className="hint">
                {selectedType
                  ? `Выбрано: ${selectedType.name} · ${selectedType.durationMinutes} мин.`
                  : 'Тип встречи пока не выбран.'}
              </p>
            </>
          )}

          <p className={`health health-${health} booking-health`} role="status">
            <span className="health-dot" aria-hidden="true" />
            {healthLabels[health]}
          </p>
        </section>

        <section className="card" id="admin">
          <h2 className="card-title">Админка</h2>
          <p className="card-text">
            Владелец календаря сможет управлять типами встреч и просматривать записи.
          </p>
        </section>
      </main>

      <footer className="footer">
        <span>Calendar · учебный проект Хекслета</span>
      </footer>
    </div>
  );
}
