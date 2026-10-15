-- PostgreSQL 16–18. Только отдельная учебная БД; требуется CREATE SCHEMA.
-- psql -X -v ON_ERROR_STOP=1 -d demo -f 01-partition-constraints.sql
BEGIN;
CREATE SCHEMA partition_constraints_demo;
SET LOCAL search_path = partition_constraints_demo, pg_catalog;

CREATE TABLE owner (id integer PRIMARY KEY);
INSERT INTO owner VALUES (1);
CREATE TABLE event (
    id bigint NOT NULL,
    event_date date NOT NULL,
    owner_id integer NOT NULL REFERENCES owner(id),
    tagged boolean NOT NULL DEFAULT false,
    PRIMARY KEY (id, event_date)
) PARTITION BY RANGE (event_date);
CREATE TABLE event_january PARTITION OF event
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');

-- Обычная таблица ссылается на partitioned parent.
CREATE TABLE event_reference (
    event_id bigint,
    event_date date,
    FOREIGN KEY (event_id, event_date) REFERENCES event(id, event_date)
);
CREATE FUNCTION tag_event() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    NEW.tagged := true;
    RETURN NEW;
END;
$$;
CREATE TRIGGER tag_event BEFORE INSERT ON event
    FOR EACH ROW EXECUTE FUNCTION tag_event();
-- Будущая partition получает клон row-trigger.
CREATE TABLE event_february PARTITION OF event
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
INSERT INTO event(id, event_date, owner_id) VALUES
    (1, '2026-01-10', 1), (2, '2026-02-10', 1);
INSERT INTO event_reference VALUES (1, '2026-01-10');

DO $$
BEGIN
    IF (SELECT count(*) FROM event WHERE tagged) <> 2 THEN
        RAISE EXCEPTION 'Row-trigger не обработал обе partitions';
    END IF;
    BEGIN
        INSERT INTO event(id, event_date, owner_id) VALUES (3, '2026-01-11', 999);
        RAISE EXCEPTION 'Исходящий FK не проверен';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
    BEGIN
        INSERT INTO event_reference VALUES (999, '2026-01-10');
        RAISE EXCEPTION 'Входящий FK не проверен';
    EXCEPTION WHEN foreign_key_violation THEN NULL;
    END;
END;
$$;
SELECT 'FK и row-trigger проверки пройдены' AS result;
ROLLBACK;
