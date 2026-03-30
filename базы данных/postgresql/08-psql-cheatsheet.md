# Шпаргалка по `psql` и полезным командам PostgreSQL


## Содержание

1. [1. Подключение и базовые команды](#1-подключение-и-базовые-команды)
2. [2. Работа со схемой](#2-работа-со-схемой)
3. [3. Выполнение запросов](#3-выполнение-запросов)
4. [4. Диагностика и статистика](#4-диагностика-и-статистика)
5. [5. Работа с конфигурацией](#5-работа-с-конфигурацией)
6. [6. Администрирование](#6-администрирование)
7. [7. Журналирование и обслуживание](#7-журналирование-и-обслуживание)
8. [8. Полезные настройки `psql`](#8-полезные-настройки-psql)
9. [9. Быстрый troubleshooting workflow](#9-быстрый-troubleshooting-workflow)
10. [10. Мини-шпаргалка по EXPLAIN/ANALYZE](#10-мини-шпаргалка-по-explainanalyze)
11. [11. Резервное копирование и восстановление](#11-резервное-копирование-и-восстановление)
12. [12. Диагностика bloat и роста объектов](#12-диагностика-bloat-и-роста-объектов)

## 1. Подключение и базовые команды
- `psql postgresql://user:pass@host:port/dbname` — подключение по URI.
- `\c dbname` — переключиться на другую базу.
- `\l` — список баз данных.
- `\dt [schema.]pattern` — таблицы, `\di` — индексы, `\dv` — представления.
- `\dn` — список схем, `\dx` — расширения.
- `\x` — переключение расширенного формата вывода.

## 2. Работа со схемой
- `\d table` — описание таблицы (столбцы, типы, индексы, ограничения).
- `\d+ table` — расширенное описание с размером, TOAST-таблицей, комментариями.
- `\db` — табличные пространства, `\da` — агрегаты, `\df` — функции.
- `COMMENT ON TABLE schema.table IS '...';` — документирование объектов.
- Автодополнение по клавише `Tab` помогает быстро вводить команды и имена объектов.

## 3. Выполнение запросов
- `\timing on` — вывод времени выполнения запросов.
- `\watch 5` — повторять последний запрос каждые 5 секунд (мониторинг).
- `\copy table TO 'file.csv' CSV HEADER` — выгрузка данных; `\copy table FROM 'file.csv' CSV` — загрузка.
- `\set AUTOCOMMIT off` — отключение автокоммита в psql.
- `\gexec` — выполнить результат предыдущего запроса как команды SQL.

## 4. Диагностика и статистика
- `SELECT * FROM pg_stat_activity WHERE state <> 'idle';` — активные запросы.
- `SELECT * FROM pg_locks;` — текущие блокировки.
- `SELECT datname, numbackends, xact_commit, blks_read, blks_hit FROM pg_stat_database;` — общие показатели по БД.
- `SELECT relname, seq_scan, idx_scan FROM pg_stat_user_tables ORDER BY seq_scan DESC LIMIT 20;` — таблицы с частыми последовательными сканированиями.
- `SELECT pid, wait_event_type, wait_event FROM pg_stat_activity WHERE wait_event IS NOT NULL;` — ожидания.

## 5. Работа с конфигурацией
- `SHOW ALL;` — текущие параметры.
- `SHOW work_mem;` / `SET work_mem = '256MB';` — чтение и установка параметров (в пределах сессии).
- `ALTER SYSTEM SET log_min_duration_statement = '500ms';` — изменение конфигурации на уровне кластера.
- `SELECT pg_reload_conf();` — перезагрузка конфигурации без рестарта.
- `SELECT current_setting('parameter', true);` — получить значение параметра с учётом конфигураций.

## 6. Администрирование
- `SELECT version();` — версия сервера.
- `\du` — список ролей, `CREATE ROLE/USER` — создание.
- `GRANT SELECT ON TABLE schema.table TO role;` — назначение прав.
- `REVOKE ALL ON DATABASE db FROM PUBLIC;` — отзыв прав.
- `CREATE EXTENSION pg_stat_statements;` — установка расширения.
- `VACUUM (VERBOSE, ANALYZE) table;` — ручной вакуум.

## 7. Журналирование и обслуживание
- `SELECT pg_size_pretty(pg_database_size('dbname'));` — размер базы.
- `SELECT pg_size_pretty(pg_total_relation_size('schema.table'));` — размер таблицы с индексами.
- `SELECT age(datfrozenxid) FROM pg_database WHERE datname = 'dbname';` — оценка риска wraparound.
- `SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) FROM pg_stat_replication;` — лаг репликации.
- `SELECT * FROM pg_stat_progress_vacuum;` — прогресс VACUUM.

## 8. Полезные настройки `psql`
- `.psqlrc` позволяет настраивать алиасы (`\set`, `\pset`), цвета, шаблоны.
- `\set VERBOSITY verbose` — подробные ошибки.
- `\set HISTSIZE 10000` — увеличить историю команд.
- `\encoding UTF8` — кодировка сессии.
- `\set PROMPT1 '%n@%/%R%# '` — кастомизация приглашения.

## 9. Быстрый troubleshooting workflow
Если база «тормозит» или приложение жалуется на таймауты, полезно идти коротким маршрутом:

1. Проверить активные запросы:
   ```sql
   SELECT pid, usename, state, wait_event_type, wait_event, query
   FROM pg_stat_activity
   WHERE state <> 'idle'
   ORDER BY query_start;
   ```
2. Посмотреть блокировки и кто кого ждёт:
   ```sql
   SELECT blocked.pid AS blocked_pid,
          blocking.pid AS blocking_pid,
          blocked.query AS blocked_query,
          blocking.query AS blocking_query
   FROM pg_stat_activity blocked
   JOIN pg_locks blocked_locks ON blocked.pid = blocked_locks.pid
   JOIN pg_locks blocking_locks
     ON blocked_locks.locktype = blocking_locks.locktype
    AND blocked_locks.database IS NOT DISTINCT FROM blocking_locks.database
    AND blocked_locks.relation IS NOT DISTINCT FROM blocking_locks.relation
    AND blocked_locks.page IS NOT DISTINCT FROM blocking_locks.page
    AND blocked_locks.tuple IS NOT DISTINCT FROM blocking_locks.tuple
    AND blocked_locks.virtualxid IS NOT DISTINCT FROM blocking_locks.virtualxid
    AND blocked_locks.transactionid IS NOT DISTINCT FROM blocking_locks.transactionid
    AND blocked_locks.classid IS NOT DISTINCT FROM blocking_locks.classid
    AND blocked_locks.objid IS NOT DISTINCT FROM blocking_locks.objid
    AND blocked_locks.objsubid IS NOT DISTINCT FROM blocking_locks.objsubid
    AND blocked_locks.pid <> blocking_locks.pid
   JOIN pg_stat_activity blocking ON blocking.pid = blocking_locks.pid
   WHERE NOT blocked_locks.granted
     AND blocking_locks.granted;
   ```
3. Проверить, не упирается ли система в последовательные сканирования, VACUUM или WAL:
   - `SELECT * FROM pg_stat_progress_vacuum;`
   - `SELECT relname, seq_scan, idx_scan FROM pg_stat_user_tables ORDER BY seq_scan DESC LIMIT 20;`
   - `SELECT pg_wal_lsn_diff(pg_current_wal_lsn(), replay_lsn) FROM pg_stat_replication;`

> **Практический совет**: сначала соберите факты из `pg_stat_activity`, `pg_locks` и `pg_stat_database`, а уже потом завершайте сессии через `pg_terminate_backend(pid)`. Иначе легко убрать симптом, но не понять причину.

## 10. Мини-шпаргалка по EXPLAIN/ANALYZE
- `EXPLAIN SELECT ...;` — только план выполнения без запуска запроса.
- `EXPLAIN ANALYZE SELECT ...;` — реально выполняет запрос и показывает фактические времена.
- `EXPLAIN (ANALYZE, BUFFERS) SELECT ...;` — добавляет статистику чтения буферов.
- `EXPLAIN (ANALYZE, VERBOSE, BUFFERS) SELECT ...;` — подробный вариант для сложной отладки.

Частые сигналы в плане:

| Признак | Что обычно означает | Что проверить |
|---------|---------------------|---------------|
| `Seq Scan` на большой таблице | Планировщик не выбрал индекс | Есть ли подходящий индекс, актуальна ли статистика |
| Большой `Rows Removed by Filter` | Читается много лишних строк | Селективность условий, составной индекс |
| `Sort` или `Hash` с большим временем | Недостаток памяти или неудачный план | `work_mem`, необходимость индекса |
| Сильное расхождение `rows=` и `actual rows=` | Устаревшая статистика | `ANALYZE`, автосбор статистики |

> **Важно**: `EXPLAIN ANALYZE` выполняет запрос по-настоящему. Для `UPDATE`, `DELETE` и тяжёлых `SELECT` его нужно запускать осторожно, желательно на staging или в транзакции с `ROLLBACK`.

## 11. Резервное копирование и восстановление
- `pg_dump -Fc -d dbname -f backup.dump` — логический backup в custom format.
- `pg_restore -d dbname --clean --if-exists backup.dump` — восстановление из custom backup.
- `pg_dumpall -g` — выгрузка глобальных объектов (роли, tablespace).
- `\! pg_dump -t schema.table dbname > table.sql` — запуск команды shell прямо из `psql`.

Полезные сценарии:
- сделать backup одной схемы: `pg_dump -n schema_name dbname > schema.sql`;
- восстановить только структуру: `pg_restore --schema-only -d dbname backup.dump`;
- восстановить только данные: `pg_restore --data-only -d dbname backup.dump`.

## 12. Диагностика bloat и роста объектов
- Таблицы и индексы со временем разрастаются из-за MVCC, `UPDATE/DELETE` и неидеального VACUUM.
- Быстрая первичная проверка размера объектов:
  ```sql
  SELECT schemaname,
         relname,
         pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
         n_live_tup,
         n_dead_tup
  FROM pg_stat_user_tables
  ORDER BY pg_total_relation_size(relid) DESC
  LIMIT 20;
  ```
- Если `n_dead_tup` стабильно велик, проверьте autovacuum, частоту `UPDATE/DELETE` и длительные транзакции.
- Для индексов полезно сравнивать размер и частоту использования:
  ```sql
  SELECT schemaname,
         relname,
         indexrelname,
         idx_scan,
         pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
  FROM pg_stat_user_indexes
  ORDER BY pg_relation_size(indexrelid) DESC
  LIMIT 20;
  ```
- Если индекс большой и почти не используется, возможно, он только замедляет запись и занимает диск.

Шпаргалка помогает быстро вспомнить команды `psql`, диагностические запросы, эксплуатационные сценарии и администрирование PostgreSQL без обращения к документации.
