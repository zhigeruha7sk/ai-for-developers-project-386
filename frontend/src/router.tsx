import { createBrowserRouter, createMemoryRouter } from 'react-router-dom';
import Layout from './components/Layout';
import Home from './pages/Home';
import BookingType from './pages/BookingType';
import Booking from './pages/Booking';
import Admin from './pages/Admin';

// Шапка (логотип + Записаться + Админка) на всех страницах — общий Layout.
const routes = [
  {
    element: <Layout />,
    children: [
      { path: '/', element: <Home /> },
      { path: '/booking', element: <BookingType /> },
      { path: '/booking/:typeId', element: <Booking /> },
      { path: '/admin', element: <Admin /> },
    ],
  },
];

export const router = createBrowserRouter(routes);

// For SSR/ hydration we fall back to a memory router during dev:
export const memoryRouter = createMemoryRouter(routes, { initialEntries: ['/'] });
