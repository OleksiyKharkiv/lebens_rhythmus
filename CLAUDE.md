# Lebens Rhythmus (LR) — tlab29.com, студия творческого развития

Read docs/README.md once if you're unsure where something lives — it's the
human-facing index of the folder structure below.

> Сестринский проект: `numi` (`C:\Users\hudos\IdeaProjects\numi`). Разные
> кодовые базы, разные CLAUDE.md, разные тикет-префиксы (LR-XXX vs NUMI-XXX).
> Не путать контекст между сессиями — каждая сессия стартует из своей рабочей
> директории, см. её собственный CLAUDE.md.

## Language conventions
- **Chat/session dialogue:** Russian.
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

## Grep-only — NEVER load whole
- docs/context/CHANGELOG.md — как накопится, `grep -n "<keyword>"`.
- docs/tickets/archive.md — закрытые тикеты, историческая справка.

## Per-ticket, not session-wide
- docs/tickets/tickets.md держит ВЕСЬ backlog. Смотри только свой тикет — его
  вставят или укажут явно. Backlog заведён заново 2026-07-20 (проект был в
  фризе ~6 месяцев) — docs/23_10_2025_MVP_tickets в корне docs/ до-фризовый,
  не в этой конвенции, не путать с активным трекером.

## Known open item
Frontend сейчас — статический multi-page HTML/CSS/JS через nginx (18
страниц, свой самодельный `partials-loader.js` для header/footer, без
бандлера). Круглый стол 2026-07-20 рассматривает переход на фреймворк
(кандидат — Svelte, по аналогии с numi ADR-002, но не решено) — это
ADR-уровневое решение, результат будет зафиксирован в
`docs/architecture/ARCHITECTURE_OLD.md` / решениях круглого стола, не делать
миграцию по умолчанию в рамках обычного тикета до утверждения роадмапа.

`docs/architecture/erm.drawio` и `docs/architecture/infrastructure_scheme.drawio`
— чекпоинт архитектуры на СТАРТЕ проекта (>8 месяцев назад, до полугодового
фриза) — исторический контекст для понимания стартовой идеи, НЕ текущая
истина. Не следовать как актуальной документации, сверяться с реальным
кодом (см. "Файловая карта" в PROJECT_INDEX.md после инвентаризации
2026-07-20).

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
  `docs/ops/infra-fix-shutdown.md`, никаких изменений сетевых/firewall правил
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

**Ticket bookkeeping тоже pre-approved:** Edit/Write на `docs/tickets/tickets.md`
и `docs/tickets/archive.md` — allow-listed, создание follow-up тикетов и
перенос закрытых в архив (шаги 7-8 workflow) не требуют permission prompt.
`git commit`/`git push` этим не затронуты — всегда требуют явной инструкции.

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
- Backend: **Gradle** (подтверждено 2026-07-20 — `build.gradle`, `gradlew`,
  CI использует `gradle:8.5-jdk21`), `./gradlew build`, package `com.be`,
  Java 21, Spring Boot 3.5.7. ⚠️ CI (`build-backend` job) сейчас вызывает
  только `./gradlew bootJar` — тесты (`BackendApplicationTests`,
  `PasswordEncoderTest`, Testcontainers) существуют в `src/test/`, но
  никогда не выполняются в пайплайне — см. тикет LR-002.
- Frontend: статический HTML/CSS/JS — нет build-шага пока не появится
  bundler; если добавят (Vite/etc, кандидат на замену всего фронтенда —
  Svelte, см. Known open item выше) — обновить этот раздел.
- DB-схема: **нет ни Flyway, ни Liquibase** — `spring.jpa.hibernate.ddl-auto=update`
  + рантайм-костыль `DatabaseFixConfig` (см. KNOWN_ISSUES.md). Не добавлять
  новые ALTER-в-коде по этому паттерну для новых изменений схемы — см.
  CODING_PROTOCOL.md запрещённые паттерны.
- Деньги/платежи: если появится платёжный домен — тесты сумм только через
  `BigDecimal`, никогда через `float`/`double` сравнение (см. CODING_PROTOCOL §forbidden).
