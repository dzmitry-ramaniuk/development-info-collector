# System Design

Комплексный раздел по проектированию масштабируемых, надёжных и наблюдаемых систем. Материалы помогут понять базовые компромиссы архитектуры, научиться выбирать подходящие компоненты и уверенно обсуждать решения на собеседованиях.

## 📚 Содержание

1. [Основы и архитектурные компромиссы](01-основы-и-архитектурные-компромиссы.md)
   - latency, throughput, availability, consistency
   - CAP, PACELC и цена компромиссов
   - SLA, SLO, error budget
   - нефункциональные требования и оценка нагрузки

2. [Сетевое взаимодействие и API](02-сетевое-взаимодействие-и-api.md)
    - HTTP, gRPC, WebSocket и очереди
    - идемпотентность, ретраи, таймауты
    - polling, webhooks и push-модели
    - load balancer, reverse proxy, API gateway
    - rate limiting, versioning и контракт API
    - внешний и внутренний контракт сервиса

3. [Хранение данных и выбор базы](03-хранение-данных-и-выбор-базы.md)
    - SQL vs NoSQL
    - репликация, шардирование, индексы
    - consistent hashing и object storage
    - изоляция, блокировки и contention
    - CQRS, read/write patterns
    - транзакции и согласованность данных

4. [Кэширование](04-кэширование.md)
   - cache-aside, write-through, write-behind
   - local cache и distributed cache
   - TTL, eviction, invalidation
   - защита от cache stampede

5. [Асинхронность и событийные системы](05-асинхронность-и-событийные-системы.md)
   - broker, queue, topic, stream
   - at-most-once, at-least-once, exactly-once
   - outbox, saga, DLQ, retry topics
   - event-driven integration

6. [Масштабирование, надёжность и отказоустойчивость](06-масштабирование-надежность-и-отказоустойчивость.md)
   - load balancing и горизонтальное масштабирование
   - health checks, failover, circuit breaker
   - multi-AZ, disaster recovery, graceful degradation
   - bottleneck analysis и capacity planning

7. [Микросервисы, границы и декомпозиция](07-микросервисы-границы-и-декомпозиция.md)
   - bounded context и service boundaries
   - synchronous vs asynchronous collaboration
   - shared nothing и anti-pattern distributed monolith
   - evolutionary architecture

8. [Наблюдаемость, безопасность и эксплуатация](08-наблюдаемость-безопасность-и-эксплуатация.md)
   - logs, metrics, traces
   - authentication, authorization, secrets
   - SLI/SLO, alerting, runbooks
   - production readiness checklist

9. [Разбор популярных задач и подготовка к интервью](09-разбор-популярных-задач-и-подготовка-к-интервью.md)
   - как проходить system design interview
   - шаблон разбора задачи
   - кейсы: URL shortener, chat, news feed, rate limiter
   - список типовых вопросов и ответов

10. [Multi-region и geo-distributed системы](10-multi-region-и-geo-distributed-системы.md)
    - active-passive и active-active
    - routing, data locality, failover
    - RTO, RPO и межрегиональная репликация
    - когда multi-region действительно оправдан

11. [CDC, event sourcing и materialized views](11-cdc-event-sourcing-и-materialized-views.md)
    - change data capture и data propagation
    - event sourcing и replay
    - projection/read models
    - trade-offs между простотой и эволюцией данных

12. [Эволюция системы и миграции без простоя](12-эволюция-системы-и-миграции-без-простоя.md)
    - strangler fig и incremental migration
    - online schema change, backfill, dual-read/dual-write
    - feature flags и безопасный rollout
    - как менять архитектуру без остановки бизнеса

## 🧭 Рекомендуемые маршруты по разделу

- **Для уверенной базы**: 01 → 02 → 03 → 04 → 06
- **Для backend-разработчика**: 01 → 02 → 03 → 05 → 06 → 08
- **Для подготовки к собеседованию**: 01 → 03 → 04 → 05 → 06 → 09
- **Для advanced-погружения**: 03 → 05 → 06 → 10 → 11 → 12
- **Для техлида или архитектора**: изучайте весь раздел по порядку, связывая материалы с текущими системами команды

## 🎯 Как использовать

### Для начинающих
1. Начните с компромиссов и нефункциональных требований, а не с технологий.
2. На каждом шаге спрашивайте себя: где узкое место, где точка отказа, где цена масштабирования.
3. Сопоставляйте теорию с привычными системами: интернет-магазин, чат, платёжный сервис, analytics pipeline.

### Для подготовки к собеседованиям
1. Привыкайте обсуждать требования вслух: нагрузка, SLA, consistency, storage, failure handling.
2. Не пытайтесь сразу рисовать идеальную архитектуру — сначала фиксируйте assumptions и ограничения.
3. После выбора решения всегда проговаривайте trade-offs и альтернативы.

### Для практикующих инженеров
1. Используйте материалы как чек-лист при проектировании новых сервисов.
2. Возвращайтесь к разделам про данные, отказоустойчивость и observability перед запуском в production.
3. Сравнивайте принятые в команде решения с описанными паттернами и антипаттернами.

## 📈 Как выйти на intermediate/advanced уровень

### Intermediate
- Научитесь не просто перечислять компоненты, а объяснять **trade-offs**: почему здесь нужен кэш, а не реплика; почему здесь async лучше sync.
- Потренируйтесь считать order of magnitude: RPS, размер данных, read/write ratio, peak factor.
- Разберите типовые паттерны глубже: outbox, saga, CQRS, read replicas, rate limiting, graceful degradation.
- Добавьте в словарь обязательные distinctions: load balancer vs reverse proxy vs API gateway, polling vs webhooks, object storage vs OLTP database.
- Связывайте design с эксплуатацией: health checks, retry budgets, alerting, rollback, disaster recovery.

### Advanced
- Изучите multi-region и geo-distributed сценарии: latency-based routing, active-active vs active-passive, RPO/RTO.
- Углубитесь в data-intensive темы: CDC, stream processing, materialized views, hot partitions, storage internals.
- Освойте migration patterns: strangler fig, online schema changes, dual write avoidance, backfill.
- Тренируйтесь обсуждать не только «как построить», но и **как эволюционировать** систему без остановки бизнеса.
- Для закрепления переходите к страницам [10](10-multi-region-и-geo-distributed-системы.md), [11](11-cdc-event-sourcing-и-materialized-views.md) и [12](12-эволюция-системы-и-миграции-без-простоя.md).

### Что изучать после этого раздела
- Для событийной интеграции и streaming: [Apache Kafka](../очереди/кафка/README.md)
- Для транзакций, репликации, индексов и партиционирования: [PostgreSQL](../базы данных/postgresql/README.md)
- Для production-развёртывания и операционной устойчивости: [Kubernetes](../kubernetes/README.md)

## 💡 Рекомендации

- **Сначала требования, потом компоненты**: без понимания нагрузки и ограничений любые схемы бесполезны.
- **Ищите bottleneck**: чаще всего system design — это история про самое слабое звено.
- **Не оптимизируйте всё сразу**: усложнение архитектуры должно окупаться конкретной потребностью.
- **Проектируйте под отказ**: сети, базы, брокеры и сервисы периодически недоступны.
- **Следите за данными**: неправильный выбор модели хранения и консистентности бьёт по системе сильнее, чем выбор framework.
- **Думайте категориями building blocks**: routing, storage, cache, queue, search, object storage, observability и security должны складываться в целостную систему.

## ⚠️ Важные замечания

> **System design — это не набор шаблонов**: одна и та же задача может иметь несколько хороших решений при разных требованиях.

> **Нет бесплатной масштабируемости**: кэш, шардирование, event-driven integration и multi-region усложняют эксплуатацию и сопровождение.

> **Интервью и production — не одно и то же**: на интервью важно показать мышление и компромиссы, а в production — ещё и стоимость, сроки, зрелость команды.

## 🔗 Связанные темы

- [Базы данных](../базы данных/README.md)
- [Очереди](../очереди/README.md)
- [Kubernetes](../kubernetes/README.md)
- [Паттерны проектирования](../паттерны проектирования/README.md)
- [Apache Kafka](../очереди/кафка/README.md) — практический мост к event-driven архитектуре
- [PostgreSQL](../базы данных/postgresql/README.md) — практический мост к хранению данных, индексам и транзакциям
- [Kubernetes](../kubernetes/README.md) — практический мост к эксплуатации, масштабированию и observability

---

[← Назад к главной странице](../README.md)
