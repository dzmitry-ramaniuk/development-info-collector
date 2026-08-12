---
---

# SQS: очереди, ретраи, DLQ и идемпотентность

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Standard и FIFO](#standard-и-fifo)
3. [Сравнение доставки](#сравнение-доставки)
4. [Ретраи и DLQ](#ретраи-и-dlq)
5. [Практический baseline для production](#практический-baseline-для-production)
6. [Вопросы для самопроверки](#вопросы-для-самопроверки)

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/welcome.html) · [quotas](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/quotas-messages.html) · [pricing](https://aws.amazon.com/sqs/pricing/)
## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** публичные API и документация AWS, проверенные 2026-08-04; версии управляемых сервисов уточняются в тексте
- **Статус примеров:** `current`
- **Первичные источники:** [AWS Documentation](https://docs.aws.amazon.com/); [AWS Architecture Center](https://aws.amazon.com/architecture/)

## Standard и FIFO

**SQS Standard** даёт практически неограниченный throughput, at-least-once delivery и best-effort ordering. **SQS FIFO** упорядочивает сообщения внутри `MessageGroupId` и дедуплицирует send requests; разные groups обрабатываются параллельно. Даже с FIFO side effects делайте идемпотентными: сбой после эффекта, но до delete/ack, вызывает повторную доставку.

## Сравнение доставки

На 4 августа 2026 года размеры и retry quotas ниже зависят от региона, endpoint и аккаунта; перед проектированием проверяйте официальные quotas каждого сервиса.

| Свойство | SQS | SNS | EventBridge |
|---|---|---|---|
| Delivery | Standard: at-least-once; FIFO: exactly-once processing в пределах 5-минутного окна дедупликации, но consumer всё равно проектируют идемпотентным | At-least-once для поддерживаемых durable endpoints; поведение зависит от transport | At-least-once до target |
| Ordering | Standard не гарантирует; FIFO строго внутри `MessageGroupId` | Standard не гарантирует; FIFO topic сохраняет порядок внутри message group только с FIFO SQS/Lambda endpoints | Глобальный порядок не гарантируется |
| Retries | Visibility timeout и redrive policy; consumer/Lambda event source управляет повторами | Delivery policy зависит от endpoint; server-side/client-side backoff, затем subscription DLQ | Target retry policy: по умолчанию до 24 часов и 185 попыток, затем DLQ/потеря события |
| DLQ | Redrive в SQS DLQ после `maxReceiveCount` | SQS DLQ привязывается к subscription и хранит недоставленные сообщения | SQS DLQ привязывается к rule target; для event bus есть отдельная DLQ шифрования/permissions |
| Максимальный event/message size | 1 MiB | 256 KB, включая attributes | 256 KB для event entry |

Для payload больше 1 MiB (SQS) либо 256 KB (SNS/EventBridge) храните данные в S3 и отправляйте ссылку; проверьте security и lifecycle объекта. Числовые пределы проверены 4 августа 2026 года и могут быть изменены AWS.

## Ретраи и DLQ

`VisibilityTimeout` должен превышать ожидаемое время обработки; heartbeat может продлевать его. После ошибки сообщение снова становится видимым, а redrive policy переносит его в DLQ после `maxReceiveCount`. Retention DLQ должна позволять расследование; source и DLQ должны быть одного типа (Standard или FIFO). Redrive запускайте контролируемо, устранив причину poison message.

Для Lambda используйте partial batch response: возвращайте только failed message IDs. Идемпотентность стройте по бизнес-ключу, а не по receive count.

## Практический baseline для production

1. DLQ, alarm по `ApproximateAgeOfOldestMessage` и runbook redrive.
2. Visibility timeout, batch size/window и concurrency согласованы с downstream capacity.
3. Идемпотентный consumer, exponential backoff с jitter и bounded retries.
4. FIFO только при реальном требовании порядка; `MessageGroupId` выбирайте без hot group.

## Вопросы для самопроверки

1. Почему FIFO не отменяет идемпотентность consumer?
2. Чем visibility timeout отличается от message retention?
3. Где хранить payload, превышающий лимит сообщения?

---

[← К разделу AWS](README.html)
