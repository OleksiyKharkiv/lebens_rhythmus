# Lebens Rhythmus (LR) — tlab29.com, студия творческого развития

Read docs/README.md once if you're unsure where something lives — it's the
human-facing index of the folder structure below.

> Сестринский проект: `numi` (`C:\Users\hudos\IdeaProjects\numi`). Разные
> кодовые базы, разные CLAUDE.md, разные тикет-префиксы (LR-XXX vs NUMI-XXX).
> Не путать контекст между сессиями — каждая сессия стартует из своей рабочей
> директории, см. её собственный CLAUDE.md.

## Language conventions
- **Chat/session dialogue:** Russian, informal address ("ты", not "вы") —
  explicit instruction 2026-07-24, applies across sessions.
- **Code comments** (backend `.java`, frontend `.js`/`.html`/`.css` — актуальный
  исходный код, не доки): English only, always, даже в русскоязычной сессии.
  Если в файле, который редактируешь, остался русскоязычный комментарий —
  переведи его как часть этого же edit'а, не оставляй смешанный язык.
- **Docs** (всё под `docs/`, плюс этот файл): Russian по умолчанию. Исключение
  — этот файл (`CLAUDE.md`) остаётся на английском: это конфигурация для
  Claude Code, не документация для человека, аналогично тому как класс на Java
  пишется по-английски независимо от языка разработчика.
- **Сообщения для третьих лиц** (переводы для клиентов/родителей на DE/EN):
  всегда сопровождать русским обратным переводом в том же ответе для проверки
  перед отправкой.
- **Commit messages:** всегда English, Conventional Commits. Включай ID тикета
  (`LR-XXX`) если коммит привязан к тикету; тривиальные коммиты без тикета
  (typo, favicon, `.gitignore`) — не выдумывать ID. **Никогда не добавлять
  `Co-Authored-By: Claude ...` трейлер**, если явно не попросили — тот же
  принцип что и в numi.

## Always-loaded context
@docs/context/CODING_PROTOCOL.md
@docs/context/KNOWN_ISSUES.md

## Read on demand (do NOT auto-load)
- docs/context/PROJECT_INDEX.md — архитектура, файловая карта, ADR, глоссарий.
- docs/architecture/decisions.md — канонический источник ADR (Context →
  Decision → Consequences). Заведён 2026-07-20 по итогам Roundtable #1
  (LR-ADR-001..012 + перенесённые INFRA-ADR-001..005) — больше не
  черновик в PROJECT_INDEX.md §8, тот раздел устарел, сверяться с этим
  файлом.
- docs/infra/INFRA-LR.md — топология инфраструктуры (узлы, DNS, Cloudflare
  Tunnel, Helm-чарт) — справочник "что и почему так построено".
- docs/runbooks/infra-fix-shutdown.md — пошаговые рецепты восстановления
  после сбоя, процедурный документ "что делать прямо сейчас" (разделение от
  infra/ решено 2026-07-20, см. docs/README.md). Читать при любой
  инфра-задаче или инциденте.
- docs/compliance/DATENSCHUTZ.md — только когда диф касается персональных
  данных несовершеннолетних, платежей или auth (см. HIGH-tier ниже). Файла
  ещё нет (только страницы-заглушки во frontend/pages/impressum/*, без
  юридической ревизии) — см. тикет LR-001, срок конец августа 2026.
- **docs/security/ARCHITECTURE.md** — синтезирующий обзор "как устроена
  вся система безопасности целиком и почему" (не дублирует audit-2026-08-06.md
  / roadmap.md / decisions.md — ссылается на них за деталями). Читать при
  любом security-тикете для контекста, и **обязательно обновлять в том же
  диффе** при любом изменении в системе безопасности (новая мера защиты,
  новая метрика, закрытие находки, изменение alerting) — правило закреплено
  в самом файле, не опционально.

## Grep-only — NEVER load whole
- docs/context/CHANGELOG.md — как накопится, `grep -n "<keyword>"`.
- docs/tickets/archive.md — закрытые тикеты, историческая справка.
- docs/decision-history/roundtable-log.md — добавлено 2026-08-13
  (артефакт-аудит): 1338+ строк, 8+ роундтейблов, тот же профиль размера,
  что у файлов выше. Читать целиком нет смысла — `grep -n "Roundtable #N"`
  или по теме, если ссылается на него ADR в decisions.md.

## Per-ticket, not session-wide
- docs/tickets/tickets.md держит ВЕСЬ backlog. Смотри только свой тикет — его
  вставят или укажут явно. Backlog заведён заново 2026-07-20 (проект был в
  фризе ~6 месяцев) — docs/23_10_2025_MVP_tickets в корне docs/ до-фризовый,
  не в этой конвенции, не путать с активным трекером.

## Known open item
**Обновлено 2026-08-13 (артефакт-аудит) — предыдущая версия этого раздела
была устаревшей минимум с 2026-07-21/22, см. находку ниже.**

Frontend-фреймворк вопрос **решён**: `LR-ADR-001` (Roundtable #1,
2026-07-20) — полный снос статического MPA и перестройка на SvelteKit,
не поэтапная миграция. Выполнено: `frontend-svelte/` — реальный
SvelteKit-проект (`adapter-static`, SPA-fallback mode из-за клиентской
загрузки данных через `$effect`, см. `LR-002` в tickets/archive), давно
в проде на `tlab29.com`. Старый статический MPA — история, не текущее
состояние. `ARCHITECTURE_OLD.md` — это НЕ запись итогового решения (как
ошибочно утверждала прошлая версия этого раздела), а инвентаризация
"как было" от 2026-07-20, использованная как baseline для самого
Roundtable #1 — читать с этим пониманием, не как текущую архитектуру.

`docs/architecture/erm.drawio` — **актуально поддерживается**, не
исторический чекпоинт: каждое архитектурно значимое решение с
`LR-ADR-021` (2026-08-09, эпик "Курсы") обновляет этот файл в том же
диффе (см. Consequences-секции `LR-ADR-021`..`LR-ADR-023`). Доверять как
текущей истине, но — как и любому артефакту — стоит проверить дату
последнего связанного ADR, если диф выглядит подозрительно старым.

`docs/infra/infrastructure_scheme.drawio` (путь исправлен — раньше здесь
ошибочно стоял `docs/architecture/infrastructure_scheme.drawio`, файла
там нет) — актуальность НЕ проверена в рамках этого аудита, статус
неизвестен, не приравнивать к erm.drawio.

## Ticket risk tiers — match ceremony to actual risk
Указывай tier в первом ответе по тикету.

- **LOW** — чистый frontend/CSS/copy/UI, без касания service/entity слоя,
  без docs/ вне самого тикета: plan → code → test. Без Architecture Pre-Check,
  без reviewer-субагента, если что-то не насторожило по ходу.
- **MED** — новый endpoint/controller/service-метод, не касается auth, данных
  несовершеннолетних, платежей: полный Architecture Pre-Check
  (CODING_PROTOCOL.md §1-3). `architect-reviewer` опционально, использовать
  если диф трогает больше 2-3 файлов.
- **HIGH** — касается auth/JWT, персональных данных детей (ФИО, дата рождения,
  контакты опекунов, медицинские/аллергические заметки для физической
  активности), платежей/бронирования, либо пересекает ARCH-1..4 границу, либо
  подразумевает новое ADR-уровневое решение: полный Architecture Pre-Check +
  Datenschutz-чеклист (CODING_PROTOCOL.md §4) + `architect-reviewer`
  ОБЯЗАТЕЛЕН перед approval.
- **INFRA** — касается k8s-манифестов, Cloudflare Tunnel, GitLab CI/CD,
  Proxmox/VM-конфигурации: **обязательно** свериться с
  `docs/runbooks/infra-fix-shutdown.md` (путь исправлен 2026-08-13 —
  `docs/ops/` не существует с 2026-07-20, см. docs/README.md), никаких
  изменений сетевых/firewall правил
  без явного проговаривания последствий. Мы уже словили один серьёзный
  инцидент (power outage → каскад из 7 независимых поломок) — ceremony здесь
  оправдана.

## Parallel work (multiple tickets)
Один тикет = один worktree = одна ветка: `claude --worktree LR-XXX` на тикет,
отдельный терминал на каждый. LOW-tier UI-баги — хорошие кандидаты для 2-3
параллельных worktree.

## Review before apply (MED/HIGH/INFRA тикеты)
Делегировать `architect-reviewer` субагенту (`.claude/agents/architect-reviewer.md`)
перед показом дифа на approval. Он не пишет код. Если находит проблему —
доработать, перетестировать, ре-ревью. Финальный approval всегда за мной.

## Exploration is pre-approved — don't ask, just look
Read, Grep, Glob и Bash(ls/cd/cat/find/grep/head/tail/pwd) где угодно в
пределах проекта — allow-listed в `.claude/settings.local.json`. Не нужно
спрашивать разрешение на исследование кода/docs/. Спрашивать только перед
Write, Edit, либо git-командой, мутирующей историю (commit/push).

**Ticket bookkeeping и CHANGELOG тоже pre-approved (расширено 2026-08-04):**
Edit/Write на `docs/tickets/tickets.md`, `docs/tickets/archive.md` и
`docs/context/CHANGELOG.md` — allow-listed в `settings.local.json`, не
требуют permission prompt. Конкретно значит:
- Обновление `CHANGELOG.md` после успешного закрытия работы (шаг 7 workflow)
  — без спроса.
- Перенос закрытых тикетов в `archive.md` (шаг 8) — без спроса.
- Заведение нового тикета в `tickets.md` **в моменте по ходу работы, если
  я явно подтвердил его создание в диалоге** (как было 2026-08-04 с
  LR-013, email-верификацией) — тоже без повторного спроса. Подтверждение
  должно быть реальным (я сказал "да, заводи тикет" / согласился на
  предложенный тикет), не самостоятельным решением Claude завести тикет
  без вопроса в первый раз.
`git commit`/`git push` этим не затронуты — всегда требуют явной инструкции
каждый раз, это отдельное правило и `ask`-запись в `settings.local.json`.

## Workflow contract
1. Озвучить risk tier тикета (LOW/MED/HIGH/INFRA) в начале.
2. Architecture Pre-Check если MED/HIGH/INFRA (формат — CODING_PROTOCOL.md §5).
3. План, дождаться approval перед написанием кода.
4. Написать код.
5. `architect-reviewer` субагент если MED/HIGH/INFRA.
6. Conventional commit message (English; `LR-XXX` если привязан к тикету).
7. Обновить docs/context/CHANGELOG.md (по файлам, по своему шаблону).
8. Перенести тикет в docs/tickets/archive.md (pre-approved).
9. НИКОГДА не делать git commit / push без явной инструкции — это же
   закреплено правилом `ask` в settings.local.json.

## Build & verify
**Обновлено 2026-08-13 (артефакт-аудит)** — предыдущая версия описывала
состояние на 2026-07-20/22, до эпика "Курсы" и полной пересборки
фронтенда; проверено против реального кода/CI/`decisions.md`.

- Backend: **Gradle** (`build.gradle`, `gradlew`, CI — `gradle:8.5-jdk21`),
  `./gradlew build`, package `com.be`, Java 21, Spring Boot 3.5.7.
  27 тестовых файлов в `src/test/java` на 2026-08-13 (юнит + Testcontainers-
  интеграционные), локально зелёные. ⚠️ **Всё ещё актуально, не устарело:**
  CI (`build-backend`) вызывает только `./gradlew bootJar` — тесты в
  пайплайне не выполняются вообще, ни разу, до сих пор (подтверждено
  прямым чтением `.gitlab-ci.yml` и открытого тикета `LR-002`, п.1
  явно помечен "не устранено в CI"). Каждый прогон тестов этой сессией
  был только локальным — реальная защита от регрессии в CI отсутствует.
- Frontend: **SvelteKit** (`frontend-svelte/`, `adapter-static`, SPA-fallback
  mode) — `npm run build`/`npm run check`/`npm test` (vitest). Старый
  статический MPA — история, см. `LR-ADR-001`.
- DB-схема: **Flyway**, `backend/src/main/resources/db/migration/`,
  `V1__baseline.sql` .. `V10__...` на 2026-08-13 (`LR-ADR-003`). Раньше
  здесь стоял `ddl-auto=update` + `DatabaseFixConfig` — оба убраны, не
  актуальны, не предлагать их паттерн для новых изменений схемы.
- Деньги/платежи: если появится платёжный домен — тесты сумм только через
  `BigDecimal`, никогда через `float`/`double` сравнение (см. CODING_PROTOCOL §forbidden).
