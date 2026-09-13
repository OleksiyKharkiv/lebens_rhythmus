# Сет-предложение тикетов-кандидатов в бэклог (tickets.md)
**Версия:** 1.1 (скорректированная по итогам совместного ревью Antigravity + Claude Code + `architect-reviewer`)  
**Дата:** 2026-09-13  
**Основание:** Аналитический отчет `docs/repotrs/analysis-report-ag.md` v1.1  
**Диапазон номеров:** `LR-099` .. `LR-108`

---

## ⚡ ТОП-3 СРОЧНЫХ ТИКЕТА ДЛЯ БЛИЖАЙШЕГО ВНЕДРЕНИЯ

1. **LR-099 [HIGH / SEC] — Устранение утечки PII учителей и площадок (`GET /teachers` и `GET /venues`), изоляция `GET /teachers/me`, закрытие под ADMIN, редирект при 403**  
   *Почему срочно:* Активная уязвимость прямо сейчас на проде. Любой авторизованный пользователь с ролью `USER` может выкачать личные телефоны и email всех преподавателей и контактные данные площадок в нарушение ст. 5, 8 и 9 DSGVO.
2. **LR-102 [HIGH / UX] — Ликвидация сброса языка (Silent Locale Drop) во всех внутренних ссылках и 401-хендлере (`localizeHref`)**  
   *Почему срочно:* Прямой дефект на живом сайте `tlab29.com`. Любой англо- или украиноязычный посетитель при клике на просмотр деталей курса/воркшопа или вход молча сбрасывается на немецкий язык.
3. **LR-100 [HIGH / INFRA-SEC] — Добавление HTTP Security Headers в `nginx.conf` (CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)**  
   *Почему срочно:* Базовый нулевой рубеж защиты веб-сервера. Закрывает угрозы Clickjacking, MIME-sniffing и несанкционированных внешних скриптов на продакшене.

---

## РАЗДЕЛ 1: БЕЗОПАСНОСТЬ И АВТОРИЗАЦИЯ

### LR-099 — Устранение утечки PII: изоляция `GET /teachers/me`, закрытие `GET /venues` и `GET /teachers` под ADMIN, редирект при 403

**Tier:** HIGH (авторизация, защита PII по DSGVO)  
**Раздел:** Безопасность и авторизация  
**Приоритет:** P0 (Критический — закрыть немедленно)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §2.1, §2.2)

#### Контекст и проблема
1. `TeacherController` (`GET /teachers`) и `VenueController` (`GET /venues`, `GET /venues/{id}`) не имеют аннотаций `@PreAuthorize` на бэкенде. Любой авторизованный пользователь (роль `USER`) может получить массив `TeacherInfoDTO` и `VenueDTO` с личными телефонами, email-адресами и статусами преподавателей и контактными данными площадок.
2. Фронтенд (`teacher/+page.svelte`) для поиска своего ID скачивает всех учителей и фильтрует их на клиенте через `allTeachers.find(...)`.
3. В `api.ts` метод `authRequest` перехватывает только статус 401, а при статусе 403 Forbidden не делает редирект, оставляя пользователя на экранах админки.

#### Что сделать (DoD)
1. **Backend:**
   - Создать эндпоинт `GET /api/v1/teachers/me` (возвращает `TeacherInfoDTO` только текущего авторизованного преподавателя).
   - Защитить полные листинги `GET /api/v1/teachers` и `GET /api/v1/venues` аннотацией `@PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")`.
2. **Frontend:**
   - В `teacher/+page.svelte` заменить вызов `getTeachers()` на вызов `getTeacherMe()`.
   - В `src/lib/api.ts` внутри `authRequest` добавить обработку статуса 403: принудительный переход на `/dashboard` с очисткой состояния несанкционированного доступа.

#### Затрагиваемые файлы
- `backend/src/main/java/com/be/web/controller/TeacherController.java`
- `backend/src/main/java/com/be/web/controller/VenueController.java`
- `backend/src/main/java/com/be/service/TeacherService.java`
- `frontend-svelte/src/lib/api.ts`
- `frontend-svelte/src/routes/teacher/+page.svelte`
- `frontend-svelte/src/routes/admin/+layout.svelte`

---

### LR-100 — Добавление HTTP Security Headers в `nginx.conf` фронтенда

**Tier:** INFRA / HIGH (защита от Clickjacking, XSS, MIME-sniffing)  
**Раздел:** Безопасность / Инфраструктура  
**Приоритет:** P1 (Высокий — топ-3)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §2.3)

#### Контекст и проблема
В `frontend-svelte/nginx.conf` настроена раздача статики и SPA-fallback, но полностью отсутствуют защитные HTTP-заголовки. Сайт уязвим к встраиванию в iframe сторонними ресурсами (Clickjacking) и атакам через некорректную интерпретацию MIME-типов.

#### Что сделать (DoD)
1. Добавить в блок `server` в `frontend-svelte/nginx.conf` стандартный комплект заголовков:
   - `add_header X-Frame-Options "DENY" always;`
   - `add_header X-Content-Type-Options "nosniff" always;`
   - `add_header Referrer-Policy "strict-origin-when-cross-origin" always;`
   - `add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;`
   - `add_header Content-Security-Policy "default-src 'self'; connect-src 'self' https://api.tlab29.com https://api.stripe.com; img-src 'self' data: https:; style-src 'self' 'unsafe-inline'; font-src 'self'; script-src 'self'; frame-ancestors 'none';" always;`
2. Проверить сборку Docker-образа фронтенда.

#### Затрагиваемые файлы
- `frontend-svelte/nginx.conf`

---

### LR-101 — Ужесточение CORS: удаление незащищенного HTTP и сужение методов

**Tier:** HIGH (CORS hygiene, сетевой периметр)  
**Раздел:** Безопасность и авторизация  
**Приоритет:** P1 (Высокий)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §2.4)

#### Контекст и проблема
В `CorsProperties.java` в список доверенных продакшн-источников включен `http://tlab29.com` (небезопасный протокол HTTP) наряду с HTTPS при включенном `allowCredentials = true`. В `WebMvcConfig.java` выставлены `allowedMethods("*")` и `allowedHeaders("*")`.

#### Что сделать (DoD)
1. Удалить `http://tlab29.com` из списка `allowedOrigins` по умолчанию.
2. В `WebMvcConfig.java` ограничить разрешенные HTTP-методы: `GET, POST, PUT, DELETE, PATCH, OPTIONS`.
3. Ограничить `allowedHeaders` до используемых заголовков: `Authorization, Content-Type, Accept, Origin, X-Requested-With`.

#### Затрагиваемые файлы
- `backend/src/main/java/com/be/config/CorsProperties.java`
- `backend/src/main/java/com/be/config/WebMvcConfig.java`
- `backend/src/test/java/com/be/config/CorsPropertiesTest.java`

---

## РАЗДЕЛ 2: ФРОНТЕНД И UX

### LR-102 — Ликвидация сброса языка (Silent Locale Drop) во всех внутренних ссылках и 401-хендлере

**Tier:** LOW (фронтенд-шаблоны)  
**Раздел:** Фронтенд / UX  
**Приоритет:** P1 (Критический для UX — топ-3)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.1)

#### Контекст и проблема
Paraglide JS переключается на язык по умолчанию (`de`), если URL не содержит префикса (`/en/..` или `/uk/..`). На ключевых страницах обнаружены голые ссылки без обертки `localizeHref`:
- `src/routes/courses/+page.svelte:70`: `<a href={`/courses/${c.id}`}>`
- `src/routes/workshops/+page.svelte:76, 102`: `<a href={`/workshops/${w.id}`}>`
- `src/routes/+page.svelte:29`: `href={resolve('/login')}` (главная кнопка регистрации в Hero)
- `src/routes/verify-email/+page.svelte:34, 39`: `href={resolve('/login')}`
- `src/lib/components/EnrollButton.svelte:39`: `window.location.href = `/login?returnTo=${returnTo}``
- `src/lib/api.ts:163`: 401-хендлер `window.location.href = '/login'`
При клике на любую из этих ссылок посетитель, выбравший английский или украинский язык, внезапно возвращается на немецкую версию сайта.

#### Что сделать (DoD)
1. Заменить все указанные ссылки на `localizeHref('/courses/' + c.id)` и `localizeHref('/workshops/' + w.id)`.
2. В `+page.svelte` и `verify-email/+page.svelte` заменить `resolve('/login')` на `localizeHref('/login')`.
3. В `EnrollButton.svelte` формировать URL возврата с сохранением текущей локали через `localizeHref`.
4. В `src/lib/api.ts` в 401-хендлере использовать `window.location.href = localizeHref('/login')`.
5. Добавить юнит-тесты на проверку формирования ссылок.

#### Затрагиваемые файлы
- `frontend-svelte/src/routes/courses/+page.svelte`
- `frontend-svelte/src/routes/workshops/+page.svelte`
- `frontend-svelte/src/routes/+page.svelte`
- `frontend-svelte/src/routes/verify-email/+page.svelte`
- `frontend-svelte/src/lib/components/EnrollButton.svelte`
- `frontend-svelte/src/lib/api.ts`

---

### LR-103 — Полноценный вывод мультиязычных полей DTO на страницах курсов и устранение хардкода строк

**Tier:** LOW (фронтенд-компоненты и словари сообщений)  
**Раздел:** Фронтенд / i18n  
**Приоритет:** P2 (Средний)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.2)

#### Контекст и проблема
1. Бэкенд передает мультиязычные поля для курсов (`titleDe/En/Ua`, `descriptionDe/En/Ua`, `formatDisclaimerDe/En/Ua`). Однако `courses/+page.svelte` и `courses/[id]/+page.svelte` жестко выводят только немецкие поля (`.titleDe`, `.descriptionDe`, `.formatDisclaimerDe`), игнорируя локаль пользователя.
2. В коде страниц захардкожены немецкие строки:
   - `login/+page.svelte`: ошибки валидации.
   - `feedback/+page.svelte`: `'Fehler beim Senden.'`
   - `workshops/+page.svelte`: метка `"Start:"`
   - `Input.svelte`: `aria-label` для пароля.
3. Форматирование дат в `scheduleUtils.ts` жестко привязано к `'de-DE'`.

#### Что сделать (DoD)
1. Создать хелпер `localizedField(item, fieldName)` в `$lib/i18nUtils.ts`, возвращающий перевод в соответствии с активной локалью `getLocale()`.
2. Обновить шаблоны `courses/+page.svelte` и `courses/[id]/+page.svelte` для вывода локализованных полей.
3. Вынести все захардкоженные строки в словари сообщений Paraglide.
4. Сделать утилиту форматирования дат чувствительной к языку интерфейса.

#### Затрагиваемые файлы
- `frontend-svelte/src/lib/i18nUtils.ts` (новый)
- `frontend-svelte/src/routes/courses/+page.svelte`
- `frontend-svelte/src/routes/courses/[id]/+page.svelte`
- `frontend-svelte/src/routes/login/+page.svelte`
- `frontend-svelte/src/routes/feedback/+page.svelte`
- `frontend-svelte/src/routes/workshops/+page.svelte`
- `frontend-svelte/src/lib/components/Input.svelte`
- `frontend-svelte/messages/de.json`, `en.json`, `uk.json`

---

### LR-104 — Точечная замена редиректов на `goto()` с сохранением full-reload для смены сессии

**Tier:** LOW (фронтенд-навигация)  
**Раздел:** Фронтенд  
**Приоритет:** P2 (Средний)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.4)

#### Контекст и проблема
При смене auth-состояния (401 в `api.ts`, логаут, кик неавторизованного) полный релод через `window.location.href = localizeHref(...)` архитектурно необходим, так как `localStorage` нереактивен и SvelteKit не перемонтирует корневой layout при `goto()` (задокументировано в `+layout.svelte:65-77`). Однако в случаях, когда пользователь уже залогинен, но пытается перейти в раздел не своей роли («не та роль» в `admin/+layout.svelte` и `teacher/+page.svelte`), полный релод избыточен.

#### Что сделать (DoD)
1. В ветках «не та роль» (`admin/+layout.svelte` и `teacher/+page.svelte`) заменить `window.location.href = '/dashboard'` на SvelteKit-роутер `goto(localizeHref('/dashboard'))`.
2. Во всех остальных местах смены auth-состояния (`EnrollButton.svelte`, `feedback`, `api.ts`, логаут) сохранить `window.location.href`, убедившись, что путь обернут в `localizeHref(...)`.

#### Затрагиваемые файлы
- `frontend-svelte/src/routes/admin/+layout.svelte`
- `frontend-svelte/src/routes/teacher/+page.svelte`
- `frontend-svelte/src/routes/+layout.svelte`
- `frontend-svelte/src/lib/api.ts`

---

### LR-105 — Адаптивная калибровка шрифта мобильных экранов внутри системы брейкпоинтов

**Tier:** LOW (CSS)  
**Раздел:** Фронтенд / Стили  
**Приоритет:** P3 (Средний)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.5)

#### Контекст и проблема
Размер 22px (`text-[1.375rem]`) в карточках направлений главной страницы (`routes/+page.svelte`) был прямым запросом Олены от 2026-08-19 ("+0.5rem"). Однако на смартфонах с шириной экрана 360–390px 22px приводит к неэстетичным разрывам строк. Архитектура лестницы брейкпоинтов сохраняется, так как защищает от багов сортировки каскада в Tailwind v4 (`RESPONSIVE_SCALING_PLAYBOOK.md`).

#### Что сделать (DoD)
1. Согласовать с Оленой адаптивное уменьшение шрифта карточек направлений строго для экранов `<640px` (до `1.125rem` / 18px), сохраняя размер `1.375rem` для десктопов.
2. Внести правку внутрь принятой системы CSS-классов без нарушения каскада Tailwind v4.

#### Затрагиваемые файлы
- `frontend-svelte/src/routes/+page.svelte`
- `frontend-svelte/src/routes/layout.css`

---

### LR-106 — Повышение доступности (A11y) и унификация компонентов ввода

**Tier:** LOW (a11y / UI-компоненты)  
**Раздел:** Фронтенд / Доступность  
**Приоритет:** P4 (Низкий)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.6)

#### Что сделать (DoD)
1. Убрать `tabindex="-1"` с кнопки пароля в `Input.svelte`, добавить динамический переводимый `aria-label`.
2. В `Button.svelte` добавить атрибут `aria-busy={busy}` и заменить троеточие на аккуратный SVG-спиннер с сохранением фиксированной высоты кнопки.
3. Заменить сырые теги `<select>` в админке на компонент `Select.svelte`.

#### Затрагиваемые файлы
- `frontend-svelte/src/lib/components/Input.svelte`
- `frontend-svelte/src/lib/components/Button.svelte`
- `frontend-svelte/src/routes/admin/users/+page.svelte`
- `frontend-svelte/src/routes/admin/groups/+page.svelte`

---

## РАЗДЕЛ 3: БЭКЕНД И API

### LR-107 — Ликвидация клиентского N+1: эндпоинт `GET /api/v1/users/me/media`

**Tier:** MED (новый эндпоинт в API личного кабинета)  
**Раздел:** Бэкенд  
**Приоритет:** P2 (Средний)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §3.3)

#### Контекст и проблема
На странице `dashboard/+page.svelte` фронтенд получает список бронирований пользователя и делает веер отдельных запросов `GET /api/v1/workshops/{id}`, чтобы собрать список прикрепленных файлов (`media`).

#### Что сделать (DoD)
1. Реализовать на бэкенде эндпоинт `GET /api/v1/users/me/media`, возвращающий медиа-файлы тех курсов/воркшопов, где у пользователя есть активная запись.
2. На фронтенде заменить `Promise.all(workshopIds.map(getWorkshop))` на единичный вызов `getMyMedia()`.

#### Затрагиваемые файлы
- `backend/src/main/java/com/be/web/controller/UserController.java`
- `backend/src/main/java/com/be/service/UserService.java`
- `frontend-svelte/src/lib/api.ts`
- `frontend-svelte/src/routes/dashboard/+page.svelte`

---

### LR-108 — Архитектурная подготовка миграции JWT из `localStorage` в `HttpOnly SameSite Cookies`

**Tier:** HIGH (безопасность сессий, XSS-защита)  
**Раздел:** Безопасность и авторизация  
**Приоритет:** P3 (Плановое улучшение)  
**Статус:** Proposed  
**Источник:** аудит фронтенда Antigravity 2026-09-13 (`docs/repotrs/analysis-report-ag.md` §2.5)

#### Что сделать (DoD)
1. Провести Architecture Pre-Check по переводу сессий на `Set-Cookie: authToken=...; HttpOnly; Secure; SameSite=Lax`.
2. Настроить фильтр Spring Security на чтение JWT как из заголовка `Authorization`, так и из Cookie.

#### Затрагиваемые файлы
- `backend/src/main/java/com/be/web/controller/AuthController.java`
- `backend/src/main/java/com/be/config/SecurityConfig.java`
- `frontend-svelte/src/lib/api.ts`
