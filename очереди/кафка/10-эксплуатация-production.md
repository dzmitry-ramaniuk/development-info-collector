# Эксплуатация Apache Kafka 4.0.0 в production

> **Версионная база главы — Apache Kafka 4.0.0.** В этой версии режим ZooKeeper удалён; новый и уже мигрировавший кластер работает только в **KRaft**. Миграцию из ZooKeeper нужно завершить на Kafka 3.9.x до обновления до 4.0.0.

Факты о версии сверены с [release notes Kafka 4.0.0](https://archive.apache.org/dist/kafka/4.0.0/RELEASE_NOTES.html), [KIP-833](https://cwiki.apache.org/confluence/display/KAFKA/KIP-833%3A+Mark+KRaft+as+Production+Ready) и [KIP-896](https://cwiki.apache.org/confluence/display/KAFKA/KIP-896%3A+Remove+ZooKeeper+mode). Это точная учебная база, а не обозначение «4.x»: перед внедрением следующего patch/minor-релиза повторно проверьте release notes.

## Содержание

1. [Архитектура KRaft-first](#архитектура-kraft-first)
2. [Production-конфигурация Kafka 4.0.0](#production-конфигурация-kafka-400)
3. [Форматирование storage и запуск](#форматирование-storage-и-запуск)
4. [Ежедневные операции](#ежедневные-операции)
5. [Восстановление KRaft](#восстановление-kraft)
6. [Совместимость и rolling upgrade](#совместимость-и-rolling-upgrade)
7. [Legacy migration: ZooKeeper → KRaft](#legacy-migration-zookeeper--kraft)
8. [Disaster recovery и capacity planning](#disaster-recovery-и-capacity-planning)
9. [Проблемы и решения](#проблемы-и-решения)
10. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Архитектура KRaft-first

**Broker** хранит реплики партиций и обслуживает producer/consumer API. **Controller** не обслуживает данные: он хранит metadata log и участвует в Raft-quorum. Один controller — active leader, остальные — voters/followers. Кворум из трёх controllers переносит отказ одного; из пяти — двух.

| Роль | `process.roles` | Назначение |
|------|-----------------|------------|
| Broker | `broker` | Клиентский трафик и реплики партиций |
| Controller | `controller` | Metadata log, выбор leader и управление кластером |
| Combined | `broker,controller` | Только dev/малый кластер; сбой узла одновременно уменьшает data- и metadata-capacity |

Production-база: три нечётных dedicated controller в разных failure domains и не менее трёх brokers. `node.id` уникален среди всех controllers и brokers. Каждый ID из `controller.quorum.voters` должен совпадать с `node.id` соответствующего controller, а hostname/port — с его `CONTROLLER` listener.

## Production-конфигурация Kafka 4.0.0

### Dedicated controller

```properties
# controller-1.properties; для controller-2/3 меняются node.id и listener address
process.roles=controller
node.id=1
controller.listener.names=CONTROLLER
listeners=CONTROLLER://controller1.example.net:9093
listener.security.protocol.map=CONTROLLER:SSL
controller.quorum.voters=1@controller1.example.net:9093,2@controller2.example.net:9093,3@controller3.example.net:9093
metadata.log.dir=/var/lib/kafka/metadata
```

### Dedicated broker

```properties
# broker-101.properties
process.roles=broker
node.id=101
controller.listener.names=CONTROLLER
controller.quorum.voters=1@controller1.example.net:9093,2@controller2.example.net:9093,3@controller3.example.net:9093

listeners=INTERNAL://0.0.0.0:9092
advertised.listeners=INTERNAL://broker101.example.net:9092
inter.broker.listener.name=INTERNAL
listener.security.protocol.map=CONTROLLER:SSL,INTERNAL:SSL

log.dirs=/data/kafka-1,/data/kafka-2
broker.rack=az-a
num.partitions=6
default.replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false
auto.create.topics.enable=false

offsets.topic.replication.factor=3
transaction.state.log.replication.factor=3
transaction.state.log.min.isr=2
```

> TLS/SASL-параметры, ACL и secret paths добавляются под конкретную PKI. Не копируйте example hostnames в production.

## Форматирование storage и запуск

Один `cluster.id` создаётся **один раз** и используется на всех узлах. Форматирование записывает `meta.properties`, но не создаёт кластер в запущенном quorum.

```bash
# Выполнить один раз и сохранить ID в inventory/secret storage
CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
printf '%s\n' "$CLUSTER_ID"

# На каждом controller и broker — его собственный config
bin/kafka-storage.sh format --cluster-id "$CLUSTER_ID" --config config/controller-1.properties
bin/kafka-storage.sh format --cluster-id "$CLUSTER_ID" --config config/broker-101.properties

# Сначала controllers, затем brokers
bin/kafka-server-start.sh -daemon config/controller-1.properties
bin/kafka-server-start.sh -daemon config/broker-101.properties

# Проверка metadata quorum через broker endpoint
bin/kafka-metadata-quorum.sh --bootstrap-server broker101.example.net:9092 describe --status
bin/kafka-metadata-quorum.sh --bootstrap-server broker101.example.net:9092 describe --replication
```

**Нельзя** повторно запускать `format` для «починки» узла: новый cluster/node identity может отделить его от кластера. `--ignore-formatted` нужен лишь в специальных bootstrap-сценариях, когда часть путей уже отформатирована; это не штатная команда восстановления.

## Ежедневные операции

### Добавление и удаление broker

Новому broker назначают уникальный `node.id`, форматируют storage с **тем же** `cluster.id`, запускают и переносят на него реплики. Kafka не перебалансирует существующие партиции автоматически.

```bash
# topics.json: {"version":1,"topics":[{"topic":"orders"},{"topic":"customers"}]}
bin/kafka-reassign-partitions.sh --bootstrap-server broker101.example.net:9092 \
  --topics-to-move-json-file topics.json --broker-list '101,102,103' --generate

# Проверить generated JSON, сохранить proposed assignment в reassignment.json
bin/kafka-reassign-partitions.sh --bootstrap-server broker101.example.net:9092 \
  --reassignment-json-file reassignment.json --execute
bin/kafka-reassign-partitions.sh --bootstrap-server broker101.example.net:9092 \
  --reassignment-json-file reassignment.json --verify
```

Для удаления broker:

1. Сгенерировать/проверить assignment, в котором нет удаляемого ID.
2. Выполнить и дождаться `--verify`.
3. Убедиться, что broker не содержит реплик и не является leader, затем остановить его.
4. Удалить его из inventory/monitoring.

> В KRaft **нет** ZooKeeper znode и нет шага `zookeeper-shell ... rmr /brokers/ids/...`. Не стирайте диски, пока reassignment не завершён.

### Изменение metadata quorum

Пример выше использует **статический** `controller.quorum.voters`. Для него нет безопасной admin-команды «удалить voter на лету»: замену controller выполняют по документированной для релиза процедуре, сохраняя majority. Не следует выдавать broker reassignment за изменение quorum: это разные операции.

Kafka 4.0 также поддерживает dynamic quorum с `controller.quorum.bootstrap.servers` и `kafka-metadata-quorum.sh add-controller/remove-controller`. Его надо выбирать при проектировании кластера, а не смешивать с static voters. Перед `remove-controller` всегда проверяйте текущий leader, lag и что после операции останется majority.

## Восстановление KRaft

| Сбой | Правильное действие | Опасное действие |
|------|---------------------|--------------------|
| Broker потерян, реплики здоровы | Заменить узел с новым `node.id`, перераспределить реплики | Форматировать оставшиеся data dirs |
| Потерян один controller из трёх | Оставить quorum в работе; восстанавливать/заменять только по release runbook | Останавливать ещё controller или менять `cluster.id` |
| Нет majority controllers | Остановить изменения, сохранить все копии metadata log, следовать disaster-recovery документации именно Kafka 4.0.0 | Создавать новый cluster ID, пускать пустой quorum поверх data dirs |

В runbook зафиксируйте `cluster.id`, `node.id`, voter topology, пути, security settings и inventory. Снимки metadata log не заменяют бэкап полезных данных: Kafka replication — не backup, а metadata не содержат payload партиций.

## Совместимость и rolling upgrade

Нужно различать три независимые плоскости:

| Плоскость | Что проверять |
|-----------|----------------|
| Broker ↔ broker/controller | Допустимый upgrade path, порядок controllers/brokers и `metadata.version` |
| Client ↔ broker | Версии Java client и broker не обязаны совпадать: client согласует API versions, но нужно проверить матрицу конкретной client library и необходимые API/features |
| Record/protocol | В KRaft compatibility фиксирует `metadata.version`; это не то же самое, что формат пользовательских records |

В Kafka 4.0 KRaft не нужно следовать старой инструкции «после всех brokers поднять `inter.broker.protocol.version` и `log.message.format.version`». Для KRaft feature level управляется `metadata.version`.

### Безопасная стратегия

1. Прочитать release/upgrade notes **исходной и целевой** версий; не перескакивать неподдерживаемые промежуточные шаги.
2. Снять baseline: quorum status/lag, offline partitions, under-replicated partitions, consumer lag; проверить rollback на staging.
3. Обновлять dedicated controllers по одному, не теряя majority, и проверять quorum после каждого.
4. Обновлять brokers по одному: controlled shutdown, старт, API check, восстановление ISR; не переходить к следующему при URP/offline partitions.
5. Не поднимать `metadata.version`, пока все узлы не обновлены и rollback ещё нужен. Затем проверить финальный feature level и отдельно поднять его только по upgrade notes.
6. Обновлять clients отдельно по волнам/canary; новый broker не делает автоматически безопасным любой старый client.

```bash
bin/kafka-metadata-quorum.sh --bootstrap-server broker101.example.net:9092 describe --status
bin/kafka-topics.sh --bootstrap-server broker101.example.net:9092 --describe --under-replicated-partitions
bin/kafka-broker-api-versions.sh --bootstrap-server broker101.example.net:9092
bin/kafka-features.sh --bootstrap-server broker101.example.net:9092 describe
```

## Legacy migration: ZooKeeper → KRaft

> **Только для существующих legacy-кластеров. ZooKeeper нельзя выбирать для нового кластера, а Kafka 4.0.0 не умеет запускать ZooKeeper mode или выполнять исходную миграцию.**

Целевая цепочка: поддерживаемая исходная версия → Kafka 3.9.x в ZooKeeper mode → online migration в KRaft на 3.9.x → завершение migration и удаление ZooKeeper settings → rolling upgrade KRaft-кластера до 4.0.0.

1. Проверить KRaft limitations, security mappings, `metadata.version`, plugins и supported upgrade path; сделать rollback rehearsal.
2. На 3.9.x создать три dedicated KRaft controllers с единым cluster ID и migration settings из официальной инструкции 3.9; дождаться копирования metadata.
3. Rolling restart brokers 3.9.x в migration mode. В переходной фазе ZooKeeper ещё нужен; не выключать его раньше времени.
4. После валидации перевести brokers в KRaft-only конфигурацию, отключить migration flag по documented state machine и только после завершения вывести ZooKeeper.
5. Зафиксировать необратимую точку в runbook. Затем выполнить обычный KRaft rolling upgrade до 4.0.0.

Конкретные migration properties намеренно не помещены в production 4.0.0 configs: они применимы только к переходной 3.9.x и должны быть взяты из [документации Kafka 3.9](https://kafka.apache.org/39/documentation/zk2kraft.html) для точной топологии. `zookeeper.connect` и `zookeeper-shell.sh` не применимы к KRaft-only кластеру.

## Disaster recovery и capacity planning

Считайте диск как `ingress bytes/s × retention seconds × replication factor × reserve`. Отдельно учитывайте network replication, rebalance headroom, page cache, compaction и failure одной AZ. При `replication.factor=3`, `min.insync.replicas=2` и `acks=all` кластер может пережить отказ одной реплики без потери уже подтверждённых записей, но потеря capacity может остановить новые записи.

Для DR используйте отдельный кластер и MirrorMaker 2/replication service. Тестируйте failover, offset translation, ACL/config replication, DNS/client bootstrap и failback. Ни metadata quorum, ни три реплики в одном failure domain не заменяют межкластерный DR.

## Проблемы и решения

| Симптом | Проверка | Действие |
|---------|----------|----------|
| Quorum lag растёт | `kafka-metadata-quorum.sh ... describe --replication`, disk/network controller | Вернуть controller capacity; не перезапускать majority одновременно |
| Under-replicated partitions | Broker health, ISR, disk/network, replica fetch | Устранить capacity/сетевую причину до maintenance |
| Consumer lag | Processing time, partition count, rebalances | Масштабировать consumers до числа партиций, оптимизировать handler |
| Disk full | Retention, compaction lag, skew | Освободить capacity контролируемо; не удалять файлы партиций вручную |

## Вопросы для самопроверки

1. **Какой metadata mode доступен в Kafka 4.0.0?**
   Только KRaft; ZooKeeper mode удалён.
2. **Чем broker отличается от controller?**
   Broker обслуживает records и clients, controller реплицирует metadata log и управляет метаданными.
3. **Почему нельзя повторять `kafka-storage.sh format` при сбое?**
   Форматирование задаёт cluster/node identity; это bootstrap, а не repair.
4. **Как удалить KRaft broker?**
   Перенести все его реплики partition reassignment, проверить завершение и остановить; ZooKeeper-команд нет.
5. **Когда можно поднять `metadata.version`?**
   После обновления всех узлов, проверок и когда rollback больше не нужен; точно по upgrade notes целевого релиза.
6. **Где выполнять ZooKeeper → KRaft migration?**
   На Kafka 3.9.x; до перехода на 4.0.0.
