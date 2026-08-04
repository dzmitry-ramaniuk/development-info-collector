# Релизы и deployment strategies

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Release не равен deployment](#release-не-равен-deployment)
3. [Миграции базы данных](#миграции-базы-данных)
4. [Feature flags](#feature-flags)
5. [Стратегии deployment](#стратегии-deployment)
6. [Health gates и rollback](#health-gates-и-rollback)
7. [Практический runbook](#практический-runbook)
8. [Audit trail](#audit-trail)
9. [Типичные ошибки](#типичные-ошибки)
10. [Практические упражнения](#практические-упражнения)
11. [Вопросы для самопроверки](#вопросы-для-самопроверки)
12. [Связанные темы](#связанные-темы)

## Актуальность материала

- **Проверено:** 4 августа 2026 года.
- **Целевые версии примеров:** Kubernetes 1.34 Deployments; Flyway 11.x для версионированных SQL-миграций; AWS deployment concepts без привязки к одному сервису.
- **Статус примеров:** `current`; команды Kubernetes иллюстративны и требуют проверки namespace/context.
- **Первичные источники:** [Kubernetes Deployments v1.34](https://v1-34.docs.kubernetes.io/docs/concepts/workloads/controllers/deployment/), [Kubernetes rollout command](https://kubernetes.io/docs/reference/kubectl/generated/kubectl_rollout/), [Flyway migrations](https://documentation.red-gate.com/fd/migrations-184127470.html), [AWS deployment strategies](https://docs.aws.amazon.com/whitepapers/latest/overview-deployment-options/deployment-strategies.html), [OpenFeature specification](https://openfeature.dev/specification/).

## Release не равен deployment

**Deployment** доставляет новую версию в runtime; **release** открывает поведение пользователям. Feature flag позволяет разнести эти события. Это уменьшает риск, но добавляет состояния и операционную сложность.

Перед изменением зафиксируйте:

- artifact digest и configuration revision;
- owner, окно и blast radius;
- SLI/порог автоматической остановки;
- совместимость БД/API/events;
- rollback/roll-forward и ответственного за решение.

## Миграции базы данных

Миграция данных — отдельный deployment с собственным риском. Для zero-downtime используйте **expand/contract**:

1. **Expand:** добавить nullable column/table/index совместимо со старым кодом.
2. Развернуть код, умеющий работать со старой и новой схемой; при необходимости dual-write.
3. Выполнить ограниченный, возобновляемый backfill с метриками.
4. Переключить чтение, проверить консистентность.
5. **Contract:** удалить старый путь/column только после rollback horizon.

```sql
-- V42__add_normalized_email.sql: сначала совместимое расширение
ALTER TABLE customer ADD COLUMN normalized_email varchar(320);

-- Индекс/constraint для большой таблицы создавайте способом,
-- который минимизирует блокировки именно в вашей СУБД и версии.
```

Migration tool должен вести schema history и получать lock, но он не делает SQL автоматически безопасным. Проверяйте длительность locks, размер transaction log, replication lag, disk headroom и возможность прервать/backfill продолжить. Не запускайте destructive migration одновременно с приложением, которое ещё использует удаляемое поле.

Rollback бинарника не откатывает данные. Down migration часто разрушительна; безопаснее roll-forward и совместимая схема. Подробнее — [эволюция системы и миграции без простоя](../system%20design/12-эволюция-системы-и-миграции-без-простоя.md).

## Feature flags

Типы flags:

- **release flag** — отделяет deployment от включения функции;
- **experiment flag** — распределяет аудиторию и измеряет результат;
- **ops/kill switch** — быстро отключает дорогой или опасный путь;
- **permission flag** — долгоживущее правило доступа, не временный release toggle.

Правила эксплуатации: владелец, цель, default, дата удаления, аудит изменений и безопасное поведение при недоступности flag provider. Тестируйте минимум оба состояния и критичные комбинации. Flag не должен обходить authorization.

## Стратегии deployment

| Стратегия | Механика | Преимущество | Цена/риск |
|---|---|---|---|
| Rolling | Постепенно заменяет экземпляры | Не нужна двойная ёмкость целиком | Версии сосуществуют; нужен совместимый контракт |
| Blue-green | Новая среда рядом, traffic switch целиком | Быстрое переключение назад | Двойная ёмкость, state/schema остаются общими |
| Canary | Малой доле traffic/users дают новую версию | Проверка реальным трафиком с малым blast radius | Нужны маршрутизация, метрики и статистически осмысленные gates |

### Rolling deployment

В Kubernetes Deployment `RollingUpdate` управляется `maxUnavailable` и `maxSurge`. Readiness не даёт неподготовленному Pod трафик, startup probe защищает медленный старт, PodDisruptionBudget относится к voluntary disruptions и не заменяет rollout settings.

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: orders
spec:
  replicas: 6
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 0
      maxSurge: 1
  minReadySeconds: 30
```

### Blue-green

Blue обслуживает traffic, green проходит smoke tests и warm-up. Переключается router/service; старую среду сохраняют на rollback horizon. Sticky sessions, background jobs и consumers требуют отдельного плана: простой switch HTTP Service их не переключает. Общая БД должна поддерживать обе версии.

### Canary

Шаги могут быть 1% → 5% → 25% → 50% → 100% с паузами, но числа выбирают по объёму трафика и времени обнаружения ошибки. Сравнивайте canary с control по error rate, latency, saturation и бизнес-метрикам; разделяйте signal версии по labels. При малом трафике используйте synthetic tests или более длинное окно.

Canary по случайным запросам опасен для stateful flow: маршрутизируйте стабильно по user/tenant, если последовательность должна видеть одну версию.

## Health gates и rollback

Deployment успешен не потому, что Pods `Ready`, а потому что service-level health остаётся допустимым. Gate должен иметь:

- query, baseline и окно наблюдения;
- допустимый threshold и minimum sample size;
- действие: pause, abort/rollback или ручное решение;
- защиту от отсутствующих/запаздывающих данных.

```bash
kubectl --context prod -n orders rollout status deployment/orders --timeout=10m
kubectl --context prod -n orders rollout history deployment/orders
kubectl --context prod -n orders rollout undo deployment/orders --to-revision=17
```

`rollout undo` возвращает pod template, но не отменяет внешние side effects, schema migrations, events и flag/config changes. Поэтому заранее проверяйте:

1. предыдущий artifact доступен по digest;
2. схема и сообщения обратно совместимы;
3. конфигурация/flags имеют известную предыдущую revision;
4. повтор deployment идемпотентен;
5. если rollback небезопасен, есть быстрый roll-forward/kill switch.

## Практический runbook

**До rollout:**

- подтвердить digest/provenance/SBOM и approvals;
- проверить backups там, где они действительно восстанавливаемы, capacity и on-call;
- применить expand migration, дождаться replication/health;
- зафиксировать dashboard, alerts, rollback target и change record.

**Во время:**

- deploy с выключенным release flag или малым canary;
- наблюдать технические и бизнес-SLI на каждом шаге;
- не продолжать при неполных telemetry данных;
- записывать решения автоматики и оператора.

**После:**

- подтвердить SLO и background jobs/consumers;
- завершить backfill и reconciliation;
- удалить временный flag и contract-код после rollback horizon;
- закрыть change record фактическими метриками и результатом.

## Audit trail

Записывайте actor, approval, pipeline/config revision, artifact digest, DB migration version, flag change, traffic steps, метрики, timestamps и причину rollback. Связывайте deployment с PR и инцидентом, но не помещайте секреты или персональные данные в audit events.

## Типичные ошибки

- **Readiness считается бизнес-проверкой:** добавить SLI и synthetic/business checks.
- **Canary получает мало событий:** установить minimum sample size и адекватное окно.
- **Rollback планируется после сбоя:** репетировать его и контролировать retention.
- **Flag остаётся навсегда:** назначить owner и expiry/removal issue.
- **Schema меняется несовместимо:** expand/contract и отдельный backfill.

## Практические упражнения

1. Напишите runbook canary для сервиса с 100 запросами в минуту и определите minimum sample.
2. Разложите переименование обязательной колонки на expand/contract deployments.
3. Проведите game day: rollback приложения после уже начавшегося backfill.

## Вопросы для самопроверки

1. **Blue-green гарантирует простой rollback?** Нет: shared data, migrations и side effects могут быть несовместимы.
2. **Чем canary отличается от rolling?** Canary ограничивает exposure и оценивает новую версию по gates; rolling прежде всего постепенно заменяет capacity.
3. **Зачем отделять release от deployment?** Чтобы доставить и проверить код до управляемого включения поведения.
4. **Почему down migration не основной rollback?** Она может терять данные и блокировать таблицы; совместимый roll-forward обычно безопаснее.

## Связанные темы

- [Артефакты и promotion](03-артефакты-и-promotion.md)
- [Kubernetes](../kubernetes/README.md)
- [AWS](../aws/README.md)
- [Эволюция системы и миграции без простоя](../system%20design/12-эволюция-системы-и-миграции-без-простоя.md)
