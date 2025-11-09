# JUnit — фреймворк модульного тестирования для Java

JUnit — это один из наиболее популярных фреймворков для написания и выполнения модульных тестов в Java. Он обеспечивает структурированный подход к тестированию, предоставляя аннотации, утверждения и инструменты для организации тестового кода.

## Содержание
1. [Введение и основы JUnit](#введение-и-основы-junit)
2. [Аннотации и жизненный цикл тестов](#аннотации-и-жизненный-цикл-тестов)
3. [Assertions — утверждения](#assertions--утверждения)
4. [Параметризованные тесты](#параметризованные-тесты)
5. [Тестовые наборы и теги](#тестовые-наборы-и-теги)
6. [Расширения (Extensions)](#расширения-extensions)
7. [Интеграция с инструментами сборки](#интеграция-с-инструментами-сборки)
8. [Лучшие практики](#лучшие-практики)
9. [Вопросы для самопроверки](#вопросы-для-самопроверки)

---

## Введение и основы JUnit

### История и версии
- **JUnit 3** — первая широко используемая версия (требовала наследования от `TestCase`)
- **JUnit 4** — введены аннотации (`@Test`, `@Before`, `@After`), упрощена структура тестов
- **JUnit 5** (Jupiter) — современная версия с модульной архитектурой, новыми возможностями и лучшей расширяемостью

### Архитектура JUnit 5
JUnit 5 состоит из трёх основных компонентов:
- **JUnit Platform** — основа для запуска тестовых фреймворков на JVM
- **JUnit Jupiter** — новая модель программирования и расширений для написания тестов
- **JUnit Vintage** — поддержка запуска тестов JUnit 3 и JUnit 4

### Базовая зависимость (Maven)
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

### Простейший тест
```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    
    @Test
    void additionShouldReturnCorrectSum() {
        Calculator calculator = new Calculator();
        int result = calculator.add(2, 3);
        assertEquals(5, result);
    }
}
```

---

## Аннотации и жизненный цикл тестов

### Основные аннотации

#### `@Test`
Обозначает метод как тестовый. Методы с этой аннотацией выполняются тестовым раннером.

```java
@Test
void shouldCalculateSquare() {
    assertEquals(16, Math.pow(4, 2));
}
```

#### `@BeforeEach` и `@AfterEach`
Методы, выполняемые перед и после каждого теста.

```java
class DatabaseTest {
    private DatabaseConnection connection;
    
    @BeforeEach
    void setUp() {
        connection = new DatabaseConnection();
        connection.connect();
    }
    
    @AfterEach
    void tearDown() {
        connection.disconnect();
    }
    
    @Test
    void shouldInsertRecord() {
        // тест использует connection
    }
}
```

#### `@BeforeAll` и `@AfterAll`
Методы, выполняемые один раз перед всеми тестами и после них. Должны быть `static`.

```java
class ResourceTest {
    private static ExpensiveResource resource;
    
    @BeforeAll
    static void initResource() {
        resource = new ExpensiveResource();
    }
    
    @AfterAll
    static void cleanupResource() {
        resource.close();
    }
    
    @Test
    void testResourceUsage() {
        // использование resource
    }
}
```

### Дополнительные аннотации

#### `@DisplayName`
Задаёт читаемое имя теста, которое отображается в отчётах.

```java
@Test
@DisplayName("Проверка деления на ноль должна выбросить исключение")
void divisionByZeroShouldThrowException() {
    assertThrows(ArithmeticException.class, () -> {
        int result = 10 / 0;
    });
}
```

#### `@Disabled`
Отключает выполнение теста или класса тестов.

```java
@Test
@Disabled("Функция ещё не реализована")
void testNewFeature() {
    // тест временно отключён
}
```

#### `@Timeout`
Устанавливает максимальное время выполнения теста.

```java
@Test
@Timeout(value = 100, unit = TimeUnit.MILLISECONDS)
void shouldCompleteQuickly() {
    // тест должен завершиться за 100 мс
}
```

### Жизненный цикл экземпляра теста

По умолчанию JUnit создаёт новый экземпляр тестового класса для каждого теста. Можно изменить это поведение:

```java
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SharedStateTest {
    // один экземпляр для всех тестов
    // позволяет использовать нестатические @BeforeAll и @AfterAll
}
```

---

## Assertions — утверждения

Assertions проверяют ожидаемое поведение программы. JUnit предоставляет богатый набор методов утверждений.

### Базовые assertions

#### `assertEquals` / `assertNotEquals`
```java
@Test
void testEquality() {
    assertEquals(4, 2 + 2);
    assertEquals("Hello", "Hel" + "lo");
    assertNotEquals(5, 2 + 2);
}
```

#### `assertTrue` / `assertFalse`
```java
@Test
void testBoolean() {
    assertTrue(5 > 3);
    assertFalse("text".isEmpty());
}
```

#### `assertNull` / `assertNotNull`
```java
@Test
void testNull() {
    String str = null;
    assertNull(str);
    
    str = "value";
    assertNotNull(str);
}
```

#### `assertSame` / `assertNotSame`
Проверяют идентичность объектов (ссылаются ли на один объект).

```java
@Test
void testIdentity() {
    String a = "test";
    String b = a;
    assertSame(a, b);
    
    String c = new String("test");
    assertNotSame(a, c);
}
```

### Assertions для массивов и коллекций

#### `assertArrayEquals`
```java
@Test
void testArrays() {
    int[] expected = {1, 2, 3};
    int[] actual = {1, 2, 3};
    assertArrayEquals(expected, actual);
}
```

#### `assertIterableEquals`
```java
@Test
void testIterables() {
    List<String> expected = Arrays.asList("a", "b", "c");
    List<String> actual = Arrays.asList("a", "b", "c");
    assertIterableEquals(expected, actual);
}
```

### Assertions для исключений

#### `assertThrows`
```java
@Test
void testException() {
    Exception exception = assertThrows(
        IllegalArgumentException.class,
        () -> {
            throw new IllegalArgumentException("Invalid argument");
        }
    );
    
    assertEquals("Invalid argument", exception.getMessage());
}
```

#### `assertDoesNotThrow`
```java
@Test
void testNoException() {
    assertDoesNotThrow(() -> {
        // код, который не должен выбрасывать исключения
        int result = 10 / 2;
    });
}
```

### Групповые assertions

#### `assertAll`
Выполняет несколько утверждений, собирая все ошибки, а не останавливаясь на первой.

```java
@Test
void testPerson() {
    Person person = new Person("John", "Doe", 30);
    
    assertAll("person",
        () -> assertEquals("John", person.getFirstName()),
        () -> assertEquals("Doe", person.getLastName()),
        () -> assertEquals(30, person.getAge())
    );
}
```

### Assertions с сообщениями

Все assertions принимают опциональное сообщение об ошибке:

```java
@Test
void testWithMessage() {
    assertEquals(5, 2 + 2, "Сумма 2 + 2 должна быть 4");
    
    // Ленивое вычисление сообщения (для дорогих операций)
    assertEquals(5, 2 + 2, () -> "Сумма неверна: " + calculateExpensive());
}
```

---

## Параметризованные тесты

Параметризованные тесты позволяют запускать один и тот же тест с разными наборами данных.

### Зависимость для параметризованных тестов
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-params</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

### `@ValueSource`
Простейший источник параметров для примитивных типов и строк.

```java
@ParameterizedTest
@ValueSource(ints = {1, 2, 3, 5, 8})
void testFibonacci(int number) {
    assertTrue(isFibonacciNumber(number));
}

@ParameterizedTest
@ValueSource(strings = {"", "  ", "\t", "\n"})
void testBlankStrings(String input) {
    assertTrue(input.isBlank());
}
```

### `@CsvSource`
Передаёт несколько параметров в формате CSV.

```java
@ParameterizedTest
@CsvSource({
    "1, 1, 2",
    "2, 3, 5",
    "5, 8, 13",
    "8, 13, 21"
})
void testAddition(int a, int b, int expected) {
    assertEquals(expected, a + b);
}
```

### `@CsvFileSource`
Загружает данные из CSV-файла.

```java
@ParameterizedTest
@CsvFileSource(resources = "/test-data.csv", numLinesToSkip = 1)
void testFromFile(String input, String expected) {
    assertEquals(expected, process(input));
}
```

### `@MethodSource`
Использует метод-фабрику для генерации аргументов.

```java
@ParameterizedTest
@MethodSource("provideStringsForIsBlank")
void testIsBlank(String input, boolean expected) {
    assertEquals(expected, input.isBlank());
}

static Stream<Arguments> provideStringsForIsBlank() {
    return Stream.of(
        Arguments.of("", true),
        Arguments.of("  ", true),
        Arguments.of("not blank", false)
    );
}
```

### `@EnumSource`
Перебирает значения перечисления.

```java
@ParameterizedTest
@EnumSource(DayOfWeek.class)
void testDaysOfWeek(DayOfWeek day) {
    assertNotNull(day);
}

@ParameterizedTest
@EnumSource(value = Month.class, names = {"APRIL", "JUNE", "SEPTEMBER", "NOVEMBER"})
void testMonthsWith30Days(Month month) {
    assertEquals(30, month.length(false));
}
```

### Пользовательские имена для параметризованных тестов

```java
@ParameterizedTest(name = "{index} => sum({0}, {1}) = {2}")
@CsvSource({
    "1, 1, 2",
    "2, 3, 5",
    "5, 8, 13"
})
void testAdditionWithCustomName(int a, int b, int expected) {
    assertEquals(expected, a + b);
}
```

---

## Тестовые наборы и теги

### Теги (`@Tag`)
Теги позволяют группировать тесты и выборочно их запускать.

```java
@Tag("fast")
@Test
void fastTest() {
    // быстрый тест
}

@Tag("slow")
@Tag("integration")
@Test
void integrationTest() {
    // медленный интеграционный тест
}
```

Запуск тестов с определёнными тегами через Maven:
```bash
mvn test -Dgroups="fast"
mvn test -Dgroups="slow | integration"
mvn test -DexcludedGroups="slow"
```

### Вложенные тесты (`@Nested`)
Позволяют организовать связанные тесты в иерархическую структуру.

```java
class StackTest {
    
    @Nested
    @DisplayName("Когда стек новый")
    class WhenNew {
        
        private Stack<Object> stack;
        
        @BeforeEach
        void createNewStack() {
            stack = new Stack<>();
        }
        
        @Test
        @DisplayName("должен быть пустым")
        void isEmpty() {
            assertTrue(stack.isEmpty());
        }
        
        @Test
        @DisplayName("выбрасывает EmptyStackException при pop()")
        void throwsExceptionWhenPopped() {
            assertThrows(EmptyStackException.class, stack::pop);
        }
        
        @Nested
        @DisplayName("После добавления элемента")
        class AfterPushing {
            
            @BeforeEach
            void pushAnElement() {
                stack.push("element");
            }
            
            @Test
            @DisplayName("не должен быть пустым")
            void isNotEmpty() {
                assertFalse(stack.isEmpty());
            }
            
            @Test
            @DisplayName("возвращает элемент при pop()")
            void returnElementWhenPopped() {
                assertEquals("element", stack.pop());
            }
        }
    }
}
```

### Условное выполнение тестов

#### По операционной системе
```java
@Test
@EnabledOnOs(OS.LINUX)
void onLinux() {
    // запускается только на Linux
}

@Test
@DisabledOnOs(OS.WINDOWS)
void notOnWindows() {
    // не запускается на Windows
}
```

#### По версии Java
```java
@Test
@EnabledOnJre(JRE.JAVA_11)
void onJava11() {
    // только для Java 11
}

@Test
@EnabledForJreRange(min = JRE.JAVA_11, max = JRE.JAVA_17)
void onJava11To17() {
    // для Java 11-17
}
```

#### По системным свойствам
```java
@Test
@EnabledIfSystemProperty(named = "os.arch", matches = ".*64.*")
void on64BitArchitecture() {
    // на 64-битной архитектуре
}
```

#### По переменным окружения
```java
@Test
@EnabledIfEnvironmentVariable(named = "ENV", matches = "staging")
void onStagingEnvironment() {
    // только в staging окружении
}
```

---

## Расширения (Extensions)

JUnit 5 имеет мощную модель расширений, позволяющую добавлять дополнительное поведение к тестам.

### Встроенные расширения

#### `@TempDir` — временные директории
```java
@Test
void testWithTempDirectory(@TempDir Path tempDir) {
    Path testFile = tempDir.resolve("test.txt");
    Files.write(testFile, "content".getBytes());
    
    assertTrue(Files.exists(testFile));
    // tempDir автоматически удаляется после теста
}
```

### Пользовательские расширения

#### Extension API
JUnit 5 предоставляет несколько интерфейсов для создания расширений:
- `BeforeAllCallback` / `AfterAllCallback`
- `BeforeEachCallback` / `AfterEachCallback`
- `BeforeTestExecutionCallback` / `AfterTestExecutionCallback`
- `TestInstancePostProcessor`
- `ParameterResolver`
- `TestExecutionExceptionHandler`

#### Пример: расширение для замера времени выполнения
```java
public class TimingExtension implements 
    BeforeTestExecutionCallback, AfterTestExecutionCallback {
    
    private static final String START_TIME = "start time";
    
    @Override
    public void beforeTestExecution(ExtensionContext context) {
        getStore(context).put(START_TIME, System.currentTimeMillis());
    }
    
    @Override
    public void afterTestExecution(ExtensionContext context) {
        long startTime = getStore(context).remove(START_TIME, long.class);
        long duration = System.currentTimeMillis() - startTime;
        
        System.out.println(
            String.format("Test %s took %d ms", 
                context.getDisplayName(), duration)
        );
    }
    
    private Store getStore(ExtensionContext context) {
        return context.getStore(
            Namespace.create(getClass(), context.getRequiredTestMethod())
        );
    }
}
```

Использование:
```java
@ExtendWith(TimingExtension.class)
class TimedTests {
    
    @Test
    void testMethod() {
        // время выполнения будет выведено
    }
}
```

#### Пример: расширение для внедрения зависимостей
```java
public class DatabaseExtension implements 
    BeforeAllCallback, AfterAllCallback, ParameterResolver {
    
    private Database database;
    
    @Override
    public void beforeAll(ExtensionContext context) {
        database = new Database();
        database.connect();
    }
    
    @Override
    public void afterAll(ExtensionContext context) {
        database.disconnect();
    }
    
    @Override
    public boolean supportsParameter(
        ParameterContext parameterContext, 
        ExtensionContext extensionContext) {
        
        return parameterContext.getParameter()
            .getType().equals(Database.class);
    }
    
    @Override
    public Object resolveParameter(
        ParameterContext parameterContext, 
        ExtensionContext extensionContext) {
        
        return database;
    }
}
```

Использование:
```java
@ExtendWith(DatabaseExtension.class)
class DatabaseTests {
    
    @Test
    void testQuery(Database db) {
        // db автоматически внедряется расширением
        List<Record> records = db.query("SELECT * FROM users");
        assertFalse(records.isEmpty());
    }
}
```

### Регистрация расширений

#### Декларативная регистрация
```java
@ExtendWith(TimingExtension.class)
@ExtendWith(DatabaseExtension.class)
class MyTests {
    // тесты
}
```

#### Программная регистрация
```java
class MyTests {
    
    @RegisterExtension
    static TimingExtension timing = new TimingExtension();
    
    @Test
    void test() {
        // тест
    }
}
```

#### Автоматическая регистрация
Через файл `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`:
```
com.example.TimingExtension
com.example.DatabaseExtension
```

---

## Интеграция с инструментами сборки

### Maven

#### Конфигурация Surefire Plugin
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>2.22.2</version>
            <configuration>
                <!-- Запуск тестов с определёнными тегами -->
                <groups>fast</groups>
                <excludedGroups>slow,integration</excludedGroups>
                
                <!-- Включение параллельного выполнения -->
                <parallel>methods</parallel>
                <threadCount>4</threadCount>
                
                <!-- Отчёты -->
                <reportFormat>plain</reportFormat>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### Запуск тестов
```bash
# Запустить все тесты
mvn test

# Запустить конкретный тестовый класс
mvn test -Dtest=CalculatorTest

# Запустить конкретный метод
mvn test -Dtest=CalculatorTest#testAddition

# Запустить с тегами
mvn test -Dgroups="fast"
```

### Gradle

#### Конфигурация в build.gradle
```groovy
test {
    useJUnitPlatform {
        // Включить тесты с тегами
        includeTags 'fast'
        excludeTags 'slow', 'integration'
    }
    
    // Параллельное выполнение
    maxParallelForks = 4
    
    // Логирование
    testLogging {
        events "passed", "skipped", "failed"
        exceptionFormat "full"
    }
}

dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.0'
}
```

#### Запуск тестов
```bash
# Запустить все тесты
./gradlew test

# Запустить конкретный класс
./gradlew test --tests CalculatorTest

# Запустить конкретный метод
./gradlew test --tests CalculatorTest.testAddition

# Непрерывное выполнение
./gradlew test --continuous
```

---

## Лучшие практики

### Именование тестов

#### Используйте выразительные имена
```java
// Плохо
@Test
void test1() { }

// Хорошо
@Test
void whenDividingByZero_shouldThrowArithmeticException() { }

// Ещё лучше с @DisplayName
@Test
@DisplayName("Деление на ноль должно выбрасывать ArithmeticException")
void divisionByZero() { }
```

#### Паттерны именования
- **Given-When-Then**: `givenNewStack_whenPop_thenThrowsException()`
- **Should-When**: `shouldThrowException_whenDividingByZero()`
- **Subject-Action-Result**: `stack_pop_throwsExceptionWhenEmpty()`

### Структура тестов: AAA паттерн

```java
@Test
void testUserRegistration() {
    // Arrange (подготовка)
    UserService service = new UserService();
    User newUser = new User("john@example.com", "password123");
    
    // Act (действие)
    boolean result = service.register(newUser);
    
    // Assert (проверка)
    assertTrue(result);
    assertNotNull(service.findByEmail("john@example.com"));
}
```

### Принцип единственной ответственности

Каждый тест должен проверять только один аспект поведения:

```java
// Плохо
@Test
void testUserOperations() {
    User user = service.create("John");
    assertEquals("John", user.getName());
    
    user.setName("Jane");
    service.update(user);
    assertEquals("Jane", service.findById(user.getId()).getName());
    
    service.delete(user.getId());
    assertNull(service.findById(user.getId()));
}

// Хорошо — разделить на три теста
@Test
void createUser_shouldSetCorrectName() {
    User user = service.create("John");
    assertEquals("John", user.getName());
}

@Test
void updateUser_shouldChangeName() {
    User user = service.create("John");
    user.setName("Jane");
    service.update(user);
    assertEquals("Jane", service.findById(user.getId()).getName());
}

@Test
void deleteUser_shouldRemoveFromDatabase() {
    User user = service.create("John");
    service.delete(user.getId());
    assertNull(service.findById(user.getId()));
}
```

### Независимость тестов

Тесты не должны зависеть друг от друга или от порядка выполнения:

```java
// Плохо — тесты зависят друг от друга
private static User user;

@Test
void test1_createUser() {
    user = service.create("John");
}

@Test
void test2_updateUser() {
    user.setName("Jane");  // зависит от test1
    service.update(user);
}

// Хорошо — каждый тест независим
@Test
void createUser() {
    User user = service.create("John");
    assertNotNull(user);
}

@Test
void updateUser() {
    User user = service.create("John");  // создаём свои данные
    user.setName("Jane");
    service.update(user);
    assertEquals("Jane", service.findById(user.getId()).getName());
}
```

### Тестирование исключений

```java
// Явная проверка типа и сообщения исключения
@Test
void divideByZero_shouldThrowWithMessage() {
    ArithmeticException exception = assertThrows(
        ArithmeticException.class,
        () -> calculator.divide(10, 0)
    );
    
    assertEquals("/ by zero", exception.getMessage());
}
```

### Использование тестовых данных

#### Test Fixtures
```java
class OrderTest {
    
    private Order order;
    private Product product;
    
    @BeforeEach
    void setUp() {
        product = new Product("Laptop", 1000.0);
        order = new Order();
    }
    
    @Test
    void addProduct_shouldIncreaseTotal() {
        order.addProduct(product, 2);
        assertEquals(2000.0, order.getTotal());
    }
}
```

#### Builder Pattern для тестовых данных
```java
class UserTestDataBuilder {
    private String email = "default@example.com";
    private String name = "Default User";
    private int age = 25;
    
    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }
    
    public UserTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }
    
    public UserTestDataBuilder withAge(int age) {
        this.age = age;
        return this;
    }
    
    public User build() {
        return new User(email, name, age);
    }
}

// Использование
@Test
void testAdultUser() {
    User user = new UserTestDataBuilder()
        .withAge(30)
        .build();
    
    assertTrue(user.isAdult());
}
```

### Покрытие кода тестами

Стремитесь к высокому покрытию, но помните:
- **Не всегда 100% покрытие — это цель**. Важнее качество тестов.
- **Покрывайте критичную бизнес-логику**.
- **Не тестируйте геттеры/сеттеры** без логики.
- **Используйте инструменты**: JaCoCo, Cobertura для измерения покрытия.

### Мокирование и стабы

Для изоляции тестов используйте библиотеки мокирования (Mockito, EasyMock):

```java
@Test
void testWithMockito() {
    // Создание мока
    UserRepository repository = mock(UserRepository.class);
    
    // Настройка поведения
    when(repository.findById(1L))
        .thenReturn(Optional.of(new User("John")));
    
    // Использование
    UserService service = new UserService(repository);
    User user = service.getUser(1L);
    
    // Проверка
    assertEquals("John", user.getName());
    verify(repository, times(1)).findById(1L);
}
```

---

## Вопросы для самопроверки

### Базовые вопросы

1. **Какие основные различия между JUnit 4 и JUnit 5?**
   - JUnit 5 имеет модульную архитектуру (Platform, Jupiter, Vintage)
   - Новые аннотации (`@BeforeEach` вместо `@Before`, `@BeforeAll` вместо `@BeforeClass`)
   - Поддержка лямбда-выражений в assertions
   - Параметризованные тесты встроены
   - Расширенная модель расширений
   - Условное выполнение тестов

2. **Что такое assertion и зачем он нужен?**
   - Assertion — это проверка ожидаемого результата в тесте
   - Если проверка не пройдена, тест считается проваленным
   - JUnit предоставляет класс `Assertions` с множеством методов

3. **В чём разница между `@BeforeEach` и `@BeforeAll`?**
   - `@BeforeEach` выполняется перед каждым тестовым методом
   - `@BeforeAll` выполняется один раз перед всеми тестами (должен быть `static`)

4. **Зачем использовать `@DisplayName`?**
   - Для создания читаемых описаний тестов
   - Полезно для генерации понятных отчётов
   - Позволяет использовать спецсимволы и пробелы

5. **Как проверить, что метод выбросил исключение?**
   ```java
   assertThrows(IllegalArgumentException.class, () -> {
       method.throwException();
   });
   ```

### Продвинутые вопросы

6. **Что такое параметризованные тесты и когда их использовать?**
   - Позволяют запускать один тест с разными наборами данных
   - Используются для проверки одного поведения на множестве входных значений
   - Требуют аннотации `@ParameterizedTest` и источника данных

7. **Объясните разницу между `assertEquals` и `assertSame`.**
   - `assertEquals` проверяет равенство через метод `equals()`
   - `assertSame` проверяет идентичность (ссылаются ли на один объект через `==`)

8. **Что такое Test Lifecycle в JUnit 5?**
   - Определяет, когда создаётся экземпляр тестового класса
   - `PER_METHOD` (по умолчанию) — новый экземпляр для каждого теста
   - `PER_CLASS` — один экземпляр для всех тестов класса

9. **Как организовать связанные тесты в JUnit?**
   - Использовать вложенные классы с `@Nested`
   - Группировать по функциональности
   - Применять теги `@Tag` для категоризации

10. **Что такое Extensions в JUnit 5 и приведите примеры использования.**
    - Механизм для добавления поведения к тестам
    - Примеры: логирование, замер времени, внедрение зависимостей
    - Реализуют интерфейсы Extension API

### Практические вопросы

11. **Как правильно тестировать приватные методы?**
    - Тестировать через публичные методы, которые их вызывают
    - Если приватный метод сложен и требует отдельного тестирования, возможно, это признак нарушения Single Responsibility

12. **Что такое AAA паттерн в тестировании?**
    - Arrange (подготовка) — настройка данных и объектов
    - Act (действие) — выполнение тестируемого метода
    - Assert (проверка) — проверка результата

13. **Как обеспечить изоляцию тестов?**
    - Не использовать статические переменные между тестами
    - Очищать состояние в `@AfterEach`
    - Использовать моки для зависимостей
    - Каждый тест создаёт свои тестовые данные

14. **Когда использовать `@Disabled`?**
    - Временно отключить тест, который падает из-за известного бага
    - Тест для функции, которая ещё не реализована
    - Лучше оставить комментарий с причиной отключения

15. **Что такое Test Doubles и какие их виды существуют?**
    - Mock — объект с запрограммированными ожиданиями
    - Stub — объект с предопределёнными ответами
    - Fake — упрощённая рабочая реализация
    - Spy — обёртка над реальным объектом
    - Dummy — объект для заполнения параметров

### Вопросы на интеграцию

16. **Как настроить параллельное выполнение тестов в Maven?**
    ```xml
    <configuration>
        <parallel>methods</parallel>
        <threadCount>4</threadCount>
    </configuration>
    ```

17. **Как запустить только тесты с определёнными тегами?**
    - Maven: `mvn test -Dgroups="fast"`
    - Gradle: `includeTags 'fast'` в конфигурации

18. **Какие инструменты можно использовать для измерения покрытия кода?**
    - JaCoCo (Java Code Coverage)
    - Cobertura
    - Встроенные инструменты IDE (IntelliJ IDEA, Eclipse)

19. **В чём разница между модульными и интеграционными тестами?**
    - Модульные тесты проверяют отдельные компоненты в изоляции
    - Интеграционные тесты проверяют взаимодействие компонентов
    - Модульные тесты быстрее и проще в поддержке

20. **Что такое TDD (Test-Driven Development)?**
    - Методология, где тесты пишутся до кода
    - Цикл: Red (тест падает) → Green (тест проходит) → Refactor (улучшение кода)
    - Помогает проектировать API и улучшает покрытие кода

---

## Заключение

JUnit — мощный и гибкий фреймворк для тестирования Java-приложений. Знание его возможностей позволяет создавать надёжные, поддерживаемые тесты, которые обеспечивают уверенность в качестве кода. Регулярная практика написания тестов и следование лучшим практикам помогают разрабатывать более чистый и устойчивый к ошибкам код.

### Дополнительные ресурсы
- [Официальная документация JUnit 5](https://junit.org/junit5/docs/current/user-guide/)
- [JUnit 5 на GitHub](https://github.com/junit-team/junit5)
- [Mockito — фреймворк для мокирования](https://site.mockito.org/)
- [AssertJ — библиотека для fluent assertions](https://assertj.github.io/doc/)
