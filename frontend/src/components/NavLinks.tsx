import { Link } from 'react-router-dom';

export function NavLinks() {
  return (
    <nav className="nav">
      <Link to="/" className="button">
        <svg
          className="logo"
          viewBox="0 0 24 24"
          width="24"
          height="24"
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
        <span className="brand-name">Calendar</span>
      </Link>
      <Link to="/booking" className="button button-primary">
        Записаться
      </Link>
      <Link to="/admin" className="button">
        Админка
      </Link>
    </nav>
  );
}
