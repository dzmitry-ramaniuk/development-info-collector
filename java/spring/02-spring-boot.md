# Spring Boot

Spring Boot — это фреймворк, построенный поверх Spring Framework, который радикально упрощает создание production-ready приложений за счёт автоконфигурации, встроенных серверов и принципа "convention over configuration". Цель Spring Boot — свести к минимуму XML-конфигурацию и boilerplate-код, позволяя разработчикам сосредоточиться на бизнес-логике.

**Историческая справка.** Spring Boot появился в 2014 году (версия 1.0) как ответ на растущую сложность конфигурирования Spring-приложений. До Spring Boot разработчикам приходилось вручную настраивать веб-контейнеры, источники данных, шаблонизаторы. Spring Boot автоматизировал эти процессы через "умные" значения по умолчанию и авто-конфигурацию, что ускорило разработку микросервисов и облачных приложений.

## Философия "Convention over Configuration"

Spring Boot следует принципу "соглашения важнее конфигурации": фреймворк предоставляет разумные настройки по умолчанию, которые работают в большинстве случаев. Разработчику нужно явно указывать только отклонения от стандартного поведения.

**Примеры соглашений:**
- Веб-приложение по умолчанию запускается на порту 8080
- Статические ресурсы ищутся в `/static`, `/public`, `/resources`, `/META-INF/resources`
- Шаблоны (Thymeleaf, FreeMarker) — в `/templates`
- Конфигурационные файлы — `application.properties` или `application.yml` в `/src/main/resources`
- Точка входа — класс с `@SpringBootApplication` и методом `main`

Эти соглашения можно переопределить через конфигурационные файлы или программно, но в большинстве случаев значения по умолчанию оптимальны.

## Автоконфигурация Spring Boot

**Автоконфигурация** (Auto-configuration) — ключевая особенность Spring Boot. При старте приложения фреймворк анализирует classpath, существующие бины и properties, затем автоматически конфигурирует необходимые компоненты.

### Механизм работы

1. **Сканирование classpath:** Spring Boot проверяет, какие библиотеки присутствуют в classpath (например, `spring-boot-starter-web`, `spring-boot-starter-data-jpa`)
2. **Условная регистрация бинов:** На основе найденных классов и конфигурации активируются соответствующие автоконфигурационные классы
3. **Применение условий:** Используются `@Conditional` аннотации для проверки условий (`@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnProperty`)
4. **Создание бинов по умолчанию:** Регистрируются бины с настройками по умолчанию, если пользователь не определил собственные

**Пример автоконфигурации DataSource:**

```java
@Configuration
@ConditionalOnClass({DataSource.class, EmbeddedDatabaseType.class})
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

Если в classpath есть драйвер БД и не определён пользовательский `DataSource`, Spring Boot создаст его автоматически, используя свойства из `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
```

### Управление автоконфигурацией

**Просмотр активных автоконфигураций:**

Запустите приложение с флагом `--debug` или установите `debug=true` в `application.properties`:

```
DEBUG=true java -jar myapp.jar
```

В логах появится раздел **AUTO-CONFIGURATION REPORT** с двумя секциями:
- **Positive matches** — активированные автоконфигурации
- **Negative matches** — отключённые (условия не выполнены)

**Исключение автоконфигураций:**

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

Или через `application.properties`:

```properties
spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
```

**Создание собственной автоконфигурации:**

1. Создайте класс конфигурации с `@Configuration`
2. Используйте `@ConditionalOn*` аннотации для условной активации
3. Зарегистрируйте класс в `META-INF/spring.factories`:

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.MyAutoConfiguration
```

Пример:

```java
@Configuration
@ConditionalOnClass(RedisTemplate.class)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        return template;
    }
}
```

## Spring Boot Starters

**Стартеры** — это набор зависимостей, которые упрощают добавление функциональности в проект. Вместо ручного указания множества библиотек достаточно добавить один стартер.

### Популярные стартеры

**Веб-приложения:**
- `spring-boot-starter-web` — REST API, Spring MVC, встроенный Tomcat
- `spring-boot-starter-webflux` — реактивные веб-приложения на Netty

**Работа с данными:**
- `spring-boot-starter-data-jpa` — JPA, Hibernate, транзакции
- `spring-boot-starter-data-mongodb` — работа с MongoDB
- `spring-boot-starter-data-redis` — интеграция с Redis
- `spring-boot-starter-jdbc` — работа с реляционными БД через JDBC

**Безопасность:**
- `spring-boot-starter-security` — Spring Security для аутентификации и авторизации

**Мониторинг и управление:**
- `spring-boot-starter-actuator` — эндпоинты для мониторинга, метрики, health checks

**Тестирование:**
- `spring-boot-starter-test` — JUnit 5, Mockito, AssertJ, Spring Test

**Messaging:**
- `spring-boot-starter-amqp` — RabbitMQ
- `spring-boot-starter-kafka` — Apache Kafka

**Шаблонизация:**
- `spring-boot-starter-thymeleaf` — серверная шаблонизация
- `spring-boot-starter-freemarker` — альтернативный шаблонизатор

### Структура стартера

Стартер — это обычная Maven/Gradle зависимость, которая транзитивно подтягивает необходимые библиотеки. Например, `spring-boot-starter-web` включает:
- `spring-boot-starter` (базовый стартер с логированием, автоконфигурацией)
- `spring-boot-starter-tomcat` (встроенный сервер)
- `spring-web`, `spring-webmvc` (Spring MVC)
- `jackson-databind` (сериализация JSON)

### Создание собственного стартера

**Когда создавать:**
- У вас есть переиспользуемая функциональность для нескольких проектов
- Нужна автоконфигурация библиотеки или сервиса
- Хотите упростить интеграцию внешней системы

**Структура проекта:**

1. **Модуль автоконфигурации** (`my-library-spring-boot-autoconfigure`) — содержит `@Configuration` классы и логику
2. **Модуль стартера** (`my-library-spring-boot-starter`) — зависимость-обёртка, которая включает автоконфигурацию и необходимые библиотеки

**Пример:**

Автоконфигурация:

```java
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyServiceProperties.class)
public class MyServiceAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyServiceProperties properties) {
        return new MyService(properties.getApiKey(), properties.getEndpoint());
    }
}
```

Properties:

```java
@ConfigurationProperties(prefix = "myservice")
public class MyServiceProperties {
    private String apiKey;
    private String endpoint = "https://api.example.com";
    
    // getters/setters
}
```

Регистрация в `META-INF/spring.factories`:

```
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.autoconfigure.MyServiceAutoConfiguration
```

Использование в проекте:

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-library-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```properties
myservice.api-key=secret123
myservice.endpoint=https://custom.api.example.com
```

## Структура Spring Boot приложения

### Типичная структура проекта

```
my-application/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/myapp/
│   │   │       ├── MyApplication.java          # Точка входа
│   │   │       ├── controller/                 # REST-контроллеры
│   │   │       ├── service/                    # Бизнес-логика
│   │   │       ├── repository/                 # JPA-репозитории
│   │   │       ├── model/                      # Domain-модели
│   │   │       ├── dto/                        # DTO для API
│   │   │       ├── config/                     # Конфигурационные классы
│   │   │       └── exception/                  # Обработчики исключений
│   │   └── resources/
│   │       ├── application.properties          # Основная конфигурация
│   │       ├── application-dev.properties      # Профиль dev
│   │       ├── application-prod.properties     # Профиль prod
│   │       ├── static/                         # Статические ресурсы (CSS, JS, images)
│   │       ├── templates/                      # Шаблоны (Thymeleaf, FreeMarker)
│   │       └── db/migration/                   # Flyway/Liquibase миграции
│   └── test/
│       └── java/
│           └── com/example/myapp/
│               ├── MyApplicationTests.java
│               ├── controller/
│               ├── service/
│               └── repository/
├── pom.xml / build.gradle
└── README.md
```

### Точка входа (@SpringBootApplication)

```java
@SpringBootApplication
public class MyApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

`@SpringBootApplication` — комбинированная аннотация, включающая:
- `@Configuration` — класс является источником бинов
- `@EnableAutoConfiguration` — включает автоконфигурацию
- `@ComponentScan` — сканирует текущий пакет и вложенные на наличие компонентов

### Настройка SpringApplication

Можно кастомизировать поведение через `SpringApplicationBuilder` или напрямую:

```java
public static void main(String[] args) {
    SpringApplication app = new SpringApplication(MyApplication.class);
    app.setBannerMode(Banner.Mode.OFF);
    app.setAdditionalProfiles("dev");
    app.setDefaultProperties(Collections.singletonMap("server.port", "8081"));
    app.run(args);
}
```

Или через builder:

```java
new SpringApplicationBuilder(MyApplication.class)
    .profiles("dev")
    .properties("server.port=8081")
    .run(args);
```

### Встроенные серверы

Spring Boot поддерживает несколько встроенных серверов:
- **Tomcat** (по умолчанию для servlet-приложений)
- **Jetty** (лёгкая альтернатива)
- **Undertow** (высокопроизводительный)
- **Netty** (для реактивных приложений WebFlux)

Смена сервера на Jetty:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jetty</artifactId>
</dependency>
```

## Профили и внешняя конфигурация

Spring Boot поддерживает гибкую систему конфигурации через файлы properties/YAML, переменные окружения, аргументы командной строки.

### Файлы конфигурации

**application.properties:**

```properties
spring.application.name=my-app
server.port=8080
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
spring.datasource.password=pass
logging.level.com.example=DEBUG
```

**application.yml** (более структурированный формат):

```yaml
spring:
  application:
    name: my-app
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass

server:
  port: 8080

logging:
  level:
    com.example: DEBUG
```

### Профили (Profiles)

Профили позволяют иметь различные конфигурации для разных окружений (dev, test, prod).

**Создание профиль-специфичных файлов:**
- `application-dev.properties`
- `application-test.properties`
- `application-prod.properties`

**Активация профиля:**

Через переменную окружения:
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar myapp.jar
```

Через аргумент командной строки:
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

В `application.properties`:
```properties
spring.profiles.active=dev
```

**Профиль-специфичные бины:**

```java
@Configuration
@Profile("dev")
public class DevConfig {
    
    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .build();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://prod-server:5432/mydb");
        return ds;
    }
}
```

### Приоритет источников конфигурации

Spring Boot загружает конфигурацию из множества источников в следующем порядке (от высшего к низшему приоритету):

1. Devtools global settings (`~/.spring-boot-devtools.properties`)
2. `@TestPropertySource` в тестах
3. `@SpringBootTest#properties`
4. Аргументы командной строки
5. Properties из `SPRING_APPLICATION_JSON`
6. `ServletConfig` init parameters
7. `ServletContext` init parameters
8. JNDI attributes
9. Java System properties (`System.getProperties()`)
10. OS environment variables
11. `RandomValuePropertySource` (только `random.*`)
12. Profile-specific application properties вне JAR (`application-{profile}.properties`)
13. Profile-specific application properties в JAR
14. Application properties вне JAR (`application.properties`)
15. Application properties в JAR
16. `@PropertySource` на `@Configuration` классах
17. Default properties (`SpringApplication.setDefaultProperties`)

**Важно:** Свойства из источников с более высоким приоритетом переопределяют значения из источников с низким приоритетом.

### Внешние конфигурационные файлы

Spring Boot ищет `application.properties` в следующих местах (по убыванию приоритета):
1. Подкаталог `/config` в текущей директории
2. Текущая директория
3. `classpath:/config/`
4. `classpath:/` (корень classpath)

Можно указать кастомное расположение:
```bash
java -jar myapp.jar --spring.config.location=file:/etc/myapp/application.properties
```

### Работа с конфигурацией в коде

**Через @Value:**

```java
@Service
public class MyService {
    
    @Value("${app.name}")
    private String appName;
    
    @Value("${app.max-users:100}") // значение по умолчанию
    private int maxUsers;
}
```

**Через @ConfigurationProperties (рекомендуется):**

```java
@ConfigurationProperties(prefix = "app")
@Component
public class AppProperties {
    
    private String name;
    private int maxUsers = 100;
    private Database database = new Database();
    
    // getters/setters
    
    public static class Database {
        private String url;
        private String username;
        // getters/setters
    }
}
```

```yaml
app:
  name: My Application
  max-users: 500
  database:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
```

Преимущества `@ConfigurationProperties`:
- Type-safety и валидация через JSR-303
- Группировка связанных свойств
- Поддержка вложенных структур
- IDE auto-completion

### Конфигурация через переменные окружения

Spring Boot автоматически преобразует переменные окружения в properties:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb
export SPRING_DATASOURCE_USERNAME=user
export APP_MAX_USERS=200
```

Соответствует:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/mydb
spring.datasource.username=user
app.max-users=200
```

**Правила преобразования:**
- Заглавные буквы → строчные
- `_` → `.`
- `__` → `-`

## Spring Boot Actuator

**Actuator** предоставляет production-ready функции для мониторинга, метрик, health checks и управления приложением.

### Подключение Actuator

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

### Основные эндпоинты

По умолчанию доступны на `/actuator`:

- `/actuator/health` — статус здоровья приложения (UP/DOWN)
- `/actuator/info` — метаинформация о приложении
- `/actuator/metrics` — метрики JVM, HTTP-запросов, БД
- `/actuator/env` — переменные окружения и properties
- `/actuator/beans` — список всех Spring-бинов
- `/actuator/mappings` — список всех HTTP-endpoint'ов
- `/actuator/configprops` — все `@ConfigurationProperties`
- `/actuator/loggers` — управление уровнями логирования
- `/actuator/threaddump` — дамп потоков JVM
- `/actuator/heapdump` — дамп heap памяти
- `/actuator/prometheus` — экспорт метрик для Prometheus

### Настройка доступа к эндпоинтам

По умолчанию публично доступны только `/health` и `/info`. Остальные требуют настройки:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
        # или include: '*' для всех
  endpoint:
    health:
      show-details: always
```

**Безопасность:** В production ограничьте доступ к Actuator через Spring Security:

```java
@Configuration
public class ActuatorSecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.requestMatcher(EndpointRequest.toAnyEndpoint())
            .authorizeRequests()
            .anyRequest().hasRole("ADMIN");
    }
}
```

### Custom Health Indicators

```java
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("validationQuery", "SELECT 1")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
        return Health.down().build();
    }
}
```

### Custom Metrics

С использованием Micrometer:

```java
@Service
public class OrderService {
    
    private final MeterRegistry meterRegistry;
    private final Counter orderCounter;
    
    public OrderService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.orderCounter = Counter.builder("orders.created")
            .description("Total orders created")
            .tag("type", "online")
            .register(meterRegistry);
    }
    
    public void createOrder(Order order) {
        // создание заказа
        orderCounter.increment();
        
        // метрика с таймером
        Timer.Sample sample = Timer.start(meterRegistry);
        // выполнение операции
        sample.stop(Timer.builder("order.processing.time")
            .register(meterRegistry));
    }
}
```

## Создание Executable JAR и развертывание

### Maven

Spring Boot Maven Plugin создаёт "fat JAR" со всеми зависимостями и встроенным сервером:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

Сборка:
```bash
mvn clean package
java -jar target/myapp-1.0.0.jar
```

### Gradle

```gradle
plugins {
    id 'org.springframework.boot' version '2.7.0'
}

bootJar {
    mainClass = 'com.example.MyApplication'
}
```

Сборка:
```bash
./gradlew bootJar
java -jar build/libs/myapp-1.0.0.jar
```

### Развертывание в виде WAR

Для развертывания на внешний сервер приложений (Tomcat, JBoss):

```java
@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {
    
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(MyApplication.class);
    }
    
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

```xml
<packaging>war</packaging>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-tomcat</artifactId>
    <scope>provided</scope>
</dependency>
```

### Layered JARs для Docker

С Spring Boot 2.3+ можно создавать слоёные JAR для оптимизации Docker-образов:

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
    <configuration>
        <layers>
            <enabled>true</enabled>
        </layers>
    </configuration>
</plugin>
```

Dockerfile:

```dockerfile
FROM openjdk:11-jre-slim as builder
WORKDIR application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

FROM openjdk:11-jre-slim
WORKDIR application
COPY --from=builder application/dependencies/ ./
COPY --from=builder application/spring-boot-loader/ ./
COPY --from=builder application/snapshot-dependencies/ ./
COPY --from=builder application/application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]
```

Преимущество: при изменении только кода приложения Docker кеширует слои с зависимостями, что ускоряет сборку.

## Работа с базами данных

### Автоконфигурация DataSource

При наличии драйвера БД в classpath Spring Boot автоматически конфигурирует `DataSource`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: user
    password: pass
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000
```

### Использование JPA/Hibernate

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
```

**Entity:**

```java
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String username;
    
    @Column(nullable = false)
    private String email;
    
    // getters/setters
}
```

**Repository:**

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    List<User> findByEmailContaining(String email);
}
```

### Инициализация БД

**schema.sql и data.sql:**

Spring Boot автоматически выполняет SQL-скрипты при старте:
- `schema.sql` — DDL для создания таблиц
- `data.sql` — DML для заполнения данных

```yaml
spring:
  sql:
    init:
      mode: always
      # или mode: embedded (только для H2, HSQL)
```

**Flyway Migration:**

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
```

Создайте файлы в `src/main/resources/db/migration/`:
- `V1__create_users_table.sql`
- `V2__add_email_column.sql`

**Liquibase:**

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml
```

## REST API с Spring Boot

### Создание REST-контроллера

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;
    
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public List<UserDto> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return userService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createUser(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }
    
    @PutMapping("/{id}")
    public UserDto updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }
}
```

### Валидация

```java
public class CreateUserRequest {
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank
    @Email
    private String email;
    
    @NotNull
    @Min(18)
    private Integer age;
    
    // getters/setters
}
```

### Обработка исключений

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(errors);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

### CORS Configuration

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3000")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

### Content Negotiation

Spring Boot автоматически поддерживает JSON (Jackson) и XML (если добавлен JAXB):

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```

Запрос с `Accept: application/xml` вернёт XML, с `Accept: application/json` — JSON.

## Безопасность с Spring Security

### Базовая настройка

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

При добавлении зависимости Spring Boot автоматически:
- Защищает все эндпоинты
- Генерирует пароль в консоли при старте
- Создаёт пользователя `user`

### Кастомная конфигурация

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeRequests()
                .antMatchers("/api/public/**").permitAll()
                .antMatchers("/api/admin/**").hasRole("ADMIN")
                .antMatchers("/api/**").authenticated()
            .and()
            .httpBasic()
            .and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
            .withUser("user")
                .password(passwordEncoder().encode("password"))
                .roles("USER")
            .and()
            .withUser("admin")
                .password(passwordEncoder().encode("admin"))
                .roles("USER", "ADMIN");
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### JWT Authentication

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration:3600000}")
    private long validityInMilliseconds;
    
    public String createToken(String username, List<String> roles) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put("roles", roles);
        
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);
        
        return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secretKey).parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public String getUsername(String token) {
        return Jwts.parser()
            .setSigningKey(secretKey)
            .parseClaimsJws(token)
            .getBody()
            .getSubject();
    }
}
```

## Тестирование Spring Boot приложений

### Unit-тесты сервисов

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    @Test
    void shouldFindUserById() {
        // given
        User user = new User(1L, "john", "john@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        
        // when
        Optional<UserDto> result = userService.findById(1L);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("john");
        verify(userRepository).findById(1L);
    }
}
```

### Интеграционные тесты контроллеров

```java
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldCreateUser() throws Exception {
        CreateUserRequest request = new CreateUserRequest("john", "john@example.com", 25);
        UserDto response = new UserDto(1L, "john", "john@example.com");
        
        when(userService.create(any())).thenReturn(response);
        
        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("john"));
    }
}
```

### Тестирование с реальной БД

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldSaveAndFindUser() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        
        User saved = userRepository.save(user);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }
}
```

### Тестирование только слоя данных

```java
@DataJpaTest
class UserRepositoryTest {
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void shouldFindByUsername() {
        User user = new User();
        user.setUsername("john");
        user.setEmail("john@example.com");
        entityManager.persist(user);
        entityManager.flush();
        
        Optional<User> found = userRepository.findByUsername("john");
        
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("john@example.com");
    }
}
```

### Тестирование только веб-слоя

```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UserService userService;
    
    @Test
    void shouldReturnUserById() throws Exception {
        UserDto user = new UserDto(1L, "john", "john@example.com");
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("john"));
    }
}
```

## Кеширование

### Включение кеширования

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

```java
@SpringBootApplication
@EnableCaching
public class MyApplication {
    // ...
}
```

### Использование кеша

```java
@Service
public class UserService {
    
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        // медленный запрос к БД
        return userRepository.findById(id).orElseThrow();
    }
    
    @CachePut(value = "users", key = "#user.id")
    public User update(User user) {
        return userRepository.save(user);
    }
    
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        userRepository.deleteById(id);
    }
    
    @CacheEvict(value = "users", allEntries = true)
    public void deleteAll() {
        userRepository.deleteAll();
    }
}
```

### Интеграция с Redis

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    password: secret
  cache:
    type: redis
    redis:
      time-to-live: 600000
```

## Асинхронная обработка

### Включение async

```java
@SpringBootApplication
@EnableAsync
public class MyApplication {
    // ...
}
```

### Асинхронные методы

```java
@Service
public class EmailService {
    
    @Async
    public CompletableFuture<String> sendEmail(String to, String subject, String body) {
        // отправка email
        try {
            Thread.sleep(5000); // симуляция долгой операции
            return CompletableFuture.completedFuture("Email sent to " + to);
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

### Настройка Executor

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {
    
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-");
        executor.initialize();
        return executor;
    }
    
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            System.err.println("Exception in async method: " + method.getName());
            ex.printStackTrace();
        };
    }
}
```

## Scheduled Tasks

### Включение планирования

```java
@SpringBootApplication
@EnableScheduling
public class MyApplication {
    // ...
}
```

### Запланированные задачи

```java
@Component
public class ScheduledTasks {
    
    @Scheduled(fixedRate = 5000)
    public void reportCurrentTime() {
        System.out.println("Current time: " + LocalDateTime.now());
    }
    
    @Scheduled(fixedDelay = 10000, initialDelay = 5000)
    public void cleanupOldData() {
        // очистка старых данных
    }
    
    @Scheduled(cron = "0 0 2 * * *") // каждый день в 02:00
    public void generateDailyReport() {
        // генерация отчёта
    }
}
```

## Практические упражнения

1. **Создайте простое REST API приложение** с CRUD операциями для управления задачами (Task). Используйте H2 для хранения данных.

2. **Настройте профили для разных окружений**: dev с H2, prod с PostgreSQL. Создайте профиль-специфичные конфигурационные файлы.

3. **Добавьте Spring Security** в приложение с JWT-аутентификацией. Реализуйте эндпоинты `/login` и `/register`.

4. **Создайте собственный стартер** для интеграции с внешним API (например, отправка SMS). Стартер должен автоматически конфигурировать клиент на основе properties.

5. **Добавьте Actuator** и создайте custom health indicator, который проверяет доступность внешнего сервиса.

6. **Реализуйте кеширование** для запросов к БД с использованием Redis. Настройте TTL и проверьте работу кеша.

7. **Создайте асинхронный сервис** для отправки email-уведомлений. Используйте `CompletableFuture` для композиции нескольких асинхронных операций.

8. **Напишите интеграционные тесты** с использованием Testcontainers для PostgreSQL. Проверьте работу всех слоёв приложения.

9. **Настройте миграции БД** с использованием Flyway или Liquibase. Создайте несколько версий схемы и проверьте корректность применения миграций.

10. **Соберите приложение в Docker-образ** с использованием layered JARs. Оптимизируйте Dockerfile для минимального размера образа.

## Вопросы на собеседовании

1. **Что такое Spring Boot и в чём его основные преимущества?**
   
   *Ответ:* Spring Boot — фреймворк поверх Spring Framework для упрощения создания production-ready приложений. Преимущества: автоконфигурация (меньше boilerplate), встроенные серверы (не нужен внешний Tomcat), стартеры (упрощённое управление зависимостями), Actuator (мониторинг из коробки), convention over configuration (разумные значения по умолчанию).

2. **Как работает механизм автоконфигурации Spring Boot?**
   
   *Ответ:* При старте Spring Boot сканирует classpath, находит автоконфигурационные классы (перечисленные в `META-INF/spring.factories`), применяет `@Conditional` аннотации для проверки условий (наличие классов, бинов, properties), затем регистрирует бины по умолчанию, если пользователь не определил собственные. Можно просмотреть активные автоконфигурации через `--debug` или исключить ненужные через `@SpringBootApplication(exclude=...)`.

3. **В чём разница между @Component, @Service, @Repository и @Controller?**
   
   *Ответ:* Все четыре аннотации помечают класс как Spring-бин для автоматической регистрации. Различие — семантическое: `@Component` — общий компонент, `@Service` — бизнес-логика, `@Repository` — доступ к данным (добавляет автоматическую трансляцию SQLException в DataAccessException), `@Controller` — веб-контроллер (MVC), `@RestController` = `@Controller` + `@ResponseBody` (для REST API).

4. **Что такое стартеры Spring Boot?**
   
   *Ответ:* Стартеры — предконфигурированные наборы зависимостей, которые упрощают подключение функциональности. Например, `spring-boot-starter-web` включает Spring MVC, Tomcat, Jackson. Вместо ручного добавления десятков зависимостей достаточно одного стартера. Можно создавать собственные стартеры для переиспользуемой функциональности.

5. **Какие способы внешней конфигурации поддерживает Spring Boot?**
   
   *Ответ:* application.properties/yml, переменные окружения, аргументы командной строки, SPRING_APPLICATION_JSON, JNDI, System properties, профиль-специфичные файлы, @PropertySource, external config location. Приоритет от высшего к низшему: командная строка → переменные окружения → application.properties → default properties.

6. **Как работают профили в Spring Boot?**
   
   *Ответ:* Профили позволяют активировать разные конфигурации для разных окружений (dev, test, prod). Создаются файлы `application-{profile}.properties`, активируются через `spring.profiles.active`. Можно использовать `@Profile` на бинах и `@Configuration` классах для условной регистрации компонентов в зависимости от активного профиля.

7. **Что такое Spring Boot Actuator и для чего он используется?**
   
   *Ответ:* Actuator — модуль для production-мониторинга и управления приложением. Предоставляет HTTP-эндпоинты для health checks (`/actuator/health`), метрик JVM/HTTP (`/actuator/metrics`), информации о бинах (`/actuator/beans`), управления логированием (`/actuator/loggers`), экспорта в Prometheus. Критичные эндпоинты должны быть защищены через Spring Security.

8. **Как Spring Boot создаёт executable JAR?**
   
   *Ответ:* Spring Boot Maven/Gradle Plugin упаковывает приложение, все зависимости и встроенный сервер в один "fat JAR". Используется кастомный ClassLoader (`LaunchedURLClassLoader`) для загрузки классов из вложенных JAR. Точка входа — `JarLauncher` или `WarLauncher`, который затем запускает пользовательский `main` метод. С версии 2.3 поддерживаются layered JARs для оптимизации Docker-образов.

9. **В чём разница между @SpringBootApplication и комбинацией @Configuration + @EnableAutoConfiguration + @ComponentScan?**
   
   *Ответ:* `@SpringBootApplication` — мета-аннотация, объединяющая три аннотации: `@Configuration` (класс является источником бинов), `@EnableAutoConfiguration` (включает автоконфигурацию), `@ComponentScan` (сканирует текущий пакет и вложенные). Функционально эквивалентны, `@SpringBootApplication` просто сокращает код.

10. **Как переопределить автоконфигурацию Spring Boot?**
    
    *Ответ:* Несколько способов: 1) Создать собственный бин с тем же именем/типом (автоконфигурация использует `@ConditionalOnMissingBean`), 2) Изменить properties (`spring.datasource.*`, `server.port`), 3) Исключить автоконфигурацию через `@SpringBootApplication(exclude=...)` или `spring.autoconfigure.exclude`, 4) Создать `@Configuration` класс с более высоким приоритетом.

11. **Что такое @ConfigurationProperties и когда его использовать?**
    
    *Ответ:* `@ConfigurationProperties` связывает группу properties с Java-объектом (POJO), обеспечивая type-safety, валидацию через JSR-303, вложенные структуры, IDE auto-completion. Используется для группировки связанных настроек (например, все настройки БД в `DatabaseProperties`). Предпочтительнее `@Value` для сложных конфигураций.

12. **Как реализовать graceful shutdown в Spring Boot?**
    
    *Ответ:* С Spring Boot 2.3+:
    ```yaml
    server:
      shutdown: graceful
    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s
    ```
    При получении SIGTERM сервер перестаёт принимать новые запросы, завершает обработку текущих в течение заданного таймаута, затем останавливается. Для старых версий нужно вручную реализовывать через `ApplicationListener<ContextClosedEvent>`.

13. **Как работает @Transactional в Spring Boot?**
    
    *Ответ:* `@Transactional` использует AOP для создания прокси, который начинает транзакцию перед вызовом метода и коммитит/откатывает после. Spring Boot автоматически конфигурирует `PlatformTransactionManager` (обычно `JpaTransactionManager` при наличии JPA). Аннотация может применяться к методам и классам. Параметры: `propagation` (поведение вложенных транзакций), `isolation` (уровень изоляции), `readOnly`, `timeout`, `rollbackFor`.

14. **Как протестировать Spring Boot приложение?**
    
    *Ответ:* Spring Boot предоставляет аннотации для различных типов тестов: `@SpringBootTest` (полный контекст), `@WebMvcTest` (только веб-слой), `@DataJpaTest` (только JPA), `@MockBean` (мокирование в контексте). Для интеграционных тестов с реальной БД используется Testcontainers. Для unit-тестов — обычный JUnit + Mockito без Spring.

15. **Что такое CommandLineRunner и ApplicationRunner?**
    
    *Ответ:* Интерфейсы для выполнения кода после старта приложения. `CommandLineRunner.run(String... args)` принимает аргументы как String массив, `ApplicationRunner.run(ApplicationArguments args)` — как типизированный объект с доступом к опциям. Используются для инициализации данных, выполнения миграций, warming up кеша. Можно управлять порядком выполнения через `@Order`.

16. **Как Spring Boot интегрируется с внешними системами конфигурации (Spring Cloud Config)?**
    
    *Ответ:* Spring Boot может загружать конфигурацию из внешнего Config Server при старте через `bootstrap.properties`:
    ```properties
    spring.cloud.config.uri=http://config-server:8888
    spring.application.name=my-service
    ```
    Config Server хранит конфигурацию в Git, приложения получают профиль-специфичные properties при старте. Поддерживается обновление конфигурации без рестарта через `/actuator/refresh` или Spring Cloud Bus.

17. **Как реализовать health check для зависимых сервисов?**
    
    *Ответ:* Создать кастомный `HealthIndicator`:
    ```java
    @Component
    public class ExternalServiceHealthIndicator implements HealthIndicator {
        @Autowired
        private RestTemplate restTemplate;
        
        @Override
        public Health health() {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(
                    "https://external-api.com/health", String.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    return Health.up().withDetail("service", "available").build();
                }
            } catch (Exception e) {
                return Health.down().withException(e).build();
            }
            return Health.down().build();
        }
    }
    ```

18. **Какие лучшие практики развертывания Spring Boot приложений?**
    
    *Ответ:* 1) Использовать executable JAR с встроенным сервером, 2) Применять layered JARs для Docker, 3) Не хранить secrets в application.properties (использовать environment variables или Vault), 4) Настроить graceful shutdown, 5) Включить Actuator с ограниченным доступом, 6) Использовать внешнюю конфигурацию (Config Server), 7) Логировать в JSON для централизованной агрегации, 8) Настроить health checks для оркестраторов (Kubernetes), 9) Ограничить ресурсы JVM (`-Xmx`, `-XX:MaxMetaspaceSize`).

19. **Как оптимизировать время старта Spring Boot приложения?**
    
    *Ответ:* 1) Использовать ленивую инициализацию (`spring.main.lazy-initialization=true`), 2) Исключить ненужные автоконфигурации, 3) Уменьшить количество сканируемых пакетов через `@ComponentScan(basePackages)`, 4) Использовать Spring Native / GraalVM для AOT-компиляции, 5) Отключить неиспользуемые стартеры, 6) Настроить индексирование компонентов через `spring-context-indexer`, 7) Профилировать через Spring Boot Actuator `/actuator/startup`.

20. **В чём разница между spring-boot-starter-parent и dependency management через BOM?**
    
    *Ответ:* `spring-boot-starter-parent` — родительский POM, который предоставляет dependency management, плагины, настройки компилятора, encoding. Наследование через `<parent>`. BOM (Bill of Materials) — альтернативный способ через `<dependencyManagement>` + `<scope>import</scope>`, позволяет использовать свой собственный parent POM. Функционально похожи, но BOM более гибкий при наличии корпоративного parent POM.

## Дополнительные материалы

- [Spring Boot Reference Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Boot Guides](https://spring.io/guides) — пошаговые туториалы
- Книга "Spring Boot in Action" by Craig Walls — практическое руководство
- Книга "Cloud Native Java" by Josh Long — микросервисы и облачные приложения
- [Baeldung Spring Boot Tutorials](https://www.baeldung.com/spring-boot) — подробные статьи
- [Spring Boot Actuator Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [12 Factor App](https://12factor.net/) — методология для cloud-native приложений
