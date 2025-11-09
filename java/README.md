# Java Learning Roadmap

Java — зрелая экосистема со множеством направлений развития. Чтобы не утонуть в количестве технологий, материалы организованы
как набор взаимосвязанных глав. Каждая глава содержит подробное изложение теории, практические советы и блок вопросов для
самопроверки.

## Как устроен раздел
- [Java Core](01-java-core.md) — фундаментальные темы платформы. [→ Обзор раздела](java-core/README.md)
  - [Виртуальная машина и платформа](java-core/01-jvm-runtime.md)
  - [Управление памятью и сборка мусора](java-core/02-memory-management.md)
  - [Основы языка и синтаксис](java-core/03-language-basics.md)
  - [Объектно-ориентированное программирование](java-core/04-oop-design.md)
  - [Коллекции и обобщения](java-core/05-collections-generics.md)
  - [Работа с данными и ввод/вывод](java-core/06-data-io.md)
  - [Функциональные возможности и современные фичи](java-core/07-functional-modern-java.md)
  - [Алгоритмическая сложность и анализ производительности](java-core/08-algorithms-complexity.md)
- [Multithreading](02-multithreading.md) — модель памяти Java, синхронизация и конкуррентные библиотеки. [→ Обзор раздела](multithreading/README.md)
  - [Java Memory Model и гарантии видимости](multithreading/01-jmm-visibility.md)
  - [Управление потоками и пулами](multithreading/02-thread-pools.md)
  - [Асинхронные вычисления и координация](multithreading/03-async-coordination.md)
  - [Синхронизаторы и конкурентные структуры данных](multithreading/04-synchronizers.md)
  - [Потоковое локальное состояние и неизменяемость](multithreading/05-threadlocal-immutability.md)
  - [Диагностика и устранение проблем](multithreading/06-diagnostics-problems.md)
  - [Шаблоны и практические приёмы](multithreading/07-patterns.md)
  - [Практические упражнения](multithreading/08-exercises.md)
  - [Вопросы на собеседовании](multithreading/09-interview-questions.md)
- [Spring](03-spring.md) — руководство по экосистеме фреймворка и ключевым проектам. [→ Обзор раздела](spring/README.md)
  - [Spring Core](spring/01-spring-core.md)
  - [Spring Boot](spring/02-spring-boot.md)
  - [Spring Data](spring/03-spring-data.md)
  - [Spring Integration](spring/04-spring-integration.md)

## Рекомендации по изучению
1. Пройдите главы в указанном порядке: они выстроены от внутренних механизмов платформы к инструментам высокого уровня.
2. Выполняйте небольшие практические задания, описанные в главах, чтобы закрепить материал.
3. Используйте блоки «Вопросы на собеседовании» для повторения перед интервью и для самооценки.
4. Возвращайтесь к конспектам, чтобы дополнять примерами из собственных проектов и ссылками на документацию.

> При обновлении языка конспекты расширяются новыми возможностями. Следите за пометками «Историческая справка» и «Современная
> практика», чтобы видеть эволюцию платформы.
