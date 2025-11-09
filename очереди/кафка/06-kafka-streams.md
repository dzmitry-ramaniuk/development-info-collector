# Kafka Streams

## Введение

**Kafka Streams** — это клиентская библиотека для построения приложений обработки потоков данных в реальном времени, которая работает поверх Apache Kafka. В отличие от других фреймворков потоковой обработки (Spark Streaming, Flink), Kafka Streams представляет собой легковесную библиотеку, которая встраивается непосредственно в приложение без необходимости развертывания отдельного кластера обработки.

### Ключевые преимущества

- **Простота развертывания**: Обычное Java-приложение без отдельной инфраструктуры
- **Масштабируемость**: Горизонтальное масштабирование через партиции Kafka
- **Fault tolerance**: Автоматическое восстановление состояния при сбоях
- **Exactly-once semantics**: Гарантия ровно одной обработки сообщения
- **Stateful обработка**: Встроенная поддержка локального состояния с персистентностью
- **Интеграция с Kafka**: Нативная работа с топиками, партициями и consumer groups

## Основные концепции

### Stream vs Table

Kafka Streams вводит две ключевые абстракции:

**KStream (Event Stream)**
- Неограниченная последовательность событий
- Каждая запись — независимое событие
- Подходит для событий типа "произошло действие"
- Пример: клики пользователей, транзакции, логи

```java
KStream<String, Purchase> purchases = builder.stream("purchases-topic");
```

**KTable (Changelog Stream)**
- Представление таблицы как потока изменений
- Каждая запись обновляет состояние по ключу
- Подходит для моделирования изменяемых данных
- Пример: профили пользователей, остатки на счетах

```java
KTable<String, UserProfile> users = builder.table("users-topic");
```

**GlobalKTable**
- Полностью реплицированная таблица на каждом экземпляре приложения
- Используется для справочных данных (reference data)
- Не требует co-partitioning для join операций

### Топология обработки

Топология (Topology) описывает граф обработки данных:

```java
StreamsBuilder builder = new StreamsBuilder();

// Source: чтение из топика
KStream<String, Order> orders = builder.stream("orders");

// Processor: трансформация данных
KStream<String, EnrichedOrder> enriched = orders
    .filter((key, order) -> order.getAmount() > 100)
    .mapValues(order -> enrichOrder(order));

// Sink: запись в топик
enriched.to("enriched-orders");

Topology topology = builder.build();
```

## Операции обработки

### Stateless-операции

**filter / filterNot**
```java
KStream<String, Order> largeOrders = orders
    .filter((key, order) -> order.getAmount() > 1000);
```

**map / mapValues**
```java
KStream<String, OrderDTO> dtos = orders
    .mapValues(order -> new OrderDTO(order));
```

**flatMap / flatMapValues**
```java
KStream<String, String> words = sentences
    .flatMapValues(sentence -> Arrays.asList(sentence.split(" ")));
```

**branch**
```java
Map<String, KStream<String, Order>> branches = orders
    .split(Named.as("order-"))
    .branch((key, order) -> order.getStatus() == Status.PENDING, 
            Branched.as("pending"))
    .branch((key, order) -> order.getStatus() == Status.COMPLETED,
            Branched.as("completed"))
    .defaultBranch(Branched.as("other"));
```

**selectKey**
```java
KStream<String, Order> reKeyed = orders
    .selectKey((oldKey, order) -> order.getCustomerId());
```

### Stateful-операции

**groupBy / groupByKey**
```java
KGroupedStream<String, Order> grouped = orders
    .groupByKey(Grouped.with(Serdes.String(), orderSerde));
```

**aggregate**
```java
KTable<String, TotalAmount> totals = orders
    .groupByKey()
    .aggregate(
        () -> new TotalAmount(0.0),  // инициализатор
        (key, order, total) -> total.add(order.getAmount()),  // агрегатор
        Materialized.with(Serdes.String(), totalAmountSerde)
    );
```

**count**
```java
KTable<String, Long> counts = orders
    .groupByKey()
    .count(Materialized.as("orders-count-store"));
```

**reduce**
```java
KTable<String, Order> latest = orders
    .groupByKey()
    .reduce((oldOrder, newOrder) -> newOrder);
```

### Windowing (оконные операции)

**Tumbling Window (Фиксированные окна)**
```java
TimeWindowedKStream<String, Order> windowed = orders
    .groupByKey()
    .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(5)));

KTable<Windowed<String>, Long> counts = windowed.count();
```

**Hopping Window (Скользящие окна)**
```java
TimeWindowedKStream<String, Order> hopping = orders
    .groupByKey()
    .windowedBy(
        TimeWindows
            .ofSizeWithNoGrace(Duration.ofMinutes(10))
            .advanceBy(Duration.ofMinutes(5))
    );
```

**Sliding Window (Окна скольжения)**
```java
TimeWindowedKStream<String, Click> sliding = clicks
    .groupByKey()
    .windowedBy(
        SlidingWindows
            .ofTimeDifferenceWithNoGrace(Duration.ofMinutes(10))
    );
```

**Session Window (Окна сессий)**
```java
SessionWindowedKStream<String, Event> sessions = events
    .groupByKey()
    .windowedBy(
        SessionWindows.ofInactivityGapWithNoGrace(Duration.ofMinutes(30))
    );
```

### Joins (операции соединения)

**KStream-KStream Join**
```java
// Inner join с временным окном
KStream<String, JoinedData> joined = leftStream.join(
    rightStream,
    (leftValue, rightValue) -> new JoinedData(leftValue, rightValue),
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5)),
    StreamJoined.with(Serdes.String(), leftSerde, rightSerde)
);

// Left join
KStream<String, JoinedData> leftJoined = leftStream.leftJoin(
    rightStream,
    (leftValue, rightValue) -> new JoinedData(leftValue, rightValue),
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5))
);

// Outer join
KStream<String, JoinedData> outerJoined = leftStream.outerJoin(
    rightStream,
    (leftValue, rightValue) -> new JoinedData(leftValue, rightValue),
    JoinWindows.ofTimeDifferenceWithNoGrace(Duration.ofMinutes(5))
);
```

**KStream-KTable Join**
```java
// Обогащение потока данными из таблицы
KStream<String, EnrichedOrder> enriched = orders.join(
    customers,  // KTable
    (order, customer) -> enrichOrder(order, customer)
);

// Left join (сохраняет записи из stream, даже если нет соответствия в table)
KStream<String, EnrichedOrder> leftJoined = orders.leftJoin(
    customers,
    (order, customer) -> enrichOrder(order, customer)
);
```

**KTable-KTable Join**
```java
// Inner join двух таблиц
KTable<String, JoinedProfile> joined = usersTable.join(
    settingsTable,
    (user, settings) -> new JoinedProfile(user, settings)
);

// Left join
KTable<String, JoinedProfile> leftJoined = usersTable.leftJoin(
    settingsTable,
    (user, settings) -> new JoinedProfile(user, settings)
);

// Outer join
KTable<String, JoinedProfile> outerJoined = usersTable.outerJoin(
    settingsTable,
    (user, settings) -> new JoinedProfile(user, settings)
);
```

**KStream-GlobalKTable Join**
```java
// Не требует co-partitioning, использует произвольный ключ
KStream<String, EnrichedOrder> enriched = orders.join(
    globalTable,
    (orderKey, order) -> order.getProductId(),  // KeyValueMapper
    (order, product) -> enrichOrder(order, product)
);
```

## State Stores (хранилища состояния)

### Типы State Stores

**In-Memory Store**
```java
StoreBuilder<KeyValueStore<String, Long>> storeBuilder = 
    Stores.keyValueStoreBuilder(
        Stores.inMemoryKeyValueStore("counts-store"),
        Serdes.String(),
        Serdes.Long()
    );
```

**Persistent Store (RocksDB)**
```java
StoreBuilder<KeyValueStore<String, Long>> storeBuilder = 
    Stores.keyValueStoreBuilder(
        Stores.persistentKeyValueStore("counts-store"),
        Serdes.String(),
        Serdes.Long()
    ).withCachingEnabled();
```

**Window Store**
```java
StoreBuilder<WindowStore<String, Long>> windowStoreBuilder = 
    Stores.windowStoreBuilder(
        Stores.persistentWindowStore(
            "windowed-counts",
            Duration.ofDays(1),
            Duration.ofMinutes(5),
            false
        ),
        Serdes.String(),
        Serdes.Long()
    );
```

**Session Store**
```java
StoreBuilder<SessionStore<String, Long>> sessionStoreBuilder = 
    Stores.sessionStoreBuilder(
        Stores.persistentSessionStore(
            "sessions",
            Duration.ofMinutes(30)
        ),
        Serdes.String(),
        Serdes.Long()
    );
```

### Интерактивные запросы (Interactive Queries)

Чтение состояния из работающего приложения:

```java
// Получение store
ReadOnlyKeyValueStore<String, Long> store = 
    streams.store(
        StoreQueryParameters.fromNameAndType(
            "counts-store",
            QueryableStoreTypes.keyValueStore()
        )
    );

// Чтение по ключу
Long count = store.get("user-123");

// Итерация по всем ключам
KeyValueIterator<String, Long> all = store.all();
while (all.hasNext()) {
    KeyValue<String, Long> entry = all.next();
    System.out.println(entry.key + ": " + entry.value);
}

// Диапазонный запрос
KeyValueIterator<String, Long> range = store.range("a", "m");
```

## Обработка времени

### Event Time vs Processing Time

**Event Time** — время, когда событие произошло (содержится в данных)
**Processing Time** — время, когда событие обрабатывается системой

```java
// Использование event time через TimestampExtractor
StreamsConfig config = new StreamsConfig(props);
config.put(
    StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG,
    WallclockTimestampExtractor.class  // или EventTimeExtractor
);
```

**Пользовательский TimestampExtractor**
```java
public class OrderTimestampExtractor implements TimestampExtractor {
    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        Order order = (Order) record.value();
        return order.getCreatedAt().toEpochMilli();
    }
}
```

### Grace Period

Период ожидания поздних событий:

```java
TimeWindows windows = TimeWindows
    .ofSizeAndGrace(
        Duration.ofMinutes(5),      // размер окна
        Duration.ofSeconds(30)       // grace period
    );
```

## Конфигурация и оптимизация

### Основные параметры

```java
Properties props = new Properties();

// Обязательные параметры
props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-processor");
props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

// Сериализация по умолчанию
props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, 
    Serdes.String().getClass());
props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, 
    Serdes.String().getClass());

// Оптимизация производительности
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 4);
props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 10 * 1024 * 1024L);
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1000);

// Обработка ошибок
props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
    LogAndContinueExceptionHandler.class);

// Exactly-once семантика
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, 
    StreamsConfig.EXACTLY_ONCE_V2);

// State directory
props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
```

### Оптимизация производительности

**Настройка кэширования**
```java
Materialized.<String, Long, KeyValueStore<Bytes, byte[]>>as("store-name")
    .withCachingEnabled()
    .withLoggingDisabled();  // отключить changelog topic
```

**Batch processing**
```java
// Увеличить размер batch для уменьшения накладных расходов
props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 100 * 1024 * 1024L);
props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 5000);
```

**Параллелизм**
```java
// Увеличить количество потоков обработки
props.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG, 
    Runtime.getRuntime().availableProcessors());
```

## Обработка ошибок и восстановление

### Exception Handlers

**Deserialization Exception Handler**
```java
public class CustomDeserializationHandler 
    implements DeserializationExceptionHandler {
    
    @Override
    public DeserializationHandlerResponse handle(
        ProcessorContext context,
        ConsumerRecord<byte[], byte[]> record,
        Exception exception) {
        
        log.error("Deserialization error", exception);
        
        // Продолжить обработку или остановить
        return DeserializationHandlerResponse.CONTINUE;
        // return DeserializationHandlerResponse.FAIL;
    }
}

props.put(
    StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG,
    CustomDeserializationHandler.class
);
```

**Production Exception Handler**
```java
public class CustomProductionHandler implements ProductionExceptionHandler {
    @Override
    public ProductionExceptionHandlerResponse handle(
        ProducerRecord<byte[], byte[]> record,
        Exception exception) {
        
        log.error("Production error", exception);
        
        return ProductionExceptionHandlerResponse.CONTINUE;
    }
}

props.put(
    StreamsConfig.DEFAULT_PRODUCTION_EXCEPTION_HANDLER_CLASS_CONFIG,
    CustomProductionHandler.class
);
```

### Uncaught Exception Handler

```java
KafkaStreams streams = new KafkaStreams(topology, props);

streams.setUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
    log.error("Uncaught exception in thread " + thread.getName(), throwable);
    
    // Заменить поток или остановить приложение
    return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
});
```

### State Store восстановление

При сбое приложения Kafka Streams автоматически восстанавливает состояние из changelog topics:

```java
// Мониторинг восстановления
streams.setStateListener((newState, oldState) -> {
    log.info("State changed from {} to {}", oldState, newState);
    
    if (newState == KafkaStreams.State.REBALANCING) {
        log.warn("Rebalancing in progress...");
    }
});
```

## Тестирование

### TopologyTestDriver

```java
@Test
public void testOrderProcessing() {
    // Создание топологии
    StreamsBuilder builder = new StreamsBuilder();
    KStream<String, Order> orders = builder.stream("orders");
    orders
        .filter((key, order) -> order.getAmount() > 100)
        .to("large-orders");
    
    Topology topology = builder.build();
    
    // Создание test driver
    Properties props = new Properties();
    props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
    props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
    
    try (TopologyTestDriver testDriver = new TopologyTestDriver(topology, props)) {
        // Входной топик
        TestInputTopic<String, Order> inputTopic = testDriver.createInputTopic(
            "orders",
            Serdes.String().serializer(),
            orderSerde.serializer()
        );
        
        // Выходной топик
        TestOutputTopic<String, Order> outputTopic = testDriver.createOutputTopic(
            "large-orders",
            Serdes.String().deserializer(),
            orderSerde.deserializer()
        );
        
        // Отправка тестовых данных
        Order smallOrder = new Order("1", 50.0);
        Order largeOrder = new Order("2", 150.0);
        
        inputTopic.pipeInput("1", smallOrder);
        inputTopic.pipeInput("2", largeOrder);
        
        // Проверка результатов
        assertThat(outputTopic.readKeyValue()).isEqualTo(new KeyValue<>("2", largeOrder));
        assertThat(outputTopic.isEmpty()).isTrue();
    }
}
```

### Тестирование с State Stores

```java
@Test
public void testAggregation() {
    StreamsBuilder builder = new StreamsBuilder();
    
    builder.stream("orders", Consumed.with(Serdes.String(), orderSerde))
        .groupByKey()
        .aggregate(
            () -> 0.0,
            (key, order, total) -> total + order.getAmount(),
            Materialized.<String, Double, KeyValueStore<Bytes, byte[]>>as("totals")
                .withKeySerde(Serdes.String())
                .withValueSerde(Serdes.Double())
        );
    
    try (TopologyTestDriver testDriver = new TopologyTestDriver(builder.build(), props)) {
        TestInputTopic<String, Order> inputTopic = testDriver.createInputTopic(
            "orders",
            Serdes.String().serializer(),
            orderSerde.serializer()
        );
        
        // Отправка данных
        inputTopic.pipeInput("customer-1", new Order("1", 100.0));
        inputTopic.pipeInput("customer-1", new Order("2", 50.0));
        
        // Проверка state store
        KeyValueStore<String, Double> store = testDriver.getKeyValueStore("totals");
        assertThat(store.get("customer-1")).isEqualTo(150.0);
    }
}
```

## Мониторинг и метрики

### JMX Метрики

Kafka Streams экспортирует множество метрик через JMX:

**Stream Metrics**
- `process-rate` — скорость обработки записей
- `process-latency-avg` — средняя задержка обработки
- `commit-rate` — частота коммитов
- `poll-rate` — частота poll операций

**State Store Metrics**
- `put-rate` — скорость записи в store
- `get-rate` — скорость чтения из store
- `flush-rate` — частота flush операций
- `restore-rate` — скорость восстановления состояния

**Task Metrics**
- `active-task-count` — количество активных задач
- `standby-task-count` — количество standby задач

### Логирование

```java
// Включить подробное логирование
props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG");

// Кастомный reporter для метрик
List<MetricsReporter> reporters = new ArrayList<>();
reporters.add(new PrometheusMetricsReporter());
props.put(StreamsConfig.METRIC_REPORTER_CLASSES_CONFIG, reporters);
```

## Паттерны и best practices

### 1. Co-partitioning для joins

Для KStream-KStream и KStream-KTable joins необходимо обеспечить co-partitioning:

```java
// Одинаковое количество партиций
// Одинаковая логика партиционирования
// Одинаковые ключи

// Если партиционирование разное, нужен repartition
KStream<String, Order> rekeyed = orders
    .selectKey((key, order) -> order.getCustomerId())
    .repartition(Repartitioned.numberOfPartitions(10));
```

### 2. Обработка поздних событий

```java
// Настройка grace period и retention
TimeWindows windows = TimeWindows
    .ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1))
    .until(Duration.ofHours(24));  // retention период
```

### 3. Идемпотентность обработки

```java
// Использование exactly-once семантики
props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, 
    StreamsConfig.EXACTLY_ONCE_V2);

// Транзакционный ID генерируется автоматически на основе application.id
```

### 4. Масштабирование приложения

```java
// Количество экземпляров <= количество партиций входных топиков
// Каждый экземпляр обрабатывает subset партиций
// Используйте один application.id для всех экземпляров

// Graceful shutdown
Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
```

### 5. Управление памятью

```java
// Ограничение памяти для кэша
props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 100 * 1024 * 1024L);

// RocksDB тюнинг
RocksDBConfigSetter configSetter = (storeName, options, configs) -> {
    options.setMaxWriteBufferNumber(3);
    options.setWriteBufferSize(16 * 1024 * 1024L);
};

props.put(StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, configSetter);
```

## Практический пример: Real-time Analytics

```java
public class OrderAnalyticsApp {
    
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-analytics");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        
        StreamsBuilder builder = new StreamsBuilder();
        
        // 1. Чтение потока заказов
        KStream<String, Order> orders = builder.stream(
            "orders",
            Consumed.with(Serdes.String(), new JsonSerde<>(Order.class))
        );
        
        // 2. Фильтрация и обогащение
        KTable<String, Customer> customers = builder.table(
            "customers",
            Consumed.with(Serdes.String(), new JsonSerde<>(Customer.class))
        );
        
        KStream<String, EnrichedOrder> enrichedOrders = orders
            .selectKey((key, order) -> order.getCustomerId())
            .join(
                customers,
                (order, customer) -> new EnrichedOrder(order, customer)
            );
        
        // 3. Агрегация по временным окнам
        KTable<Windowed<String>, OrderStats> hourlyStats = enrichedOrders
            .groupBy((key, order) -> order.getCategory())
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofHours(1)))
            .aggregate(
                OrderStats::new,
                (key, order, stats) -> stats.add(order),
                Materialized.<String, OrderStats, WindowStore<Bytes, byte[]>>as("hourly-stats")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(new JsonSerde<>(OrderStats.class))
            );
        
        // 4. Вывод результатов
        hourlyStats.toStream()
            .map((windowedKey, stats) -> KeyValue.pair(
                windowedKey.key() + "@" + windowedKey.window().start(),
                stats
            ))
            .to("order-stats", Produced.with(Serdes.String(), new JsonSerde<>(OrderStats.class)));
        
        // 5. Обнаружение аномалий
        enrichedOrders
            .filter((key, order) -> order.getAmount() > 10000)
            .to("high-value-orders");
        
        // 6. Запуск приложения
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        
        streams.setUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception: " + throwable);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
        
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        
        streams.start();
    }
}
```

## Вопросы для самопроверки

1. **В чем разница между KStream и KTable?**
   - KStream — неограниченный поток событий, KTable — changelog stream с последним значением по ключу

2. **Что такое co-partitioning и когда он необходим?**
   - Требование одинакового партиционирования для join операций между KStream-KStream и KStream-KTable

3. **Как обеспечить exactly-once семантику?**
   - Установить `processing.guarantee=exactly_once_v2`, включить idempotent producer

4. **Что происходит при сбое экземпляра Kafka Streams?**
   - Rebalancing, переназначение партиций, восстановление state stores из changelog topics

5. **Какие типы окон поддерживает Kafka Streams?**
   - Tumbling (фиксированные), Hopping (скользящие), Sliding (окна скольжения), Session (сессионные)

6. **В чем разница между State Store и KTable?**
   - State Store — локальное хранилище для stateful операций, KTable — абстракция над ним с дополнительной логикой

7. **Как масштабировать Kafka Streams приложение?**
   - Запустить несколько экземпляров с одним application.id, количество <= количества партиций

8. **Что такое Interactive Queries?**
   - Возможность читать state stores напрямую из работающего приложения для ad-hoc запросов

9. **Как обрабатывать поздние события?**
   - Настроить grace period для окон, использовать event time с timestamp extractor

10. **В чем преимущество GlobalKTable?**
    - Полная репликация на каждый экземпляр, не требует co-partitioning для joins

## Дополнительные ресурсы

- [Kafka Streams Documentation](https://kafka.apache.org/documentation/streams/)
- [Confluent Kafka Streams Examples](https://github.com/confluentinc/kafka-streams-examples)
- [Designing Event-Driven Systems (книга)](https://www.confluent.io/designing-event-driven-systems/)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
