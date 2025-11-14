# Основы языка и синтаксис


## Содержание

1. [Типы данных и литералы](#типы-данных-и-литералы)
2. [Управляющие конструкции](#управляющие-конструкции)
3. [Исключения в Java](#исключения-в-java)
   - [Иерархия исключений](#иерархия-исключений)
   - [Checked vs Unchecked исключения](#checked-vs-unchecked-исключения)
   - [Try-Catch-Finally](#try-catch-finally)
   - [Try-with-resources](#try-with-resources)
   - [Создание пользовательских исключений](#создание-пользовательских-исключений)
   - [Multi-catch и Suppressed Exceptions](#multi-catch-и-suppressed-exceptions)
   - [Best Practices](#best-practices-исключений)
4. [Классы, пакеты и модульность](#классы-пакеты-и-модульность)
5. [Работа со строками и форматированием](#работа-со-строками-и-форматированием)
6. [Ввод/вывод и взаимодействие с окружением](#вводвывод-и-взаимодействие-с-окружением)
7. [Best practices](#best-practices)
8. [Практические упражнения](#практические-упражнения)
9. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Типы данных и литералы
Java строго типизирована: каждый идентификатор имеет известный тип на этапе компиляции. Примитивы (`byte`, `short`, `int`,
`long`, `float`, `double`, `boolean`, `char`) хранятся в стеке и передаются по значению. Ссылочные типы (классы, интерфейсы,
массивы, перечисления) живут в куче и оперируют ссылками. Для работы с примитивами существуют обёртки (`Integer`, `Double`),
которые помогают интегрироваться с коллекциями и generics. Литералы поддерживают разделители `_`, суффиксы (`L`, `f`), числовые
системы (`0b`, `0x`), текстовые блоки (`"""`) и символы Unicode.

## Управляющие конструкции
- **Условия**: `if`, `switch` (включая `switch`-выражения с Java 14+). Новые `switch`-выражения позволяют возвращать значение и
  использовать `yield`.
- **Циклы**: `for`, enhanced `for`, `while`, `do-while`. Используйте `break`/`continue` с метками осторожно.

## Исключения в Java

Исключения — это механизм обработки ошибок и исключительных ситуаций в Java. Правильная обработка исключений критична 
для создания надёжных и maintainable приложений.

### Иерархия исключений

Все исключения в Java наследуются от класса `Throwable`. Иерархия построена следующим образом:

```
Throwable
├── Error
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   ├── VirtualMachineError
│   └── ... (другие системные ошибки)
└── Exception
    ├── IOException (checked)
    ├── SQLException (checked)
    ├── ClassNotFoundException (checked)
    └── RuntimeException (unchecked)
        ├── NullPointerException
        ├── IllegalArgumentException
        ├── IndexOutOfBoundsException
        ├── ArithmeticException
        └── ... (другие runtime исключения)
```

**Основные категории:**

1. **Error** — серьёзные проблемы, от которых приложение обычно не может восстановиться:
```java
// Примеры Error (не следует ловить)
try {
    recursiveMethod();  // Может вызвать StackOverflowError
} catch (StackOverflowError e) {  // Плохая практика!
    // Обычно невозможно корректно обработать
}
```

2. **Exception** — проблемы, которые приложение может и должно обрабатывать:
```java
// Checked exceptions — обязательно обрабатывать
try {
    FileReader reader = new FileReader("file.txt");  // IOException
} catch (IOException e) {
    // Обработка обязательна
}

// Unchecked exceptions (RuntimeException) — обработка опциональна
String str = null;
str.length();  // NullPointerException в runtime
```

**Методы Throwable:**
```java
String getMessage()           // Сообщение об ошибке
String getLocalizedMessage()  // Локализованное сообщение
Throwable getCause()         // Причина исключения
StackTraceElement[] getStackTrace()  // Stack trace
void printStackTrace()       // Вывод stack trace
void addSuppressed(Throwable)  // Добавить подавленное исключение
Throwable[] getSuppressed()  // Получить подавленные исключения
```

### Checked vs Unchecked исключения

#### Checked Exceptions (Проверяемые исключения)

**Характеристики:**
- Наследуются от `Exception` (но не от `RuntimeException`)
- Компилятор требует обязательной обработки или декларации в `throws`
- Представляют ожидаемые проблемы, которые можно предвидеть

**Примеры:**
```java
// IOException
try {
    BufferedReader reader = new BufferedReader(new FileReader("config.txt"));
    String line = reader.readLine();
    reader.close();
} catch (IOException e) {
    System.err.println("Error reading file: " + e.getMessage());
}

// SQLException
try {
    Connection conn = DriverManager.getConnection(url, user, password);
    Statement stmt = conn.createStatement();
} catch (SQLException e) {
    System.err.println("Database error: " + e.getMessage());
}

// Декларация через throws
public void readFile(String path) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(path));
    // ... работа с файлом
}
```

**Когда использовать checked exceptions:**
- Ожидаемые проблемы (файл не найден, сетевая ошибка)
- Caller может обработать ошибку разумным образом
- Внешние ресурсы (файлы, сеть, БД)

#### Unchecked Exceptions (Непроверяемые исключения)

**Характеристики:**
- Наследуются от `RuntimeException`
- Не требуют обязательной обработки
- Представляют ошибки программирования

**Примеры:**
```java
// NullPointerException
String str = null;
int length = str.length();  // NPE в runtime

// IllegalArgumentException
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("Invalid age: " + age);
    }
    this.age = age;
}

// ArithmeticException
int result = 10 / 0;  // ArithmeticException: / by zero

// IndexOutOfBoundsException
List<String> list = new ArrayList<>();
String item = list.get(5);  // IndexOutOfBoundsException
```

**Когда использовать unchecked exceptions:**
- Ошибки программирования (null pointer, illegal argument)
- Нарушение контракта API
- Условия, которые не должны происходить в корректной программе

**Сравнение:**

| Характеристика | Checked Exception | Unchecked Exception |
|----------------|-------------------|---------------------|
| Базовый класс | Exception | RuntimeException |
| Обязательная обработка | Да | Нет |
| Проверка компилятором | Да | Нет |
| Типичные причины | Внешние проблемы | Ошибки программирования |
| Примеры | IOException, SQLException | NullPointerException, IllegalArgumentException |
| Когда использовать | Ожидаемые проблемы | Нарушения контракта |

### Try-Catch-Finally

**Базовый синтаксис:**
```java
try {
    // Код, который может бросить исключение
    String content = readFile("data.txt");
} catch (FileNotFoundException e) {
    // Обработка конкретного исключения
    System.err.println("File not found: " + e.getMessage());
} catch (IOException e) {
    // Обработка более общего исключения
    System.err.println("IO error: " + e.getMessage());
} finally {
    // Выполняется всегда (даже если был return или exception)
    System.out.println("Cleanup completed");
}
```

**Множественные catch блоки:**
```java
try {
    processData();
} catch (FileNotFoundException e) {
    // Самое специфичное исключение первым
    log.error("File not found", e);
    useDefaultData();
} catch (IOException e) {
    // Более общее исключение
    log.error("IO error", e);
    throw new ProcessingException("Failed to read data", e);
} catch (Exception e) {
    // Самое общее исключение последним
    log.error("Unexpected error", e);
    throw new RuntimeException("Unexpected error", e);
}
```

**Finally блок:**
```java
FileInputStream fis = null;
try {
    fis = new FileInputStream("file.txt");
    // Работа с файлом
    return true;  // finally выполнится перед return
} catch (IOException e) {
    log.error("Error", e);
    return false;  // finally выполнится перед return
} finally {
    // Выполнится в любом случае
    if (fis != null) {
        try {
            fis.close();  // Освобождение ресурса
        } catch (IOException e) {
            log.error("Error closing file", e);
        }
    }
}
```

**Порядок выполнения:**
```java
public int demonstrateFinally() {
    try {
        System.out.println("1. Try block");
        return 1;
    } catch (Exception e) {
        System.out.println("2. Catch block");
        return 2;
    } finally {
        System.out.println("3. Finally block");
        // return 3;  // Плохая практика! Перезапишет return из try/catch
    }
}

// Вывод:
// 1. Try block
// 3. Finally block
// Возвращает: 1
```

### Try-with-resources

**Try-with-resources** (Java 7+) автоматически закрывает ресурсы, реализующие `AutoCloseable`.

**Базовый синтаксис:**
```java
// До Java 7 (verbose)
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("file.txt"));
    String line = reader.readLine();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    if (reader != null) {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// С Java 7+ (try-with-resources)
try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
    String line = reader.readLine();
} catch (IOException e) {
    e.printStackTrace();
}
// reader.close() вызывается автоматически
```

**Множественные ресурсы:**
```java
try (FileInputStream fis = new FileInputStream("input.txt");
     FileOutputStream fos = new FileOutputStream("output.txt");
     BufferedInputStream bis = new BufferedInputStream(fis);
     BufferedOutputStream bos = new BufferedOutputStream(fos)) {
    
    int data;
    while ((data = bis.read()) != -1) {
        bos.write(data);
    }
} catch (IOException e) {
    e.printStackTrace();
}
// Все ресурсы закрываются автоматически в обратном порядке
```

**Эффективный вариант (Java 9+):**
```java
BufferedReader reader = new BufferedReader(new FileReader("file.txt"));
try (reader) {  // Можно использовать final или effectively final переменные
    String line = reader.readLine();
}
```

**Создание AutoCloseable ресурса:**
```java
public class DatabaseConnection implements AutoCloseable {
    private Connection connection;
    
    public DatabaseConnection(String url) throws SQLException {
        this.connection = DriverManager.getConnection(url);
    }
    
    public void executeQuery(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeQuery(sql);
        }
    }
    
    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Connection closed");
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}

// Использование
try (DatabaseConnection db = new DatabaseConnection(url)) {
    db.executeQuery("SELECT * FROM users");
}  // close() вызывается автоматически
```

### Создание пользовательских исключений

**Базовая структура:**
```java
// Checked exception
public class DataValidationException extends Exception {
    public DataValidationException(String message) {
        super(message);
    }
    
    public DataValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Unchecked exception
public class InvalidConfigurationException extends RuntimeException {
    public InvalidConfigurationException(String message) {
        super(message);
    }
    
    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**С дополнительной информацией:**
```java
public class OrderProcessingException extends Exception {
    private final String orderId;
    private final OrderStatus status;
    
    public OrderProcessingException(String message, String orderId, OrderStatus status) {
        super(message);
        this.orderId = orderId;
        this.status = status;
    }
    
    public OrderProcessingException(String message, Throwable cause, 
                                    String orderId, OrderStatus status) {
        super(message, cause);
        this.orderId = orderId;
        this.status = status;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public OrderStatus getStatus() {
        return status;
    }
    
    @Override
    public String toString() {
        return String.format("%s [orderId=%s, status=%s]", 
            super.toString(), orderId, status);
    }
}
```

**Использование:**
```java
public void processOrder(Order order) throws OrderProcessingException {
    try {
        validateOrder(order);
        chargeCustomer(order);
        shipOrder(order);
    } catch (ValidationException e) {
        throw new OrderProcessingException(
            "Order validation failed",
            e,
            order.getId(),
            OrderStatus.VALIDATION_FAILED
        );
    } catch (PaymentException e) {
        throw new OrderProcessingException(
            "Payment failed",
            e,
            order.getId(),
            OrderStatus.PAYMENT_FAILED
        );
    }
}
```

### Multi-catch и Suppressed Exceptions

#### Multi-catch (Java 7+)

**Синтаксис:**
```java
// До Java 7
try {
    performOperation();
} catch (IOException e) {
    log.error("Error occurred", e);
    throw new OperationException("Operation failed", e);
} catch (SQLException e) {
    log.error("Error occurred", e);
    throw new OperationException("Operation failed", e);
}

// С Java 7+ (multi-catch)
try {
    performOperation();
} catch (IOException | SQLException e) {
    log.error("Error occurred", e);
    throw new OperationException("Operation failed", e);
}
```

**Ограничения multi-catch:**
```java
// Нельзя использовать связанные типы
try {
    // ...
} catch (Exception | IOException e) {  // Ошибка! IOException is a subclass of Exception
    // ...
}

// Переменная e является final
try {
    // ...
} catch (IOException | SQLException e) {
    e = new IOException();  // Ошибка компиляции! e is final
}
```

#### Suppressed Exceptions

**Suppressed exceptions** — исключения, которые произошли во время обработки другого исключения.

**Пример проблемы без try-with-resources:**
```java
public void readFile(String path) throws IOException {
    FileInputStream fis = new FileInputStream(path);
    try {
        // Читаем данные
        int data = fis.read();
        throw new IOException("Error reading data");  // Основное исключение
    } finally {
        fis.close();  // Если close() тоже бросит исключение, 
                      // исключение из try будет потеряно!
    }
}
```

**Решение с try-with-resources:**
```java
public void readFile(String path) throws IOException {
    try (FileInputStream fis = new FileInputStream(path)) {
        int data = fis.read();
        throw new IOException("Error reading data");  // Основное исключение
        // close() может бросить исключение, но оно будет suppressed
    }
}

// Обработка suppressed exceptions
try {
    readFile("data.txt");
} catch (IOException e) {
    System.err.println("Main exception: " + e.getMessage());
    
    Throwable[] suppressed = e.getSuppressed();
    for (Throwable t : suppressed) {
        System.err.println("Suppressed exception: " + t.getMessage());
    }
}
```

**Ручное добавление suppressed exceptions:**
```java
public void processMultipleFiles(List<String> paths) throws IOException {
    IOException mainException = null;
    
    for (String path : paths) {
        try {
            processFile(path);
        } catch (IOException e) {
            if (mainException == null) {
                mainException = e;  // Первое исключение — главное
            } else {
                mainException.addSuppressed(e);  // Остальные — suppressed
            }
        }
    }
    
    if (mainException != null) {
        throw mainException;
    }
}
```

### Best Practices исключений

#### 1. Ловите специфичные исключения

```java
// Плохо
try {
    performOperation();
} catch (Exception e) {  // Слишком общее!
    e.printStackTrace();
}

// Хорошо
try {
    performOperation();
} catch (FileNotFoundException e) {
    log.error("File not found", e);
    useDefaultConfiguration();
} catch (IOException e) {
    log.error("IO error", e);
    retryOperation();
}
```

#### 2. Не глушите исключения

```java
// Плохо
try {
    riskyOperation();
} catch (Exception e) {
    // Пустой catch — проглатывание исключения!
}

// Хорошо
try {
    riskyOperation();
} catch (Exception e) {
    log.error("Operation failed", e);
    // Или re-throw
    throw new OperationException("Failed", e);
}
```

#### 3. Используйте try-with-resources

```java
// Плохо
InputStream is = null;
try {
    is = new FileInputStream("file.txt");
    // ...
} finally {
    if (is != null) {
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Хорошо
try (InputStream is = new FileInputStream("file.txt")) {
    // ...
}
```

#### 4. Не используйте исключения для control flow

```java
// Плохо
try {
    int value = array[index];
} catch (ArrayIndexOutOfBoundsException e) {
    value = defaultValue;
}

// Хорошо
int value = (index >= 0 && index < array.length) 
    ? array[index] 
    : defaultValue;
```

#### 5. Сохраняйте cause исключения

```java
// Плохо
try {
    performDatabaseOperation();
} catch (SQLException e) {
    throw new BusinessException("Operation failed");  // Потеряли cause!
}

// Хорошо
try {
    performDatabaseOperation();
} catch (SQLException e) {
    throw new BusinessException("Operation failed", e);  // Сохранили cause
}
```

#### 6. Логируйте или бросайте, но не оба

```java
// Плохо
try {
    operation();
} catch (Exception e) {
    log.error("Error", e);
    throw e;  // Exception будет залогирован дважды!
}

// Хорошо
try {
    operation();
} catch (Exception e) {
    throw new OperationException("Failed", e);  // Caller залогирует
}

// Или
try {
    operation();
} catch (Exception e) {
    log.error("Error", e);
    return defaultValue;  // Обработали локально
}
```

#### 7. Документируйте исключения

```java
/**
 * Reads user data from the database.
 *
 * @param userId the ID of the user to read
 * @return the user data
 * @throws UserNotFoundException if user with given ID doesn't exist
 * @throws DatabaseException if database connection fails
 */
public User readUser(long userId) 
        throws UserNotFoundException, DatabaseException {
    // ...
}
```

#### 8. Используйте стандартные исключения

```java
// Предпочитайте стандартные исключения вместо custom
throw new IllegalArgumentException("Age must be positive");
throw new IllegalStateException("Object not initialized");
throw new UnsupportedOperationException("Read-only collection");
throw new NullPointerException("Name cannot be null");
```

**Часто используемые стандартные исключения:**

| Исключение | Когда использовать |
|------------|-------------------|
| `IllegalArgumentException` | Недопустимый аргумент метода |
| `IllegalStateException` | Некорректное состояние объекта |
| `NullPointerException` | Неожиданный null параметр |
| `IndexOutOfBoundsException` | Индекс вне допустимого диапазона |
| `UnsupportedOperationException` | Операция не поддерживается |
| `ConcurrentModificationException` | Concurrent модификация коллекции |

## Классы, пакеты и модульность
Код организуется в пакеты (`package`) и модули (`module-info.java`). Модуль декларирует экспортируемые пакеты и требуемые
зависимости. Для инкапсуляции используйте уровни доступа: `private`, package-private (по умолчанию), `protected`, `public`.
Вложенные классы делятся на статические (не требуют экземпляра) и внутренние (имеют ссылку на внешний объект). Классы можно
помечать как `sealed`, ограничивая наследников.

## Работа со строками и форматированием
Строки неизменяемы, поэтому частая конкатенация в цикле неэффективна — используйте `StringBuilder` или `StringBuffer`.
Начиная с Java 15 появились текстовые блоки, облегчающие создание многострочных литералов. Для форматирования применяйте
`String.format`, `MessageFormat` или `Formatter`. Сравнивайте строки методом `equals`, учитывая локаль (`Collator`) и нормализацию.

## Ввод/вывод и взаимодействие с окружением
Базовые операции ввода/вывода реализованы через `java.io` и `java.nio`. Для чтения из консоли используйте `Scanner` или
`BufferedReader`. Файлы и директории управляются классами `Files`, `Paths` (NIO.2). Для работы с процессами применяйте
`ProcessBuilder`, а для взаимодействия с системными свойствами — `System.getProperty`.

## Best practices
- Соблюдайте `Effective Java`: минимизируйте изменяемость, используйте фабрики, отдавайте предпочтение композиции.
- Документируйте публичные API через Javadoc (`/** ... */`).
- Следите за предупреждениями компилятора и включайте `-Xlint`.
- Пишите юнит-тесты (JUnit, TestNG) и покрывайте ими ключевые классы.

## Практические упражнения
1. Реализуйте калькулятор командной строки, поддерживающий текстовые блоки для подсказок и `switch`-выражения.
2. Напишите класс с несколькими уровнями вложенности и проанализируйте байт-код, чтобы увидеть синтетические ссылки.
3. Создайте модульное приложение (`module-info.java`), экспортируйте API и запустите его через `java --module-path`.

## Вопросы на собеседовании
1. **Чем отличаются примитивные и ссылочные типы?**
   *Ответ:* Примитивы хранят значение напрямую и не допускают `null`. Ссылочные типы содержат адрес объекта, могут быть `null` и
   обрабатываются сборщиком мусора. Примитивы быстрее и занимают меньше памяти, но не работают с generics без автоупаковки.
2. **Когда использовать `switch`-выражение вместо классического `switch`?**
   *Ответ:* Когда нужно вернуть значение или сократить шаблонный код `break`. `switch`-выражения обрабатывают `enum`, `String`,
   поддерживают множественные метки и гарантируют исчерпывающий анализ при работе с `enum`.
3. **Что такое `try-with-resources` и как оно работает?**
   *Ответ:* Конструкция автоматически закрывает ресурсы, которые реализуют `AutoCloseable`. В байт-коде компилятор разворачивает
   блок в `try-finally`, обрабатывая подавленные исключения (`Throwable.addSuppressed`).
4. **Зачем нужны `sealed` классы?**
   *Ответ:* `sealed` ограничивает множество наследников, что облегчает контроль инвариантов и делает `switch` по `enum`-подобным
   иерархиям безопасным. Это упрощает анализ и генерацию pattern matching.
5. **Почему строки неизменяемы и к чему это приводит?**
   *Ответ:* Иммутабельность обеспечивает безопасность (пулы строк, использование в ClassLoader), упрощает кеширование и делает
   объекты потокобезопасными. Недостаток — необходимость дополнительных копий при модификации, поэтому используют `StringBuilder`.
