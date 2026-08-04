# Spring Platform Overview

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Структура раздела](#структура-раздела)
3. [Как использовать материалы](#как-использовать-материалы)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** Java 17–25, Spring Framework 6.2.x, Spring Boot 3.5.x
- **Статус примеров:** `current`
- **Первичные источники:** [Spring Framework Reference](https://docs.spring.io/spring-framework/reference/); [Spring Boot Reference](https://docs.spring.io/spring-boot/index.html)

## Структура раздела
- [Spring Core](spring/01-spring-core.md) — принципы IoC/DI, контекст приложения и жизненный цикл бинов.
- [Spring Boot](spring/02-spring-boot.md) — автоконфигурация, структура приложения и производственный-ready функционал.
- [Spring Data](spring/03-spring-data.md) — репозитории, работа с JPA и расширение под разные источники данных.
- [Spring Integration](spring/04-spring-integration.md) — messaging, адаптеры и построение интеграционных потоков.
- [Проксирование бинов](spring/05-spring-proxying.md) — JDK Dynamic Proxy, CGLIB, механизмы AOP и работа с прокси.
- [Реактивное программирование](spring/06-reactive-programming.md) — Reactive Streams, Project Reactor, RxJava, Spring WebFlux и WebClient.
- [Spring Security](spring/07-spring-security.md) — `SecurityFilterChain`, аутентификация, OAuth 2.0/OIDC, JWT и многоуровневая авторизация.

## Как использовать материалы
1. Начните с раздела по Spring Core, чтобы понять базовые строительные блоки фреймворка.
2. Освойте Spring Boot для быстрого старта и конфигурирования приложений.
3. Изучите Spring Data, чтобы эффективно работать с базами данных и внешними хранилищами.
4. Перейдите к Spring Integration для построения асинхронных и событийных архитектур.
5. Завершите маршрут Spring Security: настройте request- и method-level защиту и закрепите её негативными тестами.

> По мере изучения дополняйте конспекты заметками о практическом опыте и ссылками на документацию. Это поможет поддерживать раздел в актуальном состоянии.
