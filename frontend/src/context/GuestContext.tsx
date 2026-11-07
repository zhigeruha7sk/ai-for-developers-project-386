import { useEffect, useState } from 'react';
import type { Guest } from '../lib/guest';

const defaultGuest: Guest = {
  name: '',
  avatar: '🦊',
};

/** Хранилище гостя. По умолчанию пустой. */
export function useGuest() {
  const [guest, setGuest] = useState<Guest>(defaultGuest);

  useEffect(() => {
    const stored = localStorage.getItem('calendar-guest');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setGuest(parsed as Guest);
      } catch {
        // ignore corrupt data
      }
    }
  }, []);

  const refresh = () => {
    const avatar = ['🦊', '🦉', '🐼', '🐺', '🦁'][Math.floor(Math.random() * 5)];
    const name = `${['Андрей', 'Борис', 'Владимир', 'Гость', 'Анна', 'Елена', 'Дмитрий'][Math.floor(Math.random() * 7)]}`;
    const newGuest: Guest = { name, avatar };
    setGuest(newGuest);
    localStorage.setItem('calendar-guest', JSON.stringify(newGuest));
  };

  return { guest, refresh };
}
