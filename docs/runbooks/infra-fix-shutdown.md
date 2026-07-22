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

## Доступ — шпаргалка

```bash
kubectl get pods -n lr-dev
kubectl logs -n lr-dev -l app=lr-backend --tail=50
sudo gitlab-runner status
sudo gitlab-runner verify
```
