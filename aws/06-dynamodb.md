---
---

# DynamoDB: моделирование данных и масштабирование

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Когда выбирать DynamoDB](#когда-выбирать-dynamodb)
3. [Моделирование данных](#моделирование-данных)
4. [Индексы и доступ](#индексы-и-доступ)
5. [Производительность и стоимость](#производительность-и-стоимость)
6. [Практический baseline для production](#практический-baseline-для-production)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Introduction.html) · [quotas](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/ServiceQuotas.html) · [pricing](https://aws.amazon.com/dynamodb/pricing/)
## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** публичные API и документация AWS, проверенные 2026-08-04; версии управляемых сервисов уточняются в тексте
- **Статус примеров:** `current`
- **Первичные источники:** [AWS Documentation](https://docs.aws.amazon.com/); [AWS Architecture Center](https://aws.amazon.com/architecture/)

## Когда выбирать DynamoDB

**DynamoDB** хороша для высоконагруженных сервисов с простыми и чётко определёнными access patterns.

## Моделирование данных

- Проектируйте схему **от запросов**.
- Выбирайте partition key так, чтобы равномерно распределять нагрузку.
- Используйте sort key для диапазонов и иерархий.

## Индексы и доступ

- **GSI**: альтернативные ключи доступа.
- **LSI**: альтернативная сортировка в пределах partition key.
- **TTL**: автоудаление временных записей.
- **Streams**: реакция на изменения данных.

## Производительность и стоимость

- On-demand для непредсказуемой нагрузки.
- Provisioned + auto scaling для стабильной нагрузки.
- Следите за hot partitions и throttling.

## Практический baseline для production

1. Чётко описанные access patterns.
2. Метрики throttles/latency/consumed capacity.
3. Ретраи с backoff на уровне клиента.
4. TTL для временных сущностей.
5. PITR (Point-in-time recovery) для восстановления.

## Вопросы для самопроверки

1. Почему в DynamoDB нельзя начинать с ER-модели как в SQL?
2. Когда нужен GSI?
3. Что вызывает hot partition и как избежать?

---

[← К разделу AWS](README.html)
