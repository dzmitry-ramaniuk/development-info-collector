# Лучшие практики и типичные ошибки в тестировании

Даже опытные инженеры совершают ошибки при написании тестов. Этот раздел содержит проверенные рекомендации и типичные антипаттерны, которых следует избегать.

## Содержание

1. [Общие принципы тестирования](#общие-принципы-тестирования)
2. [Типичные ошибки](#типичные-ошибки)
3. [Антипаттерны в тестировании](#антипаттерны-в-тестировании)
4. [Best practices](#best-practices)
5. [Организация тестового кода](#организация-тестового-кода)
6. [Производительность тестов](#производительность-тестов)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

---

## Общие принципы тестирования

### Пирамида тестирования

```
        /\
       /  \  E2E Tests (мало, медленно)
      /----\
     /      \ Integration Tests (умеренно)
    /--------\
   /          \ Unit Tests (много, быстро)
  /------------\
```

**Принцип:**
- **Юнит-тесты** (70%): быстрые, изолированные, много
- **Интеграционные** (20%): взаимодействие компонентов
- **E2E тесты** (10%): полный сценарий, медленные

### F.I.R.S.T. принципы

- **Fast** — быстрые (миллисекунды)
- **Independent** — независимые друг от друга
- **Repeatable** — повторяемые (одинаковый результат)
- **Self-validating** — самопроверяющиеся (pass/fail)
- **Timely** — своевременные (пишутся вместе с кодом)

### Arrange-Act-Assert (AAA)

```java
@Test
void shouldCalculateDiscount() {
    // Arrange — подготовка
    PriceCalculator calculator = new PriceCalculator();
    Product product = new Product(100.0);
    User premiumUser = new User(true);
    
    // Act — действие
    double finalPrice = calculator.calculatePrice(product, premiumUser);
    
    // Assert — проверка
    assertEquals(80.0, finalPrice); // 20% скидка
}
```

---

## Типичные ошибки

### 1. Избыточные моки

**Проблема:** тесты становятся хрупкими и привязанными к реализации.

```java
// ПЛОХО: проверяем детали реализации
@Test
void badTest() {
    service.createUser("alice@example.com");
    
    verify(repo, times(1)).save(any());
    verify(logger, times(1)).info(anyString());
    verify(cache, times(1)).put(anyString(), any());
}

// ХОРОШО: проверяем результат
@Test
void goodTest() {
    User user = service.createUser("alice@example.com");
    
    assertNotNull(user);
    assertEquals("alice@example.com", user.getEmail());
    assertTrue(user.getId() > 0);
}
```

**Правило:** Проверяй результат, а не внутреннее поведение.

---

### 2. Использование H2 вместо реальной БД

**Проблема:** поведение H2 отличается от PostgreSQL/MySQL.

```java
// ПЛОХО: H2 может вести себя иначе
@DataJpaTest
class H2Test {
    // Используется H2 по умолчанию
}

// ХОРОШО: Testcontainers с реальной БД
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PostgresTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = 
        new PostgreSQLContainer<>("postgres:16-alpine");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

**Проблемы с H2:**
- Отличия в SQL-диалектах
- Различия в транзакционном поведении
- Отсутствие специфичных функций PostgreSQL/MySQL
- Различия в индексах и планах выполнения

---

### 3. Thread.sleep() в тестах

**Проблема:** замедляет тесты и создаёт нестабильность (flaky tests).

```java
// ПЛОХО: непредсказуемо
@Test
void badTest() throws InterruptedException {
    service.processAsync();
    Thread.sleep(5000); // А что если обработка займёт 6 секунд?
    assertTrue(service.isCompleted());
}

// ХОРОШО: Awaitility
@Test
void goodTest() {
    service.processAsync();
    
    await().atMost(Duration.ofSeconds(10))
           .pollInterval(Duration.ofMillis(100))
           .until(() -> service.isCompleted());
}
```

**Зависимость:**
```xml
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.0</version>
    <scope>test</scope>
</dependency>
```

---

### 4. Непредсказуемость времени и UUID

**Проблема:** тесты падают из-за различий во времени/случайных значениях.

```java
// ПЛОХО: зависимость от системного времени
class BadService {
    public Event createEvent() {
        return new Event(UUID.randomUUID(), LocalDateTime.now());
    }
}

// ХОРОШО: внедряем зависимости
class GoodService {
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    
    public GoodService(Clock clock, Supplier<UUID> uuidSupplier) {
        this.clock = clock;
        this.uuidSupplier = uuidSupplier;
    }
    
    public Event createEvent() {
        return new Event(
            uuidSupplier.get(), 
            LocalDateTime.now(clock)
        );
    }
}

// Тест
@Test
void testCreateEvent() {
    UUID fixedUuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    Clock fixedClock = Clock.fixed(
        Instant.parse("2025-01-01T00:00:00Z"), 
        ZoneOffset.UTC
    );
    
    GoodService service = new GoodService(
        fixedClock, 
        () -> fixedUuid
    );
    
    Event event = service.createEvent();
    
    assertEquals(fixedUuid, event.getId());
    assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), event.getTimestamp());
}
```

---

### 5. Отсутствие очистки данных между тестами

**Проблема:** данные одного теста влияют на другой.

```java
// ПЛОХО: тесты зависят друг от друга
@SpringBootTest
class BadTests {
    @Autowired UserRepository repo;
    
    @Test
    void firstTest() {
        repo.save(new User("alice@example.com"));
        assertEquals(1, repo.count());
    }
    
    @Test
    void secondTest() {
        // Может упасть, если firstTest выполнился раньше!
        assertEquals(0, repo.count());
    }
}

// ХОРОШО: очистка после каждого теста
@SpringBootTest
class GoodTests {
    @Autowired UserRepository repo;
    
    @AfterEach
    void cleanup() {
        repo.deleteAll();
    }
    
    @Test
    void firstTest() {
        repo.save(new User("alice@example.com"));
        assertEquals(1, repo.count());
    }
    
    @Test
    void secondTest() {
        assertEquals(0, repo.count()); // Всегда проходит
    }
}
```

**Для @DataJpaTest:** транзакции автоматически откатываются.

---

### 6. @DirtiesContext без необходимости

**Проблема:** сбрасывает Spring context → тесты медленные.

```java
// ПЛОХО: контекст пересоздаётся каждый раз
@SpringBootTest
@DirtiesContext
class SlowTest {
    @Test void test1() { }
    @Test void test2() { }
}

// ХОРОШО: используй точечную очистку данных
@SpringBootTest
class FastTest {
    @Autowired UserRepository repo;
    
    @AfterEach
    void cleanup() {
        repo.deleteAll(); // Только данные, не весь контекст
    }
    
    @Test void test1() { }
    @Test void test2() { }
}
```

---

### 7. Верификация вызовов без нужды

**Проблема:** тест проверяет, что метод вызван, хотя достаточно проверить результат.

```java
// ПЛОХО: хрупкий тест
@Test
void badTest() {
    service.createUser("alice@example.com");
    
    verify(repo).save(any(User.class));
    verify(emailSender).send(eq("alice@example.com"), anyString());
}

// ХОРОШО: проверяем результат
@Test
void goodTest() {
    User user = service.createUser("alice@example.com");
    
    assertNotNull(user);
    assertEquals("alice@example.com", user.getEmail());
    // verify() только для критичных взаимодействий с внешними системами
}
```

**Когда использовать verify():**
- Отправка email/SMS
- Публикация событий в Kafka
- Запись в audit log
- Вызовы внешних API

---

### 8. Сложные фикстуры

**Проблема:** большие setup-блоки затрудняют чтение.

```java
// ПЛОХО: неясно, что важно для теста
@BeforeEach
void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("alice@example.com");
    user.setName("Alice");
    user.setAge(30);
    user.setCountry("USA");
    user.setCity("New York");
    user.setZipCode("10001");
    user.setPremium(true);
    user.setVerified(true);
    // ... ещё 20 полей
}

// ХОРОШО: Test Data Builder
class UserBuilder {
    private String email = "default@example.com";
    private String name = "Default User";
    private boolean premium = false;
    
    public UserBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserBuilder withPremium(boolean premium) {
        this.premium = premium;
        return this;
    }
    
    public User build() {
        return new User(email, name, premium);
    }
}

@Test
void test() {
    User user = new UserBuilder()
        .withEmail("alice@example.com")
        .withPremium(true)
        .build();
    
    // Понятно, что важно для этого теста
}
```

---

### 9. Зависимость от порядка выполнения тестов

**Проблема:** тесты влияют друг на друга.

```java
// ПЛОХО: тесты зависят от порядка
private static User user;

@Test
void test1_createUser() {
    user = service.create("alice@example.com");
}

@Test
void test2_updateUser() {
    user.setName("Alice Updated"); // NullPointerException, если test1 не выполнился!
    service.update(user);
}

// ХОРОШО: каждый тест независим
@Test
void createUser() {
    User user = service.create("alice@example.com");
    assertNotNull(user);
}

@Test
void updateUser() {
    User user = service.create("bob@example.com"); // Создаём свои данные
    user.setName("Bob Updated");
    service.update(user);
    assertEquals("Bob Updated", service.getUser(user.getId()).getName());
}
```

---

### 10. Плохие названия тестов

**Проблема:** непонятно, что именно проверяется.

```java
// ПЛОХО
@Test void test1() { }
@Test void test2() { }
@Test void testUser() { }

// ХОРОШО
@Test void registerUser_validEmail_createsNewUser() { }
@Test void registerUser_existingEmail_throwsException() { }
@Test void calculatePrice_premiumUser_applies20PercentDiscount() { }

// ЕЩЁ ЛУЧШЕ с @DisplayName
@Test
@DisplayName("Регистрация пользователя с валидным email создаёт нового пользователя")
void registerUser_validEmail() { }
```

**Паттерны именования:**
- `methodName_condition_expectedResult`
- `shouldDoSomething_whenCondition`
- `given_when_then`

---

### 11. Недостаточное покрытие ошибок

**Проблема:** тесты проверяют только позитивные сценарии.

```java
// НЕДОСТАТОЧНО: только happy path
@Test
void testCalculatePrice() {
    assertEquals(100.0, calculator.calculate(100.0, 0));
}

// ХОРОШО: покрываем граничные случаи
@Test
void calculatePrice_normalDiscount_success() {
    assertEquals(80.0, calculator.calculate(100.0, 20));
}

@Test
void calculatePrice_zeroDiscount_returnsOriginalPrice() {
    assertEquals(100.0, calculator.calculate(100.0, 0));
}

@Test
void calculatePrice_negativeDiscount_throwsException() {
    assertThrows(IllegalArgumentException.class, 
        () -> calculator.calculate(100.0, -10));
}

@Test
void calculatePrice_discountOver100_throwsException() {
    assertThrows(IllegalArgumentException.class, 
        () -> calculator.calculate(100.0, 150));
}

@Test
void calculatePrice_negativePrice_throwsException() {
    assertThrows(IllegalArgumentException.class, 
        () -> calculator.calculate(-100.0, 20));
}
```

---

### 12. Отсутствие проверки сериализации JSON

**Проблема:** изменения DTO могут ломать контракты API.

```java
// ХОРОШО: добавь @JsonTest
@JsonTest
class UserDtoJsonTest {
    
    @Autowired
    private JacksonTester<UserDto> json;
    
    @Test
    void shouldSerializeCorrectly() throws Exception {
        UserDto dto = new UserDto("alice@example.com", "Alice");
        
        assertThat(json.write(dto))
            .extractingJsonPathStringValue("$.email")
            .isEqualTo("alice@example.com");
        
        // Проверка, что структура не изменилась
        assertThat(json.write(dto)).isEqualToJson("""
            {
                "email": "alice@example.com",
                "name": "Alice"
            }
            """);
    }
}
```

---

### 13. Несогласованность данных при параллельном запуске

**Проблема:** тесты используют одну и ту же БД или очередь.

```java
// ПЛОХО: конфликт при параллельном запуске
@SpringBootTest
class ParallelTest1 {
    @Test void test() {
        repo.save(new User("test@example.com"));
    }
}

@SpringBootTest
class ParallelTest2 {
    @Test void test() {
        repo.save(new User("test@example.com")); // Конфликт уникальности!
    }
}

// ХОРОШО: уникальные данные
@SpringBootTest
class ParallelTest1 {
    @Test void test() {
        String email = "test1-" + UUID.randomUUID() + "@example.com";
        repo.save(new User(email));
    }
}
```

---

### 14. Отсутствие документации тестов

```java
// ХОРОШО: используй @DisplayName и группируй по тегам
@DisplayName("Регистрация пользователя")
@Tag("registration")
class UserRegistrationTest {
    
    @Nested
    @DisplayName("Позитивные сценарии")
    class PositiveScenarios {
        
        @Test
        @DisplayName("Регистрация с валидным email")
        void validEmail() { }
    }
    
    @Nested
    @DisplayName("Негативные сценарии")
    class NegativeScenarios {
        
        @Test
        @DisplayName("Регистрация с существующим email")
        void existingEmail() { }
    }
}
```

---

## Антипаттерны в тестировании

### The Liar

Тест проходит, но не проверяет ничего полезного.

```java
// Антипаттерн
@Test
void test() {
    service.doSomething();
    assertTrue(true); // Всегда проходит!
}
```

### The Giant

Один тест проверяет слишком много.

```java
// Антипаттерн
@Test
void testEverything() {
    // Create
    User user = service.create("alice@example.com");
    assertEquals("alice@example.com", user.getEmail());
    
    // Update
    user.setName("Alice Updated");
    service.update(user);
    
    // Delete
    service.delete(user.getId());
    
    // ... ещё 100 строк
}
```

### The Slow Poke

Тест выполняется слишком долго.

```java
// Антипаттерн
@Test
void slowTest() throws InterruptedException {
    Thread.sleep(10000); // 10 секунд!
}
```

### The Secret Catcher

Тест ловит и проглатывает исключения.

```java
// Антипаттерн
@Test
void test() {
    try {
        service.doSomething();
    } catch (Exception e) {
        // Игнорируем
    }
    assertTrue(true);
}
```

---

## Best practices

### 1. Тестируй поведение, а не реализацию

```java
// ПЛОХО: завязан на реализацию
verify(cache).get("user:1");
verify(repo).findById(1L);

// ХОРОШО: проверяем результат
User user = service.getUser(1L);
assertEquals("John", user.getName());
```

### 2. Один тест — одна концепция

```java
// ПЛОХО
@Test
void testUserService() {
    // тест create
    // тест update
    // тест delete
}

// ХОРОШО
@Test void createUser_success() { }
@Test void updateUser_success() { }
@Test void deleteUser_success() { }
```

### 3. Используй явные assertions

```java
// ПЛОХО
assertTrue(user.getName().equals("John"));

// ХОРОШО
assertEquals("John", user.getName());

// ЕЩЁ ЛУЧШЕ с AssertJ
assertThat(user.getName()).isEqualTo("John");
```

### 4. Тестируй граничные случаи

- Пустые коллекции
- null значения
- Максимальные/минимальные значения
- Специальные символы в строках
- Конкурентный доступ

### 5. Избегай логики в тестах

```java
// ПЛОХО
@Test
void test() {
    for (int i = 0; i < 10; i++) {
        if (i % 2 == 0) {
            // ...
        }
    }
}

// ХОРОШО: используй @ParameterizedTest
@ParameterizedTest
@ValueSource(ints = {0, 2, 4, 6, 8})
void test(int value) {
    // ...
}
```

### 6. Не тестируй сторонние библиотеки

```java
// ПЛОХО: тестируешь Spring
@Test
void testSpring() {
    assertNotNull(applicationContext);
}

// ХОРОШО: тестируешь свой код
@Test
void testMyService() {
    User user = myService.createUser("alice@example.com");
    assertNotNull(user);
}
```

---

## Организация тестового кода

### Структура пакетов

```
src/test/java/
  ├── com/example/myapp/
  │   ├── unit/              # Юнит-тесты
  │   │   ├── service/
  │   │   └── util/
  │   ├── integration/       # Интеграционные тесты
  │   │   ├── repository/
  │   │   └── api/
  │   └── e2e/               # End-to-end тесты
```

### Именование тестовых классов

```java
// Для класса UserService
UserServiceTest           // Юнит-тесты
UserServiceIntegrationTest // Интеграционные
```

### Группировка тестов

```java
@Tag("unit")
class UserServiceTest { }

@Tag("integration")
class UserRepositoryTest { }

@Tag("slow")
class E2ETest { }
```

Запуск:
```bash
mvn test -Dgroups="unit"
mvn test -DexcludedGroups="slow"
```

---

## Производительность тестов

### Измерение времени

```java
@Test
@Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
void fastTest() {
    // должен завершиться за 100мс
}
```

### Параллельное выполнение (JUnit 5)

`junit-platform.properties`:
```properties
junit.jupiter.execution.parallel.enabled=true
junit.jupiter.execution.parallel.mode.default=concurrent
junit.jupiter.execution.parallel.config.strategy=dynamic
```

### Оптимизация Spring контекста

```java
// Переиспользуй контекст
@SpringBootTest
class Test1 { }

@SpringBootTest // тот же контекст!
class Test2 { }
```

---

## Вопросы для самопроверки

1. **Почему Thread.sleep() в тестах — плохая практика?**
   - Непредсказуемость, замедление, flaky tests

2. **Когда использовать verify() в Mockito?**
   - Только для критичных взаимодействий с внешними системами

3. **Почему H2 не подходит для интеграционных тестов?**
   - Различия в SQL-диалектах и транзакционном поведении

4. **Что такое flaky test?**
   - Тест, который иногда проходит, а иногда падает без изменений в коде

5. **Зачем нужен Test Data Builder?**
   - Упрощает создание сложных тестовых объектов

---

## Заключение

Качественные тесты — это инвестиция в долгосрочную поддерживаемость проекта. Избегайте антипаттернов, следуйте best practices, и ваши тесты будут надёжными, быстрыми и понятными.

### Дополнительные ресурсы

- [Test Smells Catalog](http://testsmells.org/)
- [Martin Fowler: Test Pyramid](https://martinfowler.com/bliki/TestPyramid.html)
- [XUnit Test Patterns](http://xunitpatterns.com/)
- [Effective Unit Testing](https://www.manning.com/books/effective-unit-testing)

---

[← Назад к разделу Тестирование](README.md)
