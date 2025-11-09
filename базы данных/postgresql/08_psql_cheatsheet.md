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

Шпаргалка помогает быстро вспомнить команды `psql`, диагностические запросы и администрирование PostgreSQL без обращения к документации.
