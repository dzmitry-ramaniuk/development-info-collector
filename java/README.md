---
---

# Java

## Содержание

1. [📚 Содержание](#-содержание)
   - [[Java Core](01-java-core.html)](#java-core01-java-coremd)
   - [[Multithreading](02-multithreading.html)](#multithreading02-multithreadingmd)
   - [[Spring Framework](03-spring.html)](#spring-framework03-springmd)
   - [[Hibernate и JPA](04-hibernate.html)](#hibernate-и-jpa04-hibernatemd)
   - [[Эволюция версий Java](05-java-versions.html)](#эволюция-версий-java05-java-versionsmd)
   - [[Тюнинг и мониторинг JVM](06-jvm-tuning-monitoring.html)](#тюнинг-и-мониторинг-jvm06-jvm-tuning-monitoringmd)
2. [Статус актуальности](#статус-актуальности)
3. [🧭 Рекомендуемые маршруты по разделу](#-рекомендуемые-маршруты-по-разделу)
4. [🎯 Как использовать](#-как-использовать)
   - [Для начинающих](#для-начинающих)
   - [Для подготовки к собеседованиям](#для-подготовки-к-собеседованиям)
   - [Для опытных разработчиков](#для-опытных-разработчиков)
5. [💡 Рекомендации по изучению](#-рекомендации-по-изучению)


Материалы по языку Java, экосистеме и сопутствующим инструментам. Каждая тема содержит подробное изложение теории, практические советы и блок вопросов для самопроверки.

## 📚 Содержание
### [Java Core](01-java-core.html)

Фундаментальные темы платформы Java:
  - [Виртуальная машина и платформа](java-core/01-jvm-runtime.html)
  - [Управление памятью и сборка мусора](java-core/02-memory-management.html)
  - [Основы языка и синтаксис](java-core/03-language-basics.html)
  - [Объектно-ориентированное программирование](java-core/04-oop-design.html)
  - [Коллекции](java-core/05-collections.html)
  - [Работа с данными и ввод/вывод](java-core/06-data-io.html)
  - [Функциональные возможности и современные фичи](java-core/07-functional-modern-java.html)
  - [Алгоритмическая сложность и анализ производительности](java-core/08-algorithms-complexity.html)
  - [Stream API](java-core/09-stream-api.html)
  - [Типы ссылок](java-core/10-reference-types.html)
  - [Обобщения (Generics)](java-core/11-generics.html)

→ [Короткая навигация](01-java-core.html) · [Подробное оглавление](java-core/README.html)

### [Multithreading](02-multithreading.html)

Многопоточность в Java: модель памяти, синхронизация и конкуррентные библиотеки:
  - [Java Memory Model и гарантии видимости](multithreading/01-jmm-visibility.html)
  - [Управление потоками и пулами](multithreading/02-thread-pools.html)
  - [Асинхронные вычисления и координация](multithreading/03-async-coordination.html)
  - [Синхронизаторы и конкурентные структуры данных](multithreading/04-synchronizers.html)
  - [Потоковое локальное состояние и неизменяемость](multithreading/05-threadlocal-immutability.html)
  - [Диагностика и устранение проблем](multithreading/06-diagnostics-problems.html)
  - [Шаблоны и практические приёмы](multithreading/07-patterns.html)
  - [Практические упражнения](multithreading/08-exercises.html)
  - [Вопросы на собеседовании](multithreading/09-interview-questions.html)
  - [Synchronized: теория и практика](multithreading/10-synchronized.html)

→ [Короткая навигация](02-multithreading.html) · [Подробное оглавление](multithreading/README.html)

> `02-multithreading.md` — обзор и рекомендуемый маршрут по теме, а каталог
> `multithreading/` содержит отдельные подробные главы. Это один раздел, а не два
> независимых набора материалов.

### [Spring Framework](03-spring.html)

Экосистема Spring Framework и ключевые проекты:
  - [Spring Core](spring/01-spring-core.html)
  - [Spring Boot](spring/02-spring-boot.html)
  - [Spring Data](spring/03-spring-data.html)
  - [Spring Integration](spring/04-spring-integration.html)
  - [Spring Proxying и AOP](spring/05-spring-proxying.html)
  - [Реактивное программирование](spring/06-reactive-programming.html)
  - [Spring Security](spring/07-spring-security.html)

→ [Короткая навигация](03-spring.html) · [Подробное оглавление](spring/README.html)

### [Hibernate и JPA](04-hibernate.html)

ORM-фреймворк для работы с реляционными базами данных:
  - JPA спецификация и реализация Hibernate
  - Маппинг сущностей и связи между ними
  - Контекст персистентности и жизненный цикл
  - Кеширование первого и второго уровней
  - HQL, JPQL, Criteria API
  - Транзакции и блокировки
  - Оптимизация производительности

→ [Перейти к материалам по Hibernate](04-hibernate.html)

### [Эволюция версий Java](05-java-versions.html)

Краткая история релизов Java от JDK 1.0 до JDK 25 (проверено 4 августа 2026 года):
  - Что появилось в каждой версии и почему это важно
  - Какие версии являются LTS и как выбирать production-базу
  - Как менялись язык, JVM, GC и конкурентность
  - Практические рекомендации по миграции между версиями

→ [Перейти к материалу по версиям Java](05-java-versions.html)

### [Тюнинг и мониторинг JVM](06-jvm-tuning-monitoring.html)

Практический раздел по эксплуатации JVM в production:
  - ключевые метрики памяти, GC, CPU и потоков;
  - безопасный подход к тюнингу через baseline и итерации;
  - базовый набор JVM-флагов для server-side приложений;
  - инструменты диагностики: GC logs, jcmd/jstat, JFR, async-profiler;
  - типовые инциденты (OOM, latency spikes, GC pauses) и план действий.

→ [Перейти к материалу по тюнингу и мониторингу JVM](06-jvm-tuning-monitoring.html)

## Статус актуальности

- [Эволюция версий Java](05-java-versions.html) — последняя ревизия: **27 апреля 2026**.
- [Тюнинг и мониторинг JVM](06-jvm-tuning-monitoring.html) — последняя ревизия: **27 апреля 2026**.
- [Spring Framework](03-spring.html) — последняя ревизия: **27 апреля 2026**.
- [Hibernate и JPA](04-hibernate.html) — последняя ревизия: **27 апреля 2026**.

## 🧭 Рекомендуемые маршруты по разделу

- **Базовый маршрут по платформе**: [Java Core](01-java-core.html) → [Multithreading](02-multithreading.html) → [Spring Framework](03-spring.html)
- **Маршрут для backend-разработки**: [Java Core](01-java-core.html) → [Hibernate и JPA](04-hibernate.html) → [Spring Framework](03-spring.html) → [Тестирование](../тестирование/README.html)
- **Маршрут для повторения перед собеседованием**: память и JVM → коллекции и Stream API → JMM и синхронизация → Spring Core/Boot → транзакции и ORM

## 🎯 Как использовать

### Для начинающих
Изучайте материалы последовательно, начиная с Java Core. Каждая тема содержит практические примеры и упражнения.

### Для подготовки к собеседованиям
Используйте разделы "Вопросы для самопроверки" в каждом файле. Особое внимание уделите темам памяти, многопоточности и Spring.

### Для опытных разработчиков
Материалы помогут систематизировать знания и заполнить пробелы в понимании внутренних механизмов платформы.

## 💡 Рекомендации по изучению
1. Пройдите главы в указанном порядке: они выстроены от внутренних механизмов платформы к инструментам высокого уровня.
2. Выполняйте небольшие практические задания, описанные в главах, чтобы закрепить материал.
3. Используйте блоки «Вопросы на собеседовании» для повторения перед интервью и для самооценки.
4. Возвращайтесь к конспектам, чтобы дополнять примерами из собственных проектов и ссылками на документацию.

> При обновлении языка конспекты расширяются новыми возможностями. Следите за пометками «Историческая справка» и «Современная
> практика», чтобы видеть эволюцию платформы.
