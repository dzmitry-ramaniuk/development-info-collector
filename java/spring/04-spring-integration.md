# Spring Integration

## Введение

Spring Integration — это фреймворк для построения enterprise-интеграционных решений на основе паттернов из книги "Enterprise Integration Patterns" (EIP) Грегора Хоупа и Бобби Вулфа. Он предоставляет легковесный messaging-подход для связывания различных компонентов и внешних систем в единое приложение.

**Основные задачи Spring Integration:**
- Интеграция с внешними системами (файлы, базы данных, очереди сообщений, REST API, FTP, Email и др.)
- Организация асинхронной обработки данных через message-driven архитектуру
- Маршрутизация, трансформация и обогащение сообщений
- Реализация паттернов интеграции без написания низкоуровневого кода

**Преимущества:**
- Декларативный подход — большинство интеграционных паттернов настраиваются через конфигурацию
- Слабая связанность — компоненты общаются через сообщения, не зная друг о друге
- Тестируемость — легко подменять компоненты и имитировать внешние системы
- Расширяемость — простое добавление собственных адаптеров и компонентов
- Мониторинг — встроенная интеграция с Micrometer для метрик

**Историческая справка.** Spring Integration появился в 2007 году как реализация паттернов EIP для Spring Framework. Изначально конфигурация была только через XML, с версии 5.0 появился удобный Java DSL, значительно упростивший создание интеграционных потоков. С Spring Integration 6.0 (Spring Framework 6) добавлена поддержка виртуальных потоков и улучшена интеграция с реактивными типами.

## Основные концепции

### Message (Сообщение)

**Message** — основная единица данных в Spring Integration. Сообщение состоит из двух частей:

1. **Payload (полезная нагрузка)** — данные любого типа (String, byte[], POJO, коллекция и т.д.)
2. **Headers (заголовки)** — метаданные в формате Map<String, Object> (ID сообщения, timestamp, correlation ID, routing information и др.)

```java
Message<String> message = MessageBuilder
    .withPayload("Hello, Spring Integration!")
    .setHeader("userId", 12345)
    .setHeader("priority", "HIGH")
    .build();

String payload = message.getPayload();
Integer userId = message.getHeaders().get("userId", Integer.class);
```

**Стандартные заголовки:**
- `MessageHeaders.ID` — уникальный UUID сообщения
- `MessageHeaders.TIMESTAMP` — время создания сообщения (long)
- `MessageHeaders.REPLY_CHANNEL` — канал для ответа
- `MessageHeaders.ERROR_CHANNEL` — канал для обработки ошибок
- Custom headers — любые пользовательские метаданные

> **Важно**: Сообщения в Spring Integration иммутабельны. Для изменения используйте `MessageBuilder.fromMessage(original)`.

### Message Channel (Канал сообщений)

**Message Channel** — компонент для передачи сообщений между отправителем и получателем. Каналы обеспечивают слабую связанность: отправитель не знает, кто получит сообщение, и наоборот.

**Основные типы каналов:**

#### 1. DirectChannel (по умолчанию)

Синхронный канал типа point-to-point. Сообщение обрабатывается в потоке отправителя. Если подписчиков несколько, используется round-robin балансировка.

```java
@Bean
public DirectChannel orderChannel() {
    return new DirectChannel();
}
```

**Характеристики:**
- Выполнение в потоке вызывающего
- Поддержка транзакций
- Простая отладка (stack trace содержит весь путь обработки)
- Не подходит для долгих операций (блокирует вызывающий поток)

#### 2. PublishSubscribeChannel

Широковещательный канал — все подписчики получают копию сообщения.

```java
@Bean
public PublishSubscribeChannel notificationChannel() {
    return new PublishSubscribeChannel();
}
```

**Применение:**
- Параллельная обработка сообщения несколькими подписчиками
- Рассылка уведомлений
- Event broadcasting

#### 3. QueueChannel

Асинхронный канал с буфером (очередь). Сообщения помещаются в очередь, обработка происходит в отдельном потоке.

```java
@Bean
public QueueChannel asyncChannel() {
    return new QueueChannel(100); // ёмкость очереди
}
```

**Характеристики:**
- Асинхронная обработка
- Буферизация сообщений
- Требуется poller для чтения из канала
- Возможность ограничить размер очереди

#### 4. PriorityChannel

Канал с приоритизацией сообщений. Сообщения с высоким приоритетом обрабатываются первыми.

```java
@Bean
public PriorityChannel priorityChannel() {
    return new PriorityChannel(100, (msg1, msg2) -> {
        Integer p1 = msg1.getHeaders().get("priority", Integer.class);
        Integer p2 = msg2.getHeaders().get("priority", Integer.class);
        return Integer.compare(p2, p1); // больший приоритет первым
    });
}
```

#### 5. ExecutorChannel

Асинхронный канал, делегирующий выполнение TaskExecutor. Каждое сообщение обрабатывается в отдельном потоке из пула.

```java
@Bean
public ExecutorChannel executorChannel() {
    return new ExecutorChannel(taskExecutor());
}

@Bean
public TaskExecutor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(25);
    return executor;
}
```

#### 6. RendezvousChannel

Канал без буфера — отправитель блокируется до тех пор, пока получатель не примет сообщение (аналог SynchronousQueue).

```java
@Bean
public RendezvousChannel rendezvousChannel() {
    return new RendezvousChannel();
}
```

**Сравнение каналов:**

| Тип канала | Синхронность | Буфер | Использование |
|-----------|-------------|-------|--------------|
| DirectChannel | Синхронный | Нет | Простые пайплайны, транзакции |
| PublishSubscribeChannel | Синхронный | Нет | Broadcasting, параллельная обработка |
| QueueChannel | Асинхронный | Да | Разделение отправителя/получателя |
| PriorityChannel | Асинхронный | Да (упорядоченный) | Обработка по приоритету |
| ExecutorChannel | Асинхронный | Пул потоков | Параллельная обработка |
| RendezvousChannel | Синхронный | Нет (handoff) | Синхронизация потоков |

### Message Endpoint (Конечная точка)

**Message Endpoint** — компонент, который обрабатывает сообщения: принимает из канала, выполняет логику, отправляет в другой канал. Spring Integration предоставляет множество готовых типов endpoint'ов.

## Message Endpoints

### Service Activator

Вызывает метод POJO для обработки сообщения. Самый распространённый тип endpoint.

```java
@ServiceActivator(inputChannel = "orderChannel", outputChannel = "confirmationChannel")
public OrderConfirmation processOrder(Order order) {
    // бизнес-логика
    return new OrderConfirmation(order.getId(), "PROCESSED");
}
```

Метод может принимать:
- `Message<T>` — полное сообщение с заголовками
- `T` — только payload
- `@Header` — доступ к конкретным заголовкам
- `@Headers Map<String, Object>` — все заголовки

```java
@ServiceActivator(inputChannel = "userChannel")
public void processUser(@Payload User user, 
                        @Header("requestId") String requestId,
                        @Headers Map<String, Object> headers) {
    // обработка
}
```

### Transformer

Преобразует сообщение из одного формата в другой.

```java
@Transformer(inputChannel = "xmlChannel", outputChannel = "jsonChannel")
public String xmlToJson(String xml) {
    // конвертация XML → JSON
    return convertToJson(xml);
}
```

**Типовые трансформеры:**
- Object-to-String, Object-to-JSON
- Payload type transformation
- Header enrichment
- Content enricher (добавление данных из внешних источников)

```java
@Bean
public IntegrationFlow transformFlow() {
    return IntegrationFlows.from("inputChannel")
        .transform(String.class, String::toUpperCase)
        .transform(Transformers.toJson())
        .channel("outputChannel")
        .get();
}
```

### Filter

Фильтрует сообщения на основе условия. Сообщения, не прошедшие фильтр, отбрасываются или направляются в отдельный канал.

```java
@Filter(inputChannel = "orderChannel", outputChannel = "validOrderChannel")
public boolean filterValidOrders(Order order) {
    return order.getAmount() > 0 && order.getCustomerId() != null;
}
```

С обработкой отброшенных сообщений:

```java
@Filter(inputChannel = "orderChannel", 
        outputChannel = "validOrderChannel",
        discardChannel = "invalidOrderChannel")
public boolean filterValidOrders(Order order) {
    return order.isValid();
}
```

### Router

Направляет сообщения в различные каналы на основе содержимого или заголовков.

```java
@Router(inputChannel = "orderChannel")
public String routeOrder(Order order) {
    if (order.getAmount() > 10000) {
        return "largeOrderChannel";
    } else {
        return "standardOrderChannel";
    }
}
```

Множественная маршрутизация:

```java
@Router(inputChannel = "notificationChannel")
public List<String> routeNotification(Notification notification) {
    List<String> channels = new ArrayList<>();
    if (notification.isSmsEnabled()) {
        channels.add("smsChannel");
    }
    if (notification.isEmailEnabled()) {
        channels.add("emailChannel");
    }
    return channels;
}
```

**Специализированные роутеры:**
- **Header Value Router** — маршрутизация по значению заголовка
- **Payload Type Router** — маршрутизация по типу payload
- **Exception Type Router** — маршрутизация исключений
- **Recipient List Router** — статический список каналов-получателей

### Splitter

Разбивает одно сообщение на несколько.

```java
@Splitter(inputChannel = "orderChannel", outputChannel = "orderItemChannel")
public List<OrderItem> splitOrder(Order order) {
    return order.getItems();
}
```

Каждый элемент списка отправляется как отдельное сообщение. Исходные заголовки копируются, добавляются `sequenceNumber` и `sequenceSize` для отслеживания.

### Aggregator

Объединяет несколько сообщений в одно. Используется в паре со Splitter для паттерна Scatter-Gather.

```java
@Aggregator(inputChannel = "responseChannel", 
            outputChannel = "aggregatedChannel")
public Order aggregateOrderItems(List<OrderItem> items) {
    Order order = new Order();
    order.setItems(items);
    return order;
}
```

Настройка группировки:

```java
@Aggregator(inputChannel = "responseChannel",
            outputChannel = "aggregatedChannel",
            correlationStrategy = "correlationStrategy",
            releaseStrategy = "releaseStrategy")
public Order aggregate(List<OrderItem> items) {
    return createOrder(items);
}

@Bean
public CorrelationStrategy correlationStrategy() {
    return message -> message.getHeaders().get("orderId");
}

@Bean
public ReleaseStrategy releaseStrategy() {
    return group -> group.size() >= 5 || group.isComplete();
}
```

### Enricher

Обогащает сообщение дополнительными данными без изменения структуры payload.

```java
@Bean
public IntegrationFlow enrichFlow() {
    return IntegrationFlows.from("orderChannel")
        .enrich(e -> e.requestChannel("userLookupChannel")
            .requestPayloadExpression("payload.userId")
            .propertyExpression("user", "payload"))
        .channel("enrichedOrderChannel")
        .get();
}
```

### Bridge

Соединяет два канала, позволяя организовать промежуточную обработку или изменить характеристики канала (синхронный → асинхронный).

```java
@BridgeFrom("inputChannel")
@BridgeTo("outputChannel")
public void bridge() {
}

// или через конфигурацию
@Bean
public IntegrationFlow bridgeFlow() {
    return IntegrationFlows.from("syncChannel")
        .channel(MessageChannels.executor(taskExecutor()))
        .bridge()
        .channel("asyncChannel")
        .get();
}
```

## Gateway и MessagingTemplate

### Gateway

**Gateway** — фасад для отправки сообщений в интеграционный поток. Скрывает детали messaging API и предоставляет простой Java-интерфейс.

```java
@MessagingGateway(defaultRequestChannel = "orderProcessingChannel")
public interface OrderGateway {
    
    @Gateway(requestChannel = "orderChannel", replyChannel = "confirmationChannel")
    OrderConfirmation submitOrder(Order order);
    
    @Gateway(requestChannel = "orderChannel", replyTimeout = 5000)
    Future<OrderConfirmation> submitOrderAsync(Order order);
    
    @Gateway(requestChannel = "orderQueryChannel")
    Order getOrder(@Header("orderId") Long orderId);
}
```

Использование:

```java
@Service
public class OrderService {
    @Autowired
    private OrderGateway orderGateway;
    
    public void placeOrder(Order order) {
        OrderConfirmation confirmation = orderGateway.submitOrder(order);
        log.info("Order confirmed: {}", confirmation);
    }
}
```

**Возможности Gateway:**
- Синхронные и асинхронные вызовы (возврат `Future`, `CompletableFuture`)
- Автоматическая конвертация payload
- Настройка timeout'ов
- Error handling
- Поддержка request-reply паттерна

### MessagingTemplate

`MessagingTemplate` — низкоуровневый API для отправки и получения сообщений. Используется, когда Gateway недостаточно гибок.

```java
@Autowired
private MessagingTemplate messagingTemplate;

public void sendOrder(Order order) {
    messagingTemplate.convertAndSend("orderChannel", order);
}

public OrderConfirmation sendAndReceive(Order order) {
    Message<?> reply = messagingTemplate.sendAndReceive("orderChannel", 
        MessageBuilder.withPayload(order).build());
    return (OrderConfirmation) reply.getPayload();
}

public Order receiveOrder() {
    Message<?> message = messagingTemplate.receive("orderQueueChannel", 1000);
    return (Order) message.getPayload();
}
```

## Адаптеры для внешних систем

Адаптеры связывают Spring Integration с внешними системами. Бывают двух типов:
- **Inbound Adapter** — получает данные из внешней системы и отправляет в канал
- **Outbound Adapter** — получает сообщения из канала и отправляет во внешнюю систему

### File Adapter

Работа с файловой системой.

**Inbound (чтение файлов):**

```java
@Bean
public IntegrationFlow fileReadingFlow() {
    return IntegrationFlows
        .from(Files.inboundAdapter(new File("/input"))
                .patternFilter("*.txt")
                .autoCreateDirectory(true),
            e -> e.poller(Pollers.fixedDelay(1000)))
        .transform(File.class, file -> {
            // обработка файла
            return readFileContent(file);
        })
        .channel("fileProcessingChannel")
        .get();
}
```

**Outbound (запись файлов):**

```java
@Bean
public IntegrationFlow fileWritingFlow() {
    return IntegrationFlows.from("outputChannel")
        .handle(Files.outboundAdapter(new File("/output"))
            .fileNameGenerator(message -> 
                "output-" + System.currentTimeMillis() + ".txt")
            .autoCreateDirectory(true))
        .get();
}
```

### HTTP Adapter

Интеграция с REST API.

**Outbound (HTTP запросы):**

```java
@Bean
public IntegrationFlow httpOutboundFlow() {
    return IntegrationFlows.from("requestChannel")
        .handle(Http.outboundGateway("https://api.example.com/orders")
            .httpMethod(HttpMethod.POST)
            .expectedResponseType(OrderResponse.class)
            .headerExpression("Authorization", "'Bearer ' + @tokenService.getToken()"))
        .channel("responseChannel")
        .get();
}
```

**Inbound (HTTP endpoints):**

```java
@Bean
public IntegrationFlow httpInboundFlow() {
    return IntegrationFlows
        .from(Http.inboundGateway("/api/orders")
            .requestMapping(m -> m.methods(HttpMethod.POST))
            .requestPayloadType(Order.class))
        .handle("orderService", "processOrder")
        .get();
}
```

### JMS Adapter

Интеграция с очередями сообщений (ActiveMQ, IBM MQ, RabbitMQ через AMQP).

```java
@Bean
public IntegrationFlow jmsInboundFlow(ConnectionFactory connectionFactory) {
    return IntegrationFlows
        .from(Jms.inboundAdapter(connectionFactory)
            .destination("orders.queue"))
        .handle("orderService", "processOrder")
        .get();
}

@Bean
public IntegrationFlow jmsOutboundFlow(ConnectionFactory connectionFactory) {
    return IntegrationFlows.from("notificationChannel")
        .handle(Jms.outboundAdapter(connectionFactory)
            .destination("notifications.queue"))
        .get();
}
```

### JDBC Adapter

Работа с базой данных.

**Inbound (polling database):**

```java
@Bean
public IntegrationFlow jdbcInboundFlow(DataSource dataSource) {
    return IntegrationFlows
        .from(Jdbc.inboundAdapter(dataSource, 
                "SELECT * FROM orders WHERE status = 'NEW'")
            .updateSql("UPDATE orders SET status = 'PROCESSING' WHERE id IN (:id)")
            .rowMapper(new OrderRowMapper()),
            e -> e.poller(Pollers.fixedDelay(5000)))
        .channel("orderProcessingChannel")
        .get();
}
```

**Outbound (insert/update):**

```java
@Bean
public IntegrationFlow jdbcOutboundFlow(DataSource dataSource) {
    return IntegrationFlows.from("orderChannel")
        .handle(Jdbc.outboundAdapter(dataSource,
            "INSERT INTO orders (id, amount, status) VALUES (:payload.id, :payload.amount, :payload.status)"))
        .get();
}
```

### Mail Adapter

Отправка и получение email.

**Outbound (отправка):**

```java
@Bean
public IntegrationFlow mailOutboundFlow(JavaMailSender mailSender) {
    return IntegrationFlows.from("emailChannel")
        .enrichHeaders(h -> h
            .header(MailHeaders.TO, "user@example.com")
            .header(MailHeaders.SUBJECT, "Order Confirmation"))
        .handle(Mail.outboundAdapter("smtp.gmail.com")
            .port(587)
            .credentials("username", "password")
            .javaMailProperties(p -> p.put("mail.smtp.auth", "true")))
        .get();
}
```

**Inbound (получение):**

```java
@Bean
public IntegrationFlow mailInboundFlow() {
    return IntegrationFlows
        .from(Mail.imapInboundAdapter("imap://user:pass@imap.gmail.com/INBOX")
            .shouldDeleteMessages(false)
            .shouldMarkMessagesAsRead(true),
            e -> e.poller(Pollers.fixedDelay(60000)))
        .channel("emailProcessingChannel")
        .get();
}
```

### FTP/SFTP Adapter

Работа с FTP/SFTP серверами.

```java
@Bean
public IntegrationFlow ftpInboundFlow() {
    return IntegrationFlows
        .from(Ftp.inboundAdapter(ftpSessionFactory())
            .remoteDirectory("/remote/orders")
            .localDirectory(new File("/local/input"))
            .deleteRemoteFiles(true),
            e -> e.poller(Pollers.fixedDelay(10000)))
        .channel("fileProcessingChannel")
        .get();
}

@Bean
public IntegrationFlow ftpOutboundFlow() {
    return IntegrationFlows.from("uploadChannel")
        .handle(Ftp.outboundAdapter(ftpSessionFactory())
            .remoteDirectory("/remote/output")
            .fileNameGenerator(msg -> "file-" + System.currentTimeMillis() + ".dat"))
        .get();
}
```

### WebSocket Adapter

Реал-тайм коммуникация.

```java
@Bean
public IntegrationFlow webSocketInboundFlow() {
    return IntegrationFlows
        .from(WebSockets.inboundAdapter("/ws/messages"))
        .channel("messageProcessingChannel")
        .get();
}

@Bean
public IntegrationFlow webSocketOutboundFlow() {
    return IntegrationFlows.from("broadcastChannel")
        .handle(WebSockets.outboundAdapter("/topic/notifications"))
        .get();
}
```

## Конфигурация интеграционных потоков

### Java DSL (рекомендуется)

Java DSL — современный способ конфигурации, предоставляющий type-safe и читаемый код.

**Простой поток:**

```java
@Configuration
@EnableIntegration
public class IntegrationConfig {
    
    @Bean
    public IntegrationFlow simpleFlow() {
        return IntegrationFlows
            .from("inputChannel")
            .filter(String.class, s -> s.length() > 0)
            .transform(String::toUpperCase)
            .handle("messageService", "process")
            .get();
    }
}
```

**Сложный поток с ветвлением:**

```java
@Bean
public IntegrationFlow orderProcessingFlow() {
    return IntegrationFlows.from("orderChannel")
        .filter(Order.class, Order::isValid, 
            f -> f.discardChannel("invalidOrderChannel"))
        .enrichHeaders(h -> h.header("processedAt", System.currentTimeMillis()))
        .route(Order.class, order -> order.getType(),
            mapping -> mapping
                .subFlowMapping("STANDARD", standardOrderFlow())
                .subFlowMapping("EXPRESS", expressOrderFlow())
                .defaultSubFlowMapping(defaultOrderFlow()))
        .get();
}

private IntegrationFlow standardOrderFlow() {
    return f -> f
        .handle("orderService", "processStandard")
        .channel("confirmationChannel");
}

private IntegrationFlow expressOrderFlow() {
    return f -> f
        .handle("orderService", "processExpress")
        .channel("confirmationChannel");
}
```

**Scatter-Gather паттерн:**

```java
@Bean
public IntegrationFlow scatterGatherFlow() {
    return IntegrationFlows.from("requestChannel")
        .scatterGather(
            scatterer -> scatterer
                .recipientFlow("service1Channel", f -> f.handle("service1", "process"))
                .recipientFlow("service2Channel", f -> f.handle("service2", "process"))
                .recipientFlow("service3Channel", f -> f.handle("service3", "process")),
            gatherer -> gatherer
                .releaseStrategy(group -> group.size() == 3)
                .outputProcessor(group -> {
                    List<String> results = new ArrayList<>();
                    group.getMessages().forEach(m -> 
                        results.add((String) m.getPayload()));
                    return results;
                }))
        .channel("aggregatedResultChannel")
        .get();
}
```

### Аннотации

Конфигурация через аннотации на методах POJO.

```java
@Component
public class OrderProcessor {
    
    @ServiceActivator(inputChannel = "orderChannel")
    public Order processOrder(Order order) {
        // обработка заказа
        return order;
    }
    
    @Transformer(inputChannel = "xmlChannel", outputChannel = "orderChannel")
    public Order transformXmlToOrder(String xml) {
        return parseXml(xml);
    }
    
    @Filter(inputChannel = "orderChannel", outputChannel = "validOrderChannel")
    public boolean isValidOrder(Order order) {
        return order.getAmount() > 0;
    }
    
    @Router(inputChannel = "orderChannel")
    public String routeOrder(Order order) {
        return order.isPriority() ? "priorityChannel" : "standardChannel";
    }
    
    @Splitter(inputChannel = "orderChannel", outputChannel = "itemChannel")
    public List<OrderItem> splitOrder(Order order) {
        return order.getItems();
    }
    
    @Aggregator(inputChannel = "itemChannel", outputChannel = "orderChannel")
    public Order aggregateItems(List<OrderItem> items) {
        return createOrder(items);
    }
}
```

### XML-конфигурация (legacy)

XML-конфигурация поддерживается для обратной совместимости.

```xml
<int:channel id="inputChannel"/>
<int:channel id="outputChannel"/>

<int:service-activator input-channel="inputChannel"
                       output-channel="outputChannel"
                       ref="orderService"
                       method="processOrder"/>

<int:transformer input-channel="xmlChannel"
                 output-channel="orderChannel"
                 expression="T(com.example.XmlParser).parse(payload)"/>

<int:filter input-channel="orderChannel"
            output-channel="validOrderChannel"
            expression="payload.amount > 0"/>

<int:router input-channel="orderChannel"
            expression="payload.type">
    <int:mapping value="STANDARD" channel="standardChannel"/>
    <int:mapping value="EXPRESS" channel="expressChannel"/>
</int:router>
```

## Poller и планирование

Для `QueueChannel`, `PriorityChannel` и всех inbound-адаптеров требуется **Poller** — компонент, опрашивающий источник данных.

**Настройка поллера:**

```java
@Bean
public IntegrationFlow pollingFlow() {
    return IntegrationFlows
        .from("queueChannel", e -> e.poller(Pollers.fixedDelay(1000)
            .maxMessagesPerPoll(10)
            .transactional()
            .errorChannel("errorChannel")))
        .handle("service", "process")
        .get();
}
```

**Типы поллеров:**
- `fixedDelay(long period)` — фиксированная задержка между окончанием обработки и началом следующего опроса
- `fixedRate(long period)` — фиксированная частота (независимо от времени обработки)
- `cron(String expression)` — cron-выражение для сложных расписаний

**Расширенная конфигурация:**

```java
@Bean
public PollerMetadata defaultPoller() {
    return Pollers.fixedDelay(500)
        .maxMessagesPerPoll(5)
        .receiveTimeout(1000)
        .taskExecutor(taskExecutor())
        .transactional(transactionManager())
        .advice(retryAdvice())
        .get();
}

@Bean
public Advice retryAdvice() {
    RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
    RetryTemplate retryTemplate = new RetryTemplate();
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(3));
    advice.setRetryTemplate(retryTemplate);
    return advice;
}
```

## Обработка ошибок

### Error Channel

Spring Integration автоматически создаёт глобальный канал `errorChannel` для необработанных исключений.

```java
@ServiceActivator(inputChannel = "errorChannel")
public void handleError(ErrorMessage errorMessage) {
    Throwable cause = errorMessage.getPayload();
    Message<?> failedMessage = errorMessage.getOriginalMessage();
    
    log.error("Error processing message: {}", failedMessage, cause);
    
    // логика восстановления или уведомления
}
```

**Пользовательский error channel:**

```java
@Bean
public IntegrationFlow errorHandlingFlow() {
    return IntegrationFlows.from("inputChannel")
        .handle("service", "process",
            e -> e.advice(errorAdvice()).errorChannel("customErrorChannel"))
        .get();
}

@ServiceActivator(inputChannel = "customErrorChannel")
public void handleCustomError(ErrorMessage error) {
    // специфичная обработка ошибок
}
```

### Retry и Recovery

Использование `RequestHandlerRetryAdvice` для автоматического повтора.

```java
@Bean
public Advice retryAdvice() {
    RequestHandlerRetryAdvice advice = new RequestHandlerRetryAdvice();
    
    RetryTemplate retryTemplate = new RetryTemplate();
    
    // Политика повторов
    SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
    retryPolicy.setMaxAttempts(3);
    retryTemplate.setRetryPolicy(retryPolicy);
    
    // Политика задержки
    ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
    backOffPolicy.setInitialInterval(1000);
    backOffPolicy.setMultiplier(2.0);
    backOffPolicy.setMaxInterval(10000);
    retryTemplate.setBackOffPolicy(backOffPolicy);
    
    advice.setRetryTemplate(retryTemplate);
    
    // Recovery callback
    advice.setRecoveryCallback(context -> {
        Message<?> message = (Message<?>) context.getAttribute("message");
        log.error("All retries exhausted for message: {}", message);
        return null;
    });
    
    return advice;
}

@Bean
public IntegrationFlow flowWithRetry() {
    return IntegrationFlows.from("inputChannel")
        .handle("unreliableService", "process",
            e -> e.advice(retryAdvice()))
        .get();
}
```

### Circuit Breaker

Интеграция с Resilience4j для circuit breaker паттерна.

```java
@Bean
public Advice circuitBreakerAdvice() {
    CircuitBreakerRequestHandlerAdvice advice = new CircuitBreakerRequestHandlerAdvice();
    
    CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .build();
    
    CircuitBreaker circuitBreaker = CircuitBreaker.of("externalService", config);
    advice.setCircuitBreaker(circuitBreaker);
    
    return advice;
}
```

## Транзакции

Spring Integration поддерживает транзакционную обработку сообщений.

```java
@Bean
public IntegrationFlow transactionalFlow(PlatformTransactionManager txManager) {
    return IntegrationFlows
        .from("queueChannel", 
            e -> e.poller(Pollers.fixedDelay(1000)
                .transactional(txManager)))
        .handle(Jpa.updatingGateway(entityManagerFactory())
            .entityClass(Order.class)
            .persistMode(PersistMode.PERSIST))
        .get();
}
```

**Транзакционная синхронизация:**

```java
@Bean
public IntegrationFlow txSyncFlow() {
    return IntegrationFlows.from("inputChannel")
        .handle("orderService", "processOrder",
            e -> e.transactional(true))
        .handle("notificationService", "sendConfirmation",
            e -> e.transactional(new TransactionSynchronizationFactory() {
                @Override
                public TransactionSynchronization create(Object key) {
                    return new TransactionSynchronizationAdapter() {
                        @Override
                        public void afterCommit() {
                            // выполняется после успешного коммита
                        }
                    };
                }
            }))
        .get();
}
```

## Мониторинг и метрики

### Micrometer Integration

Spring Integration автоматически публикует метрики в Micrometer.

```java
@Configuration
@EnableIntegrationManagement
public class MetricsConfig {
    
    @Bean
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}
```

**Доступные метрики:**
- `spring.integration.channels` — статистика каналов (send, receive count, duration)
- `spring.integration.handlers` — статистика endpoint'ов (duration, active count)
- `spring.integration.sources` — статистика источников сообщений

**Добавление custom метрик:**

```java
@Component
public class MetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    
    public MetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCounter = Counter.builder("orders.processed")
            .tag("type", "integration")
            .register(meterRegistry);
    }
    
    @ServiceActivator(inputChannel = "orderChannel")
    public Order process(Order order) {
        orderCounter.increment();
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // обработка
            return order;
        } finally {
            sample.stop(Timer.builder("order.processing.time")
                .register(meterRegistry));
        }
    }
}
```

### JMX Export

Экспорт интеграционных компонентов в JMX.

```java
@Configuration
@EnableIntegration
@EnableIntegrationMBeanExport
public class JmxConfig {
    
    @Bean
    public IntegrationMBeanExporter integrationMBeanExporter() {
        IntegrationMBeanExporter exporter = new IntegrationMBeanExporter();
        exporter.setDefaultDomain("com.example.integration");
        return exporter;
    }
}
```

### Message History

Отслеживание пути сообщения через компоненты.

```java
@Configuration
@EnableMessageHistory("*Channel", "orderService*")
public class MessageHistoryConfig {
}

@ServiceActivator(inputChannel = "debugChannel")
public void printHistory(Message<?> message) {
    MessageHistory history = MessageHistory.read(message);
    history.forEach(entry -> 
        log.info("Component: {}, Timestamp: {}", 
            entry.getComponentName(), entry.getTimestamp()));
}
```

### Wire Tap

Перехват сообщений для логирования или аудита без влияния на основной поток.

```java
@Bean
public IntegrationFlow mainFlow() {
    return IntegrationFlows.from("inputChannel")
        .wireTap("loggingChannel")
        .transform(String::toUpperCase)
        .channel("outputChannel")
        .get();
}

@ServiceActivator(inputChannel = "loggingChannel")
public void logMessage(Message<?> message) {
    log.info("Message: {}", message);
}
```

## Тестирование

### MockIntegration

Spring Integration предоставляет утилиты для тестирования.

```java
@SpringBootTest
@DirtiesContext
public class IntegrationFlowTest {
    
    @Autowired
    private MessageChannel inputChannel;
    
    @Autowired
    private PollableChannel outputChannel;
    
    @Test
    public void testFlow() {
        Message<String> message = MessageBuilder
            .withPayload("test")
            .build();
        
        inputChannel.send(message);
        
        Message<?> received = outputChannel.receive(1000);
        assertNotNull(received);
        assertEquals("TEST", received.getPayload());
    }
}
```

### Тестирование с Gateway:

```java
@SpringBootTest
public class GatewayTest {
    
    @Autowired
    private OrderGateway orderGateway;
    
    @MockBean
    private OrderService orderService;
    
    @Test
    public void testGateway() {
        Order order = new Order(1L, 100.0);
        OrderConfirmation expected = new OrderConfirmation(1L, "OK");
        
        when(orderService.processOrder(order)).thenReturn(expected);
        
        OrderConfirmation actual = orderGateway.submitOrder(order);
        
        assertEquals(expected, actual);
        verify(orderService).processOrder(order);
    }
}
```

### Тестирование адаптеров:

```java
@SpringBootTest
public class FileAdapterTest {
    
    @TempDir
    Path tempDir;
    
    @Autowired
    private MessageChannel fileInputChannel;
    
    @Test
    public void testFileReading() throws IOException {
        File testFile = tempDir.resolve("test.txt").toFile();
        Files.write(testFile.toPath(), "test content".getBytes());
        
        // триггер чтения файла
        Message<?> message = MessageBuilder.withPayload(testFile).build();
        fileInputChannel.send(message);
        
        // проверка обработки
    }
}
```

### Embedded Test Endpoints:

```java
@Configuration
public class TestConfig {
    
    @Bean
    public IntegrationFlow testFlow() {
        return IntegrationFlows.from("inputChannel")
            .handle(message -> {
                // тестовая логика
            })
            .channel("outputChannel")
            .get();
    }
    
    @Bean
    @BridgeFrom("outputChannel")
    public QueueChannel testOutputChannel() {
        return new QueueChannel();
    }
}
```

## Реактивная поддержка

Spring Integration 5.x+ поддерживает реактивные потоки.

```java
@Bean
public IntegrationFlow reactiveFlow() {
    return IntegrationFlows
        .from(WebFlux.inboundGateway("/api/reactive")
            .requestMapping(m -> m.methods(HttpMethod.POST)))
        .channel(MessageChannels.flux())
        .transform(Flux.class, flux -> flux.map(String::toUpperCase))
        .channel("outputChannel")
        .get();
}
```

**Реактивные адаптеры:**

```java
@Bean
public IntegrationFlow r2dbcFlow(ConnectionFactory connectionFactory) {
    return IntegrationFlows
        .from(R2dbc.inboundChannelAdapter(connectionFactory,
            "SELECT * FROM orders WHERE status = 'NEW'"),
            e -> e.poller(Pollers.fixedDelay(1000)))
        .channel("orderProcessingChannel")
        .get();
}
```

## Продвинутые паттерны

### Content-Based Router с условиями

```java
@Bean
public IntegrationFlow contentBasedRouter() {
    return IntegrationFlows.from("inputChannel")
        .route(Message.class, message -> {
            Integer priority = message.getHeaders().get("priority", Integer.class);
            if (priority != null && priority > 5) {
                return "highPriorityChannel";
            } else {
                return "normalPriorityChannel";
            }
        })
        .get();
}
```

### Claim Check Pattern

Сохранение больших payload в хранилище, передача только ссылки.

```java
@Bean
public IntegrationFlow claimCheckFlow() {
    return IntegrationFlows.from("largeMessageChannel")
        .claimCheckIn(messageStore())
        .channel("processChannel")
        .claimCheckOut(messageStore())
        .channel("outputChannel")
        .get();
}

@Bean
public MessageStore messageStore() {
    return new SimpleMessageStore();
}
```

### Idempotent Receiver

Предотвращение дублирующей обработки сообщений.

```java
@Bean
public IntegrationFlow idempotentFlow() {
    return IntegrationFlows.from("inputChannel")
        .filter(Message.class, message -> {
            String messageId = (String) message.getHeaders().get("messageId");
            return !processedMessages.contains(messageId);
        })
        .handle("service", "process")
        .handle(message -> {
            String messageId = (String) message.getHeaders().get("messageId");
            processedMessages.add(messageId);
        })
        .get();
}
```

### Message Expiration

Отбрасывание устаревших сообщений.

```java
@Bean
public IntegrationFlow expirationFlow() {
    return IntegrationFlows.from("queueChannel")
        .filter(Message.class, message -> {
            Long timestamp = message.getHeaders().getTimestamp();
            long age = System.currentTimeMillis() - timestamp;
            return age < 60000; // сообщения старше 1 минуты отбрасываются
        }, f -> f.discardChannel("expiredChannel"))
        .channel("processChannel")
        .get();
}
```

## Практические советы

### Производительность

1. **Используйте асинхронные каналы для долгих операций:**
   ```java
   return IntegrationFlows.from("inputChannel")
       .channel(MessageChannels.executor(taskExecutor()))
       .handle("slowService", "process")
       .get();
   ```

2. **Настройте размер пула потоков:**
   ```java
   @Bean
   public TaskExecutor integrationTaskExecutor() {
       ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
       executor.setCorePoolSize(10);
       executor.setMaxPoolSize(50);
       executor.setQueueCapacity(100);
       executor.setThreadNamePrefix("integration-");
       return executor;
   }
   ```

3. **Используйте batch обработку:**
   ```java
   @Aggregator(inputChannel = "itemChannel", 
               outputChannel = "batchChannel",
               releaseStrategy = "releaseStrategy")
   public List<Item> batch(List<Item> items) {
       return items;
   }
   
   @Bean
   public ReleaseStrategy releaseStrategy() {
       return new MessageCountReleaseStrategy(100); // батч из 100 сообщений
   }
   ```

### Отладка

1. **Включите логирование:**
   ```properties
   logging.level.org.springframework.integration=DEBUG
   logging.level.org.springframework.integration.handler=TRACE
   ```

2. **Используйте Wire Tap для отладки:**
   ```java
   .wireTap("debugChannel")
   ```

3. **Message History для трассировки:**
   ```java
   @EnableMessageHistory
   ```

### Безопасность

1. **Защита Gateway endpoints:**
   ```java
   @PreAuthorize("hasRole('ADMIN')")
   @MessagingGateway
   public interface SecuredGateway {
       void processOrder(Order order);
   }
   ```

2. **Шифрование payload:**
   ```java
   @Transformer(inputChannel = "inputChannel", outputChannel = "encryptedChannel")
   public byte[] encrypt(String plaintext) {
       return encryptionService.encrypt(plaintext);
   }
   ```

### Масштабирование

1. **Горизонтальное масштабирование с очередями:**
   - Используйте JMS/RabbitMQ для распределения нагрузки между инстансами
   - Настройте concurrent consumers

2. **Партиционирование:**
   ```java
   @Router(inputChannel = "inputChannel")
   public String partition(Order order) {
       int partition = order.getCustomerId() % 4;
       return "partition-" + partition + "-channel";
   }
   ```

## Типичные сценарии использования

### Интеграция с микросервисами

```java
@Bean
public IntegrationFlow orderMicroserviceFlow() {
    return IntegrationFlows.from("orderRequestChannel")
        // Обогащение данными пользователя
        .enrich(e -> e
            .requestChannel("userServiceChannel")
            .requestPayloadExpression("payload.userId")
            .propertyExpression("user", "payload"))
        // Проверка инвентаря
        .handle(Http.outboundGateway("http://inventory-service/check")
            .httpMethod(HttpMethod.POST)
            .expectedResponseType(InventoryResponse.class))
        // Обработка результата
        .route(InventoryResponse.class, r -> r.isAvailable(),
            mapping -> mapping
                .subFlowMapping(true, approveFlow())
                .subFlowMapping(false, rejectFlow()))
        .get();
}
```

### ETL процесс

```java
@Bean
public IntegrationFlow etlFlow() {
    return IntegrationFlows
        // Extract
        .from(Jdbc.inboundAdapter(dataSource(), 
                "SELECT * FROM source_table WHERE processed = false")
            .updateSql("UPDATE source_table SET processed = true WHERE id = :id"),
            e -> e.poller(Pollers.fixedDelay(60000)))
        // Transform
        .split()
        .transform(sourceRecord -> transformToTarget(sourceRecord))
        .filter(Objects::nonNull)
        // Load
        .aggregate(a -> a
            .releaseStrategy(new MessageCountReleaseStrategy(1000))
            .correlationExpression("1"))
        .handle(Jdbc.outboundAdapter(targetDataSource(),
            "INSERT INTO target_table (data) VALUES (:payload.data)"))
        .get();
}
```

### Event-driven архитектура

```java
@Bean
public IntegrationFlow eventPublisher() {
    return IntegrationFlows.from("eventChannel")
        .publishSubscribeChannel(s -> s
            .subscribe(f -> f
                .handle(Kafka.outboundChannelAdapter(kafkaTemplate())
                    .topic("events")))
            .subscribe(f -> f
                .handle(Jms.outboundAdapter(jmsConnectionFactory())
                    .destination("events.queue")))
            .subscribe(f -> f
                .handle("auditService", "logEvent")))
        .get();
}
```

### Файловая обработка с архивированием

```java
@Bean
public IntegrationFlow fileProcessingFlow() {
    return IntegrationFlows
        .from(Files.inboundAdapter(new File("/input"))
                .patternFilter("*.csv"),
            e -> e.poller(Pollers.fixedDelay(5000)))
        // Обработка
        .handle("csvProcessor", "process")
        // Перемещение в архив
        .enrichHeaders(h -> h.header(FileHeaders.FILENAME, 
            (Message<?> m) -> "processed-" + 
                m.getHeaders().get(FileHeaders.FILENAME)))
        .handle(Files.outboundAdapter(new File("/archive")))
        .get();
}
```

## Практические упражнения

1. Создайте интеграционный поток для чтения файлов CSV, преобразования в JSON и отправки в REST API.
2. Реализуйте scatter-gather паттерн для параллельного вызова нескольких микросервисов и агрегации результатов.
3. Настройте JMS inbound adapter с обработкой ошибок и retry механизмом.
4. Создайте Gateway интерфейс для асинхронной отправки email с подтверждением доставки.
5. Реализуйте ETL процесс с polling базы данных, трансформацией данных и загрузкой в другую систему.
6. Настройте мониторинг интеграционного потока через Micrometer и Actuator.

## Вопросы на собеседовании

1. **Что такое Spring Integration и для чего он используется?**
   
   *Ответ:* Spring Integration — фреймворк для построения enterprise-интеграций на основе паттернов EIP. Предоставляет message-driven архитектуру для связывания различных компонентов и внешних систем (базы данных, очереди, файлы, REST API). Основные компоненты: Message, Channel, Endpoint. Используется для асинхронной обработки, интеграции микросервисов, ETL процессов, event-driven архитектур.

2. **В чём разница между DirectChannel и QueueChannel?**
   
   *Ответ:* `DirectChannel` — синхронный канал, сообщение обрабатывается в потоке отправителя, поддерживает транзакции, используется для простых пайплайнов. `QueueChannel` — асинхронный канал с буфером (очередь), требует poller для чтения, разделяет отправителя и получателя, подходит для буферизации и сглаживания нагрузки. QueueChannel позволяет отправителю не ждать обработки.

3. **Какие типы Message Endpoint существуют?**
   
   *Ответ:* Service Activator (вызов метода), Transformer (преобразование данных), Filter (фильтрация сообщений), Router (маршрутизация по условию), Splitter (разбиение на несколько сообщений), Aggregator (объединение нескольких сообщений), Enricher (обогащение данными), Bridge (соединение каналов), Gateway (интерфейс для отправки сообщений).

4. **Как работает Gateway в Spring Integration?**
   
   *Ответ:* Gateway — прокси-интерфейс, скрывающий детали messaging API. При вызове метода Gateway автоматически создаёт Message с payload из аргументов, отправляет в указанный канал, ожидает ответ (если метод возвращает значение), извлекает payload из ответного сообщения. Поддерживает синхронные/асинхронные вызовы (Future, CompletableFuture), настройку timeout, обработку ошибок.

5. **Что такое Poller и когда он нужен?**
   
   *Ответ:* Poller — компонент для опроса источников данных (QueueChannel, PriorityChannel, inbound adapters). Периодически проверяет наличие сообщений и отправляет в поток обработки. Настраивается через `fixedDelay`, `fixedRate` или `cron`. Можно настроить `maxMessagesPerPoll`, транзакционность, error handling, advice (retry). Без Poller асинхронные каналы и адаптеры не будут работать.

6. **Как обрабатывать ошибки в интеграционных потоках?**
   
   *Ответ:* Несколько подходов: глобальный `errorChannel` для всех необработанных исключений, пользовательские error channels для специфичной обработки, `RequestHandlerRetryAdvice` для автоматического повтора с backoff политикой, `RequestHandlerCircuitBreakerAdvice` для circuit breaker паттерна, `RecoveryCallback` для fallback логики после исчерпания повторов, обработка через `@ServiceActivator` на error channel.

7. **Объясните паттерн Scatter-Gather.**
   
   *Ответ:* Scatter-Gather — паттерн параллельной обработки: одно входящее сообщение рассылается нескольким обработчикам (scatter), результаты собираются и агрегируются в одно сообщение (gather). Используется для параллельного вызова нескольких сервисов с последующим объединением ответов. В Spring Integration реализуется через `scatterGather()` в Java DSL с настройкой `recipientFlow` и `ReleaseStrategy`.

8. **Как настроить транзакционную обработку сообщений?**
   
   *Ответ:* Для транзакций нужен `PlatformTransactionManager`. В Poller указывается `.transactional(txManager)`. Для Service Activator и других endpoint'ов: `.handle(..., e -> e.transactional(true))`. Транзакция включает получение сообщения из канала, обработку и отправку в выходной канал. При ошибке происходит rollback, сообщение возвращается в канал (для QueueChannel). Можно использовать `TransactionSynchronization` для post-commit/rollback действий.

9. **Что такое Wire Tap и Message History?**
   
   *Ответ:* Wire Tap — неинвазивный перехват сообщений для логирования, аудита, мониторинга без влияния на основной поток. Сообщение копируется в отдельный канал через `.wireTap("loggingChannel")`. Message History — трассировка пути сообщения через компоненты интеграции, добавляет записи о каждом пройденном endpoint с timestamp. Включается через `@EnableMessageHistory`. Полезно для отладки сложных потоков.

10. **Как тестировать интеграционные потоки?**
    
    *Ответ:* Используйте `@SpringBootTest` для загрузки контекста, `MessageChannel` для отправки тестовых сообщений, `PollableChannel` (QueueChannel) для получения результатов с timeout. `@MockBean` для подмены внешних зависимостей. Gateway можно тестировать как обычный Spring bean. Для изоляции используйте `@DirtiesContext`. MockIntegration utilities для специфичных проверок. Embedded test endpoints (QueueChannel с `@BridgeFrom`) для перехвата сообщений.

11. **В чём преимущества Java DSL перед XML конфигурацией?**
    
    *Ответ:* Java DSL предоставляет: type-safety (ошибки на этапе компиляции), рефакторинг через IDE (rename, extract method), читаемость (fluent API), условная логика создания потоков, отладка (breakpoints в конфигурации), lambda-выражения для inline обработки, современный подход. XML сложнее поддерживать, нет type-safety, рефакторинг затруднён, но позволяет внешнюю конфигурацию без перекомпиляции.

12. **Как реализовать idempotent receiver паттерн?**
    
    *Ответ:* Idempotent receiver предотвращает дублирующую обработку одного сообщения. Реализация: хранить ID обработанных сообщений в Set, Redis, БД; в filter проверять наличие ID; если сообщение уже обработано — отбрасывать. Spring Integration предоставляет `IdempotentReceiverInterceptor` с `MetadataStore` для хранения состояния. Можно использовать `@IdempotentReceiver` или настроить через selector в filter.

## Дополнительные материалы

- [Spring Integration Reference Documentation](https://docs.spring.io/spring-integration/docs/current/reference/html/)
- [Enterprise Integration Patterns](https://www.enterpriseintegrationpatterns.com/) — каталог паттернов интеграции
- Книга "Enterprise Integration Patterns" by Gregor Hohpe and Bobby Woolf — фундаментальный труд по паттернам интеграции
- [Spring Integration Samples](https://github.com/spring-projects/spring-integration-samples) — примеры различных интеграционных сценариев
- [Baeldung Spring Integration](https://www.baeldung.com/spring-integration) — туториалы и практические примеры
- [Spring Integration in Action](https://www.manning.com/books/spring-integration-in-action) — книга с реальными кейсами использования
