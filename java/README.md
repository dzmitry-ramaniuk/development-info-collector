# Java

Материалы по языку Java, экосистеме и сопутствующим инструментам. Каждая тема содержит подробное изложение теории, практические советы и блок вопросов для самопроверки.

## 📚 Содержание
### [Java Core](java-core/README.md)

Фундаментальные темы платформы Java:
  - [Виртуальная машина и платформа](java-core/01-jvm-runtime.md)
  - [Управление памятью и сборка мусора](java-core/02-memory-management.md)
  - [Основы языка и синтаксис](java-core/03-language-basics.md)
  - [Объектно-ориентированное программирование](java-core/04-oop-design.md)
  - [Коллекции](java-core/05-collections.md)
  - [Работа с данными и ввод/вывод](java-core/06-data-io.md)
  - [Функциональные возможности и современные фичи](java-core/07-functional-modern-java.md)
  - [Алгоритмическая сложность и анализ производительности](java-core/08-algorithms-complexity.md)
  - [Stream API](java-core/09-stream-api.md)
  - [Типы ссылок](java-core/10-reference-types.md)
  - [Обобщения (Generics)](java-core/11-generics.md)

→ [Перейти к материалам по Java Core](java-core/README.md)

### [Multithreading](multithreading/README.md)

Многопоточность в Java: модель памяти, синхронизация и конкуррентные библиотеки:
  - [Java Memory Model и гарантии видимости](multithreading/01-jmm-visibility.md)
  - [Управление потоками и пулами](multithreading/02-thread-pools.md)
  - [Асинхронные вычисления и координация](multithreading/03-async-coordination.md)
  - [Синхронизаторы и конкурентные структуры данных](multithreading/04-synchronizers.md)
  - [Потоковое локальное состояние и неизменяемость](multithreading/05-threadlocal-immutability.md)
  - [Диагностика и устранение проблем](multithreading/06-diagnostics-problems.md)
  - [Шаблоны и практические приёмы](multithreading/07-patterns.md)
  - [Практические упражнения](multithreading/08-exercises.md)
  - [Вопросы на собеседовании](multithreading/09-interview-questions.md)

→ [Перейти к материалам по Multithreading](multithreading/README.md)

### [Spring Framework](spring/README.md)

Экосистема Spring Framework и ключевые проекты:
  - [Spring Core](spring/01-spring-core.md)
  - [Spring Boot](spring/02-spring-boot.md)
  - [Spring Data](spring/03-spring-data.md)
  - [Spring Integration](spring/04-spring-integration.md)
  - [Spring Proxying и AOP](spring/05-spring-proxying.md)
  - [Реактивное программирование](spring/06-reactive-programming.md)

→ [Перейти к материалам по Spring Framework](spring/README.md)

### [Hibernate и JPA](04-hibernate.md)

ORM-фреймворк для работы с реляционными базами данных:
  - JPA спецификация и реализация Hibernate
  - Маппинг сущностей и связи между ними
  - Контекст персистентности и жизненный цикл
  - Кеширование первого и второго уровней
  - HQL, JPQL, Criteria API
  - Транзакции и блокировки
  - Оптимизация производительности

→ [Перейти к материалам по Hibernate](04-hibernate.md)

## 🧭 Рекомендуемые маршруты по разделу

- **Базовый маршрут по платформе**: [Java Core](java-core/README.md) → [Multithreading](multithreading/README.md) → [Spring Framework](spring/README.md)
- **Маршрут для backend-разработки**: [Java Core](java-core/README.md) → [Hibernate и JPA](04-hibernate.md) → [Spring Framework](spring/README.md) → [Тестирование](../тестирование/README.md)
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
