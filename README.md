# Календарь звонков

[![hexlet-check](https://github.com/zhigeruha7sk/ai-for-developers-project-386/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/zhigeruha7sk/ai-for-developers-project-386/actions)

Сервис бронирования встреч: владелец публикует доступное время, гость выбирает тип встречи и
свободный слот. Подробное описание продукта — [docs/spec.md](docs/spec.md).

Учебный проект Хекслета: https://ru.hexlet.io/programs/ai-for-developers

## Публичная ссылка

Приложение развёрнуто и доступно по адресу:
**<https://calendar-production-2a3ad289.up.railway.app>**

- Гость: <https://calendar-production-2a3ad289.up.railway.app/booking>
- Админка владельца: <https://calendar-production-2a3ad289.up.railway.app/admin>
- Проверка сервиса: <https://calendar-production-2a3ad289.up.railway.app/api/health>

Ссылка ведёт на тот же сервис, который собирается из `Dockerfile`: Spring Boot отдаёт и API, и
собранный фронтенд.

## Стек

- **Backend:** Java 21, Spring Boot 4.1.2, Maven.
- **Frontend:** React 19, Vite 8, TypeScript 6.
- **Контракт:** TypeSpec 1.17 → OpenAPI 3 → TypeScript SDK (`@hey-api/openapi-ts`).
- **Инструменты:** [mise](https://mise.jdx.dev) — версии Java, Maven, Node, hk и pitchfork.
- **Проверки:** Spotless + google-java-format, ESLint + Prettier, JUnit, Vitest, Playwright.
- **Контейнер:** multi-stage `Dockerfile`, один процесс Spring Boot на порту из `PORT`.
- **CI:** GitHub Actions.

## Установка

Нужны Git и [mise](https://mise.jdx.dev/installing-mise.html). Остальные инструменты mise поставит
сам.

```bash
git clone https://github.com/zhigeruha7sk/ai-for-developers-project-386.git
cd ai-for-developers-project-386
mise install
```

`mise install` ставит инструменты и через postinstall-хук зависимости проекта. Если хуки отключены,
выполните установку зависимостей отдельно:

```bash
mise run install
```

## Использование

Запуск бэкенда и фронтенда одной командой:

```bash
mise run dev
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080
- Проверка сервиса: http://localhost:8080/api/health

Управление фоновыми процессами:

```bash
mise daemons status
mise daemons logs
mise daemons stop
```

Отдельный запуск сервисов:

```bash
mise run dev:backend
mise run dev:frontend
```

## Docker

В корне лежит `Dockerfile`. На первом этапе он собирает фронтенд и TypeScript SDK, на втором —
Spring Boot jar со статикой фронтенда внутри, на третьем запускает приложение от непривилегированного
пользователя.

```bash
docker build -t calendar .
docker run --rm -e PORT=8080 -p 8080:8080 calendar
```

Приложение стартует автоматически и слушает порт из переменной `PORT`. Если переменная не задана,
используется `8080`. После запуска откройте <http://localhost:8080>, проверка сервиса —
<http://localhost:8080/api/health>.

## Деплой

Хостинг — [Railway](https://railway.com). Конфигурация лежит в `railway.json`: Railway собирает
образ по `Dockerfile`, передаёт приложению `PORT` и проверяет `/api/health`.

Порядок деплоя:

1. Подключите GitHub-репозиторий к Railway и создайте сервис из репозитория.
2. Railway сам найдёт `Dockerfile` и `railway.json`; отдельный build command не нужен.
3. Убедитесь, что у сервиса есть публичный домен: **Settings → Networking → Generate Domain**.
4. Проверьте `GET /api/health` на домене и откройте главную страницу.

## MCP хостинга

Проектная конфигурация агента — `opencode.json`. В ней подключён официальный удалённый MCP-сервер
Railway по адресу `https://mcp.railway.com`; секретов в файле нет.

Подключение через OAuth:

1. Откройте OpenCode в корне репозитория.
2. Выполните `/mcps`, выберите `railway` и войдите в аккаунт Railway в браузере.
3. Убедитесь, что статус сервера — `connected`.

Тот же вход из CLI (интерактивный, OAuth-токен хранится вне репозитория):

```bash
opencode mcp auth railway
opencode mcp list
```

Если нужен вход по токену, создайте account token в Railway и передайте его через переменную
окружения `RAILWAY_API_TOKEN`; не записывайте токен в `opencode.json`, README или git.

## Хранение данных

Данные хранятся в памяти процесса: брони и типы встреч не пишутся в базу. После перезапуска
приложения, контейнера или повторного деплоя на хостинге записи и созданные типы встреч
сбрасываются; начальные типы «Встреча 15 минут» и «Встреча 30 минут» создаются заново. Это же
означает, что публичная ссылка не подходит для долговременного хранения записей.

## Проверки

Одна команда запускает линтеры, проверку типов, тесты бэкенда и фронтенда, а также браузерный
E2E-сценарий против настоящего сервера — локально и в GitHub Actions:

```bash
mise run check
```

Отдельные задачи:

```bash
mise run lint
mise run typecheck
mise run test
```

## Контракт API и SDK

- `api/main.tsp` — источник правды для API.
- `mise run api:gen` собирает `api/openapi/openapi.yaml` (коммитится).
- `mise run sdk:gen` генерирует TypeScript SDK в `frontend/src/api/generated` (в `.gitignore`).
- Сгенерированные файлы не редактируются вручную.

## Структура

```
api/        TypeSpec-контракт и OpenAPI
backend/    Spring Boot-приложение
frontend/   React + Vite
docs/       спецификация продукта
mise.toml   инструменты, окружение и задачи
hk.pkl      git-хуки
```

Правила разработки и формат коммитов — [AGENTS.md](AGENTS.md).

---

<details>
<summary>Автоматические тесты Хекслета</summary>

Тесты запускаются на каждый коммит. За запуск отвечает файл `.github/workflows/hexlet-check.yml` — не удаляйте и не переименовывайте ни его, ни репозиторий.

</details>

## О Хекслете

[Хекслет](https://ru.hexlet.io/) — школа программирования: авторские программы обучения с практикой, поддержкой наставников и реальными проектами, которые остаются в резюме. Этот репозиторий — один из таких проектов.
