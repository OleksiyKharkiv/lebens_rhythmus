# Lebens Rhythmus — CHANGELOG
> Формат: [дата] [тип] [файл/область] — описание
> Типы: feat | fix | security | compliance | refactor | infra | docs
> Вставляй последние 10 записей в контекст AI при работе с затронутыми областями.
> Это файл генезиса проекта — фактические изменения и "грабли" (ошибочные
> предположения + их фикс), чтобы не наступать повторно. Не дублировать
> сюда обычные тикеты — им место в `docs/tickets/tickets.md`.

## 2026-07-22 — compliance: срочный фикс живых юр-страниц (Impressum/Datenschutz/AGB/Widerruf)

### Область (`frontend/pages/impressum/{impressum,datenschutzerklaerung,agb,widerruf}.html`)

- **compliance (примитивная "подушка безопасности", не замена юриста —
  см. LR-001)** — по прямому запросу владельца, критический разбор
  (не тикет — фикс сразу) четырёх юр-страниц, которые оказались
  ChatGPT-черновиком с живыми незаполненными плейсхолдерами
  (`[Nachname]`, `[Straße]`, `[PLZ Ort]`, `[Nummer]`) в проде — реальный
  риск Abmahnung по § 5 TMG прямо на момент находки. Реальные данные от
  владельца: Olena Khudoshyna, Ritterspornweg 1, 50129 Bergheim,
  Kleinunternehmer (§ 19 Abs. 1 UStG, НДС не взимается).
- **Конкретные находки и фиксы:**
  - Email на всех 4 страницах указывал на несуществующий домен
    `lebensrhythmus.de` (реальный — `tlab29.com`) — на странице Widerruf
    это было видно буквально: `href="mailto:info@tlab29.com"`, но
    видимый текст ссылки — `olena@lebensrhythmus.de`. Унифицировано на
    `info@tlab29.com` везде.
  - `§ 55 Abs. 2 RStV` — закон заменён Medienstaatsvertrag ещё в 2020 →
    `§ 18 Abs. 2 MStV`.
  - Раздел "Datenschutzbeauftragter" (Art. 37 DSGVO) с плейсхолдером
    вместо имени — **удалён**, не выдуман: обязательный DPO по §38 BDSG
    почти наверняка не требуется для бизнеса такого размера; ложное
    заявление о наличии DPO хуже отсутствия раздела.
  - Datenschutzerklärung §5 "Cookies" утверждала про session-cookies,
    хотя по коду (`main.js`) JWT реально лежит в `localStorage` —
    переписано под факт. Добавлен отсутствовавший раздел про реально
    собираемые при регистрации данные (включая IBAN/Steuer-ID
    преподавателей — те самые поля, зашифрованные в этой же сессии).
  - AGB §5.4 (новая) — явно зафиксировано, что законное право на
    Widerruf не отменяется договорной политикой отмены §5.2/5.3 (разные
    точки отсчёта — от даты договора vs от даты старта курса — могли
    конфликтовать).
  - AGB §6.4 — безусловная фраза "Eltern haften für ihre minderjährigen
    Kinder" сужена до "im Rahmen ihrer gesetzlichen Aufsichtspflicht
    (§ 832 BGB)" — блан­кетная ответственность родителей юридически не
    работает так просто.
  - Widerruf — убран нерелевантный пункт про "digitale Inhalte" (студия
    не продаёт цифровой контент); добавлен HTML-комментарий для
    разработчика: исключение права отзыва для услуг юридически
    работает только при явном чекбоксе согласия в форме записи,
    которого в текущем flow нет — не полагаться на эту оговорку в AGB,
    пока чекбокс не появится в UI.
  - HTML-баг в Datenschutzerklärung: секции 8-9 рендерились вне
    `.content`-обёртки — попутно исправлено при переносе/переномерации.
  - Опечатка "diese Seine" → "diese Seite" (SSL/TLS-раздел).
- **Не тронуто намеренно:** ссылка на EU ODR-платформу оставлена как
  есть — есть неподтверждённое подозрение, что платформа могла быть
  свёрнута Еврокомиссией в 2025, но не проверено достоверно, трогать
  на основе догадки не стал.
- **Только для старого статического фронтенда** (`frontend/`, то, что
  реально в проде сейчас) — у `frontend-svelte/` этих страниц пока нет
  вообще, будут построены заново с этими же реальными данными, когда
  дойдёт очередь до Волны 1.

## 2026-07-20 — docs: полная инвентаризация кодовой базы, ARCHITECTURE_OLD.md для круглого стола

### Область (`CLAUDE.md`, `docs/README.md`, `docs/context/{PROJECT_INDEX,KNOWN_ISSUES}.md`, `docs/runbooks/infra-fix-shutdown.md`, `docs/architecture/ARCHITECTURE_OLD.md`, `docs/tickets/tickets.md`, `backend/src/main/java/com/be/service/AuthService.java`)

- **docs** — калибровочная сессия: `docs/context/PROJECT_INDEX.md` и
  `CLAUDE.md` были собраны 2026-07-20 утром без доступа к реальному коду
  (только по логам/kubectl во время инфра-recovery) — сегодня же
  верифицированы и исправлены построчным чтением кода. Build tool
  подтверждён (Gradle, не Maven), отсутствие Flyway/Liquibase
  подтверждено, ссылки на несуществующие пути (`docs/README.md` →
  реально `docs_README.md`, `docs/ops/infra-fix-shutdown.md` → файла не
  было) исправлены.
- **docs** — реорганизация: `docs/ops/` (пустая директория-заглушка)
  упразднена в пользу разделения `docs/infra/` (справочник по топологии)
  + `docs/runbooks/` (процедурные рецепты восстановления, по аналогии с
  numi `docs/runbooks/`) — обоснование в `docs/README.md`.
- **docs** — `docs/tickets/tickets.md` заведён заново (backlog был пуст
  после ~6-месячного фриза проекта): LR-001 (DSGVO/GoBD-ревизия
  юридических страниц, до 2026-08-31), LR-002 (переработка CI/CD —
  тесты не запускаются, мёртвый `--set flannel.backend=host-gw` параметр),
  LR-003 (бэкапы PostgreSQL, near-term).
- **docs** — `docs/architecture/ARCHITECTURE_OLD.md` создан: полный
  архитектурный срез бэкенда (20 контроллеров, 17 entity, ERM), фронтенда
  (18 страниц, 18 JS-файлов, инвентаризация API-вызовов), CI/CD и инфры
  (Helm/Dockerfile/cert-manager), плюс разбор соответствия задуманной
  гексагональной архитектуре факту (вердикт: слоистый Spring MVC монолит
  с корректным DTO/mapper разделением, но НЕ гексагон — JPA-аннотации
  сидят прямо на domain entity, периферия прошита в ядро). Базовый
  документ для круглого стола архитекторов.
- **security (near-miss, не инцидент)** — при инвентаризации закомментированная
  строка `passwordEncoder.encode(...)` в `AuthService.register()` была по
  ошибке принята за "хеширование пароля отсутствует" (git blame показал
  только сам факт комментирования, без прослеживания вызова
  `UserService.createUser()`, где хеширование реально происходит с
  2025-11-30). Фикс "вернуть строку" **чуть не был закоммичен** — вернул
  бы реальный баг двойного хеширования (сломанный логин для всех новых
  регистраций). Пойман `architect-reviewer` до коммита, ничего не
  задеплоено. Урок и правило "хеширование пароля — только в
  `UserService`" зафиксированы в `docs/context/KNOWN_ISSUES.md`.
- **Найденные, но не исправленные в этой сессии проблемы** (вне скоупа
  инвентаризации, требуют отдельных тикетов/решений — см.
  `ARCHITECTURE_OLD.md` §6 и §2.5): `GroupController` — единственный
  контроллер без `@PreAuthorize` на write-методах; `JWT_SECRET` передаётся
  как plaintext `value:` в Helm-манифесте вместо `secretKeyRef` (в отличие
  от DB-credentials в том же файле); CORS настроен дважды независимо
  (`WebMvcConfig` + `SecurityConfig`), не проверено рантаймом на конфликт;
  **несоответствие документированной Cloudflare Tunnel-модели и
  найденного `devops/cloud_flare/cf_worker_lr.js`**, который проксирует
  напрямую на публичный IP:порт — открытый вопрос к заказчику, блокирует
  инфра-работу до выяснения.

## 2026-07-21 — feat/infra/docs: Roundtable #1 доведён до ответов Olena, ERM v2.0, старт кода (auth+шифрование, Flyway, новый SvelteKit-фронтенд)

### Область (`docs/decision-history/roundtable-log.md`, `docs/architecture/{decisions,IMPLEMENTATION-PROTOCOL-2026-07,lr-erm-2026-07.drawio}`, `docs/product/olena-questionnaire.md`, `.claude/{settings.local.json,launch.json}`, `backend/**`, `devops/helm/lr-app/**`, `.gitlab-ci.yml`, `frontend-svelte/**` (новая директория))

**Круглый стол и продукт:**
- **docs** — Roundtable #1 полностью проведён: панель из 17 именованных
  экспертов + Enthusiast/Tech Lead (`roundtable-log.md`), брифинг по
  `ARCHITECTURE_OLD.md`, вопросы владельцу, живое исследование реального
  UI (браузер/WebFetch не достучались до `tlab29.com` — 403/таймаут,
  прочитан исходник трёх страниц напрямую: нашли `lang="en"` при
  немецком тексте на 2 страницах, мёртвую ссылку "Passwort vergessen?",
  нулевую дизайн-систему), Round 3 — полный опросник Olena обработан.
  §6.1 закрыт: `cf_worker_lr.js` подтверждён как неиспользуемый (реальный
  трафик идёт через CF Tunnel).
- **docs** — `docs/architecture/decisions.md`: ADR-001..012 (техническая
  часть круглого стола) + ADR-013..019 (продуктовые решения из ответов
  Olena — аудитория/язык по умолчанию, визуальное направление, состав
  главного экрана, минимальный скоуп личного кабинета, двухканальная
  оплата, статус логотипа, простые admin-инструменты). Стал каноническим
  источником ADR — `CLAUDE.md` обновлён (ссылка на `PROJECT_INDEX.md` §8
  как черновик убрана).
- **docs** — `docs/product/olena-questionnaire.md` + HTML-версия (артефакт,
  не в git): 20 вопросов, ~80% UI/UX (закрытые вопросы, по best practices
  клиентских опросников — см. поиск в сессии) / ~20% функционал, простым
  языком.
- **docs** — `docs/architecture/lr-erm-2026-07.drawio` (валидный XML,
  проверено `python -c "xml.dom.minidom.parse(...)"`): 20 сущностей,
  3 цвета по волнам реализации (MVP / кабинет+оплата+роль
  "Преподаватель" / спектакли-остальное).
- **docs** — `IMPLEMENTATION-PROTOCOL-2026-07.md`: рабочая карта на время
  активной фазы фиксов, отдельно от ADR (постоянных) и лога (нарратива).

**Бэкенд (LR-ADR-002):**
- **security/refactor** — удалён мёртвый `CustomUserDetailsService`
  (нигде не использовался — не был подключён ни к какому
  `AuthenticationProvider`).
- **security** — `EncryptedStringConverter` (AES-256-GCM, ключ из
  `FIELD_ENCRYPTION_KEY`, fail-fast как в `JwtUtils`) — применён к
  `User.iban`/`User.taxId` (реально нашлись в коде банковские/налоговые
  поля, не гипотетические). 5 unit-тестов (round-trip, разный ciphertext
  на одинаковый plaintext, null passthrough, fail-fast на отсутствующий/
  короткий ключ) — все зелёные.
- **fix** — `JWT_EXPIRATION` был мёртвым параметром (Helm/CI его
  передавали, `JwtUtils` игнорировал, хардкодил константу) — теперь
  реально читается через `@Value`.
- **infra** — `JWT_SECRET` в `backend-deployment.yaml`: `value:` →
  `secretKeyRef` (Secret `lr-backend-secrets`, ключи `jwt-secret` +
  `field-encryption-key`). **Требует ручного шага перед деплоем** —
  команда создания Secret вписана в `values.yaml`, сам Secret не создан
  (нет доступа к кластеру из этой сессии).
- **infra** — `.gitlab-ci.yml`: убран теперь мёртвый
  `--set backend.env.JWT_SECRET=...` (тот же класс проблемы, что уже
  описан для `flannel.backend` в тикете LR-002 — deploy-параметр без
  потребителя в шаблоне).

**БД (LR-ADR-003):**
- **infra** — Flyway подключён (`flyway-core` + `flyway-database-postgresql`),
  `spring.jpa.hibernate.ddl-auto` → `validate`, `DatabaseFixConfig.java`
  удалён (тот самый runtime-костыль из `KNOWN_ISSUES.md`).
- **infra** — `V1__baseline.sql`: все 20 таблиц, построчно сверено с
  каждым `@Entity` (Docker был недоступен весь день — см. "грабли" ниже,
  генерация через живой Postgres невозможна).
- **найдено, не исправлено** — реальный баг модели: `Group.participants`
  (`@OneToMany @JoinColumn("participant_id")`, без `mappedBy`) и
  `Participant.group` (`@JoinColumn("group_id")`) — два независимых
  отображения одной и той же связи. Не косметика: `Group.participants`
  активно читается в `GroupService` (проверка вместимости). Не трогал —
  правка means редактировать `GroupService`, вне скоупа "добавить
  миграции". Кандидат на Wave 2 (роль "Преподаватель" всё равно
  переписывает `Group`).

**Фронтенд (LR-ADR-001/008/009/010/014/015):**
- **feat** — новый проект `frontend-svelte/` (SvelteKit, TypeScript,
  Tailwind v4, Paraglide i18n DE/UK/EN, `adapter-static`). Шрифты
  self-hosted через `@fontsource` (Quicksand + Nunito Sans) — осознанно,
  не Google Fonts CDN (утечка IP посетителя в Google без согласия —
  известная DSGVO-проблема для немецких сайтов, релевантно раз LR-001
  всё равно про DSGVO).
- **feat** — три страницы (Home/About/Login+Register) по координатам
  LR-ADR-014 (глубокий тёплый фон вместо чёрного + два ярких акцента —
  осознанно НЕ паттерн "неон на чёрном"). Login/Register реально
  собирают JSON и шлют на `/api/v1/auth/{login,register}` (тот же
  контракт, что у старого фронтенда).
- **infra** — `.claude/launch.json`: добавлена конфигурация
  `frontend-svelte` (порт 5174).
- **verified live** — dev-сервер поднят, все 3 страницы × 3 языка
  проверены через Browser-инструмент (`read_page`, не только компиляция):
  контент, заголовки `<title>`, консоль без ошибок, network — все запросы
  200/304 (включая шрифты).

### Грабли этой сессии (чтобы не наступать повторно)

1. **Docker Desktop весь день не был запущен** (`docker info` →
   `failed to connect to the docker API at npipe:...dockerDesktopLinuxEngine`,
   проверено минимум 3 раза в разное время суток, каждый раз одна и та же
   ошибка). Последствия: (а) `BackendApplicationTests` (Testcontainers)
   не мог пройти ни разу за сессию — не баг кода, ограничение среды; (б)
   `V1__baseline.sql` собран вручную построчным чтением entity, **не
   сгенерирован и не проверен против живого Postgres** — реальная
   валидация откладывается на `ddl-auto=validate` при первом настоящем
   старте приложения (упадёт громко, если есть ошибка, но раньше не
   поймать). **Урок:** перед началом любой БД-миграционной работы —
   сначала `docker info`, и если недоступен — либо запустить Docker
   Desktop, либо явно принять и озвучить риск "миграция не
   верифицирована", не тратить время на повторные попытки в течение
   сессии.
2. **`.claude/launch.json` — добавленная мидсессионно конфигурация не
   подхватывалась `preview_start`.** Добавил конфиг `frontend-svelte`,
   `preview_start({name: "frontend-svelte"})` вернул ошибку "Available
   servers: frontend, backend" (старый список) — **дважды**, с разным
   форматом конфига (сначала `cwd`-поле, потом `bash -c "cd ... && npm
   run dev"` по образцу рабочего `backend`-конфига) — оба раза тот же
   кэш. **Урок:** если `preview_start {name}` не видит только что
   добавленный конфиг — не тратить попытки на повторное редактирование
   `launch.json` в расчёте на реload; поднимать сервер напрямую через
   `Bash` (`run_in_background: true`) и подключать Browser-инструмент
   через `preview_start({url: "http://localhost:<port>"})` к уже
   работающему серверу.
3. **Paraglide i18n молча не переключал язык по URL** — `/en`, `/uk`
   корректно генерировались в ссылках (`localizeHref` работал), но
   реальный контент при прямом заходе на `/en` оставался немецким.
   Причина: `paraglideVitePlugin` по умолчанию использует
   `strategy: ["cookie", "globalVariable", "baseLocale"]` — **`"url"` не
   входит в дефолт**, хотя это самый очевидный сценарий (i18n-роутинг
   по префиксу пути). Нашёл только реальной проверкой в браузере
   (`read_page`/`<title>` на `/en`), не чтением конфига. **Фикс:**
   `strategy: ['url', 'cookie', 'baseLocale']` в `vite.config.ts` —
   после этого `/en`, `/uk` заработали корректно (перепроверено).
   **Урок:** при связке SvelteKit + Paraglide + URL-префиксы — сразу
   явно задавать `strategy` с `"url"` первым, не полагаться на дефолт,
   и обязательно проверять переключение языка живым запросом браузера,
   не только тем, что ссылки в навигации выглядят правильно.

## 2026-07-22 — fix/docs/security: Docker поднят владельцем, backend полностью протестирован, license policy

### Область (`docs/context/{CODING_PROTOCOL,KNOWN_ISSUES}.md`, `backend/src/test/resources/docker-java.properties`)

- **fix (грабли, продолжение вчерашней записи)** — владелец обновил и
  запустил Docker Desktop. Первая попытка прогона тестов вскрыла
  **второй, отдельный** Docker-баг: Docker Desktop 4.52+ поднял движок
  до v29, минимальная поддерживаемая API-версия выросла до 1.40, а
  `docker-java` внутри Testcontainers 1.x (версия, которую тянет Spring
  Boot 3.5.7) хардкодит откат на 1.32 вместо согласования — падало с
  `Status 400: client version 1.32 is too old`. Известный апстрим-баг
  (testcontainers-java #11210/#11235, апдейт от 2026). Фикс —
  `backend/src/test/resources/docker-java.properties` с
  `api.version=1.44` (обходной путь; настоящий фикс — апгрейд
  Testcontainers до 2.0.2+, не делал, крупнее по риску). Записано в
  `KNOWN_ISSUES.md`, чтобы не терять время повторно при следующем
  обновлении Docker Desktop.
- **verified** — после фикса: **7/7 тестов зелёные**
  (`BackendApplicationTests.contextLoads()` — реальный Postgres через
  Testcontainers, впервые в этой сессии; `PasswordEncoderTest`;
  5× `EncryptedStringConverter`). Это первая реальная проверка
  `V1__baseline.sql` против живого Postgres — раньше миграция была
  выверена только построчным чтением entity, без исполнения.
- **compliance (постоянное требование, добавлено владельцем)** — новое
  правило в `CODING_PROTOCOL.md` §3: только MIT-подобные (permissive)
  лицензии для новых зависимостей — MIT/BSD/ISC/Apache-2.0. Запрещены
  проприетарное ПО и copyleft/раскрытие-кода (GPL/LGPL/AGPL/MPL).
  Ретроактивно проверил всё добавленное в этой сессии: `flyway-core`/
  `flyway-database-postgresql` — Apache-2.0 (подтверждено поиском, не
  предположением — лицензирование Flyway после перехода на Redgate
  периодически путают); весь новый `frontend-svelte` стек (SvelteKit,
  Vite, Tailwind, TypeScript, Paraglide, Prettier) — MIT/Apache-2.0.
  Шрифты `@fontsource/{quicksand,nunito-sans}` — сам пакет MIT, но сами
  файлы шрифтов лицензированы под SIL OFL 1.1 (отдельная категория для
  шрифтов/медиа, не код, не проприетарна, не требует раскрытия исходников
  приложения — соответствует духу правила, но не буквально "MIT-подобная").
  Java-рантайм (`eclipse-temurin`, OpenJDK) — формально GPLv2, но с
  Classpath Exception — стандартное отраслевое исключение именно для
  этого случая, не нарушение.

