# Эксплуатация и Production Best Practices


## Содержание

1. [Production Deployment](#production-deployment)
   - [Sizing и Capacity Planning](#sizing-и-capacity-planning)
   - [Конфигурация Production кластера](#конфигурация-production-кластера)
   - [Высокая доступность (HA)](#высокая-доступность-ha)
   - [Disaster Recovery](#disaster-recovery)
   - [Операции обслуживания](#операции-обслуживания)
   - [Производительность и тюнинг](#производительность-и-тюнинг)
   - [Квоты и Rate Limiting](#квоты-и-rate-limiting)
   - [Troubleshooting](#troubleshooting)
2. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Production Deployment

### Sizing и Capacity Planning

#### Расчет требований к ресурсам

**Хранилище (Disk)**
```
Размер = (throughput_mb_per_sec × retention_seconds) × replication_factor × (1 + overhead)

Пример:
- Throughput: 100 MB/s
- Retention: 7 дней (604800 секунд)
- Replication: 3
- Overhead: 0.2 (20% на индексы, сегменты)

Размер = 100 × 604800 × 3 × 1.2 = 217 TB
```

**Network**
```
Peak bandwidth = throughput × replication_factor × 2

Пример:
- Throughput: 100 MB/s
- Replication: 3
- ×2 для produce + consume

Peak bandwidth = 100 × 3 × 2 = 600 MB/s
```

**Memory**
- Page cache: ~75% RAM для page cache
- Heap: 4-6 GB для JVM heap (не больше!)
- Минимум: 32 GB RAM на broker
- Оптимально: 64-128 GB RAM

**CPU**
- Минимум: 8 cores
- Оптимально: 16-32 cores
- Compression увеличивает CPU usage

#### Количество партиций

**Рекомендации**
```
Max partitions per broker = 4000 (консервативно)
Max partitions per cluster = 200000

Партиций на топик = max(
    throughput_required / throughput_per_partition,
    max_consumers_needed,
    storage_required / max_partition_size
)

Пример:
- Требуется 1 GB/s throughput
- Одна партиция обрабатывает ~50 MB/s
- Партиций = 1000 / 50 = 20 партиций
```

### Конфигурация Production кластера

**Broker конфигурация**
```properties
# server.properties

############################# Server Basics #############################

# Unique ID для каждого брокера
broker.id=1

############################# Socket Server Settings #############################

# Listeners
listeners=PLAINTEXT://0.0.0.0:9092,SSL://0.0.0.0:9093
advertised.listeners=PLAINTEXT://broker1.example.com:9092,SSL://broker1.example.com:9093

# Сетевые потоки
num.network.threads=8
num.io.threads=16

# Размеры буферов
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

############################# Log Basics #############################

# Директории для данных (RAID-10 или отдельные диски)
log.dirs=/data/kafka-logs-1,/data/kafka-logs-2,/data/kafka-logs-3

# Партиций по умолчанию для новых топиков
num.partitions=3

# Recovery threads
num.recovery.threads.per.data.dir=2

############################# Log Retention Policy #############################

# Retention по времени
log.retention.hours=168  # 7 дней
log.retention.check.interval.ms=300000

# Retention по размеру (опционально)
# log.retention.bytes=1073741824

# Размер сегмента
log.segment.bytes=1073741824  # 1 GB

############################# Replication #############################

# Минимальные ISR для приёма записи
min.insync.replicas=2

# Replication фактор по умолчанию
default.replication.factor=3

# Timeout для реплик
replica.lag.time.max.ms=30000

# Размер fetch для follower реплик
replica.fetch.max.bytes=1048576

############################# Log Flush Policy #############################

# Не форсировать flush, полагаться на OS page cache
# log.flush.interval.messages=10000
# log.flush.interval.ms=1000

############################# Zookeeper / KRaft #############################

# ZooKeeper connection
zookeeper.connect=zk1:2181,zk2:2181,zk3:2181/kafka
zookeeper.connection.timeout.ms=18000

# Или KRaft mode (Kafka 3.0+)
# process.roles=broker,controller
# node.id=1
# controller.quorum.voters=1@kafka1:9093,2@kafka2:9093,3@kafka3:9093

############################# Group Coordinator Settings #############################

# Offset retention
offsets.retention.minutes=10080  # 7 дней

# Transaction timeout
transaction.state.log.replication.factor=3
transaction.state.log.min.isr=2

############################# Performance Tuning #############################

# Compression
compression.type=producer  # использовать producer compression

# Background threads
background.threads=10

# Replica fetcher threads
num.replica.fetchers=4

# Auto create topics
auto.create.topics.enable=false  # отключить в production

# Leader imbalance
auto.leader.rebalance.enable=true
leader.imbalance.per.broker.percentage=10

############################# Quotas #############################

# Producer quotas (bytes/sec)
# quota.producer.default=10485760  # 10 MB/s

# Consumer quotas (bytes/sec)
# quota.consumer.default=20971520  # 20 MB/s
```

**OS тюнинг (Linux)**

```bash
# /etc/sysctl.conf

# Увеличить файловые дескрипторы
fs.file-max = 500000

# Сетевые буферы
net.core.rmem_max = 134217728
net.core.wmem_max = 134217728
net.ipv4.tcp_rmem = 4096 87380 67108864
net.ipv4.tcp_wmem = 4096 65536 67108864

# TCP settings
net.ipv4.tcp_window_scaling = 1
net.ipv4.tcp_max_syn_backlog = 10000
net.core.netdev_max_backlog = 10000

# Memory overcommit
vm.overcommit_memory = 1

# Swap usage (минимизировать)
vm.swappiness = 1

# Dirty pages (для большого RAM)
vm.dirty_ratio = 80
vm.dirty_background_ratio = 5

# Apply
sysctl -p
```

**Limits для Kafka пользователя**
```bash
# /etc/security/limits.conf

kafka soft nofile 100000
kafka hard nofile 100000
kafka soft nproc 32768
kafka hard nproc 32768
```

**JVM настройки**
```bash
# kafka-server-start.sh или systemd

export KAFKA_HEAP_OPTS="-Xms6g -Xmx6g"  # НЕ больше 6-8 GB!
export KAFKA_JVM_PERFORMANCE_OPTS="-server \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=20 \
    -XX:InitiatingHeapOccupancyPercent=35 \
    -XX:G1HeapRegionSize=16M \
    -XX:MinMetaspaceFreeRatio=50 \
    -XX:MaxMetaspaceFreeRatio=80 \
    -XX:+ExplicitGCInvokesConcurrent"

# GC логирование
export KAFKA_GC_LOG_OPTS="-Xlog:gc*:file=/var/log/kafka/gc.log:time,tags:filecount=10,filesize=100M"

# JMX для мониторинга
export JMX_PORT=9999
```

### Высокая доступность (HA)

#### Multi-broker кластер

**Минимальная конфигурация HA**
- 3 брокера
- Replication factor = 3
- min.insync.replicas = 2
- acks = all

**Rack awareness**
```properties
# Распределение реплик по rack (DC, availability zones)
broker.rack=rack1

# Топики с rack awareness
kafka-topics.sh --create \
  --topic orders \
  --partitions 6 \
  --replication-factor 3 \
  --config min.insync.replicas=2 \
  --replica-assignment 1:2:3,2:3:1,3:1:2,1:2:3,2:3:1,3:1:2
```

#### ZooKeeper Ensemble

**Production ZooKeeper конфигурация**
```properties
# zoo.cfg

# Data directory
dataDir=/var/lib/zookeeper

# Transaction log directory (отдельный диск!)
dataLogDir=/var/lib/zookeeper-logs

# Client port
clientPort=2181

# Cluster configuration
initLimit=10
syncLimit=5

# Servers
server.1=zk1:2888:3888
server.2=zk2:2888:3888
server.3=zk3:2888:3888

# Auto purge
autopurge.snapRetainCount=3
autopurge.purgeInterval=24

# Performance
maxClientCnxns=0
tickTime=2000
```

**Мониторинг ZooKeeper**
```bash
# Health check
echo ruok | nc localhost 2181
# Ответ: imok

# Statistics
echo stat | nc localhost 2181

# Watch count
echo wchs | nc localhost 2181
```

#### KRaft Mode (без ZooKeeper)

**Controller конфигурация**
```properties
# Kafka 3.3+ production

process.roles=controller
node.id=1
controller.quorum.voters=1@controller1:9093,2@controller2:9093,3@controller3:9093
controller.listener.names=CONTROLLER
listeners=CONTROLLER://0.0.0.0:9093

# Metadata log directory
metadata.log.dir=/var/lib/kafka-metadata

# Quorum configuration
controller.quorum.election.timeout.ms=1000
controller.quorum.fetch.timeout.ms=2000
```

**Combined broker+controller**
```properties
process.roles=broker,controller
node.id=1
controller.quorum.voters=1@kafka1:9093,2@kafka2:9093,3@kafka3:9093

listeners=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
advertised.listeners=PLAINTEXT://kafka1:9092

controller.listener.names=CONTROLLER
inter.broker.listener.name=PLAINTEXT

metadata.log.dir=/var/lib/kafka-metadata
log.dirs=/var/lib/kafka-data
```

### Disaster Recovery

#### Backup стратегии

**MirrorMaker 2.0**
```properties
# mm2.properties

# Source and target clusters
clusters=source, target
source.bootstrap.servers=source-cluster:9092
target.bootstrap.servers=target-cluster:9092

# Replication flows
source->target.enabled=true
source->target.topics=.*  # или specific topics

# Consumer group replication
sync.group.offsets.enabled=true
sync.group.offsets.interval.seconds=60

# Topic configuration sync
sync.topic.configs.enabled=true
sync.topic.acls.enabled=true

# Heartbeat
emit.heartbeats.enabled=true
emit.heartbeats.interval.seconds=5

# Checkpoints
emit.checkpoints.enabled=true
emit.checkpoints.interval.seconds=60
```

**Запуск MirrorMaker 2.0**
```bash
connect-mirror-maker.sh mm2.properties
```

**Kafka Connect для backup в S3**
```json
{
  "name": "s3-backup-sink",
  "config": {
    "connector.class": "io.confluent.connect.s3.S3SinkConnector",
    "tasks.max": "10",
    "topics.regex": ".*",
    "s3.region": "us-east-1",
    "s3.bucket.name": "kafka-backup",
    "flush.size": "10000",
    "format.class": "io.confluent.connect.s3.format.parquet.ParquetFormat",
    "partitioner.class": "io.confluent.connect.storage.partitioner.TimeBasedPartitioner",
    "partition.duration.ms": "3600000",
    "path.format": "'year'=YYYY/'month'=MM/'day'=dd/'hour'=HH",
    "storage.class": "io.confluent.connect.s3.storage.S3Storage",
    "rotate.interval.ms": "3600000"
  }
}
```

#### Failover procedures

**Active-Passive setup**
```bash
# 1. Мониторим primary cluster
# 2. При сбое переключаемся на secondary
# 3. Обновляем DNS или load balancer
# 4. Клиенты переподключаются к secondary

# Ручное переключение
# Обновить bootstrap.servers в приложениях
# Или использовать DNS failover
```

**Active-Active (multi-DC)**
```
Primary DC ←→ Secondary DC
    ↓              ↓
MirrorMaker 2  MirrorMaker 2
    ↓              ↓
Bi-directional replication
```

### Операции обслуживания

#### Обновление версии Kafka

**Rolling upgrade процедура**
```bash
# 1. Прочитать release notes и совместимость
# 2. Обновить конфигурацию (если требуется)

# 3. По одному брокеру:
# a. Остановить брокер
systemctl stop kafka

# b. Обновить бинарники
tar -xzf kafka_2.13-3.5.0.tgz -C /opt/kafka

# c. Запустить брокер
systemctl start kafka

# d. Проверить логи и метрики
tail -f /var/log/kafka/server.log
kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# e. Подождать восстановления ISR
kafka-topics.sh --describe --bootstrap-server localhost:9092 --under-replicated-partitions

# f. Повторить для следующего брокера

# 4. После обновления всех брокеров обновить inter.broker.protocol.version и log.message.format.version
```

#### Добавление брокера в кластер

```bash
# 1. Установить и сконфигурировать новый брокер
# Уникальный broker.id
# Те же настройки что и у других брокеров

# 2. Запустить брокер
systemctl start kafka

# 3. Создать reassignment plan
kafka-topics.sh --bootstrap-server localhost:9092 --topics-to-move-json-file topics.json --broker-list "1,2,3,4" --generate

# topics.json
{
  "version": 1,
  "topics": [
    {"topic": "orders"},
    {"topic": "customers"}
  ]
}

# 4. Выполнить reassignment
kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --reassignment-json-file reassignment.json --execute

# 5. Проверить прогресс
kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --reassignment-json-file reassignment.json --verify
```

#### Удаление брокера

```bash
# 1. Создать reassignment план БЕЗ удаляемого брокера
kafka-topics.sh --topics-to-move-json-file topics.json --broker-list "1,2,3" --generate

# 2. Выполнить reassignment
kafka-reassign-partitions.sh --reassignment-json-file reassignment.json --execute

# 3. Дождаться завершения
kafka-reassign-partitions.sh --reassignment-json-file reassignment.json --verify

# 4. Остановить брокер
systemctl stop kafka

# 5. Удалить из ZooKeeper (если необходимо)
zookeeper-shell.sh localhost:2181 rmr /brokers/ids/4
```

#### Rebalancing партиций

**Preferred replica election**
```bash
# Auto (включено по умолчанию)
auto.leader.rebalance.enable=true

# Вручную для всех топиков
kafka-leader-election.sh --bootstrap-server localhost:9092 --election-type PREFERRED --all-topic-partitions

# Для конкретного топика
kafka-leader-election.sh --bootstrap-server localhost:9092 --election-type PREFERRED --topic orders
```

**Manual reassignment**
```bash
# 1. Текущее распределение
kafka-topics.sh --describe --topic orders --bootstrap-server localhost:9092

# 2. Создать reassignment JSON
{
  "version": 1,
  "partitions": [
    {"topic": "orders", "partition": 0, "replicas": [1,2,3]},
    {"topic": "orders", "partition": 1, "replicas": [2,3,1]},
    {"topic": "orders", "partition": 2, "replicas": [3,1,2]}
  ]
}

# 3. Выполнить
kafka-reassign-partitions.sh --execute --reassignment-json-file reassignment.json --bootstrap-server localhost:9092

# 4. Throttle для минимизации влияния
kafka-configs.sh --alter --add-config 'leader.replication.throttled.rate=10485760,follower.replication.throttled.rate=10485760' --entity-type brokers --entity-name 1 --bootstrap-server localhost:9092
```

#### Управление дисками

**Добавление нового диска**
```bash
# 1. Примонтировать новый диск
mkdir /data/kafka-logs-4
mount /dev/sdd1 /data/kafka-logs-4

# 2. Обновить конфигурацию
log.dirs=/data/kafka-logs-1,/data/kafka-logs-2,/data/kafka-logs-3,/data/kafka-logs-4

# 3. Перезапустить брокер
systemctl restart kafka

# 4. Новые партиции будут создаваться на всех дисках
# Существующие партиции можно переместить с помощью reassignment
```

**Замена диска**
```bash
# 1. Идентифицировать партиции на диске
ls -la /data/kafka-logs-broken/

# 2. Переместить партиции на другие брокеры
kafka-reassign-partitions.sh --execute ...

# 3. После завершения reassignment остановить брокер
systemctl stop kafka

# 4. Заменить диск
# 5. Очистить директорию или создать новую
# 6. Запустить брокер
systemctl start kafka
```

### Производительность и тюнинг

#### Producer оптимизация

```java
Properties props = new Properties();

// Батчинг для throughput
props.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);  // 32 KB
props.put(ProducerConfig.LINGER_MS_CONFIG, 10);

// Compression
props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

// Для высокого throughput
props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);  // 64 MB

// Idempotence и exactly-once
props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
props.put(ProducerConfig.ACKS_CONFIG, "all");
props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

// Retry настройки
props.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 100);
props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
```

#### Consumer оптимизация

```java
Properties props = new Properties();

// Fetch size для throughput
props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);  // 1 KB
props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
props.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1048576);  // 1 MB

// Poll настройки
props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);  // 5 минут

// Session timeout
props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);

// Auto commit (или manual)
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

// Isolation level для transactional reads
props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
```

#### Broker тюнинг

```properties
# Размер replica fetch
replica.fetch.max.bytes=1048576
replica.fetch.min.bytes=1

# Сетевые потоки
num.network.threads=8
num.io.threads=16

# Request queue
queued.max.requests=500

# Log cleaner
log.cleaner.threads=2
log.cleaner.dedupe.buffer.size=134217728

# Compression
compression.type=producer

# Message size
message.max.bytes=1000012
replica.fetch.max.bytes=1048588
```

### Квоты и Rate Limiting

**Producer quotas**
```bash
# Установить квоту для пользователя
kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter \
  --add-config 'producer_byte_rate=1048576,consumer_byte_rate=2097152' \
  --entity-type users \
  --entity-name alice

# Квота для client.id
kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter \
  --add-config 'producer_byte_rate=1048576' \
  --entity-type clients \
  --entity-name producer-app

# Квота по умолчанию
kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter \
  --add-config 'producer_byte_rate=10485760,consumer_byte_rate=20971520' \
  --entity-type users \
  --entity-default
```

**Request quotas**
```bash
# Ограничение request rate
kafka-configs.sh --bootstrap-server localhost:9092 \
  --alter \
  --add-config 'request_percentage=50' \
  --entity-type users \
  --entity-name bob
```

### Troubleshooting

**Under-replicated partitions**
```bash
# Найти URP
kafka-topics.sh --describe --under-replicated-partitions --bootstrap-server localhost:9092

# Причины:
# - Медленный follower
# - Сетевые проблемы
# - Disk I/O проблемы
# - Недостаточно replica.fetch.max.bytes

# Решения:
# - Проверить broker метрики
# - Увеличить replica.lag.time.max.ms
# - Добавить capacity
```

**High consumer lag**
```bash
# Проверить lag
kafka-consumer-groups.sh --describe --group my-group --bootstrap-server localhost:9092

# Причины:
# - Медленная обработка
# - Недостаточно consumers
# - Большие сообщения

# Решения:
# - Масштабировать consumer group
# - Оптимизировать обработку
# - Увеличить max.poll.records
# - Уменьшить max.poll.interval.ms
```

**Disk full**
```bash
# Очистить старые логи
kafka-configs.sh --alter --add-config 'retention.ms=86400000' --topic large-topic --bootstrap-server localhost:9092

# Удалить неиспользуемые топики
kafka-topics.sh --delete --topic old-topic --bootstrap-server localhost:9092

# Добавить диски
# См. раздел "Управление дисками"
```

## Вопросы для самопроверки

1. **Как рассчитать требуемое дисковое пространство?**
   - throughput × retention × replication_factor × (1 + overhead)

2. **Сколько RAM нужно для Kafka broker?**
   - Минимум 32 GB, оптимально 64-128 GB, JVM heap не более 6 GB

3. **Как выполнить rolling upgrade кластера?**
   - По одному брокеру: stop → upgrade → start → проверить ISR → следующий

4. **Что такое preferred replica election?**
   - Восстановление равномерного распределения лидеров партиций по брокерам

5. **Как добавить новый брокер в кластер?**
   - Установить, запустить, выполнить partition reassignment

6. **Зачем нужен MirrorMaker?**
   - Репликация данных между кластерами для DR или multi-DC setup

7. **Какие JVM флаги важны для Kafka?**
   - G1GC, heap size 6GB max, GC logging, MaxGCPauseMillis

8. **Как ограничить throughput для пользователя?**
   - Через quotas: producer_byte_rate, consumer_byte_rate

9. **Что делать при under-replicated partitions?**
   - Проверить broker health, сеть, disk I/O, добавить capacity

10. **Как организовать multi-DC setup?**
    - MirrorMaker 2.0 для репликации, stretch cluster или active-passive
