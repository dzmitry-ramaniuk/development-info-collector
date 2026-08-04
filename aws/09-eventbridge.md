# EventBridge: маршрутизация событий, ретраи и DLQ

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-what-is.html) · [quotas](https://docs.aws.amazon.com/eventbridge/latest/userguide/eb-quota.html) · [pricing](https://aws.amazon.com/eventbridge/pricing/)

## Содержание

1. [Event bus и правила](#event-bus-и-правила)
2. [Сравнение доставки](#сравнение-доставки)
3. [Ретраи и DLQ](#ретраи-и-dlq)
4. [Контракты событий](#контракты-событий)
5. [Практический baseline для production](#практический-baseline-для-production)
6. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Event bus и правила

**EventBridge** сопоставляет события AWS/SaaS/custom applications с event patterns и направляет их в targets. Он удобен для content-based routing, cross-account buses и интеграций; Scheduler используйте вместо legacy scheduled rules для новых расписаний. EventBridge не является ordered log: consumer должен выдерживать дубликаты и перестановку событий.

## Сравнение доставки

На 4 августа 2026 года размеры и retry quotas ниже зависят от региона, target и аккаунта; проверяйте официальные quotas каждого сервиса.

| Свойство | SQS | SNS | EventBridge |
|---|---|---|---|
| Delivery | Standard: at-least-once; FIFO: exactly-once processing в пределах 5-минутного окна дедупликации, но consumer всё равно проектируют идемпотентным | At-least-once для поддерживаемых durable endpoints; поведение зависит от transport | At-least-once до target |
| Ordering | Standard не гарантирует; FIFO строго внутри `MessageGroupId` | Standard не гарантирует; FIFO topic сохраняет порядок внутри message group только с FIFO SQS/Lambda endpoints | Глобальный порядок не гарантируется |
| Retries | Visibility timeout и redrive policy; consumer/Lambda event source управляет повторами | Delivery policy зависит от endpoint; server-side/client-side backoff, затем subscription DLQ | Target retry policy: по умолчанию до 24 часов и 185 попыток, затем DLQ/потеря события |
| DLQ | Redrive в SQS DLQ после `maxReceiveCount` | SQS DLQ привязывается к subscription и хранит недоставленные сообщения | SQS DLQ привязывается к rule target; для event bus есть отдельная DLQ шифрования/permissions |
| Максимальный event/message size | 1 MiB | 256 KB, включая attributes | 256 KB для event entry |

Максимум 256 KB на event entry проверен 4 августа 2026 года. Размер считается после сериализации entry; для больших данных используйте S3 claim-check pattern.

## Ретраи и DLQ

При retryable error EventBridge по умолчанию повторяет delivery до 24 часов и максимум 185 раз с exponential backoff и jitter. Оба числа проверены 4 августа 2026 года; это настраиваемые target policy, а региональные/account-specific quotas следует сверять в документации. После исчерпания policy событие попадает в настроенную **SQS DLQ**, иначе удаляется. Ошибки permissions или отсутствующий target могут отправляться в DLQ без обычных ретраев.

DLQ rule target должна быть Standard SQS queue; дайте EventBridge permission `sqs:SendMessage` с `aws:SourceArn`. Отдельно настройте alarms по `FailedInvocations`, `InvocationsSentToDLQ` и depth очереди.

## Контракты событий

Версионируйте schema, сохраняйте backward compatibility, добавляйте immutable event ID, event time, correlation/causation IDs и источник. Идемпотентность consumer привязывайте к event/business ID. Не считайте timestamp или arrival order уникальными.

## Практический baseline для production

1. Явные retry policy и DLQ на каждом критичном target.
2. Least-privilege bus/resource policies и cross-account allow-list.
3. Schema contract tests, replay plan и archive только при подтверждённой потребности.
4. Наблюдаемость от publish до side effect, включая DLQ redrive.

## Вопросы для самопроверки

1. Почему EventBridge не подходит как ordered event log?
2. Что произойдёт после исчерпания retries без DLQ?
3. Чем target DLQ отличается от архива и replay?

---

[← К разделу AWS](README.md)
