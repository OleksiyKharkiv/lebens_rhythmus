# Lebens Rhythmus — CHANGELOG
> Формат: [дата] [тип] [файл/область] — описание
> Типы: feat | fix | security | compliance | refactor | infra | docs

## 2026-08-14 — refactor+security: артефакт-аудит — `LR-ADR-005` компромисс закреплён, `erm.drawio` актуализирован, последняя mass-assignment дыра на `Group` закрыта

### Область (`docs/architecture/{decisions.md,erm.drawio}`, `docs/context/{CLAUDE.md,CODING_PROTOCOL.md}`, `docs/decision-history/roundtable-log.md` (список grep-only), `backend/src/main/java/com/be/{service/GroupService.java,web/controller/GroupController.java,web/dto/request/GroupUpdateDTO.java (new)}`, `backend/src/test/java/com/be/service/GroupServiceTest.java`, `frontend-svelte/src/{lib/api.ts,routes/admin/{courses,groups}/+page.svelte}`)

- **refactor (архитектурное решение, не код)** — предложенный полный
  domain/persistence-split по Мартину для `Group`/`Workshop` (5 новых
  файлов на сущность: `domain/model`, переименованный `*Entity`, порт,
  адаптер, маппер) представлен владельцу и **осознанно отклонён** —
  цена не оправдана теоретической (не практической) пользой при
  зафиксированном стеке Java/Spring/Postgres. `LR-ADR-005` дополнен
  апдейтом 2026-08-14: JPA-аннотации в `entity/` — норма для всех
  сущностей, аксиомы про Spring/Postgres/будущие изолированные БД-модули
  зафиксированы явно. `CODING_PROTOCOL.md`'s "непроверенное допущение"
  предупреждение снято — решение теперь явное, не черновик.
- **security (найдено при аудите, не живой инцидент)** — последняя
  `@RequestBody`-на-сырую-сущность дыра на этом контроллере:
  `GroupController.updateGroup` биндил `Group` напрямую (тот же класс
  риска, что `LR-030` уже закрыл на `createGroup` — крафченный
  `enrollments`-массив мог re-parent'ить чужой Enrollment,
  `CascadeType.ALL`+`orphanRemoval=true`). Заменено на `GroupUpdateDTO`
  (flat ids, без `workshopId` — реассайн воркшопа на update и раньше не
  поддерживался). Заодно применён authoritative-clear-on-null паттерн
  (id отсутствует → явно `null`, не skip) — тот же баг-класс, что нашёлся
  живьём в `CourseService`/`WorkshopService` этой же сессией. Frontend
  (`admin/courses`, `admin/groups`) синхронизирован — оба места слали
  вложенные `{id}`-объекты, теперь flat id, добавлен `toUpdateRequest()`
  зеркально уже существующему `toCreateRequest()`.
- **refactor (docs)** — `erm.drawio`: `Venue→Workshop` ребро (устарело с
  `LR-015`) исправлено на `Venue→Group`; добавлено прямое `Group→Workshop`
  ребро (не было вообще); 4 фантомных join-сущности без backing-кода
  (`ActivityGroup`, `ParticipantGroup`, `GroupWorkshop`, `TeacherWorkshop`)
  заменены прямыми FK-рёбрами; 2 неимплементированных узла (`Payment
  Method`, `ParticipantPerformance` — оба подтверждены закомментированными/
  free-text в реальном коде) удалены без замены. XML провалидирован,
  висячих ссылок нет. `roundtable-log.md` добавлен в grep-only список.
- **verify** — `./gradlew compileTestJava`/`test --tests GroupServiceTest`
  зелёные (3 новых теста: resolve, authoritative-clear, mutual-exclusivity
  guard); `npm run check` 0 ошибок; `npm test` 13/13.
- **docs** — `architect-reviewer` на этот дифф см. следующую запись/коммит.

## 2026-08-11 — feat: LR-073 закрыт — admin-страница Teachers + фикс `@Pattern`-бага на пустом телефоне (3 DTO)

### Область (`frontend-svelte/src/{lib/api.ts,routes/admin/{teachers/+page.svelte (new),+layout.svelte}}`, `frontend-svelte/messages/{de,en,uk}.json`, `backend/src/main/java/com/be/web/dto/request/{TeacherRequestDTO,UserUpdateDTO,ParticipantRequestDTO}.java`, `backend/src/test/java/com/be/web/dto/request/RequestDtoValidationTest.java`, `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-073)** — `admin/teachers/+page.svelte` (по образцу
  `admin/age-groups`), полный CRUD поверх уже готового
  `TeacherController`. Нав-пункт "Lehrkräfte" + i18n во всех локалях.
- **fix (найдено живьём при первой реальной отправке формы)** —
  `TeacherRequestDTO.phone`'s `@Pattern` не пропускал `""` (только
  `null` автоматически валиден в Bean Validation) — `POST /teachers`
  400'ил для любого учителя без телефона, самый частый случай.
  Идентичный скопированный паттерн нашёлся и в `UserUpdateDTO.phone`/
  `ParticipantRequestDTO.phone` — исправлены все три одним дифом
  (`"^$|"` перед существующим regex), не только блокирующий текущую
  форму. 3 новых теста в `RequestDtoValidationTest`.
- **verify** — живой браузерный прогон полного CRUD-цикла (create 201
  → edit 200 → delete 204) через настоящую форму, шифрование/
  расшифровка PII подтверждена без ошибок. `npm run check`/`build`
  чисто, полный `./gradlew test` — 0 failures/errors.
- **docs** — `LR-073` закрыт → `archive.md`.

## 2026-08-11 — feat: LR-072 закрыт — `Workshop.teacher` мигрирован `User` → `Teacher` + фикс 403 в личном кабинете учителя

### Область (`backend/src/main/{resources/db/migration/V9__migrate_workshop_teacher_to_teacher.sql,java/com/be/{domain/entity/Workshop.java,service/WorkshopService.java,web/{dto/{request/WorkshopCreateDTO,response/{WorkshopListDTO,WorkshopDetailDTO}}.java,mapper/WorkshopMapper.java}}}`, `backend/src/test/java/com/be/service/WorkshopServiceTest.java`, `frontend-svelte/src/{lib/api.ts,routes/{admin/workshops/+page.svelte,teacher/+page.svelte}}`, `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-072)** — `Workshop.teacher` `User` → `Teacher` (устраняет
  asymmetry с уже корректным `Group.teacher`). `V9`-миграция
  безопасна по построению (не зависит от предварительного знания
  состояния таблицы — см. отдельная заметка ниже об отклонении от
  буквы тикета): remap по email-совпадению, явная запись несовпавших
  случаев в `lr072_unmigrated_workshop_teachers` до обнуления, потом
  смена FK-constraint на `teachers(id)`.
- **fix (найдено тем же дифом, тот же корень путаницы)** —
  `teacher/+page.svelte` слал `User.id` туда, где backend уже сравнивал
  с резолвнутым `Teacher.id` — "Мои воркшопы" в личном кабинете учителя
  падало в 403 для любого учителя с несовпадающими id. Исправлено
  вместе с миграцией, не отдельным тикетом.
- **docs (честно зафиксировано)** — пользователь дал команду
  "доделай миграции" без результата запрошенной живой прод-проверки
  (тикет явно требовал её перед миграцией данных). Решение: не
  повторно блокировать работу, а спроектировать миграцию, которая сама
  себе гарантия безопасности, независимо от факта проверки — см.
  подробности в `archive.md`.
- **verify** — реальный Postgres, вручную засеяны 3 сценария
  (совпадающий email, несовпадающий, `NULL`) через `psql`, миграция
  применена, все три случая подтверждены: корректный remap, обнуление
  + аудит-запись, `NULL` не тронут, FK подтверждён указывающим на
  `teachers`. Отдельно — полный `bootRun` против уже смигрированной
  схемы, Hibernate `ddl-auto=validate` прошла чисто. `npm run check`
  чисто, полный `./gradlew test` — 0 failures/errors.
- **docs** — `LR-072` закрыт → `archive.md`; `LR-074`'s описание
  уточнено (teacher-дропдаун уже починен здесь, не в его скоупе).

## 2026-08-11 — feat: LR-071 закрыт — `Performance.courseId` (nullable FK) + попутный фикс `workshopId`-клиринга

### Область (`backend/src/main/{resources/db/migration/V8__add_performance_course_fk.sql,java/com/be/{domain/entity/Performance.java,service/PerformanceService.java,web/{dto/{request/PerformanceRequestDTO,response/PerformanceResponseDTO}.java,mapper/PerformanceMapper.java}}}`, `backend/src/test/java/com/be/service/PerformanceServiceTest.java` (new), `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-071/LR-ADR-021)** — `Performance.course` (nullable,
  добавлено рядом с уже существующим `workshop`, не заменяет —
  Performance может завершать Course, Workshop, оба или ни то ни
  другое). Миграция + DTO-поля + `PerformanceService` резолвит
  `courseId` через новый `CourseRepository`-инжект.
- **fix (найдено по ходу, не отдельным тикетом)** — тот же
  клиринг-баг ("устанавливает, но не снимает" при `null`), что уже
  дважды чинился на этой неделе (`CourseService`/`WorkshopService`),
  нашёлся и у уже существующего `Performance.workshop` —
  `admin/performances/+page.svelte` реально позволяет сбросить
  workshop через "—". Исправлено тем же дифом вместе с `courseId`, обе
  связи авторитетны на каждый update.
- **test** — новый `PerformanceServiceTest.java` (сервис раньше не
  имел ни одного юнит-теста) — 3 теста, включая регрессионный на
  очистку `workshop`.
- **verify** — `npm run check` чисто, `./gradlew compileJava
  compileTestJava` чисто, полный `./gradlew test` — 0 failures/errors.
- **docs** — `LR-071` закрыт → `archive.md`.

## 2026-08-11 — feat: LR-070 закрыт — `Workshop.courseId` (nullable FK)

### Область (`backend/src/main/{resources/db/migration/V7__add_workshop_course_fk.sql,java/com/be/{domain/entity/Workshop.java,service/WorkshopService.java,web/{dto/{request/WorkshopCreateDTO,response/WorkshopDetailDTO}.java,mapper/WorkshopMapper.java}}}`, `backend/src/test/java/com/be/service/WorkshopServiceTest.java` (new), `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-070/LR-ADR-021)** — `Workshop.course` (nullable
  `@ManyToOne`, zero-to-many Course→Workshop), миграция + DTO-поля +
  `WorkshopService` резолвит `courseId` через новый
  `CourseRepository`-инжект.
- **fix (применено проактивно, урок из предыдущей записи)** —
  `updateWorkshop` обнуляет `course` при `dto.getCourseId() == null`
  (authoritative on every update), не skip-if-null — тот же класс бага,
  что был только что найден живьём для `teacher`, не повторён здесь.
- **test** — новый `WorkshopServiceTest.java` (сервис раньше не имел ни
  одного юнит-теста) — 3 теста, включая регрессионный на очистку
  `teacher` через `null` (доказывает фикс из предыдущей записи
  применяется и к этому сервису).
- **verify** — `npm run check` чисто, `./gradlew compileJava
  compileTestJava` чисто, полный `./gradlew test` — 0 failures/errors.
- **docs** — `LR-070` закрыт → `archive.md`.

## 2026-08-09 — feat: LR-069/075/076/078 закрыты — Course MVP (backend+admin+public+AGB) + 6 живых прод-фиксов

### Область (`backend/src/main/{resources/db/migration/V6__add_courses.sql,java/com/be/{domain/entity/Course.java,domain/repository/CourseRepository.java,service/CourseService.java,web/{controller/CourseController.java,dto/{request/CourseCreateDTO.java,response/{CourseListDTO,CourseDetailDTO}.java},mapper/CourseMapper.java},config/SecurityConfig.java}}`, `backend/src/test/java/com/be/service/CourseServiceTest.java` (new), `frontend-svelte/src/{lib/api.ts,routes/{admin/{courses/+page.svelte (new),+layout.svelte},courses/{+page.svelte,[id]/+page.svelte} (new),+layout.svelte,agb/+page.svelte}}`, `frontend-svelte/messages/{de,en,uk}.json`, `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-069)** — `Course` entity/repository/service/controller/DTO/mapper, чисто описательная сущность без полей расписания (`LR-ADR-023`). Прямой запрос заказчика добавил `Course.teacher` (temporary `User` FK, поиск по фамилии/email) — задокументировано как уточнение, не тихое отступление от ADR (Workshop уже имеет тот же паттерн: свой `teacher` отдельно от `Group.teacher`).
- **feat (LR-075/076)** — `admin/courses/+page.svelte` (форма создания/редактирования, live-поиск учителя через уже существующий `GET /users/search`), `routes/courses/{+page,[id]/+page}.svelte` (публичный каталог + детальная страница с полем "Kursleitung"), нав-пункт "Kurse" (public+admin), i18n-ключи во всех трёх локалях.
- **compliance (LR-078)** — AGB §9 (Theaterlabor, с `id="theaterlabor"` для прямой ссылки из письма в ZFU)/§10 (Änderungsvorbehalt)/§11 (Aufführungen-съёмка, с явной оговоркой п.11.4 про Theaterlabor) — текст дословно из `docs/compliance/tlab29-zfu-compliance-brief.md`.
- **fix (прод-инцидент, найден по репорту заказчика после пуша)** — `SecurityConfig`'s GET-permitAll список не включал `/api/v1/courses/**` (401 даже для admin, т.к. фронт намеренно шлёт этот запрос без JWT, как для Workshop) — добавлено рядом с workshops/activities/performances.
- **fix (UI, найден по репорту заказчика)** — `admin/courses/+page.svelte`'s мультиязычные поля в 3-колоночной grid визуально разъезжались (`Input`/`Textarea` рендерят label+input как несвёрнутые top-level siblings — CSS Grid расставляет их как N×2 независимых элементов, не парами) — обёрнуто в `<div>` per-field. Тот же баг подтверждён в `admin/groups/+page.svelte` — не тронут в рамках этого тикета, вынесен отдельным spawn_task.
- **fix (UX, найден по репорту заказчика)** — активный пункт admin sub-nav не подсвечивался — добавлена `isActive()`-логика (точное совпадение для `/admin`, префиксное для остальных).
- **fix (UX, найден по репорту заказчика)** — admin-контейнер (`max-w-6xl`) слишком узкий для плотных 3-колоночных форм — расширен до `max-w-7xl`.
- **fix (CI, пре-существующий баг, пойман пользователем при пуше)** — `+layout.svelte`'s locale-switcher (`resolve(localizeHref(...) as Pathname)`) не тайпчекался — убран лишний `resolve()`-вызов для динамического пути, оставлен только для статических ссылок.
- **fix (данные, найден по репорту заказчика после редактирования курса)** — `CourseService.applyAgeGroupAndTeacher()` только *устанавливал* `teacher`/`ageGroup`, если `dto` содержал не-`null` id, но никогда не снимал связь при `null` — админ-форма всегда шлёт весь текущий стейт (`PUT`, не `PATCH`), значит `null` после клика "Ändern" означает "снять привязку", а не "не менять". Ранее выбранный учитель молча возвращался при повторном открытии формы. Оба поля (`teacher`/`ageGroup`) сделаны authoritative на каждый update; кнопка переименована из общего "Löschen" в "Ändern" (клик не удаляет курс, а открывает поиск замены).
- **fix (найдено `architect-reviewer` при ревью null-clearing фикса выше)** — тот же баг ("только устанавливает, никогда не снимает" при `null`) подтверждён и в `WorkshopService.updateWorkshop()`'s `teacher`-полю — реально достижимо через UI (`admin/workshops/+page.svelte`'s дропдаун с опцией "—"). Не отложено на отдельный тикет — исправлено сразу тем же паттерном (`else { existing.setTeacher(null); }`), новый `WorkshopServiceTest.java` (не существовал раньше) с 2 тестами.
- **fix (i18n, найдено `architect-reviewer`)** — кнопка "Ändern" на месте старого "Löschen" была захардкожена по-немецки, а не через `m.*()` — добавлен ключ `admin_change` во все 3 локали (`de/en/uk.json`).
- **test** — `CourseServiceTest` (Mockito, 8 тестов — включая 2 новых регрессионных на очистку `teacher`/`ageGroup` через `null`), новый `WorkshopServiceTest` (2 теста, тот же регрессионный паттерн).
- **verify** — полный локальный прогон против реального Postgres в Docker (`bootRun`): регистрация → логин ADMIN → создание Course с учителем → публичный `GET /courses` (200) → `GET /age-groups` (401 без JWT / 200 с JWT). `npm run check`/`build` чисто, `./gradlew compileJava` чисто, полный `./gradlew test` (85 тестов, включая Testcontainers-интеграционные) — 0 failures/errors.
- **docs** — `LR-069`/`LR-075`/`LR-076`/`LR-078` закрыты → `archive.md`; `LR-070`/`LR-071`/`LR-077` разблокированы; новый `LR-085` (recurrence-поля на форме `Group` для Course-групп, отдельно от Course-формы). **Подтверждено заказчиком:** `LR-ADR-023` остаётся как есть — Course не получает полей расписания, несмотря на первоначальную формулировку запроса ("интервал дат") — это Workshop-паттерн, для Course решён иначе.

## 2026-08-09 — docs: LR-068 закрыт — Roundtable #8 + `LR-ADR-023` (модель расписания `Course`) + 4 новых тикета

### Область (`docs/decision-history/roundtable-log.md`, `docs/architecture/{decisions.md,erm.drawio}`, `docs/tickets/{tickets.md,archive.md}`)

- **docs (Roundtable #8)** — панель Fowler/Vernon/McGrane/Long по
  вопросу, явно отложенному `LR-ADR-021`: как `Course` хранит
  регулярность занятий. Итог: `Course` остаётся чисто описательной
  сущностью без полей расписания; существующая `Group` (тот же
  прецедент, что `Workshop→Group`, `LR-015`) получает `courseId` +
  правило повторения; существующая `Session` (`LR-067`/`LR-ADR-022`)
  переиспользуется без изменения структуры для материализации занятий.
- **review** — `architect-reviewer` **до** формализации ADR, не после:
  подтвердил факты о коде (`Group.java`/`Enrollment.java`/
  `SessionService.java` реально в заявленном состоянии), нашёл 5
  реальных пробелов — все закрыты правкой протокола/ADR до принятия, не
  отложены: (1) пробел `Enrollment.workshop` шире, чем два
  nullable-поля — затрагивает `EnrollmentService.enroll` (статус
  выводится из `workshop.getPrice()`, у `Course` цены нет),
  `EnrollmentController`'s route, `Order`, и `uk_user_workshop` (не
  защищает от дублей после nullable — Postgres не считает `NULL`
  дубликатом); (2) `replaceSessionsForGroup` (`LR-067`) —
  деструктивный `clear()`+re-add, при масштабе Course (~150
  занятий/курс) реально стирает ручные правки при регенерации правила —
  принят trade-off (регенерация только при изменении полей правила, не
  на каждый save) + обязательное предупреждение в будущем admin UI;
  (3) McGrane-флаги не покрывают "только для взрослых"/"без
  сертификата" из ZFU-брифа — осознанно оставлено свободным текстом;
  (4) уточнена семантика `Group.startDateTime`/`endDateTime` для
  Course-группы (диапазон генерации, `endDateTime` обязателен);
  (5) mutual-exclusivity `workshopId`/`courseId` нигде не enforced —
  зафиксировано explicit-guard требованием в `LR-083`.
- **docs (`LR-ADR-023`)** — формализует итог Roundtable #8 со всеми
  правками `architect-reviewer`, закрывает вопрос, отложенный
  `LR-ADR-021`.
- **docs (`erm.drawio`)** — снята пометка "TBD" на `Course`, добавлена
  связь `Group↔Course` (`courseId`, nullable, та же визуальная
  конвенция "структура решена, не реализована"). XML провалидирован.
- **docs (tickets)** — `LR-068` закрыт → `archive.md`; `LR-065`'s
  прогресс-чеклист обновлён; `LR-069` переформулирован под финальную
  модель (явно "без полей расписания", `teacherId`/`venueId` убраны из
  скоупа — атрибуты `Group`, не `Course`); `LR-075` разблокирован от
  `LR-068`; созданы `LR-081` (расширение `Group`), `LR-082`
  (`SessionService`-генерация + change-guard), `LR-083`
  (mutual-exclusivity guard), `LR-084` (расширенный
  `Enrollment`/`EnrollmentService`/`EnrollmentController`/`Order`-фикс).
- **verify** — стрей-символьный скан (`grep -nP
  '[\x{FFFD}\x{00AD}\x{4E00}-\x{9FFF}\x{3040}-\x{39FF}]'`) на всех
  изменённых файлах — чисто.

## 2026-08-09 — feat: LR-067 закрыт — сущность `Session` (мульти-день расписание `Group`)

### Область (`backend/src/main/{resources/db/migration/V5__add_group_sessions.sql,java/com/be/{domain/entity/{Session,Group}.java,domain/repository/SessionRepository.java,service/SessionService.java}}`, `backend/src/test/java/com/be/{service/SessionServiceTest.java,SessionIntegrationTest.java}` (new), `docs/tickets/{tickets.md,archive.md}`)

- **feat (LR-067/LR-ADR-022)** — новая сущность `Session`
  (`group_sessions`, дочерняя `Group`) для мульти-дневных воркшопов: одна
  запись/один список участников на весь `Group`, несколько `Session`-строк
  под ним со своим `start`/`end`/`venue` на каждый день.
  `workshop_groups`'s собственные `start_date_time`/`end_date_time`/
  `venue_id` не тронуты — остаются значением "первого/единственного дня".
  `SessionRepository`, `SessionService` (`replaceSessionsForGroup` —
  рассчитан под форму LR-074, весь список дней пересылается целиком,
  не инкрементально).
- **test** — `SessionServiceTest` (Mockito) + **`SessionIntegrationTest`
  (новый, real-DB через Testcontainers)** — добавлен по находке
  `architect-reviewer`: Mockito-тест не мог доказать, что второй вызов
  `replaceSessionsForGroup` реально удаляет старые строки из БД
  (Hibernate `orphanRemoval`), а не просто отвязывает их в Java-коллекции
  — новый тест подтверждает по факту (прямой JDBC `count(*)` + поиск по
  старым id → пусто).
- **review** — `architect-reviewer`: approve with changes, единственная
  should-fix находка закрыта до мержа, не отложена на потом.
- **verify** — `./gradlew test` зелёный, включая новый integration-тест
  против реального Postgres.
- **docs** — `LR-067` закрыт, перенесён в `archive.md`; `LR-065`'s
  прогресс-чеклист обновлён.

## 2026-08-09 — docs: сверка устаревшей `lr-erm-2026-07.drawio` — найдены 2 пропущенные сущности

### Область (`docs/architecture/erm.drawio`, `docs/tickets/tickets.md`)

По запросу заказчика — проверена промежуточная ERM
`lr-erm-2026-07.drawio` (приоритет-раскраска волн после Roundtable #1,
2026-07-21) на предмет ценного, что не попало в основную `erm.drawio`.
Сама раскраска по волнам устарела (все три волны закрыты) — но найдены
и подтверждены по реальному коду (`Enrollment.java`, `WorkshopFile.java`)
**две сущности, отсутствующие в основной ERM**: `Enrollment`
(`User`+`Workshop` обязательные FK, `Group` — nullable) и
`WorkshopFile` (`Workshop`, `optional=false`). Обе добавлены в
`erm.drawio`.

Побочная находка при проверке: `File` и `WorkshopFile` — похоже,
дублирующие сущности (обе полностью реализованы, обе `@ManyToOne
Workshop`, но `Workshop.files`-коллекция смотрит только на
`WorkshopFile` — `File` физически "сирота" со стороны Workshop).
Отмечено прямо в ERM (текстовая заметка) + новый тикет **LR-080**.

## 2026-08-09 — docs: Эпик "Курсы" — ERM-коррекция (Course/Session) + 14 тикетов + 2 ADR

### Область (`docs/architecture/{erm.drawio,decisions.md,IMPLEMENTATION-PROTOCOL-2026-07.md}`, `docs/tickets/{tickets.md,archive.md}`)

Прямой срочный запрос заказчика (Olena) — страница "Курсы" в админке,
юридически связано с `docs/compliance/tlab29-zfu-compliance-brief.md`
(ZFU/FernUSG-заявка для онлайн-курса "Theaterlabor"). Прочитан бриф
полностью — задача оказалась шире, чем просто новая CRUD-страница:
затрагивает публичный контент, AGB, и требует юридически консистентных
формулировок между сайтом и перепиской с госорганом.

- **fix (архитектурная коррекция, подтверждена по коду)** — заказчик
  указал, что на стадии проектирования `Course` и `Workshop` были
  ошибочно слиты в один ERM-узел ("Workshop/Course"). Прочитаны реальные
  entity-классы (`Workshop.java`, `Group.java`, `Teacher.java`,
  `Performance.java`) — подтверждено: `Workshop.java` не имеет понятия
  периодичности расписания вообще, `Workshop.teacher` физически
  ссылается на `User`, а не `Teacher` (в отличие от корректного
  `Group.teacher`) — реальная находка, не гипотеза.
- **docs (ERM)** — `erm.drawio`: узел переименован в "Workshop", добавлены
  `Course`/`Session`, связи `courseId` (Workshop/Performance → Course,
  nullable) и `Group → Session`. XML провалидирован.
- **docs (2 новых ADR)** — `LR-ADR-021` (Course — отдельная сущность,
  с таблицей сравнения Workshop/Course по длительности/периодичности/
  формату) и `LR-ADR-022` (`Session` — новая дочерняя `Group` сущность
  для мульти-дня, не отдельный `Group` на день — аргумент через
  `Group.enrollments`: участник регистрируется один раз на весь
  многодневный воркшоп).
- **docs (2 других документа)** — `IMPLEMENTATION-PROTOCOL-2026-07.md`
  помечен историческим (все 3 волны закрыты, не редактируется задним
  числом); `PROJECT_INDEX.md` §4 сознательно не тронут — уже в очереди
  на полную пересборку через `LR-039`, точечная правка дублировала бы
  системную работу.
- **docs (14 новых тикетов, `LR-065`/`LR-067`..`LR-079`)** — полный эпик
  от backend-сущностей до публичного контента: `Session`-backend
  (`LR-067`) → **круглый стол по модели расписания `Course`** (`LR-068`,
  гейтит `LR-069`/`LR-075` — несколько технически валидных подходов,
  RRULE/день-недели-массив/материализованные Session, нужно осознанное
  решение) → `Course`-backend (`LR-069`) → nullable FK на Workshop/
  Performance (`LR-070`/`LR-071`) → фикс `Workshop.teacher`→`Teacher`
  (`LR-072`, с явным требованием живой проверки прод-данных перед
  миграцией) → admin-страница Teachers (`LR-073`) → форма Workshop
  мульти-день (`LR-074`) → форма Course (`LR-075`) → публичный каталог
  Course (`LR-076`) → ZFU-комплаенс контент Theaterlabor (`LR-077`,
  публикация только после согласования точного текста заказчиком) →
  AGB-правки (`LR-078`, три новые секции, текст продиктован брифом
  дословно) → форма согласия на съёмку Aufführungen (`LR-079`, backlog,
  "резерв на будущее" по формулировке брифа, не срочно).
- **docs (бухгалтерия)** — `LR-066` (сама ERM/ADR-коррекция) закрыт
  сразу же, перенесён в `archive.md`. Мастер-тикет `LR-065` трекает
  весь эпик.

## 2026-08-09 — security: LR-034 закрыт — живая проверка логирования на прод-поде пройдена

### Область (`docs/tickets/{tickets.md,archive.md}`)

- **security (LR-034, закрыт)** — `kubectl logs` на реальном
  `lr-backend` после деплоя: `0` DEBUG-строк в последних 200,
  все строки в выборке `INFO`/`WARN`, реальный запрос к API + повторная
  проверка логов — пусто на `DEBUG`/`security`/`web`. Побочно живьём
  подтверждена и находка `LR-064` (`Hibernate: select ...` виден в логе
  безусловно). Перенесён в `archive.md`.
- **fix (собственная бухгалтерская ошибка, найдена и исправлена
  сразу)** — при закрытии `LR-033` (предыдущая запись) новая версия
  `LR-034` (с "Сделано"/`architect-reviewer`) была по невнимательности
  дописана в конец файла через `Edit`, а не заменила существующую
  запись LR-034 на её месте (созданную ещё в первой ретроспективе,
  2026-08-08) — короткое время в `tickets.md` физически было два блока
  `## LR-034` одновременно, старый (без апдейтов) и новый. Найдено при
  попытке закрыть тикет (искал по заголовку — grep вернул два
  совпадения вместо одного), исправлено немедленно: устаревшая копия
  удалена, актуальная перенесена в архив. Полная проверка на дубли по
  всему `tickets.md`/`archive.md` (`grep ... | sort | uniq -d`) — чисто.

## 2026-08-08 — security: LR-034 — DEBUG-логирование в проде понижено (код готов, живая проверка — нет)

### Область (`backend/src/main/resources/application.properties`, `backend/README.md`, `backend/src/test/java/com/be/config/LoggingLevelsTest.java` (new), `docs/tickets/tickets.md`)

- **security (LR-034, HIGH)** — `logging.level.{org.springframework.security,.web,com.be}`
  переведены с постоянного `DEBUG` на `${LOG_LEVEL_SECURITY:WARN}`/
  `${LOG_LEVEL_WEB:WARN}`/`${LOG_LEVEL_APP:INFO}` — тот же env-var-override
  паттерн, что и LR-033. DEBUG остаётся доступен для разовой диагностики
  через явный env var, задокументировано в `backend/README.md`.
- **test** — `LoggingLevelsTest.java` (новый) — резолвит реальный
  `application.properties` через `StandardEnvironment`+
  `ResourcePropertySource` (не полный Spring-контекст — logging-уровни
  не наша логика, framework сам их обрабатывает, тестируем именно то,
  что наше — сам факт отсутствия хардкода DEBUG в файле).
  `architect-reviewer`: approve as-is — эмпирически прогнал тест,
  сверил `.debug`-вызовы в `GlobalExceptionHandler`/`AuthService`: все
  дублируются Micrometer-метриками `LR-031` Фазы 1, реального
  операционного слепого пятна нет.
- **security (побочная находка, LR-064, новый тикет)** —
  `spring.jpa.show-sql=true` логирует SQL безусловно, не через
  `logging.level.*` — не затронуто этим фиксом, заведено отдельно, не
  расширяя скоуп.
- **Не закрыто до конца** — `tickets.md`'s LR-034 остаётся `Open`:
  нужна живая проверка реального уровня логирования на прод-поде после
  деплоя (`kubectl logs`) — не может быть сделано из этой сессии.

## 2026-08-08 — security: LR-033 закрыт — живая проверка CORS на прод-поде пройдена

### Область (`docs/tickets/{tickets.md,archive.md}`)

- **security (LR-033, закрыт)** — 4 сценария живой проверки на реальном
  `api.tlab29.com` (с mgmt-core, прямой `curl`): `tlab29.com` получает
  `access-control-allow-origin`/`-credentials` на обычном запросе и
  preflight; `localhost:3000` получает `403` без единого CORS-заголовка
  на обоих. Регрессия (dev-origins в прод-allow-листе) подтверждена
  закрытой реальным ответом пода, не фактом смерженного кода. Перенесён
  в `archive.md`.

## 2026-08-08 — security: LR-033 — CORS dev-origins убраны из прод-конфига (код готов, живая проверка — нет)

### Область (`backend/src/main/{resources/application.properties,java/com/be/config/CorsProperties.java}`, `backend/README.md`, `backend/src/test/java/com/be/config/CorsPropertiesTest.java` (new))

- **security (LR-033, HIGH)** — `application.properties`'s
  `cors.allowed-origins` (единственный CORS-конфиг-файл, который реально
  деплоится в прод) содержал одновременно прод-домены и три localhost
  dev-origins, вместе с `cors.allow-credentials=true`. Дефолт теперь
  прод-only (`${CORS_ALLOWED_ORIGINS:https://tlab29.com,...}`),
  dev-origins доступны только через явный env var, задокументированный в
  `backend/README.md`. `CorsProperties.java`'s собственный Java-дефолт
  тоже поправлен (defense in depth, на случай отсутствия properties-
  строки). `http://tlab29.com`/`https://www.tlab29.com` сознательно
  оставлены (нет соответствующего Ingress host rule — не живая
  поверхность атаки, не расширять скоуп security-фикса).
- **test** — `CorsPropertiesTest.java` (новый) — грузит РЕАЛЬНЫЙ
  `application.properties` (не дублирует значение литералом),
  подтверждает: дефолт не содержит `localhost`, env-var override
  реально заменяет список. `architect-reviewer` эмпирически проверил
  диф (откатил файл к до-фикс версии, подтвердил, что тест реально
  ловит регрессию — не просто звучит правдоподобно) и заодно закрыл
  старый висящий вопрос из CHANGELOG.md 2026-07-21 ("CORS настроен
  дважды независимо, не проверено рантаймом") — не дважды,
  `SecurityConfig`'s `.cors(Customizer.withDefaults())` просто
  делегирует в `WebMvcConfig`'s MVC-конфиг, единственный источник
  правды.
- **verify** — `./gradlew test` зелёный (полный прогон + изолированный
  прогон нового теста).
- **Не закрыто до конца** — `docs/tickets/tickets.md`'s LR-033 остаётся
  `Open`: нужна живая проверка реального `Access-Control-Allow-Origin`
  заголовка на прод-поде после деплоя (`architect-reviewer`'s находка) —
  не может быть сделано из этой сессии, тот же класс "код смержен ≠
  проверено на живом проде", что уже стоил времени с Litestream/
  `numi-nat.service`.

## 2026-08-08 — docs: Roundtable #6/#7 (Frontend UI/UX, кросс-проектный мониторинг) + 11 новых тикетов

### Область (`docs/decision-history/roundtable-log.md`, `docs/tickets/{tickets.md,archive.md}`)

По прямому запросу заказчика — проведены оба круглых стола, заведённых
предыдущей ретроспективой (`LR-048`/`LR-049`), в режиме "best practices
каждого эксперта, спроецированные на актуальное состояние проекта", не
абстрактное обсуждение.

- **docs (Roundtable #6 — Frontend UI/UX)** — панель Wroblewski
  (mobile-first)/Nielsen (usability heuristics)/Cooper (goal-directed
  design)/Harris (создатель Svelte/SvelteKit)/Soueidan (a11y).
  Заземлён на свежей инвентаризации реального кода `frontend-svelte`
  (route tree, навигация, admin/личный дашборд, UI-примитивы, токены) —
  Explore-агентом, не по памяти/ADR.
- **docs (Roundtable #7 — Мониторинг)** — панель Majors (observability)/
  Volz (со-автор Prometheus)/Fong-Jones (SRE/alerting)/Gregg (USE-метод)/
  Long (Spring/Micrometer, продолжение с Roundtable #4/#5). Явно
  зафиксировано: архитектура единой ноды VM600 уже решена на Roundtable
  #5 — этот стол закрывает то, что #5 не покрывал (схема именования
  метрик, канал алертинга, self-monitoring VM600).
- **review** — оба протокола отправлены `architect-reviewer` на проверку
  архитектурной релевантности **до** финализации и заведения тикетов
  (по прямому требованию заказчика). Фактических ошибок не найдено ни в
  одном факте "исходных данных" (все проверены против реального кода).
  Найдено и исправлено: (а) Roundtable #6's пункт про `load()`
  изначально недооценивал реальную цену вопроса — `hooks.server.ts`/
  `load()` не исполняются на проде под текущим `adapter-static`
  (`vite.config.ts`/`nginx.conf`), значит это вопрос пересмотра всего
  `LR-002` (смена адаптера, переработка Docker/nginx/k3s), не "узкое
  исследование" — переформулировано; (б) Roundtable #7 частично
  переизобретал уже решённую на #5 архитектуру VM600, не цитируя её —
  добавлена явная ссылка; (в) не учтён VM300's dual-role риск
  (numi-хост + k3s-worker для остановленного workout-evo — тот же класс
  риска, что уже отмечен для `gateway-core`) — добавлено; (г)
  Telegram-бот для алертинга не был явно отделён от уже упомянутого в
  `LR-ADR-016` клиент-facing бота для напоминаний — уточнено (разные
  токены/аудитории); (д) вопрос про severity-бейджи и WCAG 1.4.1 был
  оставлен открытым, хотя код уже даёт ответ (текстовая подпись рядом с
  цветом) — закрыт напрямую, не тикетится.
- **docs (тикеты)** — 11 новых: `LR-053`..`LR-060` (единая навигация,
  секционные loading-состояния, пересмотр `LR-002`, admin-дашборд IA,
  мобильный Payments, точечный a11y-аудит, вынос `Table`/`Badge`,
  проверка номенклатуры меню) из Roundtable #6; `LR-061`..`LR-063`
  (общий контракт именования метрик, канал уведомлений — Telegram-бот
  по умолчанию, требования к self-monitoring/размещению VM600) из
  Roundtable #7.
- **docs (бухгалтерия)** — `LR-048`/`LR-049` закрыты и перенесены в
  `archive.md` со ссылкой на протоколы и результирующие тикеты.
  `LR-031`'s "Не начинать Фазу 3 без" обновлено — зависимость от
  результата LR-049 снята содержательно (результат есть), единственный
  оставшийся гейт — реальный провижининг VM600 (`INFRA-008`).

## 2026-08-08 — docs: полная ретроспектива бэклога/архива против архитектурных решений и находок аудита

### Область (`docs/tickets/{tickets.md,archive.md}`, `docs/security/audit-2026-08-06.md`, `docs/infra/INFRA-LR.md`)

По прямому запросу заказчика — полный анализ `tickets.md`+`archive.md`
(31 открытый + 18 закрытых тикетов на момент старта) против
`docs/architecture/decisions.md` (25 ADR), `docs/security/
audit-2026-08-06.md` (1 CRITICAL/5 HIGH/11 MEDIUM/9 LOW), `docs/infra/
INFRA-LR.md`, `docs/context/PROJECT_INDEX.md`, `docs/decision-history/
roundtable-log.md`. Два параллельных агента для инвентаризации (не
раздувать собственный контекст сырым текстом `archive.md`, который по
правилу проекта нельзя читать целиком), синтез и все решения — вручную.

- **fix (бэкенд-гигиена бэклога)** — три реальные находки:
  1. **LR-004** был закрыт (`Статус: Closed 2026-07-23` в собственном
     теле тикета), но физически оставался в `tickets.md`, не перенесён
     в `archive.md`. Перенесено.
  2. **LR-032** был открыт, но физически лежал в `archive.md` (заведён
     inline внутри записи о закрытии LR-029, никогда не попал в активный
     бэклог). Перенесено.
  3. **LR-008** — ID использовался в комментарии кода
     (`GroupService.java`) и в теле LR-009 с самого начала, но не имел
     собственной записи ни в одном файле — отсюда разрыв нумерации
     LR-007→LR-009. Заведена retroactive-запись в `archive.md` (сам фикс
     — `startDateTime`/`endDateTime` не копировались при update — давно
     сделан и закрыт, 2026-07-23, только не задокументирован отдельно).
- **security** — сверка находок `audit-2026-08-06.md` против реально
  закрытых тикетов выявила, что **сам файл аудита содержал устаревшие
  статусы**: H1/H2/H3/M11 помечены "Open" в тексте, хотя фактически
  закрыты (LR-024/025/026/030). Файл-протокол НЕ отредактирован задним
  числом (точка-во-времени, сохраняет историческую целостность) — вместо
  этого добавлен раздел "Addendum 2026-08-08" с живой таблицей статусов
  и ссылками на тикеты.
- **security (новые тикеты, находки аудита без трекинга)** — четыре
  находки из `audit-2026-08-06.md` не имели вообще никакого тикета:
  **M5** (root-контейнеры, нет resource limits → `LR-036`), **M6**
  (CORS смешивает prod/dev origins в живом прод-конфиге → `LR-033`),
  **M9** (verbose DEBUG-логирование активно в проде → `LR-034`), **M10**
  (нет rate limiting нигде, прямое расхождение с уже принятым
  `LR-ADR-007` → `LR-035`). Плюс 7 LOW-находок без тикетов
  (`LR-040`..`LR-043`, birthDate-шифрование, Contract-шифрование,
  BCrypt/JWT-ключ/image-pinning гигиена, исполнение уже принятого
  `LR-ADR-012`).
- **docs (архитектурный/процессный долг без тикетов)** — из
  `PROJECT_INDEX.md`/`INFRA-LR.md`/`roundtable-log.md`: аудит
  `@Transactional`-границ, никогда не проводился (флаг ещё с Roundtable
  #1, 2026-07-20 → `LR-037`); возможная избыточность/конфликт
  `DatabaseFixConfig` теперь, когда есть Flyway (→ `LR-038`);
  `PROJECT_INDEX.md` заметно устарел относительно реального репозитория
  — всё ещё описывает фронтенд как статический HTML, не знает про
  Flyway (→ `LR-039`); плюс три инфра-гигиена тикета
  (`LR-044`/`LR-045`/`LR-046`/`LR-047` — `revisionHistoryLimit`,
  cloudflared origin doc, unused `caddy.service`, registry-token
  automation, `qemu-guest-agent`).
- **docs (роадмап-разрыв, замечен по пути)** — `LR-022`'s собственный
  прогресс-чеклист показывает п.6 (финальная верификация аудита) как
  "не начата, гейтится на закрытии LR-024..030/LR-021" — все эти тикеты
  уже закрыты, гейт снят, но никто не заметил, что п.6 готова к старту.
  Отмечено явно в новой секции "Спринты".
- **docs (сеть тикетов "круглый стол")** — по прямому запросу заказчика
  заведены `LR-048` (Frontend UI/UX — состав главной, меню, навигация,
  дашборды, мобильная версия) и `LR-049` (Мониторинг — единый подход в
  скоупе с numi/workout-evo, включая нерешённый до сих пор вопрос канала
  алертинга). `LR-031`'s "Не начинать Фазу 3 без" дополнено зависимостью
  от `LR-049`.
- **docs (приоритизация)** — новая секция "Спринты (приоритизация)" в
  `tickets.md` — не буквальные Scrum-спринты (в проекте такой практики
  нет), волны срочности: "Немедленно" (LR-003 бэкап — дедлайн прошёл;
  LR-033/LR-034 — активные прод-issue; LR-027/LR-028 — уже HIGH, застряли
  на блокере), "Ближайшее", "Плановое/бэклог", "Заблокировано",
  "Планирование" (два круглых стола).
- **docs** — `INFRA-LR.md`'s §8 checklist: `NetworkPolicy default-deny`
  пункт был устаревшим (`[ ]`, хотя закрыт вчера, `LR-031` Фаза 2) —
  исправлено на `[x]` со ссылкой на реальные тикеты/доказательства;
  остальные два пункта чек-листа получили ссылки на новые тикеты
  (`LR-044`/`LR-045`).
- **Итог по числам:** 14 → 31 тикет в `tickets.md` (17 новых: 15
  находок-без-тикетов + 2 круглых стола; LR-004 при этом ушёл в
  архив, LR-032 пришёл из архива — оба учтены), 17 → 18 в `archive.md`
  (+LR-004, +LR-008 retroactive, -LR-032 переехал в бэклог), 0 тикетов
  потеряно или удалено как "мусор" — при полной сверке не нашлось ни
  одного тикета, который стоило бы закрыть как избыточный/неактуальный
  (весь существующий бэклог оказался обоснованным).

## 2026-08-08 — infra: LR-031 Фаза 2 — `NetworkPolicy` применён и подтверждён на живом кластере (M4 закрыт)

### Область (`devops/helm/lr-app/values.yaml`, `docs/{runbooks/infra-fix-shutdown.md,security/{roadmap.md,ARCHITECTURE.md},context/KNOWN_ISSUES.md,tickets/tickets.md}`)

- **security (LR-031 Фаза 2, закрыта)** — `networkPolicy.enabled: true`
  закоммичен, манифесты из вчерашнего дня применены с mgmt-core.
  Закрывает audit-находку **M4**, подтверждено негативным тестом (под
  без нужных меток → `nc -zv lr-postgres 5432` → exit code 1), не
  только позитивными проверками (curl 200 через публичный путь, DNS
  резолвится).
- **incident (короткий, самоустранённый)** — первая попытка применения
  вызвала реальный простой сайта: `networkpolicy-baseline.yaml`
  (default-deny всего ingress) применён отдельным шагом ДО
  ingress-allow правил для backend/frontend — Traefik на несколько
  минут не мог достучаться ни до чего. Откачено (`kubectl delete
  networkpolicy -n lr-dev --all`), процедура исправлена (все нужные для
  доступности сайта правила — одним `kubectl apply` без человеческой
  паузы между "запретить всё" и "разрешить нужное"), повторное
  применение — чисто. Полный разбор — `docs/context/KNOWN_ISSUES.md`.
- **docs** — `docs/runbooks/infra-fix-shutdown.md`'s "LR-031 Phase 2"
  переписан под исправленную (batch) процедуру + добавлен пример
  явного чтения exit code вместо доверия `kubectl wait`/verbose-выводу
  `nc` (тоже найдено в процессе — `busybox nc -v` может ничего не
  печатать при неудачном подключении, `sh -c '...; echo $?'` даёт
  однозначный ответ). `docs/security/roadmap.md`'s "Статус" и
  `docs/tickets/tickets.md`'s LR-031 progress — Фаза 2 → `[x]`.
  `docs/security/ARCHITECTURE.md` §2.9 — статус обновлён с
  "authored, not applied" на "применено и подтверждено", с полными
  доказательствами.

## 2026-08-07 — infra: LR-031 Фаза 2 — сетевая изоляция (`NetworkPolicy`, код готов, не применён)

### Область (`devops/helm/lr-app/templates/{networkpolicy-baseline,networkpolicy-backend,networkpolicy-frontend,networkpolicy-postgres,networkpolicy-postgres-backup}.yaml (new)`, `devops/helm/lr-app/templates/postgres-backup-cronjob.yaml`, `devops/helm/lr-app/values.yaml`, `docs/{runbooks/infra-fix-shutdown.md,security/{roadmap.md,ARCHITECTURE.md},tickets/tickets.md}`)

- **infra (LR-031 Фаза 2, INFRA-tier)** — default-deny `NetworkPolicy`
  для namespace `lr-dev`, с explicit allow: DNS (namespace-wide),
  ingress только от Traefik (backend/frontend), backend↔postgres на
  5432, backend/backup-job → интернет (порты 443/587, исключая
  `10.0.0.0/8` — k3s pod/service CIDR + VM LAN одним исключением).
  Закрывает audit-находку **M4** ("Postgres достижим с любого пода,
  защита только на пароле") — но только после реального применения, не
  в момент написания манифеста.
- **fix** — `postgres-backup-cronjob.yaml`'s pod template получил
  `labels: app: lr-postgres-backup` (раньше не было вообще никаких
  меток) — без этого NetworkPolicy-селекторы не смогли бы отличить этот
  под от любого другого.
- **safety** — вся фича спрятана за `networkPolicy.enabled: false`
  (`values.yaml`), тот же паттерн и та же причина, что и у
  `postgresBackup.enabled`: `.gitlab-ci.yml` гонит `helm upgrade
  --install` на каждый пуш безусловно, живое сетевое поведение не
  должно меняться попутно с несвязанным тикетом.
- **verify** — `helm template`/`helm lint` локально, оба состояния
  (`networkPolicy.enabled` true/false, все 4 комбинации с
  `postgresBackup.enabled`) — рендерится корректно, 0 ресурсов при
  дефолтных `values.yaml`. **НЕ применялось на реальном кластере** — эта
  сессия не имела доступа к живому k3s.
- **review** — `architect-reviewer`: approve with changes. Нашёл
  реальный HIGH-пробел: изначальная процедура применения проверяла
  только позитивные пути (разрешённое работает), никогда — что запрет
  реально работает. Это критично именно здесь: голый flannel НЕ
  энфорсит `NetworkPolicy` сам по себе (нужен kube-router, не
  подтверждён для этого кластера) — без негативного теста все
  "позитивные" проверки прошли бы одинаково успешно и при полностью
  неработающей политике. Исправлено: добавлен шаг 0a (проверка, что
  netpol-контроллер вообще включён) и обязательный негативный тест
  (под без нужной метки НЕ должен достучаться до postgres) в
  `docs/runbooks/infra-fix-shutdown.md`. Также поймал, что
  `docs/security/ARCHITECTURE.md` не был обновлён вопреки собственному
  явному правилу файла — исправлено (§2.9 добавлен).
- **docs** — `docs/runbooks/infra-fix-shutdown.md`: новый раздел
  "LR-031 Phase 2" — пошаговая процедура применения с mgmt-core (шаг за
  шагом, не всем чартом разом), включая предусловие "свериться с
  реальными метками кластера, не с предположением из комментариев" и
  негативный тест. `docs/security/ARCHITECTURE.md` §2.9 (новый) +
  правки §2.8/§3.3. `docs/security/roadmap.md`'s "Статус" и
  `docs/tickets/tickets.md`'s LR-031 progress — Фаза 2 остаётся `[ ]`
  (честно, не закрыта до реального применения+негативного теста), с
  полным разбором того, что готово vs. что требует ручного шага.
- **Прошлый инцидент, почему особая осторожность** —
  `docs/infra/INFRA-LR.md` §6.3: старый `flannel-patch.yaml` в этом же
  Helm-чарте когда-то мутировал общий кластерный ConfigMap, вызвав
  каскад поломок по всему кластеру, не только LR. Новые манифесты
  ничего не трогают за пределами namespace `lr-dev` — не повторяют этот
  класс ошибки.

## 2026-08-07 — security: LR-031 Фаза 1 — observability-фундамент

### Область (`backend/{build.gradle,src/main/{resources/application.properties,java/com/be/{service/AuthService,web/handler/GlobalExceptionHandler}.java},src/test/java/com/be/{ApiSurfaceAllowlistTest(new),service/AuthServiceTest,web/handler/GlobalExceptionHandlerTest,web/controller/{UserControllerTest,PaymentControllerTest}}.java}`, `devops/helm/lr-app/{values.yaml,templates/backend-deployment.yaml}`, `.gitlab-ci.yml`, `docs/{security/roadmap.md,tickets/tickets.md}`)

- **feat (LR-031 Фаза 1, HIGH)** — `micrometer-registry-prometheus`
  добавлен в `backend/build.gradle`;
  `management.endpoints.web.exposure.include` дополнен `prometheus` —
  `/actuator/prometheus` в формате, который `otelcol-contrib` уже умеет
  скрейпить (проверенный на numi паттерн).
- **feat (LR-031 Фаза 1)** — security-метрики в choke-point'ах:
  `AuthService.authenticate()`/`register()` → `auth.login.failure{reason}`
  (4 фиксированных значения тега: `unknown_email`/`locked`/
  `bad_password`/`email_not_verified` — не сырой ввод, кардинальность
  ограничена по построению), `auth.login.success`, `auth.register`.
  `GlobalExceptionHandler.handleAccessDenied()`/`handleNoResourceFound()`
  → `authz.denied`, `http.unmapped_path` — **осознанно без тегов
  `path`/`role`**, в отличие от исходной спецификации в
  `docs/security/roadmap.md` (raw-path/role как тег — известный
  источник неограниченной кардинальности; см. комментарий в коде и
  обновлённый `roadmap.md` "Статус" для полного обоснования).
  `http.unmapped_path` отмечен в роадмапе как "ретроспективно самый
  ценный" — существуй он на момент LR-023, exploit-трафик
  (`GET/PATCH /users`, `/participants` вне `/api/v1/**`) был бы виден с
  первого дня, а не спустя ~9.5 месяцев.
- **test (LR-031 Фаза 1, новый файл)** — `ApiSurfaceAllowlistTest` —
  "правильный" CI-guard взамен точечного grep по имени зависимости,
  **двухслойный** (не как в исходной спецификации роадмапа):
  1. путь-based проверка на `RequestMappingHandlerMapping` — каждый
     смонтированный путь либо под `/api/v1/**`, либо в explicit
     allow-листе (`/error`);
  2. **добавлено по находке `architect-reviewer`** — enumeration всех
     бинов `HandlerMapping` по имени против explicit allow-листа. Причина:
     проверено напрямую по исходникам `spring-data-rest-webmvc` — свои
     auto-exposed пути (LR-023) регистрирует внутри package-private
     `DelegatingHandlerMapping`, обёртывающего `RepositoryRestHandlerMapping`/
     `BasePathAwareHandlerMapping` как обычные `new`'ые объекты, никогда
     не публикуемые Spring-бинами сами по себе — **проверка #1 сама по
     себе НЕ поймала бы LR-023**, если бы существовала на момент
     инцидента. Только комбинация обоих слоёв ловит фактический механизм.
- **fix (регрессия, найдена своим же изменением)** — конструктор
  `GlobalExceptionHandler` теперь требует `MeterRegistry` → оба
  существующих `@WebMvcTest`-среза (`UserControllerTest`,
  `PaymentControllerTest`) не поднимали контекст
  (`NoSuchBeanDefinitionException` — `@WebMvcTest` не автоконфигурирует
  Micrometer). Исправлено `@MockitoBean(answers = Answers.RETURNS_MOCKS)`
  на оба — `RETURNS_DEFAULTS` дал бы `null` на `.counter(...)` →
  `NullPointerException` на цепочечном `.increment()`.
- **infra (LR-031 Фаза 1, предусловие Фазы 2)** —
  `management.server.port=${MANAGEMENT_SERVER_PORT:9090}` — actuator
  переезжает с публичного бизнес-порта на отдельный (образец numi's
  `9090`). Обновлены в том же дифе, чтобы не сломать деплой:
  `backend-deployment.yaml`'s liveness/readinessProbe (были захардкожены
  на `8080`) + новый `containerPort`/env var; `.gitlab-ci.yml`'s
  smoke-test (был `curl .../actuator/health`, теперь реальный публичный
  `/api/v1/workshops` — де-факто лучшая проверка). `backend-service.yaml`
  сознательно НЕ тронут — управляющий порт остаётся недоступен через
  Service/Ingress уже сейчас, полная сетевая изоляция (`NetworkPolicy`)
  — предмет Фазы 2, не этой.
- **review** — `architect-reviewer`: approve with changes. Нашёл реальный
  HIGH-пробел в первой версии `ApiSurfaceAllowlistTest` (см. выше) —
  исправлено до закрытия, перепройдено ревью не потребовалось (фикс
  строго расширяет покрытие, не меняет уже одобренную часть). Также
  поймал вводящий в заблуждение комментарий про `/actuator/**` в
  path-based тесте (actuator использует `WebMvcEndpointHandlerMapping`,
  sibling-класс, а не subtype `RequestMappingHandlerMapping` —
  исходное предположение спецификации роадмапа было неверным) —
  скорректировано в коде и в `roadmap.md`.
- **docs** — `docs/security/roadmap.md`'s "Статус": Фаза 0 (была
  пропущена при предыдущем закрытии) и Фаза 1 отмечены `[x]` с полным
  разбором всех отклонений от исходной спецификации. `tickets.md`'s
  LR-031 progress — Фаза 1 отмечена `[x]`.
- **verify** — `./gradlew test` зелёный (полный прогон + изолированный
  прогон `ApiSurfaceAllowlistTest`), `FIELD_ENCRYPTION_KEY`/`JWT_SECRET`
  сгенерированы заново на прогон, `JAVA_HOME` — `temurin-21.0.11`.

## 2026-08-07 — security: LR-031 Фаза 0 — 6 тикетов закрыты (LR-021/024/025/026/029/030)

### Область (`backend/src/main/java/com/be/{web/controller/{WorkshopController,GroupController,EnrollmentController,OrderController},service/{TeacherService,GroupService,AuthService},domain/repository/TeacherRepository,web/handler/GlobalExceptionHandler,web/dto/request/{GroupCreateDTO(new),PaymentRequestDTO,OrderRequestDTO}}.java`, `frontend-svelte/src/{lib/api.ts,routes/admin/groups/+page.svelte}`, новые тесты `OrderOwnershipTest`/`GroupServiceTest`/`GlobalExceptionHandlerTest`/`AuthServiceTest`)

- **security (LR-024, HIGH)** — teacher IDOR: `EnrollmentController.
  participantsForGroup`/`WorkshopController.byTeacher`/`GroupController.
  getGroupsByTeacher` (последний — вообще без `@PreAuthorize`) проверяли
  только роль, не владение. Новый `TeacherRepository.findByEmail()` +
  `TeacherService.resolveTeacherIdForUser()` резолвит User→Teacher по
  email (единственная существующая связь, раньше матчилась только на
  фронте) — все три эндпоинта теперь 403'ят при чужом `teacherId`/`groupId`.
- **security (LR-025, HIGH)** — `OrderController.getById()` читал
  несуществующий JWT-claim `"roles"` (реальный — `"role"`) →
  `NullPointerException` на каждом вызове → проверка владения никогда не
  исполнялась. Исправлено на `JwtAuthUtils.hasRole()`. Новый реальный
  E2E-тест `OrderOwnershipTest`.
- **security (LR-026, MED)** — account-lockout кидал `RuntimeException`
  → 500, отличимый от bad-credentials' 401 — content-based oracle
  существования аккаунта. Теперь та же `BadCredentialsException`.
- **security (LR-029, MED, частично)** — `GlobalExceptionHandler`'s
  catch-all больше не эхо `ex.getMessage()` клиенту (закрывает реальную
  утечку для 59+ мест `RuntimeException("X not found")` разом). Полная
  замена на типизированное 404-исключение вынесена в новый `LR-032`
  (LOW, отдельный проход по 18 сервисам).
- **security (LR-030, LOW-MED)** — `GroupController.createGroup`
  биндил raw JPA entity без allow-листа полей (риск: чужой `enrollments`
  ID, произвольный `capacityLeft`). Новый `GroupCreateDTO` (тот же
  паттерн, что уже применён для `WorkshopCreateDTO`) — структурно не
  может нести эти поля. Фронтенд: `GroupCreateRequestDTO` +
  `toCreateRequest()`, `updateGroup`/PUT не тронут.
- **feat (LR-021, MED)** — `@DecimalMin`/`@NotNull` на amount,
  `@Pattern` на currency, `@Size` по реальным `@Column` — на
  `PaymentRequestDTO`/`OrderRequestDTO`. Осознанно не сделано: `status`
  → enum (нет установленного домена значений в коде — продуктовое
  решение, не гадать), `orderNumber` → `@NotBlank` (DTO общий с
  `update()`, который его не читает).
- **review** — `architect-reviewer`: один пакетный обзор на все 6
  тикетов сразу, approve with changes (единственный "must-fix" —
  окружение самого ревьюера, не код; независимо подтвердил каждый
  открытый вопрос по каждому тикету).
- **docs** — `backend/README.md`: добавлена секция про обязательные
  `FIELD_ENCRYPTION_KEY`/`JWT_SECRET` для локального `./gradlew test`
  (найдено ревьюером — без документации любой свежий клон падает по
  той же причине, не относящейся ни к одному конкретному тикету).
- **verify** — `./gradlew test` зелёный, 61 тест (перепрогнан `--rerun`
  для подтверждения).
- LR-021/024/025/026/029(частично)/030 закрыты и заархивированы. Новый
  `LR-032` заведён (follow-up). LR-027/LR-028 остаются open — блокируются
  на живом инфра-доступе (ручной `psql`, GitLab CI/CD Variables).

## 2026-08-06 — docs: LR-031 — роадмап "структура безопасности + мониторинг" (Roundtable #4/#5)

### Область (`docs/decision-history/roundtable-log.md`, `docs/security/roadmap.md` (new), `docs/tickets/tickets.md`)

- **docs** — по запросу заказчика собран круглый стол (тот же состав,
  что проектировал LR-022: Moussouris, Mackey, Wysopal, Winch, Long) на
  две задачи: (1) ретроспектива LR-023 — почему дыра прожила ~9.5
  месяцев (зависимость попала в `build.gradle` в один из первых
  коммитов бэкенда, 2025-10-21, и не была названа вслух ни разу за всю
  историю проекта, включая архитектурный Roundtable #1 с явными
  security-местами — записано как **Roundtable #4**); (2) не точечный
  CI-guard, а полный роадмап структуры безопасности + интеграции с
  централизованным мониторингом на VM600 (`numi/infra/infra-backlog.md`
  INFRA-008 — записано как **Roundtable #5**).
- **docs** — `docs/security/roadmap.md`: 4 фазы — observability-
  фундамент в приложении (Micrometer/Prometheus, security-метрики через
  уже существующие choke-point'ы `GlobalExceptionHandler`/`AuthService`,
  обобщённый CI-guard на allow-лист HTTP-путей вместо точечного grep'а
  по имени зависимости), сетевая изоляция (`NetworkPolicy` в `lr-dev` —
  закрывает заодно M4 и уже известный пункт `INFRA-LR.md` §8; отдельный
  management-порт — закрывает заодно M7), подключение к VM600 (гейтится
  на INFRA-008, не в скоупе одного LR), институционализация
  (периодический повтор аудита, SBOM в CI).
- **docs** — заведён `LR-031` (master-тикет роадмапа, HIGH,
  `docs/tickets/tickets.md`), трекает прогресс по фазам, ссылается на
  Roundtable #5 и `roadmap.md` вместо дублирования обоснования.
- Порядок в `roundtable-log.md` также исправлен (Roundtable #4 ранее
  случайно попал перед Roundtable #1 в результате предыдущей правки —
  восстановлена хронология, добавлена явная сноска про пропуск в
  нумерации #2/#3, которые синтезировались напрямую в `tickets.md`/
  `CHANGELOG.md`, не в этот лог).

## 2026-08-06 — docs: LR-022 фазы 1-5 завершены — протокол `docs/security/audit-2026-08-06.md`, LR-024..LR-030 заведены

### Область (`docs/security/audit-2026-08-06.md` (new), `docs/tickets/tickets.md`)

- **docs** — три фоновых агента (backend/infra/frontend), "круглый стол"
  экспертов (Moussouris, Mackey, Wysopal, Winch, Long) + методологии
  (SbD, Zero Trust, NIST SSDF, OWASP SbD, STRIDE), сверка против
  `docs/architecture/decisions.md`/`IMPLEMENTATION-PROTOCOL-2026-07.md`/
  `PROJECT_INDEX.md`. Итог: 1 CRITICAL (LR-023, уже закрыт вне очереди),
  5 HIGH, 6 MEDIUM, 9 LOW/INFORMATIONAL.
- **прямой ответ на вопрос заказчика** — прямого доступа к БД из
  фронтенда не обнаружено (подтверждено grep'ом на все известные
  DB-клиентские библиотеки и сырой SQL, фронтенд общается с бэкендом
  исключительно через `fetch()` к `/api/v1/**`).
- **docs** — заведены LR-024 (teacher IDOR — контакты детей из чужих
  групп, HIGH), LR-025 (`OrderController` NPE — проверка владения
  никогда не исполняется, HIGH), LR-026 (account-lockout content oracle,
  MED — тот же класс, что уже закрытый LR-014), LR-027 (scoped DB-роль
  вместо суперюзера Postgres, HIGH, расширяет LR-003 п.6), LR-028
  (scoped CI kubeconfig вместо вероятного cluster-admin, HIGH, требует
  сначала живой проверки GitLab CI/CD Variables), LR-029
  (`GlobalExceptionHandler` системная утечка `ex.getMessage()` +
  entity-existence oracle, MED), LR-030 (`GroupController` mass
  assignment через raw-entity binding, LOW-MED).
- LR-022 (сам аудит): П.1-5 готовы. П.6 (финальная верификация) —
  гейтится на закрытии LR-024..LR-030 и уже открытого LR-021, не начата.

## 2026-08-06 — security: LR-023 (CRITICAL) — Spring Data REST давал полный обход авторизации + само-эскалацию до ADMIN

### Область (`backend/build.gradle`, `backend/src/main/java/com/be/web/handler/GlobalExceptionHandler.java`, `backend/src/test/java/com/be/SpringDataRestExposureTest.java` (new))

- **security (CRITICAL)** — `spring-boot-starter-data-rest` был на classpath
  без единого механизма отключения авто-экспозиции репозиториев.
  Подтверждено вживую (не только статически): свежезарегистрированный
  обычный `USER` через `GET /users` получал полный дамп таблицы (bcrypt-
  хэши, токены верификации, расшифрованные ФИО), а через `PATCH /users/1
  {"role":"ADMIN"}` реально повышал себя до администратора — в обход
  всех `@PreAuthorize` во всех контроллерах. Найдено фоновым агентом
  DevSecOps-аудита (LR-022, фаза 1), устранено немедленно вне очереди по
  прямому указанию заказчика, не дожидаясь остальных фаз аудита.
- **fix** — зависимость удалена полностью (не переконфигурирована —
  нигде в коде `@RepositoryRestResource`/`RepositoryRestConfigurer` не
  используется, подтверждено grep). Фронтендовский `/users`-вызов
  резолвится в защищённый `/api/v1/users`, не в уязвимый путь — ничего
  не сломано.
- **fix** — `GlobalExceptionHandler` получил `@ExceptionHandler
  (NoResourceFoundException.class)` → честный 404 вместо введённого в
  заблуждение 500 (найдено при написании регрессионного теста).
- **test** — `SpringDataRestExposureTest`, реальный E2E (не mock):
  регистрация → логин → запрос, 4 теста (`USER` и `ADMIN` × `GET`/`PATCH`),
  все должны быть 404 (эндпоинта не должно существовать вообще, ни для
  какой роли) + проверка через репозиторий, что роль не изменилась.
- **review** — `architect-reviewer`: approve, must-fix не найдено.
  Ревьюер сам пересобрал и прогнал тест против реальной Testcontainers
  Postgres, не поверил на слово.
- **verify** — `./gradlew clean test` — полный сьют зелёный.
- LR-023 закрыт и заархивирован. LR-022 (сам аудит) продолжается —
  фазы 1-3 готовы, фаза 4 (протокол в `docs/security/`) следующая.

## 2026-08-06 — security: LR-020 — Bean Validation не работал нигде в бэкенде (провайдер отсутствовал); feat: LR-012 — `@Size` на address/phone/city/zipCode

### Область (`backend/build.gradle`, `backend/src/main/java/com/be/web/dto/request/{UserRegistrationDTO,UserUpdateDTO,TeacherRequestDTO,ParticipantRequestDTO}.java`, `backend/src/test/java/com/be/web/dto/request/RequestDtoValidationTest.java`)

- **security (LR-020, HIGH)** — `build.gradle` объявлял только
  `jakarta.validation-api` (интерфейсы), никогда `spring-boot-starter-
  validation`/Hibernate Validator — то есть **ни один** `@Valid
  @RequestBody` по всему бэкенду реально не исполнялся в проде. Spring MVC
  молча теряет провайдера (`OptionalValidatorFactoryBean` ловит
  `NoProviderFoundException` внутри себя, `targetValidator = null`,
  `validate()` — no-op), без ошибки при старте и без исключения на запрос.
  Мертвы были: `@Email` на регистрации, `@Size(min=8)` на пароле, и —
  самое серьёзное — **`@AssertTrue` на `acceptedTerms`/
  `privacyPolicyAccepted`**, единственная точка проверки согласия при
  регистрации. Шире DTO: JPA-уровневая bean-валидация (Hibernate,
  `validation.mode=AUTO` по умолчанию) тоже была отключена тем же багом —
  теперь тоже активна на `Order.orderNumber`/`Workshop.workshopName`/
  `User.firstName`/`lastName` (границы щедрые, риск для существующих
  данных Olena низкий, но первое место искать при `ConstraintViolation`
  на flush после деплоя).
- **fix** — `jakarta.validation-api` заменён на `spring-boot-starter-
  validation` (тянет Hibernate Validator 8.0.3.Final; версия API ушла с
  зафиксированной 3.1.1 на управляемую Spring Boot BOM 3.0.2 —
  `architect-reviewer` подтвердил: не конфликтует, стандартная пара).
- **feat (LR-012)** — `@Size` на `address`/`city`/`zipCode`/`phone`
  (`UserRegistrationDTO`/`UserUpdateDTO`). Попутно найдено и исправлено:
  `TeacherRequestDTO`/`ParticipantRequestDTO` не имели вообще ни одной
  аннотации валидации (не только про address-семейство) — добавлены
  `@Size`/`@Email`/`@Pattern` на firstName/lastName/email/phone.
- **test** — `RequestDtoValidationTest` (прямая Bean Validation, без
  Spring-контекста), 6 тестов; именно этот тест первым упал с
  `NoProviderFoundException` и вскрыл LR-020.
- **review** — `architect-reviewer`: approve with changes, re-тир LR-012→
  выделен в отдельный HIGH-тикет LR-020, must-fix'ов в коде не найдено.
  Follow-up заведён отдельно: LR-021 (`PaymentRequestDTO`/
  `OrderRequestDTO` вообще без валидации — MED, платёжные поля, отдельный
  внимательный проход, не "по аналогии").
- **verify** — `./gradlew clean test` — полный сьют, 52 теста, 0 упавших
  (включая context-loading/Testcontainers-интеграционные тесты — реальное
  включение валидации app-wide ничего не сломало).
- LR-012 и LR-020 закрыты, перенесены в `archive.md`. LR-021 заведён,
  остаётся open.

## 2026-08-06 — security: LR-014 — timing side-channel в `resend-verification` закрыт

### Область (`backend/src/main/java/com/be/service/AuthService.java`, `backend/src/main/resources/application.properties`, `backend/src/test/java/com/be/service/AuthServiceTest.java`)

- **security** — `POST /auth/resend-verification` больше не выдаёт факт
  "этот email существует и не подтверждён" через разницу во времени
  ответа. Быстрая ветка (неизвестный/уже-подтверждённый email — один
  `findByEmail`) и медленная (реальный неподтверждённый аккаунт — токен +
  запись в БД + синхронный SMTP) теперь неотличимы по скорости: элапсед-
  тайм меряется вокруг всей ветки, при необходимости досыпается до
  `app.email-verification.resend-min-response-ms` (дефолт 400мс,
  `EMAIL_VERIFICATION_RESEND_MIN_MS`).
- **refactor** — `AuthService` переведён с `@RequiredArgsConstructor` на
  явный конструктор ради `@Value`-инъекции порога — тот же паттерн, что
  уже используется в `EmailVerificationService`.
- **test** — новый `resendVerification_unknownEmail_
  stillTakesAtLeastMinResponseTime` (конструктор в тестах получает
  50мс-порог, не 400мс — сьют не замедляется, тест не флеки).
- **verify** — `./gradlew test` зелёный (полный сьют).
- LR-014 закрыт, перенесён в `archive.md`.

## 2026-08-06 — feat: LR-019 — светлая/тёмная тема + переключатель; docs: LR-018 заведён

### Область (`frontend-svelte/src/routes/layout.css`, `+layout.svelte`, `src/app.html`, `messages/*.json`; `docs/tickets/tickets.md`)

- **feat** — переключатель темы (солнце/луна) в header, десктоп и
  мобильное меню. Светлая тема — переопределение тех же CSS-переменных
  (`--color-ink`/`--color-paper`/`--color-gold`/`--color-teal`/
  `--color-error`) под `:root[data-theme="light"]`; так как весь фронтенд
  уже и до этого использовал только Tailwind-классы токенов (`bg-ink`,
  `text-paper`...), а не хардкод-цвета, ни одна страница/компонент не
  потребовали правки. Persist через `localStorage`, anti-FOUC inline-
  скрипт в `app.html` (выполняется до гидратации). Дефолт — тёмная тема
  независимо от `prefers-color-scheme` (сознательно, не менять дефолтное
  первое впечатление без явного клика посетителя).
- **docs** — `layout.css` ранее содержал комментарий "no light/dark
  toggle — a deliberate brand choice" (ссылка на LR-ADR-014) — сама ADR
  такого пункта буквально не формулирует, это была интерпретация
  реализации на момент дизайна, не отдельно подтверждённое заказчиком
  решение. Прямой запрос заказчика 2026-08-06 отменяет её для этого
  пункта — комментарий обновлён, ADR не переписывается (не противоречит).
- **verify** — проверено вживую в браузере (dev-сервер, desktop 1280×720
  + mobile 375×812): дефолт/переключение/persist через reload/aria-label
  — всё корректно на обоих брейкпоинтах. `npm run check` — 0 ошибок.
- **docs** — LR-019 закрыт и перенесён в `archive.md`. Заведён LR-018
  (bootstrap первого админа — сейчас только через ручной `psql`, нет
  env-var/seed-механизма) — остаётся open, требует решения заказчика
  (код vs документация), не начат.

## 2026-08-05 — closed: LR-015 — дашборд-метрики M1/M4/M5/M6 (M2/M3 отложены на LR-017)

### Область (backend: `web/dto/response/{GroupFillRateDTO,RegistrationTrendPointDTO,WorkshopAlertDTO,RetentionDTO,AdminMetricsDTO}.java`, `service/MetricsService.java`, `web/controller/MetricsController.java`, `domain/repository/{UserRepository,EnrollmentRepository}.java`, `test/.../MetricsServiceTest.java`; frontend: `lib/api.ts`, `routes/admin/+page.svelte`, `messages/*.json`)

- **feat** — новый `GET /api/v1/admin/metrics` (`ADMIN`/`BUSINESS_OWNER`),
  реализует М1 (заполненность по группам, считает только `PENDING`+
  `CONFIRMED`, не `CANCELLED` — сознательное расхождение с `Group.
  getEnrolledCount()`, который всё ещё считает любой статус на публичной
  странице воркшопа, отдельный, не исправленный здесь пробел), М4 (тренд
  новых регистраций за 30 дней, только `Role.USER`), М5 (алерты по
  многоступенчатым порогам — 7д/<30% инфо, 5д/<50% предупреждение,
  3д/<70% срочно, 1д/<90% критично — финально подтверждены заказчиком),
  М6 (retention — пользователь с `CONFIRMED`-записью в ≥2 разных
  воркшопах, только `Role.USER`).
- **feat** — `admin/+page.svelte` дополнен четырьмя секциями метрик рядом
  с уже существующей статистикой пользователей/ближайшими воркшопами.
- **test** — `MetricsServiceTest` (параметризованный, Mockito, без
  Spring-контекста) — 13 граничных кейсов для порогов М5, включая "ровно
  на границе" (строгое "<": попадание точно в порог его НЕ пробивает).
- **review** — `architect-reviewer` пройден, один must-fix найден и
  исправлен: `EnrollmentRepository.countDistinctWorkshopsPerUserWithStatus`
  изначально не фильтровал по роли — `EnrollmentController.enroll`
  разрешает создавать enrollment-записи не только `USER`, но и `TEACHER`/
  `ADMIN`/`BUSINESS_OWNER`, то есть тестовые/служебные записи могли раздуть
  М6. Добавлен параметр `role` в JPQL-запрос, вызов ограничен `Role.USER`
  (симметрично М4).
- **verify** — `./gradlew test` зелёный (полный сьют, включая новый
  `MetricsServiceTest`), `npm run check` — 0 ошибок.
- LR-015 закрыт полностью в рамках согласованного MVP-скоупа, перенесён
  в `docs/tickets/archive.md`. М2/М3 остаются в `docs/tickets/tickets.md`
  как заблокированные на LR-017 (механизм подтверждения регистрации/оплаты).

## 2026-08-05 — feat: LR-015 Б.4/Б.5 — venue переехал на Group, добавлен room + UI для возрастных групп

### Область (backend: `db/migration/V4__*.sql`, `domain/entity/{Group,Venue,Workshop}.java`, `service/{GroupService,VenueService,WorkshopService}.java`, `web/mapper/{GroupMapper,VenueMapper,WorkshopMapper}.java`, `web/dto/**`; frontend: `lib/api.ts`, `routes/admin/{groups,venues,workshops,age-groups}/+page.svelte`, `routes/admin/+layout.svelte`, `routes/workshops/**`, `messages/*.json`)

- **feat/migration** — `V4__venue_to_group_level_plus_room.sql`: `venue_id`
  перенесён с `workshops` на `workshop_groups` (архитектурное решение
  заказчика, Круглый стол #3 — локация принципиально привязана к
  конкретной сессии/группе, не к воркшопу целиком), `venues` получил поле
  `room` (одно физическое место может иметь несколько независимо
  бронируемых залов). Данные существующих воркшопов скопированы на все их
  группы перед дропом старой колонки — не потеряны. По ходу пойман и
  исправлен баг миграции: join использовал несуществующую колонку `w.id`
  вместо реальной `w.workshop_id` (`Workshop.java`'s `@Column(name =
  "workshop_id")`) — упало на `./gradlew test` (Flyway против реального
  Postgres через Testcontainers), не в проде.
- **feat** — возрастные группы для `Group`: схема уже поддерживала
  (`Group.ageGroup`), добавлен UI-флоу — новая admin-страница
  `admin/age-groups` (CRUD, зеркало `admin/venues`) + select в форме
  `admin/groups`. `GroupDTO.ageGroupName` composed server-side
  ("titleDe (min–max)"). Дефолтную "все возрасты" сознательно не завели
  (заказчик подтвердил, Круглый стол #3, п. 2.3).
- **fix** — `GroupMapper.toDto()` (маппер, который реально используется
  `GroupController`'ом на `/api/v1/groups`, в отличие от `WorkshopMapper.
  toGroupDTO()` для публичной страницы воркшопа) не отдавал `venueId`/
  `venueName` вообще — секция "Место/зал" в самой админке всегда была
  пустой. Исправлено.
- **fix** — `GroupService.update()` (ручное копирование полей на managed
  entity, установленный паттерн из-за raw-entity binding в
  `GroupController`) не копировал `venue` — редактирование зала у
  существующей группы тихо не сохранялось. Исправлено (та же строка
  рядом с уже существующими `setActivity`/`setAgeGroup`/`setTeacher`).
- **review** — `architect-reviewer` пройден, approve as-is. Один
  follow-up без блокировки: `workshop_groups.venue_id` FK без `ON
  DELETE` — не регрессия (тот же пробел уже был у старой
  `workshops.venue_id` в V1__baseline.sql), тикет по необходимости.
- **verify** — `./gradlew test` зелёный (полный сьют), `npm run check`
  (svelte-check) — 0 ошибок, 1084 файла.

## 2026-08-05 — docs: Круглый стол #3 — скоуп admin/owner-дашборда согласован (LR-015/016/017)

### Область (`docs/tickets/tickets.md`, `.claude/settings.local.json`, `CLAUDE.md`)

- **docs** — заказчик подтвердил весь скоуп admin-дашборда: `venue`
  переносится с `Workshop` на `Group` (не просто новое поле), `Venue`
  получает поле `room`, авторство воркшопов и дефолтная возрастная
  группа сознательно не делаются для MVP, роли `TEACHER`/`BUSINESS_OWNER`
  архитектурно предусмотрены, но не реализуются отдельно в MVP (весь
  функционал — под `ADMIN`). Метрики М1/М3/М4/М5/М6 — строим; М2
  (реальная выручка) — MVP-прокси через цену×регистрации, настоящая
  оплата (Stripe/Numi) — отдельная задача. Заведены `LR-015` (основной
  скоуп), `LR-016` (авто-проверка конфликтов зала, сознательно low
  priority/не MVP), `LR-017` (механизм подтверждения участия — блокирует
  точность М2/М3, начинать реализацию рано).
- **infra** — `docs/context/CHANGELOG.md` добавлен в allow-list
  `.claude/settings.local.json` (по аналогии с `docs/tickets/*.md`) —
  обновление CHANGELOG после закрытия работы больше не требует
  permission prompt. `CLAUDE.md` обновлён — явно зафиксировано, что
  заведение нового тикета "в моменте" без повторного спроса допустимо
  только если пользователь реально подтвердил его создание в диалоге,
  не как самостоятельное решение без вопроса в первый раз.

## 2026-08-04 — closed: Brevo SMTP настроен вживую, email-верификация подтверждена рабочей в проде (LR-013)

### Область (`devops/helm/lr-app/{templates/backend-deployment.yaml,values.yaml}`, `frontend-svelte/src/routes/login/+page.svelte`, прод-БД — ручная очистка)

- **infra** — `SMTP_USERNAME`/`SMTP_PASSWORD` подключены в
  `backend-deployment.yaml` (`secretKeyRef` на `lr-backend-secrets`,
  `optional: true` — под продолжает стартовать и без этих ключей, подхватит
  их на следующий рестарт после появления). `values.yaml` — обновлена
  документация `kubectl patch secret` для добавления SMTP-ключей без
  трогания существующих `jwt-secret`/`field-encryption-key`.
- **готово вручную заказчиком** — домен `tlab29.com` аутентифицирован в
  Brevo (TXT-верификация + SPF + DKIM CNAME `brevo1._domainkey`, все —
  DNS only в Cloudflare, без прокси — обязательно для email-аутентификации,
  прокси сломал бы DNS-lookup для принимающих серверов), добавлен sender
  `noreply@tlab29.com` / "Lebens Rhythmus", SMTP-креды (Brevo выдаёт
  логин вида `bNNN***@smtp-brevo.com`, не email аккаунта — отличается от
  более старых Brevo-аккаунтов) внесены в `lr-backend-secrets` через
  `kubectl patch`, под перезапущен.
- **найден и исправлен реальный прод-баг (не новый код, старые данные)**
  — при логине `hudoshin7605@gmail.com` (id=1) — 500,
  `ArrayIndexOutOfBoundsException: last source index 12 out of bounds for
  byte[5]` в `EncryptedStringConverter.convertToEntityAttribute`.
  Подтверждено живым логом: это ровно тот сценарий, что был описан в
  LR-011 как известный риск — `first_name` этого аккаунта остался
  plaintext с домиграционных времён, `PiiReencryptionRunner` для него не
  запускался. Данные были не нужны — решение: удалить аккаунт, не чинить.
- **прод-данные, ручная очистка** — три тестовых аккаунта (`hudoshin7605@
  gmail.com` id=1 — сломанный legacy plaintext, `klanov0705@gmail.com`
  id=2, `claude-debug-test-1@example.com` id=3 — созданы этой сессией при
  диагностике/тестах) удалены из `lr-dev` вместе с зависимыми строками
  (`feedbacks`/`user_notifications`/`enrollments`/`payments`/`orders` по
  `user_id`, `workshops.teacher_id` обнулён, не удалялся). Промежуточный
  шаг — `email_verified = false` вместо `DELETE` (см. LR-011/LR-013
  историю) — использовался для проверки, что логин реально блокирует
  неподтверждённые аккаунты, прежде чем данные снесли целиком.
- **feat** — кнопка "отправить письмо повторно" добавлена и на экран
  успешной регистрации ("проверьте почту"), не только на экран неудачного
  логина (там уже была) — раньше при "письмо не пришло" сразу после
  регистрации восстановиться было нечем. Отдельное `busy`/`sent`-состояние
  от login-варианта (тот же API-вызов, разные экраны). Проверено вживую:
  реальная форма регистрации → успех → resend → "письмо отправлено
  повторно", без ошибок в консоли.
- **подтверждено заказчиком** — фича работает в проде end-to-end:
  регистрация → письмо реально приходит → подтверждение по ссылке →
  логин.

## 2026-08-04 — fix: `/actuator/health` 503 в проде из-за MailHealthIndicator (нашли по упавшему smoke-test)

### Область (`backend/src/main/resources/application.properties`)

- **fix (прод-инцидент, найден по failed smoke-test job в GitLab CI)** —
  после деплоя email-верификации (коммиты `56d1f87`/`229830b7`)
  `curl https://api.tlab29.com/actuator/health` стал отдавать 503. Причина:
  `spring-boot-starter-mail` автоматически регистрирует
  `MailHealthIndicator` (Spring Boot actuator,
  `MailHealthContributorAutoConfiguration` — срабатывает на любой bean
  `MailSender`) — он реально пробует SMTP-коннект как часть
  `/actuator/health`. Без настоящих Brevo-кредов (LR-013 всё ещё open)
  этот пробник падает, тащит агрегированный статус health в `DOWN` →
  Spring Boot по умолчанию маппит `DOWN` в HTTP 503. Хуже того:
  readiness/liveness пробы пода в `backend-deployment.yaml` смотрят на тот
  же `/actuator/health` — то есть под мог реально стать `NotReady` или
  начать рестартовать, не только упасть CI-шаг.
- **fix** — `management.health.mail.enabled=false`. Отключает только
  почтовый health-индикатор, не весь `/actuator/health` — реальная логика
  приложения (логин, регистрация и т.д.) это не проверяет и не должна
  зависеть от того, настроен ли SMTP.
- Полный бэкенд-сьют перепрогнан после фикса — 32/32, 0 ошибок.

## 2026-08-04 — feat: email-верификация при регистрации (Brevo SMTP) + фикс двух фронтенд-багов

### Область (backend: `service/{AuthService,EmailVerificationService,MailService}.java`, `domain/exception/{EmailNotVerified,InvalidVerificationToken}Exception.java`, `web/{controller/AuthController,handler/GlobalExceptionHandler}.java`, `web/dto/**`, `domain/entity/User.java`, `domain/repository/UserRepository.java`, `db/migration/V3__add_email_verification.sql`, `build.gradle`, `application.properties`; frontend: `src/lib/api.ts`, `src/routes/{login/+page.svelte,verify-email/+page.svelte,+layout.svelte}`, `messages/{de,en,uk}.json`)

- **найдено заказчиком** — `User.emailVerified` существовал с V1, но нигде не
  устанавливался в `true`, никакого письма не отправлялось вообще, логин
  не проверял поле — любой email (даже несуществующий/чужой) фактически
  принимался при регистрации без единого подтверждения.
- **feat** — при регистрации теперь генерируется одноразовый токен (SHA-256
  хеш в БД, plaintext только в письме — та же логика, что для паролей),
  письмо со ссылкой уходит через Brevo SMTP (`spring-boot-starter-mail`,
  тот же провайдер, что у numi, отдельный sender). Логин теперь **блокирует**
  неподтверждённые аккаунты (`EmailNotVerifiedException` → 403, code
  `EMAIL_NOT_VERIFIED`) — по решению заказчика 2026-08-04.
- **security** — проверка `emailVerified` идёт **после** сверки пароля, не
  до: иначе кто угодно мог бы узнать "существует ли и подтверждён ли этот
  email" без единой попытки подобрать пароль. Токен подтверждения хранится
  только как хеш (не plaintext) — компрометация БД/бэкапа не выдаёт рабочих
  ссылок. `resendVerification` не палит существование/статус аккаунта —
  всегда одинаковый ответ вызывающему, реально отправляет письмо только
  если аккаунт есть и ещё не подтверждён.
- **⚠️ критический риск деплоя, исправлено в той же миграции** —
  `email_verified` был `false` у **вообще всех** существующих пользователей
  (фичи не было — некому было его выставлять). Без бэкфилла деплой этой
  миграции мгновенно заблокировал бы логин всем текущим пользователям,
  включая рабочий аккаунт заказчика — им бы неоткуда было взять письмо для
  подтверждения (зарегистрировались до появления фичи). `V3` теперь
  включает `UPDATE users SET email_verified = true WHERE email_verified =
  false` — корректно только потому, что Flyway гарантирует однократное
  применение строго до первой новой регистрации по новому коду.
- **fix (не фатально для регистрации)** — реальных Brevo-кредов пока нет
  (`SMTP_USERNAME`/`SMTP_PASSWORD` — пустые дефолты), значит отправка
  письма будет падать прямо сейчас. Ошибка отправки **перехватывается и
  логируется на ERROR** (не проглатывается тихо — тот самый урок про
  "SMTP молча замокан" на numi, только наоборот: здесь цель не молчать, а
  не 500'ить всю регистрацию из-за недоступной почты), аккаунт создаётся,
  токен сохраняется — повторная отправка через `resendVerification` после
  реальной настройки Brevo сработает с тем же (новым) токеном.
- **fix (баг фронтенда, найден заказчиком)** — после успешного логина кнопка
  в меню продолжала показывать "Anmelden" вместо "Abmelden". Причина:
  `isAuthenticated()`/`getStoredRole()` читают `localStorage`, не реактивный
  Svelte-state — `$effect()` без реактивных зависимостей внутри выполнялся
  ровно один раз при монтировании layout'а и не перезапускался при
  client-side навигации `/login` → `/dashboard` после логина. Исправлено
  через `afterNavigate` (`$app/navigation`) — реально реагирует на
  логин/логаут, не только на первую загрузку.
- **лицензия** — `spring-boot-starter-mail` тянет Angus Mail
  (`jakarta.mail`), лицензия **EPL-2.0 OR GPL-2.0-WITH-Classpath-Exception-2.0**
  — тот же паттерн, что уже явно принят в `CODING_PROTOCOL.md` для
  OpenJDK/Temurin, проверено явно (не по умолчанию популярности пакета).
- **test** — `AuthServiceTest` (7): порядок проверок пароль→verified (не
  наоборот), register больше не возвращает токен, `resendVerification` не
  палит существование аккаунта. `EmailVerificationServiceTest` (5): реальный
  round-trip токена, хеш ≠ plaintext, expired-токен отклоняется, ошибка
  почты не прокидывается наружу. Итого бэкенд — 32 теста, 0 ошибок.
- **не проверено в этой сессии** — реальная доставка письма через Brevo
  (нет живых кредов), и сам бэкфилл на **реальном** `lr-dev` (проверено
  только через reasoning + то, что миграция синтаксически валидна и
  применяется в тестах на чистом Postgres — там просто нет "старых" строк
  до неё).
- **architect-reviewer: approve with changes** — независимо перезапустил
  тесты (32/32), нашёл 2 находки:
  1. **Исправлено** — `backend-deployment.yaml` не задавал `strategy`,
     значит наследовал k8s-дефолт `RollingUpdate` (`maxSurge` округляется
     до 1 пода даже при `replicas: 1`) — узкое окно, где новый под мог бы
     начать обслуживать трафик до завершения старого, и регистрация,
     попавшая на СТАРЫЙ под именно в этот момент, теоретически могла бы
     попасть под бэкфилл нового пода как "существующий" аккаунт, минуя
     верификацию. Добавлен `strategy: {type: Recreate}` — ничего не
     стоит, реплика и так одна.
  2. **Зафиксировано на будущее, не блокирует** — `resendVerification`
     не палит существование аккаунта по содержимому ответа, но реальное
     **время ответа** отличается (синхронный SMTP-коннект только для
     реального неподтверждённого аккаунта) — timing side-channel, заведён
     LR-014, низкий приоритет (не платёжные/детские данные за этим
     логином).

## 2026-08-03 — fix: фавикон показывал лого SvelteKit вместо Lebens Rhythmus

### Область (`frontend-svelte/{static/,src/app.html,src/routes/+layout.svelte,src/lib/assets/favicon.svg (удалён)}`)

- **fix (найдено заказчиком, реальный прод-баг)** — `src/lib/assets/
  favicon.svg` с самого создания SvelteKit-приложения (`npm create svelte`,
  задача №17 этой сессии) буквально содержал стоковое SVG-лого SvelteKit
  (`<title>svelte-logo</title>` внутри самого файла) — никогда не был
  заменён на реальный. Подтверждено напрямую на проде: `<link rel="icon">`
  отдавал data-URI именно этого SVG.
- **fix** — перенесены реальные иконки студии (набор favicon.io, уже
  использовавшийся живым старым статическим сайтом —
  `frontend/assets/favicon_io/`) в `frontend-svelte/static/`:
  `favicon.ico`, `favicon-{16x16,32x32}.png`, `apple-touch-icon.png`,
  `android-chrome-{192x192,512x512}.png`, `site.webmanifest` (заодно
  заполнены `name`/`short_name` — в оригинальном файле были пустые
  строки). Ссылки на них — явными `<link>` в `src/app.html` (стандартно,
  без Vite-обработки ассетов), а не через Svelte-компонент — старый
  `<svelte:head><link rel="icon" href={favicon} /></svelte:head>` в
  `+layout.svelte` и сам плейсхолдер `favicon.svg` удалены.
- **проверено вживую** — прод-сборка (`npm run build` + `npm run
  preview`): все 5 файлов (`favicon.ico`, оба PNG, `apple-touch-icon.png`,
  `site.webmanifest`) отдаются реальным сервером с кодом 200, `<head>`
  содержит правильные `<link>`-теги без единого следа SvelteKit-лого.
  `svelte-check` 0 ошибок, Vitest 12/12.

## 2026-08-03 — feat: инструмент ре-шифрования legacy-PII, LR-011 почти закрыт

### Область (`backend/src/main/java/com/be/tools/PiiReencryptionRunner.java`, `backend/src/test/java/com/be/PiiReencryptionRunnerTest.java`, `docs/tickets/tickets.md`)

- **feat** — `PiiReencryptionRunner` (`@Profile("reencrypt-pii")`, полностью
  инертен при обычном запуске приложения) — одноразовый инструмент для
  безопасной миграции legacy plaintext в зашифрованных PII-колонках
  (`users`/`teachers`/`participants`). Dry-run по умолчанию, `--apply`
  для реальной записи. Работает через raw `JdbcTemplate`, не через
  JPA/`@Convert` — иначе чтение legacy-строки уронило бы сам процесс
  миграции раньше, чем инструмент успел бы её обработать (та же причина,
  по которой обычный деплой против непроверенной БД тоже упал бы).
  Идемпотентен: уже зашифрованные строки определяются реальной попыткой
  расшифровки (GCM auth tag делает "случайное" совпадение
  вычислительно неосуществимым), не эвристикой по длине.
- **test** — `PiiReencryptionRunnerTest`: сеет смесь legacy-plaintext
  (вставлен напрямую через JDBC, в обход конвертера — как выглядела бы
  настоящая до-миграционная строка) и уже-зашифрованных строк, проверяет
  dry-run (ничего не пишет), `--apply` (шифрует только plaintext, не
  трогает уже зашифрованное), и — главное доказательство — что обычное
  чтение через `UserRepository` после этого больше не падает (именно тот
  сценарий отказа, который описывает LR-011).
- **проверено на реальном `lr-dev`** (2026-08-03, запрос заказчика на
  кластере): `teachers`/`participants` пусты, в `users` — один тестовый
  аккаунт заказчика с непустым `first_name`. Реальных клиентских данных
  нет — по решению заказчика этот один тестовый аккаунт будет удалён
  вручную перед деплоем, инструмент ре-шифрования не понадобился для
  этого конкретного случая, но остаётся в репозитории на будущее (если
  реальные данные накопятся до следующего похожего изменения схемы).
- **LR-011 почти закрыт** — остаётся: удалить один тестовый аккаунт
  перед деплоем (команды даны в чате), и помнить, что LR-003 (бэкап)
  всё ещё не активен на кластере — деплоить с осторожностью.
- Полный тест-сьют бэкенда — 20 тестов, 0 ошибок.

## 2026-08-03 — fix: два реальных бага, найденных architect-reviewer при шифровании PII (продолжение 2026-07-24)

### Область (`backend/src/main/java/com/be/{service/UserService,domain/repository/UserRepository}.java`, `backend/src/main/resources/db/migration/V2__widen_encrypted_pii_columns.sql`, `backend/src/test/java/com/be/service/UserServiceTest.java`)

- **fix (сломанный поиск)** — `UserService.searchUsers()` использовал
  Spring Data derived query
  `findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase`
  (SQL `LIKE`) — после шифрования `firstName`/`lastName` это тихо всегда
  возвращало бы пустой список (шифротекст никогда не матчится по `LIKE`).
  Исправлено: `findAll()` + фильтрация в памяти (decrypt-and-filter) —
  расшифровка прозрачна на уровне JPA, приемлемо для ожидаемого масштаба
  одной студии. Blind-index колонка — правильное решение, если это
  когда-нибудь придётся масштабировать, отдельным follow-up, не сейчас.
  Мёртвый derived-query метод удалён из `UserRepository` (не было других
  вызывающих — проверено `grep`).
- **fix (риск переполнения VARCHAR(255))** — шифротекст всегда длиннее
  исходного текста (12-байт IV + 16-байт GCM tag, затем base64 ×4/3);
  `address` не имеет вообще никакого ограничения длины на уровне
  приложения. Новая миграция `V2__widen_encrypted_pii_columns.sql`
  расширяет `users.{first_name,last_name,phone,address,city,zip_code}`,
  `teachers.{first_name,last_name,phone}`,
  `participants.{first_name,last_name,phone}` до `TEXT` (в Postgres
  функционально идентично неограниченному `VARCHAR`, без риска
  когда-либо упереться в лимит снова). `iban`/`tax_id` не тронуты —
  формат IBAN ограничен 34 символами, реального риска там нет.
- **test** — новый `UserServiceTest` (2 теста: case-insensitive матч по
  имени/фамилии; null-поля не роняют поиск с NPE).
- **проверено** — полный набор бэкенд-тестов (19 тестов, включая
  `SensitiveFieldEncryptionIntegrationTest` с применённой V2-миграцией) —
  0 ошибок.

## 2026-07-24 — security: инвентаризация шифрования чувствительных полей (DSGVO/GoBD) — Participant/User/Teacher

### Область (`backend/src/main/java/com/be/domain/entity/{User,Teacher,Participant}.java`, `backend/src/test/java/com/be/SensitiveFieldEncryptionIntegrationTest.java`)

- **аудит** — до этой сессии полем на шифрование были только `User.iban`/
  `User.taxId` (подтверждено новым интеграционным тестом — см. ниже).
  Полная инвентаризация всех сущностей нашла: `Participant` (ФИО+email+
  телефон+дата рождения участника воркшопа — отдельная от `User` сущность,
  без привязки к аккаунту-опекуну) не имел вообще никакой защиты;
  `Teacher` (ФИО/телефон, частично публичный профиль через `GET /teachers`)
  и сам `User` (ФИО/телефон/адрес/город/индекс) — тоже нет.
- **security** — по решению заказчика 2026-07-24 добавлено
  `@Convert(EncryptedStringConverter.class)` на:
  `User.{firstName,lastName,phone,address,city,zipCode}`,
  `Teacher.{firstName,lastName,phone}`,
  `Participant.{firstName,lastName,phone}`.
  Расшифровка прозрачна на уровне JPA — публичные ответы API (например,
  `GET /teachers`) продолжают отдавать настоящее имя, шифруется только то,
  что реально лежит в БД/бэкапе.
- **сознательно НЕ зашифровано** — `email` везде (`User`/`Teacher`/
  `Participant`): `UNIQUE`-constraint в БД, а AES-GCM даёт разный
  шифротекст на каждый вызов для одного и того же значения — шифрование
  сломало бы реальную проверку дублей молча (не ошибкой, а тихой потерей
  функциональности). `birthDate`/`Participant`: нативная колонка `DATE`,
  текущий конвертер — только `String→String`; отдельный `LocalDate`-конвертер
  — separate follow-up, не в этой сессии. `Venue` (адрес/контакты арендованного
  зала) — организационные данные, не персональные, DSGVO не касается.
- **проверено вживую, не только юнит-тестом конвертера** — новый
  `SensitiveFieldEncryptionIntegrationTest` (Testcontainers + реальный
  Postgres): для `User`, `Teacher`, `Participant` — реальный
  `save()` → `entityManager.flush()+clear()` → `findById()` (не
  закэшированный Java-объект) → значения корректно расшифровались;
  **отдельно** прочитано сырое значение колонки напрямую через JDBC, в
  обход Hibernate — подтверждено, что в реальной БД лежит шифротекст
  (не содержит исходное значение даже подстрокой), а `email` — как есть,
  не тронут конвертером.
- **⚠️ операционный риск, требующий внимания перед деплоем** — если в
  живой `lr-dev` БД уже есть реальные строки `users`/`teachers`/
  `participants` с открытым (не зашифрованным) `firstName`/`lastName`/
  `phone`/`address`/`city`/`zipCode`, то после деплоя этого изменения
  Hibernate попытается расшифровать эти старые plaintext-значения как
  если бы это был base64(IV+ciphertext) — и упадёт с ошибкой при первом
  же чтении такой строки. **Нужно явно подтвердить перед деплоем**: либо
  таблицы пока пустые/тестовые, либо нужен одноразовый скрипт
  ре-шифрования существующих данных. Резервная копия (LR-003) для отката
  в случае проблемы **тоже ещё не активна на кластере** — дополнительная
  причина не деплоить это без явного решения.

## 2026-07-24 — infra: интерим-бэкапы PostgreSQL — CronJob + restic + IDrive e2 (LR-003)

### Область (`devops/helm/lr-app/{templates/postgres-backup-{cronjob,configmap}.yaml,values.yaml}`, `docs/runbooks/infra-fix-shutdown.md`, `docs/tickets/tickets.md`)

- **infra** — `lr-postgres` не имел вообще никакого бэкапа. Добавлен
  Kubernetes CronJob `lr-postgres-backup` (`postgres:16-alpine` + `restic`
  через `apk` на лету, не отдельный кастомный образ): `pg_dump` →
  `restic backup` → `restic forget --prune` (retention 14 daily / 8
  weekly), по расписанию 03:00 UTC ежедневно. Назначение — IDrive e2
  (тот же провайдер, что numi Litestream, отдельный bucket/credentials),
  механизм — Kubernetes CronJob (не systemd на VM200, как изначально
  предполагал тикет) — оба решения подтверждены заказчиком.
- **проверено локально** (не на проде) — `postgres:16-alpine` реально
  содержит `pg_dump`, `restic` ставится и работает через `apk`; полный
  прогон script'а (`pg_dump`/`restic init`/`restic backup`/`restic
  forget --prune`) против тестовой Postgres — все шаги отработали,
  снапшот создан. `helm template` — 0 ошибок; `postgresBackup.enabled:
  false` корректно убирает CronJob и ConfigMap из рендера.
- **docs** — процедура восстановления добавлена в
  `infra-fix-shutdown.md` (новый раздел) — явно помечена как ни разу не
  проверенная на реальном кластере (тот же класс риска, что уже
  случался с numi/Litestream: непроверенный бэкап не доказан рабочим).
- **не может быть сделано мной** — создание IDrive e2 bucket, реальный
  `kubectl create secret lr-backup-secrets`, деплой через `helm upgrade`,
  и хотя бы один реальный прогон restore — всё требует доступа к живой
  прод-инфраструктуре, см. чек-лист "Осталось сделать" в `tickets.md`
  LR-003.
- **architect-reviewer: approve with changes** — нашёл реальный,
  довольно серьёзный rollout-баг:
  1. **Блокирующее** — `postgresBackup.enabled: true` по умолчанию + то,
     что `deploy-dev` в `.gitlab-ci.yml` делает `helm upgrade --install`
     безусловно на КАЖДЫЙ пуш без `--set postgresBackup.*` — означало,
     что самый обычный следующий пуш (по любому другому тикету) молча
     создал бы CronJob со ссылкой на несуществующий Secret и пустым
     `repository`, который падал бы каждую ночь незамеченным. Исправлено:
     `enabled: false` по умолчанию, включать явно (`--set` или прямое
     значение в `values.yaml`) только когда `lr-backup-secrets` и
     `repository` реально существуют.
  2. **Should-fix** — добавлен `backoffLimit: 1` (был бы default 6 —
     6 попыток `apk add`+`pg_dump`+`restic` подряд за одну ночь при
     постоянной ошибке, лучше дождаться завтрашнего расписания).
  3. **Некритично, зафиксировано на будущее (не решено сейчас)** —
     backup-job переиспользует ту же учётку `lr-db-credentials`, что и
     сам StatefulSet (полные права владельца БД, хотя для `pg_dump`
     достаточно read-only) — отдельной read-only роли в проекте вообще
     нет нигде, создание такой роли требует ручной `psql`-сессии против
     прод-БД, не делается этим диффом. Нет алертинга при падении job'а
     (узнать можно только `kubectl get cronjob`). Оба пункта — реальные,
     стоит явно занести в LR-003 как follow-up, не считать закрытыми
     молча.
  `helm template` перепроверен после фиксов — 0 ошибок, `enabled` по
  умолчанию корректно убирает оба ресурса, `--set enabled=true`
  корректно их включает с `backoffLimit` на месте.

## 2026-07-24 — fix: мобильное меню не закрывалось после выбора пункта

### Область (`frontend-svelte/src/routes/+layout.svelte`)

- **fix (найдено заказчиком в проде, iPhone 14 portrait, сразу после
  деплоя)** — мобильный `<nav>` (`{#if mobileOpen}`) не сбрасывал
  `mobileOpen` при клике на ссылку/кнопку внутри — SvelteKit's client
  router не перемонтирует layout при навигации между страницами одного
  layout, так что состояние просто оставалось `true`, меню "зависало",
  занимая ~60% высоты экрана. Добавлен `closeMobileMenu()`, повешен на
  все ссылки и на кнопку logout (`+ handleLogout()` в одном обработчике)
  внутри мобильного nav.
- **проверено вживую** — `svelte-check` 0/1064, Vitest 12/12; в браузере
  на вьюпорте 390×844 (iPhone 14): открыл меню, кликнул "Über uns" —
  переход произошёл И меню закрылось (`mobileNavCount` вернулся к 1,
  блок `{#if mobileOpen}` пропал из DOM); отдельно проверил кнопку
  logout — сессия очистилась, редирект на `/`, меню тоже закрылось.

## 2026-07-23 — infra: CI собирает и тестирует `frontend-svelte`, старый статический сайт больше не деплоится (LR-002 пп.4, 4b)

### Область (`.gitlab-ci.yml`, `frontend-svelte/{Dockerfile,nginx.conf,.dockerignore}`)

- **infra** — новый job `test-frontend` (stage `build`, `node:22-alpine`):
  `npm ci && npm run check && npm run test`, реальный gate через
  `needs:` у `docker-frontend` (красный check/test — образ не собирается).
  Закрывает явное требование LR-ADR-020 не повторить ошибку backend'а
  (тесты существуют, но никогда не выполняются в pipeline).
- **infra** — `docker-frontend` job переключён с `cd frontend` на
  `cd frontend-svelte`. Старый статический сайт (`frontend/`, снесён
  круглым столом ещё 2026-07-20) больше не собирается и не деплоится
  вообще — единственный frontend-образ теперь `frontend-svelte`.
  Helm-чарт не тронут — проверено, контракт (image repo/tag через
  `--set`, `containerPort: 80`) не меняется.
- **feat** — `frontend-svelte/Dockerfile` (multi-stage: `node:22-alpine`
  собирает `npm run build`, `nginx:alpine` отдаёт статику),
  `frontend-svelte/nginx.conf` (`try_files $uri $uri/ /index.html;` —
  обязателен из-за вчерашнего перехода на SPA fallback mode, без него
  прямой заход на любой маршрут кроме `/` дал бы 404 у реального nginx),
  `frontend-svelte/.dockerignore` (`node_modules`/`build`/`.svelte-kit` —
  контекст сборки иначе тянул бы сотни МБ).
- **проверено вживую, не только локальным `vite preview` (у него
  fallback уже встроенный, не показательно для реального nginx)** —
  `docker build` реального образа, `docker run`, затем **прямой `curl`**
  (без единой строчки JS/клиентской навигации) на `/`, `/workshops`,
  `/admin/users`, `/dashboard` — везде 200, содержимое — корректный
  SPA-shell со ссылками на `_app/immutable/...`-ассеты. Образ и контейнер
  (`lr-frontend-test`) удалены после проверки, в реестр не пушились.
- **architect-reviewer: approve with changes** — нашёл ровно то, что
  локальная проверка не могла показать (тестировался уже "загрязнённый"
  рабочей сессией checkout, не чистый):
  1. **Блокирующее** — `src/lib/paraglide` (импортируется в корневом
     layout, значит бьёт почти каждую страницу) генерируется только
     Vite-плагином paraglide (`buildStart`), который срабатывает при
     `vite build`/`vite dev`/Vitest, но **не** при `svelte-kit sync`/
     `svelte-check`. Директория в `.gitignore`, кэша в `.gitlab-ci.yml`
     нет — на чистом checkout `npm run check` в новом `test-frontend`
     job упал бы всегда, блокируя `docker-frontend` навсегда. У меня
     локально это было не видно, потому что `src/lib/paraglide` уже
     лежала на диске от более ранних `vite dev`/`build` в этой же сессии.
     **Исправлено:** `prepare`-скрипт в `package.json` теперь сам
     компилирует paraglide (`paraglide-js compile ...`), не полагаясь на
     побочный эффект от другой команды — `npm ci` (первый шаг что в CI,
     что в Dockerfile) гарантированно генерирует директорию перед чем
     угодно ещё. **Перепроверено по-настоящему** — скопировал текущее
     рабочее дерево (`tar` с `--exclude` на `node_modules/.svelte-kit/
     src/lib/paraglide/build`, не через `git clone`, чтобы попали именно
     несохранённые правки этой сессии) в чистую директорию, прогнал
     `npm ci && npm run check && npm run test` — 0 ошибок, 12/12 тестов,
     без единого файла с диска до этого.
  2. **Should-fix** — `nginx.conf`'s `/_app/` правило кэша было
     префиксным и захватывало `_app/version.json` (не хэшированный,
     меняется на каждый деплой, используется клиентским рантаймом
     SvelteKit для детекта новой версии) — кэшировать его на год
     сломало бы механизм обнаружения обновлений. Исправлено:
     `location ^~ /_app/immutable/` только для реально хэшированных
     ассетов + явный `Cache-Control: no-cache` на `index.html`
     (иначе браузер мог бы держать устаревший shell после деплоя).
     Перепроверено через `docker build`+`curl -I`: hashed-ассет →
     `public, immutable`, `index.html` → `no-cache`, `version.json` →
     без агрессивного кэша.
  3. Non-blocking — старый `frontend/` (мёртвый, никем не
     референсится в `devops/`) и пара доков (`PROJECT_INDEX.md`,
     `INFRA-LR.md`) всё ещё описывают фронтенд как статический —
     не блокирует этот тикет, отдельный follow-up при полном сносе
     `frontend/`.
  4. Minor — `.dockerignore` не исключал `.env`/`.env.*` (сейчас
     файла нет, но на будущее) — добавлено.

## 2026-07-23 — fix: `npm run build` больше не падает — SPA fallback вместо форсированного prerender (LR-002 п.3b)

### Область (`frontend-svelte/vite.config.ts`, `docs/tickets/tickets.md`)

- **fix** — `adapter-static` требовал `prerender=true` на всех 23
  маршрутах (найдено ещё 2026-07-22, не чинилось до сих пор). Причина:
  весь фронтенд грузит данные клиентски (`$effect`), никогда через
  SvelteKit `load()`, и часть страниц (dashboard/admin/teacher) —
  принципиально динамические (auth-gated), их бессмысленно
  prerender'ить. Решение — `adapter({ fallback: 'index.html' })` (SPA
  fallback mode), а не форсировать prerender там, где это противоречит
  архитектуре страниц. `sveltekit()` в этом проекте конфигурируется
  инлайн прямо в `vite.config.ts` (`sveltekit({ adapter: ... })`) —
  отдельного `svelte.config.js` в проекте нет и не было, это
  поддерживаемый способ конфигурации в SvelteKit 2.70+, не пропавший файл.
- **проверено вживую** — `npm run build` зелёный ("Wrote site to build"),
  `svelte-check` 0/1064, Vitest 12/12. `npm run preview` — прямой заход
  (не клиентская навигация) на `/workshops` корректно отдал fallback +
  клиентский роутинг подхватил, без ошибок в консоли.
- **найдено, не исправлено (та же задача, пункт 4b в LR-002)** — реальный
  nginx в Docker-образе будет НЕ прощать прямые заходы на вложенные
  маршруты без `try_files $uri /index.html;` — `vite preview` это скрывает
  (у него fallback уже встроен), нужно явно учесть при написании
  Dockerfile/nginx-конфига, не отдельным тикетом.

## 2026-07-23 — feat: admin-панель (7 страниц) + teacher-дашборд, портированы с исправлением реальных багов старого сайта

### Область (`frontend-svelte/src/routes/{admin/**,teacher/**,+layout.svelte}`, `frontend-svelte/src/lib/api.ts`, `frontend-svelte/messages/{de,en,uk}.json`)

- **feat** — `admin/+layout.svelte` (guard: ADMIN|BUSINESS_OWNER + общий nav),
  `admin/` (overview/stats, users, activities, workshops, groups, venues,
  performances — 7 страниц), `teacher/+layout.svelte` (guard: TEACHER|
  BUSINESS_OWNER|ADMIN) + `teacher/` (мои воркшопы, мои группы, участники
  по запросу). Глобальный nav (`+layout.svelte`) теперь ролево-зависим:
  ADMIN/BUSINESS_OWNER → `/admin`, TEACHER → `/teacher`, остальные →
  `/dashboard`.
- **research (перед кодом)** — полное сравнение старого статического
  admin/teacher UI против реальных backend DTO (не доверяя старому JS)
  нашло несколько реальных, разных по природе багов, все исправлены при
  портировании (не унаследованы):
  1. Group-teacher select старого UI брал User.id из `/users/role/TEACHER`,
     но `Group.teacher` — FK на отдельную сущность `Teacher`, не `User`.
     Новый UI берёт `teacherId` из `GET /teachers`.
  2. Performance `venue` — обычная `String` на бэкенде, не Venue FK; старый
     JS слал `venueId` (бэкенд его молча игнорировал — venue никогда не
     применялся) и читал несуществующие `p.date`/`p.venueName` вместо
     реальных `performanceDate`/`venue`. Новый UI — обычное текстовое поле.
  3. `WorkshopDetailDTO` не содержит `maxParticipants` вообще (значение
     есть в БД, но не читается назад через API) — задокументировано в UI
     явной подсказкой, не задача этой сессии чинить DTO.
  4. Роль `CONTENT_MANAGER` отсутствовала в select старого UI (реальных
     5 значений enum `Role`, было только 4) — добавлена.
  5. Teacher-дашборд в старом статическом сайте был вообще пустым файлом-
     заглушкой (`<html><body></body></html>`, ни одной строчки JS) — не
     "портирован", построен с нуля против реальных backend-эндпоинтов.
- **research** → **фикс в бэкенде** (см. отдельные записи выше в этом же
  файле от 2026-07-23): LR-006 (`GroupController` auth), LR-007 (reactivate
  + 500→403), `GroupService.update()` (startDateTime/endDateTime) — все
  найдены именно при подготовке этих страниц, не отдельно.
- **teacher-дашборд, известный компромисс** — нет FK между `User` и
  `Teacher` в данных; залогиненный TEACHER резолвит свою `Teacher.id` через
  email-match по `GET /teachers` (подтверждено заказчиком как временное
  решение 2026-07-23). Хрупко, если email когда-либо разойдутся между
  записями — постоянный FK не в этой сессии.
- **проверено вживую** (backend не поднят — те же ограничения, что и для
  личного дашборда): `svelte-check` 0 ошибок на 902 файлах, Vitest 12/12;
  в браузере — guard-логика всех направлений (unauthenticated→login,
  wrong-role→dashboard в обе стороны — USER от `/admin`, TEACHER от
  `/admin`, ADMIN пропускается) подтверждена вручную через подставленную
  `localStorage`-сессию на все 7 admin-страниц + teacher-дашборд, без
  ошибок в консоли ни на одной. Реальные CRUD-операции против живого API
  не проверены (нет локального backend+DB в этой сессии).
- **architect-reviewer: approve with changes** — независимая проверка
  нашла именно то, что клиентская проверка без реального backend не могла
  показать:
  1. **Блокирующее** — `getVenues()`, `getTeachers()`, `getGroups()` в
     `api.ts` использовали неавторизованный `request()` вместо
     `authRequest()` — эти эндпоинты требуют JWT (не в `permitAll`-списке
     `SecurityConfig`, хотя и без `@PreAuthorize` в контроллере), значит
     против реального бэкенда список groups и оба select'а (venue/teacher)
     всегда 401'или бы. Это ломало бы сам teacher-дашборд полностью —
     ключевую цель этой задачи. Исправлено (3 функции).
  2. **Should-fix** — редактирование группы позволяло менять workshop в
     select'е, хотя `GroupService.update()` его молча игнорирует (LR-009)
     — несогласованно с тем же паттерном, уже применённым для
     `maxParticipants` на странице Workshops (явная подсказка вместо
     тихого no-op). Исправлено: select теперь `disabled` при
     редактировании + explanatory note.
  Оба фикса применены, `svelte-check` 0/902, Vitest 12/12 повторно
  зелёные после фиксов.

## 2026-07-23 — fix: `GroupService.update()` не копировал `startDateTime`/`endDateTime`

### Область (`backend/src/main/java/com/be/service/GroupService.java`, `docs/tickets/tickets.md`)

- **fix (найдено при чтении сервиса перед построением admin-страницы
  Groups, не проактивным поиском)** — `update()` копировал `titleDe/En/Ua,
  capacity, activity, ageGroup, language, teacher, active`, но **не**
  `startDateTime`/`endDateTime` — редактирование расписания существующей
  группы молча ничего не делало. Добавлены оба поля. `workshop`-
  реассайнмент при редактировании осознанно НЕ добавлен — открытый вопрос
  про существующие enrollments, заведён `LR-009`, не решать по умолчанию.
- Ранее найденный при исследовании (research subagent) баг с
  `capacityLeft` сброшенным на каждый update — **проверено напрямую,
  оказался ложной тревогой**: `update()` вообще не трогает
  `capacityLeft`, что бы клиент ни прислал в payload, значение игнорируется
  сервисом. Не чинить то, чего нет — но полезно, что перепроверил перед
  тем как "чинить".
- `./gradlew compileJava` зелёный.

## 2026-07-23 — feat/fix: reactivate-эндпоинт + 500→403 фикс для всех `@PreAuthorize`-отказов (LR-007)

### Область (`backend/src/main/java/com/be/{domain/repository/UserRepository,service/UserService,web/controller/UserController,web/handler/GlobalExceptionHandler}.java`, `backend/src/test/java/com/be/web/controller/UserControllerTest.java`)

- **feat** — `PUT /api/v1/users/{userId}/reactivate` (ADMIN-only), симметрично
  существующему `DELETE` (soft-deactivate). Подтверждено заказчиком:
  добавить, не оставлять как read-only статус.
- **fix (найдено при написании теста на этот эндпоинт, не специфично для
  него)** — `GlobalExceptionHandler` не имел обработчика для
  `AuthorizationDeniedException`/`AccessDeniedException` — **любой**
  `@PreAuthorize`-отказ во всём приложении (аутентифицирован, не та роль)
  возвращался клиенту как 500 вместо 403. Добавлен явный
  `@ExceptionHandler`, 403. Это затрагивало все существующие защищённые
  эндпоинты, не только новый — реальный, довольно серьёзный баг обработки
  ошибок, обнаруженный случайно при тестировании.
- **test** — `UserControllerTest` (3 теста: ADMIN→200, USER→403,
  unauthenticated→401). Грабли по ходу: `jwt().jwt(j -> j.claim("role",
  "ADMIN"))` не прогоняет claim через `SecurityConfig`'s кастомный
  `JwtAuthenticationConverter` — тестовый `jwt()` строит `Authentication`
  напрямую, читает по умолчанию только "scope"/"scp". Нужно
  `.authorities(new SimpleGrantedAuthority("ROLE_..."))` явно — без этого
  оба тест-кейса (ADMIN и USER) получали `AuthorizationDeniedException`,
  что и вскрыло баг выше.
- Полный `./gradlew test`: 14 тестов, 1 fail (тот же
  pre-existing `BackendApplicationTests.contextLoads()` env-var-only —
  не регрессия).

## 2026-07-23 — security: `GroupController` write-методы без `@PreAuthorize` (LR-006)

### Область (`backend/src/main/java/com/be/web/controller/GroupController.java`, `docs/tickets/archive.md`)

- **security (найдено при исследовании перед таском "teacher/admin панели",
  не проактивным поиском уязвимостей)** — `POST/PUT/DELETE /api/v1/groups`
  не имели `@PreAuthorize` вообще, в отличие от всех пяти соседних
  контроллеров одного уровня (Activity/Workshop/Venue/Performance/Teacher).
  `SecurityConfig`'s `.anyRequest().authenticated()` не давал полностью
  анонимный доступ, но любой залогиненный обычный клиент мог
  создавать/менять/удалять группы занятий. Добавлен
  `@PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_OWNER')")` на все
  три метода, по образцу большинства соседей. `TEACHER` намеренно не
  добавлена — write-права преподавателя над своими группами (LR-ADR-004)
  ещё не спроектированы, это другая задача. `./gradlew compileJava` зелёный,
  тестов на этот контроллер не было (ничего не сломано).

## 2026-07-23 — feat/test: self-scoped payment history (LR-004, LR-ADR-016)

### Область (`backend/src/main/java/com/be/{web/controller/PaymentController,service/PaymentService,domain/repository/PaymentRepository}.java`, `backend/src/test/java/com/be/web/controller/PaymentControllerTest.java`, `docs/tickets/tickets.md`)

- **feat** — `GET /api/v1/payments/me`, `@PreAuthorize("isAuthenticated()")`,
  `userId` резолвится из JWT через `JwtAuthUtils.extractUserId()`, не
  принимается от клиента (нет `userId`-параметра — исключает IDOR по
  конструкции, не только по проверке). Закрывает требование "история
  платежей" личного дашборда (LR-ADR-016) — до этого `PaymentController`
  не имел вообще ни одного маршрута, доступного не-admin пользователю.
- **test** — `PaymentControllerTest` (`@WebMvcTest` + `spring-security-test`'s
  `jwt()` mock-принципал): happy path (пользователь получает свои платежи)
  и unauthenticated → 401. Первый `@WebMvcTest`-контроллер-тест в проекте —
  прецедентов не было (только `@SpringBootTest`/unit), пришлось решить два
  новых для этого репо инфраструктурных вопроса по ходу:
  1. `@MockBean` (`org.springframework.boot.test.mock.mockito.MockBean`)
     не совместим с AOT test-processing, которое здесь включено проектным
     `org.graalvm.buildtools.native` Gradle-плагином (`processTestAot`
     падал на `UnsupportedTypeValueCodeGenerationException` для
     `MockDefinition` — Spring Framework это в принципе не умеет
     код-генерировать под AOT). Фикс — новый `@MockitoBean`
     (`org.springframework.test.context.bean.override.mockito.MockitoBean`,
     официальная замена `@MockBean` начиная с Spring Boot 3.4+/Spring
     Framework 6.2), которая AOT поддерживает.
  2. `@WebMvcTest`-срез не поднимает `@ConfigurationProperties`-бины без
     явного включения — `WebMvcConfig` требует `CorsProperties`
     (`@ConfigurationProperties(prefix="cors")`, без `@Component`), что
     давало `NoSuchBeanDefinitionException` при старте контекста. Фикс —
     `@EnableConfigurationProperties(CorsProperties.class)` на тестовом
     классе рядом с `@Import(SecurityConfig.class)`.
  Оба фикса — только в тестовом классе, продакшн-код не тронут.
- **тикет** — заведён и сразу закрыт `LR-004` (`docs/tickets/tickets.md`).
- **продуктовое решение (заказчик, 2026-07-23)** — `note` **скрыт** от
  self-view: `PaymentMapper.toSelfViewDTO()` обнуляет поле перед отдачей в
  `/payments/me`, admin-эндпоинты (`toResponseDTO()`) не тронуты. Проверено
  по ERM — customer-facing заметки об оплате нигде не спроектированы,
  значит поле по умолчанию — admin/accounting reference. Покрыто
  `PaymentMapperTest` (2 теста: self-view без `note`, admin-view с `note`).

## 2026-07-23 — feat: личный дашборд (расписание + медиа + оплаты, LR-ADR-016)

### Область (`frontend-svelte/src/routes/dashboard/+page.svelte`, `frontend-svelte/src/routes/+layout.svelte`, `frontend-svelte/src/lib/api.ts`, `frontend-svelte/messages/{de,en,uk}.json`)

- **feat** — `/dashboard`: три секции (расписание — `GET /users/me/enrollments`,
  уже существовавший эндпоинт, ранее не использованный ни одной страницей;
  медиа — платежи — `GET /payments/me`, LR-004). Первая по-настоящему
  auth-gated страница проекта — клиентский guard (`isAuthenticated()` в
  `$effect`, редирект на `/login`, если нет сессии); `adapter-static` не
  даёт сделать это на сервере, только в браузере после mount (тот же
  паттерн, что уже был в `authRequest()`).
- **feat** — медиа-секция не имеет отдельного бэкенд-эндпоинта (не
  заводился — не нужен): собирается на клиенте из `WorkshopDetail.files`
  по уникальным `workshopId` из расписания пользователя, переиспользуя уже
  существующий `getWorkshop()`. Осознанный выбор — не расширять бэкенд
  ради страницы, которая и так уже получает нужные данные другим путём.
- **feat** — `+layout.svelte` стал auth-aware: `nav_login` заменяется на
  `nav_dashboard` + `nav_logout` (кнопка, вызывает `clearSession()`), когда
  `isAuthenticated()` истинен. До этого изменения залогиненный пользователь
  не имел в навигации пути к `/dashboard`, кроме редиректа сразу после
  логина (`login/+page.svelte`'s `redirectForRole`).
- **проверено вживую** (backend не поднят локально в этой сессии —
  проверка ограничена тем, что не требует реального API): `svelte-check`
  0 ошибок, 12/12 Vitest тестов; в браузере — неавторизованный `/dashboard`
  редиректит на `/login`; с подставленной в `localStorage` сессией guard
  пропускает, страница показывает корректный error-state (нет реального
  бэкенда — fetch падает, поймано `.catch()`, без необработанных ошибок в
  консоли); nav верно переключается Login ⇄ Mein Bereich/Abmelden; logout
  корректно чистит сессию и возвращает на главную. Полная проверка с
  реальными данными (расписание/медиа/оплаты) ждёт поднятого backend+DB —
  не входит в объём этой сессии.
- **architect-reviewer (2026-07-23): approve as-is**, весь батч (payments/me
  + дашборд) проверен независимо — реально прогнаны тесты и `svelte-check`,
  не просто поверено на слово. Единственное non-blocking замечание:
  `PaymentMapper.toSelfViewDTO()` — блок-лист (строит полный admin DTO и
  обнуляет `note`), не allow-лист; безопасно сегодня для одного поля, но
  любое новое admin-only поле в `Payment`/`PaymentResponseDTO` в будущем
  утечёт в `/payments/me`, если про него забудут здесь же обнулить. Не
  блокирует этот тикет, зафиксировано на будущее — см. `LR-005` ниже.

## 2026-07-22 — feat/security: 11 новых страниц frontend-svelte, найден и закрыт реальный 401-баг

### Область (`frontend-svelte/src/routes/{impressum,datenschutz,agb,widerruf,activities,workshops,workshops/[id],performances,contact,corporate,feedback}/`, `frontend-svelte/src/lib/{api.ts,components/{Textarea,Select}.svelte}`, `backend/.../config/SecurityConfig.java`)

- **feat** — 11 страниц (юр-страницы с реальными данными Olena; каталог
  Activities/Workshops/Workshop-detail/Performances с реальной интеграцией
  под `api.ts`; Contact/Corporate/Feedback). Все проверены: `svelte-check`
  0 ошибок, 12/12 Vitest тестов, живой рендер в браузере без console-ошибок.
- **fix (найден при портировании, не выдуман)** — старый `feedback.js` слал
  `{feedbackType, subject, message, email}`, реальный
  `FeedbackRequestDTO.java` принимает только `{content, rating}` — форма
  фидбека была рассинхронизирована с бэкендом уже в старом фронтенде.
  Новая форма шлёт правильный контракт.
- **security (найдено architect-reviewer, не мной)** — `SecurityConfig.java`
  разрешал `GET` без авторизации только для `/api/v1/workshops/**` — для
  `/api/v1/activities/**` и `/api/v1/performances/**` такого правила не
  было, хотя оба контроллера рассчитаны на публичный доступ. Значит
  страницы Activities и Performances **всегда** получали бы 401 для любого
  анонимного посетителя — то есть были нефункциональны для всей своей
  целевой аудитории. Добавлены оба `permitAll()`-матчера рядом с
  workshops, бэкенд пересобран (`compileJava` зелёный).
- **refactor** — вынесены `Textarea`/`Select` в `src/lib/components/` (было
  начавшееся дублирование стилей `Input.svelte` в `feedback`-форме — по
  находке ревью, до третьей копипасты, не после).
- **note** — `api.ts`: `WorkshopDetail` дополнен полем `files` (было
  упущено, реальный `WorkshopDetailDTO` его содержит) — не используется
  пока нигде, добавлено заранее для личного кабинета (LR-ADR-016).
- **найдено, не исправлено (отдельный ticket needed)** — `npm run build`
  у `frontend-svelte` не проходит: `adapter-static` требует `prerender`
  на каждый роут, ни один (включая уже одобренные Home/About/Login) этого
  не объявляет — существовало и до сегодняшних 11 страниц, не регрессия
  этой сессии, но раз обнаружено — фиксирую здесь, привязать к LR-002
  (CI всё равно ещё не собирает `frontend-svelte`, тот же блокер).

> Вставляй последние 10 записей в контекст AI при работе с затронутыми областями.
> Это файл генезиса проекта — фактические изменения и "грабли" (ошибочные
> предположения + их фикс), чтобы не наступать повторно. Не дублировать
> сюда обычные тикеты — им место в `docs/tickets/tickets.md`.

## 2026-07-22 — feat/refactor: инженерный каркас frontend-svelte (LR-ADR-020)

### Область (`frontend-svelte/{src/routes/layout.css,src/lib/components/,src/lib/api.ts,src/routes/login/+page.svelte,vite.config.ts,package.json,tsconfig.json}`, `docs/architecture/decisions.md`, `docs/tickets/tickets.md`)

- **refactor** — `architect-reviewer` поймал, что 2 из 5 пунктов
  предложенного плана уже были сделаны (дизайн-токены в `layout.css`
  `@theme`, API-слой в `api.ts`) — не стал молча соглашаться с
  переформулировкой, скорректировал план перед тем, как писать код.
- **fix** — реальный баг из ревью: `login`-форма использовала
  `--color-gold` и для CTA-кнопки, и для текста ошибки. Добавлены
  семантические `--color-error`/`--color-success`, отдельные от
  брендовых акцентов.
- **refactor** — извлечены `Button`/`Input`/`Card`/`ErrorText` в
  `src/lib/components/` — устранило 8-кратное дублирование `<label>`/
  `<input>` разметки в `login/+page.svelte`.
- **feat** — Vitest + `@testing-library/svelte` + `@testing-library/
  jest-dom` подключены, 12 тестов на новые примитивы — **реально
  прогнаны** (`npm run test`), не просто написаны. Playwright сознательно
  НЕ установлен — нет CI-стадии, которая бы его запускала, и нет
  реального бэкенда рядом для e2e (см. LR-002).
- **feat** — `api.ts`: `authRequest`/`getCurrentUser` — Bearer-токен +
  401-обработка (clearSession + редирект на `/login`), паттерн для
  Волны 2 (личный кабинет, роль "Преподаватель"), решён один раз заранее.
- **compliance** — цель доступности WCAG 2.1 AA зафиксирована явно
  (`ErrorText` уже несёт `role="alert"`).

### Грабли (три отдельных, все настоящие, не выдуманные)

1. **Vitest резолвил Svelte-компоненты в SSR/server-режиме** —
   `mount(...) is not available on the server`. Фикс — `resolve:
   {conditions: ['browser']}`, **строго только под `process.env.VITEST`**
   (глобально сломало бы реальный `vite build`, которому нужна server-
   сборка для prerendering).
2. **`defineConfig` из `vitest/config` конфликтовал по типам** с версией
   `vite`, которую использует этот проект (несовпадение internal Plugin
   type между `vitest`'ным `vite` и установленным) — `svelte-check` падал
   с гигантской ошибкой типов, при этом тесты рантаймово работали
   нормально. Фикс — официально рекомендованный Vitest-паттерн:
   `defineConfig` из `vite` и из `vitest/config` раздельно + `mergeConfig`,
   вместо одного `defineConfig` из `vitest/config`.
3. **`@testing-library/svelte` не делает auto-cleanup между тестами** (в
   отличие от React-версии) — без explicit `afterEach(() => cleanup())` в
   `vitest-setup.ts` рендеры из разных тестов накапливались в одном
   `document.body`, ловилось как "Found multiple elements with role
   X" — не баг компонентов, баг отсутствующей настройки тестового
   окружения.
- **verified live** — визуально подтверждено в браузере: `getComputedStyle`
  на реальном alert-сообщении вернул `rgb(226, 87, 76)` — ровно
  `--color-error`, не `--color-gold`.

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

