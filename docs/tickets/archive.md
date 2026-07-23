# archive.md — Lebens Rhythmus closed tickets

> Закрытые тикеты переносятся сюда из `tickets.md` (per workflow §8 в
> корневом `CLAUDE.md`).

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
