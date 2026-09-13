# Экспертный аналитический отчет аудита фронтенда Lebens Rhythmus (tlab29.com)
**Версия:** 1.1 (скорректированная по итогам совместного ревью Antigravity + Claude Code + `architect-reviewer`)  
**Дата:** 2026-09-13  
**Объект аудита:** `frontend-svelte/` (SvelteKit 2, Svelte 5 runes, Vite 8, Tailwind CSS v4, Paraglide JS i18n, adapter-static SPA-fallback)  
**Базовые регуляторные и архитектурные рамки:** DSGVO (Art. 5, 8, 9), ZFU/FernUSG, LR-ADR-001..024, CODING_PROTOCOL.md (§4, §4b), RESPONSIVE_SCALING_PLAYBOOK.md.

---

## 1. Резюме и статус согласования

Отчет отражает согласованную позицию независимого аудита Antigravity и ревью Claude Code с субагентом `architect-reviewer`. 
Из 10 исходных пунктов 7 подтверждены полностью, 3 скорректированы с учётом глубокого контекста кодовой базы (механика клиентского гейта vs PII-эндпоинтов, обоснованность `window.location.href` при смене auth-состояния из-за нереактивности `localStorage`, и сохранение выстраданной лестницы брейкпоинтов вместо `clamp()` из-за бага сортировки медиа-правил в Tailwind v4). Дополнительно обнаружена ещё одна аналогичная PII-уязвимость в `VenueController`.

---

## 2. Раздел 1: Безопасность и уязвимости (PII, Auth, CORS, Headers)

### 2.1. Утечка персональных данных преподавателей и площадок (PII Overfetching по DSGVO) — [КРИТИЧНО / P0]
* **Локация:** 
  - `src/routes/teacher/+page.svelte` (строки 22–37), бэкенд `TeacherController.java` (`GET /teachers`).
  - `VenueController.java` (`GET /venues`, `GET /venues/{id}`), `VenueDTO`.
* **Суть проблемы:**
  1. Для отображения кабинета преподавателя фронтенд вызывает `GET /teachers` и фильтрует преподавателей на клиенте:
     ```typescript
     const allTeachers = await getTeachers();
     const myTeacherRow = allTeachers.find((t) => t.email === user.email);
     ```
     Эндпоинт `GET /teachers` отдает массив `TeacherInfoDTO`, содержащий **личные телефоны, email-адреса, полные имена и статусы всех преподавателей студии**. На бэкенде у этого эндпоинта нет ограничений по роли (`@PreAuthorize`) — он доступен любому авторизованному аккаунту с ролью `USER`.
  2. Аналогичная дыра (найдена `architect-reviewer`): `VenueController.getAllVenues` и `getVenueById` также не имеют `@PreAuthorize`, а `VenueDTO` светит `contactPhone` и `contactEmail` площадок любому авторизованному пользователю.
* **Последствия:**
  Прямое нарушение требований ст. 5 (минимизация данных) и ст. 8/9 DSGVO. Любой зарегистрированный пользователь через консоль или вкладку Network может выгрузить контакты всех преподавателей и площадок.
* **Решение:**
  - На бэкенде создать эндпоинт `GET /api/v1/teachers/me`, возвращающий профиль строго вызывающего преподавателя.
  - Закрыть полные листинги `GET /api/v1/teachers` и `GET /api/v1/venues` аннотацией `@PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")`.
  - Для публичных страниц (если потребуется) отдавать строго очищенные DTO без телефонов и личных email.

---

### 2.2. Клиентский гейт админки и отсутствие редиректа при 403 Forbidden — [P1]
* **Локация:** `src/lib/api.ts` (`getStoredRole()`, `authRequest()`), `src/routes/admin/+layout.svelte`.
* **Уточненная оценка механики:**
  Все 23 мутирующих эндпоинта бэкенда корректно защищены `@PreAuthorize`, поэтому серверной эскалации привилегий не происходит. Однако:
  1. `getStoredRole()` читает незащищенный `localStorage.getItem('userData')`, что позволяет любому пользователю через F12 подменить JSON и отрисовать интерфейс админки в браузере.
  2. Клиентское декодирование клейма `role` из JWT без валидации сервером не спасает (security theater), так как браузер без секрета не может проверить HMAC-подпись, и злоумышленник может положить строку с фейковой подписью.
  3. В `api.ts` метод `authRequest` перехватывает и очищает сессию только при статусе **401 Unauthorized**. При статусе **403 Forbidden** (когда бэкенд правомерно блокирует несанкционированный запрос) `authRequest` не делает редирект, а страницы глушат ошибку в `catch(() => error = true)`. Пользователь остается внутри интерфейса админки.
* **Решение:**
  - При получении **403 Forbidden** в `authRequest` немедленно выполнять принудительный редирект `goto(localizeHref('/dashboard'))` с понятным сообщением об отказе в доступе.
  - Сверять права через серверный `GET /users/me` при входе в защищенные разделы.

---

### 2.3. Полное отсутствие Security Headers в веб-сервере — [P1]
* **Локация:** `frontend-svelte/nginx.conf`.
* **Суть проблемы:**
  В конфигурации Nginx полностью отсутствуют базовые заголовки безопасности:
  - Нет `Content-Security-Policy` (CSP).
  - Нет `X-Frame-Options: DENY` (риск Clickjacking).
  - Нет `X-Content-Type-Options: nosniff` (риск MIME-sniffing).
  - Нет `Referrer-Policy: strict-origin-when-cross-origin`.
  - Нет `Permissions-Policy`.
* **Решение:**
  Добавить директивы `add_header` в `frontend-svelte/nginx.conf`.

---

### 2.4. Конфигурация CORS (CORS Hygiene) — [P1]
* **Локация:** `backend/src/main/java/com/be/config/CorsProperties.java`, `WebMvcConfig.java`.
* **Суть проблемы:**
  В `CorsProperties.java` в список доверенных продакшн-источников включен незашифрованный `http://tlab29.com` при `allowCredentials = true`. В `WebMvcConfig.java` выставлены `allowedMethods("*")` и `allowedHeaders("*")`.
* **Решение:**
  Удалить `http://tlab29.com` из продакшн-списка разрешенных origins; ограничить методы до явного перечня `GET, POST, PUT, DELETE, OPTIONS, PATCH`.

---

### 2.5. Хранение Bearer JWT в `localStorage` (Долгосрочный риск) — [P3]
* **Локация:** `src/lib/api.ts` (`persistSession()`, `isAuthenticated()`, `authRequest()`).
* **Суть проблемы:**
  JWT-токен хранится в открытом виде в `localStorage.getItem('authToken')`. Любой XSS-вектор позволяет эксфильтрировать токен с 24-часовым сроком жизни.
* **Решение:**
  Плановая миграция на сессионные куки с флагами `HttpOnly`, `Secure`, `SameSite=Lax`.

---

## 3. Раздел 2: UI/UX, обработка данных и верстка

### 3.1. Молчаливый сброс языка интерфейса (Silent Locale Drop) — [КРИТИЧНО ДЛЯ UX / P1]
* **Локация:** 
  - `src/routes/courses/+page.svelte` (строка 70): `<a href={`/courses/${c.id}`}>`
  - `src/routes/workshops/+page.svelte` (строки 76, 102): `<a href={`/workshops/${w.id}`}>`
  - `src/routes/+page.svelte` (строка 29): `href={resolve('/login')}` на главной кнопке в Hero
  - `src/routes/verify-email/+page.svelte` (строки 34, 39): `href={resolve('/login')}`
  - `src/lib/components/EnrollButton.svelte` (строка 39): `window.location.href = `/login?returnTo=${returnTo}``
  - `src/lib/api.ts` (строка 163): 401-хендлер `window.location.href = '/login'` (найдено Claude Code)
* **Суть проблемы:**
  Paraglide JS при отсутствии языкового префикса сбрасывает локаль на `baseLocale` (`de`). Посетитель на английском или украинском языке при переходе по любой из этих ссылок молча переключается обратно на немецкую версию.
* **Решение:**
  Обернуть все указанные пути в `localizeHref(...)`.

---

### 3.2. Хардкод немецких полей и строк при наличии мультиязычного DTO — [P2]
* **Локация:**
  - `src/routes/courses/+page.svelte` и `courses/[id]/+page.svelte`: отображаются исключительно `.titleDe`, `.descriptionDe` и `.formatDisclaimerDe`. Переводы на английский (`En`) и украинский (`Ua`), приходящие с бэкенда, игнорируются.
  - `src/routes/login/+page.svelte`: ошибки `'E-Mail oder Passwort falsch.'`, `'Bitte Bedingungen akzeptieren.'`, `'Passwörter stimmen nicht überein.'` захардкожены на немецком.
  - `src/routes/feedback/+page.svelte`: строка `'Fehler beim Senden.'` захардкожена.
  - `src/routes/workshops/+page.svelte`: метка `"Start:"` захардкожена.
  - `src/lib/components/Input.svelte`: `aria-label` для пароля захардкожен.
  - Форматирование дат (`scheduleUtils.ts` `formatDateDE`, `performances/+page.svelte`, `workshops/+page.svelte`): жестко привязано к `'de-DE'`.
* **Решение:**
  Реализовать хелпер `localizedField(item, field)` с учетом `getLocale()`, вынести строки в словари Paraglide, поддержать локали в форматерах дат.

---

### 3.3. Сетевой N+1 антипаттерн в дашборде пользователя — [P2]
* **Локация:** `src/routes/dashboard/+page.svelte` (строки 41–45).
* **Суть проблемы:**
  Клиент загружает бронирования и делает веер отдельных запросов `getWorkshop(id)` для каждого воркшопа ради получения списка файлов.
* **Решение:**
  Реализовать эндпоинт `GET /api/v1/users/me/media` на бэкенде, чтобы дашборд забирал медиа одним запросом.

---

### 3.4. Архитектура клиентской навигации: точечный `goto()` vs `window.location.href` — [P2]
* **Уточненная позиция:**
  Ковровый перевод всех редиректов на `goto()` сломает интерфейс. В коде [src/routes/+layout.svelte](file:///c:/Users/hudos/IdeaProjects/lebens_rhythmus/frontend-svelte/src/routes/+layout.svelte#L65-L77) зафиксировано: `localStorage` нереактивен, и SvelteKit не ремонтирует корневой layout при клиентском переходе через смену авторизации (в шапке останутся старые кнопки).
* **Решение:**
  - **Сохранить `window.location.href` (с добавлением `localizeHref`) в 5 местах смены auth-состояния**: 401-хендлер в `api.ts`, логаут, редирект неавторизованного в `dashboard`, `feedback` и `EnrollButton`.
  - **Использовать `goto(localizeHref(...))` только в 2 местах**: ветка «не та роль» в `admin/+layout.svelte` и `teacher/+page.svelte` (пользователь уже залогинен, ремонтировать layout не требуется).

---

### 3.5. Адаптивная типографика: калибровка внутри принятой лестницы брейкпоинтов — [P3]
* **Уточненная позиция:**
  Снос лестницы брейкпоинтов на `clamp()` отклонен: в Tailwind v4 кастомные и встроенные брейкпоинты не сортируются по пикселям (`798f0ae`, `RESPONSIVE_SCALING_PLAYBOOK.md`), и чистые CSS-классы с упорядоченными `@media` — это стабильное решение. Кроме того, размер 22px (`text-[1.375rem]`) был прямым запросом Олены от 2026-08-19 ("+0.5rem").
* **Решение:**
  Сохранить архитектуру лестницы. Добавить адаптивное уменьшение шрифта для мобильных экранов (<640px) внутри принятой системы классов, предварительно согласовав правку с Оленой.

---

### 3.6. Доступность (A11y) и унификация компонентов — [P4]
- Убрать `tabindex="-1"` с кнопки пароля в `Input.svelte`.
- Добавить `aria-busy={busy}` в `Button.svelte` и заменить троеточие на спиннер с сохранением геометрии.
- Унифицировать использование `Select.svelte` в админке.
