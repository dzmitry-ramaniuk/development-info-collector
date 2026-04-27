# Spring Framework

[Краткая навигация раздела](../03-spring.md)


Руководство по экосистеме Spring Framework и ключевым проектам: от основ DI/IoC до интеграции с внешними системами.

## 📚 Содержание

1. [Spring Core](01-spring-core.md)
   - Inversion of Control (IoC)
   - Dependency Injection (DI)
   - ApplicationContext
   - Bean lifecycle
   - Конфигурация (Java, XML, аннотации)
   - AOP (Aspect-Oriented Programming)

2. [Spring Boot](02-spring-boot.md)
   - Auto-configuration
   - Starters
   - Embedded servers
   - Properties и конфигурация
   - Actuator
   - Spring Boot DevTools

3. [Spring Data](03-spring-data.md)
   - Spring Data JPA
   - Repositories
   - Query methods
   - Specifications
   - Transactions
   - Auditing

4. [Spring Integration](04-spring-integration.md)
   - Enterprise Integration Patterns
   - Channels и Messages
   - Endpoints
   - Adapters
   - Интеграция с внешними системами

5. [Spring Proxying и AOP](05-spring-proxying.md)
   - Механизмы проксирования
   - JDK Dynamic Proxy vs CGLIB
   - @Transactional и проксирование
   - Aspectj
   - Типичные проблемы с прокси

6. [Реактивное программирование](06-reactive-programming.md)
   - Reactive Streams и спецификация
   - Project Reactor (Mono и Flux)
   - RxJava
   - Spring WebFlux
   - WebClient
   - Backpressure и тестирование

## 🎯 Как использовать

### Для начинающих
Начните с Spring Core для понимания фундаментальных концепций IoC/DI. Затем переходите к Spring Boot для практической разработки.

### Для подготовки к собеседованиям
Сосредоточьтесь на понимании жизненного цикла бинов, механизмов DI, транзакций и AOP. Эти темы часто обсуждаются на собеседованиях.

### Для опытных разработчиков
Изучите продвинутые темы: механизмы проксирования, интеграционные паттерны, оптимизация производительности Spring-приложений.

## 💡 Рекомендации

- Понимайте разницу между различными scope бинов (singleton, prototype, request, session)
- Знайте, когда использовать constructor injection, а когда field/setter injection
- Изучите порядок инициализации и уничтожения бинов
- Понимайте ограничения AOP и проксирования (self-invocation problem)
- Используйте Spring Boot для быстрого старта проектов
- Применяйте Spring Data для упрощения работы с БД

## ⚠️ Важные замечания

> **Constructor injection предпочтительнее**: Использование constructor injection обеспечивает иммутабельность и упрощает тестирование.

> **Транзакционность и прокси**: Аннотация @Transactional работает через прокси. Self-invocation не будет транзакционной!

> **Auto-configuration в Spring Boot**: Понимайте, что происходит "под капотом". Используйте debug режим для анализа auto-configuration.

## 🔗 Связанные темы

Для эффективной работы со Spring рекомендуется также изучить:

- **Java Core**: Рефлексия, аннотации, прокси
- **Базы данных**: JDBC, JPA, транзакции
- **Тестирование**: Spring Test, MockMvc, Testcontainers
- **Многопоточность**: Асинхронность в Spring (@Async, @Scheduled)

## 🔜 Планируется добавить

- **Spring Security**: Аутентификация и авторизация
- **Spring Cloud**: Микросервисы и распределённые системы
- **Spring Batch**: Batch processing

---

[← Назад к разделу Java](../README.md)