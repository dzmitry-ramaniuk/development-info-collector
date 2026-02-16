# Factory Method (Фабричный метод)

Factory Method — порождающий паттерн проектирования, который определяет общий интерфейс для создания объектов в суперклассе, позволяя подклассам изменять тип создаваемых объектов.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Примеры использования](#примеры-использования)
6. [Factory Method vs Constructor](#factory-method-vs-constructor)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Factory Method используется когда:
- Заранее неизвестны типы и зависимости объектов, с которыми должен работать код
- Необходимо предоставить пользователям библиотеки или фреймворка возможность расширять внутренние компоненты
- Нужно экономить системные ресурсы, повторно используя уже созданные объекты

**Типичные примеры использования:**
- Создание объектов разных типов на основе входных данных
- Логирование в различные источники (файл, консоль, база данных)
- Создание подключений к разным СУБД
- Фабрики документов в текстовых редакторах

## Проблема, которую решает

### Проблема: Жесткая привязка к конкретным классам

```java
public class Application {
    public void createDocument(String type) {
        Document document;
        
        if (type.equals("word")) {
            document = new WordDocument();
        } else if (type.equals("pdf")) {
            document = new PdfDocument();
        } else if (type.equals("excel")) {
            document = new ExcelDocument();
        } else {
            throw new IllegalArgumentException("Unknown document type");
        }
        
        document.open();
        // Работа с документом
    }
}
```

**Проблемы:**
- Нарушение Open/Closed Principle
- Жесткая привязка к конкретным классам
- Сложность добавления новых типов
- Дублирование логики создания

### Решение: Factory Method

Делегировать создание объектов подклассам через фабричный метод.

## Структура паттерна

```java
// Продукт - общий интерфейс
interface Document {
    void open();
    void save();
    void close();
}

// Конкретные продукты
class WordDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening Word document");
    }
    
    @Override
    public void save() {
        System.out.println("Saving Word document");
    }
    
    @Override
    public void close() {
        System.out.println("Closing Word document");
    }
}

class PdfDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening PDF document");
    }
    
    @Override
    public void save() {
        System.out.println("Saving PDF document");
    }
    
    @Override
    public void close() {
        System.out.println("Closing PDF document");
    }
}

// Создатель (Creator)
abstract class Application {
    // Фабричный метод
    protected abstract Document createDocument();
    
    // Бизнес-логика, использующая продукт
    public void openDocument() {
        Document doc = createDocument();
        doc.open();
        // Работа с документом
        doc.save();
        doc.close();
    }
}

// Конкретные создатели
class WordApplication extends Application {
    @Override
    protected Document createDocument() {
        return new WordDocument();
    }
}

class PdfApplication extends Application {
    @Override
    protected Document createDocument() {
        return new PdfDocument();
    }
}

// Использование
Application app = new WordApplication();
app.openDocument(); // Создаст и откроет Word документ

app = new PdfApplication();
app.openDocument(); // Создаст и откроет PDF документ
```

## Реализация

### Пример 1: Система логирования

```java
// Продукт - логгер
interface Logger {
    void log(String message);
    void error(String message);
}

// Конкретные логгеры
class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("[CONSOLE LOG]: " + message);
    }
    
    @Override
    public void error(String message) {
        System.err.println("[CONSOLE ERROR]: " + message);
    }
}

class FileLogger implements Logger {
    private final String filename;
    
    public FileLogger(String filename) {
        this.filename = filename;
    }
    
    @Override
    public void log(String message) {
        writeToFile("[LOG]: " + message);
    }
    
    @Override
    public void error(String message) {
        writeToFile("[ERROR]: " + message);
    }
    
    private void writeToFile(String message) {
        System.out.println("Writing to " + filename + ": " + message);
        // Реальная запись в файл
    }
}

class DatabaseLogger implements Logger {
    private final String connectionString;
    
    public DatabaseLogger(String connectionString) {
        this.connectionString = connectionString;
    }
    
    @Override
    public void log(String message) {
        saveToDatabase("INFO", message);
    }
    
    @Override
    public void error(String message) {
        saveToDatabase("ERROR", message);
    }
    
    private void saveToDatabase(String level, String message) {
        System.out.println("Saving to DB [" + connectionString + "]: " + 
                          level + " - " + message);
        // Реальная запись в БД
    }
}

// Создатель
abstract class LoggerFactory {
    // Фабричный метод
    protected abstract Logger createLogger();
    
    // Шаблонный метод, использующий фабричный
    public void logMessage(String message) {
        Logger logger = createLogger();
        logger.log(message);
    }
    
    public void logError(String message) {
        Logger logger = createLogger();
        logger.error(message);
    }
}

// Конкретные фабрики
class ConsoleLoggerFactory extends LoggerFactory {
    @Override
    protected Logger createLogger() {
        return new ConsoleLogger();
    }
}

class FileLoggerFactory extends LoggerFactory {
    private final String filename;
    
    public FileLoggerFactory(String filename) {
        this.filename = filename;
    }
    
    @Override
    protected Logger createLogger() {
        return new FileLogger(filename);
    }
}

class DatabaseLoggerFactory extends LoggerFactory {
    private final String connectionString;
    
    public DatabaseLoggerFactory(String connectionString) {
        this.connectionString = connectionString;
    }
    
    @Override
    protected Logger createLogger() {
        return new DatabaseLogger(connectionString);
    }
}

// Использование
LoggerFactory factory = new ConsoleLoggerFactory();
factory.logMessage("Application started");
factory.logError("An error occurred");

// Переключение на файловое логирование
factory = new FileLoggerFactory("app.log");
factory.logMessage("Logging to file");

// Переключение на БД
factory = new DatabaseLoggerFactory("jdbc:postgresql://localhost/logs");
factory.logMessage("Logging to database");
```

### Пример 2: Фабрика подключений к БД

```java
// Продукт - соединение с БД
interface DatabaseConnection {
    void connect();
    void executeQuery(String query);
    void disconnect();
}

// Конкретные соединения
class PostgreSQLConnection implements DatabaseConnection {
    private final String url;
    
    public PostgreSQLConnection(String url) {
        this.url = url;
    }
    
    @Override
    public void connect() {
        System.out.println("Connecting to PostgreSQL: " + url);
    }
    
    @Override
    public void executeQuery(String query) {
        System.out.println("Executing on PostgreSQL: " + query);
    }
    
    @Override
    public void disconnect() {
        System.out.println("Disconnecting from PostgreSQL");
    }
}

class MySQLConnection implements DatabaseConnection {
    private final String url;
    
    public MySQLConnection(String url) {
        this.url = url;
    }
    
    @Override
    public void connect() {
        System.out.println("Connecting to MySQL: " + url);
    }
    
    @Override
    public void executeQuery(String query) {
        System.out.println("Executing on MySQL: " + query);
    }
    
    @Override
    public void disconnect() {
        System.out.println("Disconnecting from MySQL");
    }
}

class MongoDBConnection implements DatabaseConnection {
    private final String url;
    
    public MongoDBConnection(String url) {
        this.url = url;
    }
    
    @Override
    public void connect() {
        System.out.println("Connecting to MongoDB: " + url);
    }
    
    @Override
    public void executeQuery(String query) {
        System.out.println("Executing on MongoDB: " + query);
    }
    
    @Override
    public void disconnect() {
        System.out.println("Disconnecting from MongoDB");
    }
}

// Создатель
abstract class DatabaseService {
    // Фабричный метод
    protected abstract DatabaseConnection createConnection();
    
    // Бизнес-логика
    public void performDatabaseOperation(String query) {
        DatabaseConnection connection = createConnection();
        
        connection.connect();
        connection.executeQuery(query);
        connection.disconnect();
    }
}

// Конкретные сервисы
class PostgreSQLService extends DatabaseService {
    private final String url;
    
    public PostgreSQLService(String url) {
        this.url = url;
    }
    
    @Override
    protected DatabaseConnection createConnection() {
        return new PostgreSQLConnection(url);
    }
}

class MySQLService extends DatabaseService {
    private final String url;
    
    public MySQLService(String url) {
        this.url = url;
    }
    
    @Override
    protected DatabaseConnection createConnection() {
        return new MySQLConnection(url);
    }
}

class MongoDBService extends DatabaseService {
    private final String url;
    
    public MongoDBService(String url) {
        this.url = url;
    }
    
    @Override
    protected DatabaseConnection createConnection() {
        return new MongoDBConnection(url);
    }
}

// Использование
DatabaseService service = new PostgreSQLService("localhost:5432/mydb");
service.performDatabaseOperation("SELECT * FROM users");

service = new MySQLService("localhost:3306/mydb");
service.performDatabaseOperation("SELECT * FROM orders");
```

### Пример 3: Статический фабричный метод (Variation)

Упрощенный вариант без создания подклассов:

```java
// Продукт
interface Transport {
    void deliver();
}

class Truck implements Transport {
    @Override
    public void deliver() {
        System.out.println("Доставка по земле в контейнере");
    }
}

class Ship implements Transport {
    @Override
    public void deliver() {
        System.out.println("Доставка по морю в контейнере");
    }
}

class Plane implements Transport {
    @Override
    public void deliver() {
        System.out.println("Доставка по воздуху в контейнере");
    }
}

// Фабрика со статическими методами
class TransportFactory {
    public static Transport createTransport(String type) {
        switch (type.toLowerCase()) {
            case "land":
            case "truck":
                return new Truck();
            case "sea":
            case "ship":
                return new Ship();
            case "air":
            case "plane":
                return new Plane();
            default:
                throw new IllegalArgumentException("Unknown transport type: " + type);
        }
    }
    
    // Именованные фабричные методы (более выразительно)
    public static Transport createTruck() {
        return new Truck();
    }
    
    public static Transport createShip() {
        return new Ship();
    }
    
    public static Transport createPlane() {
        return new Plane();
    }
}

// Использование
Transport transport1 = TransportFactory.createTransport("truck");
transport1.deliver();

Transport transport2 = TransportFactory.createShip();
transport2.deliver();
```

## Примеры использования

### Из Java Collections Framework

```java
// Статические фабричные методы
List<String> list1 = List.of("a", "b", "c");
Set<Integer> set1 = Set.of(1, 2, 3);
Map<String, Integer> map1 = Map.of("one", 1, "two", 2);

// Более старые примеры
List<String> list2 = Collections.emptyList();
List<String> list3 = Collections.singletonList("item");
```

### Из java.util.Calendar

```java
// getInstance() - статический фабричный метод
Calendar calendar = Calendar.getInstance();
Calendar calendar2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
Calendar calendar3 = Calendar.getInstance(Locale.FRANCE);
```

### Из java.text.NumberFormat

```java
// Различные фабричные методы для разных форматов
NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
NumberFormat percentFormat = NumberFormat.getPercentInstance();
NumberFormat numberFormat = NumberFormat.getNumberInstance();
NumberFormat integerFormat = NumberFormat.getIntegerInstance();
```

### Из JDBC

```java
// DriverManager использует Factory Method для создания соединений
Connection connection = DriverManager.getConnection(
    "jdbc:postgresql://localhost:5432/mydb",
    "user",
    "password"
);
```

## Factory Method vs Constructor

### Преимущества фабричных методов над конструкторами

**1. Фабричные методы имеют имена**

```java
// Конструкторы - непонятно что создается
BigInteger value1 = new BigInteger("123");
BigInteger value2 = new BigInteger(100, new Random());

// Фабричные методы - понятно и выразительно
BigInteger value3 = BigInteger.valueOf(123);
BigInteger value4 = BigInteger.probablePrime(100, new Random());
```

**2. Могут возвращать объекты подтипов**

```java
class ConnectionFactory {
    // Возвращает интерфейс, скрывая конкретную реализацию
    public static Connection createConnection(String type) {
        if (type.equals("pooled")) {
            return new PooledConnection();
        } else {
            return new DirectConnection();
        }
    }
}
```

**3. Могут возвращать существующие экземпляры (кеширование)**

```java
class Boolean {
    public static final Boolean TRUE = new Boolean(true);
    public static final Boolean FALSE = new Boolean(false);
    
    // Фабричный метод возвращает закешированные экземпляры
    public static Boolean valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }
}

// Использование
Boolean b1 = Boolean.valueOf(true);
Boolean b2 = Boolean.valueOf(true);
System.out.println(b1 == b2); // true - один и тот же объект
```

**4. Могут возвращать объекты классов, которых не существует на момент написания**

```java
// JDBC - драйвер определяется в runtime
public interface Connection { }

public class DriverManager {
    public static Connection getConnection(String url) {
        // Находит подходящий драйвер динамически
        // Возвращает реализацию, которая может быть загружена во время выполнения
        return null; // упрощенно
    }
}
```

## Преимущества и недостатки

### Преимущества

✅ **Избавление от привязки к конкретным классам**
- Код работает с интерфейсами, а не с конкретными классами

✅ **Single Responsibility Principle**
- Код создания объектов находится в одном месте

✅ **Open/Closed Principle**
- Можно добавлять новые типы продуктов без изменения существующего кода

✅ **Гибкость и расширяемость**
- Легко добавлять новые варианты создания объектов

✅ **Повторное использование кода**
- Общая логика в базовом классе

### Недостатки

❌ **Усложнение кода**
- Требуется создание множества подклассов

❌ **Может быть избыточным**
- Для простых случаев достаточно конструктора или простой фабрики

❌ **Параллельная иерархия классов**
- Для каждого типа продукта нужен свой создатель

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Factory Method?**

*Ответ:* Factory Method — это порождающий паттерн, который определяет интерфейс для создания объекта, но позволяет подклассам решать, какой класс инстанцировать. Паттерн делегирует создание объектов наследникам.

**2. В чем отличие Factory Method от Simple Factory?**

*Ответ:*
- **Simple Factory** — это просто класс с методом, который создает объекты на основе параметра. Не является паттерном GoF.
- **Factory Method** — использует наследование и полиморфизм. Определяет интерфейс для создания, подклассы выбирают конкретный класс.

**3. Приведите примеры Factory Method из JDK**

*Ответ:*
- `Calendar.getInstance()`
- `NumberFormat.getInstance()`
- `Collections.emptyList()`, `Collections.singletonList()`
- `List.of()`, `Set.of()`, `Map.of()` (Java 9+)
- `DriverManager.getConnection()` в JDBC

**4. Когда следует использовать Factory Method?**

*Ответ:*
- Когда заранее неизвестны типы создаваемых объектов
- Когда нужна возможность расширения внутренних компонентов библиотеки/фреймворка
- Когда нужно делегировать логику создания объектов подклассам
- Когда нужна централизация логики создания объектов

### Продвинутые вопросы

**5. В чем преимущества статических фабричных методов над конструкторами?**

*Ответ:*
1. Имеют понятные имена (не ограничены именем класса)
2. Не обязаны создавать новый объект при каждом вызове (могут кешировать)
3. Могут возвращать объект любого подтипа
4. Класс возвращаемого объекта может меняться от вызова к вызову
5. Класс возвращаемого объекта может не существовать на момент написания метода

**6. Как Factory Method связан с другими паттернами?**

*Ответ:*
- **Template Method** часто использует Factory Method для создания объектов
- **Abstract Factory** часто реализуется через Factory Method
- **Prototype** может быть альтернативой Factory Method
- **Iterator** часто создается через Factory Method

**7. Какие недостатки у Factory Method?**

*Ответ:*
- Усложнение кода из-за создания дополнительных подклассов
- Для каждого нового продукта нужен новый создатель
- Может быть избыточным для простых случаев
- Создает параллельную иерархию классов (продукты и создатели)

**8. В чем разница между Factory Method и Abstract Factory?**

*Ответ:*
- **Factory Method**: Один метод создает один тип продукта. Фокус на наследовании.
- **Abstract Factory**: Фабрика создает семейство связанных продуктов. Фокус на композиции.
- Factory Method обычно является частью Abstract Factory.

**9. Как реализовать Factory Method с параметризацией?**

*Ответ:*
```java
abstract class Creator {
    // Параметризованный фабричный метод
    protected abstract Product createProduct(String type);
    
    public void doSomething(String type) {
        Product product = createProduct(type);
        product.use();
    }
}

class ConcreteCreator extends Creator {
    @Override
    protected Product createProduct(String type) {
        switch(type) {
            case "A": return new ProductA();
            case "B": return new ProductB();
            default: throw new IllegalArgumentException();
        }
    }
}
```

**10. Можно ли использовать Factory Method для Singleton?**

*Ответ:* Да, фабричный метод может гарантировать создание только одного экземпляра:

```java
class SingletonFactory {
    private static Product instance;
    
    public static synchronized Product getInstance() {
        if (instance == null) {
            instance = new ConcreteProduct();
        }
        return instance;
    }
}
```

---

[← Назад к разделу Порождающие паттерны](README.md)
