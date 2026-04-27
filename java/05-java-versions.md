# Эволюция версий Java: что добавлялось в каждом релизе

## Содержание

1. [Зачем знать историю версий Java](#зачем-знать-историю-версий-java)
2. [Быстрый ориентир по актуальным версиям](#быстрый-ориентир-по-актуальным-версиям)
3. [Хронология релизов Java (1.0 → 26)](#хронология-релизов-java-10--26)
4. [Ключевые изменения по эпохам](#ключевые-изменения-по-эпохам)
5. [Практические рекомендации по выбору версии](#практические-рекомендации-по-выбору-версии)
6. [Частые проблемы при апгрейде и как их решать](#частые-проблемы-при-апгрейде-и-как-их-решать)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)
8. [Актуальность материала](#актуальность-материала)

## Актуальность материала

- **Последняя проверка:** **27 апреля 2026**.
- **Фокус ревизии:** актуализированы ориентиры по LTS/feature-релизам и связанные риски апгрейда.

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
| **Java 24** | 2025 | Продолжение стабилизации новых языковых и runtime-возможностей, улучшения производительности JVM и GC. |
| **Java 25 (LTS)** | 2025 | Новая LTS-база для production: накопленные улучшения языка/рантайма, стабильный целевой апгрейд с 17/21. |
| **Java 26** | 2026 | Удаление Applet API, HTTP/3 для HTTP Client API, новые preview/инкубационные улучшения (включая Structured Concurrency, Vector API и pattern matching-эволюцию). |

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
2. Если вы на Java 17, переход на Java 21/25 обычно даёт лучший throughput, удобнее конкурентное программирование и более современный язык.
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
