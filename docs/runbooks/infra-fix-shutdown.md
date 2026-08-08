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
предположением из комментариев в манифестах.** ✅ Выполнено 2026-08-08,
подтверждено на реальном кластере:
```bash
kubectl get pods -n kube-system --show-labels | grep -i dns
# → coredns-*, k8s-app=kube-dns — совпадает с networkpolicy-baseline.yaml.

kubectl get ns traefik --show-labels
# → kubernetes.io/metadata.name=traefik — совпадает с networkpolicy-backend.yaml
# и networkpolicy-frontend.yaml.
```
**Поправка по факту (2026-08-08):** на этом кластере flannel живёт в
СВОЕЙ namespace `kube-flannel`, не в `kube-system` — новее версии k3s
переехали на такую раскладку. Не влияет ни на один манифест (фланнел —
CNI, реализующий проверку пакетов на уровне ядра/iptables, а не под, с
которым что-либо из наших `NetworkPolicy` должно взаимодействовать по
IP/label-селектору) — просто исходный `grep -i flannel` в `kube-system`
искал не в том месте, реальное расположение подтверждено `kubectl get
pods -A`: `kube-flannel-ds-*` × 4 (по одному на ноду), все `Running`.

**0a. Критичное предусловие (найдено `architect-reviewer` — без этого
шага весь Phase 2 может быть no-op'ом, никем не замеченным), с поправкой
после первого реального прогона 2026-08-08:**

⚠️ Изначальная версия этого шага искала под `kube-router` в
`kube-system` — это неверно для k3s конкретно (не общая ошибка про
Kubernetes вообще). У k3s netpol-контроллер (урезанная версия
kube-router, только сетевые политики, не полный роутинг) **встроен
прямо в бинарник `k3s server`/`k3s agent`**, запускается как горутина
внутри процесса, а не разворачивается отдельным подом — в отличие от
"ванильной" установки kube-router. Реальный прогон `kubectl get pods -A`
на этом кластере это подтвердил: пода `kube-router` нет нигде, что
**само по себе НЕ значит, что энфорсмент отключён** — это ожидаемо для
k3s с flannel по умолчанию. Проверять нужно не наличие пода, а
конфигурацию самого k3s:
```bash
# На control-plane ноде (mgmt-core, VM200 — уже там, если следуешь этой процедуре):
sudo cat /etc/systemd/system/k3s.service | grep -i disable-network-policy
# Пусто = флаг НЕ установлен = netpol-контроллер включён (k3s default).
# Если видишь --disable-network-policy — весь Phase 2 не будет работать
# без переустановки k3s-сервиса без этого флага, СТОП, не продолжать.

sudo journalctl -u k3s --no-pager | grep -i "network polic"
# Ожидается строка про старт network policy controller при последнем
# рестарте k3s (может не найтись, если рестарта давно не было и старые
# логи уже ушли — отсутствие строки НЕ равно отсутствию контроллера,
# в отличие от найденного --disable-network-policy, который однозначен).
```
Если оба флажковых чека чистые (флага нет) — это хороший знак, но
**не финальное доказательство**, только предварительное. Финальное
доказательство — исключительно негативный тест в шаге 2a ниже, ничего
раньше него. Если бы энфорсмента не было, проверки в шагах 1-2 (DNS
резолвится, curl проходит) **тоже прошли бы** — они проверяют только
разрешённое, не запрещённое — то есть ложно показали бы "всё работает",
пока `lr-postgres` остаётся доступен с любого пода ровно как до Phase 2.
Тот же класс ошибки, что уже стоил времени с `numi-nat.service`/Litestream
(`KNOWN_ISSUES.md`): "активно"/"создано" ≠ "реально работает под
настоящим условием, для которого создано".

✅ **Выполнено 2026-08-08.** `--disable-network-policy` в конфиге k3s
отсутствует; `journalctl -u k3s` показывает регулярные `"Starting
network policy controller version v2.5.0"` при каждом рестарте сервиса,
последний зафиксированный запуск — 27-28 сентября (предыдущего цикла
логов). Предварительный сигнал чистый. Переходим к шагу 1 — окончательное
подтверждение всё ещё только шаг 2a.

**1. Применять baseline + ВСЕ ingress-allow правила, нужные для базовой
доступности сайта, ОДНИМ `kubectl apply -f -` — не по файлу за раз.**

⚠️ **Поправлено 2026-08-08 по факту реального инцидента.** Первая версия
этого раздела предписывала применить `networkpolicy-baseline.yaml`,
остановиться и проверить, и только потом (отдельным шагом, отдельной
командой) — `networkpolicy-backend.yaml`/`-frontend.yaml`. Ровно так и
сделали при первом реальном прогоне — и сайт упал: `lr-default-deny-all`
блокирует ВЕСЬ ingress в namespace сразу, включая от Traefik, а разрешение
для Traefik лежит в отдельных файлах, применяемых следующим шагом.
Между ними образовалось окно (пока человек читает следующий пункт
runbook'а и печатает команду — не миллисекунды), в течение которого
сайт был недоступен снаружи. Откатили `kubectl delete networkpolicy -n
lr-dev --all`, восстановилось мгновенно. Разбор — `KNOWN_ISSUES.md`.
Исправленный порядок ниже применяет всё необходимое для живого сайта
одним вызовом, без человеческого окна между "заблокировать всё" и
"разрешить нужное":
```bash
cat <<'EOF' | kubectl apply -n lr-dev -f -
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: lr-default-deny-all
spec:
  podSelector: {}
  policyTypes:
    - Ingress
    - Egress
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: lr-allow-dns-egress
spec:
  podSelector: {}
  policyTypes:
    - Egress
  egress:
    - to:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: kube-system
          podSelector:
            matchLabels:
              k8s-app: kube-dns
      ports:
        - protocol: UDP
          port: 53
        - protocol: TCP
          port: 53
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: lr-backend-netpol
spec:
  podSelector:
    matchLabels:
      app: lr-backend
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: traefik
      ports:
        - protocol: TCP
          port: 8080
  egress:
    - to:
        - podSelector:
            matchLabels:
              app: lr-postgres
      ports:
        - protocol: TCP
          port: 5432
    - to:
        - ipBlock:
            cidr: 0.0.0.0/0
            except:
              - 10.0.0.0/8
      ports:
        - protocol: TCP
          port: 443
        - protocol: TCP
          port: 587
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: lr-frontend-netpol
spec:
  podSelector:
    matchLabels:
      app: lr-frontend
  policyTypes:
    - Ingress
  ingress:
    - from:
        - namespaceSelector:
            matchLabels:
              kubernetes.io/metadata.name: traefik
      ports:
        - protocol: TCP
          port: 80
---
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: lr-postgres-netpol
spec:
  podSelector:
    matchLabels:
      app: lr-postgres
  policyTypes:
    - Ingress
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: lr-backend
      ports:
        - protocol: TCP
          port: 5432
EOF
```
(Строку с `app: lr-postgres-backup` в `lr-postgres-netpol`'s `ingress.from`
добавить вручную перед вставкой, если `postgresBackup.enabled=true` уже
включён — см. `networkpolicy-postgres.yaml` в репозитории для точного
вида блока.)

**Сразу после — полная проверка, не только "под жив":**
```bash
kubectl get pods -n lr-dev   # все Running, не CrashLoop
kubectl exec -n lr-dev -it $(kubectl get pod -n lr-dev -l app=lr-backend -o jsonpath='{.items[0].metadata.name}') -- nslookup lr-postgres
curl -sf -o /dev/null -w "%{http_code}\n" https://api.tlab29.com/api/v1/workshops   # ожидается 200
curl -sf -o /dev/null -w "%{http_code}\n" https://tlab29.com/                        # ожидается 200
```
Если что-то не 200/не резолвится — откат немедленно, не разбираться на
живом проде:
```bash
kubectl delete networkpolicy -n lr-dev --all
```

✅ **Выполнено 2026-08-08** (второй попыткой — см. врезку выше про
первую, вызвавшую короткий простой). Оба `curl` вернули `200`, поды
остались `Running`, `nslookup` нашёл `lr-postgres.lr-dev.svc.cluster.local`.

**2a. Обязательный негативный тест (не пропускать — весь смысл M4/Phase
2 именно в этом, а не в проверке выше):** подтвердить, что под БЕЗ
метки `app: lr-backend`/`app: lr-postgres-backup` **не может**
достучаться до `lr-postgres`, а не только что легитимные поды могут:
```bash
kubectl run -n lr-dev netpol-probe --restart=Never \
  --image=busybox --overrides='{"spec":{"nodeSelector":{"project":"lr"}}}' \
  -- sh -c 'nc -zv -w3 lr-postgres 5432; echo "EXITCODE=$?"'

kubectl logs -n lr-dev netpol-probe
kubectl delete pod -n lr-dev netpol-probe
```
**Ожидаемый результат: `EXITCODE=1` в логах** (соединение не удалось).
Читать код явно через `echo`, не полагаться на `kubectl wait
...phase=Failed` — оборачивание в `sh -c '...; echo ...'` меняет
итоговый exit code КОНТЕЙНЕРА на код последней команды (`echo`, всегда
0/`Succeeded`), даже если сам `nc` внутри провалился; текстовый вывод
`nc -v` при таймауте в некоторых сборках busybox тоже может быть
пустым — `EXITCODE=` в логах устраняет обе эти неоднозначности разом.
Если `EXITCODE=0` — `NetworkPolicy` не энфорсится (см. шаг 0a) ИЛИ
селектор в `lr-postgres-netpol` слишком широкий — не считать M4
закрытым, разбираться, не продолжать до backup-политики ниже.

✅ **Выполнено 2026-08-08.** `EXITCODE=1` — соединение не прошло, M4
подтверждён закрытым не по факту существования манифеста, а реальным
тестом с mgmt-core.

**3. Только если `postgresBackup.enabled=true` уже включён** — отдельным
шагом (egress-only, никакого влияния на живой ingress-трафик сайта, не
требует той же спешки, что шаг 1):
```bash
cd devops/helm/lr-app
helm template lr-app . -f values.yaml --set networkPolicy.enabled=true --set postgresBackup.enabled=true \
  --show-only templates/networkpolicy-postgres-backup.yaml | kubectl apply -n lr-dev -f -
kubectl create job -n lr-dev --from=cronjob/lr-postgres-backup manual-netpol-check
kubectl logs -n lr-dev -l job-name=manual-netpol-check --follow
```

**4. Только после того, как шаги 1/2a (и 3, если применимо) подтверждены
реальным успешным ответом** — зафиксировать `networkPolicy.enabled: true`
в `values.yaml` самим коммитом (не оставлять расхождение между тем, что
в git, и тем, что реально в кластере — тот же урок, что уже стоил
времени с ownership-багами в numi, "то, что в конфиге" и "то, что
реально применено" должны совпадать).

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
