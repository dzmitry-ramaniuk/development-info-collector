---
---

# Исполняемые упражнения Java Core и ORM

## Содержание

1. [Java Core](#java-core)
2. [ORM-практикум](#orm-практикум)
3. [Границы проверки](#границы-проверки)

## Java Core

[CoreExamples.java](CoreExamples.java) — самостоятельная программа без внешних зависимостей. Она проверяет:

- передачу копии ссылки и примитивного значения;
- защитную копию компонента record и две политики Strategy;
- UTF-8, закрытие `Files.lines`, bulk I/O, частичные записи ByteBuffer и оборванный вход;
- `Optional<Person>` → `Optional<String>`, `sorted/limit`, generic instanceof, фабрику массива, изменяемость списков и создание PriorityQueue из int[];
- weak interner, условное удаление устаревшей cache entry и повторный explicit close с Cleaner.

Запустите из корня репозитория с JDK 17 или 21:

```bash
mkdir -p work/java-core-classes
javac --release 17 -Xlint:all -d work/java-core-classes examples/java-core/CoreExamples.java
java -cp work/java-core-classes CoreExamples
```

Ожидаемый вывод: `Core examples: all checks passed`. Проверки используют явные условия и работают без `-ea`.
Для очереди reference вызывается `enqueue()`: тестируется обработчик уведомлений, а не предполагаемое время запуска GC.

Разборы заданий: [ООП](../../java/java-core/04-oop-design.html#практические-упражнения),
[I/O](../../java/java-core/06-data-io.html#практические-упражнения),
[типы ссылок](../../java/java-core/10-reference-types.html#практические-примеры).

## ORM-практикум

[OrmExercisesTest.java](orm/src/test/java/example/core/OrmExercisesTest.java) и [pom.xml](orm/pom.xml)
содержат три JUnit-сценария на Hibernate **6.6.15.Final**, Jakarta Persistence **3.1** и H2 **2.3.232**:

1. `merge` создаёт managed-копию detached-объекта; `flush` с последующим rollback не фиксирует изменения.
2. При трёх клиентах с двумя заказами обычная lazy-загрузка выполняет четыре SQL-запроса, fetch join — один; результат одинаков.
3. Стандартная `GenerationType.UUID` назначает UUID и позволяет найти сохранённую сущность.

```bash
mvn -f examples/java-core/orm/pom.xml test
```

Ожидается: `Tests run: 3, Failures: 0, Errors: 0`. Сборка и отчёты находятся в `work/java-core-orm-target/`.
Кэш Maven можно задать обычным `-Dmaven.repo.local=...`, если среда запрещает запись в домашний каталог.

Разбор исходных данных и результатов — в [главе Hibernate/JPA](../../java/04-hibernate.html#практикум-merge-flushrollback-и-n1).

## Границы проверки

Проверено **2026-10-15**: компиляция `--release 17` и исполнение на **OpenJDK 21.0.12.1**, Maven **3.9.11**;
программа Core и все три ORM-теста прошли. Отдельная JVM 17 не запускалась.

Это проверка указанных сценариев. Остальные фрагменты в конспектах не объявляются автоматически скомпилированными.
Для ORM использована in-memory H2, поэтому из этих тестов нельзя делать вывод о блокировках, SQL-диалекте и производительности
PostgreSQL/MySQL. GC-паузы, автоматическая очистка по времени и сборка всей матрицы JDK этими проверками не подтверждаются.
