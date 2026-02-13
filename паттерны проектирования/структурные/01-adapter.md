# Adapter (Адаптер)

Adapter — структурный паттерн проектирования, который позволяет объектам с несовместимыми интерфейсами работать вместе.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Типы адаптеров](#типы-адаптеров)
5. [Реализация](#реализация)
6. [Примеры использования](#примеры-использования)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Adapter используется когда:
- Нужно использовать существующий класс, но его интерфейс не соответствует требуемому
- Необходима интеграция с legacy кодом
- Требуется создать повторно используемый класс, взаимодействующий с классами с несовместимыми интерфейсами
- Нужно адаптировать сторонние библиотеки под свой интерфейс

**Типичные примеры использования:**
- Интеграция старого и нового кода
- Адаптация сторонних библиотек
- Работа с различными форматами данных
- Унификация API разных систем

## Проблема, которую решает

### Проблема: Несовместимые интерфейсы

```java
// У нас есть интерфейс для работы с данными
interface DataProcessor {
    void process(String data);
}

// Наш код работает с этим интерфейсом
class Application {
    private DataProcessor processor;
    
    public Application(DataProcessor processor) {
        this.processor = processor;
    }
    
    public void run() {
        processor.process("Some data");
    }
}

// Но есть сторонняя библиотека с другим интерфейсом
class LegacyDataHandler {
    public void handleData(byte[] rawData) {
        System.out.println("Processing raw data: " + rawData.length + " bytes");
    }
}

// Проблема: не можем использовать LegacyDataHandler 
// напрямую, т.к. интерфейсы не совпадают
// Application app = new Application(new LegacyDataHandler()); // Ошибка!
```

**Проблемы:**
- Несовместимые интерфейсы
- Невозможно изменить существующий код (legacy или сторонняя библиотека)
- Дублирование функциональности
- Нарушение принципа DRY

### Решение: Adapter

Создать адаптер, который преобразует один интерфейс в другой.

## Структура паттерна

```java
// Целевой интерфейс (Target)
interface DataProcessor {
    void process(String data);
}

// Адаптируемый класс (Adaptee)
class LegacyDataHandler {
    public void handleData(byte[] rawData) {
        System.out.println("Processing raw data: " + rawData.length + " bytes");
    }
}

// Адаптер (Adapter)
class DataHandlerAdapter implements DataProcessor {
    private LegacyDataHandler legacyHandler;
    
    public DataHandlerAdapter(LegacyDataHandler legacyHandler) {
        this.legacyHandler = legacyHandler;
    }
    
    @Override
    public void process(String data) {
        // Преобразуем интерфейс
        byte[] bytes = data.getBytes();
        legacyHandler.handleData(bytes);
    }
}

// Теперь можем использовать
LegacyDataHandler legacy = new LegacyDataHandler();
DataProcessor processor = new DataHandlerAdapter(legacy);

Application app = new Application(processor);
app.run();  // Работает!
```

## Типы адаптеров

### 1. Адаптер объектов (Object Adapter) - использует композицию

```java
// Рекомендуемый подход - использует композицию

interface MediaPlayer {
    void play(String filename);
}

class MP3Player {
    public void playMP3(String filename) {
        System.out.println("Playing MP3: " + filename);
    }
}

// Адаптер через композицию
class MP3Adapter implements MediaPlayer {
    private MP3Player mp3Player;
    
    public MP3Adapter(MP3Player mp3Player) {
        this.mp3Player = mp3Player;
    }
    
    @Override
    public void play(String filename) {
        mp3Player.playMP3(filename);
    }
}
```

**Преимущества:**
- Более гибкий (можно адаптировать целую иерархию классов)
- Следует принципу "композиция важнее наследования"
- Можно адаптировать интерфейс, который изменяется

### 2. Адаптер классов (Class Adapter) - использует множественное наследование

```java
// В Java множественное наследование классов невозможно,
// но можно наследовать класс и реализовывать интерфейс

interface MediaPlayer {
    void play(String filename);
}

class MP3Player {
    public void playMP3(String filename) {
        System.out.println("Playing MP3: " + filename);
    }
}

// Адаптер через наследование
class MP3ClassAdapter extends MP3Player implements MediaPlayer {
    @Override
    public void play(String filename) {
        playMP3(filename);  // Вызываем метод родителя
    }
}

// Использование
MediaPlayer player = new MP3ClassAdapter();
player.play("song.mp3");
```

**Недостатки:**
- Менее гибкий
- Адаптирует только один конкретный класс
- В Java нельзя наследовать несколько классов

## Реализация

### Пример 1: Адаптер для логирования

```java
// Целевой интерфейс нашего приложения
interface Logger {
    void log(String message);
    void error(String message);
}

// Сторонняя библиотека логирования с другим API
class ThirdPartyLogger {
    public void writeLog(String level, String msg) {
        System.out.println("[" + level + "] " + msg);
    }
    
    public void writeError(String msg, int errorCode) {
        System.out.println("[ERROR-" + errorCode + "] " + msg);
    }
}

// Адаптер
class ThirdPartyLoggerAdapter implements Logger {
    private ThirdPartyLogger thirdPartyLogger;
    
    public ThirdPartyLoggerAdapter(ThirdPartyLogger thirdPartyLogger) {
        this.thirdPartyLogger = thirdPartyLogger;
    }
    
    @Override
    public void log(String message) {
        thirdPartyLogger.writeLog("INFO", message);
    }
    
    @Override
    public void error(String message) {
        thirdPartyLogger.writeError(message, 500);
    }
}

// Наш код работает с единым интерфейсом
class Application {
    private Logger logger;
    
    public Application(Logger logger) {
        this.logger = logger;
    }
    
    public void doWork() {
        logger.log("Starting work");
        try {
            // Работа...
        } catch (Exception e) {
            logger.error("Error occurred: " + e.getMessage());
        }
    }
}

// Использование
ThirdPartyLogger thirdParty = new ThirdPartyLogger();
Logger logger = new ThirdPartyLoggerAdapter(thirdParty);

Application app = new Application(logger);
app.doWork();
```

### Пример 2: Адаптер для различных форматов данных

```java
// Целевой интерфейс - работа с JSON
interface JsonDataSource {
    String readJson();
    void writeJson(String data);
}

// Адаптируемый класс - работа с XML
class XmlDataSource {
    private String xmlData;
    
    public String getXml() {
        return xmlData;
    }
    
    public void setXml(String xml) {
        this.xmlData = xml;
    }
}

// Адаптер XML -> JSON
class XmlToJsonAdapter implements JsonDataSource {
    private XmlDataSource xmlSource;
    
    public XmlToJsonAdapter(XmlDataSource xmlSource) {
        this.xmlSource = xmlSource;
    }
    
    @Override
    public String readJson() {
        String xml = xmlSource.getXml();
        // Конвертируем XML в JSON
        return convertXmlToJson(xml);
    }
    
    @Override
    public void writeJson(String jsonData) {
        // Конвертируем JSON в XML
        String xml = convertJsonToXml(jsonData);
        xmlSource.setXml(xml);
    }
    
    private String convertXmlToJson(String xml) {
        // Упрощенная конвертация
        return "{\"data\": \"" + xml.replaceAll("<[^>]*>", "") + "\"}";
    }
    
    private String convertJsonToXml(String json) {
        // Упрощенная конвертация
        return "<root>" + json.replaceAll("[{}\"]", "") + "</root>";
    }
}

// Использование
XmlDataSource xmlSource = new XmlDataSource();
xmlSource.setXml("<user><name>John</name></user>");

JsonDataSource jsonSource = new XmlToJsonAdapter(xmlSource);
String json = jsonSource.readJson();
System.out.println(json);  // {"data": "John"}
```

### Пример 3: Адаптер для различных платежных систем

```java
// Единый интерфейс для платежей
interface PaymentProcessor {
    boolean processPayment(double amount);
    String getTransactionId();
}

// PayPal API
class PayPalService {
    public void makePayment(String email, double amount) {
        System.out.println("PayPal payment: $" + amount + " to " + email);
    }
    
    public String getLastTransactionReference() {
        return "PP-" + System.currentTimeMillis();
    }
}

// Stripe API
class StripeService {
    public boolean charge(int amountInCents, String currency) {
        System.out.println("Stripe charge: " + amountInCents + " " + currency);
        return true;
    }
    
    public String retrieveChargeId() {
        return "ch_" + System.currentTimeMillis();
    }
}

// Адаптер для PayPal
class PayPalAdapter implements PaymentProcessor {
    private PayPalService payPalService;
    private String userEmail;
    
    public PayPalAdapter(PayPalService payPalService, String userEmail) {
        this.payPalService = payPalService;
        this.userEmail = userEmail;
    }
    
    @Override
    public boolean processPayment(double amount) {
        payPalService.makePayment(userEmail, amount);
        return true;
    }
    
    @Override
    public String getTransactionId() {
        return payPalService.getLastTransactionReference();
    }
}

// Адаптер для Stripe
class StripeAdapter implements PaymentProcessor {
    private StripeService stripeService;
    
    public StripeAdapter(StripeService stripeService) {
        this.stripeService = stripeService;
    }
    
    @Override
    public boolean processPayment(double amount) {
        int amountInCents = (int) (amount * 100);
        return stripeService.charge(amountInCents, "USD");
    }
    
    @Override
    public String getTransactionId() {
        return stripeService.retrieveChargeId();
    }
}

// Клиентский код работает с единым интерфейсом
class ShoppingCart {
    private PaymentProcessor paymentProcessor;
    
    public void setPaymentProcessor(PaymentProcessor processor) {
        this.paymentProcessor = processor;
    }
    
    public void checkout(double totalAmount) {
        if (paymentProcessor.processPayment(totalAmount)) {
            String transactionId = paymentProcessor.getTransactionId();
            System.out.println("Payment successful! Transaction ID: " + transactionId);
        }
    }
}

// Использование
ShoppingCart cart = new ShoppingCart();

// Оплата через PayPal
PayPalService payPal = new PayPalService();
cart.setPaymentProcessor(new PayPalAdapter(payPal, "user@example.com"));
cart.checkout(99.99);

// Оплата через Stripe
StripeService stripe = new StripeService();
cart.setPaymentProcessor(new StripeAdapter(stripe));
cart.checkout(149.99);
```

### Пример 4: Двунаправленный адаптер

```java
// Старый интерфейс
interface OldInterface {
    void oldMethod();
}

// Новый интерфейс
interface NewInterface {
    void newMethod();
}

// Двунаправленный адаптер
class TwoWayAdapter implements OldInterface, NewInterface {
    private OldInterface oldObject;
    private NewInterface newObject;
    
    public TwoWayAdapter(OldInterface oldObject) {
        this.oldObject = oldObject;
    }
    
    public TwoWayAdapter(NewInterface newObject) {
        this.newObject = newObject;
    }
    
    @Override
    public void oldMethod() {
        if (newObject != null) {
            newObject.newMethod();  // Адаптируем новый к старому
        } else {
            oldObject.oldMethod();
        }
    }
    
    @Override
    public void newMethod() {
        if (oldObject != null) {
            oldObject.oldMethod();  // Адаптируем старый к новому
        } else {
            newObject.newMethod();
        }
    }
}
```

## Примеры использования

### Java Collections

```java
// Arrays.asList() - адаптирует массив к List
String[] array = {"A", "B", "C"};
List<String> list = Arrays.asList(array);

// Collections.list() - адаптирует Enumeration к List
Enumeration<String> enumeration = new Vector<>(Arrays.asList("X", "Y", "Z")).elements();
ArrayList<String> arrayList = Collections.list(enumeration);
```

### Java I/O

```java
// InputStreamReader - адаптер InputStream к Reader
FileInputStream fileInput = new FileInputStream("file.txt");
Reader reader = new InputStreamReader(fileInput);  // Адаптер

// OutputStreamWriter - адаптер OutputStream к Writer
FileOutputStream fileOutput = new FileOutputStream("file.txt");
Writer writer = new OutputStreamWriter(fileOutput);  // Адаптер
```

### Spring Framework

```java
// HandlerAdapter в Spring MVC
// Адаптирует различные типы контроллеров к единому интерфейсу

public interface HandlerAdapter {
    boolean supports(Object handler);
    ModelAndView handle(HttpServletRequest request, 
                       HttpServletResponse response, 
                       Object handler);
}

// SimpleControllerHandlerAdapter - адаптирует Controller
// HttpRequestHandlerAdapter - адаптирует HttpRequestHandler
// AnnotationMethodHandlerAdapter - адаптирует @RequestMapping методы
```

## Преимущества и недостатки

### Преимущества

✅ **Single Responsibility Principle**
- Отделяет логику преобразования интерфейса от бизнес-логики

✅ **Open/Closed Principle**
- Можно добавлять новые адаптеры без изменения существующего кода

✅ **Интеграция несовместимого кода**
- Позволяет использовать legacy код или сторонние библиотеки

✅ **Переиспользование существующего кода**
- Не нужно переписывать работающий код

✅ **Гибкость через композицию**
- Адаптер объектов позволяет адаптировать целую иерархию

### Недостатки

❌ **Усложнение кода**
- Дополнительный уровень абстракции
- Больше классов

❌ **Производительность**
- Дополнительные вызовы методов
- Преобразование данных

❌ **Может быть проще изменить источник**
- Иногда проще изменить адаптируемый класс напрямую

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Adapter и когда его использовать?**

*Ответ:* Adapter — это структурный паттерн, который позволяет объектам с несовместимыми интерфейсами работать вместе. Используется когда:
- Нужно использовать существующий класс с несовместимым интерфейсом
- Требуется интеграция с legacy кодом
- Необходимо адаптировать сторонние библиотеки

**2. В чем разница между адаптером объектов и адаптером классов?**

*Ответ:*
- **Адаптер объектов**: использует композицию, более гибкий, рекомендуется
- **Адаптер классов**: использует наследование, менее гибкий, в Java ограничен
- Адаптер объектов может адаптировать целую иерархию, адаптер классов — только один класс

**3. Приведите примеры Adapter из JDK**

*Ответ:*
- `Arrays.asList()` — адаптирует массив к List
- `InputStreamReader` — адаптирует InputStream к Reader
- `OutputStreamWriter` — адаптирует OutputStream к Writer
- `Collections.list()` — адаптирует Enumeration к ArrayList

**4. В чем разница между Adapter и Decorator?**

*Ответ:*
- **Adapter**: изменяет интерфейс объекта (преобразование)
- **Decorator**: сохраняет интерфейс, добавляет функциональность
- Adapter решает проблему несовместимости, Decorator расширяет возможности

### Продвинутые вопросы

**5. Как Adapter соблюдает SOLID принципы?**

*Ответ:*
- **SRP**: адаптер отвечает только за преобразование интерфейса
- **OCP**: можно добавлять новые адаптеры без изменения существующего кода
- **LSP**: адаптер может заменить целевой интерфейс
- **ISP**: клиент работает с узким интерфейсом
- **DIP**: зависимость от абстракций, а не от конкретных классов

**6. Можно ли создать адаптер для интерфейса с большим количеством методов?**

*Ответ:* Да, но это может быть сложно. Решения:
1. Реализовать только нужные методы, остальные бросают `UnsupportedOperationException`
2. Использовать абстрактный класс-адаптер с пустыми реализациями (паттерн Adapter для событий)
3. Разбить большой интерфейс на несколько маленьких (ISP)

```java
abstract class MouseAdapter implements MouseListener {
    public void mouseClicked(MouseEvent e) {}
    public void mousePressed(MouseEvent e) {}
    public void mouseReleased(MouseEvent e) {}
    // ... остальные методы
}

// Клиент переопределяет только нужные
class MyMouseAdapter extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
        // Обработка клика
    }
}
```

**7. Как Adapter помогает при рефакторинге legacy кода?**

*Ответ:* Adapter позволяет:
- Постепенно мигрировать на новый API, не ломая существующий код
- Изолировать legacy код за адаптером
- Тестировать новый код независимо
- Минимизировать изменения в клиентском коде

```java
// Legacy код
class LegacyUserService {
    public void saveUser(String name, String email) { }
}

// Новый интерфейс
interface UserService {
    void save(User user);
}

// Адаптер для миграции
class LegacyUserServiceAdapter implements UserService {
    private LegacyUserService legacy;
    
    @Override
    public void save(User user) {
        legacy.saveUser(user.getName(), user.getEmail());
    }
}
```

**8. Когда НЕ стоит использовать Adapter?**

*Ответ:*
- Когда можно изменить адаптируемый класс напрямую
- Когда интерфейсы слишком разные (нужно много преобразований)
- Когда производительность критична (overhead адаптера неприемлем)
- Когда добавление адаптера усложняет код больше, чем решает проблем

**9. Как комбинировать Adapter с другими паттернами?**

*Ответ:*
- **Facade**: Adapter адаптирует один класс, Facade упрощает работу с подсистемой
- **Decorator**: можно декорировать адаптер
- **Bridge**: Adapter адаптирует существующий код, Bridge проектируется заранее
- **Factory**: фабрика может создавать адаптеры

**10. Как тестировать адаптеры?**

*Ответ:*
```java
class PayPalAdapterTest {
    @Test
    public void shouldAdaptPayPalToPaymentProcessor() {
        // Arrange
        PayPalService payPal = mock(PayPalService.class);
        PayPalAdapter adapter = new PayPalAdapter(payPal, "test@test.com");
        
        when(payPal.getLastTransactionReference()).thenReturn("PP-123");
        
        // Act
        adapter.processPayment(100.0);
        String txId = adapter.getTransactionId();
        
        // Assert
        verify(payPal).makePayment("test@test.com", 100.0);
        assertEquals("PP-123", txId);
    }
}
```

---

[← Назад к разделу Структурные паттерны](README.md)
