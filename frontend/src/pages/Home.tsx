import { Link } from 'react-router-dom';
import { useGuest } from '../context/GuestContext';

export default function HomePage() {
  const { guest } = useGuest();

  return (
    <div className="home-page">
      <span className="guest-badge">
        <span className="guest-name">{guest.name || 'Гость'}</span>
      </span>

      <section className="home-hero">
        <h1 className="hero-title">Календарь звонков</h1>
        <p className="hero-text">
          Выберите удобное время и забронируйте встречу. Свободные слоты показываются по часовому
          поясу Asia/Almaty.
        </p>
        <Link to="/booking" className="button button-primary button-large">
          Выбрать время
        </Link>
      </section>

      <section className="home-bottom">
        <div className="home-half">
          <p className="kicker">БЫСТРАЯ ЗАПИСЬ НА ЗВОНОК</p>
          <h1 className="home-word">Calendar</h1>
          <p className="home-text">
            Забронируйте встречу за минуту, выберите тип события и<br />
            удобное время
          </p>
          <Link to="/booking" className="button button-primary button-large">
            Записаться
          </Link>
        </div>

        <div className="home-half">
          <h2 className="home-features-title">Возможности</h2>
          <ul className="home-features">
            <li>Выбор типа события и удобного времени для встреч</li>
            <li>Быстрое бронирование с подтверждением и дополнительными заметками</li>
            <li>Управление типами встреч и просмотр записей в админке</li>
          </ul>
        </div>
      </section>
    </div>
  );
}
