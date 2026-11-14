# Календарь звонков

[![hexlet-check](https://github.com/zhigeruha7sk/ai-for-developers-project-386/actions/workflows/hexlet-check.yml/badge.svg)](https://github.com/zhigeruha7sk/ai-for-developers-project-386/actions)

Сервис бронирования встреч: владелец публикует доступное время, гость выбирает тип встречи и
свободный слот. Подробное описание продукта — [docs/spec.md](docs/spec.md).

Учебный проект Хекслета: https://ru.hexlet.io/programs/ai-for-developers

## Стек

- **Backend:** Java 21, Spring Boot 4.1.2, Maven.
- **Frontend:** React 19, Vite 8, TypeScript 6.
- **Контракт:** TypeSpec 1.17 → OpenAPI 3 → TypeScript SDK (`@hey-api/openapi-ts`).
- **Инструменты:** [mise](https://mise.jdx.dev) — версии Java, Maven, Node, hk и pitchfork.
- **Проверки:** Spotless + google-java-format, ESLint + Prettier, JUnit, Vitest, Playwright.
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
