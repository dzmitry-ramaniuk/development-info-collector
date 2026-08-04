# Lambda: serverless-функции и современные паттерны

> **Дата ревизии:** 4 августа 2026 года. Числовые значения проверены на эту дату; доступность функций, цены и quotas зависят от региона, типа аккаунта и одобренных AWS повышений. Перед production-развёртыванием сверяйтесь с Service Quotas и AWS Console.

**Официальные источники:** [service documentation](https://docs.aws.amazon.com/lambda/latest/dg/welcome.html) · [quotas](https://docs.aws.amazon.com/lambda/latest/dg/gettingstarted-limits.html) · [pricing](https://aws.amazon.com/lambda/pricing/)

## Содержание

1. [Когда выбирать Lambda](#когда-выбирать-lambda)
2. [Лимиты выполнения](#лимиты-выполнения)
3. [Runtimes и архитектуры](#runtimes-и-архитектуры)
4. [Модели concurrency](#модели-concurrency)
5. [Современные сценарии](#современные-сценарии)
   - [SnapStart](#snapstart)
   - [Response streaming](#response-streaming)
   - [SQS partial batch response](#sqs-partial-batch-response)
   - [Ретраи event source и destinations](#ретраи-event-source-и-destinations)
   - [Идемпотентность](#идемпотентность)
6. [Практический baseline для production](#практический-baseline-для-production)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Когда выбирать Lambda

**AWS Lambda** подходит для event-driven и bursty-нагрузки: HTTP API, обработки событий S3/SQS/EventBridge, automation и коротких фоновых задач. Для постоянной высокой нагрузки, выполнения дольше 15 минут, особых требований к хосту или предсказуемой latency контейнеры либо EC2 часто проще и дешевле.

## Лимиты выполнения

Значения ниже проверены 4 августа 2026 года. Это не контракт: часть quotas региональная и повышаемая на уровне аккаунта, поэтому проверяйте [Lambda quotas](https://docs.aws.amazon.com/lambda/latest/dg/gettingstarted-limits.html) и Service Quotas.

| Параметр | Лимит / диапазон | Повышаемый |
|---|---:|---|
| Timeout одного invocation | 900 секунд (15 минут) | Нет |
| Память | 128–10 240 MB, шаг 1 MB | Нет |
| Ephemeral storage `/tmp` | 512–10 240 MB, шаг 1 MB | Нет |
| Environment variables | 4 KB суммарно | Нет |
| Layers | 5 | Нет |
| ZIP deployment package | 50 MB сжатый; 250 MB распакованный вместе с layers | Нет |
| Container image | 10 GB | Нет |
| Sync request/response payload | 6 MB каждый | Нет |
| Streamed sync response | 200 MB; первые 6 MB без ограничения скорости, далее 2 MB/s | Нет |
| Async invocation payload | 1 MB | Нет |
| Concurrent executions | 1 000 на регион по умолчанию | Да, account-specific |
| Масштабирование функции | до 1 000 execution environments за 10 секунд | Нет |

## Runtimes и архитектуры

AWS регулярно добавляет и выводит runtimes из поддержки. Таблица отражает managed runtime-линейки, указанные AWS на 4 августа 2026 года; точные minor versions и даты deprecation всегда проверяйте в [runtime support policy](https://docs.aws.amazon.com/lambda/latest/dg/lambda-runtimes.html).

| Семейство | Поддерживаемые линии | Архитектуры |
|---|---|---|
| Node.js | 20, 22, 24 | `x86_64`, `arm64` |
| Python | 3.11, 3.12, 3.13, 3.14 | `x86_64`, `arm64` |
| Java | 8 (`al2`), 11, 17, 21, 25 | `x86_64`, `arm64` |
| .NET | 8, 9, 10 | `x86_64`, `arm64` |
| Ruby | 3.3, 3.4 | `x86_64`, `arm64` |
| OS-only custom runtime | Amazon Linux 2, Amazon Linux 2023 | `x86_64`, `arm64` |

> Container images также позволяют custom runtime, но образ должен быть собран для **одной** архитектуры; multi-architecture images Lambda не принимает.

## Модели concurrency

| Механизм | Что ограничивает или резервирует | Для чего применять | Важный нюанс |
|---|---|---|---|
| **Account concurrency** | Общий региональный пул одновременных executions | Планирование ёмкости аккаунта | По умолчанию 1 000; минимум 100 units остаётся unreserved для функций без reserved concurrency |
| **Reserved concurrency** | Одновременно и гарантирует функции долю пула, и ставит ей верхнюю границу | Изоляция noisy neighbor, защита downstream; `0` отключает функцию | Не прогревает environments и отдельно не тарифицируется |
| **Provisioned concurrency** | Заранее инициализирует заданное число environments у version/alias | Стабильная низкая latency и снижение cold starts | Платная, расходует concurrency и не является верхней границей; сверх неё возможен обычный scale-out |

## Современные сценарии

### SnapStart

**SnapStart** создаёт зашифрованный snapshot уже инициализированного execution environment при публикации версии и восстанавливает его при scale-out. Он полезен для функций с тяжёлой инициализацией и нерегулярными cold starts. Проверяйте [поддерживаемые runtimes и ограничения SnapStart](https://docs.aws.amazon.com/lambda/latest/dg/snapstart.html): нельзя сочетать SnapStart с provisioned concurrency, EFS и ephemeral storage больше 512 MB. После restore заново обеспечьте уникальность случайных значений, соединений и временных credentials; используйте runtime hooks.

### Response streaming

**Response streaming** через function URL либо поддерживаемую интеграцию уменьшает time-to-first-byte и позволяет отдавать ответы больше обычных 6 MB. На 4 августа 2026 года streamed response ограничен 200 MB; значение регионально доступно и может измениться. Обрабатывайте backpressure, disconnect клиента и ошибку после отправки заголовков; streaming не делает долгую задачу асинхронной.

### SQS partial batch response

При batch polling ошибка одного сообщения по умолчанию возвращает весь batch. Включите `ReportBatchItemFailures` и возвращайте `batchItemFailures` только для неуспешных message IDs. Успешные сообщения тогда удаляются, а проблемные становятся видимыми после visibility timeout. Для FIFO после первой ошибки прекращайте обработку и возвращайте ошибочное и все необработанные сообщения, чтобы сохранить порядок. См. [partial batch responses](https://docs.aws.amazon.com/lambda/latest/dg/services-sqs-errorhandling.html).

### Ретраи event source и destinations

- **Асинхронные push-вызовы**: Lambda по умолчанию повторяет ошибки функции ещё два раза; `MaximumRetryAttempts` и `MaximumEventAgeInSeconds` ограничивают ретраи и возраст. `on-success`/`on-failure destinations` передают результат в SQS, SNS, EventBridge или Lambda; DLQ хранит только исходное событие и применяется к async invocation.
- **SQS/Kinesis/DynamoDB Streams**: ретраями управляет event source mapping. Для streams настраивайте maximum record age, retry attempts, `bisectBatchOnFunctionError` и on-failure destination; для SQS — visibility timeout, redrive policy и partial batch response.
- **Синхронные вызовы**: Lambda не повторяет их автоматически; политика retry принадлежит клиенту. Используйте exponential backoff с jitter и ограниченный retry budget.

### Идемпотентность

At-least-once delivery означает дубликаты даже при успешной обработке. Выберите стабильный idempotency key из бизнес-события, атомарно фиксируйте состояние `IN_PROGRESS`/`COMPLETED` в DynamoDB с conditional write и TTL, кэшируйте результат и делайте downstream side effects повторяемыми либо защищёнными unique constraint. Не используйте `awsRequestId`: он новый на каждый retry. Для Java/Python/TypeScript можно применять idempotency utility из AWS Lambda Powertools.

## Практический baseline для production

1. Зафиксируйте reserved concurrency и защитите downstream; provisioned concurrency добавляйте по измерениям p95/p99.
2. Настройте retry budget, DLQ/destination и runbook redrive отдельно для каждого trigger.
3. Делайте handlers идемпотентными и тестируйте дубликаты, partial failures и out-of-order delivery.
4. Используйте structured logging, correlation ID, tracing и alarms по errors, throttles, iterator age и DLQ.
5. Бенчмаркайте `arm64` и memory size; стоимость сверяйте с [Lambda pricing](https://aws.amazon.com/lambda/pricing/).

## Вопросы для самопроверки

1. Почему reserved concurrency одновременно является гарантией и верхней границей?
2. Чем destination отличается от DLQ и кому принадлежат ретраи при sync invocation?
3. Как partial batch response уменьшает повторную обработку SQS batch?
4. Какие состояния нельзя бездумно сохранять в SnapStart snapshot?

---

[← К разделу AWS](README.md)
