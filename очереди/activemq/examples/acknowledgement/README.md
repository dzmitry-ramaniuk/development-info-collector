---
---

# JMS acknowledgement: rollback, commit и DLQ

## Запуск

Нужны JDK 17+ и Maven 3.9+. Из этого каталога:

```bash
mvn test
```

В [pom.xml](pom.xml) закреплены Classic 6.1.5 и JUnit 5.11.4. Это учебный baseline, не указание выбирать старый patch для production. Тест поднимает broker с уникальным именем в текущей JVM; внешние broker, Docker и порты не нужны. После теста broker останавливается.

## Проверяемое поведение

Первый тест получает сообщение в transacted Session, делает rollback, проверяет `JMSRedelivered`/`JMSXDeliveryCount`, затем commit. Второй исчерпывает ограничение redelivery и проверяет сообщение в `ActiveMQ.DLQ`.

15 октября 2026 года оба теста прошли на JDK 21 с `--release 17`. Broker работает без persistence: тесты не доказывают сохранность при crash/disk failure и не охватывают Artemis, XA или атомарность с внешней БД.

Подробнее — [основы ActiveMQ](../../01-основы-и-архитектура.html).
