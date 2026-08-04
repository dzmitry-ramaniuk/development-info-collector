# Scheduling, ресурсы и autoscaling в Kubernetes

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Requests, limits и QoS](#requests-limits-и-qos)
3. [Probes](#probes)
4. [PodDisruptionBudget](#poddisruptionbudget)
5. [Размещение Pods](#размещение-pods)
6. [Taints и tolerations](#taints-и-tolerations)
7. [HPA, VPA и Cluster Autoscaler](#hpa-vpa-и-cluster-autoscaler)
8. [Практический пример](#практический-пример)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Kubernetes 1.34 (baseline раздела)
- **Статус манифестов:** `current` для встроенных `v1`, `policy/v1` и `autoscaling/v2`; VPA и Cluster Autoscaler — `current`, но являются отдельно устанавливаемыми компонентами/CRD
- **Первичные источники:** [Scheduling](https://v1-34.docs.kubernetes.io/docs/concepts/scheduling-eviction/); [Resource Management](https://v1-34.docs.kubernetes.io/docs/concepts/configuration/manage-resources-containers/); [HPA](https://v1-34.docs.kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/); [Node Autoscaling](https://v1-34.docs.kubernetes.io/docs/concepts/cluster-administration/node-autoscaling/)

## Requests, limits и QoS

Scheduler размещает Pod по сумме **requests**, а не текущему потреблению. CPU limit реализуется throttling; превышение memory limit может привести к OOM kill. Слишком низкие requests вызывают contention и неточные решения autoscaler, слишком высокие — Pending Pods и низкую утилизацию. Для extended resources обычно request равен limit.

**QoS** определяется на уровне Pod:

| Класс | Условие | Поведение при давлении |
|---|---|---|
| Guaranteed | У каждого контейнера CPU и memory request = limit | Последний кандидат среди Pods при прочих равных |
| Burstable | Есть хотя бы один request/limit, но не выполнен Guaranteed | Приоритет зависит в том числе от превышения request |
| BestEffort | Нет CPU/memory requests и limits | Первый кандидат на eviction |

QoS не является абсолютной гарантией: Pod может быть evicted, процесс — убит cgroup OOM, а узел — потерян. Namespace `LimitRange` задаёт defaults/границы, `ResourceQuota` ограничивает совокупный бюджет.

## Probes

- **startupProbe** даёт медленно стартующему процессу окно; пока она не успешна, liveness/readiness не выполняются.
- **readinessProbe** управляет готовностью endpoint принимать трафик, но не перезапускает контейнер.
- **livenessProbe** обнаруживает необратимое зависание и вызывает restart; она не должна зависеть от временно недоступной БД.

Настройте `timeoutSeconds`, `periodSeconds`, thresholds по измеренному startup/latency. Ошибка — одинаковый `/health`, который проверяет все downstream: outage зависимости превращается в restart storm. Для graceful shutdown readiness должна стать false до завершения процесса, а `terminationGracePeriodSeconds` — покрывать drain.

```yaml
startupProbe:
  httpGet: {path: /health/startup, port: 8080}
  periodSeconds: 5
  failureThreshold: 24
readinessProbe:
  httpGet: {path: /health/ready, port: 8080}
  periodSeconds: 5
  failureThreshold: 2
livenessProbe:
  httpGet: {path: /health/live, port: 8080}
  periodSeconds: 10
  failureThreshold: 3
```

## PodDisruptionBudget

**PDB** ограничивает одновременные добровольные disruptions (например, drain или cluster autoscaler), но не защищает от падения узла, OOM, удаления Deployment и не гарантирует доступность. Выбирайте ровно одно: `minAvailable` или `maxUnavailable`; selector обязан совпадать с workload. PDB не создаёт реплики — обеспечьте их контроллером и размещайте по failure domains.

```yaml
apiVersion: policy/v1
kind: PodDisruptionBudget
metadata: {name: api, namespace: shop}
spec:
  maxUnavailable: 1
  selector:
    matchLabels: {app: api}
```

Слишком строгий PDB (`minAvailable: 100%` при малом числе реплик) блокирует drain и обновление узлов. Мониторьте allowed disruptions и заранее проверяйте maintenance.

## Размещение Pods

**nodeAffinity** выбирает узлы по labels; `requiredDuringSchedulingIgnoredDuringExecution` — жёсткое условие, `preferred...` — score. **podAffinity/antiAffinity** размещает рядом/врозь относительно других Pods и требует корректный `topologyKey`. Жёсткая anti-affinity повышает HA, но может оставить Pod Pending при нехватке доменов.

**Topology spread constraints** равномерно распределяют подходящие Pods между zone/node. `maxSkew` ограничивает дисбаланс, `whenUnsatisfiable: DoNotSchedule` делает правило жёстким, `ScheduleAnyway` — предпочтением. Для HA чаще задают spread по zone и hostname, но проверяют labels и число доступных доменов.

```yaml
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: DoNotSchedule
    labelSelector:
      matchLabels: {app: api}
  - maxSkew: 1
    topologyKey: kubernetes.io/hostname
    whenUnsatisfiable: ScheduleAnyway
    labelSelector:
      matchLabels: {app: api}
```

Предпочитайте spread constraints для равномерности; anti-affinity — когда важно явно не совмещать конкретные workloads. Защищайте node labels через NodeRestriction/контролируемый provisioning, если они несут security-смысл.

## Taints и tolerations

**Taint** отталкивает Pods (`NoSchedule`, `PreferNoSchedule`, `NoExecute`), **toleration** лишь разрешает Pod рассматриваться для такого узла, но не притягивает его. Для dedicated pool сочетайте taint+toleration с node affinity. `NoExecute` может выселить уже запущенные Pods; `tolerationSeconds` задаёт задержку eviction.

```yaml
tolerations:
  - key: workload
    operator: Equal
    value: batch
    effect: NoSchedule
affinity:
  nodeAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      nodeSelectorTerms:
        - matchExpressions:
            - {key: workload, operator: In, values: [batch]}
```

## HPA, VPA и Cluster Autoscaler

**HPA** меняет replicas по resource/custom/external metrics. Для CPU utilization нужны requests и Metrics API (обычно Metrics Server). Используйте `autoscaling/v2`, несколько метрик, stabilization window и ограничение scale policies. Не управляйте одновременно `.spec.replicas` из HPA и GitOps, иначе будет drift loop.

**VPA** рекомендует или меняет requests. Это отдельный autoscaler с CRD, не встроенный API. Режим recommendation безопасен для начала; автоматическое применение может пересоздавать Pods. Не сочетайте HPA по CPU utilization с VPA, меняющим CPU request, без осмысленной модели; HPA по абсолютным/custom metrics обычно совместимее.

**Cluster Autoscaler (CA)**/реализация node autoscaling добавляет узлы для unschedulable Pods и удаляет недоиспользуемые, если Pods можно безопасно переместить. Это внешний компонент, тесно связанный с cloud/node groups. CA опирается на requests, constraints и PDB; local storage, строгая affinity/PDB и системные Pods могут блокировать scale-down. HPA создаёт demand, node autoscaler предоставляет capacity — задержки cold start нужно учитывать.

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata: {name: api, namespace: shop}
spec:
  scaleTargetRef: {apiVersion: apps/v1, kind: Deployment, name: api}
  minReplicas: 3
  maxReplicas: 20
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 300
  metrics:
    - type: Resource
      resource:
        name: cpu
        target: {type: Utilization, averageUtilization: 65}
```

## Практический пример

Для API: измерьте p95 CPU/memory, задайте requests с запасом; три реплики; zone+hostname spread; PDB `maxUnavailable: 1`; startup/readiness/liveness с разными смыслами; HPA по CPU и, лучше, latency/queue metric; load test с node scale-up и drain. Проверяйте `kubectl describe pod`, scheduler events, `kubectl top` и метрики throttling/OOM.

## Вопросы для самопроверки

1. **Почему limit не участвует в bin-packing?** Scheduler резервирует capacity по requests.
2. **Зачем startup probe?** Она защищает медленный корректный старт от преждевременных liveness restarts.
3. **Гарантирует ли PDB две доступные реплики?** Только ограничивает добровольные eviction и не покрывает аварии.
4. **Почему toleration недостаточно для dedicated nodes?** Она разрешает размещение, но не требует его; добавьте affinity.
5. **Как связаны HPA и CA?** HPA увеличивает Pods, CA добавляет nodes, когда новые Pods некуда разместить.

[← К разделу Kubernetes](README.md)
