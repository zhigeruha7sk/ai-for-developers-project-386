# AGENTS.md

Правила для людей и ИИ-агентов, работающих с этим репозиторием.

## О проекте

Сервис бронирования встреч «Calendar»: владелец публикует доступное время, гость выбирает тип
встречи и свободный слот. Полное описание продукта — [docs/spec.md](docs/spec.md).

## Стек и структура

- `api/` — контракт TypeSpec, из него генерируется `api/openapi/openapi.yaml`.
- `backend/` — Java 21, Spring Boot 4.1.2, Maven (`backend/mvnw`).
- `frontend/` — React 19, Vite 8, TypeScript 6. SDK в `frontend/src/api/generated` генерируется и в
  git не попадает.
- `mise.toml` — версии инструментов, переменные окружения и все задачи.
- `Dockerfile` — production-образ: фронтенд и SDK собираются, статика кладётся в Spring Boot jar,
  приложение слушает `PORT`.
- `railway.json` — деплой Docker-образа на Railway и healthcheck `/api/health`.
- `opencode.json` — проектная конфигурация MCP-сервера Railway (OAuth, без секретов).
- `hk.pkl` — git-хуки, вызывают те же проверки, что и CI.
- `docs/spec.md` — источник требований к продукту.

## Команды

| Команда                                 | Что делает                                                       |
| --------------------------------------- | ---------------------------------------------------------------- |
| `mise install`                          | Ставит инструменты из `mise.toml` и зависимости проекта          |
| `mise run install`                      | Ставит зависимости npm и Maven                                   |
| `mise run check`                        | Все проверки: линтеры, типы, тесты                               |
| `mise run lint`                         | Линтеры TypeSpec, Java (Spotless), фронтенда (ESLint + Prettier) |
| `mise run typecheck`                    | Проверка типов фронтенда по сгенерированному SDK                 |
| `mise run test`                         | Тесты бэкенда (JUnit), фронтенда (Vitest) и браузерный E2E       |
| `mise run test:e2e`                     | Браузерный сценарий Playwright против настоящего сервера         |
| `mise run api:gen`                      | Компилирует TypeSpec в OpenAPI                                   |
| `mise run sdk:gen`                      | Генерирует TypeScript SDK из OpenAPI                             |
| `mise run build`                        | Собирает бэкенд и фронтенд                                       |
| `mise run dev`                          | Запускает daemons: бэкенд `:8080`, фронтенд `:5173`              |
| `mise daemons status` / `logs` / `stop` | Управление фоновыми сервисами                                    |

Перед коммитом достаточно `mise run check` — эта же команда выполняется в GitHub Actions.

## Контракт и генерация SDK

1. Источник правды — `api/main.tsp`. Меняйте контракт только там.
2. `mise run api:gen` обновляет `api/openapi/openapi.yaml`; файл коммитится.
3. `mise run sdk:gen` обновляет `frontend/src/api/generated`; папка в `.gitignore`.
4. Никогда не редактируйте сгенерированные файлы вручную — изменения потеряются.
5. CI повторяет генерацию и падает, если `api/openapi/openapi.yaml` не совпадает с `api/main.tsp`.
6. Фронтенд ходит в API только через сгенерированный SDK, а браузерный E2E работает против
   настоящих backend и frontend без подмены ответов API.

## Формат коммитов

Только [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <краткое описание>
```

- Типы: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `ci`, `build`, `perf`.
- Описание — в повелительном наклонении, без точки в конце, на английском.
- Один коммит — одно логическое изменение.
- Каждый коммит реализации ссылается на тикет: добавьте `Refs #<номер>` в тело коммита.
- Примеры: `feat(booking): add slot overlap validation` с `Refs #16`,
  `docs: describe local setup`, `ci: run mise check on push`.

Автоматические релизы ведёт release-please по Conventional Commits: на push в `main` он
открывает или обновляет release PR, а после его merge создаёт git tag и GitHub Release. Конфигурация —
`.github/workflows/release-please.yml`.

## Доменные правила

Их нельзя нарушать при реализации фич:

- Часовой пояс — `Asia/Almaty` (Казахстан).
- Рабочие часы владельца: 09:00–13:00 и 14:00–18:00.
- Окно записи — ближайшие 14 календарных дней, включая сегодняшний.
- Длительность слота равна длительности выбранного типа встречи; шаг стартов фиксируется в
  спецификации.
- Пересечения интервалов запрещены, в том числе частичные и между разными типами встреч. Следующая
  встреча может начинаться ровно в момент окончания предыдущей.
- Ограничения должны соблюдаться и при одновременных запросах.
- Владелец один, авторизации нет. Гость заходит без регистрации.
- Типы встреч заводит и редактирует владелец в админке; заметка к брони необязательна.
- Хранение — в памяти процесса; перезапуск очищает данные.
- Интерфейс — на русском языке.

## Ограничения репозитория

- Не редактировать и не удалять `.github/workflows/hexlet-check.yml`.
- Не переименовывать репозиторий.
- Не коммитить `node_modules`, `backend/target`, `frontend/dist`, `frontend/src/api/generated`.

## Agent skills

### Issue tracker

Issues живут в GitHub Issues этого репозитория (через CLI `gh`). См. `docs/agents/issue-tracker.md`.

### Triage labels

Дефолтные пять канонических ролей: needs-triage, needs-info, ready-for-agent, ready-for-human, wontfix. См. `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `GLOSSARY.md` + `docs/adr/` в корне репозитория. См. `docs/agents/domain.md`.
