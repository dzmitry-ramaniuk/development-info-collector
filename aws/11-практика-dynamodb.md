---
---

# DynamoDB: воспроизводимая практика

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Что проверяем](#что-проверяем)
3. [Подготовка и локальный запуск](#подготовка-и-локальный-запуск)
4. [Явный запуск в AWS](#явный-запуск-в-aws)
5. [Ошибки и восстановление](#ошибки-и-восстановление)
6. [Упражнения и самопроверка](#упражнения-и-самопроверка)

## Актуальность материала

- **Дата сверки API/документации:** 2026-10-15.
- **Baseline локального запуска:** AWS CLI 2.29.1, DynamoDB Local 3.3.1, Docker Engine 28.4.0, Python 3.10+; DynamoDB `CreateTable`, `PutItem`, `GetItem`, `Query`, `DeleteTable`.
- **Статус:** `current` для приведённых API; локальный emulator не подтверждает IAM, quotas, billing, PITR и production capacity. Запуск в аккаунте AWS здесь не заявляется.
- **Первичные источники:** [DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.DownloadingAndRunning.html), [CLI PutItem и condition-expression](https://docs.aws.amazon.com/cli/latest/reference/dynamodb/put-item.html), [CLI Query](https://docs.aws.amazon.com/cli/latest/reference/dynamodb/query.html).

## Что проверяем

Два заказа одного tenant сохраняются только один раз по бизнес-ключу. Повторная отправка первого заказа пытается изменить статус; conditional write отклоняет дубликат, исходные данные остаются. Чтение выполняется через `Query`, не `Scan`.

| Access pattern | Partition key | Sort key / условие |
|---|---|---|
| Получить заказ | `TENANT#demo` | `ORDER#001` |
| Список заказов tenant | `TENANT#demo` | `begins_with(SK, 'ORDER#')` |
| Не создать тот же заказ повторно | Та же пара PK/SK | `attribute_not_exists(PK)` для записываемого item |

Одна таблица содержит только учебные данные. [Скрипт](../examples/infra/02-dynamodb/01-lab.py) генерирует уникальное имя `infra-lab-...`, создаёт PK/SK, ждёт готовности, выполняет две записи, **требует** `ConditionalCheckFailedException` от повторной записи, проверяет исходный статус и порядок Query. В `finally` он удаляет только успешно созданную им таблицу и ждёт удаления. Любая другая ошибка вместо ожидаемого duplicate — провал опыта.

## Подготовка и локальный запуск

Нужны AWS CLI 2, Python 3.10+ и Docker. Из корня репозитория:

```bash
# Только локальный emulator; порт открыт на loopback.
docker run -d --name infra-dynamodb-local -p 127.0.0.1:8000:8000 amazon/dynamodb-local@sha256:ff89bd48ff32cd8d9be5fee8873b65b8854dc408f1afe881be6eb00247bc0dab -jar DynamoDBLocal.jar -inMemory -sharedDb
python3 examples/infra/02-dynamodb/01-lab.py
# Created infra-lab-...
# Duplicate rejected: ConditionalCheckFailedException
# Read verified: status=NEW; Query returned ORDER#001, ORDER#002
# Cleanup complete: infra-lab-... deleted

docker rm -f infra-dynamodb-local
python3 examples/infra/02-dynamodb/02-test-lab.py
```

Дождитесь сообщения о готовности в `docker logs infra-dynamodb-local`, прежде чем вызывать CLI; connection refused в первые секунды означает незавершённый старт. Локальный режим всегда задаёт loopback endpoint и фиктивные credentials, не требует AWS login. В команде закреплён digest emulator, фактически полученный при локальной проверке 2026-10-15; его обновляют отдельным проверяемым изменением.

Локальный цикл фактически проверен 2026-10-15: create, conditional duplicate, чтение/Query и cleanup прошли, после опыта `ListTables` вернул пустой список. Успех — сообщения о создании/проверках/удалении, exit code 0, таблица удалена. Offline-тесты отдельно проверяют cleanup при отказе и ошибочный negative test; они не обращаются к AWS.

## Явный запуск в AWS

Это необязательный отдельный опыт с **платными API/storage**. Используйте sandbox account, свой регион, именованный AWS CLI profile с короткоживущими credentials и budget; локальная команда выше не переключается в облако автоматически.

```bash
python3 examples/infra/02-dynamodb/01-lab.py --aws --profile sandbox --region eu-central-1
```

Минимальный identity policy ограничивают ресурсами `arn:aws:dynamodb:REGION:ACCOUNT:table/infra-lab-*` (подставьте регион/аккаунт; для других partition измените ARN prefix):

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Action": ["dynamodb:CreateTable", "dynamodb:DescribeTable", "dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:Query", "dynamodb:DeleteTable"],
    "Resource": "arn:aws:dynamodb:REGION:ACCOUNT:table/infra-lab-*"
  }]
}
```

Настройки аккаунта/SCP могут накладывать дополнительные условия. Не используйте административную роль ради устранения AccessDenied. Скрипт создаёт on-demand table без GSI/PITR; это учебная последовательность API, не production IaC. Для production потребуются управляемая конфигурация, monitoring, recovery и capacity design из [главы DynamoDB](06-dynamodb.html).

## Ошибки и восстановление

Если нарушен expected error, checksum бизнес-результата или порядок Query, скрипт возвращает exit code 1 и всё равно пытается cleanup. Если credentials истекли, процесс был принудительно завершён или cleanup получил отказ, найдите имя созданной таблицы в выводе и удалите **только её** в том же sandbox account/region, затем подтвердите `table-not-exists`. Автоматическая cleanup не является гарантией при kill/network/permission failure. Локальный `-inMemory` emulator удаляет все свои данные после остановки контейнера.

## Упражнения и самопроверка

1. Отправьте повторный бизнес-заказ с другим статусом: почему успешная conditional write означала бы ошибку сценария?
2. Добавьте второй tenant и проверьте, что Query первого не возвращает его данные.
3. Вызовите искусственную ошибку после create и проверьте удаление таблицы.

**Почему `attribute_not_exists(PK)` не запрещает другие SK у tenant?** Условие проверяет item с записываемой парой ключей. **Почему TTL не заменяет cleanup?** Удаление по TTL асинхронно и не удаляет саму таблицу.

[← К разделу AWS](README.html)
