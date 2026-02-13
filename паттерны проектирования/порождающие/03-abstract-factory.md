# Abstract Factory (Абстрактная фабрика)

Abstract Factory — порождающий паттерн проектирования, который позволяет создавать семейства связанных объектов, не привязываясь к конкретным классам создаваемых объектов.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Примеры использования](#примеры-использования)
6. [Abstract Factory vs Factory Method](#abstract-factory-vs-factory-method)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Abstract Factory используется когда:
- Система не должна зависеть от способа создания и компоновки объектов
- Нужно создавать семейства взаимосвязанных объектов
- Необходимо гарантировать совместимость создаваемых объектов
- Нужно предоставить библиотеку объектов, раскрывая только их интерфейсы

**Типичные примеры использования:**
- UI-фреймворки с разными темами оформления
- Кросс-платформенные приложения
- Фабрики для разных СУБД
- Создание семейств виджетов

## Проблема, которую решает

### Проблема: Создание несовместимых объектов

```java
// Хотим создать UI для разных платформ
// Плохой подход - клиент знает о конкретных классах

public class Application {
    public void createUI(String platform) {
        Button button;
        Checkbox checkbox;
        
        if (platform.equals("Windows")) {
            button = new WindowsButton();
            checkbox = new WindowsCheckbox();
        } else if (platform.equals("Mac")) {
            button = new MacButton();
            checkbox = new MacCheckbox();
        } else {
            button = new LinuxButton();
            checkbox = new LinuxCheckbox();
        }
        
        // Проблема: можно случайно смешать компоненты разных платформ!
        // button = new WindowsButton();
        // checkbox = new MacCheckbox();  // Несовместимые компоненты!
    }
}
```

**Проблемы:**
- Можно случайно создать несовместимые объекты
- Клиент привязан к конкретным классам
- Сложно добавить новую платформу
- Дублирование кода создания

### Решение: Abstract Factory

Инкапсулировать создание семейств объектов в отдельную фабрику.

## Структура паттерна

```java
// Абстрактные продукты
interface Button {
    void paint();
}

interface Checkbox {
    void paint();
}

// Конкретные продукты для Windows
class WindowsButton implements Button {
    @Override
    public void paint() {
        System.out.println("Rendering Windows button");
    }
}

class WindowsCheckbox implements Checkbox {
    @Override
    public void paint() {
        System.out.println("Rendering Windows checkbox");
    }
}

// Конкретные продукты для Mac
class MacButton implements Button {
    @Override
    public void paint() {
        System.out.println("Rendering Mac button");
    }
}

class MacCheckbox implements Checkbox {
    @Override
    public void paint() {
        System.out.println("Rendering Mac checkbox");
    }
}

// Абстрактная фабрика
interface GUIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// Конкретные фабрики
class WindowsFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new WindowsButton();
    }
    
    @Override
    public Checkbox createCheckbox() {
        return new WindowsCheckbox();
    }
}

class MacFactory implements GUIFactory {
    @Override
    public Button createButton() {
        return new MacButton();
    }
    
    @Override
    public Checkbox createCheckbox() {
        return new MacCheckbox();
    }
}

// Клиентский код не зависит от конкретных классов
class Application {
    private Button button;
    private Checkbox checkbox;
    
    public Application(GUIFactory factory) {
        button = factory.createButton();
        checkbox = factory.createCheckbox();
    }
    
    public void paint() {
        button.paint();
        checkbox.paint();
    }
}

// Использование
GUIFactory factory;

String osName = System.getProperty("os.name").toLowerCase();
if (osName.contains("win")) {
    factory = new WindowsFactory();
} else if (osName.contains("mac")) {
    factory = new MacFactory();
} else {
    factory = new LinuxFactory();
}

Application app = new Application(factory);
app.paint();  // Все компоненты гарантированно совместимы
```

## Реализация

### Пример 1: Фабрика подключений к разным БД

```java
// Абстрактные продукты
interface Connection {
    void connect();
    void disconnect();
}

interface Command {
    void execute(String sql);
}

interface Transaction {
    void begin();
    void commit();
    void rollback();
}

// Продукты для PostgreSQL
class PostgreSQLConnection implements Connection {
    @Override
    public void connect() {
        System.out.println("Connecting to PostgreSQL");
    }
    
    @Override
    public void disconnect() {
        System.out.println("Disconnecting from PostgreSQL");
    }
}

class PostgreSQLCommand implements Command {
    @Override
    public void execute(String sql) {
        System.out.println("Executing PostgreSQL command: " + sql);
    }
}

class PostgreSQLTransaction implements Transaction {
    @Override
    public void begin() {
        System.out.println("BEGIN TRANSACTION (PostgreSQL)");
    }
    
    @Override
    public void commit() {
        System.out.println("COMMIT (PostgreSQL)");
    }
    
    @Override
    public void rollback() {
        System.out.println("ROLLBACK (PostgreSQL)");
    }
}

// Продукты для MySQL
class MySQLConnection implements Connection {
    @Override
    public void connect() {
        System.out.println("Connecting to MySQL");
    }
    
    @Override
    public void disconnect() {
        System.out.println("Disconnecting from MySQL");
    }
}

class MySQLCommand implements Command {
    @Override
    public void execute(String sql) {
        System.out.println("Executing MySQL command: " + sql);
    }
}

class MySQLTransaction implements Transaction {
    @Override
    public void begin() {
        System.out.println("START TRANSACTION (MySQL)");
    }
    
    @Override
    public void commit() {
        System.out.println("COMMIT (MySQL)");
    }
    
    @Override
    public void rollback() {
        System.out.println("ROLLBACK (MySQL)");
    }
}

// Абстрактная фабрика
interface DatabaseFactory {
    Connection createConnection();
    Command createCommand();
    Transaction createTransaction();
}

// Конкретные фабрики
class PostgreSQLFactory implements DatabaseFactory {
    @Override
    public Connection createConnection() {
        return new PostgreSQLConnection();
    }
    
    @Override
    public Command createCommand() {
        return new PostgreSQLCommand();
    }
    
    @Override
    public Transaction createTransaction() {
        return new PostgreSQLTransaction();
    }
}

class MySQLFactory implements DatabaseFactory {
    @Override
    public Connection createConnection() {
        return new MySQLConnection();
    }
    
    @Override
    public Command createCommand() {
        return new MySQLCommand();
    }
    
    @Override
    public Transaction createTransaction() {
        return new MySQLTransaction();
    }
}

// Клиентский код
class DatabaseService {
    private final Connection connection;
    private final Command command;
    private final Transaction transaction;
    
    public DatabaseService(DatabaseFactory factory) {
        this.connection = factory.createConnection();
        this.command = factory.createCommand();
        this.transaction = factory.createTransaction();
    }
    
    public void performOperation() {
        connection.connect();
        transaction.begin();
        try {
            command.execute("INSERT INTO users VALUES (...)");
            command.execute("UPDATE accounts SET ...");
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
        } finally {
            connection.disconnect();
        }
    }
}

// Использование
DatabaseFactory factory = new PostgreSQLFactory();
DatabaseService service = new DatabaseService(factory);
service.performOperation();

// Легко переключиться на другую БД
factory = new MySQLFactory();
service = new DatabaseService(factory);
service.performOperation();
```

### Пример 2: Фабрика документов

```java
// Абстрактные продукты
interface Document {
    void open();
    void save();
}

interface Image {
    void draw();
}

interface Chart {
    void render();
}

// Продукты для PDF
class PdfDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening PDF document");
    }
    
    @Override
    public void save() {
        System.out.println("Saving PDF document");
    }
}

class PdfImage implements Image {
    @Override
    public void draw() {
        System.out.println("Drawing PDF image");
    }
}

class PdfChart implements Chart {
    @Override
    public void render() {
        System.out.println("Rendering PDF chart");
    }
}

// Продукты для Word
class WordDocument implements Document {
    @Override
    public void open() {
        System.out.println("Opening Word document");
    }
    
    @Override
    public void save() {
        System.out.println("Saving Word document");
    }
}

class WordImage implements Image {
    @Override
    public void draw() {
        System.out.println("Drawing Word image");
    }
}

class WordChart implements Chart {
    @Override
    public void render() {
        System.out.println("Rendering Word chart");
    }
}

// Абстрактная фабрика
interface DocumentFactory {
    Document createDocument();
    Image createImage();
    Chart createChart();
}

// Конкретные фабрики
class PdfFactory implements DocumentFactory {
    @Override
    public Document createDocument() {
        return new PdfDocument();
    }
    
    @Override
    public Image createImage() {
        return new PdfImage();
    }
    
    @Override
    public Chart createChart() {
        return new PdfChart();
    }
}

class WordFactory implements DocumentFactory {
    @Override
    public Document createDocument() {
        return new WordDocument();
    }
    
    @Override
    public Image createImage() {
        return new WordImage();
    }
    
    @Override
    public Chart createChart() {
        return new WordChart();
    }
}

// Клиентский код
class ReportGenerator {
    private final DocumentFactory factory;
    
    public ReportGenerator(DocumentFactory factory) {
        this.factory = factory;
    }
    
    public void generateReport() {
        Document doc = factory.createDocument();
        Image image = factory.createImage();
        Chart chart = factory.createChart();
        
        doc.open();
        image.draw();
        chart.render();
        doc.save();
    }
}

// Использование
DocumentFactory pdfFactory = new PdfFactory();
ReportGenerator pdfReport = new ReportGenerator(pdfFactory);
pdfReport.generateReport();

DocumentFactory wordFactory = new WordFactory();
ReportGenerator wordReport = new ReportGenerator(wordFactory);
wordReport.generateReport();
```

### Пример 3: Фабрика тем UI

```java
// Абстрактные продукты
interface Color {
    String getHexCode();
}

interface Font {
    String getName();
    int getSize();
}

interface IconSet {
    String getIconPath(String iconName);
}

// Продукты для светлой темы
class LightColor implements Color {
    private final String name;
    
    public LightColor(String name) {
        this.name = name;
    }
    
    @Override
    public String getHexCode() {
        return switch (name) {
            case "background" -> "#FFFFFF";
            case "text" -> "#000000";
            case "primary" -> "#007BFF";
            default -> "#CCCCCC";
        };
    }
}

class LightFont implements Font {
    @Override
    public String getName() {
        return "Roboto";
    }
    
    @Override
    public int getSize() {
        return 14;
    }
}

class LightIconSet implements IconSet {
    @Override
    public String getIconPath(String iconName) {
        return "/icons/light/" + iconName + ".svg";
    }
}

// Продукты для темной темы
class DarkColor implements Color {
    private final String name;
    
    public DarkColor(String name) {
        this.name = name;
    }
    
    @Override
    public String getHexCode() {
        return switch (name) {
            case "background" -> "#1E1E1E";
            case "text" -> "#FFFFFF";
            case "primary" -> "#0D6EFD";
            default -> "#333333";
        };
    }
}

class DarkFont implements Font {
    @Override
    public String getName() {
        return "Roboto";
    }
    
    @Override
    public int getSize() {
        return 14;
    }
}

class DarkIconSet implements IconSet {
    @Override
    public String getIconPath(String iconName) {
        return "/icons/dark/" + iconName + ".svg";
    }
}

// Абстрактная фабрика
interface ThemeFactory {
    Color createColor(String name);
    Font createFont();
    IconSet createIconSet();
}

// Конкретные фабрики
class LightThemeFactory implements ThemeFactory {
    @Override
    public Color createColor(String name) {
        return new LightColor(name);
    }
    
    @Override
    public Font createFont() {
        return new LightFont();
    }
    
    @Override
    public IconSet createIconSet() {
        return new LightIconSet();
    }
}

class DarkThemeFactory implements ThemeFactory {
    @Override
    public Color createColor(String name) {
        return new DarkColor(name);
    }
    
    @Override
    public Font createFont() {
        return new DarkFont();
    }
    
    @Override
    public IconSet createIconSet() {
        return new DarkIconSet();
    }
}

// Клиентский код
class UIRenderer {
    private final ThemeFactory themeFactory;
    
    public UIRenderer(ThemeFactory themeFactory) {
        this.themeFactory = themeFactory;
    }
    
    public void renderPage() {
        Color bgColor = themeFactory.createColor("background");
        Color textColor = themeFactory.createColor("text");
        Font font = themeFactory.createFont();
        IconSet icons = themeFactory.createIconSet();
        
        System.out.println("Background: " + bgColor.getHexCode());
        System.out.println("Text: " + textColor.getHexCode());
        System.out.println("Font: " + font.getName() + " " + font.getSize() + "px");
        System.out.println("Home Icon: " + icons.getIconPath("home"));
    }
}

// Использование
ThemeFactory theme = new LightThemeFactory();
UIRenderer renderer = new UIRenderer(theme);
renderer.renderPage();

// Переключение темы
theme = new DarkThemeFactory();
renderer = new UIRenderer(theme);
renderer.renderPage();
```

## Примеры использования

### Java AWT/Swing

```java
// Java использует Abstract Factory для создания компонентов UI
// в зависимости от операционной системы

// Toolkit - абстрактная фабрика
Toolkit toolkit = Toolkit.getDefaultToolkit();

// Создает компоненты, специфичные для платформы
Button button = new Button("Click me");
Frame frame = new Frame("My App");
MenuBar menuBar = new MenuBar();
```

### JDBC (неявно)

```java
// DriverManager использует паттерн для создания Connection
// в зависимости от СУБД

Connection connection = DriverManager.getConnection(
    "jdbc:postgresql://localhost:5432/mydb",  // PostgreSQL фабрика
    "user", "password"
);

// Или MySQL фабрика
Connection connection2 = DriverManager.getConnection(
    "jdbc:mysql://localhost:3306/mydb",  // MySQL фабрика
    "user", "password"
);
```

### Spring Framework

```java
// BeanFactory - абстрактная фабрика для создания бинов
@Configuration
public class AppConfig {
    @Bean
    @Profile("dev")
    public DataSource devDataSource() {
        return new H2DataSource();  // Для разработки
    }
    
    @Bean
    @Profile("prod")
    public DataSource prodDataSource() {
        return new PostgreSQLDataSource();  // Для продакшена
    }
}
```

## Abstract Factory vs Factory Method

| Аспект | Abstract Factory | Factory Method |
|--------|------------------|----------------|
| **Цель** | Создание семейств объектов | Создание одного объекта |
| **Фокус** | Композиция (has-a) | Наследование (is-a) |
| **Методы** | Несколько фабричных методов | Один фабричный метод |
| **Структура** | Интерфейс фабрики с методами | Абстрактный метод в базовом классе |
| **Использование** | Когда нужны связанные объекты | Когда тип объекта определяется подклассом |

**Пример комбинирования:**

```java
// Abstract Factory часто реализуется через Factory Method

abstract class GUIFactory {  // Abstract Factory
    // Factory Methods
    public abstract Button createButton();
    public abstract Checkbox createCheckbox();
}

class WindowsFactory extends GUIFactory {
    @Override
    public Button createButton() {  // Factory Method
        return new WindowsButton();
    }
    
    @Override
    public Checkbox createCheckbox() {  // Factory Method
        return new WindowsCheckbox();
    }
}
```

## Преимущества и недостатки

### Преимущества

✅ **Гарантия совместимости продуктов**
- Все объекты из одной фабрики совместимы между собой

✅ **Избавление от привязки к конкретным классам**
- Код работает с абстракциями, а не с конкретными классами

✅ **Single Responsibility Principle**
- Код создания продуктов в одном месте

✅ **Open/Closed Principle**
- Можно добавлять новые семейства продуктов без изменения существующего кода

✅ **Централизация создания объектов**
- Легко управлять созданием связанных объектов

### Недостатки

❌ **Усложнение кода**
- Много новых интерфейсов и классов
- Может быть избыточным для простых случаев

❌ **Сложность добавления новых типов продуктов**
- Требуется изменение интерфейса фабрики и всех конкретных фабрик
- Нарушает Open/Closed Principle при добавлении нового типа продукта

❌ **Параллельные иерархии**
- Для каждого семейства нужна своя фабрика

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Abstract Factory?**

*Ответ:* Abstract Factory — это порождающий паттерн, который предоставляет интерфейс для создания семейств взаимосвязанных объектов без указания их конкретных классов. Гарантирует совместимость создаваемых объектов.

**2. В чем отличие Abstract Factory от Factory Method?**

*Ответ:*
- **Abstract Factory**: создает семейства связанных объектов, использует композицию
- **Factory Method**: создает один объект, использует наследование
- Abstract Factory имеет несколько фабричных методов
- Factory Method — один абстрактный метод

**3. Приведите примеры Abstract Factory из JDK**

*Ответ:*
- `javax.xml.parsers.DocumentBuilderFactory`
- `javax.xml.transform.TransformerFactory`
- `java.awt.Toolkit` (создает компоненты GUI для разных платформ)

**4. Когда следует использовать Abstract Factory?**

*Ответ:*
- Нужно создавать семейства взаимосвязанных объектов
- Необходима гарантия совместимости создаваемых объектов
- Система не должна зависеть от деталей создания объектов
- Нужно предоставить набор продуктов, скрывая их реализацию

### Продвинутые вопросы

**5. Как Abstract Factory соблюдает принцип Open/Closed?**

*Ответ:* 
- **Открыт для расширения**: можно добавлять новые семейства продуктов (новые фабрики)
- **Закрыт для модификации**: клиентский код не меняется при добавлении новой фабрики

Но нарушается при добавлении нового типа продукта (нужно менять интерфейс фабрики).

**6. Как решить проблему добавления новых типов продуктов?**

*Ответ:* Несколько подходов:
1. **Прототип**: вместо фабричных методов хранить прототипы
2. **Рефлексия**: динамическое создание объектов по именам классов
3. **Параметризованный Factory Method**: один метод создает разные типы
4. **Dependency Injection**: DI-контейнер управляет зависимостями

**7. Можно ли реализовать Abstract Factory как Singleton?**

*Ответ:* Да, часто фабрики делают Singleton'ами:
```java
class WindowsFactory implements GUIFactory {
    private static WindowsFactory instance;
    
    private WindowsFactory() {}
    
    public static synchronized WindowsFactory getInstance() {
        if (instance == null) {
            instance = new WindowsFactory();
        }
        return instance;
    }
    
    // Фабричные методы...
}
```

**8. Как Abstract Factory связан с другими паттернами?**

*Ответ:*
- Часто реализуется через **Factory Method**
- Фабрики часто являются **Singleton**
- Может использовать **Prototype** для клонирования продуктов
- Может работать с **Builder** для создания сложных продуктов

**9. Какие проблемы может создать Abstract Factory?**

*Ответ:*
- Усложнение кода (много классов и интерфейсов)
- Сложность добавления новых типов продуктов
- Может быть избыточным для простых случаев
- Требует предварительного планирования семейств продуктов

**10. Как выбрать между Abstract Factory и Builder?**

*Ответ:*
- **Abstract Factory**: когда нужно создать семейство связанных объектов
- **Builder**: когда нужно пошагово создать один сложный объект

Можно комбинировать:
```java
// Фабрика создает Builder'ы
interface UIFactory {
    ButtonBuilder createButtonBuilder();
    FormBuilder createFormBuilder();
}
```

---

[← Назад к разделу Порождающие паттерны](README.md)
