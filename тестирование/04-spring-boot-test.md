# Spring Boot Test — тестирование Spring-приложений

Spring Boot Test — мощный инструмент для интеграционного тестирования Spring-приложений. Он позволяет поднимать только нужные части контекста и тестировать взаимодействие между компонентами без полного запуска приложения.

## Содержание

1. [Введение и зависимости](#введение-и-зависимости)
2. [Основные типы тестов](#основные-типы-тестов)
3. [@SpringBootTest — полный контекст](#springboottest--полный-контекст)
4. [@WebMvcTest — тестирование контроллеров](#webmvctest--тестирование-контроллеров)
5. [@DataJpaTest — тестирование JPA](#datajpatest--тестирование-jpa)
6. [@JsonTest — тестирование сериализации](#jsontest--тестирование-сериализации)
7. [@JdbcTest — тестирование JDBC](#jdbctest--тестирование-jdbc)
8. [Testcontainers интеграция](#testcontainers-интеграция)
9. [Тестирование REST API](#тестирование-rest-api)
10. [Практические советы](#практические-советы)
11. [Вопросы для самопроверки](#вопросы-для-самопроверки)

---

## Введение и зависимости

### Maven зависимость

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Spring Boot Starter Test включает:
- **JUnit 5** (Jupiter)
- **Spring Test** & **Spring Boot Test**
- **AssertJ** — fluent assertions
- **Hamcrest** — матчеры
- **Mockito** — моки
- **JSONassert** — JSON assertions
- **JsonPath** — запросы к JSON

### Gradle зависимость

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}
```

---

## Основные типы тестов

Spring Boot предоставляет специализированные аннотации для slice-тестирования — подъёма только нужных частей контекста.

| Аннотация         | Назначение                                                                  | Что поднимается                                |
| ----------------- | --------------------------------------------------------------------------- | ---------------------------------------------- |
| `@SpringBootTest` | Поднимает полный контекст Spring Boot (медленно, но удобно для интеграции) | Весь application context                       |
| `@WebMvcTest`     | Поднимает только веб-слой (контроллеры, конвертеры, валидация)             | Controllers, ControllerAdvice, Filters         |
| `@DataJpaTest`    | Поднимает слой JPA, использует встроенную БД или Testcontainers             | JPA repositories, EntityManager                |
| `@JsonTest`       | Проверяет сериализацию/десериализацию JSON через Jackson                    | Jackson ObjectMapper                           |
| `@JdbcTest`       | Для тестирования JDBC-запросов без JPA                                      | DataSource, JdbcTemplate                       |
| `@RestClientTest` | Тестирование REST-клиентов                                                  | RestTemplate, MockRestServiceServer            |

---

## @SpringBootTest — полный контекст

`@SpringBootTest` поднимает полный Spring Boot application context. Используется для интеграционных тестов.

### Базовый пример

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApplicationIntegrationTest {

    @Autowired
    private UserService userService;

    @Test
    void contextLoads() {
        assertNotNull(userService);
    }

    @Test
    void shouldCreateUser() {
        User user = userService.createUser("john@example.com", "John");
        assertNotNull(user.getId());
        assertEquals("John", user.getName());
    }
}
```

### Режимы запуска веб-сервера

```java
// Не запускать веб-сервер (по умолчанию)
@SpringBootTest(webEnvironment = WebEnvironment.NONE)

// Случайный порт (рекомендуется)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)

// Определённый порт
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)

// Mock окружение (MockMvc)
@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
```

### Пример с RANDOM_PORT и TestRestTemplate

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGetUser() {
        String url = "http://localhost:" + port + "/api/users/1";
        
        ResponseEntity<User> response = restTemplate.getForEntity(url, User.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void shouldCreateUser() {
        String url = "http://localhost:" + port + "/api/users";
        User newUser = new User("alice@example.com", "Alice");
        
        ResponseEntity<User> response = restTemplate.postForEntity(url, newUser, User.class);
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }
}
```

### Переопределение свойств

```java
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "logging.level.com.example=DEBUG"
})
class CustomPropertiesTest {
    // ...
}
```

### @MockBean и @SpyBean

```java
@SpringBootTest
class UserServiceTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Test
    void shouldFindUser() {
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(new User(1L, "John")));

        User user = userService.getUser(1L);
        assertEquals("John", user.getName());
    }
}
```

---

## @WebMvcTest — тестирование контроллеров

`@WebMvcTest` поднимает только веб-слой, без полного контекста. Все зависимости нужно мокать.

### Базовый пример

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void shouldGetUser() throws Exception {
        when(userService.getUser(1L))
            .thenReturn(new User(1L, "John", "john@example.com"));

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("John"))
            .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService).getUser(1L);
    }

    @Test
    void shouldCreateUser() throws Exception {
        User newUser = new User(null, "Alice", "alice@example.com");
        User savedUser = new User(1L, "Alice", "alice@example.com");
        
        when(userService.createUser("alice@example.com", "Alice"))
            .thenReturn(savedUser);

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "alice@example.com",
                        "name": "Alice"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"));

        verify(userService).createUser("alice@example.com", "Alice");
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(userService.getUser(999L))
            .thenThrow(new UserNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound());
    }
}
```

### Тестирование валидации

```java
@Test
void shouldValidateUserInput() throws Exception {
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "invalid-email",
                    "name": ""
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors").isArray());
}
```

### Тестирование с аутентификацией

Добавить зависимость:
```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Пример:
```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_withAdminRole_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void adminEndpoint_withUserRole_shouldFail() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withoutAuth_shouldFail() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
            .andExpect(status().isUnauthorized());
    }
}
```

---

## @DataJpaTest — тестирование JPA

`@DataJpaTest` поднимает только JPA-слой с встроенной БД (H2) или Testcontainers.

### Базовый пример

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {
        User user = new User(null, "John", "john@example.com");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("John", found.get().getName());
    }

    @Test
    void shouldFindByEmail() {
        User user = new User(null, "Alice", "alice@example.com");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("alice@example.com");
        assertTrue(found.isPresent());
        assertEquals("Alice", found.get().getName());
    }

    @Test
    void shouldDeleteUser() {
        User user = new User(null, "Bob", "bob@example.com");
        User saved = userRepository.save(user);

        userRepository.deleteById(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertFalse(found.isPresent());
    }
}
```

### Использование TestEntityManager

```java
@DataJpaTest
class UserRepositoryAdvancedTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldPersistUser() {
        User user = new User(null, "John", "john@example.com");
        User saved = entityManager.persistAndFlush(user);

        assertNotNull(saved.getId());

        User found = entityManager.find(User.class, saved.getId());
        assertEquals("John", found.getName());
    }
}
```

### Rollback автоматический

По умолчанию все изменения откатываются после каждого теста:

```java
@DataJpaTest
class TransactionRollbackTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void firstTest() {
        userRepository.save(new User(null, "Test", "test@example.com"));
        assertEquals(1, userRepository.count());
    }

    @Test
    void secondTest() {
        // БД пустая, данные из firstTest откатились
        assertEquals(0, userRepository.count());
    }
}
```

---

## @JsonTest — тестирование сериализации

`@JsonTest` позволяет тестировать Jackson сериализацию/десериализацию.

### Базовый пример

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class UserDtoJsonTest {

    @Autowired
    private JacksonTester<UserDto> json;

    @Test
    void shouldSerialize() throws Exception {
        UserDto dto = new UserDto("alice@example.com", "Alice", 30);

        assertThat(json.write(dto))
            .extractingJsonPathStringValue("$.email")
            .isEqualTo("alice@example.com");

        assertThat(json.write(dto))
            .extractingJsonPathStringValue("$.name")
            .isEqualTo("Alice");

        assertThat(json.write(dto))
            .extractingJsonPathNumberValue("$.age")
            .isEqualTo(30);
    }

    @Test
    void shouldDeserialize() throws Exception {
        String content = """
            {
                "email": "alice@example.com",
                "name": "Alice",
                "age": 30
            }
            """;

        UserDto dto = json.parse(content).getObject();

        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getName()).isEqualTo("Alice");
        assertThat(dto.getAge()).isEqualTo(30);
    }

    @Test
    void shouldSerializeToExpectedJson() throws Exception {
        UserDto dto = new UserDto("alice@example.com", "Alice", 30);

        assertThat(json.write(dto)).isEqualToJson("""
            {
                "email": "alice@example.com",
                "name": "Alice",
                "age": 30
            }
            """);
    }
}
```

### Тестирование дат и специальных форматов

```java
@JsonTest
class DateSerializationTest {

    @Autowired
    private JacksonTester<Event> json;

    @Test
    void shouldSerializeLocalDateTime() throws Exception {
        Event event = new Event("Meeting", LocalDateTime.of(2024, 1, 15, 10, 30));

        assertThat(json.write(event))
            .extractingJsonPathStringValue("$.timestamp")
            .isEqualTo("2024-01-15T10:30:00");
    }
}
```

---

## @JdbcTest — тестирование JDBC

`@JdbcTest` для тестирования прямых JDBC-запросов без JPA.

### Базовый пример

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
class UserJdbcRepositoryTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldInsertAndSelect() {
        jdbcTemplate.update(
            "INSERT INTO users (name, email) VALUES (?, ?)",
            "John", "john@example.com"
        );

        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE email = ?",
            Integer.class,
            "john@example.com"
        );

        assertEquals(1, count);
    }
}
```

---

## Testcontainers интеграция

### Зависимость

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

### Пример с PostgreSQL

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
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
    void shouldSaveUser() {
        User user = new User(null, "Alice", "alice@example.com");
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
        assertTrue(userRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void shouldFindByEmail() {
        User user = new User(null, "Bob", "bob@example.com");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("bob@example.com");
        assertTrue(found.isPresent());
        assertEquals("Bob", found.get().getName());
    }
}
```

### @SpringBootTest с Testcontainers

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class FullIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateAndRetrieveUser() {
        // Create
        User newUser = new User(null, "Alice", "alice@example.com");
        ResponseEntity<User> createResponse = restTemplate.postForEntity(
            "/api/users", newUser, User.class
        );
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        Long userId = createResponse.getBody().getId();

        // Retrieve
        ResponseEntity<User> getResponse = restTemplate.getForEntity(
            "/api/users/" + userId, User.class
        );
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertEquals("Alice", getResponse.getBody().getName());
    }
}
```

---

## Тестирование REST API

### TestRestTemplate

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RestApiTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldGetUser() {
        ResponseEntity<User> response = restTemplate.getForEntity("/api/users/1", User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldHandleNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

### WebTestClient (для WebFlux)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class WebTestClientTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldGetUser() {
        webTestClient.get()
            .uri("/api/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.name").isEqualTo("John");
    }
}
```

---

## Практические советы

### 1. Группируй тесты по слоям

```java
// Юнит-тесты
@ExtendWith(MockitoExtension.class)
class UserServiceTest { }

// Slice-тесты
@WebMvcTest(UserController.class)
class UserControllerTest { }

@DataJpaTest
class UserRepositoryTest { }

// Интеграционные тесты
@SpringBootTest
class UserIntegrationTest { }
```

### 2. Избегай @DirtiesContext

```java
// ПЛОХО: сбрасывает весь контекст
@DirtiesContext
class SlowTest { }

// ХОРОШО: используй транзакции и rollback
@DataJpaTest
class FastTest { }
```

### 3. Используй профили для тестов

```java
@SpringBootTest
@ActiveProfiles("test")
class TestWithProfile { }
```

application-test.yml:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    show-sql: true
```

### 4. Для тестов БД — Testcontainers

```java
// ПЛОХО: H2 ведёт себя иначе, чем PostgreSQL
@DataJpaTest
class H2Test { }

// ХОРОШО: реальная БД
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = NONE)
class PostgresTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### 5. Для REST API — TestRestTemplate или MockMvc

```java
// Для интеграционных тестов с реальным сервером
@SpringBootTest(webEnvironment = RANDOM_PORT)
class IntegrationTest {
    @Autowired TestRestTemplate restTemplate;
}

// Для slice-тестов контроллеров
@WebMvcTest(UserController.class)
class ControllerTest {
    @Autowired MockMvc mockMvc;
}
```

### 6. Для асинхронных операций — Awaitility

```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

```java
import static org.awaitility.Awaitility.*;

@Test
void shouldProcessAsync() {
    service.processAsync();

    await().atMost(Duration.ofSeconds(5))
        .until(() -> service.isCompleted());
}
```

### 7. Кешируй контекст между тестами

```java
// Контекст будет переиспользован для тестов с одинаковой конфигурацией
@SpringBootTest
class Test1 { }

@SpringBootTest
class Test2 { }
```

### 8. Используй @Sql для подготовки данных

```java
@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @Sql("/test-data.sql")
    void shouldFindPreparedUsers() {
        assertEquals(5, userRepository.count());
    }
}
```

test-data.sql:
```sql
INSERT INTO users (name, email) VALUES ('Alice', 'alice@example.com');
INSERT INTO users (name, email) VALUES ('Bob', 'bob@example.com');
-- ...
```

---

## Вопросы для самопроверки

### Базовые вопросы

1. **В чём разница между @SpringBootTest и @WebMvcTest?**
   - @SpringBootTest поднимает полный контекст (медленно)
   - @WebMvcTest поднимает только веб-слой (быстро)

2. **Что делает @MockBean?**
   - Подменяет бин в Spring контексте на мок

3. **Зачем нужен @DataJpaTest?**
   - Для тестирования JPA-репозиториев с rollback транзакциями

4. **Как отключить встроенную БД H2 в тестах?**
   - Использовать @AutoConfigureTestDatabase(replace = NONE) и Testcontainers

5. **Что делает @DynamicPropertySource?**
   - Позволяет динамически передавать свойства контейнеров в контекст Spring

### Продвинутые вопросы

6. **Как тестировать REST API с реальным сервером?**
   - @SpringBootTest(webEnvironment = RANDOM_PORT) + TestRestTemplate

7. **Почему @DirtiesContext вреден?**
   - Он пересоздаёт контекст, что делает тесты медленными

8. **Как протестировать асинхронный код?**
   - Awaitility или CompletableFuture.join()

9. **Как тестировать защищённые эндпоинты?**
   - spring-security-test + @WithMockUser

10. **Что такое slice-тесты?**
    - Тесты, поднимающие только часть контекста (@WebMvcTest, @DataJpaTest)

### Практические вопросы

11. **Как избежать медленных тестов?**
    - Используйте slice-тесты вместо @SpringBootTest
    - Кешируйте контекст
    - Избегайте @DirtiesContext

12. **Как тестировать валидацию?**
    - MockMvc.perform() + .andExpect(status().isBadRequest())

13. **Как подготовить тестовые данные для БД?**
    - @Sql, TestEntityManager, или setUp() методы

14. **Как тестировать JSON сериализацию?**
    - @JsonTest + JacksonTester

15. **Как тестировать с реальной БД?**
    - Testcontainers + @DynamicPropertySource

---

## Заключение

Spring Boot Test предоставляет мощные инструменты для тестирования на всех уровнях: от юнит-тестов с Mockito до интеграционных тестов с реальными БД. Правильное использование slice-тестов (@WebMvcTest, @DataJpaTest) и Testcontainers позволяет создавать быстрые и надёжные тесты.

### Дополнительные ресурсы

- [Spring Boot Testing Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Spring Framework Testing](https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html)
- [Testcontainers Spring Boot](https://www.testcontainers.org/modules/databases/jdbc/)
- [Baeldung: Testing in Spring Boot](https://www.baeldung.com/spring-boot-testing)

---

[← Назад к разделу Тестирование](README.md)
