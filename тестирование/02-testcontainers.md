# Testcontainers — интеграционное тестирование с Docker-контейнерами

Testcontainers — это Java-библиотека, которая позволяет запускать Docker-контейнеры непосредственно из тестов. Это решение особенно полезно для интеграционного тестирования, когда требуется взаимодействие с реальными базами данных, брокерами сообщений, кэшами и другими внешними зависимостями.

## Содержание

1. [Введение и основы Testcontainers](#введение-и-основы-testcontainers)
2. [Установка и зависимости](#установка-и-зависимости)
3. [Базовые примеры использования](#базовые-примеры-использования)
4. [Специализированные модули](#специализированные-модули)
5. [Интеграция с JUnit 5](#интеграция-с-junit-5)
6. [Продвинутые возможности](#продвинутые-возможности)
7. [Конфигурация и настройка](#конфигурация-и-настройка)
8. [Лучшие практики](#лучшие-практики)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

---

## Введение и основы Testcontainers

### Проблема классического подхода

В традиционном интеграционном тестировании разработчики сталкиваются с несколькими проблемами:
- **Зависимость от окружения**: тесты требуют предустановленных баз данных, очередей и других сервисов
- **Несоответствие версий**: локальное окружение может отличаться от production
- **Изоляция тестов**: сложно обеспечить чистое состояние между тестами
- **CI/CD сложности**: требуется настройка инфраструктуры для тестовых окружений

### Решение с Testcontainers

Testcontainers решает эти проблемы, предоставляя:
- **Изолированное окружение**: каждый тест запускается с чистым контейнером
- **Воспроизводимость**: одинаковое поведение на всех машинах разработчиков и CI
- **Простоту настройки**: не нужно устанавливать и конфигурировать зависимости
- **Гибкость**: поддержка любого Docker-образа

### Архитектура Testcontainers

```
Тест (JUnit) 
    ↓
Testcontainers API
    ↓
Docker Engine
    ↓
Контейнеры (PostgreSQL, Redis, Kafka и т.д.)
```

> **Важно**: Для работы Testcontainers требуется установленный и запущенный Docker

---

## Установка и зависимости

### Требования

- Java 8 или выше
- Docker Desktop (Windows/Mac) или Docker Engine (Linux)
- Maven или Gradle для управления зависимостями

### Базовая зависимость (Maven)

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Интеграция с JUnit 5 -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### Базовая зависимость (Gradle)

```groovy
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
```

### Специализированные модули

Testcontainers предоставляет готовые модули для популярных технологий:

```xml
<!-- PostgreSQL -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- MySQL -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- MongoDB -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Kafka -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redis</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<!-- Elasticsearch -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

---

## Базовые примеры использования

### Пример 1: Универсальный контейнер (GenericContainer)

`GenericContainer` позволяет запустить любой Docker-образ:

```java
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@Test
void testWithGenericContainer() {
    try (GenericContainer<?> redis = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)) {
        
        redis.start();
        
        String address = redis.getHost();
        Integer port = redis.getMappedPort(6379);
        
        // Подключение к Redis
        Jedis jedis = new Jedis(address, port);
        jedis.set("key", "value");
        assertEquals("value", jedis.get("key"));
    }
}
```

### Пример 2: PostgreSQL контейнер

Специализированный модуль для PostgreSQL упрощает настройку:

```java
import org.testcontainers.containers.PostgreSQLContainer;

@Test
void testWithPostgreSQL() {
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")) {
        
        postgres.start();
        
        // Получение параметров подключения
        String jdbcUrl = postgres.getJdbcUrl();
        String username = postgres.getUsername();
        String password = postgres.getPassword();
        
        // Подключение к базе данных
        Connection connection = DriverManager.getConnection(
            jdbcUrl, username, password
        );
        
        // Выполнение SQL
        Statement stmt = connection.createStatement();
        stmt.execute("CREATE TABLE users (id SERIAL PRIMARY KEY, name VARCHAR(100))");
        stmt.execute("INSERT INTO users (name) VALUES ('John')");
        
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        assertEquals(1, rs.getInt(1));
    }
}
```

### Пример 3: Несколько контейнеров

Запуск нескольких взаимосвязанных контейнеров:

```java
@Test
void testWithMultipleContainers() {
    Network network = Network.newNetwork();
    
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15-alpine"))
            .withNetwork(network)
            .withNetworkAliases("postgres");
         
         GenericContainer<?> app = new GenericContainer<>(
            DockerImageName.parse("myapp:latest"))
            .withNetwork(network)
            .withEnv("DB_HOST", "postgres")
            .withEnv("DB_PORT", "5432")
            .dependsOn(postgres)) {
        
        postgres.start();
        app.start();
        
        // Тестирование приложения
    }
}
```

---

## Специализированные модули

### PostgreSQL

```java
import org.testcontainers.containers.PostgreSQLContainer;

class PostgreSQLTest {
    
    @Test
    void testPostgreSQL() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
                "postgres:15-alpine")
                .withDatabaseName("mydb")
                .withUsername("user")
                .withPassword("password")
                .withInitScript("init.sql")) {  // Инициализационный SQL-скрипт
            
            postgres.start();
            
            // JDBC URL автоматически формируется
            String jdbcUrl = postgres.getJdbcUrl();
            // jdbc:postgresql://localhost:32768/mydb
        }
    }
}
```

### MySQL

```java
import org.testcontainers.containers.MySQLContainer;

class MySQLTest {
    
    @Test
    void testMySQL() {
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
                .withDatabaseName("testdb")
                .withUsername("test")
                .withPassword("test")
                .withConfigurationOverride("mysql-config")) {  // Кастомная конфигурация
            
            mysql.start();
            
            String jdbcUrl = mysql.getJdbcUrl();
        }
    }
}
```

### MongoDB

```java
import org.testcontainers.containers.MongoDBContainer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

class MongoDBTest {
    
    @Test
    void testMongoDB() {
        try (MongoDBContainer mongo = new MongoDBContainer("mongo:7.0")) {
            mongo.start();
            
            String connectionString = mongo.getReplicaSetUrl();
            
            MongoClient client = MongoClients.create(connectionString);
            // Работа с MongoDB
        }
    }
}
```

### Kafka

```java
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

class KafkaTest {
    
    @Test
    void testKafka() {
        try (KafkaContainer kafka = new KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))) {
            
            kafka.start();
            
            String bootstrapServers = kafka.getBootstrapServers();
            
            // Настройка Producer
            Properties props = new Properties();
            props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class);
            props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                StringSerializer.class);
            
            KafkaProducer<String, String> producer = new KafkaProducer<>(props);
            // Отправка сообщений
        }
    }
}
```

### Redis

```java
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

class RedisTest {
    
    @Test
    void testRedis() {
        try (GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
                .withExposedPorts(6379)) {
            
            redis.start();
            
            Jedis jedis = new Jedis(
                redis.getHost(), 
                redis.getMappedPort(6379)
            );
            
            jedis.set("test", "value");
            assertEquals("value", jedis.get("test"));
        }
    }
}
```

### Elasticsearch

```java
import org.testcontainers.elasticsearch.ElasticsearchContainer;

class ElasticsearchTest {
    
    @Test
    void testElasticsearch() {
        try (ElasticsearchContainer elasticsearch = new ElasticsearchContainer(
                "docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
                .withEnv("xpack.security.enabled", "false")) {
            
            elasticsearch.start();
            
            String httpHostAddress = elasticsearch.getHttpHostAddress();
            
            // Подключение к Elasticsearch
            RestClient client = RestClient.builder(
                HttpHost.create(httpHostAddress)
            ).build();
        }
    }
}
```

---

## Интеграция с JUnit 5

### Аннотация @Testcontainers

Упрощает управление жизненным циклом контейнеров в JUnit 5:

```java
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class DatabaseTest {
    
    @Container
    private static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        "postgres:15-alpine"
    );
    
    @Test
    void test1() {
        // Контейнер уже запущен
        String jdbcUrl = postgres.getJdbcUrl();
        // Выполнение теста
    }
    
    @Test
    void test2() {
        // Тот же контейнер используется для всех тестов
        // (если поле static)
    }
}
```

### Контейнер на метод vs на класс

#### Static контейнер (на класс)

```java
@Testcontainers
class SharedContainerTest {
    
    // Один контейнер для всех тестов
    @Container
    private static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @Test
    void test1() {
        // Использует общий контейнер
    }
    
    @Test
    void test2() {
        // Использует тот же контейнер
    }
}
```

#### Non-static контейнер (на метод)

```java
@Testcontainers
class IsolatedContainerTest {
    
    // Новый контейнер для каждого теста
    @Container
    private PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @Test
    void test1() {
        // Свой контейнер
    }
    
    @Test
    void test2() {
        // Новый контейнер
    }
}
```

### Singleton контейнер для нескольких тестовых классов

```java
public abstract class AbstractIntegrationTest {
    
    private static final PostgreSQLContainer<?> POSTGRES;
    
    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);  // Переиспользование контейнера
        POSTGRES.start();
    }
    
    protected static String getJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }
}

class UserRepositoryTest extends AbstractIntegrationTest {
    
    @Test
    void testUserRepository() {
        String jdbcUrl = getJdbcUrl();
        // Тест
    }
}

class OrderRepositoryTest extends AbstractIntegrationTest {
    
    @Test
    void testOrderRepository() {
        String jdbcUrl = getJdbcUrl();
        // Тест
    }
}
```

---

## Продвинутые возможности

### Ожидание готовности контейнера (Wait Strategies)

Testcontainers предоставляет различные стратегии ожидания:

#### HTTP Wait Strategy

```java
GenericContainer<?> app = new GenericContainer<>("myapp:latest")
    .withExposedPorts(8080)
    .waitingFor(Wait.forHttp("/health")
        .forStatusCode(200)
        .withStartupTimeout(Duration.ofMinutes(2)));
```

#### Log Wait Strategy

```java
GenericContainer<?> app = new GenericContainer<>("myapp:latest")
    .waitingFor(Wait.forLogMessage(".*Application started.*", 1)
        .withStartupTimeout(Duration.ofMinutes(2)));
```

#### Port Wait Strategy

```java
GenericContainer<?> app = new GenericContainer<>("myapp:latest")
    .withExposedPorts(8080)
    .waitingFor(Wait.forListeningPort());
```

#### HealthCheck Wait Strategy

```java
GenericContainer<?> app = new GenericContainer<>("myapp:latest")
    .waitingFor(Wait.forHealthcheck()
        .withStartupTimeout(Duration.ofMinutes(2)));
```

### Docker Compose

Testcontainers поддерживает запуск сервисов через Docker Compose:

```java
import org.testcontainers.containers.DockerComposeContainer;

@Test
void testWithDockerCompose() {
    try (DockerComposeContainer<?> compose = new DockerComposeContainer<>(
            new File("docker-compose.yml"))
            .withExposedService("postgres", 5432)
            .withExposedService("redis", 6379)) {
        
        compose.start();
        
        String postgresHost = compose.getServiceHost("postgres", 5432);
        Integer postgresPort = compose.getServicePort("postgres", 5432);
    }
}
```

### Копирование файлов в контейнер

```java
GenericContainer<?> container = new GenericContainer<>("myapp:latest")
    .withCopyFileToContainer(
        MountableFile.forClasspathResource("config.yml"),
        "/app/config.yml"
    )
    .withCopyFileToContainer(
        MountableFile.forHostPath("/local/path/data.json"),
        "/app/data.json"
    );
```

### Выполнение команд в контейнере

```java
@Test
void testExecInContainer() throws Exception {
    try (GenericContainer<?> container = new GenericContainer<>("alpine:latest")
            .withCommand("tail", "-f", "/dev/null")) {
        
        container.start();
        
        // Выполнение команды
        Container.ExecResult result = container.execInContainer(
            "echo", "Hello from container"
        );
        
        assertEquals(0, result.getExitCode());
        assertTrue(result.getStdout().contains("Hello from container"));
    }
}
```

### Логирование контейнера

```java
@Test
void testContainerLogs() {
    GenericContainer<?> container = new GenericContainer<>("myapp:latest")
        .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("container")));
    
    container.start();
    
    // Логи контейнера будут выводиться через SLF4J
}
```

### Сеть между контейнерами

```java
@Test
void testNetworking() {
    Network network = Network.newNetwork();
    
    try (GenericContainer<?> postgres = new GenericContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases("db")
            .withEnv("POSTGRES_PASSWORD", "secret");
         
         GenericContainer<?> app = new GenericContainer<>("myapp:latest")
            .withNetwork(network)
            .withEnv("DB_HOST", "db")
            .withEnv("DB_PORT", "5432")
            .dependsOn(postgres)) {
        
        postgres.start();
        app.start();
        
        // Приложение может обращаться к БД по имени "db"
    }
}
```

### Переиспользование контейнеров (Reusable Containers)

Для ускорения тестов можно переиспользовать контейнеры между запусками:

```java
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withReuse(true);
```

Требуется настройка в `.testcontainers.properties`:
```properties
testcontainers.reuse.enable=true
```

> **Важно**: Контейнер не будет автоматически остановлен после теста

---

## Конфигурация и настройка

### Конфигурационный файл

Создайте файл `.testcontainers.properties` в корне проекта:

```properties
# Переиспользование контейнеров
testcontainers.reuse.enable=true

# Образ Ryuk для очистки контейнеров
ryuk.container.image=testcontainers/ryuk:0.5.1

# Проверка доступности Docker
checks.disable=false

# Таймаут запуска контейнера (секунды)
startup.timeout=300
```

### Переменные окружения

Testcontainers поддерживает настройку через переменные окружения:

```bash
# Использовать локальный Docker socket
export DOCKER_HOST=unix:///var/run/docker.sock

# Для Docker Desktop на Mac/Windows
export DOCKER_HOST=tcp://localhost:2375

# Включить режим отладки
export TESTCONTAINERS_DEBUG=true
```

### Использование локального Docker Registry

```java
GenericContainer<?> container = new GenericContainer<>(
    DockerImageName.parse("localhost:5000/myapp:latest")
        .asCompatibleSubstituteFor("myapp")
);
```

### Кастомизация Docker-образа

```java
GenericContainer<?> container = new GenericContainer<>("myapp:latest")
    .withImagePullPolicy(PullPolicy.alwaysPull())  // Всегда скачивать образ
    .withEnv("JAVA_OPTS", "-Xmx512m")
    .withCommand("--spring.profiles.active=test");
```

---

## Лучшие практики

### 1. Используйте специализированные модули

```java
// Хорошо
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

// Менее удобно
GenericContainer<?> postgres = new GenericContainer<>("postgres:15-alpine")
    .withExposedPorts(5432)
    .withEnv("POSTGRES_PASSWORD", "test");
```

### 2. Закрепляйте версии образов

```java
// Хорошо — явная версия
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15.5-alpine");

// Плохо — может измениться поведение при обновлении
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:latest");
```

### 3. Переиспользуйте контейнеры для класса

```java
@Testcontainers
class FastIntegrationTest {
    
    // Static контейнер — запускается один раз
    @Container
    private static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @BeforeEach
    void cleanDatabase() {
        // Очистка данных между тестами
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }
}
```

### 4. Используйте @DynamicPropertySource для Spring

```java
@SpringBootTest
@Testcontainers
class SpringIntegrationTest {
    
    @Container
    private static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:15-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void test() {
        // Spring автоматически подключится к Testcontainers БД
    }
}
```

### 5. Используйте init scripts для начальных данных

```java
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
    .withInitScript("schema.sql");  // Файл в src/test/resources
```

### 6. Настройте логирование

```java
import org.testcontainers.containers.output.Slf4jLogConsumer;

GenericContainer<?> app = new GenericContainer<>("myapp:latest")
    .withLogConsumer(new Slf4jLogConsumer(log));
```

### 7. Используйте профили для отключения в локальной разработке

```java
@SpringBootTest
@Testcontainers
@EnabledIf(expression = "${testcontainers.enabled:true}", 
    loadContext = true)
class ConditionalIntegrationTest {
    // Тесты запускаются только если testcontainers.enabled=true
}
```

### 8. Обрабатывайте ресурсы правильно

```java
// Плохо — контейнер может не остановиться
@Test
void badTest() {
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    postgres.start();
    // Если тест упадёт, контейнер не остановится
}

// Хорошо — используйте try-with-resources
@Test
void goodTest() {
    try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")) {
        postgres.start();
        // Контейнер гарантированно остановится
    }
}

// Лучше — используйте @Testcontainers и @Container
@Testcontainers
class BestTest {
    @Container
    private PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    // JUnit автоматически управляет жизненным циклом
}
```

### 9. Оптимизируйте время запуска тестов

```java
// Singleton контейнер для всех тестовых классов
public abstract class AbstractIntegrationTest {
    
    static final PostgreSQLContainer<?> POSTGRES;
    
    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withReuse(true);
        POSTGRES.start();
    }
    
    @BeforeEach
    void cleanDatabase() {
        // Очистка между тестами вместо пересоздания контейнера
    }
}
```

### 10. Документируйте зависимости от Docker

```java
/**
 * Интеграционные тесты для UserService.
 * 
 * Требования:
 * - Docker должен быть установлен и запущен
 * - Минимум 512 МБ RAM для контейнеров
 * - Порты 5432 и 6379 не должны быть заняты
 */
@Testcontainers
class UserServiceIntegrationTest {
    // тесты
}
```

---

## Вопросы для самопроверки

### Базовые вопросы

1. **Что такое Testcontainers и какую проблему он решает?**
   - Библиотека для запуска Docker-контейнеров из тестов
   - Решает проблемы зависимости от окружения, изоляции и воспроизводимости тестов
   - Позволяет тестировать с реальными зависимостями (БД, очереди и т.д.)

2. **Какие требования необходимы для работы Testcontainers?**
   - Установленный и запущенный Docker
   - Java 8+
   - Зависимость testcontainers в проекте

3. **В чём разница между GenericContainer и специализированными модулями?**
   - `GenericContainer` — универсальный класс для любого Docker-образа
   - Специализированные модули (PostgreSQLContainer, KafkaContainer) предоставляют удобные методы для конкретных технологий
   - Специализированные модули автоматически настраивают порты, переменные окружения и параметры подключения

4. **Как получить параметры подключения к контейнеру?**
   ```java
   String host = container.getHost();
   Integer port = container.getMappedPort(containerPort);
   String jdbcUrl = postgresContainer.getJdbcUrl();
   ```

5. **Что делает аннотация @Testcontainers?**
   - Автоматически управляет жизненным циклом контейнеров
   - Запускает контейнеры перед тестами и останавливает после
   - Работает в связке с аннотацией `@Container`

### Продвинутые вопросы

6. **В чём разница между static и non-static полем с @Container?**
   - Static — один контейнер на весь тестовый класс (быстрее)
   - Non-static — новый контейнер для каждого тестового метода (изоляция)

7. **Что такое Wait Strategies и зачем они нужны?**
   - Стратегии ожидания готовности контейнера
   - Проверяют HTTP endpoints, логи, порты, healthcheck
   - Предотвращают запуск тестов до полной инициализации сервиса

8. **Как организовать несколько контейнеров с общей сетью?**
   ```java
   Network network = Network.newNetwork();
   container1.withNetwork(network).withNetworkAliases("service1");
   container2.withNetwork(network).dependsOn(container1);
   ```

9. **Что такое Ryuk и для чего он используется?**
   - Ryuk — служебный контейнер для очистки ресурсов
   - Автоматически останавливает и удаляет контейнеры после тестов
   - Запускается Testcontainers автоматически

10. **Как использовать Testcontainers с Docker Compose?**
    ```java
    DockerComposeContainer<?> compose = new DockerComposeContainer<>(
        new File("docker-compose.yml")
    ).withExposedService("postgres", 5432);
    ```

### Практические вопросы

11. **Как ускорить выполнение интеграционных тестов с Testcontainers?**
    - Использовать static контейнеры для переиспользования
    - Включить reusable containers
    - Создать singleton контейнер для нескольких тестовых классов
    - Очищать данные между тестами вместо пересоздания контейнера
    - Использовать Alpine образы для меньшего размера

12. **Как интегрировать Testcontainers со Spring Boot?**
    ```java
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
    }
    ```

13. **Как передать файл конфигурации в контейнер?**
    ```java
    container.withCopyFileToContainer(
        MountableFile.forClasspathResource("config.yml"),
        "/app/config.yml"
    );
    ```

14. **Как выполнить SQL-скрипт при инициализации БД?**
    ```java
    PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withInitScript("schema.sql");
    ```

15. **Как получить логи контейнера для отладки?**
    ```java
    container.withLogConsumer(new Slf4jLogConsumer(log));
    // или
    String logs = container.getLogs();
    ```

### Вопросы на отладку

16. **Контейнер не запускается. Как диагностировать проблему?**
    - Проверить, запущен ли Docker: `docker ps`
    - Включить debug логирование: `export TESTCONTAINERS_DEBUG=true`
    - Проверить логи контейнера: `container.getLogs()`
    - Убедиться, что образ существует: `docker pull <image>`

17. **Тест падает с ошибкой "Port already in use". Что делать?**
    - Testcontainers автоматически маппит на свободные порты
    - Не указывайте фиксированные порты через `withFixedExposedPort()`
    - Используйте `getMappedPort()` для получения динамического порта

18. **Как очистить данные между тестами без пересоздания контейнера?**
    ```java
    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }
    ```

19. **Можно ли использовать Testcontainers в CI/CD?**
    - Да, если CI имеет доступ к Docker
    - GitHub Actions: использовать runner с Docker
    - GitLab CI: использовать Docker-in-Docker (dind)
    - Jenkins: убедиться, что Docker доступен на агенте

20. **Как обеспечить изоляцию тестов при использовании shared контейнера?**
    - Очищать данные в `@BeforeEach` или `@AfterEach`
    - Использовать транзакции с rollback
    - Создавать уникальные схемы/БД для каждого теста
    - При необходимости полной изоляции — использовать non-static контейнер

---

## Заключение

Testcontainers — это мощный инструмент для написания надёжных интеграционных тестов. Он позволяет тестировать приложения в условиях, максимально приближенных к production, обеспечивая при этом изоляцию и воспроизводимость. Правильное использование Testcontainers значительно повышает качество тестового покрытия и уверенность в стабильности приложения.

### Ключевые преимущества

- **Реалистичность**: тестирование с реальными зависимостями
- **Изоляция**: каждый тест в чистом окружении
- **Воспроизводимость**: одинаковое поведение везде
- **Простота**: минимальная настройка
- **Гибкость**: поддержка любых Docker-образов

### Когда использовать Testcontainers

- Интеграционное тестирование с базами данных
- Тестирование взаимодействия с очередями сообщений
- Тестирование микросервисной архитектуры
- End-to-end тестирование
- Тестирование миграций схемы БД
- Локальная разработка с реальными зависимостями

### Дополнительные ресурсы

- [Официальная документация Testcontainers](https://www.testcontainers.org/)
- [Testcontainers на GitHub](https://github.com/testcontainers/testcontainers-java)
- [Примеры использования](https://github.com/testcontainers/testcontainers-java/tree/main/examples)
- [Модули для различных технологий](https://www.testcontainers.org/modules/)
