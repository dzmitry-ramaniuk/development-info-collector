# Redis: интеграция с Java и Spring

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Выбор уровня абстракции](#выбор-уровня-абстракции)
3. [Spring Cache](#spring-cache)
4. [RedisTemplate](#redistemplate)
5. [Lettuce](#lettuce)
6. [Сериализация](#сериализация)
7. [Таймауты, пул и retries](#таймауты-пул-и-retries)
8. [Testcontainers](#testcontainers)
9. [Практические советы](#практические-советы)
10. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-08-04.
- **Целевая версия Redis:** Redis Open Source 8.2.x.
- **Диапазон Java-стека:** Java 21+, Spring Boot 3.5/4.0, Spring Data Redis 3.5/4.0 и Lettuce 6.5/7.0; сверяйте совместимость по BOM выбранной линии Spring Boot.
- **Статус примеров:** `current`; package names и property binding проверяйте для конкретной minor-версии Boot.
- **Первичные источники:** [Spring Data Redis reference](https://docs.spring.io/spring-data/redis/reference/), [Spring Cache](https://docs.spring.io/spring-framework/reference/integration/cache.html), [Lettuce reference](https://redis.github.io/lettuce/), [Testcontainers Java](https://java.testcontainers.org/).

## Выбор уровня абстракции

| API | Сильная сторона | Подходит | Не подходит |
|---|---|---|---|
| Spring Cache | Декларативные `@Cacheable`, provider-neutral API | Кэш результата метода | Streams, Lua, fine-grained Redis-команды |
| `RedisTemplate` | Типизированные operations и интеграция Spring Data | Hash/Set/ZSet, transactions, scripts | Максимальный контроль Netty/connection lifecycle |
| Lettuce | Низкоуровневый sync/async/reactive Redis client | Pipelines, custom topology/reconnect, специализированный protocol usage | Когда достаточно простого method cache и важнее единообразие |

Абстракции можно сочетать, но используйте единый `RedisConnectionFactory`/client resources и согласованный key/value codec. Не допускайте, чтобы два API записывали один key разными сериализаторами.

## Spring Cache

```java
@Configuration
@EnableCaching
class CacheConfig {
    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(5))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaults)
            .withCacheConfiguration("products", defaults.entryTtl(Duration.ofMinutes(1)))
            .build();
    }
}

@Service
class ProductService {
    @Cacheable(cacheNames = "products", key = "#id", unless = "#result == null")
    Product find(long id) { return loadFromDatabase(id); }

    @CacheEvict(cacheNames = "products", key = "#result.id")
    Product update(Product product) { return saveAndCommit(product); }
}
```

`@Cacheable` работает через Spring proxy: self-invocation обходит advice. Определите ключи, TTL каждого cache name, null policy и момент eviction относительно DB transaction. Аннотация не решает stampede автоматически: добавьте local single-flight/locking либо stale strategy. `sync=true` зависит от реализации cache provider и не является распределённой гарантией между JVM.

## RedisTemplate

```java
@Bean
RedisTemplate<String, Product> productRedisTemplate(RedisConnectionFactory factory) {
    var template = new RedisTemplate<String, Product>();
    template.setConnectionFactory(factory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setHashKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
}

Optional<Product> find(RedisTemplate<String, Product> redis, long id) {
    String key = "product:" + id;
    return Optional.ofNullable(redis.opsForValue().get(key));
}

void put(RedisTemplate<String, Product> redis, Product product) {
    redis.opsForValue().set("product:" + product.id(), product, Duration.ofMinutes(5));
}
```

Используйте `SessionCallback` для операций на одном connection и `RedisScript` для атомарной серверной логики. Pipeline сокращает round trips, но меняет обработку результатов/ошибок; измеряйте размер batch. Не путайте Spring transaction synchronization с общей ACID-транзакцией Redis+SQL.

## Lettuce

Lettuce использует Netty, thread-safe connections и поддерживает sync/async/reactive API. Долгоживущий connection можно разделять для обычных команд, но blocking commands и transactions требуют выделенного connection согласно документации/архитектуре приложения.

```java
RedisURI uri = RedisURI.Builder.redis("localhost", 6379)
    .withTimeout(Duration.ofMillis(200))
    .build();

try (RedisClient client = RedisClient.create(uri);
     StatefulRedisConnection<String, String> connection = client.connect()) {
    connection.setTimeout(Duration.ofMillis(150));
    RedisCommands<String, String> commands = connection.sync();
    commands.setex("health:sample", 30, "ok");
}
```

Для async цепочки задавайте end-to-end deadline и отмену на уровне приложения; накопление unlimited futures создаёт memory pressure. Reactive API не делает Redis CPU-команды неблокирующими на сервере. В Cluster включайте topology refresh и обрабатывайте смену узлов через клиент, а не собственный список IP.

## Сериализация

- Ключи храните читаемыми UTF-8 строками с namespace и версией схемы: `catalog:v2:product:42`.
- JDK native serialization не используйте для недоверенных данных и долгоживущего формата: небезопасна, Java-specific и хрупка при эволюции классов.
- JSON удобен для диагностики и межъязыкового доступа, но больше по памяти; задайте allowlist типов, schema/version и тест backward compatibility.
- String/byte codecs быстрее и предсказуемее для counters/tokens; protobuf/CBOR уменьшают размер ценой tooling/schema governance.
- Не включайте default typing Jackson без безопасной конфигурации. Не полагайтесь на имя Java-класса как вечный wire contract.

Миграция формата: новый reader читает v2, при miss временно читает v1 и перезаписывает v2; либо меняйте namespace и прогревайте. Проверяйте реальный `MEMORY USAGE`, payload size и encode/decode latency.

## Таймауты, пул и retries

Разделяйте:

- **connect timeout** — установка TCP/TLS;
- **command timeout** — ожидание Redis response;
- **pool acquisition timeout** — ожидание connection;
- **end-to-end deadline** — бюджет всего запроса, включая retries/fallback.

Таймаут должен быть меньше upstream deadline и оставлять время на fallback. Не повторяйте бездумно команды после ambiguous timeout: `INCR`, `XADD` или Lua могли выполниться, хотя ответ потерян. Retries допустимы для идемпотентных reads и операций с idempotency token, с exponential backoff, jitter и общим retry budget.

Не создавайте connection на запрос. Пул нужен главным образом для blocking/transactional isolation; обычный Lettuce connection multiplexes commands. Ограничьте pending requests/concurrency, настройте circuit breaker и метрики pool wait. При отказе кэша fallback к DB должен иметь bulkhead, иначе приложение устроит stampede.

## Testcontainers

Интеграционный тест обязан закреплять образ Redis той же major/minor линии, что production, и не подменять важную семантику mock-объектом. Подробнее — в разделе [Testcontainers](../../тестирование/02-testcontainers.md).

```java
@Testcontainers
@SpringBootTest
class RedisCacheTest {
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8.2")
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.timeout", () -> "200ms");
    }

    @Autowired StringRedisTemplate template;

    @Test
    void valueExpires() throws InterruptedException {
        template.opsForValue().set("test:ttl", "value", Duration.ofMillis(100));
        await().atMost(Duration.ofSeconds(2))
            .until(() -> template.hasKey("test:ttl"), is(false));
    }
}
```

Не используйте фиксированный host port; Testcontainers выдаёт mapped port. Не проверяйте TTL через `sleep(101)`: active/lazy expiry и scheduler делают тест flaky — используйте bounded polling. Отдельно тестируйте serialization compatibility, cache miss/fallback, Redis restart, latency/timeout и duplicate outcome после ambiguous failure. Для Sentinel/Cluster нужен multi-container стенд или production-like ephemeral environment, а не одиночный container.

## Практические советы

- Берите версии Spring Data/Lettuce из Spring Boot BOM, не смешивайте произвольные major.
- Экспортируйте client latency, timeout/reconnect, pool wait, command errors и cache hit/miss с low-cardinality labels.
- Не записывайте key/value и credentials в exception logs/traces.
- Согласуйте SSL, ACL user с минимальными command/key permissions и ротацию credentials.
- Свяжите выбор паттерна с [материалом по кэшированию](../../system%20design/04-кэширование.md) и proxy-ограничения — с [Spring](../../java/spring/README.md).

## Вопросы для самопроверки

1. **Когда выбрать Spring Cache?** Для декларативного кэширования результата метода без Redis-specific операций.
2. **Почему `RedisTemplate` и Lettuce могут конфликтовать?** Разные codecs/serializers могут сделать один key взаимно нечитаемым.
3. **Можно ли retry `INCR` после timeout?** Не вслепую: команда могла выполниться; нужна идемпотентная схема или reconciliation.
4. **Зачем настоящий Redis в тесте?** Mock не воспроизводит TTL, serialization, scripts, network timeout, reconnect и topology.
