# Troubleshooting и disaster recovery Kubernetes

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Kubernetes 1.34 (baseline раздела); команды требуют `kubectl` в поддерживаемом version-skew диапазоне
- **Статус манифестов:** встроенные API/команды — `current`; provider-specific backup, CNI, CSI и managed control-plane процедуры — `template`, сверяются с поставщиком
- **Первичные источники:** [Troubleshooting](https://v1-34.docs.kubernetes.io/docs/tasks/debug/); [Operating etcd](https://v1-34.docs.kubernetes.io/docs/tasks/administer-cluster/configure-upgrade-etcd/); [Cluster Administration](https://v1-34.docs.kubernetes.io/docs/concepts/cluster-administration/); [Disruptions](https://v1-34.docs.kubernetes.io/docs/concepts/workloads/pods/disruptions/)

## Содержание

1. [Безопасный порядок диагностики](#безопасный-порядок-диагностики)
2. [Runbook: Pending Pods](#runbook-pending-pods)
3. [Runbook: CrashLoopBackOff](#runbook-crashloopbackoff)
4. [Runbook: OOMKilled](#runbook-oomkilled)
5. [Runbook: DNS и сеть](#runbook-dns-и-сеть)
6. [Runbook: storage incidents](#runbook-storage-incidents)
7. [Runbook: control-plane degradation](#runbook-control-plane-degradation)
8. [Disaster recovery](#disaster-recovery)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Безопасный порядок диагностики

Сначала объявите incident, зафиксируйте время/impact и остановите опасные automation changes. Двигайтесь от симптома к scope: один container → Pod → node → namespace/service → cluster/control plane → provider. Сохраните events/logs/metrics до restart. Меняйте одну гипотезу за раз, записывайте команды и результат; не удаляйте Pod/PVC и не перезапускайте control plane «для проверки».

```bash
kubectl get pods -A -o wide
kubectl get events -A --sort-by=.metadata.creationTimestamp
kubectl describe pod POD -n NS
kubectl logs POD -n NS -c CONTAINER --previous --timestamps
kubectl get nodes
kubectl get --raw='/readyz?verbose'
```

Events имеют ограниченное хранение, а `--previous` доступен лишь для предыдущего container instance — выгружайте их сразу.

## Runbook: Pending Pods

**Симптом:** Pod долго `Pending`, нет node assignment или ожидается volume.

1. `kubectl describe pod` — прочитайте `FailedScheduling`, `FailedMount`, quota/admission events.
2. Сравните requests с `kubectl describe nodes`; проверьте Ready/pressure, allocatable, ResourceQuota и LimitRange.
3. Проверьте nodeSelector/affinity, topology spread, taints/tolerations, unbound PVC и storage topology.
4. Проверьте capacity node group/autoscaler events: может ли он создать подходящий instance/zone и не достигнут ли quota/max size.
5. Mitigation: scale capacity или уменьшите **обоснованно** request; исправьте constraint/PVC. Не снимайте security/HA constraint вслепую.
6. Success: Pod Scheduled/Ready, backlog снижается, capacity headroom восстановлен. Follow-up: alert на unschedulable и capacity forecast.

## Runbook: CrashLoopBackOff

`CrashLoopBackOff` — backoff повторных запусков, не первопричина.

1. `kubectl get pod ... -o jsonpath='{.status.containerStatuses[*].lastState}'` и `kubectl logs --previous`.
2. Проверьте exit code, args/command, config/Secret, permissions, dependency endpoints, startup/liveness probes.
3. Сравните ReplicaSet/image digest с последним успешным release; проверьте rollout events.
4. Для distroless используйте `kubectl debug` ephemeral container, если политика разрешает; не модифицируйте production image.
5. Mitigation: rollback плохого release/config либо исправление probe; не увеличивайте delays, скрывая crash.
6. Success: restart count перестал расти, readiness и SLO стабильны в observation window.

## Runbook: OOMKilled

1. Подтвердите `reason: OOMKilled`, exit code 137 и различите container cgroup OOM от node `MemoryPressure`/system OOM.
2. Сопоставьте working set/RSS, limit, request, heap/off-heap/page cache и spike с нагрузкой/release.
3. Проверьте memory leak, concurrency и runtime limits (например, heap должен оставлять место native memory).
4. Краткая mitigation: rollback, ограничение нагрузки или увеличение request/limit при наличии node capacity. Не убирайте limit без оценки blast radius.
5. Long-term: profile/исправление leak, realistic load test, alert до limit; настройте VPA recommendation осторожно.
6. Success: нет OOM/restarts, latency и node pressure нормальны на peak load.

## Runbook: DNS и сеть

1. Определите направление и scope: Pod→Service, Pod→external, ingress→Pod; TCP timeout, refused, TLS или DNS.
2. Проверьте `Service`, `EndpointSlice`, ready endpoints, selectors и targetPort: `kubectl get svc,endpointslice -n NS`.
3. Из debug Pod проверьте `/etc/resolv.conf`, `getent hosts service.namespace.svc.cluster.local`, затем прямой Pod IP и Service IP.
4. Проверьте CoreDNS Pods/logs/latency, kube-proxy или dataplane CNI, NetworkPolicy обеих сторон, cloud firewall/security groups и conntrack exhaustion.
5. Mitigation должна быть узкой: исправить selector/policy/DNS capacity. Не отключайте все NetworkPolicy и не flush-ите cluster-wide networking без плана.
6. Success: DNS error/latency и packet loss нормализованы из нескольких nodes/zones; synthetic probe зелёный.

## Runbook: storage incidents

1. Не удаляйте PVC/PV. Зафиксируйте reclaim policy, VolumeAttachment, StorageClass, CSI driver/node и zone.
2. Для Pending PVC смотрите provisioner events, quota/capacity/topology. Для mount — CSI controller/node logs, attachment и node health.
3. Для I/O latency/errors проверьте provider volume health/limits, filesystem usage/inodes/read-only state и application logs.
4. При node failure следуйте CSI/provider процедуре detach/fencing: принудительный detach без подтверждения может дать multi-attach или corruption.
5. Restore делайте в новый PVC/namespace, проверяйте consistency на уровне приложения; snapshot не всегда application-consistent.
6. Success: attach/mount и чтение/запись стабильны, данные валидированы владельцем. Follow-up: backup restore test и capacity alerts.

## Runbook: control-plane degradation

**Признаки:** API latency/5xx/timeouts, controllers не reconcile, scheduling остановился, leader election flaps.

1. С отдельного trusted host проверьте `/livez`, `/readyz?verbose`, API latency/errors и provider status; исключите клиентскую сеть/credential issue.
2. Self-managed: проверьте ресурсы control-plane nodes, static Pods, certificates/time, API server/scheduler/controller-manager logs и etcd endpoint health/latency/space/leader changes. Managed: откройте severity incident поставщику и не пытайтесь править скрытые компоненты.
3. Заморозьте deploy/autoscaling, создающие API churn; сохраняйте data-plane traffic, если Pods продолжают работать. Не выполняйте массовые restarts.
4. Etcd compaction/defrag/member replacement/restore выполняйте только по версии-совместимому runbook после snapshot; неверная операция может увеличить outage.
5. Success: API SLI, scheduler/controller queues и etcd quorum стабильны; reconciliation backlog обработан. Затем постепенно разморозьте automation.

## Disaster recovery

Задайте **RTO** и **RPO** для control plane и каждого stateful приложения. Backup включает не только etcd: manifests/Git, encryption config и KMS access, PKI, CRD, external load balancer/DNS/IAM, PV snapshots и application-consistent database backups. Etcd snapshot содержит cluster state и Secrets, поэтому шифруйте и строго контролируйте его.

Минимальный план:

1. Автоматические version-compatible etcd snapshots (self-managed) и application backups в отдельном account/region с immutability.
2. Проверка restore в изолированный кластер; измерение RTO/RPO, проверка данных, DNS, identities и reconciliation.
3. Порядок восстановления: инфраструктура/control plane → CNI/CSI/DNS → CRD/controllers → policies/secrets → stateless → stateful по application runbook → ingress/DNS.
4. После etcd restore убедитесь, что encryption keys доступны, сертификаты/endpoint корректны, а external systems не получат дублирующие side effects.
5. Проводите game days и обновляйте контакты, команды, owners, dependencies и критерии failover/failback.

| Артефакт | Backup/источник | Проверка |
|---|---|---|
| Desired state | Git + immutable artifacts | Новый cluster converges без ручного drift |
| Cluster state | Etcd snapshot/provider backup | Version-compatible restore и API smoke test |
| Secrets/keys | External vault, KMS и защищённая config | Decrypt после restore с break-glass audit |
| Application data | DB-native backup/PV snapshot | Logical consistency и business queries |

## Вопросы для самопроверки

1. **Почему нельзя сразу удалить Pending Pod?** Теряются evidence/events, а неизменённая причина повторится.
2. **Что означает CrashLoopBackOff?** Kubelet замедляет рестарты; первопричину ищут в last state/logs/probes.
3. **Почему snapshot volume может быть недостаточен?** Он может не обеспечивать application consistency между файлами/томами.
4. **Что важнее наличия backup?** Регулярно доказанный restore в заданные RTO/RPO.

[← К разделу Kubernetes](README.md)
