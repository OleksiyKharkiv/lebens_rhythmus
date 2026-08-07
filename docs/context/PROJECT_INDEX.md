# Lebens Rhythmus — PROJECT_INDEX
> Версия: 0.1.0 (draft) · Обновлён: 2026-07-20 · На основе инфра-recovery сессии
> **Назначение:** вставляй этот файл в начало каждого нового чата с AI.
> Заменяет объяснения и предотвращает галлюцинации.
>
> ⚠️ Многое здесь помечено `TODO` — составлено без прямого доступа к
> репозиторию, только по данным, собранным во время recovery-сессии
> (логи, kubectl, конфиги). Первый же реальный тикет должен пройти через
> `architect-reviewer`, который сверит инварианты §5 с реальным кодом.

---

## 1. Что такое Lebens Rhythmus (30 слов)

Студия творческого развития детей, подростков и взрослых (tlab29.com):
театр, танец, гимнастика и другое. Веб-платформа: запись на занятия,
управление группами/расписанием, аутентификация пользователей. Проект
рабочий, но сырой — берём на поддержку и развитие "на поруки".

---

## 2. Stack

| Слой | Технология | Версия | Источник данных |
|------|-----------|--------|------------------|
| Backend | Spring Boot | 3.5.7 | подтверждено логами старта |
| Язык | Java | 21 | подтверждено логами старта |
| Backend package | `com.be` | — | подтверждено (`com.be.BackendApplication`) |
| DB | PostgreSQL | 16.9 | подтверждено (Hibernate connection log) |
| ORM | Hibernate / Spring Data JPA | 6.6.33.Final | подтверждено |
| Auth | Spring Security + JWT | — | подтверждено (`JwtUtils`, `customUserDetailsService`), expiry 86400s (24h) |
| Frontend | Статический HTML/CSS/JS через nginx | nginx 1.29.5 | подтверждено (Server header) |
| Оркестрация | k3s (Kubernetes) | v1.33.5+k3s1 | подтверждено |
| Ingress | Traefik | NodePort :30080/:30443 | подтверждено |
| Внешний доступ | Cloudflare Tunnel (cloudflared) | tunnel `3d689e65-9ef0-40f9-9d41-f6888b40741c` | подтверждено, порты 80/443 закрыты намеренно |
| CI/CD | GitLab CI | gitlab.com/okh3/lebens_rhythmus | подтверждено |
| Registry | GitLab Container Registry | registry.gitlab.com | подтверждено |
| Namespace (k8s) | `lr-dev` | — | подтверждено |

**TODO:** build tool (Maven/Gradle), фронтенд-инструментарий (если появится
bundler), лицензионная политика зависимостей (в numi — только MIT/Apache/BSD/MPL,
для LR ещё не зафиксировано — обсудить и внести).

---

## 3. Файловая карта (FILE MAP)

> ⚠️ Собрана частично — по данным логов и kubectl, НЕ по реальному листингу
> репозитория. Дозаполнить при первом Claude Code сеансе с доступом к коду.

```
lebens_rhythmus/
├── backend/                          # TODO: подтвердить путь
│   └── src/main/java/com/be/
│       ├── BackendApplication.java   # Entrypoint (подтверждено логами)
│       └── config/
│           ├── DatabaseFixConfig.java  # ⚠️ см. KNOWN_ISSUES — runtime ALTER
│           │                            #   COLUMN DROP NOT NULL вместо миграции
│           └── JwtUtils.java           # JWT init, expiration 86400s
│       # TODO: controller/, service/, repository/, entity/ (или domain/) —
│       #   реальные имена пакетов не подтверждены, только предполагаются
│       #   по Spring Boot конвенции
│
├── frontend/                         # TODO: подтвердить путь
│   ├── index.html                    # подтверждено (главная, DE-текст)
│   ├── login/login.html              # подтверждено (ссылка с главной)
│   ├── about/about.html              # подтверждено (ссылка с главной)
│   ├── styles/main.css               # подтверждено
│   ├── js/main.js                    # подтверждено
│   ├── js/partials-loader.js         # подтверждено
│   └── assets/favicon_io/            # подтверждено
│
├── k8s/ (или helm chart)             # TODO: подтвердить структуру
│   # namespace lr-dev, деплоймент управляется через helm — видны релизы
│   # sh.helm.release.v1.lr-dev.v222..v231 в secrets, т.е. Helm-based deploy
│
├── docs/
│   ├── context/                      # ← ТЫ ЗДЕСЬ
│   │   ├── PROJECT_INDEX.md          # Этот файл
│   │   ├── CODING_PROTOCOL.md
│   │   ├── KNOWN_ISSUES.md
│   │   └── CHANGELOG.md              # TODO: создать
│   ├── ops/
│   │   └── infra-fix-shutdown.md     # Recovery runbook (Proxmox/k3s/CF Tunnel)
│   └── architecture/                 # TODO: создать по мере накопления ADR
│
└── .claude/
    ├── agents/architect-reviewer.md
    ├── hooks/check-forbidden-patterns.sh
    ├── launch.json
    └── settings.local.json
```

---

## 4. Известные сущности БД (DB ENTITIES — подтверждено частично)

Из `workshop_groups` (через `DatabaseFixConfig`, см. KNOWN_ISSUES):

```
workshop_groups
  ├── activity_id      → FK, вероятно Activity (вид занятия: театр/танец/...)
  ├── age_group_id     → FK, вероятно AgeGroup (возрастная группа)
  ├── language_id      → FK, вероятно Language (язык проведения)
  └── teacher_id       → FK, вероятно Teacher (преподаватель)
```

Также подтверждено по коду: таблица `users` с ролями (Spring Security,
`customUserDetailsService`) — структура ролей не подтверждена.

**TODO:** полная ER-модель. Скорее всего есть также: Student/Participant,
Booking/Enrollment (запись на группу), возможно Payment. Составить по
реальным JPA-сущностям при первом заходе в код.

---

## 5. Архитектурные инварианты (CONSTRAINTS — ПРЕДПОЛАГАЕМЫЕ, не верифицированы)

По умолчанию Spring Boot слоистая архитектура — предполагаем, пока
`architect-reviewer` не сверит с реальным кодом:

```
ARCH-1  entity/ (JPA) не содержит бизнес-логики сверх простых инвариантов,
        не знает о DTO/HTTP
ARCH-2  service/ не импортирует servlet/HTTP-специфику напрямую, работает
        через repository-интерфейсы (Spring Data JPA)
ARCH-3  controller/ — parse (@RequestBody) → validate (@Valid) → call
        service → respond (ResponseEntity), без бизнес-логики в контроллере
ARCH-4  Зависимости только внутрь: controller → service → repository → entity
```

**Красный флаг:** до подтверждения через architect-reviewer, любое MED/HIGH
изменение должно явно сверяться с реальной структурой пакетов, а не
предполагать эту схему как факт.

---

## 6. Известный технический долг (см. также KNOWN_ISSUES.md)

```
DEBT-1  DatabaseFixConfig делает ALTER TABLE DROP NOT NULL в рантайме при
        каждом старте приложения вместо нормальной SQL-миграции —
        подозрительно, нужно разобраться и заменить на миграцию
        (Flyway/Liquibase — TODO уточнить, что используется)
DEBT-2  caddy.service остался в системе VM100 неиспользуемым (заменён на
        cloudflared) — засоряет — удалить/задокументировать почему оставлен
DEBT-3  MetalLB не назначает ExternalIP для Traefik — LoadBalancer сервисы
        не работают, используется NodePort как обходной путь
DEBT-4  Нет qemu-guest-agent на VM100/200/300 — усложняет recovery
        (см. docs/ops/infra-fix-shutdown.md)
DEBT-5  GitLab registry token — не автоматизировано обновление, истекает
        молча и роняет image pull в кластере
```

---

## 7. API маршруты

**TODO — не подтверждено.** Известно только:
```
GET  /actuator/health     — Spring Boot Actuator health check (подтверждено логами)
POST /auth/... (?)        — вероятно есть login endpoint (JwtUtils подтверждён)
```
Заполнить по `@RequestMapping`/`@GetMapping`/`@PostMapping` в контроллерах
при первом реальном заходе в код.

---

## 8. Инфраструктурные ADR (подтверждено по факту эксплуатации)

| ID | Решение | Причина |
|----|---------|---------|
| INFRA-ADR-001 | Cloudflare Tunnel, не открытые порты 80/443 + Caddy | Безопасность: нет прямого входящего доступа к хосту; caddy остался неактивным |
| INFRA-ADR-002 | k3s (не docker-compose/голый Kubernetes) | Лёгкий вес для homelab, HA не требуется |
| INFRA-ADR-003 | GitLab CI + приватный registry, не GitHub Actions | Единая экосистема с репозиторием |
| INFRA-ADR-004 | PostgreSQL, не SQLite/MySQL | Подтверждено по факту (в отличие от numi, где SQLite — сознательный offline-first выбор) |
| INFRA-ADR-005 | Traefik NodePort вместо MetalLB LoadBalancer | Временный обход — MetalLB не назначает ExternalIP (DEBT-3), не финальное решение |

**TODO — бизнес/код ADR (по аналогии с numi ADR-001..007):** выбор
Spring Boot vs альтернатив, статический frontend vs SPA-фреймворк,
лицензионная политика зависимостей и т.д. — зафиксировать по мере принятия
явных решений, не ретроактивно выдумывать.

---

## 9. Compliance — предварительная пометка (не юридическая консультация)

Проект работает с данными о детях (возрастные группы, вероятно ФИО,
контакты опекунов, может быть медицинские заметки для физических занятий).
Это выше обычного уровня чувствительности персональных данных по DSGVO
(Art. 8 — согласие для несовершеннолетних, Art. 9 — данные о здоровье).

**TODO:** формализовать это в `docs/compliance/DATENSCHUTZ.md` по аналогии
с `docs/compliance/COMPLIANCE.md` у numi — но с реальным юридическим
консультантом, не додумывать самостоятельно. До этого момента — CODING_PROTOCOL
§4 работает как чеклист-заглушка, помечающая когда стоит насторожиться,
не как подтверждённое юридическое требование.

---

## 10. Архитектура безопасности

Полный синтезирующий обзор (что реализовано, как и почему, состояние
мониторинга/alerting) — **`docs/security/ARCHITECTURE.md`**. Живой
документ, обязателен к обновлению при любом изменении в системе
безопасности (правило закреплено в самом файле и в `CLAUDE.md`). Для
конкретных находок аудита — `docs/security/audit-2026-08-06.md`, для
плана по фазам — `docs/security/roadmap.md`, для обоснования отдельных
решений — `docs/architecture/decisions.md`.

---

## 11. Окружение разработки

```
OS:             Windows 11 (по аналогии с numi, тот же IdeaProjects)
IDE:            IntelliJ IDEA
Путь:           C:\Users\hudos\IdeaProjects\lebens_rhythmus
Backend port:   8080 по умолчанию Spring Boot (⚠️ ПРОВЕРИТЬ конфликт: numi
                 использует 8090 именно из-за занятого 8080 WSL mini-chronos —
                 при параллельной работе над обоими проектами возможен
                 конфликт портов, задать LR_BACKEND_PORT явно если нужно)
DB:             PostgreSQL — TODO: локальный инстанс или туннель к prod?
                 (в проде: lr-postgres в namespace lr-dev, НЕ трогать
                 напрямую без крайней необходимости)
JWT_SECRET:     TODO — узнать откуда берётся локально (env var? application.yml?)
```

---

## 12. Глоссарий (черновик — DE-термины с сайта)

| Термин | Значение |
|--------|---------|
| Workshop / Kurs | Занятие/курс (theater, dance, gymnastics etc.) |
| Workshop group | Группа на конкретное занятие (см. §4 сущности) |
| Age group (Altersgruppe) | Возрастная категория участников |
| Teacher (Kursleiter) | Преподаватель/ведущий |
| Anmeldung | Запись/регистрация на занятие (предположительно) |

**TODO:** дополнить по мере погружения в код и общения с владельцем продукта.
