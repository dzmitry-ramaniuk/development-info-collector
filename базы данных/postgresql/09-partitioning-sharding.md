# Партиционирование и шардинг в PostgreSQL

## Содержание

1. [Введение](#введение)
2. [Партиционирование (Partitioning)](#партиционирование-partitioning)
   - [Типы партиционирования](#типы-партиционирования)
   - [Декларативное партиционирование](#декларативное-партиционирование)
   - [Управление партициями](#управление-партициями)
   - [Производительность и оптимизация](#производительность-и-оптимизация)
   - [Ограничения и особенности](#ограничения-и-особенности)
3. [Шардинг (Sharding)](#шардинг-sharding)
   - [Концепция шардинга](#концепция-шардинга)
   - [Подходы к шардингу в PostgreSQL](#подходы-к-шардингу-в-postgresql)
   - [Citus — расширение для шардинга](#citus--расширение-для-шардинга)
   - [Ручная реализация шардинга](#ручная-реализация-шардинга)
   - [Сравнение с партиционированием](#сравнение-с-партиционированием)
4. [Best Practices](#best-practices)
5. [Вопросы для собеседования](#вопросы-для-собеседования)

## Введение

**Партиционирование** и **шардинг** — два подхода к горизонтальному разделению данных, которые решают проблемы масштабирования, производительности и управления большими объёмами данных.

- **Партиционирование** — разделение одной большой таблицы на несколько физических частей (партиций) внутри одной базы данных, при этом логически таблица остаётся единой.
- **Шардинг** — распределение данных между несколькими независимыми серверами (шардами), каждый из которых содержит часть общего набора данных.

PostgreSQL имеет встроенную поддержку партиционирования начиная с версии 10 (декларативное партиционирование), а для шардинга существуют расширения и внешние решения.

## Партиционирование (Partitioning)

### Типы партиционирования

PostgreSQL поддерживает три основных стратегии партиционирования:

#### 1. Range Partitioning (По диапазону)
Разделение по диапазонам значений столбца. Наиболее распространённый тип.

**Применение:**
- Временные ряды (логи, метрики, события)
- Данные с естественной последовательностью (даты, числовые ID)
- Архивирование старых данных

**Пример:**
```sql
-- Создание партиционированной таблицы
CREATE TABLE measurements (
    id BIGSERIAL,
    measured_at TIMESTAMP NOT NULL,
    temperature NUMERIC,
    humidity NUMERIC
) PARTITION BY RANGE (measured_at);

-- Создание партиций
CREATE TABLE measurements_2024_01 PARTITION OF measurements
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE measurements_2024_02 PARTITION OF measurements
    FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');

CREATE TABLE measurements_2024_03 PARTITION OF measurements
    FOR VALUES FROM ('2024-03-01') TO ('2024-04-01');
```

#### 2. List Partitioning (По списку)
Разделение по конкретным значениям столбца.

**Применение:**
- Географическое разделение (страны, регионы)
- Категории или типы (статусы, департаменты)
- Фиксированные перечисления

**Пример:**
```sql
CREATE TABLE orders (
    id BIGSERIAL,
    order_date DATE NOT NULL,
    country_code VARCHAR(2) NOT NULL,
    amount NUMERIC
) PARTITION BY LIST (country_code);

CREATE TABLE orders_eu PARTITION OF orders
    FOR VALUES IN ('DE', 'FR', 'IT', 'ES', 'NL');

CREATE TABLE orders_na PARTITION OF orders
    FOR VALUES IN ('US', 'CA', 'MX');

CREATE TABLE orders_asia PARTITION OF orders
    FOR VALUES IN ('CN', 'JP', 'IN', 'KR');

CREATE TABLE orders_other PARTITION OF orders
    DEFAULT;  -- Для всех остальных значений (PostgreSQL 11+)
```

#### 3. Hash Partitioning (По хешу)
Равномерное распределение данных с помощью хеш-функции.

**Применение:**
- Равномерное распределение нагрузки
- Данные без естественного разделения
- Балансировка при отсутствии временного или категориального деления

**Пример:**
```sql
CREATE TABLE users (
    id BIGSERIAL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
) PARTITION BY HASH (id);

-- Создание 4 партиций для равномерного распределения
CREATE TABLE users_part_0 PARTITION OF users
    FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE users_part_1 PARTITION OF users
    FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE users_part_2 PARTITION OF users
    FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE users_part_3 PARTITION OF users
    FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

### Декларативное партиционирование

Начиная с PostgreSQL 10, декларативное партиционирование является рекомендуемым подходом.

**Преимущества:**
- Простой и понятный синтаксис
- Автоматическая маршрутизация запросов (partition pruning)
- Поддержка ограничений и индексов на партициях
- Возможность создания подпартиций (multi-level partitioning)

#### Многоуровневое партиционирование

```sql
-- Первый уровень — по году
CREATE TABLE sales (
    id BIGSERIAL,
    sale_date DATE NOT NULL,
    region VARCHAR(50) NOT NULL,
    amount NUMERIC
) PARTITION BY RANGE (sale_date);

-- Второй уровень — по региону
CREATE TABLE sales_2024 PARTITION OF sales
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01')
    PARTITION BY LIST (region);

CREATE TABLE sales_2024_eu PARTITION OF sales_2024
    FOR VALUES IN ('EU-WEST', 'EU-EAST', 'EU-NORTH');

CREATE TABLE sales_2024_na PARTITION OF sales_2024
    FOR VALUES IN ('NA-US', 'NA-CA');
```

### Управление партициями

#### Добавление новых партиций

```sql
-- Автоматизация через функцию
CREATE OR REPLACE FUNCTION create_monthly_partition(
    table_name TEXT,
    start_date DATE
) RETURNS VOID AS $$
DECLARE
    partition_name TEXT;
    end_date DATE;
BEGIN
    partition_name := table_name || '_' || to_char(start_date, 'YYYY_MM');
    end_date := start_date + INTERVAL '1 month';
    
    EXECUTE format(
        'CREATE TABLE IF NOT EXISTS %I PARTITION OF %I FOR VALUES FROM (%L) TO (%L)',
        partition_name, table_name, start_date, end_date
    );
END;
$$ LANGUAGE plpgsql;

-- Использование
SELECT create_monthly_partition('measurements', '2024-04-01');
```

#### Удаление старых партиций

```sql
-- Отсоединение партиции (данные сохраняются)
ALTER TABLE measurements DETACH PARTITION measurements_2023_01;

-- Удаление партиции
DROP TABLE measurements_2023_01;

-- Архивирование перед удалением
CREATE TABLE archive.measurements_2023_01 AS 
SELECT * FROM measurements_2023_01;

ALTER TABLE measurements DETACH PARTITION measurements_2023_01;
DROP TABLE measurements_2023_01;
```

#### Присоединение существующей таблицы

```sql
-- Создаём таблицу с нужной структурой
CREATE TABLE measurements_2024_04 (LIKE measurements INCLUDING ALL);

-- Добавляем ограничение для валидации
ALTER TABLE measurements_2024_04 
    ADD CONSTRAINT measurements_2024_04_check 
    CHECK (measured_at >= '2024-04-01' AND measured_at < '2024-05-01');

-- Присоединяем как партицию
ALTER TABLE measurements 
    ATTACH PARTITION measurements_2024_04 
    FOR VALUES FROM ('2024-04-01') TO ('2024-05-01');
```

### Производительность и оптимизация

#### Partition Pruning (Исключение партиций)

PostgreSQL автоматически исключает ненужные партиции при выполнении запросов:

```sql
-- План покажет только нужные партиции
EXPLAIN SELECT * FROM measurements 
WHERE measured_at BETWEEN '2024-02-01' AND '2024-02-15';

-- Проверка настроек
SHOW enable_partition_pruning;  -- должно быть ON
```

#### Индексы на партициях

```sql
-- Индекс на родительской таблице автоматически создаётся на всех партициях
CREATE INDEX idx_measurements_temp ON measurements (temperature);

-- Локальные индексы для конкретных партиций
CREATE INDEX idx_measurements_2024_01_humidity 
    ON measurements_2024_01 (humidity);
```

#### Constraint Exclusion (старый механизм)

Для наследования (устаревший подход до PostgreSQL 10):

```sql
-- Включение constraint exclusion
SET constraint_exclusion = partition;

-- Или глобально в postgresql.conf
-- constraint_exclusion = partition
```

### Ограничения и особенности

**Важные моменты:**

1. **Первичные ключи и уникальные индексы** должны включать столбцы партиционирования:
```sql
-- Правильно
CREATE TABLE events (
    id BIGSERIAL,
    event_date DATE NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id, event_date)
) PARTITION BY RANGE (event_date);

-- Ошибка: PRIMARY KEY должен включать event_date
-- CREATE TABLE events (
--     id BIGSERIAL PRIMARY KEY,
--     event_date DATE NOT NULL
-- ) PARTITION BY RANGE (event_date);
```

2. **Внешние ключи:**
   - Партиционированная таблица не может быть целью FK из непартиционированной
   - FK из партиционированной таблицы должен включать ключ партиционирования

3. **Триггеры:**
   - Row-level триггеры создаются на партициях, не на родительской таблице
   - Statement-level триггеры работают на родительской таблице

4. **Производительность:**
   - Большое количество партиций (>1000) может замедлить планирование запросов
   - Оптимальное число партиций зависит от нагрузки (обычно десятки-сотни)

## Шардинг (Sharding)

### Концепция шардинга

**Шардинг** — это распределение данных между несколькими независимыми базами данных (шардами), где каждый шард содержит уникальный поднабор данных.

**Отличия от партиционирования:**

| Характеристика | Партиционирование | Шардинг |
|----------------|-------------------|---------|
| Расположение | Один сервер | Несколько серверов |
| Масштабирование | Вертикальное | Горизонтальное |
| Управление | Встроено в PostgreSQL | Требует дополнительных инструментов |
| Сложность | Низкая | Высокая |
| Доступность | Зависит от одного сервера | Распределённая |

**Когда нужен шардинг:**
- Данные не помещаются на одном сервере
- Одного сервера недостаточно для обработки нагрузки
- Требуется географическое распределение данных
- Необходимо изолировать данные разных клиентов (multi-tenancy)

### Подходы к шардингу в PostgreSQL

#### 1. Foreign Data Wrapper (FDW)

Использование `postgres_fdw` для доступа к данным на других серверах:

```sql
-- На координирующем сервере
CREATE EXTENSION postgres_fdw;

-- Создание серверов
CREATE SERVER shard1 
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'shard1.example.com', port '5432', dbname 'mydb');

CREATE SERVER shard2 
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'shard2.example.com', port '5432', dbname 'mydb');

-- Маппинг пользователей
CREATE USER MAPPING FOR current_user
    SERVER shard1
    OPTIONS (user 'app_user', password 'secret');

CREATE USER MAPPING FOR current_user
    SERVER shard2
    OPTIONS (user 'app_user', password 'secret');

-- Создание внешних таблиц
CREATE FOREIGN TABLE users_shard1 (
    id BIGINT,
    username VARCHAR(100),
    email VARCHAR(255),
    created_at TIMESTAMP
) SERVER shard1 OPTIONS (schema_name 'public', table_name 'users');

CREATE FOREIGN TABLE users_shard2 (
    id BIGINT,
    username VARCHAR(100),
    email VARCHAR(255),
    created_at TIMESTAMP
) SERVER shard2 OPTIONS (schema_name 'public', table_name 'users');

-- Объединение через VIEW
CREATE VIEW users_all AS
    SELECT * FROM users_shard1
    UNION ALL
    SELECT * FROM users_shard2;
```

**Партиционирование + FDW:**

```sql
-- Создание партиционированной таблицы с внешними партициями
CREATE TABLE users (
    id BIGINT,
    username VARCHAR(100),
    email VARCHAR(255),
    created_at TIMESTAMP
) PARTITION BY HASH (id);

-- Локальная партиция
CREATE TABLE users_local PARTITION OF users
    FOR VALUES WITH (MODULUS 3, REMAINDER 0);

-- Внешние партиции
CREATE FOREIGN TABLE users_shard1 PARTITION OF users
    FOR VALUES WITH (MODULUS 3, REMAINDER 1)
    SERVER shard1 OPTIONS (schema_name 'public', table_name 'users');

CREATE FOREIGN TABLE users_shard2 PARTITION OF users
    FOR VALUES WITH (MODULUS 3, REMAINDER 2)
    SERVER shard2 OPTIONS (schema_name 'public', table_name 'users');
```

#### 2. Application-level Sharding

Шардинг на уровне приложения — распределение запросов между базами данных в коде:

```python
# Пример на Python
import hashlib

def get_shard_connection(user_id, shard_connections):
    """Определяет шард на основе user_id"""
    shard_count = len(shard_connections)
    shard_id = hash(str(user_id)) % shard_count
    return shard_connections[shard_id]

# Конфигурация подключений
shard_connections = [
    psycopg2.connect("host=shard1.db dbname=mydb"),
    psycopg2.connect("host=shard2.db dbname=mydb"),
    psycopg2.connect("host=shard3.db dbname=mydb")
]

# Использование
user_id = 12345
conn = get_shard_connection(user_id, shard_connections)
cursor = conn.cursor()
cursor.execute("SELECT * FROM users WHERE id = %s", (user_id,))
```

**Преимущества:**
- Полный контроль над логикой распределения
- Нет зависимости от специфичных расширений
- Гибкость в выборе стратегии шардинга

**Недостатки:**
- Сложность в коде приложения
- Нет поддержки распределённых транзакций
- Сложность в поддержке JOIN между шардами

### Citus — расширение для шардинга

**Citus** — популярное расширение PostgreSQL для горизонтального масштабирования, превращающее PostgreSQL в распределённую базу данных.

#### Архитектура Citus

```
┌─────────────┐
│ Coordinator │  ← Точка входа для запросов
└──────┬──────┘
       │
   ┌───┴────┬────────┬────────┐
   ▼        ▼        ▼        ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│ Worker │ │ Worker │ │ Worker │ │ Worker │  ← Узлы с данными
│ Node 1 │ │ Node 2 │ │ Node 3 │ │ Node 4 │
└────────┘ └────────┘ └────────┘ └────────┘
```

#### Установка и настройка

```sql
-- На всех узлах (coordinator + workers)
CREATE EXTENSION citus;

-- На coordinator: регистрация worker-узлов
SELECT citus_add_node('worker1.example.com', 5432);
SELECT citus_add_node('worker2.example.com', 5432);
SELECT citus_add_node('worker3.example.com', 5432);

-- Проверка кластера
SELECT * FROM citus_get_active_worker_nodes();
```

#### Распределённые таблицы

```sql
-- Создание обычной таблицы
CREATE TABLE events (
    event_id BIGSERIAL,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(50),
    event_data JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Распределение таблицы по user_id
SELECT create_distributed_table('events', 'user_id');

-- Проверка распределения
SELECT * FROM citus_shards WHERE table_name::text = 'events';
```

**Типы таблиц в Citus:**

1. **Distributed tables** — шардированные таблицы
2. **Reference tables** — реплицируются на все узлы (справочники)
3. **Local tables** — только на coordinator

```sql
-- Reference таблица (для справочников)
CREATE TABLE countries (
    code VARCHAR(2) PRIMARY KEY,
    name VARCHAR(100)
);
SELECT create_reference_table('countries');

-- Теперь JOIN между events и countries эффективен
SELECT e.*, c.name 
FROM events e 
JOIN countries c ON e.country_code = c.code;
```

#### Colocation (Совместное размещение)

Таблицы с одинаковым ключом распределения размещаются вместе:

```sql
-- Пользователи и их заказы будут на одних шардах
CREATE TABLE users (
    user_id BIGINT PRIMARY KEY,
    username VARCHAR(100)
);
SELECT create_distributed_table('users', 'user_id');

CREATE TABLE orders (
    order_id BIGSERIAL,
    user_id BIGINT REFERENCES users(user_id),
    amount NUMERIC
);
SELECT create_distributed_table('orders', 'user_id');

-- JOIN работает эффективно, так как данные на одном узле
SELECT u.username, COUNT(o.*) as order_count
FROM users u
JOIN orders o USING (user_id)
GROUP BY u.username;
```

#### Репартиционирование

```sql
-- Изменение количества шардов
SELECT citus_rebalance_start();

-- Мониторинг репартиционирования
SELECT * FROM citus_rebalance_status();
```

### Ручная реализация шардинга

Пример простой схемы шардинга без использования расширений:

```sql
-- Таблица маппинга user_id -> shard_id
CREATE TABLE shard_mapping (
    user_id BIGINT PRIMARY KEY,
    shard_id INTEGER NOT NULL
);

-- Функция определения шарда
CREATE OR REPLACE FUNCTION get_shard_id(p_user_id BIGINT) 
RETURNS INTEGER AS $$
BEGIN
    -- Простой модульный хеш
    RETURN (p_user_id % 4) + 1;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Использование в приложении
-- Приложение определяет шард и подключается к нужной БД
-- SELECT get_shard_id(12345); -- вернёт номер шарда
```

### Сравнение с партиционированием

**Когда использовать партиционирование:**
- Данные помещаются на одном сервере
- Требуется улучшить производительность запросов
- Упрощение управления данными (архивирование)
- Нет необходимости в горизонтальном масштабировании

**Когда использовать шардинг:**
- Объём данных превышает ёмкость одного сервера
- Требуется горизонтальное масштабирование нагрузки
- Географическое распределение данных
- Изоляция данных (multi-tenancy на уровне БД)

**Гибридный подход:**
Можно комбинировать: шардинг для распределения между серверами + партиционирование внутри каждого шарда.

```sql
-- На каждом шарде: партиционирование по времени
CREATE TABLE events_shard1 (
    event_id BIGSERIAL,
    user_id BIGINT,
    created_at TIMESTAMP
) PARTITION BY RANGE (created_at);

CREATE TABLE events_shard1_2024_01 PARTITION OF events_shard1
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

## Best Practices

### Для партиционирования

1. **Выбор ключа партиционирования:**
   - Используйте столбец, который часто фильтруется в WHERE
   - Для временных рядов — дата/время
   - Равномерное распределение для hash-партиционирования

2. **Размер партиций:**
   - Оптимально: 10-100 ГБ на партицию
   - Слишком маленькие партиции — накладные расходы на планирование
   - Слишком большие — потеря преимуществ партиционирования

3. **Автоматизация:**
   - Создавайте партиции заранее (например, на месяц вперёд)
   - Автоматизируйте удаление старых партиций
   - Используйте pg_cron или внешние планировщики

4. **Индексы:**
   - Создавайте индексы на родительской таблице
   - Для специфичных партиций — дополнительные локальные индексы
   - Регулярно проверяйте использование индексов

5. **Мониторинг:**
```sql
-- Размеры партиций
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE tablename LIKE 'measurements_%'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Статистика по партициям
SELECT * FROM pg_stat_user_tables 
WHERE schemaname = 'public' AND tablename LIKE 'measurements_%';
```

### Для шардинга

1. **Выбор ключа шардирования:**
   - Выбирайте столбец с высокой кардинальностью
   - Избегайте hotspot (неравномерного распределения)
   - Учитывайте паттерны доступа к данным

2. **Количество шардов:**
   - Планируйте с запасом на рост
   - Изменение количества шардов — сложная операция
   - Типично: начинайте с 4-8 шардов, масштабируйте до 32-64

3. **Распределённые транзакции:**
   - По возможности избегайте транзакций между шардами
   - Используйте eventual consistency где применимо
   - Рассмотрите Saga pattern для сложных операций

4. **Резервное копирование:**
   - Каждый шард — отдельная БД, отдельное резервное копирование
   - Синхронизируйте время создания бэкапов
   - Тестируйте восстановление всего кластера

5. **Мониторинг:**
   - Следите за балансом нагрузки между шардами
   - Алерты на рассинхронизацию данных
   - Метрики сетевой задержки между узлами

## Вопросы для собеседования

1. **В чём разница между партиционированием и шардингом?**
   - Партиционирование — разделение внутри одной БД, шардинг — между серверами
   - Партиционирование управляется СУБД, шардинг требует координации
   - Партиционирование для вертикального масштабирования, шардинг — горизонтального

2. **Какие типы партиционирования поддерживает PostgreSQL?**
   - Range (по диапазону)
   - List (по списку значений)
   - Hash (по хешу)
   - Поддержка многоуровневого партиционирования

3. **Что такое partition pruning?**
   - Механизм автоматического исключения ненужных партиций при выполнении запроса
   - Улучшает производительность за счёт сканирования только релевантных партиций
   - Работает на этапе планирования запроса

4. **Какие ограничения есть у партиционированных таблиц?**
   - PRIMARY KEY и UNIQUE должны включать ключ партиционирования
   - Ограничения на внешние ключи
   - Некоторые операции DDL требуют действий на каждой партиции

5. **Как работает Citus?**
   - Coordinator узел принимает запросы и распределяет их
   - Worker узлы хранят шарды данных
   - Поддержка distributed tables, reference tables, local tables
   - Автоматическая маршрутизация запросов и агрегация результатов

6. **Как выбрать между партиционированием и шардингом?**
   - Партиционирование: данные на одном сервере, улучшение производительности
   - Шардинг: данные не помещаются на одном сервере, горизонтальное масштабирование
   - Можно комбинировать: шардинг + партиционирование на каждом шарде

7. **Что такое colocation в контексте шардинга?**
   - Размещение связанных данных на одних шардах
   - Позволяет эффективно выполнять JOIN
   - Достигается использованием одного ключа распределения для связанных таблиц

8. **Как автоматизировать управление партициями?**
   - Функции для создания партиций по расписанию
   - pg_cron или pg_partman для автоматизации
   - Скрипты для удаления старых партиций
   - Мониторинг размеров и использования партиций

9. **В чём проблема горячих точек (hotspots) при шардинге?**
   - Неравномерное распределение нагрузки между шардами
   - Возникает при плохом выборе ключа шардирования
   - Решение: использование hash-based шардирования, пересмотр ключа

10. **Как обеспечить консистентность при шардинге?**
    - Избегать распределённых транзакций
    - Использовать eventual consistency
    - Saga pattern для сложных операций
    - Two-phase commit (2PC) для строгой консистентности (с осторожностью)

---

**Дополнительные ресурсы:**
- [Официальная документация PostgreSQL: Partitioning](https://www.postgresql.org/docs/current/ddl-partitioning.html)
- [Документация Citus](https://docs.citusdata.com/)
- [pg_partman - расширение для управления партициями](https://github.com/pgpartman/pg_partman)
- [Статья о масштабировании PostgreSQL](https://www.postgresql.org/docs/current/different-replication-solutions.html)
