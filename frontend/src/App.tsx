import { useEffect, useState } from 'react';
import { apiHealth } from './api/generated';

type HealthState = 'loading' | 'up' | 'down';

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

    return () => {
      active = false;
    };
  }, []);

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
          <h2 className="card-title">Бронирование</h2>
          <p className="card-text">
            Календарь свободных слотов появится здесь на следующем шаге. Сейчас доступна только
            проверка работоспособности сервиса.
          </p>
          <p className={`health health-${health}`} role="status">
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
