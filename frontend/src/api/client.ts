import { client } from './generated/client.gen';

// Запросы идут на тот же origin: в разработке их проксирует Vite,
// в production — веб-сервер, который раздаёт фронтенд.
client.setConfig({ baseUrl: '' });
