---
---

# Проверка scopes и roles в Spring Security

## Содержание

1. [Запуск](#запуск)
2. [Что проверяется](#что-проверяется)

## Запуск

Требуются JDK 17+ и Maven 3.6.3+. Пример закреплён на Spring Boot 3.5.0 и Java 17; версия выбрана для воспроизводимости учебного сценария, а не как рекомендация для production.

```bash
mvn -f examples/spring-security/pom.xml test
```

Проверено 2026-10-15: Maven 3.9.11, JDK 21 с компиляцией `--release 17`, 10 тестов прошли. Непосредственный запуск на JDK 17 в этой проверке не выполнялся.

Mockito подключён к тестовой JVM через явный `-javaagent`, как описано в [официальной инструкции для Java 21+](https://javadoc.io/static/org.mockito/mockito-core/5.17.0/org.mockito/org/mockito/Mockito.html#0.3). Это устраняет зависимость тестов от разрешения JVM на dynamic self-attach.

## Что проверяется

Тесты отправляют настоящий `Authorization: Bearer`, проверяют подпись RS256, issuer, audience и время жизни через `NimbusJwtDecoder`, а затем запускают production converter: scopes дают `SCOPE_…`, роли дают `ROLE_…`. Ключи генерируются в памяти только для тестов; внешнего issuer и сетевых запросов во время теста нет. Maven при первом запуске скачивает зависимости.

Проверки различают 401 при отсутствующей или неверной identity и 403 при недостаточных permissions. Отдельный тест контролирует одновременное сохранение scopes и roles, чтобы готовая identity из `jwt().authorities(...)` не скрывала ошибку mapping.

Это ограниченный пример чтения и проверки роли. Object-level authorization и CSRF для cookie-приложения рассматриваются в [основной странице](../../java/spring/07-spring-security.html).
