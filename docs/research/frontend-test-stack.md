# Стек тестов фронтенда: Vitest + Vite 8 / React 19

Исследование по тикету [#3](https://github.com/zhigeruha7sk/ai-for-developers-project-386/issues/3)
(метка `wayfinder:research`), ветка `research/frontend-test-stack`.

**Дата сверки:** 2026-11-10. Все номера версий и диапазоны совместимости получены из первоисточников
(npm registry через `npm view`, официальная документация Vitest/Testing Library) в день написания
файла — ничего не воспроизводилось по памяти.

## Вопрос

Какой точный тестовый стек нужен `frontend/` (React 19.3.1, Vite 8.3.5, TypeScript 6.0.3, тестов
сейчас нет вообще):

- какую мажорную версию `vitest` ставить;
- `jsdom` или `happy-dom` как DOM-окружение;
- нужны ли `@testing-library/react` и `@testing-library/jest-dom`;
- какая минимальная конфигурация и npm-скрипт.

Ограничения по объёму: ровно 2 сценария (happy-path брони; показ конфликта гостю), моки — на уровне
сгенерированного SDK (`frontend/src/api/generated`), реальный backend не поднимается.

## Рекомендация: список зависимостей (devDependencies)

Точные версии — в стиле остальных devDependencies репозитория (без `^`):

| Пакет                              | Версия   | Зачем в этом проекте                                        |
| ---------------------------------- | -------- | ----------------------------------------------------------- |
| `vitest`                           | `5.1.0`  | раннер, `vi.mock`/`vi.fn`, `expect`                          |
| `jsdom`                            | `31.0.0` | DOM-окружение для рендера компонентов                       |
| `@testing-library/react`           | `16.3.3` | `render`, `screen`, `fireEvent` для двух сценариев           |
| `@testing-library/dom`             | `10.4.2` | peer `@testing-library/react` ≥16 — обязателен с RTL v16     |
| `@testing-library/jest-dom`        | `7.0.1`  | матчеры `toBeInTheDocument`, `toHaveTextContent` и т. п.     |
| `@vitest/ui`                       | `5.1.0`  | **опционально**: `vitest --ui` для локального отладчика      |
| `@testing-library/user-event`      | `14.6.8` | **опционально**: реалистичные клики в сценарии брони         |

Команда установки обязательной части:

```bash
npm --prefix frontend install -D vitest@5.1.0 jsdom@31.0.0 @testing-library/react@16.3.3 @testing-library/dom@10.4.2 @testing-library/jest-dom@7.0.1
```

**Не нужен:** `jest`/`ts-jest` (дублируют Vitest), `MSW` (см. «Моки SDK» ниже), `happy-dom`
(см. раздел ниже), пакеты покрытия — на 2 теста преждевременно. Для двух сценариев `fireEvent` из
RTL достаточен, `user-event` — по желанию.

## Совместимость: проверка по первоисточникам

Все команды выполнялись 2026-11-10 через `mise exec node -- npm view …` (node `v24.22.2`,
npm `11.19.1` из `mise.toml`, `node = "24"`):

| Проверка                                        | Результат из первоисточника                                                                                          | Совместимо с проектом             |
| ----------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | --------------------------------- |
| `npm view vitest@5.1.0 peerDependencies`        | `vite: ^6.4.0 \|\| ^7.0.0 \|\| ^8.0.0`, `jsdom: "*"`, `happy-dom: "*"`, `@vitest/ui: 5.1.0`                          | vite `8.3.5` ✓                    |
| `npm view vitest@5.1.0 engines`                 | `node: ^22.12.0 \|\| ^24.0.0 \|\| >=26.0.0`                                                                          | node `24.22.2` ✓                  |
| `npm view jsdom@31.0.0 engines`                 | `node: ^22.22.2 \|\| ^24.15.0 \|\| >=26.0.0`                                                                         | node `24.22.2` ✓                  |
| `npm view @vitest/ui@5.1.0 peerDependencies`    | `vitest: 5.1.0` (точное совпадение версии)                                                                           | vitest `5.1.0` ✓                  |
| `npm view @testing-library/react` (версия)      | `16.3.3`                                                                                                             | —                                 |
| peerDependencies `@testing-library/react@16.3.3` | `react: ^18.0.0 \|\| ^19.0.0`, `react-dom: ^18.0.0 \|\| ^19.0.0`, `@testing-library/dom: ^10.0.0`                    | react `19.3.1` ✓, dom `10.4.2` ✓  |
| `npm view @testing-library/dom version`         | `10.4.2`                                                                                                             | —                                 |
| peerDependencies `@testing-library/jest-dom@7.0.1` | `vitest: >= 0.32`, `@testing-library/dom: >=10 <11`                                                                | vitest `5.1.0` ✓, dom `10.4.2` ✓  |
| `npm view @testing-library/user-event@14.6.8`   | peer `@testing-library/dom: >=7.21.4`                                                                                | dom `10.4.2` ✓                    |

Из официальной документации:

- vitest.dev/guide (Getting Started): «Vitest requires Vite >=v6.4.0 and Node >=v22.12.0» — Vite 8.3.5
  и Node 24 в диапазонах ✓. Скрипт по умолчанию — `"test": "vitest"`, для CI нужен `vitest run`.
- vitest.dev/guide/migration (Migrating to Vitest 5.0): Vite ≥ 6.4.0 и Node ≥ 22.12.0 — prerequisites
  мажорной версии 5; Vite в Vitest 5 перестал быть прямой зависимостью и стал peer-зависимостью, npm
  ставит peer-автоматически (в проекте `vite@8.3.5` уже есть) ✓.

Вывод: **Vitest 5 — текущая мажорная линейка (latest `5.1.0`) и она официально поддерживает
Vite 8** — брать её, не «стабильную четвёрку».

## jsdom против happy-dom: берём jsdom

Первоисточник — vitest.dev/guide/environment (Test Environment):

- `jsdom` — «emulates browser environment by providing Browser API, uses `jsdom` package»;
- `happy-dom` — «emulates browser environment by providing Browser API, **and considered to be faster
  than jsdom, but lacks some API**, uses `happy-dom` package».

Аргументы в пользу `jsdom` именно здесь:

1. Единственное преимущество happy-dom — скорость, а тестов будет 2 файла: разница в миллисекундах
   на прогон не имеет значения, а полнота API — имеет.
2. Экосистема Testing Library документируется под jsdom; матчеры `@testing-library/jest-dom`
   (`toBeVisible`, `toHaveStyle` и др.) опираются на `getComputedStyle`/каскад — неполная реализация
   happy-dom даёт ложные результаты.
3. Версии и engines сходятся с окружением репозитория: jsdom `31.0.0` требует node
   `^22.22.2 || ^24.15.0 || >=26.0.0`, в mise — node `24.22.2` ✓.
4. Если тестов станет существенно больше, переход дёшев: одна строка `environment: 'happy-dom'` в
   конфиге (и замена пакета) — Vitest поддерживает оба окружения из коробки.

## Минимальная конфигурация

### `frontend/vitest.config.ts` (создать)

```ts
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    include: ['src/**/*.test.{ts,tsx}'],
  },
});
```

Почему отдельный файл, а не блок `test` в `vite.config.ts`:

- `vitest.config.*` имеет приоритет над `vite.config.*` и **не мержится** с ним, поэтому плагин
  `@vitejs/plugin-react` (уже есть в devDependencies, `6.1.2`) продублирован явно — без него Vitest
  не транспилирует JSX/TSX;
- боевой `vite.config.ts` не трогается вовсе. Альтернатива (добавить `test` в `vite.config.ts`)
  потребовала бы правки импорта `defineConfig` на `vitest/config` или
  `/// <reference types="vitest/config" />` — тоже допустимо, но смешивает тесты с боевой конфигурацией.
- `globals: true` включает авто-очистку (cleanup) после каждого теста в React Testing Library —
  первоисточник: testing-library.com/docs/react-testing-library/setup, раздел «Auto Cleanup in
  Vitest» (без globals нужно вручную вызывать `cleanup()` в `setupFiles`).

### `frontend/src/test/setup.ts` (создать)

```ts
import '@testing-library/jest-dom/vitest';
```

Первоисточник: README `@testing-library/jest-dom`, раздел «With Vitest» — для Vitest нужен именно
импорт `/vitest`, подключаемый через `setupFiles`.

### `frontend/package.json` — добавить скрипты

```json
"test": "vitest run",
"test:watch": "vitest"
```

`test` (`vitest run`) — одноразовый прогон для CI и `mise run check`; `test:watch` — watch-режим
локально.

### `frontend/tsconfig.json` — обязательные правки

Без них упадут `mise run typecheck` и `npm run build` (оба запускают `tsc --noEmit`):

```json
{
  "compilerOptions": {
    "types": ["node", "vite/client", "vitest/globals", "@testing-library/jest-dom"]
  },
  "include": ["src", "vite.config.ts", "vitest.config.ts", "openapi-ts.config.ts"]
}
```

(`types: vitest/globals` — из-за `globals: true`; `@testing-library/jest-dom` — чтобы TypeScript знал
матчеры, источник: README jest-dom, раздел «With Vitest».)

### `.gitignore` (корень или frontend)

Добавить `.vitest/` — каталог артефактов Vitest 5 (витстенд: «Vitest and third party integrations
can use `.vitest` directory to store generated artifacts. It's recommended to add this in your
`.gitignore`»).

## Моки SDK: почему без MSW

- Мокаем модули сгенерированного SDK напрямую: `vi.mock('<путь в frontend/src/api/generated>', () =>
  ({ … }))` или `vi.spyOn` на экспорты. Сетевого слоя в тестах нет — fetch не поднимается, MSW
  перехватывал бы ровно то, чего здесь не происходит, и добавил бы инфраструктуру без выгоды.
- **Vitest 5:** `vi.mock`/`vi.hoisted` обязаны быть на top level файла — вызов внутри `describe`/`test`
  теперь не предупреждает, а бросает ошибку (миграция на 5.0). Это важно при написании двух сценариев.
- **Vitest 5:** `clearMocks` по умолчанию `true` — вызовы SDK-моков очищаются перед каждым тестом,
  удобно для проверки «SDK вызван ровно один раз». Кто-то может захотеть вернуть привычное поведение —
  `clearMocks: false` в конфиге.

## Что осталось вне этого тикета (шаги родителя)

1. Правки самих файлов конфигурации (`vitest.config.ts`, `setup.ts`, `tsconfig.json`,
   `package.json`/lockfile, `.gitignore`) — в этом исследовании ничего не устанавливалось.
2. Задача `frontend:test` в `mise.toml` и включение её в `[tasks.test]` (сейчас там только backend).
3. После установки — прогон `mise run check`: единственный непроверяемый удалённо момент совместимости
   — чтение `.d.ts` Vitest `5.1.0` компилятором TypeScript `6.0.3` (у Vitest нет peer-зависимости от
   `typescript`, проверяется запуском typecheck).

## Источники

- npm registry, команды `npm view <pkg>[@<версия>] version|peerDependencies|engines`, выполнялись
  2026-11-10 через `mise exec node -- npm` (node `24.22.2`).
- Vitest, Getting Started: <https://vitest.dev/guide/>
- Vitest, Test Environment (jsdom/happy-dom): <https://vitest.dev/guide/environment>
- Vitest, Migrating to Vitest 5.0: <https://vitest.dev/guide/migration>
- React Testing Library, Setup (auto cleanup в Vitest): <https://testing-library.com/docs/react-testing-library/setup>
- README `@testing-library/react` (установка `@testing-library/dom` начиная с RTL 16) —
  `npm view @testing-library/react readme`.
- README `@testing-library/jest-dom`, разделы «With Vitest» / «With TypeScript»:
  <https://github.com/testing-library/jest-dom#with-vitest>
- Файлы проекта: `frontend/package.json`, `frontend/vite.config.ts`, `frontend/tsconfig.json`,
  `mise.toml`.
