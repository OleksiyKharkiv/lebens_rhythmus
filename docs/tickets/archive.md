# archive.md — Lebens Rhythmus closed tickets

> Закрытые тикеты переносятся сюда из `tickets.md` (per workflow §8 в
> корневом `CLAUDE.md`).

---

## LR-024 — Teacher IDOR: доступ к контактам детей из чужих групп

**Tier:** HIGH · **Статус:** Closed 2026-08-07 · `architect-reviewer` approve
**Источник:** LR-022, фаза 1+3 (найдено независимо backend- и
frontend-агентами — `docs/security/audit-2026-08-06.md`, H1)

`GET /api/v1/teacher/groups/{groupId}/participants`
(`EnrollmentController.java`) и парные `GET /workshops/teacher/
{teacherId}`, `GET /groups/teacher/{teacherId}` (последний — вообще без
`@PreAuthorize`) проверяли только роль `TEACHER`, не владение конкретной
группой/воркшопом.

**Сделано:** `User` и `Teacher` — разные сущности без FK, связаны только
по уникальному `email` — новый `TeacherRepository.findByEmail()` +
`TeacherService.resolveTeacherIdForUser(userId)` резолвит "какой
Teacher-профиль у этого JWT-пользователя". Все три эндпоинта сравнивают
резолвнутый id с запрошенным ресурсом, 403 (`AccessDeniedException`) при
несовпадении или отсутствии профиля — ADMIN/BUSINESS_OWNER не
затронуты. `GroupController.getGroupsByTeacher` получил отсутствовавший
`@PreAuthorize`. Раньше эта связка резолвилась только на фронте
(`teacher/+page.svelte`, `t.email === user.email`) — UX-удобство, не
security-граница.

`architect-reviewer` подтвердил: NPE/500-путей нет,
`resolveTeacherIdForUser` корректно возвращает `Optional.empty()` и на
`userId == null`, и на отсутствие Teacher-строки — оба падают в 403, не
в исключение.

---

## LR-025 — `OrderController.getById()`: NPE на каждом вызове, проверка владения никогда не исполняется

**Tier:** HIGH · **Статус:** Closed 2026-08-07 · `architect-reviewer` approve
**Источник:** LR-022, фаза 1 (`docs/security/audit-2026-08-06.md`, H2)

Читал claim `"roles"` (JWT минтит только единственный `"role"`) —
`getClaimAsStringList` на отсутствующем claim'е тихо возвращал `null`,
следующий `.stream()` кидал NPE на каждом запросе, включая от админов.
Реальная проверка владения заказом никогда не исполнялась.

**Сделано:** `JwtAuthUtils.hasRole(jwt, "ADMIN")`/`"BUSINESS_OWNER"`
(уже корректно читает реальный claim). Новый реальный E2E-тест
`OrderOwnershipTest` (регистрация → логин → реальный JWT, не
mock-claim — см. предостережение LR-007) — владелец читает свой заказ
(200, не 500), посторонний получает 403 (не 500, не 200).
`architect-reviewer`: `getClaimAsStringList("roles")` больше нигде в
кодовой базе не встречается (grep).

---

## LR-026 — Account-lockout: content-based oracle существования аккаунта на `/auth/login`

**Tier:** MED · **Статус:** Closed 2026-08-07 · `architect-reviewer` approve
**Источник:** LR-022, фаза 1 (`docs/security/audit-2026-08-06.md`, H3)

`AuthService`'s лок-аут-проверка кидала голый `RuntimeException` → 500 с
телом, отличным от bad-credentials' 401 — тот же класс находки, что уже
закрытый LR-014, но на `/auth/login`, более ценном эндпоинте.

**Сделано:** та же `BadCredentialsException("Invalid credentials")`,
что и на пути неверного пароля — неотличимо по конструкции. Новый тест
`AuthServiceTest.authenticate_lockedAccount_throwsBadCredentials_
sameAsWrongPassword`.

---

## LR-029 — `GlobalExceptionHandler`: системная утечка `ex.getMessage()` + entity-existence oracle

**Tier:** MED · **Статус:** Closed 2026-08-07 (частично — см. follow-up
LR-032) · `architect-reviewer` approve
**Источник:** LR-022, фаза 1 (`docs/security/audit-2026-08-06.md`, M1)

Catch-all отдавал `ex.getMessage()` клиенту буквально для любого
необработанного исключения — в сочетании с 59 местами по 18 сервисам,
использующими идиом `orElseThrow(() -> new RuntimeException("X not
found with id: " + id))`, это был entity-existence oracle.

**Сделано (сознательно уже, чем полный текст тикета):** catch-all
теперь отдаёт фиксированное generic-сообщение, никогда не эхо
`ex.getMessage()` (полная информация по-прежнему логируется server-side
— не менялось). Это закрывает реальную утечку для всех 59+ мест сразу,
независимо от типа исключения. **Не сделано:** замена всех 59 мест на
типизированное not-found-исключение с единообразным 404 — заведён
отдельный `LR-032` (LOW, статус-код есть 500 вместо 404, но контента
больше не эхо). `architect-reviewer` подтвердил: такой скоуп разумен,
оставшийся 500-vs-404-vs-403 сигнал — материально меньшая по масштабу
утечка, чем исходная, корректно трекается отдельно, не тихо забыта.
Новый тест `GlobalExceptionHandlerTest`.

---

## LR-030 — `GroupController.createGroup`: mass assignment через raw-entity binding без allow-листа полей

**Tier:** LOW-MED · **Статус:** Closed 2026-08-07 · `architect-reviewer` approve
**Источник:** LR-022, фаза 1 (`docs/security/audit-2026-08-06.md`, M11)

`GroupService.save()` был голым `groupRepository.save(group)`, без
field allow-листа. "Needs deeper trace" из исходной находки (может ли
Jackson вообще инстанциировать `Group` через protected-конструктор) —
снято: очевидно да, поскольку создание групп уже работает в проде через
именно этот путь.

**Сделано:** новый `GroupCreateDTO` (явные поля, `xxxId: Long` — тот же
паттерн, что уже применён для `WorkshopCreateDTO`), `GroupService.
createGroup()` резолвит id через репозитории. `GroupController.
createGroup()` больше не биндит raw-entity. Фронтенд: новый
`GroupCreateRequestDTO` (плоские id) + `toCreateRequest()`-трансформация
в `admin/groups/+page.svelte` — `updateGroup`/PUT не тронут, по-прежнему
корректно защищён на уровне сервиса (`update()`'s ручное копирование
полей). `architect-reviewer` подтвердил: `GroupCreateDTO` структурно не
может нести `capacityLeft`/`enrollments` — невозможно по конструкции, не
просто "не замаплено". Новый `GroupServiceTest`.

---

## LR-021 — `PaymentRequestDTO`/`OrderRequestDTO`: нет ни одной аннотации валидации

**Tier:** MED · **Статус:** Closed 2026-08-07 · `architect-reviewer` approve
**Источник:** architect-reviewer, ревью LR-012/LR-020, 2026-08-06

**Сделано (сознательно уже полного текста тикета — см. его же
предостережение "не на автомате"):** `@NotNull @DecimalMin("0.01")` на
`amount` (обеих DTO), `@Pattern("[A-Z]{3}")` на `currency`, `@Size`
границы по реальным `@Column`-определениям (или неявному дефолту
Hibernate 255 символов, где явной длины нет). **Не сделано осознанно:**
`status` НЕ конвертирован в enum — `Payment.java`'s собственный
комментарий уже говорит "Kept as String for now — optionally switch to
enum", и grep по кодовой базе не нашёл ни одного места, где Order/
Payment.status реально устанавливается программно — придумывать enum
сейчас значило бы гадать продуктовое решение, не чинить баг.
`OrderRequestDTO.orderNumber` НЕ `@NotBlank`, несмотря на
`nullable=false` в сущности — DTO общий для create и update
(`OrderController`), а `OrderService.update()` `orderNumber` вообще не
читает; требовать его на каждом PUT было бы искусственным. DB-уровневый
`NOT NULL` по-прежнему ловит реально отсутствующее значение на create.
`architect-reviewer` подтвердил обе развилки (PaymentRequestDTO — только
create, без shared-update; OrderRequestDTO.update() не читает
orderNumber) через grep, независимо.

---

## LR-023 — `spring-boot-starter-data-rest`: каждый репозиторий торчал наружу в обход ВСЕХ `@PreAuthorize`, само-эскалация до ADMIN одним запросом

**Tier:** CRITICAL (переопределяет обычную HIGH-шкалу проекта — полный
обход авторизации + тривиальная эскалация привилегий + расшифрованный
дамп PII, включая bcrypt-хэши паролей)
**Статус:** Closed 2026-08-06 · исправлено немедленно по прямому указанию
заказчика, подтверждено живым эксплойтом до и после фикса, `architect-
reviewer` approve
**Источник:** фоновый агент DevSecOps-аудита (LR-022, фаза 1, backend)

**Находка:** `backend/build.gradle` подключал `spring-boot-starter-data-
rest` без единого механизма отключения авто-экспозиции
(`@RepositoryRestResource(exported=false)`, `RepositoryRestConfigurer`,
`spring.data.rest.detection-strategy` — ни одного нет нигде в коде).
Spring Data REST по умолчанию экспортирует **каждый** `JpaRepository`
(User, Participant, Payment, Order, ...) как полноценный HAL CRUD REST-
ресурс в корне приложения, защищённый только общим fallback'ом
`SecurityConfig`'s `.anyRequest().authenticated()` — без единой проверки
роли. Это делает бессмысленными все `@PreAuthorize` во всех
хендрайтен-контроллерах, потому что параллельный авто-сгенерированный
путь их не проходит вообще.

**Подтверждено вживую, не только статическим анализом** — поднял бэкенд
локально против чистой Postgres, зарегистрировал обычного `USER` через
реальный `/auth/register` + `/auth/login`:
- `GET /users` → **200**, полный дамп таблицы: bcrypt-хэши паролей,
  токены верификации, расшифрованные ФИО (JPA-конвертер расшифровывает
  прозрачно).
- `PATCH /users/1 {"role":"ADMIN"}` → **200**, и в БД роль реально
  сменилась на `ADMIN`. Свежезарегистрированный обычный пользователь
  сделал себя администратором одним HTTP-запросом.

**Фикс:**
- `backend/build.gradle` — зависимость `spring-boot-starter-data-rest`
  **удалена полностью** (не переконфигурирована) — grep по всему коду
  подтвердил, что `@RepositoryRestResource`/`RepositoryRestConfigurer`
  нигде не используются, это была мёртвая, никогда не нужная зависимость.
  Проверено, что фронтендовский `authRequest('/users')` резолвится в
  `/api/v1/users` (настоящий, защищённый `UserController`), не в
  уязвимый `/users` — удаление ничего не сломало.
- `GlobalExceptionHandler.java` — добавлен `@ExceptionHandler
  (NoResourceFoundException.class)` → честный 404. Понадобилось, потому
  что после удаления зависимости `GET /users` стал бросать
  `NoResourceFoundException`, которую старый catch-all глотал и превращал
  в вводящий в заблуждение 500 (и заодно утекал `ex.getMessage()`).
  Точечный фикс только этого типа исключения — более широкая проблема
  catch-all'а с `ex.getMessage()` не тронута, отдельная находка аудита.
- Новый `SpringDataRestExposureTest` (реальный E2E: `@SpringBootTest`
  + `TestRestTemplate` + Testcontainers Postgres, полный цикл
  регистрация→логин→запрос, не mock) — 4 теста: `GET /users`/
  `/participants` и `PATCH /users/{id}` для обычного `USER` **и** для
  `ADMIN` (добавлено по замечанию `architect-reviewer`) — все должны
  быть 404, не 401/403 (эндпоинта не должно существовать вообще, ни для
  какой роли), плюс прямая проверка через `userRepository`, что роль не
  изменилась.

**`architect-reviewer`:** approve, must-fix не найдено. Ревьюер сам
пересобрал и прогнал `SpringDataRestExposureTest` против реальной
Testcontainers Postgres, не поверил на слово — зелёный. Подтвердил:
удаление зависимости полностью закрывает путь (`@ConditionalOnClass`
гейтит автоконфигурацию Spring Data REST на классы, которых больше нет
на classpath — никакой property не может её частично воскресить), нет
побочных эффектов на `GlobalExceptionHandler`-фикс (бэкенд не отдаёт
статику вообще, это отдельно nginx).

**Follow-up, не блокирует:** дешёвый CI-guard (grep/dependency-assertion)
чтобы `spring-boot-starter-data-rest` не вернулся незаметно — не
заведено отдельным тикетом, можно добавить в LR-002 (CI/CD) при
следующем touch.

---

## LR-020 — Bean Validation не работал НИГДЕ в бэкенде: провайдер отсутствовал на classpath

**Tier:** HIGH (re-tier с изначальной оценки — затрагивает auth, DSGVO-
согласие на регистрации, и de-facto всю границу `@Valid` по всему
приложению; найдено architect-reviewer как переоценка тира при ревью
LR-012, согласовано)
**Статус:** Closed 2026-08-06 · исправлено, проверено полным сьютом,
architect-reviewer approve
**Источник:** обнаружено случайно при реализации LR-012 — новый юнит-тест
упал не с "невалидные данные не отклонены", а с
`jakarta.validation.NoProviderFoundException`

**Находка:** `backend/build.gradle` объявлял только
`jakarta.validation:jakarta.validation-api:3.1.1` (интерфейсы/аннотации)
— никогда `spring-boot-starter-validation` или любой другой
`jakarta.validation.spi.ValidationProvider` (Hibernate Validator и т.п.).
Значит **все** `@Valid @RequestBody` по всему бэкенду молча не
исполнялись в проде: Spring MVC ищет validator-бин для аргумента, не
находит провайдера, `OptionalValidatorFactoryBean` ловит
`NoProviderFoundException` внутри себя (DEBUG-лог, не исключение),
`targetValidator` остаётся `null`, `validate()` на `null` — no-op.
Ни ошибки при старте, ни исключения на запрос — запрос просто проходит
как будто валиден. Подтверждено `architect-reviewer` трассировкой
реального механизма Spring, не с моих слов.

**Что реально было мертво:**
- `@Email` на регистрации — формат мэйла не проверялся.
- `@Size(min=8)` на пароле (`UserRegistrationDTO`, `UserPasswordUpdateDTO`)
  — теоретически можно было зарегистрироваться с паролём в 1 символ.
- **`@AssertTrue` на `acceptedTerms`/`privacyPolicyAccepted`** —
  единственная точка проверки согласия с условиями/политикой
  конфиденциальности при регистрации была декоративной. `UserMapper`
  прокидывает булево значение как есть, сервисный слой его повторно не
  перепроверяет — то есть регистрация с `acceptedTerms: false` технически
  проходила. Самая серьёзная часть находки — DSGVO-adjacent.
- Все аннотации LR-012 (см. ниже) до фикса тоже были декоративны.
- **Шире, чем DTO**: `spring.jpa.properties.hibernate.validation.mode`
  не переопределён (дефолт `AUTO`) — значит entity-level
  bean-валидация (Hibernate's JPA event listener на persist/flush) тоже
  была тем же багом отключена и теперь тоже ожила: `Order.orderNumber`
  (`@Size(max=100)`), `Workshop.workshopName` (`@NotBlank @Size(max=200)`),
  `User.firstName`/`lastName` (`@Size(min=2,max=50)`) — это единственные
  три сущности с bean-валидацией на уровне entity (проверено grep'ом по
  `domain/entity/`). Границы щедрые, реальным данным Olena не должны
  мешать, но если после деплоя где-то вылезет `ConstraintViolationException`
  на flush существующей записи — это первое место искать.

**Фикс:** `backend/build.gradle` — `jakarta.validation-api` заменён на
`implementation 'org.springframework.boot:spring-boot-starter-validation'`
(тянет Hibernate Validator 8.0.3.Final + API транзитивно, версия API
опустилась с зафиксированной 3.1.1 до управляемой Spring Boot BOM 3.0.2 —
`architect-reviewer` подтвердил: стандартная поддерживаемая пара для
Spring Boot 3.5.7, конфликтов версии нигде в кодовой базе нет).

**Проверено:** `./gradlew clean test` — полный сьют, 52 теста, 0 упавших,
включая context-loading/Testcontainers-интеграционные тесты — ни один
существующий тест/код-путь не зависел от того, что невалидные данные
молча проходили. `architect-reviewer` — approve with changes
(must-fix'ов в самом коде не нашёл, только документационные: зафиксировать
HIGH-тир и упомянуть entity-level эффект — оба сделаны здесь).

**Follow-up, не блокирует:** `PaymentRequestDTO`/`OrderRequestDTO` вообще
без единой аннотации валидации — заведён отдельный LR-021 (MED, платёжные
поля заслуживают отдельного внимательного прохода, не "по аналогии").

---

## LR-012 — Добавить валидацию длины на `address`/`phone`/`city`/`zipCode`

**Tier:** LOW (валидация, без риска для существующих данных — сам фикс
провайдера валидации, найденный по ходу, документирован отдельно как
LR-020, HIGH)
**Статус:** Closed 2026-08-06
**Источник:** architect-reviewer, ревью фикса LR-011 (VARCHAR→TEXT), 2026-08-03

`V2__widen_encrypted_pii_columns.sql` расширил
`users.{first_name,last_name,phone,address,city,zip_code}` и аналогичные
колонки `teachers`/`participants` до `TEXT` (нужно было, чтобы починить
риск переполнения после шифрования — см. LR-011). Побочный эффект:
`VARCHAR(255)` был единственным ограничением длины для `address`/`phone`/
`city`/`zipCode` — не ограничены вообще ничем, ни в БД, ни в DTO/entity.

**Сделано:**
- `UserRegistrationDTO`/`UserUpdateDTO`: `address` (max 255), `city`
  (max 100), `zipCode` (max 20), `phone` (`@Size(max=25)` рядом с уже
  существовавшим `@Pattern`).
- Попутно найдено и исправлено то же самое, но шире: `TeacherRequestDTO`/
  `ParticipantRequestDTO` не имели **вообще ни одной** аннотации
  валидации (тикет ошибочно предполагал, что firstName/lastName там уже
  защищены, как у `User` — на самом деле нет). Добавлены
  `@Size(min=2,max=50)` на firstName/lastName (та же конвенция, что у
  `User`), `@Email`, `@Pattern`+`@Size(max=25)` на phone.
- Новый тест `RequestDtoValidationTest` (прямая Bean Validation, без
  Spring-контекста) — 6 тестов, покрывают и лимиты LR-012, и попутно
  найденный Teacher/Participant пробел.

**Важно:** сами аннотации были добавлены до того, как обнаружилось, что
провайдер валидации отсутствовал вообще (см. LR-020) — новый тест сразу
упал с `NoProviderFoundException`, что и вскрыло более серьёзную
находку. После фикса LR-020 все тесты (включая эти шесть) зелёные.

---

## LR-014 — `resendVerification`: timing side-channel палит существование аккаунта

**Tier:** LOW (низкоценная цель для атаки, не платёжные/детские данные)
**Статус:** Closed 2026-08-06 · исправлено и покрыто тестом
**Источник:** architect-reviewer, ревью email-верификации, 2026-08-04

`POST /auth/resend-verification` всегда возвращал 200 без тела независимо
от исхода — специально, чтобы не палить, существует ли аккаунт/подтверждён
ли он. Но реальное **время ответа** отличалось: для неизвестного/уже-
подтверждённого email — почти мгновенно (один `findByEmail` без побочных
действий); для реального неподтверждённого аккаунта — генерация токена +
запись в БД + **синхронный** SMTP-коннект к `smtp-relay.brevo.com:587`.
Измеримый timing-канал, различающий "этот email существует и не
подтверждён" от остальных случаев.

**Сделано:** искусственная минимальная задержка ответа (не асинхронная
отправка — проще, без нового отказоустойчивого поведения/очереди).
`AuthService.resendVerification()` меряет прошедшее время вокруг всей
ветки (включая быстрый no-op путь) и, если оно меньше
`app.email-verification.resend-min-response-ms` (дефолт 400мс,
`EMAIL_VERIFICATION_RESEND_MIN_MS`), досыпает разницу — обе ветки теперь
неотличимы по скорости ответа. `AuthService` переведён с
`@RequiredArgsConstructor` на явный конструктор ради `@Value`-инъекции
(тот же паттерн, что уже был в `EmailVerificationService`).

**Тест:** `AuthServiceTest.resendVerification_unknownEmail_
stillTakesAtLeastMinResponseTime` — конструктор в тестах получает малый
порог (50мс), чтобы не замедлять сьют и не быть флеки; проверяет, что
быстрая ветка реально не возвращается раньше порога.

`./gradlew test` — полный сьют зелёный.

---

## LR-019 — Светлая/тёмная тема + переключатель (реализовано вне очереди)

**Tier:** LOW (чистый фронтенд/CSS, без domain/service-слоя)
**Статус:** Closed 2026-08-06 · реализовано и проверено вживую в браузере
в тот же день по прямому запросу заказчика
**Источник:** прямой запрос заказчика, 2026-08-06

**Важное примечание:** `layout.css` содержал явный комментарий "Single
signature look, no light/dark toggle — a deliberate brand choice, not an
omission" (со ссылкой на LR-ADR-014). Сама ADR-014 такого пункта не
формулирует буквально — это была редакторская интерпретация на момент
реализации дизайна, не отдельно подтверждённое заказчиком решение. Запрос
заказчика 2026-08-06 явно её отменяет для этого конкретного пункта —
комментарий в коде обновлён, ADR не переписывается (сама ADR не
противоречит).

**Сделано:** CSS-переменные (`--color-ink`/`--color-paper`/`--color-gold`/
`--color-teal`/`--color-error` и т.д.) переопределены под
`:root[data-theme="light"]`, каждый компонент по всему приложению уже и
так использовал только Tailwind-классы токенов (`bg-ink`, `text-paper`,
`text-gold`...), не хардкод — переключение темы не потребовало ни одной
правки в компонентах. Тумблер (солнце/луна) в header — десктоп и мобилка,
рядом с переключателем языка. localStorage + inline-скрипт в `app.html`
(до гидратации) — без "мигания" неправильной темой при загрузке. Дефолт —
тёмная тема (текущий, уже одобренный Olena вид) независимо от
`prefers-color-scheme` системы — сознательно, чтобы не менять дефолтное
первое впечатление без явного действия посетителя; светлая — только по
явному клику, запоминается.

**Проверено вживую в браузере** (dev-сервер, desktop 1280×720 + mobile
375×812): дефолтная тёмная тема рендерится верно (`#2b1b29`/`#f7eedd`),
клик по тумблеру переключает в светлую (`#faf3e6`/`#2b1b29`) на обоих
брейкпоинтах, `localStorage`/`data-theme` персистятся и переживают полный
reload без мигания неправильной темой, aria-label кнопки корректно
отражает текущее состояние ("Helles Design aktivieren" / "Dunkles Design
aktivieren"). `npm run check` — 0 ошибок.

---

## LR-015 — Admin/Owner-дашборд: управление воркшопами (venue/зал/возрастные группы) + ключевые метрики

**Tier:** HIGH (схемная миграция, пересекает ARCH-границу — переносит `venue`
с `Workshop` на `Group`, затрагивает существующие данные)
**Статус:** Closed 2026-08-05 · согласован заказчиком (Круглый стол #3),
реализован и проверен в рамках MVP-скоупа
**Источник:** Круглый стол #2 (метрики) + Круглый стол #3 (критерии),
2026-08-05

Дашборд == функционал, который реально отличает LR от сайта-одностраничника
(в отличие от статических страниц Impressum/Activities/etc, уже
построенных этой сессией). Полный скоуп ниже — решения приняты заказчиком
по каждому пункту, роль-модель под MVP явно сужена.

**Системный контекст (важно для реализации):** Olena сегодня == owner ==
единственный автор курсов/воркшопов. Отдельные преподаватели (роль
`TEACHER`) и явная роль `BUSINESS_OWNER` — фича на будущее (рост бизнеса),
**архитектурно предусмотреть, чтобы не переписывать половину кода
позже, но не строить отдельный UI/эндпоинты под них сейчас.** Для MVP
весь функционал ниже — под ролью `ADMIN` (`@PreAuthorize` уже так и стоит
в `WorkshopController` — `hasRole('ADMIN') or hasRole('BUSINESS_OWNER')`,
менять не нужно, `TEACHER` туда не добавляем).

### Б. Управление воркшопами (все 5 пунктов — под ADMIN, не TEACHER)

1. Создание воркшопа — уже работает для ADMIN, без изменений.
2. Редактирование — уже работает для ADMIN. **Авторство (`created_by`) не
   делаем** — заказчик подтвердил, не нужно для MVP.
3. Запрет удаления для TEACHER — уже фактически верно (TEACHER не имеет
   вообще никаких прав на запись воркшопов сейчас) — переоценить при
   будущем включении роли TEACHER, не трогать сейчас.
4. ✅ **Готово (2026-08-05)** — Возрастные группы на воркшоп: `Group.
   ageGroup` уже была в схеме, добавлен UI-флоу создания/выбора —
   `admin/age-groups` (новая CRUD-страница, зеркало `admin/venues`) +
   select в форме `admin/groups`. `GroupDTO.ageGroupName` composed
   server-side ("titleDe (min–max)"). **Дефолтную "все возрасты" не
   заводили** (заказчик подтвердил).
5. ✅ **Готово (2026-08-05)** — Расписание с местом + залом:
   - `venue` перенесён с `Workshop` на `Group` — `V4__venue_to_group_
     level_plus_room.sql`, данные существующих воркшопов скопированы на
     их группы перед дропом колонки, не потеряны.
   - `Venue` получил `room` (String) — несколько залов на одном адресе
     как отдельные строки `venues`, не блокируют друг друга.
   - Автоматическая проверка конфликтов — по-прежнему НЕ строим, см.
     LR-016 (low priority, без изменений).
   - `architect-reviewer` пройден (2026-08-05): approve as-is. Два
     found-and-fixed реальных бага по ходу: `GroupMapper.toDto()`
     (реальный эндпоинт `/api/v1/groups`, не `WorkshopMapper`) не отдавал
     `venueId`/`venueName` вообще; `GroupService.update()` не копировал
     `venue` на существующую сущность (тихий no-op при редактировании) —
     оба исправлены и покрыты повторным прогоном `./gradlew test` (зелёный).
   - Follow-up, не блокирует: `workshop_groups.venue_id` FK без `ON
     DELETE` (тот же пробел уже был у старой `workshops.venue_id`,
     не регрессия этим дифом) — отдельный тикет, если удаление venue
     из админки станет реальным сценарием.

### Метрики (М1-М6, объединено с исходной анкетой)

| # | Метрика | Статус решения |
|---|---|---|
| М1 | Заполненность курса/воркшопа/занятия | ✅ **Готово (2026-08-05)** — `GET /api/v1/admin/metrics`, только `PENDING`+`CONFIRMED` (не `CANCELLED`) |
| М2 | Выручка за период | ⚠️ MVP-прокси: цена × число подтверждённых регистраций (не реальные платежи — см. LR-017 про механизм подтверждения) — не в этом дифе |
| М3 | Заполняемость ближайших групп (X из Y) | ⚠️ отложено вместе с М2 — "подтверждённое место" зависит от механизма LR-017, не по факту регистрации — не в этом дифе |
| М4 | Новые регистрации пользователей (тренд) | ✅ **Готово (2026-08-05)** — 30-дневный ряд, только `Role.USER` |
| М5 | Воркшопы, требующие внимания (алерты) | ✅ **Готово (2026-08-05)** — пороги ниже подтверждены заказчиком как финальные |
| М6 | Повторные клиенты / retention | ✅ **Готово (2026-08-05)** — ≥2 разных воркшопа с `CONFIRMED`, только `Role.USER` (найдено `architect-reviewer`: изначально считало и TEACHER/ADMIN тестовые записи — исправлено) |

**М5 — таблица порогов** (Круглый стол #3, **финально подтверждено заказчиком
2026-08-05**, реализовано как есть):

| Дней до старта | Порог заполненности | Уровень |
|---|---|---|
| 7 | < 30% | инфо |
| 5 | < 50% | предупреждение |
| 3 | < 70% | срочно |
| 1 | < 90% | критично |

`architect-reviewer` пройден дважды (venue/age-group часть отдельно,
метрики отдельно) — оба раза approve, один must-fix найден и исправлен
(M6 role-фильтр). LR-015 закрыт полностью в рамках этого MVP-скоупа;
M2/M3 остаются заблокированы на LR-017 (отдельный открытый тикет).

---

## LR-006 — `GroupController`: отсутствовал `@PreAuthorize` на write-методах (исправлено)

**Tier:** HIGH (auth boundary) · **Статус:** Closed 2026-07-23
**Источник:** обнаружено при исследовании backend перед началом таска
"teacher-дашборд + admin-панели"

`POST/PUT/DELETE /api/v1/groups/**` не имели вообще никакого `@PreAuthorize`
— в отличие от всех соседних контроллеров (`ActivityController`,
`WorkshopController`, `VenueController`, `PerformanceController`,
`TeacherController`), у которых на этих же методах стоит
`hasRole('ADMIN') or hasRole('BUSINESS_OWNER')` (иногда строже — `ADMIN`
для delete). `SecurityConfig`'s `.anyRequest().authenticated()` — fallback
блокировал полностью анонимные запросы, но **любой залогиненный обычный
пользователь** (клиент, не преподаватель и не админ) мог создавать/менять/
удалять группы занятий через прямой вызов API — обход задуманной
admin/teacher-only модели прав.

**Исправлено:** добавлен `@PreAuthorize("hasRole('ADMIN') or
hasRole('BUSINESS_OWNER')")` на все три write-метода, по образцу
большинства соседних контроллеров (3 из 5: Activity/Workshop/Performance;
Venue/Teacher строже — `ADMIN`-only на delete). `TEACHER`-роль сюда
осознанно не добавлена — LR-ADR-004 прямо говорит, что write-права для
преподавателя над своими группами — отдельный, ещё не спроектированный
кусок работы ("write-прав под это пока нет"), не путать с этим фиксом.
`./gradlew compileJava` зелёный. Существующих тестов на `GroupController`
не было (не сломано).

---

## LR-007 — Reactivate-эндпоинт для пользователя + фикс: `@PreAuthorize`-отказы возвращали 500 вместо 403 (весь бэкенд)

**Tier:** HIGH (auth) · **Статус:** Closed 2026-07-23
**Источник:** построение admin-панели (таск "teacher-дашборд + admin-панели"),
решение по reactivate — подтверждено заказчиком

**Часть 1 — `PUT /api/v1/users/{userId}/reactivate`:** старая admin-панель
умела деактивировать пользователя (`DELETE /users/{id}`, soft-disable через
`UserRepository.deactivateUser`), но обратного пути не было вообще —
подтверждено даже комментарием в старом JS ("backend doesn't seem to have
a simple reactivate endpoint"). Добавлено симметрично: `UserRepository
.activateUser()` (тот же `@Modifying @Query` паттерн, `enabled = true`),
`UserService.reactivateUser()`, `UserController`'s
`@PutMapping("/{userId}/reactivate")` (`ADMIN`-only, как и `DELETE`).

**Часть 2 — `GlobalExceptionHandler` не обрабатывал
`AuthorizationDeniedException`/`AccessDeniedException` (найдено при
написании теста на reactivate-эндпоинт, не специфично для него):**
любой `@PreAuthorize`-отказ (аутентифицирован, но не та роль) падал в
catch-all `handleAll(Exception)` и уходил клиенту как **500 Internal
Server Error** вместо **403 Forbidden** — во всём приложении, на любом
защищённом эндпоинте, не только на новом. Добавлен явный
`@ExceptionHandler({AuthorizationDeniedException.class,
AccessDeniedException.class})`, возвращающий 403. Покрыто
`UserControllerTest` (3 теста: ADMIN → 200, USER → 403 теперь корректно
вместо 500, unauthenticated → 401).

**Грабли при написании теста:** `SecurityMockMvcRequestPostProcessors
.jwt().jwt(j -> j.claim("role", "ADMIN"))` **не** прогоняет токен через
`SecurityConfig`'s собственный `JwtAuthenticationConverter` — тестовый
`jwt()` строит `Authentication` напрямую, минуя реальный
`BearerTokenAuthenticationFilter`/converter, и по умолчанию читает только
"scope"/"scp"-claim'ы. Кастомный claim `"role"` тестом молча
игнорируется — оба варианта (ADMIN и USER) без явного
`.authorities(new SimpleGrantedAuthority("ROLE_..."))` получали
`AuthorizationDeniedException`, что и вскрыло баг №2. Для тестов на
`hasRole(...)` всегда указывать authorities явно, не полагаться на
claim-based роль внутри `jwt()`.

---

## LR-011 — Перед деплоем шифрования PII: подтвердить безопасность миграции существующих данных

**Tier:** HIGH (DSGVO, риск потери доступа к данным) · **Статус:** Closed 2026-08-04
**Источник:** инвентаризация шифрования, 2026-07-24

Реальных клиентских данных на момент деплоя не было (`teachers`/
`participants` пусты), но собственный тестовый аккаунт заказчика (`users`,
id=1) имел заполненный `first_name` — единственная строка, которая бы
упала при первом чтении после включения `@Convert(EncryptedStringConverter
.class)` (Hibernate пытается расшифровать plaintext как
`base64(IV+ciphertext)`). **Решение заказчика:** удалить эту одну строку
вручную перед/после деплоя, не гонять `PiiReencryptionRunner` ради одной
тестовой записи.

**Подтверждено закрытым 2026-08-04:** ровно этот сценарий воспроизвёлся в
проде (см. `CHANGELOG.md` — `ArrayIndexOutOfBoundsException` при логине
`hudoshin7605@gmail.com`), не как неожиданность, а как предсказанный этим
тикетом риск. Аккаунт удалён вместе с зависимыми строками
(`feedbacks`/`user_notifications`/etc.). `PiiReencryptionRunner`
(`backend/src/main/java/com/be/tools/PiiReencryptionRunner.java`,
`@Profile("reencrypt-pii")`, dry-run по умолчанию, покрыт
`PiiReencryptionRunnerTest`) остаётся в кодовой базе на будущее — не
понадобился в этот раз, пригодится при следующей похожей миграции, если к
тому моменту накопятся реальные данные.

**Остаточный риск, отслеживается отдельно, не блокирует закрытие этого
тикета:** LR-003 (бэкапы) на момент этого деплоя ещё не был активен на
кластере — если бы деплой пошёл не так, отката через бэкап не было бы. Не
пригодилось в этот раз (деплой прошёл штатно), но риск был реальным во
время самого события — см. LR-003 за текущим статусом бэкапов.

**Follow-up, не блокирует, не забыт:** `Participant.birthDate` — нативная
`DATE`-колонка, текущий `EncryptedStringConverter` только `String→String`;
отдельный `LocalDate`-конвертер нужен, только если решат шифровать и дату
рождения — сейчас сознательно нет.

---

## LR-013 — Настроить реальные Brevo SMTP-креды для email-верификации

**Tier:** HIGH (auth-flow, сама работа — конфигурация) · **Статус:** Closed 2026-08-04
**Источник:** фича email-верификации, 2026-08-04

Код (`EmailVerificationService`/`MailService`/Brevo SMTP через
`spring-boot-starter-mail`) был готов и покрыт тестами с момента фичи, но
`SMTP_USERNAME`/`SMTP_PASSWORD` были пустыми дефолтами — письма не
уходили (перехвачено, не роняло регистрацию, но верификация была
недостижима).

**Закрыто:** домен `tlab29.com` аутентифицирован в Brevo (TXT + SPF + DKIM
CNAME, все — DNS only в Cloudflare, без прокси), sender `noreply@
tlab29.com` добавлен, SMTP-креды внесены в `lr-backend-secrets` через
`kubectl patch` (не пересоздавая секрет — не тронуты существующие
`jwt-secret`/`field-encryption-key`), `SMTP_USERNAME`/`SMTP_PASSWORD`
подключены в `backend-deployment.yaml` (`optional: true` — безопасно
деплоить и до появления этих ключей). Под перезапущен, реальная
регистрация проверена end-to-end: письмо приходит, ссылка подтверждает,
логин после этого проходит. Подтверждено заказчиком.

**Побочные находки в процессе закрытия (обе задокументированы отдельно,
не потеряны):**
- `MailHealthIndicator` (Spring Boot actuator) утаскивал весь
  `/actuator/health` в 503 без реальных SMTP-кредов — см. отдельную запись
  в `CHANGELOG.md` 2026-08-04, `management.health.mail.enabled=false`.
- Один legacy-аккаунт с незашифрованным `first_name` крашился при логине
  (`AttributeConverter`) — это и есть закрытие LR-011, найдено и решено в
  той же сессии.

**Follow-up, не блокирует, отслеживается отдельно:** LR-014 (timing
side-channel в `resendVerification`) остаётся open, low priority.

---

## LR-004 — Self-scoped payment history (`GET /api/v1/payments/me`)

**Tier:** MED (личный дашборд, LR-ADR-016) — не HIGH: не меняет
инвойс/платёжную запись, только читает по собственному `userId` из JWT.
**Статус:** Closed 2026-07-23
**Источник:** сессия 2026-07-22/23, построение личного дашборда

До этого изменения `PaymentController` не имел ни одного маршрута, доступного
обычному пользователю — все методы требовали `ADMIN`/`BUSINESS_OWNER`, что
блокировало требование "история платежей" личного дашборда (LR-ADR-016).

**Сделано:**
- `PaymentController.getMyPayments()` — `GET /api/v1/payments/me`,
  `@PreAuthorize("isAuthenticated()")`, `userId` берётся из JWT через
  `JwtAuthUtils.extractUserId()`, не принимается от клиента — исключает
  IDOR по конструкции (нет параметра `userId` для подмены).
- `PaymentService.getMyPayments(userId)` + `PaymentRepository
  .findByUserIdOrderByCreatedAtDesc(userId)` (тот же паттерн, что уже
  использовался в `EnrollmentRepository`).
- Найден и исправлен попутный баг в `SecurityConfig`: `GET
  /api/v1/activities/**` и `/api/v1/performances/**` не были в whitelist
  (только `/workshops/**`) — это 401'ило бы только что построенные
  публичные страницы каталога для любого анонимного посетителя.
- Тест `PaymentControllerTest` (`@WebMvcTest` + `spring-security-test`'s
  `jwt()` mock) — покрывает и happy path (пользователь получает свои
  платежи), и unauthenticated-запрос (401). Оба ревью-approve-условия
  architect-reviewer закрыты.

**Продуктовый вопрос про `note` — решён заказчиком (2026-07-23):** поле
`PaymentResponseDTO.note` (документировано как "optional refund reference or
note") **скрыто** от self-view — заведён `PaymentMapper.toSelfViewDTO()`,
используется только в `/payments/me`; admin-эндпоинты (`getAll`/`getById`)
по-прежнему используют `toResponseDTO()` с `note` как есть. Обоснование:
данных о том, что `note` реально используется как customer-facing поле, нет
(комментарий в коде — "admin/accounting reference"), значит по умолчанию —
не раскрывать, пока не появится осознанное решение сделать его
клиент-ориентированным (тогда — отдельное поле, не расширение смысла этого).
Покрыто `PaymentMapperTest` (2 теста: self-view без note, admin-view с note).

---

## LR-008 — `GroupService.update()` не копировал `startDateTime`/`endDateTime`

**Tier:** MED (тихая потеря данных при штатной операции — редактирование
расписания группы визуально "срабатывало", но не сохранялось)
**Статус:** Closed 2026-07-23
**Источник:** построение admin-панели Groups, 2026-07-23 — найдено при
чтении `GroupService.update()`

**Retroactively filed 2026-08-08** — этот ID использовался в комментарии
кода (`GroupService.java`) и в теле тикета LR-009 с самого начала, но
никогда не получал собственной записи в `tickets.md`/`archive.md` —
найдено и восстановлено при полной ретроспективе бэклога/архива. Отсюда
разрыв нумерации LR-007→LR-009 в остальной истории проекта.

`GroupService.update()` до этой сессии не копировал `startDateTime`/
`endDateTime` из запроса вообще — редактирование расписания группы через
admin-UI визуально проходило (200 OK), но время физически не менялось в
БД. Исправлено вместе с построением новой admin-страницы Groups, см.
`CHANGELOG.md` 2026-07-23.

**Не входило в скоуп этого фикса** (сознательно, стало отдельным тикетом
**LR-009**, остаётся open): смена `workshop` у уже существующей группы
при редактировании — отдельный, более сложный вопрос про судьбу
существующих `enrollments`.

---

## LR-048 — Круглый стол: Frontend UI/UX — состав и навигация

**Tier:** N/A (планирование/дизайн-сессия, не код) · **Статус:** Closed
2026-08-08 — Roundtable #6 проведён
**Источник:** прямой запрос заказчика, 2026-08-08, п.6.1 ретроспективы
бэклога

Круглый стол проведён (`docs/decision-history/roundtable-log.md`,
Roundtable #6) — панель Wroblewski/Nielsen/Cooper/Harris/Soueidan,
спроецирован на реальный код `frontend-svelte` (полная инвентаризация
route tree, навигации, дашбордов, UI-примитивов и токенов — не на текст
ADR). **Честная оговорка:** заземлён на инвентаризации реального кода,
не на живом визуальном обзоре через браузер (то, что изначально было
условием "не начинать без" в теле тикета) — по факту оказалось
достаточным для найденных находок (они все на уровне кода: дублирование
разметки, отсутствие секционных состояний, `aria`-атрибуты), но если
будущая итерация потребует именно визуальной/UX-оценки живых экранов —
это отдельный заход, не покрыт этим roundtable.

Прошёл ревью `architect-reviewer` на архитектурную релевантность —
фактических ошибок не найдено; один пункт (Harris, про `load()`)
изначально недооценивал реальную цену вопроса (требует смены Adapter,
не узкое исследование) — исправлено до финализации протокола.

**Результат:** 8 новых тикетов — `LR-053`..`LR-060` (единая навигация,
секционные loading-состояния, пересмотр `LR-002`/SPA-fallback,
admin-дашборд IA, мобильный вид Payments, точечный a11y-аудит, вынос
`Table`/`Badge` компонентов, проверка номенклатуры меню с заказчиком).

---

## LR-049 — Круглый стол: Мониторинг — универсальный подход в скоупе с numi/workout-evo

**Tier:** N/A (планирование, не код) · **Статус:** Closed 2026-08-08 —
Roundtable #7 проведён
**Источник:** прямой запрос заказчика, 2026-08-08, п.6.2 ретроспективы
бэклога

Круглый стол проведён (`docs/decision-history/roundtable-log.md`,
Roundtable #7) — панель Majors/Volz/Fong-Jones/Gregg/Long (последний —
для преемственности со Spring/Micrometer-частью, уже решённой на
Roundtable #5). Явно зафиксировано: архитектура единой ноды (VM600,
`otelcol-contrib`, per-project `scrape_configs`, `NetworkPolicy`-скоуп)
**уже была решена на Roundtable #5** — этот стол не переоткрывал её, а
закрыл то, что #5 не покрывал: схему именования метрик, канал
алертинга, self-monitoring самой VM600.

Прошёл ревью `architect-reviewer` — фактических ошибок не найдено;
дополнено по находкам ревьювера: VM300 уже сегодня несёт тот же класс
dual-role риска, что `gateway-core` (numi-хост + k3s-worker для
остановленного workout-evo), и Telegram-бот для алертинга — явно
отдельный от уже упомянутого в `LR-ADR-016` клиент-facing бота для
напоминаний.

**Результат:** 3 новых тикета — `LR-061`..`LR-063` (общий контракт
именования метрик кросс-проектно, выбор+подключение канала уведомлений
— Telegram-бот по умолчанию, требования к самомониторингу и размещению
VM600). `LR-031`'s Фаза 3 остаётся заблокирована до результата этого
стола — теперь результат есть, зависимость снята содержательно, но сама
Фаза 3 по-прежнему гейтится отдельно на реальном провижининге VM600
(`INFRA-008`).
