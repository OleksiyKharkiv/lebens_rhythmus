# INFRA-LR.md — LebensRhythmus (tlab29.com) — индекс инфраструктуры

> Версия: 1.0 · Создан: 2026-06-21 · Контекст: после инфра-сессии восстановления
> сети (NAT, node-ip, flannel-backend conflict).
> Этот файл — единая точка входа при следующей работе над LR-инфрой, CI/CD,
> авторизацией Spring или бэкапами Postgres. Загружай в начало нового чата
> вместе с `PROJECT_INDEX.md` (Numi) когда работаешь на стыке двух проектов.

---

## 1. Что такое LR / tlab29.com (30 слов)

Театральная/творческая лаборатория. Java Spring backend + статический frontend.
PostgreSQL как БД. Развёрнуто в k3s (namespace `lr-dev`) на домашнем сервере.
Первый и единственный пока пользователь — Olena, реальные данные минимальны.

---

## 2. Топология деплоя

```
GitLab (okh3/lebens_rhythmus)
  │  git push → pipeline
  ▼
GitLab CI (.gitlab-ci.yml)
  stage: build    → gradle bootJar (backend)
  stage: package  → docker build+push (backend, frontend) → registry.gitlab.com
  stage: deploy   → helm upgrade --install lr-dev ./devops/helm/lr-app
  stage: smoke    → curl api.tlab29.com/actuator/health, curl tlab29.com
  │
  │  tags: [lr-runner]  ← self-hosted GitLab runner на VM200 (mgmt-core)
  ▼
k3s cluster (kubeconfig из VM200, namespace lr-dev)
  ├── lr-backend   Deployment  (Spring Boot, port 8080)
  ├── lr-frontend  Deployment  (static, port 80)
  ├── lr-postgres  StatefulSet (postgres:16-bullseye, PVC local-path)
  ├── lr-backend / lr-frontend / lr-postgres  Services (ClusterIP)
  └── lr-ingress   Ingress (traefik, host tlab29.com / api.tlab29.com)
  │
  ▼
Traefik (namespace traefik, NodePort 30080/30443)
  │
  ▼
cloudflared (VM100 gateway-core, systemd service)
  │  CF Tunnel → origin = Traefik NodePort на одной из k3s-нод
  ▼
Cloudflare → tlab29.com (публичный домен)
```

**Узлы k3s-кластера:**
| Нода | VM | Роль | INTERNAL-IP |
|------|----|----|----|
| mgmt-core | VM200 | control-plane, master, GitLab runner | 10.10.10.2 |
| gateway-core | VM100 | worker, ТАКЖЕ шлюз (cloudflared, NAT) | 10.10.10.1 |
| lr700 | VM700 | worker, основная нода для LR (nodeSelector `project: lr`) | 10.10.10.7 |
| workout-evo | VM300 | worker, NotReady (намеренно stopped) | 10.10.10.3 |

⚠️ **Архитектурная странность, зафиксированная, не переделываем сейчас:** `gateway-core`
одновременно и шлюз (NAT, cloudflared), и k3s worker-нода. Это исторически сложилось,
создаёт риски (см. §5). Кандидат на пересмотр при переезде на новое железо (сентябрь).

---

## 3. Helm-чарт `devops/helm/lr-app`

```
devops/helm/lr-app/
├── Chart.yaml
├── values.yaml              # global.imageRegistry/imageTag заданы из CI через --set
├── templates/
│   ├── backend-deployment.yaml   # initContainer wait-for-db + main container
│   ├── backend-service.yaml      # ClusterIP :80 → :8080
│   ├── frontend-deployment.yaml
│   ├── frontend-service.yaml     # ClusterIP :80 → :80
│   ├── pg-statefulset.yaml       # PVC local-path, listen_addresses=*
│   ├── pg-service.yaml           # ClusterIP :5432
│   ├── ingress.yaml              # traefik, tlab29.com + api.tlab29.com
│   └── flannel-patch.yaml        # ⚠️ ПЕРЕНЕСЁН В ЧЕРНОВИКИ 2026-06-21, см. §6.3
└── (traefik-values.yaml живёт отдельно, deploy-traefik job закомментирован в CI)
```

**Ключевые ENV переменные backend (из `values.yaml` / Helm-templates):**
- `SPRING_DATASOURCE_URL=jdbc:postgresql://lr-postgres:5432/lebens_rhythmus` — короткое
  DNS-имя сервиса, резолвится CoreDNS автоматически в рамках одного namespace.
  **Это конфиг корректный** — не менять на FQDN, см. разбор бага в §6.
- `SPRING_DATASOURCE_USERNAME/PASSWORD` — из Secret `lr-db-credentials` (namespace `lr-dev`)
- `JWT_SECRET`, `JWT_EXPIRATION=86400000` — передаются из CI через `--set`

**Секреты:**
- `lr-db-credentials` (Secret, namespace `lr-dev`) — `username`/`password`, использует
  и `lr-postgres` StatefulSet, и `lr-backend` Deployment. Создан 2025-11-03, не менялся.
- `gitlab-registry` (imagePullSecrets) — токен для pull из `registry.gitlab.com`.
  **ToDo:** ротация ручная, см. INFRA-006 в `infra-backlog.md`.
- `tls-secret` — TLS для Ingress (tlab29.com, api.tlab29.com).

---

## 4. DNS внутри k3s — как это устроено и почему ломалось

CoreDNS (`kube-system`, Service `kube-dns` на `10.43.0.10`) резолвит:
- короткое имя `lr-postgres` → работает **только в рамках того же namespace** (`lr-dev`),
  стандартная фича Kubernetes Service Discovery.
- FQDN `lr-postgres.lr-dev.svc.cluster.local` → работает из любого namespace.

`/etc/resolv.conf` внутри каждого пода (инжектится kubelet автоматически):
```
search lr-dev.svc.cluster.local svc.cluster.local cluster.local
nameserver 10.43.0.10
options ndots:5
```

`ndots:5` — если имя содержит меньше 5 точек, резолвер сначала пробует дописать
`search`-домен, и только потом — абсолютное имя. Источник лишних `NXDomain` в логах
(безобидно, просто шум).

**Замечено и не до конца объяснено:** Java/JVM резолвер (`InetAddress`/`sun.nio.ch`)
не всегда ведёт себя как glibc-резолвер с `search`-доменами — в инциденте 2026-06-21
JVM выдавал `UnknownHostException: lr-postgres` (без попытки доставить через search),
тогда как glibc-утилиты (`psql`) с тем же именем зависали по другой причине. **Тогда
причина была не в Java и не в DNS, а в реальном сетевом сбое (см. §6), но сам факт
разного поведения резолверов в JVM vs glibc — наблюдение для будущей отладки.**

---

## 5. Cloudflare Tunnel — как проксируется tlab29.com

`cloudflared` — systemd service на **VM100 (gateway-core)**, конфиг `/etc/cloudflared/config.yml`.
Tunnel слушает за CF, проксирует на origin = Traefik NodePort (`:30080`/`:30443`) на
какой-то из k3s-нод (нужно свериться какой именно адрес прописан в config.yml — не
зафиксировано в этом файле, **ToDo: задокументировать точный origin-адрес**).

**Архитектурный риск:** `gateway-core` одновременно отвечает за NAT для всего VLAN200
и за cloudflared. Если на этой же ноде живут k3s поды (а она ещё и worker-нода) —
любая нестабильность kube-router/flannel на ней может аффектить и интернет для
остальных VM, не только LR. Зафиксировано как наблюдение для круглого стола.

**Симптом «502 Bad Gateway» от Cloudflare** = `cloudflared` жив, но не может достучаться
до origin (Traefik). Чаще всего значит: Traefik под не Running, или сеть между
cloudflared и Traefik разорвана (см. инцидент §6).

---

## 6. Инцидент 2026-06-21 — полный разбор (для будущего быстрого узнавания паттерна)

### Симптомы
- `tlab29.com` → CF возвращает `502 Bad Gateway`
- `kubectl get pods -A` → `traefik`, `metallb-controller`, `lr-frontend` все `ImagePullBackOff`
- `lr-backend` → `Init:0/1` зависает навечно на `wait-for-db` (`pg_isready` не отвечает)
- `kubectl` без `--kubeconfig` → `x509: certificate signed by unknown authority`

### Корни (было ТРИ независимых наложившихся друг на друга проблемы)

**6.1 — NAT отсутствовал на gateway-core.** `INFRA-003` из бэклога был не сделан.
Ни одна k3s-нода не имела выхода в интернет → все `ImagePullBackOff`.
Фикс: `iptables -t nat -A POSTROUTING -s 10.10.10.0/24 -o enp6s18 -j MASQUERADE`
+ systemd unit `numi-nat.service` чтобы пережил рестарт k3s/flannel (форсирует
правило After=k3s.service).

**6.2 — gateway-core зарегистрирован в кластере по внешнему IP.** k3s-agent на
`gateway-core` объявил себя нодам по `192.168.0.54` (внешний/br0) вместо `10.10.10.1`
(внутренний VLAN200) — видимо взял первый найденный интерфейс. Это ломало
pod-to-pod трафик (flannel VXLAN строит туннели по `INTERNAL-IP` ноды) для всего,
что шедулилось на этой ноде, включая CoreDNS.
Фикс: явный `node-ip: 10.10.10.1` в `/etc/rancher/k3s/config.yaml` на VM100 +
restart `k3s-agent.service`.

**6.3 — НАСТОЯЩИЙ корень, найденный последним: `flannel-patch.yaml` в Helm-чарте LR.**
Чарт `lr-app` содержал скрытый Job, который при **каждом** `helm upgrade --install`
насильно патчил **общий, кластерный** ConfigMap `kube-flannel-cfg`, переключая
`Backend.Type` между `vxlan` и `host-gw`, и затем удалял все flannel-поды кластера
(`kubectl -n kube-flannel delete pod --all`).

Это создавало гонку состояний: DaemonSet-поды flannel пересоздавались с зашитыми
старыми параметрами (в т.ч. зомби-lease записи с устаревшим `PublicIP` ноды от
ДО фикса 6.2), пока ConfigMap уже содержал другое значение backend. Результат —
непредсказуемое поведение сети: DNS то резолвился, то зависал, в зависимости от
того какая lease-запись «побеждала» в конкретный момент. Это объяснило ВСЮ
непоследовательность диагностики в течение вечера (psql зависал, busybox-тесты
проходили, разные поды вели себя по-разному).

**Диагностический отпечаток:** `kubectl logs -n kube-flannel -l app=flannel | grep BackendType`
показывает **дублирующиеся lease-записи для одной подсети с разным `BackendType`**
(`vxlan` и `host-gw` для одного и того же `PublicIP`).

**Фикс:**
1. `kubectl patch -n kube-flannel cm kube-flannel-cfg --type merge -p '...Backend.Type=vxlan...'`
   (зафиксировали vxlan как единственный источник правды)
2. `kubectl delete pod -n kube-flannel --all` (пересоздание с чистого ConfigMap)
3. `flannel-patch.yaml` **вынесен из Helm-чарта LR в черновики** (2026-06-21) —
   общая кластерная инфраструктура не должна мутироваться чартом одного проекта.

### Побочные находки в процессе (тоже почищено)
- Множество зомби-подов в `Terminating` по 23-220+ дней (`debug`, `test-pod`,
  старый `coredns`, старый `traefik`, `tailscale operator`) — принудительно удалены
  (`--grace-period=0 --force`). Не блокировали напрямую, но засоряли диагностику.
- `~/.kube/config` пользователя `oleks` был протухший — пересоздан из
  `/etc/rancher/k3s/k3s.yaml`. **Важно: НЕ использовать `sudo kubectl`** — у root
  свой `$HOME`, свой (отсутствующий/старый) kubeconfig. Просто `kubectl` без sudo.
- `kube-flannel-ds` на VM300 (workout-evo) висит в Terminating — нода stopped,
  под недостижим для удаления, не страшно, ожидаемо.
- Десятки старых ReplicaSet (DESIRED:0) накопились за 117 дней — `revisionHistoryLimit`
  не задан в чарте, чистка не настроена. См. ToDo ниже.

---

## 7. Известные баги / открытые вопросы (табличка)

| # | Баг/наблюдение | Статус | Куда смотреть дальше |
|---|---|---|---|
| LR-BUG-01 | `flannel-patch.yaml` мутировал общий кластерный ConfigMap из проектного чарта | ✅ исправлено 2026-06-21 (вынесен в черновики) | убедиться что больше не возвращался в чарт |
| LR-BUG-02 | JVM-резолвер ведёт себя иначе чем glibc для DNS внутри подов (наблюдение, не доказанная причина отдельного инцидента) | 🔍 не исследовано до конца | если повторится — сравнить `InetAddress` resolver vs `getaddrinfo` поведение явно |
| LR-BUG-03 | `psql`/`pg_isready` зависали (не explicit error) на полном FQDN при нестабильном flannel-state | 🔍 могло быть следствием LR-BUG-01, не проверено изолированно после фикса | если повторится отдельно от flannel-проблем — расследовать SCRAM handshake/MTU отдельно |
| LR-BUG-04 | Старые ReplicaSet (DESIRED:0) копятся бесконечно, `revisionHistoryLimit` не задан | 📋 не исправлено | добавить `revisionHistoryLimit: 3` в backend/frontend Deployment templates |
| LR-BUG-05 | `gateway-core` совмещает роль шлюза и k3s worker-ноды | 📋 архитектурный риск, не баг | обсудить на круглом столе при переезде на новое железо — разделить роли |
| LR-BUG-06 | Origin-адрес в `cloudflared config.yml` не задокументирован явно в этом файле | 📋 ToDo документации | прочитать `/etc/cloudflared/config.yml` на VM100, вписать сюда точный адрес |

---

## 8. ToDo (зафиксировано по итогам сессии 2026-06-21, не сделано, требует отдельного захода)

- [ ] **CI/CD к обновлённой архитектуре домашнего сервера.** Переосмыслить
  `.gitlab-ci.yml` + Helm-чарт LR в свете решений круглого стола (gateway/мониторинг/
  бэкап общие для всех проектов, проект не мутирует общую инфраструктуру).
- [ ] **Пересмотр Spring-авторизации и безопасности приложения в целом.** Не
  аудировано в этой сессии — JWT-механизм, CORS-конфигурация (видели `Access-Control-Allow-Origin`
  отсутствует при упавшем backend — стоит проверить что CORS настроен правильно
  и при здоровом backend, не просто "заработало раз ошибка ушла").
- [ ] **Бэкапирование Postgres для LR.** Сейчас НЕТ backup вообще (zero `pg_dump`/restic).
  Acer после сентябрьского апгрейда железа становится backup-контроллером —
  туда и переносить cron + restic + `pg_dump | restic backup --stdin`. До этого —
  хотя бы временный cron на VM200/lr700.
- [ ] **`revisionHistoryLimit`** в Helm templates (см. LR-BUG-04) — тикет `LR-044`.
- [ ] **Задокументировать origin-адрес cloudflared** (см. LR-BUG-06) — тикет `LR-045`.
- [x] **NetworkPolicy default-deny** для namespace `lr-dev` — закрыто
  2026-08-08 (`LR-031` Фаза 2), применено и подтверждено живьём с
  mgmt-core (негативный тест: под без нужных меток → `lr-postgres:5432`
  недостижим). Полный разбор — `docs/security/roadmap.md`,
  `docs/security/ARCHITECTURE.md` §2.9.

---

## 9. Контекст для соседства с Numi

Numi разворачивается **не в k3s** (архитектурное решение круглого стола: `systemd + nginx`,
без k3s, на отдельной VM). LR продолжает жить в k3s namespace `lr-dev`. Они физически
разделены (разные VM), но используют **общую** инфраструктуру:
- gateway-core (VM100) — общий NAT, общий cloudflared (разные tunnel-конфиги/маршруты
  для разных доменов)
- VM200 (mgmt-core) — пока k3s-нода + GitLab runner, см. отдельную заметку в
  CHANGELOG Numi про будущую роль этой VM

**Что Numi должна знать про LR при совместном планировании:**
- flannel — общий для всего k3s-кластера ресурс, любые правки (как `flannel-patch.yaml`)
  затрагивают весь кластер, не только LR
- DNS внутри k3s namespace-scoped для коротких имён — если Numi когда-либо тоже
  окажется в k3s (не текущий план, но на круглом столе обсуждалось "если рыцари
  решат куберизоваться") — нужно сразу проектировать с FQDN-осторожностью
- gateway-core перегружен ролями (NAT + cloudflared + k3s worker) — Numi точно
  не должна добавлять зависимость от этой же ноды без отдельного разговора

---

## 10. Шпаргалка доступа

```bash
# kubectl (с mgmt-core, БЕЗ sudo!)
kubectl get pods -n lr-dev
kubectl logs -n lr-dev -l app=lr-backend --tail=50

# Если kubeconfig протух:
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config

# GitLab runner статус
sudo gitlab-runner status
sudo gitlab-runner verify

# Проверка flannel backend-конфликта (если DNS снова поплывёт)
kubectl get cm -n kube-flannel kube-flannel-cfg -o jsonpath='{.data.net-conf\.json}'
kubectl logs -n kube-flannel -l app=flannel --tail=20 | grep -i backendtype
```