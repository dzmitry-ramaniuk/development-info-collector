# SNS: pub/sub, fan-out и гарантии доставки

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/sns/latest/dg/welcome.html) · [quotas](https://docs.aws.amazon.com/general/latest/gr/sns.html) · [pricing](https://aws.amazon.com/sns/pricing/)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** публичные API и документация AWS, проверенные 2026-08-04; версии управляемых сервисов уточняются в тексте
- **Статус примеров:** `current`
- **Первичные источники:** [AWS Documentation](https://docs.aws.amazon.com/); [AWS Architecture Center](https://aws.amazon.com/architecture/)

## Содержание

1. [Fan-out и подписчики](#fan-out-и-подписчики)
2. [Сравнение доставки](#сравнение-доставки)
3. [Ретраи и DLQ](#ретраи-и-dlq)
4. [Практический baseline для production](#практический-baseline-для-production)
5. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Fan-out и подписчики

**SNS** публикует одно сообщение в SQS, Lambda, HTTP(S), email/SMS, mobile push и другие endpoints. Subscription filter policies сокращают ненужный fan-out. Для durable независимых consumers предпочитайте `SNS -> отдельная SQS на consumer`: очередь буферизует пики и позволяет собственный retry/redrive.

FIFO topics обеспечивают порядок и дедупликацию внутри message group. Чтобы сохранить FIFO end-to-end, используйте совместимый FIFO endpoint; Standard topic порядка не гарантирует.

## Сравнение доставки

На 4 августа 2026 года размеры и retry quotas ниже зависят от региона, endpoint и аккаунта; проверяйте официальные quotas каждого сервиса.

| Свойство | SQS | SNS | EventBridge |
|---|---|---|---|
| Delivery | Standard: at-least-once; FIFO: exactly-once processing в пределах 5-минутного окна дедупликации, но consumer всё равно проектируют идемпотентным | At-least-once для поддерживаемых durable endpoints; поведение зависит от transport | At-least-once до target |
| Ordering | Standard не гарантирует; FIFO строго внутри `MessageGroupId` | Standard не гарантирует; FIFO topic сохраняет порядок внутри message group только с FIFO SQS/Lambda endpoints | Глобальный порядок не гарантируется |
| Retries | Visibility timeout и redrive policy; consumer/Lambda event source управляет повторами | Delivery policy зависит от endpoint; server-side/client-side backoff, затем subscription DLQ | Target retry policy: по умолчанию до 24 часов и 185 попыток, затем DLQ/потеря события |
| DLQ | Redrive в SQS DLQ после `maxReceiveCount` | SQS DLQ привязывается к subscription и хранит недоставленные сообщения | SQS DLQ привязывается к rule target; для event bus есть отдельная DLQ шифрования/permissions |
| Максимальный event/message size | 1 MiB | 256 KB, включая attributes | 256 KB для event entry |

Максимальный SNS message size 256 KB проверен 4 августа 2026 года и включает message attributes; для больших payload применяйте S3 claim-check pattern.

## Ретраи и DLQ

SNS применяет endpoint-specific delivery policy. Для managed endpoints AWS повторяет delivery с backoff; для HTTP/S можно настраивать часть policy в допустимых пределах. **Subscription DLQ** — SQS queue, куда SNS помещает сообщения, исчерпавшие delivery retries. Она не ловит ошибку бизнес-обработки после успешной доставки endpoint: для этого consumer нужен собственный retry/DLQ.

## Практический baseline для production

1. Отдельная SQS subscription и DLQ на каждого критичного consumer.
2. Least-privilege topic/queue policies, encryption и защита confused deputy через `aws:SourceArn`.
3. Версионированный event contract, idempotent consumers и correlation ID.
4. Alarms по failed/delayed notifications и DLQ depth.

## Вопросы для самопроверки

1. Где проходит граница ответственности между subscription DLQ и consumer DLQ?
2. Какие endpoints сохраняют порядок FIFO topic?
3. Когда EventBridge routing удобнее SNS fan-out?

---

[← К разделу AWS](README.md)
