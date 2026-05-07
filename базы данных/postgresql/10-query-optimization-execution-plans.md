# Оптимизация запросов и планы выполнения в PostgreSQL

## Содержание

1. [Когда нужна оптимизация](#1-когда-нужна-оптимизация)
2. [Главные инструменты PostgreSQL](#2-главные-инструменты-postgresql)
3. [Синтаксис EXPLAIN и EXPLAIN ANALYZE](#3-синтаксис-explain-и-explain-analyze)
4. [Как читать план выполнения](#4-как-читать-план-выполнения)
5. [Типовые проблемы и исправления](#5-типовые-проблемы-и-исправления)
6. [Полезные SQL-шаблоны для диагностики](#6-полезные-sql-шаблоны-для-диагностики)
7. [Практический чек-лист оптимизации](#7-практический-чек-лист-оптимизации)
8. [Антипаттерны](#8-антипаттерны)
9. [Вопросы для самопроверки](#9-вопросы-для-самопроверки)

## 1. Когда нужна оптимизация

Оптимизация нужна не «всегда», а когда есть измеримая проблема:
- высокий `p95/p99` latency;
- рост нагрузки CPU/IO;
- деградация после изменения схемы или релиза;
- нестабильные планы для одного и того же запроса.

> Сначала измеряем, потом оптимизируем. Без метрик легко «ускорить» не ту часть системы.

## 2. Главные инструменты PostgreSQL

- `EXPLAIN` — показывает **оценочный** план без выполнения.
- `EXPLAIN ANALYZE` — выполняет запрос и показывает **фактические** времена и количество строк.
- `pg_stat_statements` — агрегированная статистика по нормализованным запросам.
- `auto_explain` — автоматический лог планов медленных запросов.
- `pg_stat_activity` — активные сессии и ожидания.
- `pg_locks` — блокировки.
- `ANALYZE` / `VACUUM (ANALYZE)` — актуализация статистики и поддержка MVCC.

### Подключение нужных расширений

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

```sql
LOAD 'auto_explain';
SET auto_explain.log_min_duration = '300ms';
SET auto_explain.log_analyze = on;
SET auto_explain.log_buffers = on;
```

## 3. Синтаксис EXPLAIN и EXPLAIN ANALYZE

Базовый синтаксис:

```sql
EXPLAIN SELECT *
FROM orders
WHERE customer_id = 42;
```

С исполнением и подробностями:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE, COSTS, SETTINGS)
SELECT o.id, o.created_at, o.total_amount
FROM orders o
WHERE o.customer_id = 42
ORDER BY o.created_at DESC
LIMIT 50;
```

Машиночитаемый формат (удобен для CI/скриптов):

```sql
EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)
SELECT *
FROM payments
WHERE status = 'SUCCESS';
```

### Важные опции

- `ANALYZE` — фактическое выполнение.
- `BUFFERS` — hit/read/write по shared/local/temp буферам.
- `WAL` — генерация WAL (полезно для write-heavy сценариев).
- `TIMING` — детальные тайминги (иногда отключают для уменьшения overhead).
- `SETTINGS` — какие GUC реально повлияли на план.
- `SUMMARY` — итоговая сводка по времени/ресурсам.

## 4. Как читать план выполнения

Смотрите план сверху вниз как дерево операций.

1. Сравните `rows` (оценка) и `actual rows` (факт).
   - Большое расхождение => проблемы статистики или коррелированные данные.
2. Найдите самые дорогие узлы по `actual time`.
3. Проверьте тип сканирования:
   - `Seq Scan` — нормально для маленьких таблиц/низкой селективности.
   - `Index Scan` — хорошо для точечных/селективных выборок.
   - `Bitmap Heap Scan` — компромисс для средних выборок.
4. Проверьте операции `Sort` и `Hash`:
   - если есть `Disk:`/`temp read/write`, не хватает `work_mem`.
5. Оцените join-стратегию:
   - `Nested Loop`, `Hash Join`, `Merge Join` и почему выбран именно он.

## 5. Типовые проблемы и исправления

### Проблема: Seq Scan на большой таблице

Причины:
- нет нужного индекса;
- низкая селективность фильтра;
- статистика устарела.

Исправления:

```sql
CREATE INDEX CONCURRENTLY idx_orders_customer_created_at
  ON orders (customer_id, created_at DESC);
ANALYZE orders;
```

### Проблема: плохая оценка кардинальности

Исправления:

```sql
ALTER TABLE orders ALTER COLUMN status SET STATISTICS 1000;
ANALYZE orders;
```

Для зависимых колонок:

```sql
CREATE STATISTICS st_orders_customer_status (dependencies)
ON customer_id, status
FROM orders;
ANALYZE orders;
```

### Проблема: сортировки/хеши уходят на диск

Точечная настройка внутри транзакции:

```sql
BEGIN;
SET LOCAL work_mem = '128MB';
EXPLAIN (ANALYZE, BUFFERS)
SELECT customer_id, sum(total_amount)
FROM orders
GROUP BY customer_id;
COMMIT;
```

## 6. Полезные SQL-шаблоны для диагностики

Топ медленных запросов из `pg_stat_statements`:

```sql
SELECT query,
       calls,
       total_exec_time,
       mean_exec_time,
       rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

Запросы, которые чаще всего делают full scan:

```sql
SELECT relname,
       seq_scan,
       idx_scan,
       n_live_tup
FROM pg_stat_user_tables
ORDER BY seq_scan DESC
LIMIT 20;
```

Проверка блокировок:

```sql
SELECT a.pid,
       a.usename,
       a.state,
       a.wait_event_type,
       a.wait_event,
       a.query
FROM pg_stat_activity a
WHERE a.state <> 'idle'
ORDER BY a.query_start;
```

## 7. Практический чек-лист оптимизации

1. Выберите проблемный запрос по метрикам (`pg_stat_statements`, APM, логи).
2. Снимите baseline: latency, calls, rows, buffers.
3. Получите `EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON)`.
4. Найдите узел с максимальным вкладом во время.
5. Проверьте:
   - индексы (наличие, порядок колонок, селективность);
   - статистику (`ANALYZE`, `CREATE STATISTICS`);
   - SQL-переписывание (`EXISTS` vs `IN`, `JOIN` vs коррелированный подзапрос).
6. Проверьте эффект на реалистичных данных.
7. Введите изменение безопасно (`CREATE INDEX CONCURRENTLY`, feature flag, rollout).
8. Повторно снимите метрики и сравните с baseline.

## 8. Антипаттерны

- Оптимизация «вслепую» без baseline.
- Глобальное увеличение `work_mem` без оценки суммарной нагрузки.
- Много однотипных индексов «на всякий случай».
- Принудительное отключение `enable_seqscan`/`enable_hashjoin` в production как постоянная настройка.
- Игнорирование автообслуживания (`autovacuum`, `ANALYZE`).

## 9. Вопросы для самопроверки

1. Чем `EXPLAIN` отличается от `EXPLAIN ANALYZE`?
2. В каких случаях `Seq Scan` — это нормальный план?
3. Зачем нужен `BUFFERS` в `EXPLAIN`?
4. Когда стоит создавать `CREATE STATISTICS`, а не только индекс?
5. Почему `SET LOCAL work_mem` безопаснее, чем глобальный `SET work_mem`?
6. Как правильно сравнить эффект оптимизации до/после?

---

Грамотная оптимизация в PostgreSQL — это связка: корректный SQL, актуальная статистика, релевантные индексы и постоянная проверка метрик.
