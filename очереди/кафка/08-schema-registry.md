# Schema Registry и управление схемами

## Введение

**Confluent Schema Registry** — это сервис для централизованного управления и хранения схем данных для Apache Kafka. Он обеспечивает слой управления схемами для Avro, Protobuf и JSON Schema, позволяя производителям и потребителям обмениваться данными с гарантией совместимости.

### Зачем нужен Schema Registry

- **Централизация**: Единое хранилище схем для всей организации
- **Совместимость**: Контроль эволюции схем с проверкой совместимости
- **Эффективность**: Хранение только ID схемы в сообщении вместо полной схемы
- **Безопасность типов**: Валидация данных при сериализации/десериализации
- **Документация**: Схемы служат живой документацией формата данных

### Основные возможности

- Хранение версий схем для каждого subject
- Проверка совместимости при регистрации новых схем
- RESTful API для управления схемами
- Поддержка нескольких форматов: Avro, Protobuf, JSON Schema
- Режимы совместимости: backward, forward, full, none
- Интеграция с Kafka producers/consumers через сериализаторы

## Архитектура

### Компоненты

```
┌─────────────────┐
│   Producer      │
│  (с Avro        │
│   Serializer)   │
└────────┬────────┘
         │ 1. Получить/зарегистрировать схему
         │ 2. Получить schema ID
         ▼
┌─────────────────────┐
│ Schema Registry     │
│ - Хранит схемы      │
│ - Проверяет         │
│   совместимость     │
└─────────────────────┘
         │ 3. Отправить [ID схемы + данные]
         ▼
┌─────────────────┐
│   Kafka Broker  │
└────────┬────────┘
         │ 4. Читать сообщение
         ▼
┌─────────────────┐
│   Consumer      │
│  (с Avro        │
│   Deserializer) │
└────────┬────────┘
         │ 5. Получить схему по ID
         ▼
┌─────────────────────┐
│ Schema Registry     │
└─────────────────────┘
```

### Subject и версии

**Subject** — это scope, в котором хранятся версии схем. Обычно subject = topic name + "-key" или "-value".

Пример:
- `orders-value`: схемы для значений в топике orders
- `orders-key`: схемы для ключей в топике orders

Каждая регистрация новой схемы создаёт новую версию в рамках subject.

## Форматы схем

### Apache Avro

**Преимущества**
- Компактная бинарная сериализация
- Встроенная поддержка эволюции схем
- Схема описывается в JSON

**Пример схемы**
```json
{
  "type": "record",
  "name": "Order",
  "namespace": "com.example.orders",
  "fields": [
    {
      "name": "orderId",
      "type": "string"
    },
    {
      "name": "customerId",
      "type": "string"
    },
    {
      "name": "amount",
      "type": "double"
    },
    {
      "name": "status",
      "type": {
        "type": "enum",
        "name": "OrderStatus",
        "symbols": ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"]
      }
    },
    {
      "name": "createdAt",
      "type": "long",
      "logicalType": "timestamp-millis"
    }
  ]
}
```

**Пример использования в Java**
```java
// Producer
Properties props = new Properties();
props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
    StringSerializer.class);
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
    KafkaAvroSerializer.class);
props.put("schema.registry.url", "http://localhost:8081");

KafkaProducer<String, Order> producer = new KafkaProducer<>(props);

Order order = Order.newBuilder()
    .setOrderId("ORDER-123")
    .setCustomerId("CUST-456")
    .setAmount(99.99)
    .setStatus(OrderStatus.PENDING)
    .setCreatedAt(System.currentTimeMillis())
    .build();

producer.send(new ProducerRecord<>("orders", order.getOrderId(), order));

// Consumer
Properties consumerProps = new Properties();
consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "order-consumers");
consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
    StringDeserializer.class);
consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
    KafkaAvroDeserializer.class);
consumerProps.put("schema.registry.url", "http://localhost:8081");
consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);

KafkaConsumer<String, Order> consumer = new KafkaConsumer<>(consumerProps);
consumer.subscribe(Collections.singletonList("orders"));
```

### Protocol Buffers (Protobuf)

**Преимущества**
- Очень компактная сериализация
- Высокая производительность
- Кроссплатформенность
- Широкая поддержка языков

**Пример схемы**
```protobuf
syntax = "proto3";

package com.example.orders;

message Order {
  string order_id = 1;
  string customer_id = 2;
  double amount = 3;
  OrderStatus status = 4;
  int64 created_at = 5;
  
  enum OrderStatus {
    PENDING = 0;
    CONFIRMED = 1;
    SHIPPED = 2;
    DELIVERED = 3;
  }
}
```

**Использование**
```java
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
    KafkaProtobufSerializer.class);
props.put("schema.registry.url", "http://localhost:8081");

// Deserialization
consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
    KafkaProtobufDeserializer.class);
consumerProps.put(KafkaProtobufDeserializerConfig.SPECIFIC_PROTOBUF_VALUE_TYPE, 
    Order.class);
```

### JSON Schema

**Преимущества**
- Читаемый формат
- Легко отлаживать
- Широкая поддержка инструментов

**Пример схемы**
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Order",
  "type": "object",
  "properties": {
    "orderId": {
      "type": "string"
    },
    "customerId": {
      "type": "string"
    },
    "amount": {
      "type": "number"
    },
    "status": {
      "type": "string",
      "enum": ["PENDING", "CONFIRMED", "SHIPPED", "DELIVERED"]
    },
    "createdAt": {
      "type": "integer",
      "format": "int64"
    }
  },
  "required": ["orderId", "customerId", "amount", "status", "createdAt"]
}
```

**Использование**
```java
props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
    KafkaJsonSchemaSerializer.class);
props.put("schema.registry.url", "http://localhost:8081");
```

## Режимы совместимости

### Backward Compatibility (по умолчанию)

Новая схема может читать данные, написанные старой схемой.

**Правила**
- ✅ Можно добавлять необязательные поля (с default значениями)
- ✅ Можно удалять необязательные поля
- ❌ Нельзя удалять обязательные поля
- ❌ Нельзя менять тип поля

**Пример**
```json
// Старая схема
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"}
  ]
}

// Новая схема (backward compatible)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"},
    {"name": "email", "type": ["null", "string"], "default": null}
  ]
}
```

**Use case**: Обновляем consumers первыми, потом producers

### Forward Compatibility

Старая схема может читать данные, написанные новой схемой.

**Правила**
- ✅ Можно удалять поля
- ✅ Можно добавлять обязательные поля (с default)
- ❌ Нельзя добавлять поля без default

**Пример**
```json
// Старая схема
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"},
    {"name": "temp_field", "type": ["null", "string"], "default": null}
  ]
}

// Новая схема (forward compatible)
{
  "type": "record",
  "name": "User",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "name", "type": "string"}
  ]
}
```

**Use case**: Обновляем producers первыми, потом consumers

### Full Compatibility

Комбинация backward и forward — новая и старая схемы совместимы в обе стороны.

**Правила**
- ✅ Можно добавлять необязательные поля с default
- ✅ Можно удалять необязательные поля с default
- ❌ Строгие ограничения на изменения

**Use case**: Можно обновлять producers и consumers в любом порядке

### None

Совместимость не проверяется. Любые изменения разрешены.

**Use case**: Разработка, когда схема часто меняется

### Transitive совместимость

- **BACKWARD_TRANSITIVE**: Новая схема совместима со всеми предыдущими версиями
- **FORWARD_TRANSITIVE**: Все старые схемы совместимы с новой
- **FULL_TRANSITIVE**: Полная совместимость со всеми версиями

## REST API

### Управление схемами

**Регистрация схемы**
```bash
curl -X POST http://localhost:8081/subjects/orders-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Order\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}"
  }'
```

**Получить последнюю версию схемы**
```bash
curl http://localhost:8081/subjects/orders-value/versions/latest
```

**Получить конкретную версию**
```bash
curl http://localhost:8081/subjects/orders-value/versions/1
```

**Получить схему по ID**
```bash
curl http://localhost:8081/schemas/ids/1
```

**Список всех subjects**
```bash
curl http://localhost:8081/subjects
```

**Список версий subject**
```bash
curl http://localhost:8081/subjects/orders-value/versions
```

**Удалить версию схемы**
```bash
curl -X DELETE http://localhost:8081/subjects/orders-value/versions/1
```

**Удалить subject полностью**
```bash
# Soft delete
curl -X DELETE http://localhost:8081/subjects/orders-value

# Hard delete (окончательное удаление)
curl -X DELETE http://localhost:8081/subjects/orders-value?permanent=true
```

### Управление совместимостью

**Получить режим совместимости**
```bash
# Глобальный
curl http://localhost:8081/config

# Для конкретного subject
curl http://localhost:8081/config/orders-value
```

**Установить режим совместимости**
```bash
# Глобальный
curl -X PUT http://localhost:8081/config \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "FULL"}'

# Для subject
curl -X PUT http://localhost:8081/config/orders-value \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"compatibility": "BACKWARD"}'
```

**Проверить совместимость**
```bash
curl -X POST http://localhost:8081/compatibility/subjects/orders-value/versions/latest \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{
    "schema": "{\"type\":\"record\",\"name\":\"Order\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}"
  }'
```

## Эволюция схем

### Добавление полей

**Avro**
```json
// Версия 1
{
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "amount", "type": "double"}
  ]
}

// Версия 2 - добавляем поле с default
{
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "currency", "type": "string", "default": "USD"}
  ]
}
```

### Удаление полей

```json
// Версия 1
{
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "amount", "type": "double"},
    {"name": "legacy_field", "type": ["null", "string"], "default": null}
  ]
}

// Версия 2 - удаляем поле
{
  "type": "record",
  "name": "Order",
  "fields": [
    {"name": "id", "type": "string"},
    {"name": "amount", "type": "double"}
  ]
}
```

### Изменение типов

**Avro Schema Resolution Rules**
- `int` → `long`, `float`, `double`
- `long` → `float`, `double`
- `float` → `double`
- `string` → `bytes`
- `bytes` → `string`

```json
// Версия 1
{
  "name": "count",
  "type": "int"
}

// Версия 2 - расширяем тип
{
  "name": "count",
  "type": "long"
}
```

### Union типы

```json
// Делаем поле nullable
{
  "name": "email",
  "type": ["null", "string"],
  "default": null
}

// Множественные типы
{
  "name": "value",
  "type": ["int", "string", "double"]
}
```

## Конфигурация Schema Registry

### Основные параметры

```properties
# schema-registry.properties

# Kafka cluster connection
kafkastore.bootstrap.servers=localhost:9092

# Topic для хранения схем
kafkastore.topic=_schemas
kafkastore.topic.replication.factor=3

# Listeners
listeners=http://0.0.0.0:8081

# Режим совместимости по умолчанию
schema.compatibility.level=BACKWARD

# Security
authentication.method=BASIC
authentication.realm=SchemaRegistry
authentication.roles=admin,developer

# SSL
ssl.keystore.location=/var/private/ssl/kafka.server.keystore.jks
ssl.keystore.password=test1234
ssl.key.password=test1234
```

### High Availability

Для production развертывайте несколько экземпляров Schema Registry за load balancer:

```
┌──────────────┐
│ Load Balancer│
└──────┬───────┘
       │
   ────┼─────────────
   │   │            │
┌──▼───▼──┐   ┌────▼────┐
│Schema   │   │Schema   │
│Registry │   │Registry │
│Instance1│   │Instance2│
└────┬────┘   └────┬────┘
     │             │
     └──────┬──────┘
            │
     ┌──────▼──────┐
     │Kafka Cluster│
     │(_schemas    │
     │   topic)    │
     └─────────────┘
```

## Best Practices

### 1. Именование subject

Используйте стратегии именования:

**TopicNameStrategy (default)**
```
<topic>-key
<topic>-value
```

**RecordNameStrategy**
```
<fully-qualified-record-name>
```

**TopicRecordNameStrategy**
```
<topic>-<fully-qualified-record-name>
```

Конфигурация:
```java
props.put("value.subject.name.strategy", 
    "io.confluent.kafka.serializers.subject.TopicRecordNameStrategy");
```

### 2. Использование namespaces

```json
{
  "type": "record",
  "name": "Order",
  "namespace": "com.mycompany.orders.v1",
  "fields": [...]
}
```

### 3. Документирование схем

```json
{
  "type": "record",
  "name": "Order",
  "doc": "Represents a customer order in the e-commerce system",
  "fields": [
    {
      "name": "orderId",
      "type": "string",
      "doc": "Unique identifier for the order"
    },
    {
      "name": "amount",
      "type": "double",
      "doc": "Total order amount in USD"
    }
  ]
}
```

### 4. Версионирование

Включайте версию в namespace или имя:
```json
{
  "namespace": "com.mycompany.orders.v2",
  "name": "Order"
}
```

### 5. Default значения

Всегда указывайте default для nullable полей:
```json
{
  "name": "email",
  "type": ["null", "string"],
  "default": null
}
```

### 6. Логические типы

Используйте logicalType для специальных типов:
```json
{
  "name": "createdAt",
  "type": "long",
  "logicalType": "timestamp-millis"
},
{
  "name": "price",
  "type": "bytes",
  "logicalType": "decimal",
  "precision": 10,
  "scale": 2
}
```

## Мониторинг

### JMX Метрики

- `kafka.schema.registry:type=jetty-metrics` - HTTP метрики
- `kafka.schema.registry:type=master-slave-role` - Роль в кластере
- `kafka.schema.registry:type=registered-count` - Количество зарегистрированных схем

### Health Check

```bash
curl http://localhost:8081/
# Должен вернуть: {}
```

### Prometheus интеграция

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'schema-registry'
    static_configs:
      - targets: ['schema-registry:8081']
    metrics_path: '/metrics'
```

## Тестирование

### Unit тесты со схемами

```java
@Test
public void testSchemaCompatibility() {
    String schemaString1 = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"}]}";
    String schemaString2 = "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"id\",\"type\":\"string\"},{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}";
    
    Schema schema1 = new Schema.Parser().parse(schemaString1);
    Schema schema2 = new Schema.Parser().parse(schemaString2);
    
    SchemaCompatibility.SchemaPairCompatibility compatibility = 
        SchemaCompatibility.checkReaderWriterCompatibility(schema2, schema1);
    
    assertEquals(SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, 
                 compatibility.getType());
}
```

### Mock Schema Registry для тестов

```java
@Test
public void testWithMockSchemaRegistry() {
    MockSchemaRegistry mockRegistry = new MockSchemaRegistry();
    
    KafkaAvroSerializer serializer = new KafkaAvroSerializer(mockRegistry);
    KafkaAvroDeserializer deserializer = new KafkaAvroDeserializer(mockRegistry);
    
    Order order = createTestOrder();
    byte[] bytes = serializer.serialize("orders", order);
    Order deserialized = (Order) deserializer.deserialize("orders", bytes);
    
    assertEquals(order, deserialized);
}
```

## Вопросы для самопроверки

1. **Зачем нужен Schema Registry?**
   - Централизованное управление схемами, проверка совместимости, эффективная сериализация

2. **Что такое subject в Schema Registry?**
   - Scope для хранения версий схем, обычно topic-key или topic-value

3. **В чем разница между BACKWARD и FORWARD совместимостью?**
   - BACKWARD: новая схема читает старые данные; FORWARD: старая схема читает новые данные

4. **Какой формат схем выбрать: Avro, Protobuf или JSON Schema?**
   - Avro: компактность, Kafka ecosystem; Protobuf: производительность, cross-platform; JSON: простота

5. **Как обновить схему без downtime?**
   - Использовать backward/forward совместимость, обновлять consumers/producers поэтапно

6. **Что хранится в _schemas топике?**
   - Все зарегистрированные схемы и их метаданные

7. **Можно ли удалить старую версию схемы?**
   - Да, через soft delete (можно восстановить) или permanent delete

8. **Как проверить совместимость перед регистрацией схемы?**
   - REST API endpoint /compatibility/subjects/.../versions/...

9. **Что делать если схема несовместима?**
   - Изменить схему, использовать новый subject, или изменить режим совместимости

10. **Как обеспечить HA для Schema Registry?**
    - Несколько экземпляров за load balancer, все читают из одного _schemas топика
