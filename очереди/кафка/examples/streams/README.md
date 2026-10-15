---
---

# Kafka Streams и Avro: воспроизводимая практика

## Запуск

Нужны JDK 17+ и Maven 3.9+. Из этого каталога:

```bash
mvn test
```

Зависимости закреплены в [pom.xml](pom.xml): Kafka Streams/test-utils 4.0.2, Avro 1.12.0 и JUnit 5.11.4. При первом запуске Maven скачивает artifacts; broker и Schema Registry не нужны.

## Проверяемое поведение

- WindowedCounts использует явные Serdes, size/grace/retention, changelog и современный exception handler/RocksDB setter API.
- TopologyTestDriver проверяет суммирование и отбрасывание event после закрытия окна по stream-time.
- Avro-тесты различают BACKWARD `new reader ← old writer` и FORWARD `old reader ← new writer`, проверяют несовместимые изменения и читают реальный старый binary payload с новым reader/default.

15 октября 2026 года `mvn test` на JDK 21 с `--release 17` завершился успешно: 3 теста. Тесты не проверяют Kafka transactions, перенос task между процессами, восстановление с удалённого changelog, REST/auth/persistence Schema Registry. Для этого нужен отдельный интеграционный стенд.

## Продолжение

Вернитесь к [Kafka Streams](../../06-kafka-streams.html) и [Schema Registry](../../08-schema-registry.html). Для failover-упражнения запустите два instances с одним application ID, создайте state, остановите owner task, проверьте восстановленный aggregate и output на другом instance; сохраните фактические результаты отдельно от unit-тестов.
