# Интервью-гайд по тестированию — вопросы и ответы

Этот раздел предназначен для подготовки к техническим собеседованиям на позиции Java-разработчика. Он включает типичные вопросы и краткие формулировки ответов, которые можно использовать при устных интервью.

## Содержание

1. [Вопросы по JUnit 5](#вопросы-по-junit-5)
2. [Вопросы по Mockito](#вопросы-по-mockito)
3. [Вопросы по Spring Boot Test](#вопросы-по-spring-boot-test)
4. [Вопросы по Testcontainers](#вопросы-по-testcontainers)
5. [Вопросы по best practices](#вопросы-по-best-practices)
6. [Подковырные вопросы](#подковырные-вопросы)
7. [Практические задачи](#практические-задачи)
8. [Короткие формулировки для собеседования](#короткие-формулировки-для-собеседования)

---

## Вопросы по JUnit 5

### Базовые вопросы

**1. В чём отличие JUnit 4 от JUnit 5?**

**Ответ:**
- JUnit 5 построен на новой модульной архитектуре (Platform / Jupiter / Vintage)
- Поддерживает расширения через Extensions API вместо @RunWith
- Имеет встроенные параметризованные тесты (@ParameterizedTest)
- Поддерживает параллельное выполнение тестов
- Новые аннотации: @BeforeEach вместо @Before, @BeforeAll вместо @BeforeClass
- Улучшенные assertions с поддержкой лямбда-выражений

**2. Что делает аннотация @ExtendWith?**

**Ответ:**
Подключает расширения JUnit 5, например MockitoExtension для инициализации моков или SpringExtension для Spring контекста.

**3. Как протестировать выбрасывание исключения?**

**Ответ:**
```java
Exception exception = assertThrows(
    IllegalArgumentException.class,
    () -> service.invalidOperation()
);
assertEquals("Expected message", exception.getMessage());
```

**4. Для чего нужен assertAll()?**

**Ответ:**
Позволяет сгруппировать несколько ассертов и получить полный отчёт о всех ошибках, а не останавливаться на первой.

```java
assertAll("person",
    () -> assertEquals("John", person.getFirstName()),
    () -> assertEquals("Doe", person.getLastName()),
    () -> assertEquals(30, person.getAge())
);
```

**5. Как организовать параметры в тестах?**

**Ответ:**
Через @ParameterizedTest и источники данных:
- @ValueSource — простые значения
- @CsvSource — CSV данные
- @MethodSource — метод-фабрика
- @EnumSource — значения enum

### Продвинутые вопросы

**6. Что такое Test Lifecycle в JUnit 5?**

**Ответ:**
- `PER_METHOD` (по умолчанию): новый экземпляр тестового класса для каждого теста
- `PER_CLASS`: один экземпляр для всех тестов класса, позволяет использовать нестатические @BeforeAll/@AfterAll

**7. Разница между @BeforeEach и @BeforeAll?**

**Ответ:**
- @BeforeEach — выполняется перед каждым тестовым методом
- @BeforeAll — выполняется один раз перед всеми тестами, метод должен быть static (или @TestInstance(PER_CLASS))

**8. Как организовать связанные тесты в JUnit?**

**Ответ:**
- Использовать вложенные классы с @Nested
- Группировать по функциональности
- Применять теги @Tag для категоризации

**9. Что такое Extensions в JUnit 5?**

**Ответ:**
Механизм для добавления поведения к тестам через интерфейсы Extension API:
- BeforeEachCallback / AfterEachCallback
- ParameterResolver
- TestExecutionExceptionHandler
- и другие

**10. Как запустить тесты с определёнными тегами?**

**Ответ:**
```bash
# Maven
mvn test -Dgroups="fast"

# Gradle
./gradlew test --tests '*' -Dgroups="fast"
```

---

## Вопросы по Mockito

### Базовые вопросы

**1. В чём разница между mock и spy?**

**Ответ:**
- **Mock** — полностью поддельный объект, все методы возвращают значения по умолчанию (null, 0, false)
- **Spy** — оборачивает реальный объект и вызывает настоящие методы, если не переопределено поведение

**2. Что делает @InjectMocks?**

**Ответ:**
Создаёт экземпляр тестируемого класса и автоматически внедряет в него зависимости, помеченные @Mock или @Spy.

**3. Как проверить аргументы вызова метода?**

**Ответ:**
Использовать ArgumentCaptor:
```java
ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
verify(repo).save(captor.capture());
assertEquals("expected@example.com", captor.getValue().getEmail());
```

**4. Чем doReturn() отличается от when().thenReturn()?**

**Ответ:**
- `when().thenReturn()` вызывает реальный метод (проблема для spy)
- `doReturn().when()` не вызывает реальный метод (безопасно для spy)

**5. Как протестировать, что метод не был вызван?**

**Ответ:**
```java
verify(mock, never()).method();
verifyNoInteractions(mock);
```

### Продвинутые вопросы

**6. Что такое матчеры (matchers) и когда их использовать?**

**Ответ:**
Гибкие условия для аргументов: `any()`, `eq()`, `contains()`, `argThat()`. Используются когда точное значение аргумента неизвестно или неважно.

**7. Как проверить порядок вызовов методов?**

**Ответ:**
```java
InOrder inOrder = inOrder(mock);
inOrder.verify(mock).firstMethod();
inOrder.verify(mock).secondMethod();
```

**8. Когда следует использовать Spy вместо Mock?**

**Ответ:**
- Когда нужна реальная реализация с возможностью подмены отдельных методов
- Для legacy кода
- Когда объект сложно пересоздать

**9. Почему не стоит мокать value objects?**

**Ответ:**
Они простые, не имеют внешних зависимостей, и моканье усложняет тесты без реальной пользы.

**10. Как избежать хрупких тестов при использовании verify()?**

**Ответ:**
- Проверять только важные взаимодействия
- Фокусироваться на результате, а не на реализации
- Использовать verify() только для внешних систем (email, API, очереди)

---

## Вопросы по Spring Boot Test

### Базовые вопросы

**1. Разница между @SpringBootTest и @WebMvcTest?**

**Ответ:**
- @SpringBootTest поднимает полный Spring контекст (медленно, для интеграционных тестов)
- @WebMvcTest поднимает только веб-слой: контроллеры, фильтры, конвертеры (быстро, для slice-тестов)

**2. Что делает @MockBean?**

**Ответ:**
Подменяет бин в Spring контексте на мок. Используется когда нужно замокать зависимость в интеграционном тесте.

**3. Зачем нужен @DataJpaTest?**

**Ответ:**
Для тестирования JPA-репозиториев. Поднимает только JPA-слой, использует встроенную БД или Testcontainers, автоматически откатывает транзакции.

**4. Как отключить встроенную БД H2 в тестах?**

**Ответ:**
```java
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
```
И использовать Testcontainers для реальной БД.

**5. Что делает @DynamicPropertySource?**

**Ответ:**
Позволяет динамически передавать свойства (например, URL БД из Testcontainers) в Spring контекст.

### Продвинутые вопросы

**6. Как тестировать REST API с реальным сервером?**

**Ответ:**
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ApiTest {
    @Autowired TestRestTemplate restTemplate;
    
    @Test
    void test() {
        ResponseEntity<User> response = 
            restTemplate.getForEntity("/api/users/1", User.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
```

**7. Почему @DirtiesContext вреден?**

**Ответ:**
Он пересоздаёт весь Spring контекст, что делает тесты медленными. Лучше использовать @AfterEach для очистки данных.

**8. Как протестировать асинхронный код?**

**Ответ:**
Использовать Awaitility:
```java
await().atMost(Duration.ofSeconds(5))
       .until(() -> service.isCompleted());
```

**9. Как тестировать защищённые эндпоинты?**

**Ответ:**
```java
@WebMvcTest(AdminController.class)
class SecurityTest {
    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_withAdminRole_shouldSucceed() {
        mockMvc.perform(get("/api/admin/users"))
               .andExpect(status().isOk());
    }
}
```

**10. Что такое slice-тесты?**

**Ответ:**
Тесты, поднимающие только часть Spring контекста (@WebMvcTest, @DataJpaTest, @JsonTest), что делает их быстрее полных интеграционных тестов.

---

## Вопросы по Testcontainers

### Базовые вопросы

**1. Почему Testcontainers лучше H2?**

**Ответ:**
Testcontainers использует реальную СУБД (PostgreSQL, MySQL), что гарантирует идентичное поведение с production, в отличие от H2, которая имеет другой SQL-диалект и поведение.

**2. Как ускорить тесты с Testcontainers?**

**Ответ:**
- Использовать статические контейнеры (`static @Container`)
- Включить reuse контейнеров (testcontainers.reuse.enable=true)
- Кешировать Docker-образы в CI
- Использовать легковесные образы (alpine)

**3. Что делает аннотация @Container?**

**Ответ:**
Помечает контейнер, который будет автоматически запущен перед тестами и остановлен после.

**4. Можно ли использовать Testcontainers в CI?**

**Ответ:**
Да, при условии что доступен Docker или Podman. Многие CI системы (GitHub Actions, GitLab CI) поддерживают Docker out-of-the-box.

**5. Как протестировать Kafka или Redis с Testcontainers?**

**Ответ:**
```java
@Container
static KafkaContainer kafka = new KafkaContainer(
    DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
);

@Container
static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
    .withExposedPorts(6379);
```

### Продвинутые вопросы

**6. Статические vs нестатические контейнеры?**

**Ответ:**
- **static**: один контейнер на весь тестовый класс (быстрее, но общий state)
- **нестатические**: новый контейнер для каждого теста (изолированы, но медленнее)

**7. Как работает reuse контейнеров?**

**Ответ:**
При включении (testcontainers.reuse.enable=true) контейнеры не удаляются после тестов и переиспользуются. Подходит только для локальной разработки.

**8. Что такое @DynamicPropertySource и зачем?**

**Ответ:**
Позволяет динамически получить свойства запущенного контейнера (URL, порт, credentials) и передать их в Spring контекст.

**9. Как тестировать с Docker Compose?**

**Ответ:**
```java
@Container
static DockerComposeContainer<?> compose = 
    new DockerComposeContainer<>(new File("docker-compose.yml"))
        .withExposedService("db", 5432);
```

**10. Типичные ошибки с Testcontainers?**

**Ответ:**
- Использование `latest` тегов (нестабильность)
- Несогласованные версии драйверов
- Параллельные тесты без изоляции
- Отсутствие @DynamicPropertySource

---

## Вопросы по best practices

### Базовые вопросы

**1. Что такое пирамида тестирования?**

**Ответ:**
- 70% — юнит-тесты (быстрые, изолированные)
- 20% — интеграционные (взаимодействие компонентов)
- 10% — E2E (полный сценарий)

**2. Что такое AAA паттерн?**

**Ответ:**
- **Arrange** (подготовка) — создание объектов, настройка моков
- **Act** (действие) — вызов тестируемого метода
- **Assert** (проверка) — проверка результата

**3. Почему Thread.sleep() в тестах — плохо?**

**Ответ:**
Делает тесты медленными, непредсказуемыми (flaky) и зависимыми от производительности машины.

**4. Что такое flaky test?**

**Ответ:**
Тест, который иногда проходит, а иногда падает без изменений в коде. Обычно из-за race conditions, зависимости от времени или случайных данных.

**5. Как обеспечить изоляцию тестов?**

**Ответ:**
- Не использовать статические переменные
- Очищать состояние в @AfterEach
- Каждый тест создаёт свои данные
- Использовать rollback транзакций

### Продвинутые вопросы

**6. Что такое Test Data Builder?**

**Ответ:**
Паттерн для упрощения создания сложных тестовых объектов:
```java
User user = new UserBuilder()
    .withEmail("alice@example.com")
    .withPremium(true)
    .build();
```

**7. Как правильно тестировать время и UUID?**

**Ответ:**
Внедрять Clock и Supplier<UUID> как зависимости:
```java
class Service {
    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;
    
    public Event create() {
        return new Event(
            uuidSupplier.get(),
            LocalDateTime.now(clock)
        );
    }
}
```

**8. Когда использовать verify() в Mockito?**

**Ответ:**
Только для критичных взаимодействий с внешними системами:
- Отправка email/SMS
- Публикация в Kafka
- Audit logging
- Вызовы внешних API

**9. Что такое test smells?**

**Ответ:**
Признаки плохих тестов:
- The Liar (тест ничего не проверяет)
- The Giant (слишком много в одном тесте)
- The Slow Poke (медленный тест)
- The Secret Catcher (проглатывает исключения)

**10. F.I.R.S.T. принципы тестирования?**

**Ответ:**
- **Fast** — быстрые
- **Independent** — независимые
- **Repeatable** — повторяемые
- **Self-validating** — самопроверяющиеся
- **Timely** — своевременные

---

## Подковырные вопросы

**1. Почему не стоит мокать всё подряд?**

**Ответ:**
Тест теряет смысл — нужно проверять реальную бизнес-логику, а не взаимодействия. Моки должны изолировать только внешние зависимости (БД, API, очереди).

**2. Почему @DirtiesContext вреден?**

**Ответ:**
Он пересоздаёт весь Spring контекст, что делает тесты очень медленными. Лучше использовать точечную очистку данных через @AfterEach.

**3. Как протестировать время и UUID?**

**Ответ:**
Использовать Clock и Supplier<UUID> как зависимости, мокать их в тестах.

**4. Как добиться стабильности асинхронных тестов?**

**Ответ:**
Использовать Awaitility вместо Thread.sleep(), настроить разумные таймауты и polling intervals.

**5. Почему стоит избегать H2?**

**Ответ:**
Различия в SQL-диалектах, транзакционном поведении, индексах и специфичных функциях PostgreSQL/MySQL.

**6. Как тестировать приватные методы?**

**Ответ:**
Не тестировать напрямую! Тестировать через публичные методы. Если приватный метод сложен — возможно, это признак нарушения Single Responsibility Principle.

**7. Разница между @Mock и new Object()?**

**Ответ:**
- @Mock — контролируемый объект для проверки взаимодействий
- new Object() — реальный объект со всей логикой

**8. Когда использовать @SpringBootTest vs @WebMvcTest?**

**Ответ:**
- @SpringBootTest — для полных интеграционных тестов
- @WebMvcTest — для быстрых slice-тестов контроллеров

**9. Что делать с медленными тестами?**

**Ответ:**
- Использовать slice-тесты вместо полных
- Статические Testcontainers
- Кешировать Spring контекст
- Параллельное выполнение
- Профилирование для поиска узких мест

**10. Как измерить качество тестов?**

**Ответ:**
- Code coverage (JaCoCo) — но не 100% цель
- Mutation testing (PITest)
- Проверка на flaky tests
- Время выполнения

---

## Практические задачи

### Задача 1: Написать тест для UserService

```java
class UserService {
    private final UserRepository repository;
    private final EmailSender emailSender;
    
    public User register(String email, String name) {
        if (repository.existsByEmail(email)) {
            throw new UserAlreadyExistsException();
        }
        User user = new User(email, name);
        User saved = repository.save(user);
        emailSender.send(email, "Welcome!");
        return saved;
    }
}
```

**Решение:**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepository repository;
    @Mock EmailSender emailSender;
    @InjectMocks UserService service;
    
    @Test
    void register_validEmail_createsUser() {
        when(repository.existsByEmail("alice@example.com")).thenReturn(false);
        when(repository.save(any(User.class)))
            .thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(1L);
                return u;
            });
        
        User user = service.register("alice@example.com", "Alice");
        
        assertEquals(1L, user.getId());
        verify(emailSender).send("alice@example.com", "Welcome!");
    }
    
    @Test
    void register_existingEmail_throwsException() {
        when(repository.existsByEmail("alice@example.com")).thenReturn(true);
        
        assertThrows(UserAlreadyExistsException.class, 
            () -> service.register("alice@example.com", "Alice"));
        
        verify(repository, never()).save(any());
        verify(emailSender, never()).send(anyString(), anyString());
    }
}
```

### Задача 2: Тест контроллера с MockMvc

```java
@RestController
@RequestMapping("/api/users")
class UserController {
    private final UserService service;
    
    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id) {
        return service.getUser(id);
    }
}
```

**Решение:**
```java
@WebMvcTest(UserController.class)
class UserControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean UserService service;
    
    @Test
    void getUser_existing_returnsUser() throws Exception {
        when(service.getUser(1L))
            .thenReturn(new User(1L, "alice@example.com", "Alice"));
        
        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.name").value("Alice"));
    }
    
    @Test
    void getUser_notFound_returns404() throws Exception {
        when(service.getUser(999L))
            .thenThrow(new UserNotFoundException());
        
        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound());
    }
}
```

---

## Короткие формулировки для собеседования

### Таблица быстрых ответов

| Тема             | Короткий ответ                                                                |
| ---------------- | ----------------------------------------------------------------------------- |
| JUnit 5          | Новая архитектура, Extensions API, assertAll/assertThrows, параметризация     |
| Mockito          | Моки изолируют код, verify и ArgumentCaptor для проверки поведения            |
| Spring Boot Test | Slice-тесты для ускорения, @MockBean для подмены зависимостей                 |
| Testcontainers   | Реальное окружение, статические контейнеры, reuse и CI-friendly               |
| Best Practices   | AAA паттерн, F.I.R.S.T., пирамида тестирования, избегать моков value objects |
| Ошибки           | Не мокай всё, избегай H2, не используй sleep, тесты должны быть независимы    |

### Ключевые концепции одной строкой

- **Mock vs Spy**: Mock — полностью фиктивный, Spy — реальный с подменой методов
- **@SpringBootTest vs @WebMvcTest**: полный контекст vs только веб-слой
- **H2 vs Testcontainers**: in-memory эмуляция vs реальная БД
- **verify() vs assertEquals()**: проверка поведения vs проверка состояния
- **AAA**: Arrange-Act-Assert структура теста
- **Flaky test**: тест с нестабильным результатом
- **Test Data Builder**: паттерн для создания тестовых данных
- **Slice tests**: тесты части контекста (@DataJpaTest, @WebMvcTest)

---

## Заключение

Подготовка к собеседованию — это не только знание API, но и понимание принципов и best practices. Практикуйтесь в написании тестов, изучайте реальные проекты и анализируйте ошибки.

### Рекомендации для подготовки

1. **Практика**: напишите тесты для реального проекта
2. **Code review**: изучайте тесты в open-source проектах
3. **Документация**: читайте официальные гайды JUnit, Mockito, Spring
4. **Mock interviews**: тренируйтесь объяснять код вслух
5. **Hands-on**: готовьтесь к live coding на собеседовании

---

[← Назад к разделу Тестирование](README.md)
