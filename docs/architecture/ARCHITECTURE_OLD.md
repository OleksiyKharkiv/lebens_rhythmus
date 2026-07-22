# ARCHITECTURE_OLD.md — Lebens Rhythmus, полный архитектурный срез "как есть"

> Версия: 1.0 · Дата: 2026-07-20 · Автор среза: Claude (калибровочная сессия)
> **Назначение:** базовый документ для круглого стола архитекторов LR. Не
> план, не рекомендации — фактическая инвентаризация текущего состояния,
> верифицированная чтением реального кода (не логов/kubectl, как черновик
> `PROJECT_INDEX.md` от 2026-07-20 07:xx). Где факт не проверен построчно —
> помечено явно `[ПРЕДПОЛОЖЕНИЕ]` или `[ОТКРЫТЫЙ ВОПРОС]`, не выдаётся за
> подтверждённое.
>
> Формат — плотный, таблично-тезисный, рассчитан на AI-ревьюеров круглого
> стола не хуже, чем на человека. Ссылки на файлы — `path:line`, кликабельны
> в IDE.

---

## 0. TL;DR для тех, кто прочитает только этот абзац

LR задумывался как гексагональная архитектура (домен неприкосновенен,
периферия — БД/фронтенд/интеграции — заменяема). По факту получился
**добротный слоистый Spring MVC CRUD-монолит** с чистым разделением
`controller → service → repository → entity` и опрятными DTO/mapper —
но НЕ гексагональный: JPA-аннотации сидят прямо на доменных сущностях
(периферия — Hibernate — прошита в ядро), нет ни одного port/adapter для
внешних систем (что ожидаемо — внешних интеграций пока нет вообще).
Фронтенд — 18-страничный статический MPA без фреймворка, но с на удивление
дисциплинированным `main.js` (общий fetch-wrapper, XSS-экранирование) —
только используется непоследовательно. Инфра рабочая, с пробелами
(нет бэкапов БД, нет миграций, "мёртвые" параметры в CI, несогласованная
работа с секретами). Один открытый вопрос требует ответа заказчика ДО
круглого стола — см. §6.1 (Cloudflare Worker против задокументированного
Tunnel).

---

## 1. Общие архитектурные решения — задумка vs реальность

### 1.1 Задумка (со слов заказчика, зафиксирована здесь впервые)

Платформа проектировалась по модели **гексагональной архитектуры
(ports & adapters)**:
- Бизнес-логика — "священная корова", неприкосновенное ядро.
- Все зависимости направлены **внутрь**, к домену.
- Периферия (фронтенд, БД/СУБД, платёжные системы, внешние интеграции)
  — заменяема в любой момент без краша платформы. Любой адаптер может
  "прийти и уйти", ядро этого не замечает.

### 1.2 Реальность (проверено чтением кода, не предположение)

```
com.be
├── config/            Security, CORS, JWT — инфраструктурная обвязка
├── domain/
│   ├── entity/         17 JPA @Entity классов — ЭТО И ЕСТЬ "домен"
│   └── repository/      Spring Data JPA интерфейсы
├── service/            бизнес-логика, по классу на сущность + Auth
└── web/
    ├── controller/      REST, 20 контроллеров
    ├── dto/{request,response}/   DTO строго разделены
    ├── mapper/          entity ↔ DTO
    └── handler/         GlobalExceptionHandler
```

Направление зависимостей `controller → service → repository → entity`
выдержано насквозь — не нашёл ни одного контроллера с прямым SQL/
EntityManager, ни одного сервиса с `ResponseEntity`/HTTP-знанием (см. §5
для полного разбора). Это **настоящая многослойная архитектура**, спроектирована
не наспех.

Но это **не гексагон**: `domain/entity/` — не независимая доменная модель,
это JPA entity-классы, обвешанные `@Entity`/`@Table`/`@OneToMany`/
`@JoinColumn`. Hibernate (периферия по гексагональной идее) физически
прошит в объявление каждого доменного класса. Полный разбор — §5.

---

## 2. Backend + БД

### 2.1 Стек (подтверждено, не из логов)

| Слой | Технология | Источник подтверждения |
|---|---|---|
| Backend | Spring Boot 3.5.7, Java 21 | `build.gradle` |
| Build | **Gradle** (не Maven) | `build.gradle`, `gradlew`, CI использует `gradle:8.5-jdk21` |
| DB | PostgreSQL 16 | `pg-statefulset.yaml`, `application.properties` |
| ORM | Hibernate / Spring Data JPA | `build.gradle`, entity-аннотации |
| Auth | Spring Security + OAuth2 Resource Server (JWT, HS256) | `SecurityConfig.java` |
| Frontend | Статический HTML/CSS/JS, nginx | `frontend/Dockerfile` |

### 2.2 Доменная модель (ERM, по факту `domain/entity/`)

17 сущностей. Ключевые связи (не все FK, только образующие граф):

```
User ──1:N── Enrollment ──N:1── Workshop ──1:N── Group ──N:1── Activity
                                    │                │
                                    │                ├─N:1─ AgeGroup
                                    │                ├─N:1─ Language
                                    │                └─N:1─ Teacher
                                    ├─N:1─ Venue
                                    ├─N:1─ Teacher
                                    ├─N:1─ Language
                                    └─N:1─ AgeGroup

Order ──N:1── User, Participant, Workshop, Event, Contract
Order ──1:N── Payment ──N:1── User
Contract ──1:N── Order, Event
Event ──N:1── Venue, Workshop, Contract
Performance ──N:1── Workshop
Participant ──N:1── Group
Feedback ──N:1── User
Notification ──1:N── UserNotification ──N:1── User
Teacher/Workshop/WorkshopFile/File — файлы прикреплены к Workshop
```

**Важно про `Group`:** JPA-таблица называется `workshop_groups` — это тот
самый `DatabaseFixConfig` из `KNOWN_ISSUES.md` (runtime `ALTER TABLE ...
DROP NOT NULL` на `activity_id/age_group_id/language_id/teacher_id`), см. §2.4.

**Скрытая мультиязычность (важная находка, см. §3.6):** `Activity` и
`Group` имеют поля `titleDe/titleEn/titleUa` (`Activity` — ещё и
`descriptionDe/En/Ua`) — БД **уже** спроектирована под 3 языка контента.
Отдельно есть таблица `languages` (`Language` entity) — но это язык
**проведения** занятия (на каком языке идёт урок), не язык UI. Два разных
понятия i18n сосуществуют в модели, оба не выведены во фронтенд (см. §3.6).

**Деньги — везде `BigDecimal`:** `Workshop.price`, `Event.price`,
`Payment.amount`, `Order.amount`, `Contract.amount`, `Activity.price` — ни
одного `float`/`double`. Хорошая гигиена, соответствовала бы и
CODING_PROTOCOL numi один в один.

### 2.3 REST API — полная карта (20 контроллеров, `/api/v1/**`)

| Controller | Path | Public | User-tier | Роли (ADMIN/BUSINESS_OWNER/TEACHER) |
|---|---|---|---|---|
| Auth | `/auth/login`, `/auth/register` | POST оба | — | — |
| Workshop | `/workshops` GET, `/workshops/{id}` GET | да | — | POST/PUT/DELETE: ADMIN\|BO; `/workshops/teacher/{id}`: TEACHER\|BO\|ADMIN |
| Enrollment | `/workshops/{id}/enroll` POST | — | isAuthenticated | `/admin/workshops/{id}/participants`: ADMIN\|BO; `/teacher/groups/{id}/participants`: TEACHER\|BO\|ADMIN |
| Activity | `/activities` CRUD | GET | — | POST/PUT/DELETE: ADMIN\|BO |
| AgeGroup | `/age-groups` CRUD | GET | — | write: ADMIN\|BO |
| Language | `/languages` CRUD | GET | — | write: ADMIN\|BO |
| Teacher | `/teachers` CRUD | GET | — | write: ADMIN\|BO, delete: ADMIN |
| Venue | `/venues` CRUD | GET | — | write: ADMIN\|BO, delete: ADMIN |
| Group | `/groups` CRUD + `/groups/activity/{id}`, `/groups/teacher/{id}` | GET (все) | — | write: **без `@PreAuthorize` вообще** — см. §2.5 |
| Participant | `/participants` CRUD | — | — | всё: ADMIN\|BO\|TEACHER (read), write ADMIN\|BO, delete ADMIN |
| Contract | `/contracts` CRUD | — | — | всё: ADMIN\|BO |
| Order | `/orders` CRUD | — | POST: isAuthenticated | GET list/PUT: ADMIN\|BO; GET/{id}: +isAuthenticated; DELETE: ADMIN |
| Payment | `/payments` CRUD | — | — | всё: ADMIN\|BO, delete: ADMIN |
| Event | `/events` CRUD | GET | — | write: ADMIN\|BO |
| Performance | `/performances` CRUD | GET | — | write: ADMIN\|BO |
| Feedback | `/feedbacks` | GET, POST(auth) | isAuthenticated (POST) | delete: ADMIN\|BO |
| File | `/files` CRUD | — | — | всё: ADMIN\|BO |
| Notification | `/notifications` CRUD | — | — | всё: ADMIN |
| UserNotification | `/user-notifications`, `/{id}/read` | — | isAuthenticated | — |
| User | `/users/me` GET/PUT, `/me/password` PUT, `/me/verify-email` POST, `/me` DELETE | — | isAuthenticated (self) | list/search/role/{id}/stats: ADMIN |

**Находка (§2.5): `GroupController` — единственный контроллер без
`@PreAuthorize` на write-методах** (`POST/PUT/DELETE /groups/**`,
[GroupController.java:40-56](../../backend/src/main/java/com/be/web/controller/GroupController.java)).
Все остальные контроллеры явно требуют роль на мутирующих операциях. Не
проверял, есть ли метод-level security по умолчанию `denyAll` где-то в
`SecurityConfig` (там `.anyRequest().authenticated()` — то есть **любой
залогиненный пользователь**, включая обычный `USER`, может
создавать/менять/удалять группы занятий). Это либо осознанное решение
(маловероятно), либо пропущенная аннотация — **стоит явного решения на
круглом столе / отдельного тикета**, не чинил сам (вне текущего скоупа
сессии).

### 2.4 Проблема отсутствия миграций (главная находка бэкенда)

**Flyway/Liquibase — не подключены вообще.** `build.gradle` не содержит
ни одной migration-библиотеки. Схема БД управляется:

1. `spring.jpa.hibernate.ddl-auto=update` — Hibernate сам решает, как
   должна выглядеть схема, на основе текущих entity-классов, и молча
   применяет `ALTER`/`CREATE` при каждом старте. Никакой версионности,
   никакой истории изменений, никакого отката.
2. Поверх этого — `DatabaseFixConfig`
   ([DatabaseFixConfig.java](../../backend/src/main/java/com/be/config/DatabaseFixConfig.java)):
   `@PostConstruct`, при каждом старте приложения выполняет
   `ALTER TABLE workshop_groups ALTER COLUMN <x> DROP NOT NULL` на 4
   колонках через `JdbcTemplate`. Существует **только потому**, что
   `ddl-auto=update` умеет добавлять NOT NULL, но не умеет их снимать —
   то есть это ручной костыль поверх автоматического костыля.

**Почему это системный риск, а не просто "некрасиво":**
- Реальная схема прод-БД в любой момент — это накопленный эффект истории
  entity-классов + этого одного `@PostConstruct`, а не что-либо
  человекочитаемое и версионированное.
- Откат невозможен — нет migration history, нечего откатывать.
- Изменение entity (даже безобидное на вид) может неявно повлиять на
  реальную схему без ревью — Hibernate решает сам.
- При переходе на новую БД/окружение (staging, restore из бэкапа) — схема
  воссоздаётся заново по текущему коду, не по фактической истории
  изменений, что расходится с GoBD-подходом numi (там миграции — часть
  архитектурных инвариантов, ADR-005 аналог).

### 2.5 Прочие находки бэкенда

- **CORS настроен дважды, независимо:** `WebMvcConfig` (Spring
  `CorsRegistry`, использует `CorsProperties` бин) **и**
  `SecurityConfig.securityFilterChain()` (`.cors(Customizer.withDefaults())`,
  который ищет отдельный `CorsConfigurationSource` бин). Не верифицировал
  рантаймом, действительно ли оба механизма реально активны одновременно
  без конфликта — `[ОТКРЫТЫЙ ВОПРОС]`, стоит проверить явно (напр. через
  фактический preflight-запрос), не гадать по коду.
- **`JwtUtils` vs `JwtAuthUtils`** — не дублирование, разная
  ответственность: `JwtUtils` минтит токен при логине (собственный класс,
  `io.jsonwebtoken`), `JwtAuthUtils` — статические хелперы для чтения
  claims из уже провалидированного Spring Security `Jwt` в
  контроллерах/сервисах. Имена вводят в заблуждение, но кода это не
  дублирует.
- **`JWT_EXPIRATION` — мёртвый параметр.** Прокидывается через CI
  (`--set backend.env.JWT_EXPIRATION="86400000"`) → `values.yaml` →
  Deployment env — но `JwtUtils.java:26` хардкодит `expiration =
  86_400_000L` как Java-константу и никогда не читает `${...}` для этого
  значения. Смена `JWT_EXPIRATION` в CI/values **ничего не изменит** в
  реальном поведении токена. Аналогичный класс проблемы — мёртвый
  `--set flannel.backend=host-gw` (см. §4.2) — деплой-конфиг разошёлся с
  кодом в двух независимых местах.
- **Хеширование пароля** — сейчас корректно (единственный владелец —
  `UserService.createUser()`), но за 7 месяцев дважды чуть не ломалось в
  обе стороны (дублирование → одинарное; при этой сессии чуть не вернули
  дублирование обратно). Подробности и предостережение — уже в
  `docs/context/KNOWN_ISSUES.md`, не повторяю здесь целиком.
- **Money/decimal — чисто**, см. §2.2.
- **`ActivityController.getAll()` не принимает вообще никаких
  query-параметров** — ни `type`, ни `ageGroup`, ни `day`. Прямое
  несоответствие фронтенду, разбор в §3.5.
- **`UserController.deleteUser` — soft delete**, подтверждено
  (`UserService.deleteUser()` → `userRepository.deactivateUser(id)`), не
  хард-delete. Но **нет endpoint'а обратной активации** — подтверждено
  и самим комментарием разработчика в `admin-users.js`. Мелкий, но
  реальный API-пробел.

---

## 3. Frontend

### 3.1 Что это технически

Статический multi-page сайт: nginx отдаёт готовые HTML-файлы, никакого
build-шага, никакого фреймворка, никакого бандлера. `Dockerfile` фронтенда
— просто `COPY` папок `pages/styles/assets/js` в `nginx:alpine`.

### 3.2 Страницы (18 HTML, полный список)

```
Публичные:  home/index, about, contact, corporate, activities,
            performances, workshops, workshops/workshop-detail,
            login (login+register в одной форме), feedback,
            impressum/{impressum,agb,datenschutzerklaerung,widerruf}
Авторизован.: dashboard/dashboard, dashboard/profile, teacher/dashboard
Admin:      admin/{dashboard,users-edit,groups,activities,venues,
            performances-edit,workshops-edit}
Служебные:  partials/{header,footer} — не страницы, инклюды
```

### 3.3 `main.js` — общий утилитный слой (лучше, чем ожидалось)

[main.js](../../frontend/js/main.js) — не разрозненный набор страниц,
а осознанно общий модуль: `window.API_BASE_URL` (env-aware:
`localhost`/`127.0.0.1` → `:8080`, иначе `https://api.tlab29.com`),
`fetchJson()` (обёртка над `fetch`, обрабатывает 401 → `localStorage.clear()`
+ редирект на логин, парсит JSON, кидает осмысленные `Error`),
`escapeHtml()`, `formatLocalDate()`/`formatPrice()` (Intl API, де-локаль).
Экспортируется через `window.*` — ES-модулей нет нигде, все скрипты
классические глобальные `<script>`.

Токен: `localStorage.authToken` + `localStorage.tokenExpiry` (ms since
epoch) + `localStorage.userData` (id/email/role). `isAuthenticated()` —
чисто клиентская проверка `token && !expired`, не валидирует подпись
(и не должна — это UX-уровень, реальная защита на бэке).

### 3.4 Дублирование (конкретные примеры, не "в целом много копипасты")

| Что дублируется | Где именно | Должно жить в |
|---|---|---|
| `safeFetch`/`safeJson` (обёртка fetch + auth header) | [admin-dashboard.js:83-105](../../frontend/js/admin-dashboard.js#L83) — переизобретает то, что уже есть как `window.fetchJson`/`window.safeJson` в `main.js` | только `main.js` |
| `showNotification`/`createNotificationContainer` (toast) | Независимо в [login.js:185-208](../../frontend/js/login.js#L185) и [profile.js:180-212](../../frontend/js/profile.js#L180) — близкие, но не идентичные реализации одного и того же UI-паттерна | общий `ui-utils.js` или `main.js` |
| `debounce()` | Только в `admin-users.js` — не переиспользуется, хотя search/filter паттерн напрашивается и на других admin-страницах | общий utils |
| DTO-shape несогласованность | `admin-workshop.js` шлёт **плоские** `teacherId`/`venueId`; `admin-groups.js`/`admin-workshop.js` (group-часть) шлют **вложенные объекты** `{id: X}` для тех же по смыслу связей | нужна единая конвенция DTO на бэке + фронте |

### 3.5 Реальные интеграционные баги фронт↔бэк (подтверждено, не гипотеза)

- **Фильтры на странице Activities не работают.** `activities.js` строит
  `?type=&ageGroup=&day=` и шлёт на `/api/v1/activities` — но
  `ActivityController.getAll()` не принимает никаких параметров, всегда
  возвращает полный список (см. §2.5). UI фильтров существует и выглядит
  рабочим, реального эффекта не даёт.
- **`corporate.js`** делает `GET /activities?type=corporate` с
  собственным комментарием в коде "we try to fetch activities and filter
  if needed, or assume an endpoint exists" — по той же причине получит
  **все** активности целиком, не отфильтрованные под corporate-контекст,
  и отрендерит их все как "corporate programs".

### 3.6 Мультиязычность — скрытый актив, не используемый фронтендом

БД/entity-модель уже несёт `titleDe/titleEn/titleUa` (+`description*` у
Activity) — см. §2.2. Фронтенд: **весь текст жёстко на немецком в HTML**,
нет ни переключателя языка, ни чтения `Accept-Language`, ни малейшей
i18n-инфраструктуры. Admin-формы создания Activity/Group физически имеют
только один текстовый инпут — и **фейково заполняют** `titleUa`/
`descriptionUa`/`descriptionDe` тем же значением, что и EN
([admin-activities.js:42-45](../../frontend/js/admin-activities.js#L42),
буквально с комментарием `// Fallback`). То есть мультиязычность в БД
существует, но никем не наполняется осмысленно и нигде не читается —
это чистый задел на будущее, не работающая фича. Важно для роадмапа
(круглый стол, п.3 — "мульти language фичи") — фундамент уже частично
заложен, но требует и бэкенд-доработки (реальные API для выбора языка), и
фронтенд с нуля.

### 3.7 XSS-гигиена — непоследовательная

`window.escapeHtml()` используется корректно и последовательно в
`workshops.js`, `workshop-detail.js`, `dashboard.js`, `admin-*.js` (кроме
одного случая) — интерполируемые в `innerHTML` значения из API (title,
description, venue name) экранируются. **Исключение:** `activities.js`
рендерит `activity.name`/`activity.description` в `innerHTML`
([activities.js:44-64](../../frontend/js/activities.js#L44)) **без**
`escapeHtml`. Практический риск ограничен, т.к. эти поля сейчас
заполняются только через admin-формы (не произвольным пользовательским
вводом) — но это несогласованность практики, не осознанное решение
где-то экранировать, а где-то нет.

### 3.8 Admin-гейтинг — корректно спроектирован как чисто косметический

Каждая admin-страница делает `window.isAuthenticated()` + проверку роли
на клиенте **исключительно для UX** (не показывать форму человеку без
прав, сразу редиректить) — реальное разграничение прав везде идёт через
`@PreAuthorize` на бэкенде (см. §2.3), кроме уже отмеченного пробела в
`GroupController`. Это правильный паттерн, а не дыра — фиксирую явно,
чтобы круглый стол не тратил время на перепроверку очевидного.

### 3.9 Гипотеза круглого стола: замена на Svelte

Заказчик прямо обозначил Svelte как одну из опций замены (не
единственную, не решённую). На основе увиденного: текущий фронтенд НЕ
технический долг в смысле "плохо написан" — код местами дисциплинирован
(`main.js`), но архитектурно упирается в потолок MPA-без-фреймворка:
нет переиспользуемых компонентов (кроме header/footer через
самодельный `partials-loader.js`), нет реактивного состояния, нет типов,
18 файлов с частично дублирующейся логикой. Оценка фреймворка (Svelte
или альтернативы) — предмет отдельного решения круглого стола, не
предрешаю здесь.

---

## 4. CI/CD и инфраструктура

### 4.1 Пайплайн (`.gitlab-ci.yml`, 4 стадии)

```
build   → gradle:8.5-jdk21, ./gradlew bootJar (тесты НЕ запускаются)
package → docker build+push backend/frontend → registry.gitlab.com
deploy  → helm upgrade --install lr-dev ./devops/helm/lr-app -n lr-dev
smoke   → curl /actuator/health + curl главная страница
```

Единственное окружение — `lr-dev` namespace, нет staging/prod разделения
(осознанно на сегодня — единственный пользователь, Olena).

### 4.2 "Мёртвые" параметры деплоя (паттерн, не разовая случайность)

Второй подтверждённый случай (первый — `JWT_EXPIRATION`, §2.5):
`deploy-dev` job передаёт `--set flannel.backend=host-gw` при **каждом**
деплое. `values.yaml` не содержит ключа `flannel` вообще, ни один
активный Helm-шаблон его не читает (`flannel-patch.yaml`, который раньше
это делал и вызвал инцидент LR-BUG-01, вынесен в `drafts/` 2026-06-21).
Сейчас безвреден — но это заряженный параметр: вернись `flannel-patch.yaml`
в `templates/` — инцидент повторится мгновенно на первом же деплое.
Зафиксировано как тикет LR-002 (создан ранее в этой сессии), не чинил
сам в рамках инвентаризации — это код-фикс отдельного тикета.

### 4.3 Helm-манифесты — конкретные наблюдения

| Deployment/StatefulSet | Replicas | Liveness/Readiness | Resource limits | Секреты |
|---|---|---|---|---|
| `lr-backend` | 1 (настраиваемо) | ✅ `/actuator/health`, оба | ❌ нет | DB — `secretKeyRef` ✅; **JWT_SECRET — plaintext value в манифесте** ❌ |
| `lr-frontend` | 1 | ❌ нет вообще | ❌ нет | — |
| `lr-postgres` | 1 (StatefulSet) | ✅ `pg_isready` | ❌ нет | `secretKeyRef` ✅ |

**Находка: `JWT_SECRET` передаётся как открытый `value:` в
`backend-deployment.yaml`**
([backend-deployment.yaml:63-64](../../devops/helm/lr-app/templates/backend-deployment.yaml#L63)),
а не через `secretKeyRef`, хотя ровно тот же файл использует
`secretKeyRef` для DB credentials строчкой ниже — несогласованная
практика внутри одного манифеста. JWT_SECRET виден любому с доступом на
чтение `kubectl get deployment -o yaml` в namespace, и как минимум
проходит через CI variables/логи `--set`. Для сравнения — DB-пароль
защищён правильно, а JWT-секрет (которым можно подделать токен **любой**
роли, включая ADMIN) — нет. Явная асимметрия, стоит одного из
"инфра-маст-хэв" пунктов круглого стола.

**Отсутствует полностью:** `resources.requests/limits` — нигде ни на
одном контейнере (backend/frontend/postgres); `NetworkPolicy` — ни одной
(namespace `lr-dev` полностью открыт для pod-to-pod трафика от любого
другого namespace в кластере — общем с другими проектами домашнего
сервера); `revisionHistoryLimit` — не задан (DEBT-4, ReplicaSet копятся
бесконечно); PodDisruptionBudget — нет ни у одного компонента.

### 4.4 Бэкапы — подтверждено полное отсутствие

`lr-postgres` — StatefulSet, `local-path` PVC, 1 реплика, **ни одного**
упоминания `pg_dump`/`pgBackRest`/CronJob/restic ни в Helm-чарте, ни в
CI, ни где-либо ещё в `devops/`. Полный ноль, как и было
задокументировано в тикете LR-003 (создан ранее в этой сессии).

### 4.5 Docker-образы

- **Backend** ([backend/Dockerfile](../../backend/Dockerfile)):
  `eclipse-temurin:21-jre-alpine` (JRE, не JDK — правильно, минимальный
  размер), просто копирует готовый jar из CI build-стадии. Нет `USER`
  директивы — контейнер работает от root. Мелкое упущение по
  container-hardening, не критично для namespace без враждебных соседей,
  но стоит одной строчки на бэклог.
- **Frontend** ([frontend/Dockerfile](../../frontend/Dockerfile)):
  `nginx:alpine`, копирует статику как есть. Никакого кастомного
  `nginx.conf` — значит нет настроенного gzip/кеш-заголовков/security
  headers сверх дефолтов образа. При переходе на бандлер (Svelte и т.п.)
  этот Dockerfile придётся переписывать полностью — сейчас в нём
  физически нет build-стадии (см. тикет LR-002).

### 4.6 cert-manager / TLS

`devops/platform/cert-manager/` — стандартный upstream cert-manager
(CRDs + controller) + один `ClusterIssuer`
([cloudflare-clusterissuer.yaml](../../devops/platform/cert-manager/issuers/cloudflare-clusterissuer.yaml)):
ACME через Let's Encrypt prod-эндпоинт, DNS-01 challenge через
Cloudflare API token (`cloudflare-api-token-secret`, namespace
`cert-manager`). Один `tls-secret` покрывает оба хоста
(`tlab29.com`+`api.tlab29.com`) — согласуется с `ingress.yaml`. Настроено
корректно, без замечаний.

### 4.7 Локальный dev (`compose.yaml`)

Мелкая находка: `JWT_SECRET: ${JWT_SECRET}` прописан в блоке `postgres:`
([compose.yaml:24](../../backend/compose.yaml#L24)), а не `backend:` — Postgres
эту переменную никак не использует, это явно copy-paste-мусор. Backend,
скорее всего, всё равно получает `JWT_SECRET` через `env_file: .env`
(если он там определён) независимо от этой строки — не блокирует
работу, но стоит одной правки для чистоты.

---

## 5. Соответствие гексагональной архитектуре — детальный разбор

### 5.1 Что СООТВЕТСТВУЕТ задумке

- **Направление зависимостей строго внутрь.** `controller/` не делает
  SQL напрямую нигде (проверено по всем 20 контроллерам). `service/` не
  импортирует `HttpServletRequest`/`ResponseEntity` — единственное
  исключение по букве, не по духу: `AuthController` сам собирает
  `HttpServletRequest` только для логирования Origin-заголовка, бизнес-
  логику не трогает.
- **DTO/mapper слой реально изолирует HTTP-контракт от entity.**
  Ни один контроллер не возвращает entity напрямую — везде explicit
  `*ResponseDTO` через `*Mapper`. Это уже само по себе половина работы
  "порта" наружу.
- **Repository — уже интерфейсы** (Spring Data JPA) — `service/`
  зависит от абстракции `XxxRepository`, не от конкретной реализации.
  Формально это соответствует "порту" в терминах гексагона, пусть и
  сам интерфейс сгенерирован фреймворком, а не написан вручную под ACL.
- **Деньги через `BigDecimal` везде** — дисциплина уровня "домен
  защищён от типовых ошибок периферии" выдержана.

### 5.2 Что НЕ соответствует — и почему это принципиально, не косметика

**Главное нарушение: `domain/entity/` не является независимым доменом.**
Классы в этом пакете одновременно исполняют две роли — доменная модель
**и** JPA persistence-модель (`@Entity`, `@Table`, `@Column`,
`@OneToMany`, `@JoinColumn` — прямо на доменных полях). В строгой
гексагональной архитектуре домен не должен знать о существовании
Hibernate/JPA вообще — эти аннотации принадлежат adapter-слою
(persistence adapter), который должен маппить framework-agnostic доменные
объекты в/из JPA-модели. Здесь этого маппинга нет: entity = domain object
= JPA table row, три роли в одном классе. Практическое следствие: замена
Postgres на другую СУБД, смену ORM или переход части модели на
event-sourcing — потребует переписывать сами доменные классы, не только
"адаптер БД", что напрямую противоречит заявленному принципу "периферия
меняется, ядро не замечает".

Для сравнения — именно так спроектирован numi (`domain/` = zero
внешних зависимостей, ACL-пакеты типа `internal/einvoice` отдельно от
модели) — LR **не был построен по этому шаблону**, несмотря на
декларированное намерение.

**Второе нарушение — нет ни одного порта для внешних систем**, потому
что внешних систем пока нет (платежи, AI, Telegram/WhatsApp, Google Maps
— всё будущее, см. запрошенные п.5 круглого стола). Это не минус
реализации, это просто ещё не пройденный путь — но означает, что
гексагональность LR в части "периферия приходит и уходит" пока **не
проверена на практике вообще**, только продекларирована.

**Третье — `DatabaseFixConfig` — учебный пример утечки периферии в
ядро в чистом виде.** Инфраструктурная деталь (особенность
`ddl-auto=update`) лечится прямым SQL в `@PostConstruct`, а не через
адаптер персистентности или нормальную миграцию — это ровно тот anti-
pattern, которого гексагональная архитектура должна была не допустить
по конструкции.

**Четвёртое — DTO ближе к "зеркалу entity", чем к независимому публичному
контракту.** Структура request/response DTO во многих контроллерах почти
1:1 повторяет поля entity (см. несогласованность плоских/вложенных id в
group-payload, §3.4) — то есть периферия (фронтенд) уже неявно завязана
на форму текущей персистентной модели, не на стабильный независимый
контракт. Смена доменной модели с высокой вероятностью потребует менять
и фронтенд синхронно — обратное тому, что обещает гексагон.

### 5.3 Итоговая оценка

**Вердикт:** LR — это качественно построенный **слоистый Spring MVC
монолит** (Controller-Service-Repository-Entity, с корректным DTO/mapper
разделением) с **декларированным, но не реализованным** стремлением к
гексагональной архитектуре. Слои реальны и соблюдены; порты и адаптеры
для домена — нет. Это near-hexagonal skeleton, где основная
недостающая часть — отделение доменной модели от JPA persistence-модели.
Это не "переписать всё" — фундамент (сервисы, DTO, контроллеры) можно
сохранить; ядро работы гексагонализации — ввести framework-agnostic
доменные объекты и persistence-адаптер между ними и JPA-entity. Это один
из главных пунктов, который стоит вынести на решение круглого стола.

---

## 6. Открытые вопросы — требуют ответа ДО или ВО ВРЕМЯ круглого стола

### 6.1 [ОТКРЫТЫЙ ВОПРОС, приоритет — высокий] Cloudflare Worker vs задокументированный Tunnel

`devops/cloud_flare/cf_worker_lr.js` — Cloudflare Worker, который **не**
реализует классическую Tunnel-модель, задокументированную и в
`devops/cloud_flare/README.md`, и в `docs/infra/INFRA-LR.md`. Вместо
outbound-only `cloudflared` → `localhost:8000` (Traefik), Worker
**проксирует HTTPS-запросы напрямую на публичный IP `5.147.111.121:8443`**
(с ручной подменой `Host`-заголовка под нужный домен для роутинга на
Traefik по имени хоста).

Это прямо противоречит заявленной в `README.md`/`INFRA-LR.md` модели
безопасности ("No open ports required on router", "Zero-trust", "тоннель
— единственный путь наружу"): если Worker реально используется как
активный путь трафика, то порт `8443` на `5.147.111.121` обязан быть
доступен снаружи (иначе `fetch()` внутри Worker просто не достучится) —
то есть либо это открытый порт на роутере/сервере в обход
задокументированной схемы, либо какой-то другой canal (напр. тот же
`cloudflared`, но настроенный иначе, чем в README).

**Не выяснял и не гадал дальше** — нужен прямой ответ заказчика:
1. Этот Worker сейчас реально задеплоен и активен на Cloudflare, или
   это заброшенный эксперимент/черновик?
2. Если активен — точно ли порт 8443 открыт наружу на этот IP, и это
   осознанное решение (тогда `README.md`/`INFRA-LR.md` нужно
   переписывать, они врут о модели безопасности) — или это забытая
   дыра, которую надо закрыть?
3. Если это черновик/устаревшее — переносим в `docs/decision-history/`
   вместо `devops/cloud_flare/`, чтобы не путать следующую сессию.

Не блокирует круглый стол по архитектуре приложения, но **блокирует
любую infra-работу**, пока не выяснено — нельзя проектировать
инфра-обновления (п.4 из списка результатов круглого стола) поверх
топологии, которая, возможно, не соответствует реальности.

### 6.2 [ОТКРЫТЫЙ ВОПРОС, средний приоритет] CORS двойная конфигурация

См. §2.5 — `WebMvcConfig` и `SecurityConfig` независимо конфигурируют
CORS. Не проверено рантаймом, реален ли конфликт или один механизм молча
не участвует. Пять минут с реальным preflight-запросом закроют вопрос
однозначно — не стал утверждать по одному чтению кода.

### 6.3 [ОТКРЫТЫЙ ВОПРОС, низкий приоритет] `GroupController` без `@PreAuthorize`

См. §2.3 — любой аутентифицированный пользователь (не только
admin/business_owner/teacher) технически может писать в `/groups/**`.
Осознанное решение или пропуск? Если пропуск — отдельный HIGH-tier тикет
до круглого стола, не после (это дыра в правах доступа, не архитектурная
дискуссия).

---

## 7. Что дальше (не делается в этом документе, только фиксирую как ожидаемый результат круглого стола)

Этот документ — входные данные. Круглый стол на его основе должен дать:
1. Обновлённую архитектуру приложения (в первую очередь — решение по §5.2,
   вводим ли domain/persistence separation).
2. Роадмап перехода (миграции — Flyway/Liquibase, замена фронтенда,
   доработка i18n-фундамента из §3.6, must-have интеграции).
3. Новую фронтенд-архитектуру и стек (Svelte или альтернатива — §3.9),
   мультиязычность, UI/UX best practices.
4. Инфра-апдейты и must-have (бэкапы §4.4, секреты §4.3, resource
   limits, NetworkPolicy — с учётом ответа на §6.1 сначала).
5. Интеграции (платежи, AI, Telegram/WhatsApp, Google Maps) — с нуля,
   ничего из этого сейчас не существует в коде.
6. Анкету-ТЗ для Olena — по продуктовой части, отдельно от этого
   технического документа.
