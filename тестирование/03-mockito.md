# Mockito — фреймворк для создания тестовых двойников

Mockito — это фреймворк для создания тестовых двойников (mock objects), который позволяет изолировать тестируемый код от его зависимостей. Благодаря простоте API и интеграции с JUnit 5, он стал стандартом де-факто для юнит-тестирования в Java.

## Содержание

1. [Концепция тестовых двойников](#концепция-тестовых-двойников)
2. [Основные аннотации Mockito](#основные-аннотации-mockito)
3. [Основное API Mockito](#основное-api-mockito)
4. [Стаббинг поведения](#стаббинг-поведения)
5. [Проверка вызовов (Verification)](#проверка-вызовов-verification)
6. [ArgumentCaptor — захват аргументов](#argumentcaptor--захват-аргументов)
7. [Исключения и альтернативные ответы](#исключения-и-альтернативные-ответы)
8. [Spy — частичные моки](#spy--частичные-моки)
9. [Пример реального теста](#пример-реального-теста)
10. [Распространённые ошибки](#распространённые-ошибки)
11. [Best practices](#best-practices)
12. [Вопросы для самопроверки](#вопросы-для-самопроверки)

---

## Концепция тестовых двойников

Тестовые двойники — это объекты, которые заменяют реальные зависимости в тестах. Они позволяют изолировать поведение тестируемого класса.

### Типы тестовых двойников

* **Dummy** — объект-заглушка, не используемый в логике, просто чтобы заполнить параметр.
  ```java
  // Пример: объект передаётся в конструктор, но не используется в тесте
  Logger dummyLogger = mock(Logger.class);
  Service service = new Service(dummyLogger);
  ```

* **Stub** — возвращает предопределённые ответы (например, фиктивные данные из базы).
  ```java
  UserRepository stub = mock(UserRepository.class);
  when(stub.findById(1L)).thenReturn(Optional.of(new User("John")));
  ```

* **Mock** — имитирует зависимость и позволяет проверять взаимодействия (вызовы методов).
  ```java
  EmailSender mock = mock(EmailSender.class);
  service.registerUser("test@example.com");
  verify(mock).send("test@example.com", "Welcome!");
  ```

* **Spy** — частичный мок: вызывает реальные методы, но при необходимости может подменять некоторые.
  ```java
  List<String> spyList = spy(new ArrayList<>());
  spyList.add("Hello");
  verify(spyList).add("Hello");
  assertEquals(1, spyList.size()); // реальный метод
  ```

* **Fake** — упрощённая реализация, например, in-memory версия репозитория.
  ```java
  // Обычно не создаётся через Mockito, а реализуется вручную
  class FakeUserRepository implements UserRepository {
      private Map<Long, User> users = new HashMap<>();
      // ... упрощённая реализация
  }
  ```

---

## Основные аннотации Mockito

### Зависимость (Maven)

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>

<!-- Для JUnit 5 -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <version>5.7.0</version>
    <scope>test</scope>
</dependency>
```

### Таблица аннотаций

| Аннотация                             | Назначение                                  | Пример                                 |
| ------------------------------------- | ------------------------------------------- | -------------------------------------- |
| `@Mock`                               | Создаёт мок-объект зависимости              | `@Mock UserRepository repo;`           |
| `@InjectMocks`                        | Внедряет моки в тестируемый класс           | `@InjectMocks UserService service;`    |
| `@Spy`                                | Создаёт частичный мок реального объекта     | `@Spy List<String> list;`              |
| `@Captor`                             | Используется для захвата аргументов вызовов | `@Captor ArgumentCaptor<User> captor;` |
| `@ExtendWith(MockitoExtension.class)` | Активирует поддержку Mockito в JUnit 5      | `@ExtendWith(MockitoExtension.class)`  |

### Пример использования аннотаций

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private UserService service;

    @Test
    void shouldCreateUser() {
        User user = new User("john@example.com");
        when(repository.save(any(User.class))).thenReturn(user);

        User created = service.createUser("john@example.com");

        assertNotNull(created);
        verify(emailSender).send("john@example.com", "Welcome!");
    }
}
```

---

## Основное API Mockito

### Создание моков

#### Через аннотации (рекомендуется)
```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    @Mock
    private UserRepository repo;
}
```

#### Программно
```java
UserRepository repo = mock(UserRepository.class);
EmailSender sender = mock(EmailSender.class);
```

#### Инициализация аннотаций вручную
```java
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

---

## Стаббинг поведения

Стаббинг — это настройка поведения мок-объектов.

### Базовый стаббинг с `when().thenReturn()`

```java
@Test
void testBasicStubbing() {
    UserRepository repo = mock(UserRepository.class);
    
    // Настройка поведения
    when(repo.findById(1L)).thenReturn(Optional.of(new User(1L, "Alice")));
    
    // Использование
    Optional<User> user = repo.findById(1L);
    
    assertTrue(user.isPresent());
    assertEquals("Alice", user.get().getName());
}
```

### Множественные вызовы

```java
@Test
void testMultipleCalls() {
    Iterator<String> mock = mock(Iterator.class);
    
    // Первый вызов вернёт "Hello", второй — "World"
    when(mock.next())
        .thenReturn("Hello")
        .thenReturn("World");
    
    assertEquals("Hello", mock.next());
    assertEquals("World", mock.next());
}
```

### Стаббинг с любыми аргументами (Matchers)

```java
@Test
void testArgumentMatchers() {
    UserRepository repo = mock(UserRepository.class);
    
    // Любой аргумент
    when(repo.findById(anyLong())).thenReturn(Optional.of(new User("Default")));
    
    // Конкретное значение
    when(repo.findById(eq(1L))).thenReturn(Optional.of(new User("Alice")));
    
    // Проверка
    assertEquals("Alice", repo.findById(1L).get().getName());
    assertEquals("Default", repo.findById(999L).get().getName());
}
```

#### Популярные матчеры

| Матчер                      | Описание                                   |
| --------------------------- | ------------------------------------------ |
| `any()`                     | Любой объект                               |
| `any(Class.class)`          | Любой объект указанного типа               |
| `anyInt()`, `anyLong()`     | Любое примитивное значение                 |
| `anyString()`               | Любая строка                               |
| `eq(value)`                 | Точное совпадение со значением             |
| `isNull()`, `isNotNull()`   | Проверка на null                           |
| `contains("text")`          | Строка содержит подстроку                  |
| `startsWith()`, `endsWith()`| Проверка начала/конца строки               |
| `argThat(lambda)`           | Пользовательский матчер                    |

### Стаббинг с вычислением на основе аргументов

```java
@Test
void testAnswer() {
    UserRepository repo = mock(UserRepository.class);
    
    when(repo.save(any(User.class))).thenAnswer(invocation -> {
        User user = invocation.getArgument(0);
        user.setId(100L);
        return user;
    });
    
    User user = new User("alice@example.com");
    User saved = repo.save(user);
    
    assertEquals(100L, saved.getId());
}
```

### Стаббинг void методов

```java
@Test
void testVoidMethod() {
    EmailSender sender = mock(EmailSender.class);
    
    // void метод, который ничего не делает (по умолчанию)
    doNothing().when(sender).send(anyString(), anyString());
    
    // void метод, который выбрасывает исключение
    doThrow(new RuntimeException("SMTP Error"))
        .when(sender).send("invalid@", anyString());
    
    assertThrows(RuntimeException.class, 
        () -> sender.send("invalid@", "Hello"));
}
```

---

## Проверка вызовов (Verification)

Verification позволяет убедиться, что метод был вызван с определёнными аргументами.

### Базовая проверка

```java
@Test
void testVerification() {
    UserRepository repo = mock(UserRepository.class);
    
    repo.findById(1L);
    repo.findById(1L);
    
    // Проверка, что метод был вызван
    verify(repo).findById(1L);
    
    // Проверка количества вызовов
    verify(repo, times(2)).findById(1L);
}
```

### Различные режимы проверки

```java
@Test
void testVerificationModes() {
    UserRepository repo = mock(UserRepository.class);
    
    repo.save(new User("Alice"));
    repo.save(new User("Bob"));
    
    // Хотя бы один раз
    verify(repo, atLeastOnce()).save(any(User.class));
    
    // Не более N раз
    verify(repo, atMost(3)).save(any(User.class));
    
    // Ровно N раз
    verify(repo, times(2)).save(any(User.class));
    
    // Никогда не вызывался
    verify(repo, never()).delete(any(User.class));
}
```

### Проверка порядка вызовов

```java
@Test
void testInOrder() {
    List<String> mock = mock(List.class);
    
    mock.add("First");
    mock.add("Second");
    
    InOrder inOrder = inOrder(mock);
    inOrder.verify(mock).add("First");
    inOrder.verify(mock).add("Second");
}
```

### Проверка отсутствия взаимодействий

```java
@Test
void testNoInteractions() {
    UserRepository repo = mock(UserRepository.class);
    
    // Проверка, что с моком вообще не взаимодействовали
    verifyNoInteractions(repo);
}
```

---

## ArgumentCaptor — захват аргументов

ArgumentCaptor позволяет перехватывать аргументы, переданные в мок, для последующей проверки.

### Базовое использование

```java
@Test
void testArgumentCaptor() {
    UserRepository repo = mock(UserRepository.class);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    
    UserService service = new UserService(repo);
    service.createUser("alice@example.com", "Alice");
    
    // Захватываем аргумент
    verify(repo).save(captor.capture());
    
    // Проверяем захваченный объект
    User captured = captor.getValue();
    assertEquals("alice@example.com", captured.getEmail());
    assertEquals("Alice", captured.getName());
}
```

### Захват множественных вызовов

```java
@Test
void testMultipleCaptures() {
    EmailSender sender = mock(EmailSender.class);
    ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
    
    sender.send("alice@example.com", "Hello");
    sender.send("bob@example.com", "Hi");
    
    verify(sender, times(2)).send(emailCaptor.capture(), anyString());
    
    List<String> emails = emailCaptor.getAllValues();
    assertEquals(2, emails.size());
    assertTrue(emails.contains("alice@example.com"));
    assertTrue(emails.contains("bob@example.com"));
}
```

### С аннотацией @Captor

```java
@ExtendWith(MockitoExtension.class)
class MyTest {
    
    @Mock
    private UserRepository repo;
    
    @Captor
    private ArgumentCaptor<User> userCaptor;
    
    @Test
    void testWithAnnotation() {
        repo.save(new User("test@example.com"));
        
        verify(repo).save(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
    }
}
```

---

## Исключения и альтернативные ответы

### Выбрасывание исключений

```java
@Test
void testException() {
    UserRepository repo = mock(UserRepository.class);
    
    when(repo.findById(99L))
        .thenThrow(new RuntimeException("User not found"));
    
    assertThrows(RuntimeException.class, () -> {
        repo.findById(99L);
    });
}
```

### Исключения для void методов

```java
@Test
void testVoidException() {
    EmailSender sender = mock(EmailSender.class);
    
    doThrow(new RuntimeException("SMTP Error"))
        .when(sender).send(anyString(), anyString());
    
    assertThrows(RuntimeException.class, 
        () -> sender.send("test@example.com", "Hello"));
}
```

### Последовательные ответы

```java
@Test
void testConsecutiveCalls() {
    UserRepository repo = mock(UserRepository.class);
    
    when(repo.findById(1L))
        .thenReturn(Optional.of(new User("Alice")))
        .thenReturn(Optional.empty())
        .thenThrow(new RuntimeException("DB Error"));
    
    assertTrue(repo.findById(1L).isPresent());
    assertFalse(repo.findById(1L).isPresent());
    assertThrows(RuntimeException.class, () -> repo.findById(1L));
}
```

---

## Spy — частичные моки

Spy создаёт обёртку над реальным объектом, позволяя вызывать реальные методы и при необходимости подменять некоторые из них.

### Создание Spy

```java
@Test
void testSpy() {
    List<String> list = new ArrayList<>();
    List<String> spyList = spy(list);
    
    // Реальные методы работают
    spyList.add("Hello");
    spyList.add("World");
    
    assertEquals(2, spyList.size());
    
    // Можно верифицировать вызовы
    verify(spyList).add("Hello");
    verify(spyList).add("World");
}
```

### Стаббинг методов Spy

```java
@Test
void testSpyStubbing() {
    List<String> list = new ArrayList<>();
    list.add("Real Item");
    
    List<String> spyList = spy(list);
    
    // ВНИМАНИЕ: when() вызовет реальный метод!
    // Используйте doReturn() для spy
    doReturn("Mocked").when(spyList).get(0);
    
    assertEquals("Mocked", spyList.get(0));
    assertEquals(1, spyList.size()); // реальный метод
}
```

### Разница между when() и doReturn() для Spy

```java
@Test
void testWhenVsDoReturn() {
    List<String> spyList = spy(new ArrayList<>());
    
    // ПЛОХО: when() вызовет реальный get(0), что приведёт к ошибке
    // when(spyList.get(0)).thenReturn("Mocked");  // IndexOutOfBoundsException!
    
    // ХОРОШО: doReturn() не вызывает реальный метод
    doReturn("Mocked").when(spyList).get(0);
    
    assertEquals("Mocked", spyList.get(0));
}
```

### Spy с аннотацией @Spy

```java
@ExtendWith(MockitoExtension.class)
class SpyTest {
    
    @Spy
    private List<String> spyList = new ArrayList<>();
    
    @Test
    void testSpyAnnotation() {
        spyList.add("Test");
        
        verify(spyList).add("Test");
        assertEquals(1, spyList.size());
    }
}
```

---

## Пример реального теста

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock 
    private UserRepository repo;
    
    @Mock 
    private EmailSender emailSender;
    
    @InjectMocks 
    private UserService service;
    
    @Captor 
    private ArgumentCaptor<User> userCaptor;
    
    @Captor
    private ArgumentCaptor<String> emailCaptor;

    @Test
    @DisplayName("Регистрация пользователя: успешный сценарий")
    void registerUser_success() {
        // Arrange
        when(repo.existsByEmail("alice@example.com")).thenReturn(false);
        when(repo.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setVerified(false);
            return user;
        });
        
        // Act
        User created = service.register("alice@example.com");
        
        // Assert
        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertFalse(created.isVerified());
        
        // Проверка сохранения пользователя
        verify(repo).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals("alice@example.com", savedUser.getEmail());
        
        // Проверка отправки email
        verify(emailSender).send(
            eq("alice@example.com"), 
            emailCaptor.capture(), 
            contains("Confirm your email")
        );
        
        String emailSubject = emailCaptor.getValue();
        assertTrue(emailSubject.contains("Welcome"));
    }
    
    @Test
    @DisplayName("Регистрация пользователя: email уже существует")
    void registerUser_emailAlreadyExists() {
        // Arrange
        when(repo.existsByEmail("alice@example.com")).thenReturn(true);
        
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> {
            service.register("alice@example.com");
        });
        
        // Проверка, что save не был вызван
        verify(repo, never()).save(any(User.class));
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
```

---

## Распространённые ошибки

### 1. Отсутствие аннотации @ExtendWith(MockitoExtension.class)

```java
// ПЛОХО: моки не инициализируются
class UserServiceTest {
    @Mock private UserRepository repo;  // будет null!
    
    @Test
    void test() {
        repo.findById(1L);  // NullPointerException
    }
}

// ХОРОШО
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository repo;
}
```

### 2. Неверное использование when() с spy

```java
// ПЛОХО: вызовет реальный метод
List<String> spyList = spy(new ArrayList<>());
when(spyList.get(0)).thenReturn("Mocked");  // IndexOutOfBoundsException!

// ХОРОШО: используйте doReturn() для spy
doReturn("Mocked").when(spyList).get(0);
```

### 3. Избыточные моки

```java
// ПЛОХО: мокаем всё подряд
@Mock private String name;
@Mock private Integer count;

// ХОРОШО: мокаем только сложные зависимости
String name = "John";  // используем реальные объекты
Integer count = 5;
@Mock private UserRepository repo;  // мокаем внешние зависимости
```

### 4. Смешивание матчеров и конкретных значений

```java
// ПЛОХО: нельзя смешивать
verify(repo).save(eq(user), "literal");

// ХОРОШО: используйте матчеры для всех аргументов
verify(repo).save(eq(user), eq("literal"));

// ИЛИ: используйте конкретные значения для всех
verify(repo).save(user, "literal");
```

### 5. Несогласованные equals() в проверках

```java
// ПЛОХО: если equals() не переопределён
User user1 = new User("test@example.com");
User user2 = new User("test@example.com");
verify(repo).save(user2);  // не сработает, если user1 != user2

// ХОРОШО: используйте ArgumentCaptor или матчеры
verify(repo).save(argThat(u -> u.getEmail().equals("test@example.com")));
```

### 6. Проверка реализации вместо поведения

```java
// ПЛОХО: тест завязан на реализацию
verify(repo, times(1)).findById(1L);
verify(cache, times(1)).put(1L, user);

// ХОРОШО: проверяйте результат
User user = service.getUser(1L);
assertEquals("John", user.getName());
```

---

## Best practices

### 1. Не мокай то, что не нужно

```java
// ПЛОХО: мокаем простые объекты
@Mock private String username;
@Mock private LocalDateTime time;

// ХОРОШО: используем реальные объекты
private String username = "john";
private LocalDateTime time = LocalDateTime.now();

// Мокаем только сложные зависимости
@Mock private UserRepository repo;
@Mock private EmailSender emailSender;
```

### 2. Проверяй результат, а не внутреннюю реализацию

```java
// ПЛОХО: хрупкий тест, завязанный на детали реализации
@Test
void test() {
    service.processUser(1L);
    verify(repo).findById(1L);
    verify(cache).get(1L);
    verify(logger).info(anyString());
}

// ХОРОШО: проверяем поведение
@Test
void test() {
    User user = service.processUser(1L);
    assertNotNull(user);
    assertEquals("Processed", user.getStatus());
}
```

### 3. Изолируй внешний мир

Мокай:
- БД и репозитории
- HTTP-клиенты и внешние API
- Message Brokers (Kafka, RabbitMQ)
- Email-сервисы
- Файловые системы
- Системное время (Clock)

Не мокай:
- Простые POJO
- Value Objects
- Утилитные классы без внешних зависимостей

### 4. Для сложных зависимостей используй @Spy или Fake

```java
// Spy для частичного моканья
@Spy
private UserService userService;

@Test
void test() {
    doReturn(true).when(userService).isAdmin(anyLong());
    // остальные методы работают как обычно
}

// Fake для упрощённой реализации
class FakeUserRepository implements UserRepository {
    private Map<Long, User> users = new HashMap<>();
    
    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }
    // ...
}
```

### 5. Именуй тесты по принципу

```java
// Паттерн: methodName_condition_expectedResult

@Test
void registerUser_validEmail_createsNewUser() { }

@Test
void registerUser_existingEmail_throwsException() { }

@Test
void calculateDiscount_premiumUser_returns20Percent() { }
```

### 6. Используй @DisplayName для читаемости

```java
@Test
@DisplayName("Регистрация пользователя с валидным email создаёт нового пользователя")
void registerUser_validEmail() {
    // ...
}
```

### 7. Группируй связанные тесты с @Nested

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Nested
    @DisplayName("Регистрация пользователя")
    class Registration {
        
        @Test
        @DisplayName("успешная регистрация с валидным email")
        void success() { }
        
        @Test
        @DisplayName("ошибка при существующем email")
        void emailExists() { }
    }
    
    @Nested
    @DisplayName("Обновление профиля")
    class ProfileUpdate {
        // ...
    }
}
```

### 8. Избегай статических моков без необходимости

Mockito 3.4+ поддерживает статические моки, но используйте их осторожно:

```java
@Test
void testStaticMethod() {
    try (MockedStatic<StringUtils> utils = mockStatic(StringUtils.class)) {
        utils.when(() -> StringUtils.isEmpty("test")).thenReturn(true);
        
        assertTrue(StringUtils.isEmpty("test"));
    }
}
```

### 9. Не тестируй приватные методы напрямую

```java
// ПЛОХО: изменение видимости для тестирования
private void processInternal() { }  // изменили на package-private для теста

// ХОРОШО: тестируйте через публичный API
@Test
void publicMethod_shouldCallInternalLogic() {
    service.publicMethod();  // внутри вызывается processInternal()
    // проверяем конечный результат
}
```

### 10. Очищай ресурсы после тестов

```java
@AfterEach
void tearDown() {
    // Mockito автоматически очищает моки между тестами
    // Но если используете статические моки:
    Mockito.framework().clearInlineMocks();
}
```

---

## Вопросы для самопроверки

### Базовые вопросы

1. **Что такое мок и чем он отличается от стаба?**
   - Мок позволяет проверять взаимодействия (verify), стаб только возвращает заранее определённые значения
   - Мок фокусируется на том, "как" метод был вызван, стаб — на том, "что" он возвращает

2. **Для чего нужна аннотация @InjectMocks?**
   - Создаёт экземпляр тестируемого класса и автоматически внедряет в него зависимости, помеченные @Mock

3. **Как проверить, что метод был вызван с определёнными аргументами?**
   ```java
   verify(mock).method(eq("expectedValue"));
   ```

4. **В чём разница между @Mock и @Spy?**
   - @Mock создаёт полностью фиктивный объект
   - @Spy оборачивает реальный объект и вызывает его методы, если не переопределено

5. **Зачем нужен ArgumentCaptor?**
   - Для захвата аргументов, переданных в мок, с целью последующей детальной проверки

### Продвинутые вопросы

6. **Чем отличается when().thenReturn() от doReturn().when()?**
   - when().thenReturn() вызывает реальный метод (проблема для spy)
   - doReturn().when() не вызывает реальный метод (безопасно для spy)

7. **Как протестировать void метод, который должен выбросить исключение?**
   ```java
   doThrow(new RuntimeException()).when(mock).voidMethod();
   ```

8. **Что такое матчеры (matchers) и когда их использовать?**
   - Гибкие условия для аргументов: any(), eq(), contains()
   - Используются когда точное значение аргумента неважно или неизвестно

9. **Как проверить порядок вызовов методов?**
   ```java
   InOrder inOrder = inOrder(mock);
   inOrder.verify(mock).firstMethod();
   inOrder.verify(mock).secondMethod();
   ```

10. **Что делает verifyNoInteractions()?**
    - Проверяет, что с моком вообще не было никаких взаимодействий

### Практические вопросы

11. **Когда следует использовать Spy вместо Mock?**
    - Когда нужна реальная реализация с возможностью подмены отдельных методов
    - Для legacy кода или сложных объектов

12. **Почему не стоит мокать value objects?**
    - Они простые и не имеют внешних зависимостей
    - Моканье усложняет тесты без реальной пользы

13. **Как избежать хрупких тестов при использовании verify()?**
    - Проверяйте только важные взаимодействия
    - Фокусируйтесь на результате, а не на реализации

14. **Что такое "тестирование поведения" vs "тестирование состояния"?**
    - Поведение: проверка вызовов методов (verify)
    - Состояние: проверка результата (assertEquals)

15. **Как правильно тестировать метод, который использует LocalDateTime.now()?**
    - Внедрить Clock как зависимость и мокать его
    ```java
    @Mock private Clock clock;
    when(clock.instant()).thenReturn(fixedInstant);
    ```

### Вопросы на собеседовании

16. **Объясните разницу между моком и спаем на примере.**
    - Mock: полностью искусственный объект, все методы возвращают значения по умолчанию
    - Spy: реальный объект с возможностью подменить поведение отдельных методов

17. **Когда Mockito не подходит для тестирования?**
    - Для интеграционных тестов с реальными БД/API
    - Для тестирования статических методов (лучше рефакторинг)
    - Для тестирования приватных методов (признак плохого дизайна)

18. **Как тестировать асинхронный код с Mockito?**
    - В комбинации с Awaitility или CompletableFuture.join()
    ```java
    CompletableFuture<User> future = service.getUserAsync(1L);
    User user = future.join();
    verify(repo).findById(1L);
    ```

19. **Что такое "test double" и какие виды существуют?**
    - Общее название для объектов-заменителей
    - Виды: Dummy, Stub, Mock, Spy, Fake

20. **Как убедиться, что тест действительно тестирует что-то полезное?**
    - Mutation testing (например, PITest)
    - Намеренно сломать код и убедиться, что тест падает
    - Code coverage (JaCoCo) для проверки покрытия

---

## Заключение

Mockito — мощный инструмент для изоляции тестируемого кода от внешних зависимостей. Правильное использование моков делает тесты быстрыми, надёжными и независимыми от внешней инфраструктуры. Однако важно помнить о балансе: не стоит мокать всё подряд — тестируйте реальное поведение, а не детали реализации.

### Дополнительные ресурсы

- [Официальная документация Mockito](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Mockito на GitHub](https://github.com/mockito/mockito)
- [Baeldung: Mockito Tutorial](https://www.baeldung.com/mockito-series)
- [Test Doubles — Martin Fowler](https://martinfowler.com/bliki/TestDouble.html)

---

[← Назад к разделу Тестирование](README.md)
