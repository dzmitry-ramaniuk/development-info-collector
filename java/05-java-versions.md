---
---

# Эволюция версий Java: что добавлялось в каждом релизе

## Содержание

1. [Актуальность материала](#актуальность-материала)
   - [Что проверять при каждом обновлении](#что-проверять-при-каждом-обновлении)
2. [Зачем знать историю версий Java](#зачем-знать-историю-версий-java)
3. [Быстрый ориентир по актуальным версиям](#быстрый-ориентир-по-актуальным-версиям)
4. [Хронология релизов Java (1.0 → 26)](#хронология-релизов-java-10--26)
   - [Возможности, менявшие статус или отозванные](#возможности-менявшие-статус-или-отозванные)
5. [Ключевые изменения по эпохам](#ключевые-изменения-по-эпохам)
   - [1) До Java 8: формирование фундамента](#1-до-java-8-формирование-фундамента)
   - [2) Java 8: функциональный сдвиг](#2-java-8-функциональный-сдвиг)
   - [3) Java 9–17: модульность и новая релизная модель](#3-java-917-модульность-и-новая-релизная-модель)
   - [4) Java 18–26: эра Loom/Panama/Amber](#4-java-1826-эра-loompanamaamber)
6. [Практические рекомендации по выбору версии](#практические-рекомендации-по-выбору-версии)
7. [Частые проблемы при апгрейде и как их решать](#частые-проблемы-при-апгрейде-и-как-их-решать)
8. [Вопросы для самопроверки](#вопросы-для-самопроверки)
9. [Источники хронологии](#источники-хронологии)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Java SE 17–26; preview-возможности рассматриваются только там, где явно помечены
- **Статус примеров:** `current`
- **Первичные источники:** [OpenJDK JEP Index](https://openjdk.org/jeps/0), release notes JDK 24–26 и [Oracle Java SE Specifications](https://docs.oracle.com/javase/specs/)
- **Фокус ревизии:** для JEP из JDK 24–26 явно указан статус возможности в соответствующем релизе.

### Что проверять при каждом обновлении

- новые LTS/feature-релизы Java;
- изменения в GC/диагностике/JFR/виртуальных потоках;
- рекомендации по migration path;
- совместимость Spring/Hibernate с новыми JDK.

## Зачем знать историю версий Java

Эволюция Java — это не просто список «новых фич», а карта архитектурных решений платформы: как менялись сборщики мусора, модульность, конкурентность, синтаксис языка и модель релизов.

> Начиная с Java 9, платформа перешла на предсказуемый релизный цикл: новый feature-release каждые 6 месяцев.

## Быстрый ориентир по актуальным версиям

- **Последний feature-release на текущий момент**: **Java 26** (General Availability — **17 марта 2026**).
- **Текущая LTS-линейка**: **8, 11, 17, 21, 25**.
- **Последняя LTS-версия**: **Java 25** (сентябрь 2025).
- **Следующая плановая LTS**: **Java 29** (ожидается в сентябре 2027).

## Хронология релизов Java (1.0 → 26)

Ниже — краткая «шпаргалка» по каждой версии: что было главным изменением и зачем это важно на практике.

| Версия | Год | Ключевые изменения |
|---|---:|---|
| **JDK 1.0** | 1996 | Первый релиз Java: JVM, AWT, базовая библиотека классов, идея *Write Once, Run Anywhere*. |
| **JDK 1.1** | 1997 | Внутренние классы, JavaBeans, JDBC, RMI, новый event model (делегирование событий). |
| **J2SE 1.2** | 1998 | Swing, Collections Framework, JIT в составе HotSpot-линейки, сильное расширение стандартной библиотеки. |
| **J2SE 1.3** | 2000 | HotSpot JVM по умолчанию, JNDI в стандартной поставке, улучшения производительности и стабильности. |
| **J2SE 1.4** | 2002 | `assert`, NIO, регулярные выражения, logging API, XML (JAXP), exception chaining. |
| **Java 5 (1.5)** | 2004 | Большой языковой релиз: generics, annotations, enums, enhanced for, autoboxing, `java.util.concurrent`, varargs, static import. |
| **Java 6** | 2006 | Улучшения JVM/JIT, scripting API (JSR 223), compiler API, web services (JAX-WS), рост производительности. |
| **Java 7** | 2011 | try-with-resources, multi-catch, diamond operator, NIO.2 (`Path`, `Files`, async I/O), Fork/Join, `invokedynamic`. |
| **Java 8 (LTS)** | 2014 | Lambda, Stream API, `Optional`, Date/Time API (`java.time`), default methods, CompletableFuture, Nashorn. |
| **Java 9** | 2017 | **Project Jigsaw** (модули, JPMS), JShell, multi-release JAR, новый 6-месячный цикл релизов, G1 по умолчанию. |
| **Java 10** | 2018 | `var` (локальный вывод типа), Application CDS, улучшения GC/контейнеризации. |
| **Java 11 (LTS)** | 2018 | Новый HTTP Client, single-file source launch, TLS/crypto улучшения, удаление Java EE/CORBA из JDK. |
| **Java 12** | 2019 | Preview switch expressions, Shenandoah (экспериментально), улучшения G1 и startup/footprint. |
| **Java 13** | 2019 | Text Blocks (preview), динамический CDS-архив, продолжение работы над switch expressions. |
| **Java 14** | 2020 | switch expressions (final), records (preview), helpful NPE, pattern matching for `instanceof` (preview). |
| **Java 15** | 2020 | Text Blocks (final), sealed classes (preview), hidden classes, removal Nashorn, ZGC/Shenandoah улучшения. |
| **Java 16** | 2021 | records (final), pattern matching for `instanceof` (final), jpackage, strong encapsulation по умолчанию. |
| **Java 17 (LTS)** | 2021 | sealed classes (final), pattern matching for `switch` (preview), новый PRNG API, Foreign Function/Memory API (incubator). |
| **Java 18** | 2022 | UTF-8 по умолчанию, simple web server (`jwebserver`), Javadoc snippets, продолжение preview/incubator направлений. |
| **Java 19** | 2022 | Virtual Threads (preview), Structured Concurrency (incubator), Record Patterns (preview), Foreign Function & Memory (preview). |
| **Java 20** | 2023 | Продолжение preview: virtual threads, record patterns, pattern matching for switch, scoped values (incubator). |
| **Java 21 (LTS)** | 2023 | **Virtual Threads (final)**, **Pattern Matching for switch (final)**, **Record Patterns (final)**, Sequenced Collections, String Templates (preview), Generational ZGC. |
| **Java 22** | 2024 | Foreign Function & Memory API (final), Structured Concurrency (preview), Scoped Values (2nd preview), unnamed variables/patterns. |
| **Java 23** | 2024 | Эволюция preview/incubator-фич: class-file API (preview), Markdown в Javadoc, импорты модулей (preview), продолжение Project Loom/Panama. |
| **Java 24** | 2025 | JEP 484 Class-File API (**final**), JEP 485 Stream Gatherers (**final**), JEP 491 синхронизация виртуальных потоков без pinning (**final**), JEP 493 runtime image без JMOD (**final**); JEP 487 Scoped Values, JEP 492 Flexible Constructor Bodies, JEP 494 Module Import Declarations и JEP 499 Structured Concurrency (**preview**); JEP 489 Vector API (**incubator**); JEP 486 Security Manager окончательно отключён и JEP 490 non-generational ZGC удалён (**removed**). |
| **Java 25 (LTS)** | 2025 | JEP 506 Scoped Values, JEP 511 Module Import Declarations, JEP 512 Compact Source Files and Instance Main Methods, JEP 513 Flexible Constructor Bodies и JEP 519 Compact Object Headers (**final**); JEP 502 Stable Values, JEP 505 Structured Concurrency и JEP 507 Primitive Types in Patterns (**preview**); JEP 508 Vector API (**incubator**); JEP 503 32-bit x86 port удалён (**removed**). |
| **Java 26** | 2026 | JEP 516 AOT Object Caching with Any GC, JEP 517 HTTP/3 for HTTP Client, JEP 522 уменьшение синхронизации G1 и JEP 524 PEM Encodings (**final**); JEP 525 Structured Concurrency, JEP 526 Lazy Constants и JEP 527 Primitive Types in Patterns (**preview**); JEP 529 Vector API (**incubator**); JEP 504 Applet API удалён (**removed**). |

> Здесь **final** означает стандартную, не preview/incubator-возможность данного JDK, а **removed** — фактическое удаление или окончательное отключение компонента. Это не статус документа JEP в трекере (`Closed/Delivered`). Полный набор менее заметных изменений следует проверять в release notes.

### Возможности, менявшие статус или отозванные

| Возможность | Хронология статуса | Практический вывод |
|---|---|---|
| Structured Concurrency | JDK 19–20: **incubator**; JDK 21–24: последовательные **preview**; JDK 25: пятый **preview** с переработанным API; JDK 26: шестой **preview** | Обобщение «Java 21+» неверно: API всё ещё не **final**, а исходный код для JDK 24 и JDK 25 несовместим без адаптации. |
| Scoped Values | JDK 20: **incubator**; JDK 21–24: **preview**; JDK 25: **final** | Для production без preview-флагов ориентируйтесь на JDK 25 или новее. |
| Vector API | Последовательные **incubator**-итерации; JDK 24 — девятая, JDK 25 — десятая, JDK 26 — одиннадцатая | Incubator-модуль не является стабильным Java SE API; требуются `--add-modules jdk.incubator.vector` и повторная проверка при обновлении JDK. |
| String Templates | JDK 21 (JEP 430) и JDK 22 (JEP 459): **preview**; затем предложение **withdrawn**, в JDK 23+ возможности нет | Нельзя считать String Templates частью современной Java или рассчитывать на совместимость старых preview-примеров; используйте обычную конкатенацию, форматирование либо шаблонизатор. |
| Non-generational ZGC | В JDK 24 удалён (**removed**, JEP 490); generational mode остался | Перед обновлением удалите устаревшие ZGC-флаги и сравните GC-профиль приложения. |

## Ключевые изменения по эпохам

### 1) До Java 8: формирование фундамента
- Появились ключевые API (JDBC, Collections, NIO).
- В Java 5 сформирован «современный» синтаксис и конкурентное программирование.

### 2) Java 8: функциональный сдвиг
- Lambda + Stream API кардинально изменили стиль работы с коллекциями и pipeline-обработкой данных.

### 3) Java 9–17: модульность и новая релизная модель
- JPMS зафиксировал границы модулей и инкапсуляцию.
- Релизы каждые 6 месяцев ускорили доставку возможностей.
- Java 11 и Java 17 стали важнейшими LTS-точками для enterprise.

### 4) Java 18–26: эра Loom/Panama/Amber
- **Loom**: virtual threads и структурированная конкурентность.
- **Panama**: безопасная и быстрая работа с native-кодом через FFM API.
- **Amber**: эволюция языка (records, pattern matching, улучшения switch и т.д.).

## Практические рекомендации по выбору версии

1. Для новых production-проектов ориентируйтесь на **LTS** (обычно 21 или 25, в зависимости от экосистемы и ограничений компании).
2. Если вы на Java 17, Java 21/25 дают более современный язык и новые модели конкурентности. Не обещайте рост throughput заранее: проведите нагрузочное тестирование и отдельное GC-сравнение **конкретного приложения** на одинаковых ресурсах, данных, SLA и профиле запросов.
3. Не-LTS релизы полезны для ранней проверки новых возможностей, но в корпоративной среде чаще используются как промежуточный этап.
4. Перед апгрейдом проверяйте:
   - совместимость фреймворков (Spring, Hibernate, Gradle/Maven plugins),
   - ограничения JVM flags,
   - deprecated/removed API,
   - baseline Docker image и JDK vendor policy.

## Частые проблемы при апгрейде и как их решать

1. **Illegal reflective access / инкапсуляция модулей**
   - Причина: усиление инкапсуляции после Java 9+.
   - Решение: обновить библиотеки; временно использовать `--add-opens` как миграционный мост.

2. **Удалённые API (например, Java EE/CORBA из JDK 11)**
   - Решение: подключить необходимые зависимости отдельно (Jakarta / внешние библиотеки).

3. **Разница поведения GC и пауз**
   - Решение: сравнивать метрики до/после, профилировать workload, отдельно тюнить G1/ZGC под SLA.

4. **Проблемы со сборкой и toolchain**
   - Решение: фиксировать JDK через toolchains (Maven/Gradle), обновлять CI runner и базовые образы.

## Вопросы для самопроверки

1. Почему Java 9 считается переломным релизом для платформы?
2. Какие три изменения Java 8 повлияли на повседневный код больше всего?
3. В чём практическая ценность virtual threads (Java 21) для backend-сервисов?
4. Почему компаниям часто проще переходить между LTS-версиями, чем обновляться на каждый feature-release?
5. Какие проверки нужно сделать перед миграцией с Java 17 на Java 25?

## Источники хронологии

- [OpenJDK JEP Index](https://openjdk.org/jeps/0) — номера, целевые версии и история статусов JEP.
- [OpenJDK JDK 24](https://openjdk.org/projects/jdk/24/), [JDK 25](https://openjdk.org/projects/jdk/25/) и [JDK 26](https://openjdk.org/projects/jdk/26/) — списки JEP каждого feature-release.
- [JDK 24 release notes](https://jdk.java.net/24/release-notes), [JDK 25 release notes](https://jdk.java.net/25/release-notes) и [JDK 26 release notes](https://jdk.java.net/26/release-notes) — изменения API, совместимости и удалённые возможности, не всегда оформленные отдельным JEP.
