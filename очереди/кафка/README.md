# Apache Kafka

Комплексный учебный курс по Apache Kafka — распределённой платформе потоковой обработки данных.

## 📚 Содержание

### Основы

Фундаментальные концепции и архитектура Apache Kafka:

1. [Принципы и архитектура](01-принципы-и-архитектура.md)
2. [Типовые проблемы и риски](02-проблемы-и-риски.md)
3. [Ключевые механизмы Kafka](03-ключевые-механизмы.md)
4. [Приёмы эффективной работы](04-приемы-работы.md)
5. [Вопросы и ответы](05-вопросы-и-ответы.md)

### Продвинутые темы

Глубокое погружение в экосистему Kafka:

6. [Kafka Streams](06-kafka-streams.md) — потоковая обработка данных
   - KStream и KTable
   - Stateless и stateful операции
   - Windowing и joins
   - State stores и interactive queries
   - Тестирование и мониторинг

7. [Kafka Connect](07-kafka-connect.md) — интеграция с внешними системами
   - Source и Sink коннекторы
   - JDBC, Debezium CDC, Elasticsearch, S3
   - Single Message Transforms (SMT)
   - Distributed mode и REST API

8. [Schema Registry](08-schema-registry.md) — управление схемами данных
   - Avro, Protobuf, JSON Schema
   - Режимы совместимости
   - Эволюция схем
   - REST API и интеграция

### Безопасность и мониторинг

Защита данных и наблюдаемость системы:

9. [Безопасность и мониторинг](09-безопасность-мониторинг.md)
   - Authentication: SSL/TLS, SASL (PLAIN, SCRAM, Kerberos, OAuth)
   - Authorization: ACL, роли и разрешения
   - Encryption: in-transit и at-rest
   - JMX метрики, Prometheus, Grafana
   - Logging и alerting

### Эксплуатация

Production deployment и операционные практики:

10. [Эксплуатация и Production Best Practices](10-эксплуатация-production.md)
    - Sizing и capacity planning
    - Production конфигурация (broker, OS, JVM)
    - Высокая доступность (HA)
    - Disaster recovery (MirrorMaker, backup)
    - Обслуживание: upgrade, добавление/удаление брокеров
    - Производительность и тюнинг
    - Troubleshooting

## 🎯 Как использовать

### Для начинающих
Начните с разделов 1-5, чтобы получить твердое понимание основ Kafka, архитектуры и типовых паттернов использования.

### Для разработчиков
Изучите разделы 6-8 для глубокого понимания Kafka Streams, Connect и Schema Registry — ключевых инструментов для построения data pipelines.

### Для DevOps/SRE
Сфокусируйтесь на разделах 9-10 для понимания аспектов безопасности, мониторинга и эксплуатации Kafka в production.

### Для подготовки к собеседованиям
Каждая глава содержит раздел "Вопросы для самопроверки" с типичными вопросами и ответами.

## 💡 Рекомендации

- Начните с понимания основ распределённых систем
- Изучите архитектуру Kafka и концепцию партиционирования
- Практикуйтесь с локальным кластером Kafka
- Понимайте гарантии доставки (at-most-once, at-least-once, exactly-once)
- Изучите Kafka Streams для потоковой обработки
- Обратите внимание на мониторинг и эксплуатацию в production

## 🔗 Связанные темы

- [System Design](../../system design/README.md) — общий архитектурный контекст для event-driven систем
- [Асинхронность и событийные системы](../../system design/05-асинхронность-и-событийные-системы.md) — когда и почему broker лучше прямого sync-взаимодействия
- [Масштабирование, надёжность и отказоустойчивость](../../system design/06-масштабирование-надежность-и-отказоустойчивость.md) — как учитывать Kafka в общей fault-tolerant архитектуре

## 📖 Дополнительные ресурсы

- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Confluent Platform Documentation](https://docs.confluent.io/)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
- [Designing Event-Driven Systems](https://www.confluent.io/designing-event-driven-systems/)

---

[← Назад к разделу Очереди](../README.md)
