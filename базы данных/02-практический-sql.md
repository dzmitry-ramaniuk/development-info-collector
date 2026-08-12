---
---

# Практический SQL

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Логическая обработка и JOIN](#логическая-обработка-и-join)
3. [Подзапросы и CTE](#подзапросы-и-cte)
4. [Агрегаты и NULL](#агрегаты-и-null)
5. [Оконные функции](#оконные-функции)
6. [Сквозной пример](#сквозной-пример)
7. [Типичные ошибки](#типичные-ошибки)
8. [Упражнения](#упражнения)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 4 августа 2026 года.
- **Целевая база:** стандартные конструкции SQL и PostgreSQL 18; особенности PostgreSQL отмечены явно.
- **Статус примеров:** `current`.
- **Первичные источники:** [PostgreSQL 18: Queries](https://www.postgresql.org/docs/18/queries.html), [Window Functions](https://www.postgresql.org/docs/18/tutorial-window.html), [Aggregate Functions](https://www.postgresql.org/docs/18/functions-aggregate.html), [ISO/IEC 9075-2:2023](https://www.iso.org/standard/76584.html).

Примеры продолжают [схему сервиса бронирования](01-реляционная-модель-и-нормализация.html).

## Логическая обработка и JOIN

Ментальная модель: `FROM/JOIN` → `WHERE` → `GROUP BY` → `HAVING` → `SELECT`/окна → `DISTINCT` → `ORDER BY` → `LIMIT`. Физический план оптимизатора может быть иным.

| JOIN | Результат | Применение |
|---|---|---|
| `INNER` | Только пары | Брони с комнатами |
| `LEFT` | Все слева, отсутствующее справа как `NULL` | Включить комнаты без броней |
| `FULL` | Несовпавшие строки обеих сторон и пары | Сверка наборов |
| `CROSS` | Декартово произведение | Все даты × комнаты |

```sql
SELECT r.room_id, r.name, count(b.booking_id) AS booking_count
FROM room AS r
LEFT JOIN booking AS b
  ON b.room_id = r.room_id
 AND b.status = 'planned'
GROUP BY r.room_id, r.name;
```

Если перенести фильтр правой таблицы в `WHERE`, комнаты без брони исчезнут. При M:N соединяйте через таблицу связи:

```sql
SELECT b.booking_id, u.display_name, bp.response
FROM booking AS b
JOIN booking_participant AS bp USING (booking_id)
JOIN app_user AS u USING (user_id)
WHERE b.booking_id = 42;
```

## Подзапросы и CTE

`EXISTS` проверяет наличие, не размножая внешние строки. `NOT IN` при `NULL` в подзапросе может дать `UNKNOWN` для всех кандидатов; безопаснее `NOT EXISTS`.

```sql
SELECT r.room_id, r.name
FROM room AS r
WHERE NOT EXISTS (
    SELECT 1 FROM booking AS b
    WHERE b.room_id = r.room_id
      AND b.status = 'planned'
      AND b.starts_at < TIMESTAMPTZ '2026-08-05 11:00+00'
      AND b.ends_at > TIMESTAMPTZ '2026-08-05 10:00+00'
);
```

**CTE** именует промежуточный результат; рекурсивный CTE обходит граф. В PostgreSQL 18 нерекурсивный CTE без побочных эффектов может встраиваться; `MATERIALIZED`/`NOT MATERIALIZED` применяют после анализа плана.

```sql
WITH daily_load AS (
    SELECT room_id, starts_at::date AS day, count(*) AS bookings
    FROM booking WHERE status = 'planned'
    GROUP BY room_id, starts_at::date
)
SELECT r.name, d.day, d.bookings
FROM daily_load AS d JOIN room AS r USING (room_id)
WHERE d.bookings >= 3;
```

## Агрегаты и NULL

`NULL` — неизвестное/отсутствующее значение, не ноль. `= NULL` даёт `UNKNOWN`: используйте `IS NULL` или PostgreSQL `IS [NOT] DISTINCT FROM`. В `WHERE` проходят только `TRUE`.

- `count(*)` считает строки, `count(expr)` — не-`NULL` значения.
- Кроме `count`, агрегаты PostgreSQL на пустом наборе обычно дают `NULL`; при предметном нуле нужен `coalesce`.
- `WHERE` фильтрует строки до группировки, `HAVING` — группы после неё.

```sql
SELECT room_id, count(*) AS total,
       count(*) FILTER (WHERE status = 'cancelled') AS cancelled,
       min(starts_at) AS first_start
FROM booking
GROUP BY room_id
HAVING count(*) >= 5;
```

## Оконные функции

Окно не сворачивает строки. `PARTITION BY` задаёт группу, `ORDER BY` — порядок, frame — набор относительно текущей строки.

```sql
SELECT booking_id, room_id, starts_at,
       row_number() OVER (
           PARTITION BY room_id ORDER BY starts_at, booking_id
       ) AS sequence_no,
       lag(ends_at) OVER (
           PARTITION BY room_id ORDER BY starts_at, booking_id
       ) AS previous_end,
       count(*) OVER (PARTITION BY room_id) AS room_total
FROM booking WHERE status = 'planned';
```

У `last_value` frame по умолчанию заканчивается на последнем peer текущей строки. Для всей partition задайте `ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING`.

## Сквозной пример

Три наиболее загруженные комнаты каждого дня с сохранением равных мест:

```sql
WITH room_day AS (
    SELECT room_id, starts_at::date AS day, count(*) AS booking_count,
           sum(ends_at - starts_at) AS booked_time
    FROM booking WHERE status = 'planned'
    GROUP BY room_id, starts_at::date
), ranked AS (
    SELECT room_day.*,
           dense_rank() OVER (PARTITION BY day ORDER BY booked_time DESC) AS place
    FROM room_day
)
SELECT r.name, x.day, x.booking_count, x.booked_time
FROM ranked AS x JOIN room AS r USING (room_id)
WHERE x.place <= 3
ORDER BY x.day, x.place, r.name;
```

Проверяйте план через `EXPLAIN (ANALYZE, BUFFERS)` на тестовых данных: `ANALYZE` выполняет запрос. Индексы разобраны в [PostgreSQL](postgresql/README.html), влияние ORM — в [Hibernate/JPA](../java/04-hibernate.html).

## Типичные ошибки

| Ошибка | Последствие | Исправление |
|---|---|---|
| Пропущен `ON` | Декартово произведение | Явный JOIN и проверка кардинальности |
| Фильтр справа после `LEFT JOIN` в `WHERE` | Потеря строк без пары | Перенести условие совпадения в `ON` |
| `count(*)` после 1:N | Завышенный счётчик | Агрегировать нужный уровень заранее |
| `NOT IN` с `NULL` | Неожиданно пусто | `NOT EXISTS` |
| `= NULL` | Не `TRUE` | `IS NULL` |
| `LIMIT` без полного `ORDER BY` | Недетерминированный набор | Добавить уникальный tie-breaker |
| Функция над индексируемой колонкой | Индекс может не подойти | Sargable-условие/expression index |

> ORM не устраняет необходимость читать SQL: N+1, размножение JOIN и ошибочная пагинация возникают на границе моделей.

## Упражнения

1. Исправьте `LEFT JOIN ... WHERE booking.status='planned'`, сохранив комнаты с нулём.
2. Найдите пользователей без участий через `NOT EXISTS`; объясните эффект `NULL` для `NOT IN`.
3. Выведите ближайшую бронь комнаты через `row_number()` и PostgreSQL `DISTINCT ON`; сравните переносимость.
4. Найдите пересечения броней self join, исключив самосоединение и зеркальные пары.
5. Исправьте отчёт, где одновременный JOIN участников и броней завышает `count(*)`.
6. Объясните отличие frame `ROWS 6 PRECEDING` от календарных семи дней.

## Вопросы для самопроверки

1. **Окно и `GROUP BY` чем различаются?**
   *Ответ:* группировка сворачивает строки, окно сохраняет их.
2. **Когда нужен `EXISTS`?**
   *Ответ:* когда важен факт связанной строки, но не её колонки и не размножение результата.
3. **Гарантирует ли CTE материализацию?**
   *Ответ:* нет; в PostgreSQL это зависит от запроса и указаний `MATERIALIZED`/`NOT MATERIALIZED`.
