# infra-fix-shutdown.md — LR recovery runbook

> Процедурный документ. Открывать **во время инцидента**, не для фонового
> чтения — за разбором "почему" и полной топологией см. `docs/infra/INFRA-LR.md`.
> Источник: инцидент 2026-06-21 (полный разбор — `INFRA-LR.md` §6). Пополнять
> новыми симптомами по мере появления, не задним числом выдумывать.

---

## Быстрая диагностика по симптому

### `tlab29.com` → Cloudflare отдаёт 502 Bad Gateway

Значит: `cloudflared` жив, но не достучался до origin (Traefik). Порядок проверки:

1. `kubectl get pods -A` — смотреть на `traefik`, `lr-frontend`, `lr-backend`,
   `metallb-controller`. `ImagePullBackOff` у всех сразу → см. раздел "NAT
   отсутствует" ниже.
2. `kubectl get pods -n lr-dev` — если `lr-backend` висит `Init:0/1` — это
   `wait-for-db` initContainer, `pg_isready` не отвечает → проверить
   `lr-postgres` под и DNS (раздел "DNS" ниже).
3. Если конкретно только `traefik`/`metallb` не встают, а `lr-*` в порядке —
   проверить `10.10.10.7:30080` (Traefik NodePort) напрямую с ноды VM700, не
   полагаться на `10.10.10.100` (MetalLB LoadBalancer IP — не назначается,
   см. DEBT-3 в `PROJECT_INDEX.md`).

### Массовый `ImagePullBackOff` во всех подах `lr-dev`

Два независимых известных корня — проверить оба:

1. **NAT отсутствует на gateway-core (VM100).** Ни одна k3s-нода не имеет
   выхода в интернет.
   ```bash
   sudo iptables -t nat -L POSTROUTING -n | grep MASQUERADE
   # если пусто:
   sudo iptables -t nat -A POSTROUTING -s 10.10.10.0/24 -o enp6s18 -j MASQUERADE
   systemctl status numi-nat.service   # должен пережить рестарт k3s/flannel
   ```
2. **GitLab registry token истёк молча.**
   ```bash
   kubectl get secret gitlab-registry -n lr-dev -o yaml
   kubectl describe pod <любой упавший под> -n lr-dev   # смотреть Events
   ```
   Ротация — пока ручная (см. LR-002 / бэклог CI-CD).

### DNS внутри пода не резолвится / `psql`, `pg_isready` зависают

1. Проверить дублирующиеся lease-записи flannel (главный источник
   непоследовательного поведения в инциденте 2026-06-21):
   ```bash
   kubectl logs -n kube-flannel -l app=flannel --tail=20 | grep -i backendtype
   ```
   Если видно **и `vxlan`, и `host-gw`** для одного и того же `PublicIP` —
   это конфликт backend'ов. Фикс:
   ```bash
   kubectl patch -n kube-flannel cm kube-flannel-cfg --type merge \
     -p '{"data":{"net-conf.json":"{\"Network\":\"10.42.0.0/16\",\"Backend\":{\"Type\":\"vxlan\"}}"}}'
   kubectl delete pod -n kube-flannel --all
   ```
2. Проверить node-ip у gateway-core (VM100) — должен быть `10.10.10.1`
   (внутренний VLAN200), не внешний `192.168.0.54`:
   ```bash
   cat /etc/rancher/k3s/config.yaml   # искать node-ip:
   ```
3. **Не путать JVM-резолвер и glibc-резолвер** — ведут себя по-разному с
   `search`-доменами (`ndots:5`). Если `psql` резолвит, а Java —
   `UnknownHostException`, это не обязательно значит "чинить только Java
   сторону" — сначала исключи реальный сетевой сбой (см. п.1-2), это была
   ложная зацепка в инциденте 2026-06-21.

### `kubectl` ругается `x509: certificate signed by unknown authority`

Протухший/неверный kubeconfig у пользователя `oleks`.
```bash
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $(id -u):$(id -g) ~/.kube/config
```
⚠️ **Никогда `sudo kubectl`** — у root свой (отсутствующий) kubeconfig, будет
та же ошибка по другой причине. Просто `kubectl` без sudo.

### После restore/swap `numi.sqlite`-подобной операции с `lr-postgres`

LR использует PostgreSQL, не SQLite — но тот же класс ошибки владения файлами
актуален для PVC/`local-path` volume, если когда-либо делается ручной
restore на хосте: под пересоздаёт файлы с владельцем контейнерного процесса,
не хоста. Проверять `ls -la` внутри пути PVC на ноде после любого ручного
вмешательства, не полагаться на "было ok на прошлой неделе".

---

## Если ничего из вышеперечисленного не подошло

1. Проверить `flannel-patch.yaml` **не вернулся** в
   `devops/helm/lr-app/templates/` (был сознательно вынесен в `drafts/`,
   2026-06-21 — общая кластерная инфраструктура не должна мутироваться
   чартом одного проекта). Если он там — это самый вероятный повторный
   корень, см. `INFRA-LR.md` §6.3.
2. Проверить `.gitlab-ci.yml` — `deploy-dev` всё ещё передаёт
   `--set flannel.backend=host-gw` в `helm upgrade` (пустой параметр, ничто
   в `values.yaml`/шаблонах его не читает, пока `flannel-patch.yaml` не
   вернулся в templates/ — но это живой fitil, см. тикет LR-002).
3. Проверить зомби-поды в `Terminating` (`kubectl get pods -A | grep
   Terminating`) — не блокируют напрямую, но засоряют диагностику, можно
   `--grace-period=0 --force` удалить после подтверждения что не мешают.

### GitLab CI: `docker login` 401 unauthorized на `registry.gitlab.com`

Симптом: `build-backend` проходит, но `docker-backend`/`docker-frontend`
падают на `docker login -u $REGISTRY_USER -p $REGISTRY_PASS $REGISTRY`
с `unauthorized: HTTP Basic: Access denied` — до того, как вообще
началась сборка образа. Не связано с кодом/коммитом, чисто GitLab-side.

**Проверять в этом порядке (от дешёвого к дорогому, без раскрытия
секретов):**

1. **Settings → Repository → Protected branches** — есть ли `main` в
   списке. В Settings → CI/CD → Variables у LR все 6 переменных
   (`JWT_SECRET`, `KUBECONFIG_FILE`, `POSTGRES_PASSWORD`,
   `POSTGRES_USER`, `REGISTRY_PASS`, `REGISTRY_USER`) помечены
   `Protected` — если ветка, на которой крутится пайплайн, НЕ в списке
   protected branches, все шесть резолвятся в пустую строку на рантайме
   пайплайна, и `docker login -u "" -p ""` даёт ровно такую 401. Это
   первое, что нужно проверить — ничего не раскрывает, самый вероятный
   корень после долгого простоя проекта (переменные могли быть
   добавлены/пересозданы уже после того, как protected-branch список
   разъехался).
2. Если `main` защищена и всё равно 401 — тогда, вероятно, протухла
   конкретно ручная `REGISTRY_PASS`/`REGISTRY_USER`. **Правильный фикс —
   не обновлять токен, а удалить обе эти CI/CD-переменные.**
   `.gitlab-ci.yml` уже сам корректно алиасит
   `REGISTRY_USER: "$CI_REGISTRY_USER"` /
   `REGISTRY_PASS: "$CI_REGISTRY_PASSWORD"` — предустановленные
   GitLab-переменные (job-token), которые генерируются заново на каждый
   запуск и никогда не протухают сами по себе. Ручные project-level
   переменные с теми же именами перекрывают этот alias по приоритету
   (project variables > `.gitlab-ci.yml` variables) — значит пайплайн
   использует не свежий job-token, а что-то однажды вручную вписанное.
   Удалить лишнее — надёжнее, чем поддерживать вручную токен, у которого
   есть срок жизни.
3. Owner может раскрыть значения переменных кнопкой "Reveal values" в
   Settings → CI/CD → Variables — **не нужно для диагностики по
   пунктам 1-2 выше**, и не стоит копировать раскрытые секреты во
   внешние чаты/сессии без необходимости.

---

## Восстановление PostgreSQL из бэкапа (LR-003)

> Добавлено 2026-07-24 вместе с самим бэкапом. **Ни разу не проверялось
> восстановлением на реальном кластере** — то же самое, что случилось с
> numi/Litestream (см. `numi` `KNOWN_ISSUES.md`): бэкап, который никто не
> пробовал восстановить, не доказан рабочим. Первый реальный прогон этой
> процедуры (даже в тестовом namespace, не поверх прод-БД) должен
> случиться при первой же возможности, не откладывать до реального
> инцидента.

CronJob `lr-postgres-backup` (namespace `lr-dev`) гонит `pg_dump` +
`restic backup` в IDrive e2 по расписанию (`postgresBackup.schedule` в
`values.yaml`, по умолчанию 03:00 UTC ежедневно). Ежедневная копия
хранится 14 дней, еженедельная — 8 недель (`postgresBackup.retention`).

**Проверить, что бэкапы вообще происходят:**
```bash
kubectl get cronjob -n lr-dev lr-postgres-backup
kubectl get jobs -n lr-dev -l job-name=lr-postgres-backup  # или по времени последнего запуска CronJob'а
kubectl logs -n lr-dev <под последнего job'а>
```

**Посмотреть, какие снапшоты реально есть в IDrive e2** (запустить
разово, эфемерным подом с теми же credentials, что и CronJob):
```bash
kubectl run -n lr-dev restic-check --rm -it --restart=Never \
  --image=postgres:16-alpine \
  --overrides='{"spec":{"nodeSelector":{"project":"lr"}}}' \
  --env="RESTIC_REPOSITORY=<из values.yaml postgresBackup.repository>" \
  --env="RESTIC_PASSWORD=<из Secret lr-backup-secrets, restic-password>" \
  --env="AWS_ACCESS_KEY_ID=<из Secret lr-backup-secrets>" \
  --env="AWS_SECRET_ACCESS_KEY=<из Secret lr-backup-secrets>" \
  -- sh -c "apk add --no-cache restic >/dev/null && restic snapshots"
```

**Восстановление (полная процедура, disaster recovery):**
1. Скачать нужный снапшот в дамп-файл (тот же эфемерный под, что выше,
   но `restic restore <snapshot-id> --target /tmp/restore` вместо
   `snapshots`, затем `kubectl cp` файл наружу или сразу пайпить в шаге 2).
2. Восстановить в `lr-postgres`:
   ```bash
   kubectl exec -n lr-dev -it <под lr-postgres> -- \
     pg_restore -U <POSTGRES_USER> -d <POSTGRES_DB> --clean --if-exists /path/to/dump
   ```
3. **Перед восстановлением поверх реальной БД** — предупредить
   заказчика, это разрушительная операция (`--clean` дропает существующие
   объекты перед восстановлением). Не выполнять без явного подтверждения,
   как и любую другую destructive-операцию в проде.

**Известный класс ошибок, на который стоит проверить в первую очередь**
(тот же паттерн, что дважды ловил numi): права на state/данные при смене
`User=`/`Group=` в systemd-юните — здесь неприменимо напрямую (весь бэкап
живёт как под в кластере, не systemd-сервис на голой VM), но при
пересмотре решения на осеннем апгрейде железа (см. `INFRA-LR.md` §9) —
проверить этот класс багов заново, если появится systemd-компонент.

---

## LR-031 Phase 2 — включение NetworkPolicy default-deny (`lr-dev`)

> Добавлено 2026-08-07. Манифесты (`devops/helm/lr-app/templates/
> networkpolicy-*.yaml`) написаны и провалидированы локально (`helm
> template`/`helm lint`) без доступа к реальному кластеру — **ни разу не
> применялись на живом k3s**. Тот же принцип, что и у процедуры
> восстановления бэкапа выше: манифест, который никто не пробовал
> применить, не доказан рабочим. `docs/security/roadmap.md`'s Фаза 2
> item 6 прямо требует не доверять факту существования манифеста — нужно
> подтвердить, что flannel реально энфорсит `NetworkPolicy`, тем же
> кластерным CNI, что уже один раз ломался неожиданным образом
> (`INFRA-LR.md` §6.3 — `flannel-patch.yaml`, общий ConfigMap, каскад по
> всему кластеру, не только LR). Выполнять то, что ниже, **только с
> mgmt-core (VM200)**, не из локальной сессии — `kubeconfig` там, доступ
> см. "Доступ — шпаргалка" ниже.

**0. Предусловие — свериться с реальными метками кластера, не с
предположением из комментариев в манифестах:**
```bash
kubectl get pods -n kube-system --show-labels | grep -i dns
# ожидается: k8s-app=kube-dns где-то в списке меток. Если метка другая —
# поправить networkpolicy-baseline.yaml ПЕРЕД применением, не после.

kubectl get ns traefik --show-labels
# ожидается: kubernetes.io/metadata.name=traefik (авто-проставляется
# k8s >=1.21 для ВСЕХ namespace — должно быть, но не предполагать).

kubectl get pods -n kube-system -o wide | grep -i flannel
kubectl cluster-info | head -1   # версия k3s, для сверки с default pod/service CIDR
```

**0a. Критичное предусловие (найдено `architect-reviewer` — без этого
шага весь Phase 2 может быть no-op'ом, никем не замеченным):**
голый flannel **не** энфорсит `NetworkPolicy` сам по себе — нужен
kube-router (или другой netpol-контроллер), встроенный в k3s, но
проверить, что он реально включён на этом конкретном инстансе, а не
отключён флагом `--disable-network-policy` при установке:
```bash
kubectl get pods -n kube-system | grep -i kube-router
# если пусто — проверить серверные аргументы k3s:
sudo cat /etc/systemd/system/k3s.service 2>/dev/null | grep -i disable-network-policy
sudo journalctl -u k3s --no-pager | grep -i "network.polic" | tail -5
```
Если энфорсмента нет — применение манифестов ниже создаст объекты
`NetworkPolicy`, которые ничего не блокируют. Все проверки в шагах 1-2
(DNS резолвится, curl проходит) в этом случае **тоже пройдут** — они
проверяют только разрешённое, не запрещённое — то есть ложно покажут
"всё работает", пока `lr-postgres` остаётся доступен с любого пода
ровно как до Phase 2. Тот же класс ошибки, что уже стоил времени с
`numi-nat.service`/Litestream (`KNOWN_ISSUES.md`): "активно"/"создано"
≠ "реально работает под настоящим условием, для которого создано". Не
продолжать шаг 1, пока это не подтверждено положительно.

**1. Применять по одному файлу, не всем чартом разом** — если что-то
пойдёт не так, откатить один файл проще, чем весь `helm upgrade`:
```bash
cd devops/helm/lr-app
helm template lr-app . -f values.yaml --set networkPolicy.enabled=true \
  --show-only templates/networkpolicy-baseline.yaml | kubectl apply -n lr-dev -f -
```
**Сразу после — проверить, что ничего не сломалось, ДО применения
следующего файла:**
```bash
kubectl get pods -n lr-dev   # все должны остаться Running, не CrashLoop
kubectl exec -n lr-dev -it <под lr-backend> -- nslookup lr-postgres
# должен резолвиться — если нет, DNS-политика неверна, откатить:
kubectl delete networkpolicy -n lr-dev lr-allow-dns-egress lr-default-deny-all
```

**2. Дальше по одному:** `networkpolicy-backend.yaml`,
`networkpolicy-frontend.yaml`, `networkpolicy-postgres.yaml` (плюс
`networkpolicy-postgres-backup.yaml`, только если `postgresBackup.enabled=true`
уже раньше). После каждого — реальная проверка функциональности, не
только "под жив":
```bash
# После backend+postgres policy — реальный запрос через полный путь:
curl -sf https://api.tlab29.com/api/v1/workshops > /dev/null && echo OK
# После frontend policy:
curl -sf https://tlab29.com/ > /dev/null && echo OK
```

**2a. Обязательный негативный тест сразу после `networkpolicy-postgres.yaml`
(не пропускать — весь смысл M4/Phase 2 именно в этом, а не в шагах выше):**
проверить, что под БЕЗ метки `app: lr-backend`/`app: lr-postgres-backup`
**не может** достучаться до `lr-postgres`, а не только что легитимные
поды могут:
```bash
kubectl run -n lr-dev netpol-probe --rm -it --restart=Never \
  --image=busybox --overrides='{"spec":{"nodeSelector":{"project":"lr"}}}' \
  -- nc -zv -w3 lr-postgres 5432
```
**Ожидаемый результат: timeout/connection refused.** Если соединение
прошло успешно — `NetworkPolicy` не энфорсится (см. шаг 0a) ИЛИ
селектор в `networkpolicy-postgres.yaml` слишком широкий — не считать
M4 закрытым, разбираться, не продолжать до `networkpolicy-postgres-backup.yaml`.
```bash
# Если используется бэкап — дождаться следующего расписания или
# триггернуть вручную:
kubectl create job -n lr-dev --from=cronjob/lr-postgres-backup manual-netpol-check
kubectl logs -n lr-dev -l job-name=manual-netpol-check --follow
```

**3. Только после того, как все 4-5 файлов применены и каждый шаг выше
подтверждён реальным успешным ответом** — зафиксировать `networkPolicy.
enabled: true` в `values.yaml` самим коммитом (не оставлять расхождение
между тем, что в git, и тем, что реально в кластере — тот же урок, что
уже стоил времени с ownership-багами в numi, "то, что в конфиге" и "то,
что реально применено" должны совпадать).

**Откат при любой проблеме — удалить все NetworkPolicy разом:**
```bash
kubectl delete networkpolicy -n lr-dev --all
```
Это мгновенно возвращает namespace к open-by-default (текущее
состояние на сегодня) — безопасный откат, не полдела.

---

## Доступ — шпаргалка

```bash
kubectl get pods -n lr-dev
kubectl logs -n lr-dev -l app=lr-backend --tail=50
sudo gitlab-runner status
sudo gitlab-runner verify
```
