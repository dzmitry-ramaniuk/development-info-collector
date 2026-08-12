---
---

# AWS

## Содержание

1. [Версионный baseline](#версионный-baseline)
2. [📚 Содержание](#-содержание)
3. [🧭 Рекомендуемые маршруты по разделу](#-рекомендуемые-маршруты-по-разделу)
4. [🔗 Связанные темы](#-связанные-темы)


Раздел по Amazon Web Services для backend- и platform-инженеров: от базовых облачных концепций и IAM до сетей, вычислений, хранилищ и практик эксплуатации в production.

## Версионный baseline

Материалы раздела пересмотрены **4 августа 2026 года** по актуальной на эту дату AWS documentation. AWS не имеет единой версии платформы: managed services развиваются независимо, а feature availability, цены и quotas различаются по региону и аккаунту. Поэтому каждая тематическая страница содержит дату ревизии и прямые ссылки на официальные service documentation, quotas и pricing; числовые значения перед production-развёртыванием необходимо повторно проверить в Service Quotas и AWS Console.

## 📚 Содержание

1. [Основы AWS для разработчика](01-основы-aws.html)
   - Глобальная инфраструктура: регионы и зоны доступности
   - Модель ответственности Shared Responsibility Model
   - IAM: пользователи, роли, политики и best practices
   - Обзор AWS-сервисов и архитектурных сценариев
   - Практические сценарии для backend-приложений

2. [EC2: виртуальные машины и эксплуатация](02-ec2.html)
3. [VPC: сеть, подсети, маршрутизация и безопасность](03-vpc.html)
4. [Lambda: serverless-функции и паттерны использования](04-lambda.html)
5. [S3: объектное хранилище, безопасность и оптимизация стоимости](05-s3.html)
6. [DynamoDB: моделирование данных и масштабирование](06-dynamodb.html)
7. [SQS: очереди, ретраи, DLQ и идемпотентность](07-sqs.html)
8. [SNS: pub/sub, fan-out и уведомления](08-sns.html)
9. [EventBridge: event bus, правила и интеграции](09-eventbridge.html)
10. [Cognito: аутентификация, user pools и federation](10-cognito.html)

## 🧭 Рекомендуемые маршруты по разделу

| Роль / цель | Начать с | Затем изучить | Основной акцент |
|---|---|---|---|
| Начинающий AWS-разработчик | [Основы AWS](01-основы-aws.html), [VPC](03-vpc.html) | [EC2](02-ec2.html), [S3](05-s3.html) | Shared Responsibility, IAM, регионы/AZ и стоимость |
| Backend-разработчик | [Lambda](04-lambda.html), [S3](05-s3.html) | [DynamoDB](06-dynamodb.html), [SQS](07-sqs.html), [SNS](08-sns.html) | Идемпотентность, retries, concurrency и data modeling |
| Platform / DevOps / SRE | [Основы AWS](01-основы-aws.html), [VPC](03-vpc.html), [EC2](02-ec2.html) | [Lambda](04-lambda.html), [EventBridge](09-eventbridge.html) | Least privilege, quotas, observability, HA и runbooks |
| Архитектор event-driven систем | [SQS](07-sqs.html), [SNS](08-sns.html), [EventBridge](09-eventbridge.html) | [Lambda](04-lambda.html), [DynamoDB](06-dynamodb.html) | Delivery semantics, ordering, DLQ, schema evolution |
| Разработчик identity / B2C | [Основы AWS](01-основы-aws.html), [Cognito](10-cognito.html) | [Lambda](04-lambda.html), [S3](05-s3.html) | Federation, токены, MFA и разграничение доступа |
| Подготовка к интервью | [Основы AWS](01-основы-aws.html) | Все страницы по порядку 2–10 | Компромиссы IaaS/serverless и разбор failure modes |

## 🔗 Связанные темы

- [Kubernetes](../kubernetes/README.html) — запуск контейнерных приложений и orchestration-паттерны
- [Scheduling и autoscaling Kubernetes](../kubernetes/07-scheduling-и-autoscaling.html) — связь requests, HPA и node autoscaling с облачной capacity
- [Безопасность и политики Kubernetes](../kubernetes/06-безопасность-и-политики.html) — сопоставление ServiceAccount/RBAC, сетевой изоляции и KMS с облачными controls
- [Доставка приложений в Kubernetes](../kubernetes/08-доставка-приложений.html) — CI/CD, GitOps и progressive delivery поверх облачной инфраструктуры
- [System Design](../system%20design/README.html) — архитектурные компромиссы, масштабирование и отказоустойчивость
- [Базы данных](../базы%20данных/README.html) — выбор и эксплуатация хранилищ данных
- [Очереди](../очереди/README.html) — асинхронные интеграции и event-driven взаимодействие
- [CI/CD](../ci-cd/README.html) — promotion immutable artifacts и безопасные deployment strategies в облаке

---

[← Назад к главной странице](../README.html)
