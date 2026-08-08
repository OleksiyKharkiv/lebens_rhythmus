# KNOWN_ISSUES.md — Lebens Rhythmus

> Always-loaded (см. CLAUDE.md). Держать коротким — это список конкретных
> "не наступай сюда снова" вещей, не общий баг-трекер (для этого —
> docs/tickets/tickets.md).

---

## Инфраструктура

**Сеть VM после power outage не поднимается сама.** Все VM кроме, возможно,
уже пропатченных через `rc.local` — теряют сетевой интерфейс после грязного
шатдауна хоста. Recovery — см. `docs/ops/infra-fix-shutdown.md`, не изобретать
заново.

**NAT-правило на gateway-core (VM100) не персистентно.** После ребута
слетает `iptables -t nat POSTROUTING MASQUERADE` для `10.10.10.0/24` →
внутренние VM теряют интернет → падают image pull в k3s. Нужно
`iptables-persistent`, TODO — ещё не сделано на момент записи.

**`caddy.service` на VM100 не используется, но существует.** Реальный прокси
— `cloudflared` (Cloudflare Tunnel). Не путать, не пытаться чинить caddy как
источник проблем с сайтом — проверяй `cloudflared` и `config.yml` в
`/etc/cloudflared/`.

**GitLab registry token истекает молча.** Симптом — `ImagePullBackOff` во
всех подах `lr-dev` одновременно после долгого простоя. Проверять
`kubectl get secret gitlab-registry -n lr-dev` в первую очередь при массовых
`ImagePullBackOff`.

**MetalLB не назначает ExternalIP для Traefik.** Не полагаться на
`10.10.10.100` (LoadBalancer IP) — он никому не назначен. Реальный путь —
`10.10.10.7:30080` (Traefik NodePort на VM700).

**Применение `NetworkPolicy` default-deny в `lr-dev` по одному файлу за
раз (baseline отдельно, потом ingress-allow для backend/frontend отдельным
шагом) вызвало реальный, хоть и короткий, простой прод-сайта** (LR-031
Phase 2, 2026-08-08). `lr-default-deny-all` блокирует ВЕСЬ ingress в
namespace сразу, включая от Traefik — правило "разрешить ingress от
Traefik" лежит в отдельных файлах (`networkpolicy-backend.yaml`/
`networkpolicy-frontend.yaml`). Между применением deny-all и применением
allow-правил образуется окно (пока человек читает следующий шаг
runbook'а и печатает команду — не миллисекунды, а реальное время), в
течение которого сайт снаружи недоступен. **Откат:** `kubectl delete
networkpolicy -n lr-dev --all` — мгновенно возвращает namespace к
open-by-default, безопасно. **Правильный порядок (закреплён в
`docs/runbooks/infra-fix-shutdown.md` "LR-031 Phase 2"):** baseline
(deny-all + DNS-allow) и все ingress-allow правила, нужные для базовой
доступности сайта (`networkpolicy-backend.yaml`, `-frontend.yaml`,
`-postgres.yaml`), применять ОДНИМ `kubectl apply -f -` с
multi-document YAML, не по файлу за раз с ручной паузой на проверку
между ними — верификация (включая негативный тест) идёт уже после того,
как весь необходимый для работы сайта набор правил живёт одновременно.
Тот же общий урок, что и с ownership-багами в numi: shell-скрипт/runbook,
который переводит систему через промежуточное несогласованное
состояние, а не атомарно к целевому — сам является источником инцидента,
не только код, который он применяет. **Повторное применение исправленной
процедурой прошло без инцидента**, негативный тест подтвердил M4
закрытым (см. ниже).

**Побочная находка при верификации того же тикета: `busybox nc -zv`'s
verbose-вывод при неудачном подключении может быть ПУСТЫМ, и `kubectl
wait --for=jsonpath='{.status.phase}'=Failed` может не сработать, если
команда обёрнута в `sh -c '...; echo ...'`** (последняя команда в
цепочке — `echo`, она и определяет итоговый exit code/phase контейнера,
даже если реальная проверяемая команда до неё провалилась). Не
полагаться ни на текст вывода `nc -v`, ни на `phase` пода как на
единственное доказательство — читать exit code явно
(`echo "EXITCODE=$?"` в тот же `sh -c`, смотреть в `kubectl logs`).

---

## Backend

**Хеширование пароля при регистрации живёт ТОЛЬКО в `UserService.createUser()`
(строка `user.setPassword(passwordEncoder.encode(...))`, с проверкой на
`null`), не в `AuthService.register()`.** Это уже один раз ломали дважды:
изначально хеширование дублировалось в обоих местах (двойной `encode` →
логин не работал), это поправили 2025-12-26 (commit `a1ef361d`, "delete
duplicated password encoding") удалением вызова из `AuthService`. При
калибровочной инвентаризации 2026-07-20 закомментированная строка в
`AuthService.register()` была по ошибке прочитана как "хеширование вообще
отсутствует" и **чуть не была возвращена обратно** — что снова сломало бы
логин всем новым пользователям. Обнаружено и остановлено `architect-reviewer`
до коммита. **Если меняешь логику пароля — трогай только `UserService`,
`AuthService`/`UserMapper` должны передавать raw-пароль дальше, не
хешировать его сами.**

**`DatabaseFixConfig` меняет схему БД в рантайме при каждом старте
приложения** (`ALTER TABLE workshop_groups DROP NOT NULL` на нескольких
колонках) вместо SQL-миграции. Это означает: реальная схема таблицы может
не совпадать с тем, что видно в миграциях (если они вообще есть — TODO
проверить). При любой задаче, трогающей `workshop_groups`, сначала
проверить актуальную схему через `\d workshop_groups` в psql, не доверять
только файлам миграций.

---

**После обновления Docker Desktop до версии с Docker Engine v29+ (4.52+),
`Testcontainers`-тесты (`BackendApplicationTests`) падают с `Status 400:
client version 1.32 is too old. Minimum supported API version is 1.40`.**
Не баг кода — `docker-java`, зашитый в Testcontainers 1.x (версия,
которую тянет Spring Boot 3.5.7 через dependency management), хардкодит
откат на API 1.32 вместо согласования с демоном, а новый Engine v29
жёстко требует ≥1.40 и не откатывается. Известный баг апстрима
(testcontainers-java issue #11210/#11235). **Фикс, который сработал:**
`backend/src/test/resources/docker-java.properties` с `api.version=1.44`
— пропускает сломанное согласование версии. Настоящий фикс (не
применялся, крупнее по риску) — апгрейд Testcontainers до 2.0.2+.

## Как пополнять этот файл

Добавляй сюда только вещи, которые: (а) уже один раз стоили времени на
диагностику, (б) не очевидны из чтения кода, (в) вероятны повторно. Не
дублируй сюда обычные тикеты/баги — им место в `docs/tickets/tickets.md`.
