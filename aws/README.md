# AWS

Раздел по Amazon Web Services для backend- и platform-инженеров: от базовых облачных концепций и IAM до сетей, вычислений, хранилищ и практик эксплуатации в production.

## Версионный baseline

Материалы раздела пересмотрены **4 августа 2026 года** по актуальной на эту дату AWS documentation. AWS не имеет единой версии платформы: managed services развиваются независимо, а feature availability, цены и quotas различаются по региону и аккаунту. Поэтому каждая тематическая страница содержит дату ревизии и прямые ссылки на официальные service documentation, quotas и pricing; числовые значения перед production-развёртыванием необходимо повторно проверить в Service Quotas и AWS Console.

## 📚 Содержание

1. [Основы AWS для разработчика](01-основы-aws.md)
   - Глобальная инфраструктура: регионы и зоны доступности
   - Модель ответственности Shared Responsibility Model
   - IAM: пользователи, роли, политики и best practices
   - Обзор AWS-сервисов и архитектурных сценариев
   - Практические сценарии для backend-приложений

2. [EC2: виртуальные машины и эксплуатация](02-ec2.md)
3. [VPC: сеть, подсети, маршрутизация и безопасность](03-vpc.md)
4. [Lambda: serverless-функции и паттерны использования](04-lambda.md)
5. [S3: объектное хранилище, безопасность и оптимизация стоимости](05-s3.md)
6. [DynamoDB: моделирование данных и масштабирование](06-dynamodb.md)
7. [SQS: очереди, ретраи, DLQ и идемпотентность](07-sqs.md)
8. [SNS: pub/sub, fan-out и уведомления](08-sns.md)
9. [EventBridge: event bus, правила и интеграции](09-eventbridge.md)
10. [Cognito: аутентификация, user pools и federation](10-cognito.md)

## 🧭 Рекомендуемые маршруты по разделу

| Роль / цель | Начать с | Затем изучить | Основной акцент |
|---|---|---|---|
| Начинающий AWS-разработчик | [Основы AWS](01-основы-aws.md), [VPC](03-vpc.md) | [EC2](02-ec2.md), [S3](05-s3.md) | Shared Responsibility, IAM, регионы/AZ и стоимость |
| Backend-разработчик | [Lambda](04-lambda.md), [S3](05-s3.md) | [DynamoDB](06-dynamodb.md), [SQS](07-sqs.md), [SNS](08-sns.md) | Идемпотентность, retries, concurrency и data modeling |
| Platform / DevOps / SRE | [Основы AWS](01-основы-aws.md), [VPC](03-vpc.md), [EC2](02-ec2.md) | [Lambda](04-lambda.md), [EventBridge](09-eventbridge.md) | Least privilege, quotas, observability, HA и runbooks |
| Архитектор event-driven систем | [SQS](07-sqs.md), [SNS](08-sns.md), [EventBridge](09-eventbridge.md) | [Lambda](04-lambda.md), [DynamoDB](06-dynamodb.md) | Delivery semantics, ordering, DLQ, schema evolution |
| Разработчик identity / B2C | [Основы AWS](01-основы-aws.md), [Cognito](10-cognito.md) | [Lambda](04-lambda.md), [S3](05-s3.md) | Federation, токены, MFA и разграничение доступа |
| Подготовка к интервью | [Основы AWS](01-основы-aws.md) | Все страницы по порядку 2–10 | Компромиссы IaaS/serverless и разбор failure modes |

## 🔗 Связанные темы

- [Kubernetes](../kubernetes/README.md) — запуск контейнерных приложений и orchestration-паттерны
- [System Design](../system design/README.md) — архитектурные компромиссы, масштабирование и отказоустойчивость
- [Базы данных](../базы данных/README.md) — выбор и эксплуатация хранилищ данных
- [Очереди](../очереди/README.md) — асинхронные интеграции и event-driven взаимодействие

---

[← Назад к главной странице](../README.md)
