import { createBrowserRouter } from 'react-router-dom';
import { createMemoryRouter } from 'react-router-dom';
import Home from './pages/Home';
import BookingType from './pages/BookingType';
import Booking from './pages/Booking';
import Admin from './pages/Admin';

const routes = [
  { path: '/', element: <Home /> },
  { path: '/booking', element: <BookingType /> },
  { path: '/booking/:typeId', element: <Booking /> },
  { path: '/admin', element: <Admin /> },
];

export const router = createBrowserRouter(routes);

// For SSR/ hydration we fall back to a memory router during dev:
export const memoryRouter = createMemoryRouter(routes, { initialEntries: ['/'] });
