# Bridge (Мост)

Bridge — структурный паттерн проектирования, который разделяет абстракцию и реализацию так, чтобы они могли изменяться независимо друг от друга.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Базовый пример с устройствами](#базовый-пример-с-устройствами)
   - [Графические фигуры и рендеринг](#графические-фигуры-и-рендеринг)
   - [Платежные системы](#платежные-системы)
   - [Логирование](#логирование)
5. [Примеры в JDK и фреймворках](#примеры-в-jdk-и-фреймворках)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Bridge используется когда:
- Нужно разделить монолитный класс с несколькими вариантами реализации
- Требуется расширять класс в двух независимых направлениях
- Необходимо переключать реализацию во время выполнения
- Изменения в реализации не должны влиять на клиентский код

**Типичные примеры использования:**
- Драйверы устройств (абстракция устройства + конкретная реализация)
- Графические библиотеки (фигуры + способы рендеринга)
- Платежные системы (способы оплаты + платежные провайдеры)
- Системы логирования (логгер + способы вывода)
- JDBC (интерфейс + драйверы для разных БД)

## Проблема, которую решает

### Проблема: Комбинаторный взрыв подклассов

```java
// Без паттерна Bridge - экспоненциальный рост числа классов

// Пульты для разных устройств
class SonyTV { }
class SamsungTV { }
class LGTV { }

class BasicRemoteForSony extends SonyTV { }
class BasicRemoteForSamsung extends SamsungTV { }
class BasicRemoteForLG extends LGTV { }

class AdvancedRemoteForSony extends SonyTV { }
class AdvancedRemoteForSamsung extends SamsungTV { }
class AdvancedRemoteForLG extends LGTV { }

class SmartRemoteForSony extends SonyTV { }
class SmartRemoteForSamsung extends SamsungTV { }
class SmartRemoteForLG extends LGTV { }

// 3 типа пультов × 3 производителя = 9 классов
// 5 типов пультов × 10 производителей = 50 классов!
```

**Проблемы:**
- Экспоненциальный рост количества классов при добавлении новых вариаций
- Дублирование кода между похожими классами
- Сложность поддержки и модификации
- Нарушение принципа единственной ответственности

### Решение: Bridge

Разделить абстракцию (пульт) и реализацию (устройство).

```java
// Реализация (Implementation)
interface Device {
    void turnOn();
    void turnOff();
    void setVolume(int volume);
}

// Абстракция (Abstraction)
class RemoteControl {
    protected Device device;
    
    public RemoteControl(Device device) {
        this.device = device;
    }
    
    public void togglePower() {
        // Использует device
    }
}

// Расширенные абстракции
class AdvancedRemote extends RemoteControl {
    public AdvancedRemote(Device device) {
        super(device);
    }
    
    public void mute() {
        device.setVolume(0);
    }
}

// Конкретные реализации
class SonyTV implements Device { }
class SamsungTV implements Device { }

// Теперь: 3 типа пультов + 3 устройства = 6 классов вместо 9!
```

## Структура паттерна

```java
// Implementor (Реализатор)
interface Implementation {
    void operationImpl();
}

// ConcreteImplementation (Конкретная реализация)
class ConcreteImplementationA implements Implementation {
    @Override
    public void operationImpl() {
        System.out.println("Implementation A");
    }
}

class ConcreteImplementationB implements Implementation {
    @Override
    public void operationImpl() {
        System.out.println("Implementation B");
    }
}

// Abstraction (Абстракция)
abstract class Abstraction {
    protected Implementation implementation;
    
    protected Abstraction(Implementation implementation) {
        this.implementation = implementation;
    }
    
    public abstract void operation();
}

// RefinedAbstraction (Уточненная абстракция)
class RefinedAbstraction extends Abstraction {
    public RefinedAbstraction(Implementation implementation) {
        super(implementation);
    }
    
    @Override
    public void operation() {
        System.out.print("RefinedAbstraction: ");
        implementation.operationImpl();
    }
}
```

## Реализация

### Базовый пример с устройствами

```java
// Интерфейс реализации - устройство
interface Device {
    boolean isEnabled();
    void enable();
    void disable();
    int getVolume();
    void setVolume(int percent);
    int getChannel();
    void setChannel(int channel);
    void printStatus();
}

// Конкретная реализация - Sony TV
class SonyTV implements Device {
    private boolean on = false;
    private int volume = 30;
    private int channel = 1;
    
    @Override
    public boolean isEnabled() {
        return on;
    }
    
    @Override
    public void enable() {
        on = true;
        System.out.println("Sony TV: включен");
    }
    
    @Override
    public void disable() {
        on = false;
        System.out.println("Sony TV: выключен");
    }
    
    @Override
    public int getVolume() {
        return volume;
    }
    
    @Override
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(volume, 100));
        System.out.println("Sony TV: громкость установлена на " + this.volume + "%");
    }
    
    @Override
    public int getChannel() {
        return channel;
    }
    
    @Override
    public void setChannel(int channel) {
        this.channel = channel;
        System.out.println("Sony TV: канал переключен на " + channel);
    }
    
    @Override
    public void printStatus() {
        System.out.println("------------------------------------");
        System.out.println("| Sony TV");
        System.out.println("| Включен: " + (on ? "да" : "нет"));
        System.out.println("| Текущая громкость: " + volume + "%");
        System.out.println("| Текущий канал: " + channel);
        System.out.println("------------------------------------");
    }
}

// Конкретная реализация - Samsung TV
class SamsungTV implements Device {
    private boolean on = false;
    private int volume = 50;
    private int channel = 1;
    
    @Override
    public boolean isEnabled() {
        return on;
    }
    
    @Override
    public void enable() {
        on = true;
        System.out.println("Samsung TV: powered on");
    }
    
    @Override
    public void disable() {
        on = false;
        System.out.println("Samsung TV: powered off");
    }
    
    @Override
    public int getVolume() {
        return volume;
    }
    
    @Override
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(volume, 100));
        System.out.println("Samsung TV: volume set to " + this.volume);
    }
    
    @Override
    public int getChannel() {
        return channel;
    }
    
    @Override
    public void setChannel(int channel) {
        this.channel = channel;
        System.out.println("Samsung TV: channel changed to " + channel);
    }
    
    @Override
    public void printStatus() {
        System.out.println("------------------------------------");
        System.out.println("| Samsung TV");
        System.out.println("| Power: " + (on ? "on" : "off"));
        System.out.println("| Volume: " + volume);
        System.out.println("| Channel: " + channel);
        System.out.println("------------------------------------");
    }
}

// Конкретная реализация - Radio
class Radio implements Device {
    private boolean on = false;
    private int volume = 25;
    private int channel = 881; // FM частота
    
    @Override
    public boolean isEnabled() {
        return on;
    }
    
    @Override
    public void enable() {
        on = true;
        System.out.println("Radio: включено");
    }
    
    @Override
    public void disable() {
        on = false;
        System.out.println("Radio: выключено");
    }
    
    @Override
    public int getVolume() {
        return volume;
    }
    
    @Override
    public void setVolume(int volume) {
        this.volume = Math.max(0, Math.min(volume, 100));
        System.out.println("Radio: громкость " + this.volume);
    }
    
    @Override
    public int getChannel() {
        return channel;
    }
    
    @Override
    public void setChannel(int channel) {
        this.channel = channel;
        System.out.println("Radio: частота " + (channel / 10.0) + " FM");
    }
    
    @Override
    public void printStatus() {
        System.out.println("------------------------------------");
        System.out.println("| Radio");
        System.out.println("| Включено: " + (on ? "да" : "нет"));
        System.out.println("| Громкость: " + volume);
        System.out.println("| Частота: " + (channel / 10.0) + " FM");
        System.out.println("------------------------------------");
    }
}

// Абстракция - базовый пульт
class RemoteControl {
    protected Device device;
    
    public RemoteControl(Device device) {
        this.device = device;
    }
    
    public void togglePower() {
        System.out.println("RemoteControl: переключение питания");
        if (device.isEnabled()) {
            device.disable();
        } else {
            device.enable();
        }
    }
    
    public void volumeDown() {
        System.out.println("RemoteControl: уменьшение громкости");
        device.setVolume(device.getVolume() - 10);
    }
    
    public void volumeUp() {
        System.out.println("RemoteControl: увеличение громкости");
        device.setVolume(device.getVolume() + 10);
    }
    
    public void channelDown() {
        System.out.println("RemoteControl: предыдущий канал");
        device.setChannel(device.getChannel() - 1);
    }
    
    public void channelUp() {
        System.out.println("RemoteControl: следующий канал");
        device.setChannel(device.getChannel() + 1);
    }
}

// Расширенная абстракция - продвинутый пульт
class AdvancedRemoteControl extends RemoteControl {
    
    public AdvancedRemoteControl(Device device) {
        super(device);
    }
    
    public void mute() {
        System.out.println("AdvancedRemoteControl: отключение звука");
        device.setVolume(0);
    }
    
    public void setChannel(int channel) {
        System.out.println("AdvancedRemoteControl: прямой выбор канала");
        device.setChannel(channel);
    }
}

// Использование
class BridgeDemo {
    public static void main(String[] args) {
        Device tv = new SonyTV();
        RemoteControl remote = new RemoteControl(tv);
        
        remote.togglePower();
        remote.volumeUp();
        remote.channelUp();
        tv.printStatus();
        
        System.out.println("\n");
        
        Device samsung = new SamsungTV();
        AdvancedRemoteControl advancedRemote = new AdvancedRemoteControl(samsung);
        
        advancedRemote.togglePower();
        advancedRemote.setChannel(5);
        advancedRemote.volumeUp();
        advancedRemote.mute();
        samsung.printStatus();
        
        System.out.println("\n");
        
        Device radio = new Radio();
        AdvancedRemoteControl radioRemote = new AdvancedRemoteControl(radio);
        
        radioRemote.togglePower();
        radioRemote.setChannel(1021); // 102.1 FM
        radioRemote.volumeUp();
        radio.printStatus();
    }
}
```

### Графические фигуры и рендеринг

```java
// Интерфейс реализации - способ отрисовки
interface Renderer {
    void renderCircle(double radius);
    void renderSquare(double side);
    void renderTriangle(double base, double height);
}

// Конкретная реализация - векторный рендеринг
class VectorRenderer implements Renderer {
    @Override
    public void renderCircle(double radius) {
        System.out.println("Рисуем круг радиусом " + radius + " в векторном формате");
    }
    
    @Override
    public void renderSquare(double side) {
        System.out.println("Рисуем квадрат со стороной " + side + " в векторном формате");
    }
    
    @Override
    public void renderTriangle(double base, double height) {
        System.out.println("Рисуем треугольник (" + base + "x" + height + ") в векторном формате");
    }
}

// Конкретная реализация - растровый рендеринг
class RasterRenderer implements Renderer {
    @Override
    public void renderCircle(double radius) {
        System.out.println("Отрисовка пикселями: круг радиусом " + radius);
    }
    
    @Override
    public void renderSquare(double side) {
        System.out.println("Отрисовка пикселями: квадрат со стороной " + side);
    }
    
    @Override
    public void renderTriangle(double base, double height) {
        System.out.println("Отрисовка пикселями: треугольник " + base + "x" + height);
    }
}

// Конкретная реализация - 3D рендеринг
class ThreeDRenderer implements Renderer {
    @Override
    public void renderCircle(double radius) {
        System.out.println("3D рендеринг: сфера радиусом " + radius);
    }
    
    @Override
    public void renderSquare(double side) {
        System.out.println("3D рендеринг: куб со стороной " + side);
    }
    
    @Override
    public void renderTriangle(double base, double height) {
        System.out.println("3D рендеринг: пирамида " + base + "x" + height);
    }
}

// Абстракция - базовая фигура
abstract class Shape {
    protected Renderer renderer;
    
    protected Shape(Renderer renderer) {
        this.renderer = renderer;
    }
    
    public abstract void draw();
    public abstract void resize(double factor);
}

// Расширенная абстракция - круг
class Circle extends Shape {
    private double radius;
    
    public Circle(Renderer renderer, double radius) {
        super(renderer);
        this.radius = radius;
    }
    
    @Override
    public void draw() {
        renderer.renderCircle(radius);
    }
    
    @Override
    public void resize(double factor) {
        radius *= factor;
        System.out.println("Круг изменен, новый радиус: " + radius);
    }
}

// Расширенная абстракция - квадрат
class Square extends Shape {
    private double side;
    
    public Square(Renderer renderer, double side) {
        super(renderer);
        this.side = side;
    }
    
    @Override
    public void draw() {
        renderer.renderSquare(side);
    }
    
    @Override
    public void resize(double factor) {
        side *= factor;
        System.out.println("Квадрат изменен, новая сторона: " + side);
    }
}

// Расширенная абстракция - треугольник
class Triangle extends Shape {
    private double base;
    private double height;
    
    public Triangle(Renderer renderer, double base, double height) {
        super(renderer);
        this.base = base;
        this.height = height;
    }
    
    @Override
    public void draw() {
        renderer.renderTriangle(base, height);
    }
    
    @Override
    public void resize(double factor) {
        base *= factor;
        height *= factor;
        System.out.println("Треугольник изменен: " + base + "x" + height);
    }
}

// Использование
class ShapesDemo {
    public static void main(String[] args) {
        Shape circle = new Circle(new VectorRenderer(), 5);
        circle.draw();
        circle.resize(2);
        circle.draw();
        
        System.out.println();
        
        Shape square = new Square(new RasterRenderer(), 10);
        square.draw();
        square.resize(0.5);
        square.draw();
        
        System.out.println();
        
        Shape triangle = new Triangle(new ThreeDRenderer(), 8, 6);
        triangle.draw();
        triangle.resize(1.5);
        triangle.draw();
    }
}
```

### Платежные системы

```java
// Интерфейс реализации - платежный провайдер
interface PaymentProvider {
    boolean processPayment(double amount, String currency);
    String getTransactionId();
    void refund(String transactionId, double amount);
}

// Конкретная реализация - PayPal
class PayPalProvider implements PaymentProvider {
    private String lastTransactionId;
    
    @Override
    public boolean processPayment(double amount, String currency) {
        lastTransactionId = "PP-" + System.currentTimeMillis();
        System.out.println("PayPal: обработка платежа " + amount + " " + currency);
        System.out.println("PayPal: transaction ID: " + lastTransactionId);
        return true;
    }
    
    @Override
    public String getTransactionId() {
        return lastTransactionId;
    }
    
    @Override
    public void refund(String transactionId, double amount) {
        System.out.println("PayPal: возврат " + amount + " для транзакции " + transactionId);
    }
}

// Конкретная реализация - Stripe
class StripeProvider implements PaymentProvider {
    private String lastTransactionId;
    
    @Override
    public boolean processPayment(double amount, String currency) {
        lastTransactionId = "stripe_" + System.currentTimeMillis();
        System.out.println("Stripe: processing payment of " + amount + " " + currency);
        System.out.println("Stripe: charge ID: " + lastTransactionId);
        return true;
    }
    
    @Override
    public String getTransactionId() {
        return lastTransactionId;
    }
    
    @Override
    public void refund(String transactionId, double amount) {
        System.out.println("Stripe: refunding " + amount + " for charge " + transactionId);
    }
}

// Конкретная реализация - Банковский перевод
class BankTransferProvider implements PaymentProvider {
    private String lastTransactionId;
    
    @Override
    public boolean processPayment(double amount, String currency) {
        lastTransactionId = "BANK-" + System.currentTimeMillis();
        System.out.println("Банк: инициация перевода " + amount + " " + currency);
        System.out.println("Банк: номер операции " + lastTransactionId);
        return true;
    }
    
    @Override
    public String getTransactionId() {
        return lastTransactionId;
    }
    
    @Override
    public void refund(String transactionId, double amount) {
        System.out.println("Банк: возврат средств " + amount + " по операции " + transactionId);
    }
}

// Абстракция - способ оплаты
abstract class PaymentMethod {
    protected PaymentProvider provider;
    
    protected PaymentMethod(PaymentProvider provider) {
        this.provider = provider;
    }
    
    public abstract void pay(double amount, String currency);
}

// Расширенная абстракция - разовый платеж
class OneTimePayment extends PaymentMethod {
    
    public OneTimePayment(PaymentProvider provider) {
        super(provider);
    }
    
    @Override
    public void pay(double amount, String currency) {
        System.out.println("\n=== Разовый платеж ===");
        boolean success = provider.processPayment(amount, currency);
        if (success) {
            System.out.println("Платеж успешно выполнен!");
        }
    }
}

// Расширенная абстракция - подписка
class SubscriptionPayment extends PaymentMethod {
    private final int billingCycle; // в днях
    
    public SubscriptionPayment(PaymentProvider provider, int billingCycle) {
        super(provider);
        this.billingCycle = billingCycle;
    }
    
    @Override
    public void pay(double amount, String currency) {
        System.out.println("\n=== Подписка (период: " + billingCycle + " дней) ===");
        boolean success = provider.processPayment(amount, currency);
        if (success) {
            System.out.println("Подписка активирована!");
            System.out.println("Следующее списание через " + billingCycle + " дней");
        }
    }
    
    public void cancel() {
        System.out.println("Подписка отменена");
    }
}

// Расширенная абстракция - платеж в рассрочку
class InstallmentPayment extends PaymentMethod {
    private final int installments;
    
    public InstallmentPayment(PaymentProvider provider, int installments) {
        super(provider);
        this.installments = installments;
    }
    
    @Override
    public void pay(double amount, String currency) {
        System.out.println("\n=== Платеж в рассрочку (" + installments + " платежей) ===");
        double installmentAmount = amount / installments;
        
        for (int i = 1; i <= installments; i++) {
            System.out.println("\nПлатеж " + i + " из " + installments + ":");
            provider.processPayment(installmentAmount, currency);
        }
        System.out.println("Все платежи обработаны!");
    }
}

// Использование
class PaymentDemo {
    public static void main(String[] args) {
        // Разовый платеж через PayPal
        PaymentMethod payment1 = new OneTimePayment(new PayPalProvider());
        payment1.pay(99.99, "USD");
        
        // Подписка через Stripe
        PaymentMethod payment2 = new SubscriptionPayment(new StripeProvider(), 30);
        payment2.pay(29.99, "USD");
        
        // Рассрочка через банк
        PaymentMethod payment3 = new InstallmentPayment(new BankTransferProvider(), 3);
        payment3.pay(1500.00, "USD");
    }
}
```

### Логирование

```java
// Интерфейс реализации - способ вывода логов
interface LogOutput {
    void write(String message);
    void close();
}

// Конкретная реализация - вывод в консоль
class ConsoleOutput implements LogOutput {
    @Override
    public void write(String message) {
        System.out.println("[CONSOLE] " + message);
    }
    
    @Override
    public void close() {
        System.out.println("[CONSOLE] Output closed");
    }
}

// Конкретная реализация - вывод в файл
class FileOutput implements LogOutput {
    private final String filename;
    
    public FileOutput(String filename) {
        this.filename = filename;
    }
    
    @Override
    public void write(String message) {
        System.out.println("[FILE:" + filename + "] " + message);
        // В реальности здесь была бы запись в файл
    }
    
    @Override
    public void close() {
        System.out.println("[FILE:" + filename + "] File closed");
    }
}

// Конкретная реализация - отправка по сети
class NetworkOutput implements LogOutput {
    private final String endpoint;
    
    public NetworkOutput(String endpoint) {
        this.endpoint = endpoint;
    }
    
    @Override
    public void write(String message) {
        System.out.println("[NETWORK:" + endpoint + "] Sending: " + message);
        // В реальности здесь была бы отправка по сети
    }
    
    @Override
    public void close() {
        System.out.println("[NETWORK:" + endpoint + "] Connection closed");
    }
}

// Абстракция - логгер
abstract class Logger {
    protected LogOutput output;
    
    protected Logger(LogOutput output) {
        this.output = output;
    }
    
    public abstract void log(String message);
    
    public void close() {
        output.close();
    }
}

// Расширенная абстракция - простой логгер
class SimpleLogger extends Logger {
    
    public SimpleLogger(LogOutput output) {
        super(output);
    }
    
    @Override
    public void log(String message) {
        output.write(message);
    }
}

// Расширенная абстракция - логгер с уровнями
class LevelLogger extends Logger {
    
    public LevelLogger(LogOutput output) {
        super(output);
    }
    
    @Override
    public void log(String message) {
        info(message);
    }
    
    public void debug(String message) {
        output.write("[DEBUG] " + message);
    }
    
    public void info(String message) {
        output.write("[INFO] " + message);
    }
    
    public void warning(String message) {
        output.write("[WARNING] " + message);
    }
    
    public void error(String message) {
        output.write("[ERROR] " + message);
    }
}

// Расширенная абстракция - логгер с временными метками
class TimestampLogger extends Logger {
    
    public TimestampLogger(LogOutput output) {
        super(output);
    }
    
    @Override
    public void log(String message) {
        String timestamp = java.time.LocalDateTime.now().toString();
        output.write("[" + timestamp + "] " + message);
    }
}

// Использование
class LoggingDemo {
    public static void main(String[] args) {
        // Простой логгер в консоль
        Logger consoleLogger = new SimpleLogger(new ConsoleOutput());
        consoleLogger.log("Application started");
        consoleLogger.close();
        
        System.out.println();
        
        // Логгер с уровнями в файл
        LevelLogger fileLogger = new LevelLogger(new FileOutput("app.log"));
        fileLogger.debug("Debug information");
        fileLogger.info("User logged in");
        fileLogger.warning("Low disk space");
        fileLogger.error("Connection failed");
        fileLogger.close();
        
        System.out.println();
        
        // Логгер с временными метками по сети
        Logger networkLogger = new TimestampLogger(new NetworkOutput("log-server:9000"));
        networkLogger.log("Critical system event");
        networkLogger.log("Database backup completed");
        networkLogger.close();
    }
}
```

## Примеры в JDK и фреймворках

### JDBC (Java Database Connectivity)

```java
// JDBC - классический пример Bridge
// Abstraction: DriverManager, Connection, Statement
// Implementation: конкретные драйверы (MySQL, PostgreSQL, Oracle)

import java.sql.*;

// Абстракция остается одинаковой
Connection connection = DriverManager.getConnection(url, user, password);
Statement statement = connection.createStatement();
ResultSet result = statement.executeQuery("SELECT * FROM users");

// Реализация меняется через драйвер:
// - com.mysql.jdbc.Driver
// - org.postgresql.Driver
// - oracle.jdbc.driver.OracleDriver
```

### Collections Framework

```java
// List (абстракция) + Array/LinkedList (реализации)
List<String> list = new ArrayList<>();  // массив-реализация
List<String> list2 = new LinkedList<>(); // связанный список-реализация

// Клиентский код работает с абстракцией List
// и не зависит от конкретной реализации
```

### SLF4J (Simple Logging Facade for Java)

```java
// SLF4J - фасад (абстракция)
// Logback, Log4j, java.util.logging - реализации

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

Logger logger = LoggerFactory.getLogger(MyClass.class);
logger.info("Message"); // работает с любой реализацией
```

### AWT/Swing

```java
// Компоненты GUI (абстракция)
// Peer-классы для разных ОС (реализация)

Button button = new Button("Click");
// WindowsPeer, UnixPeer, MacPeer - разные реализации
```

## Преимущества и недостатки

### Преимущества

**1. Разделение абстракции и реализации**
- Изменения в реализации не влияют на клиентский код
- Можно изменять и расширять независимо

**2. Уменьшение количества подклассов**
- Вместо n×m классов получаем n+m классов
- Избегаем комбинаторного взрыва

**3. Переключение реализации во время выполнения**
```java
Device device = new SonyTV();
RemoteControl remote = new RemoteControl(device);

// Можно сменить устройство
device = new SamsungTV();
remote = new RemoteControl(device);
```

**4. Соблюдение принципов SOLID**
- Single Responsibility: абстракция и реализация разделены
- Open/Closed: можно добавлять новые реализации без изменения абстракций
- Dependency Inversion: зависимость от абстракций, а не конкретных классов

**5. Упрощение тестирования**
- Легко создавать mock-реализации для тестов

### Недостатки

**1. Усложнение кода**
- Больше классов и интерфейсов
- Требуется понимание связей между компонентами

**2. Излишняя абстракция**
- Для простых случаев может быть избыточным
- Добавляет уровень косвенности

**3. Сложность выбора границы разделения**
- Не всегда очевидно, что должно быть абстракцией, а что реализацией

## Вопросы на собеседовании

**1. В чем отличие Bridge от Adapter?**

*Ответ:*
- **Bridge** разрабатывается заранее для независимого изменения абстракции и реализации. Используется для разделения интерфейса и реализации.
- **Adapter** создается постфактум для совместной работы несовместимых интерфейсов. Используется для адаптации существующих классов.

```java
// Bridge - планируем заранее
abstract class Shape {
    protected Renderer renderer; // известно с самого начала
}

// Adapter - адаптируем существующий класс
class LegacyRectangle { /* существующий класс */ }
class RectangleAdapter extends Shape {
    private LegacyRectangle legacy; // адаптируем постфактум
}
```

**2. Как Bridge помогает избежать комбинаторного взрыва классов?**

*Ответ:*
Без Bridge при наличии n абстракций и m реализаций потребуется n×m классов. С Bridge нужно только n+m классов.

Пример: 3 типа пультов × 4 устройства = 12 классов
С Bridge: 3 класса пультов + 4 класса устройств = 7 классов

**3. Можно ли использовать Bridge совместно с другими паттернами?**

*Ответ:*
Да, Bridge часто комбинируется с:
- **Abstract Factory** для создания связанных пар абстракция-реализация
- **Adapter** когда нужно адаптировать существующую реализацию
- **Strategy** Bridge может использовать Strategy для выбора реализации
- **Composite** для построения сложных иерархий

```java
// Bridge + Abstract Factory
interface DeviceFactory {
    Device createDevice();
    RemoteControl createRemote(Device device);
}
```

**4. Когда НЕ стоит использовать Bridge?**

*Ответ:*
Не используйте Bridge когда:
- Есть только одна реализация и не планируется расширение
- Абстракция и реализация жестко связаны и не изменяются независимо
- Добавление уровня абстракции создаст излишнюю сложность
- Система простая и не требует такой гибкости

**5. Как Bridge соотносится с принципом SOLID?**

*Ответ:*
Bridge поддерживает:
- **SRP**: разделяет ответственность между абстракцией и реализацией
- **OCP**: открыт для расширения (новые реализации), закрыт для модификации
- **LSP**: подклассы абстракции взаимозаменяемы
- **ISP**: интерфейсы разделены по ролям
- **DIP**: зависимость от абстракций (интерфейс Implementation)

**6. Как реализовать thread-safe Bridge?**

*Ответ:*
```java
class ThreadSafeRemoteControl {
    private final Device device; // immutable
    private final Object lock = new Object();
    
    public void togglePower() {
        synchronized (lock) {
            if (device.isEnabled()) {
                device.disable();
            } else {
                device.enable();
            }
        }
    }
}
```

**7. Чем Bridge отличается от Strategy?**

*Ответ:*
- **Bridge**: разделяет абстракцию и реализацию, обе могут изменяться независимо. Фокус на структуре.
- **Strategy**: инкапсулирует взаимозаменяемые алгоритмы. Фокус на поведении.

```java
// Bridge - две иерархии
class RemoteControl {
    protected Device device; // иерархия реализаций
}

// Strategy - одна иерархия алгоритмов
class Context {
    private Strategy strategy; // алгоритм
}
```

**8. Как тестировать код с использованием Bridge?**

*Ответ:*
```java
// Создаем mock-реализацию для тестов
class MockDevice implements Device {
    public int volumeSetCount = 0;
    
    @Override
    public void setVolume(int volume) {
        volumeSetCount++;
    }
    // остальные методы
}

@Test
public void testRemoteControl() {
    MockDevice device = new MockDevice();
    RemoteControl remote = new RemoteControl(device);
    
    remote.volumeUp();
    assertEquals(1, device.volumeSetCount);
}
```

**9. Какие проблемы могут возникнуть при использовании Bridge?**

*Ответ:*
- **Сложность понимания**: новичкам трудно понять две иерархии
- **Overhead**: дополнительный уровень абстракции влияет на производительность
- **Неправильное разделение**: ошибка в определении границы между абстракцией и реализацией
- **Утечка абстракции**: когда детали реализации просачиваются в абстракцию

**10. Приведите real-world пример использования Bridge в enterprise приложениях**

*Ответ:*
```java
// Система отчетов

// Реализация - формат отчета
interface ReportFormatter {
    void format(ReportData data);
}

class PDFFormatter implements ReportFormatter { }
class ExcelFormatter implements ReportFormatter { }
class HTMLFormatter implements ReportFormatter { }

// Абстракция - тип отчета
abstract class Report {
    protected ReportFormatter formatter;
    
    protected Report(ReportFormatter formatter) {
        this.formatter = formatter;
    }
    
    public abstract void generate();
}

class SalesReport extends Report {
    public SalesReport(ReportFormatter formatter) {
        super(formatter);
    }
    
    @Override
    public void generate() {
        ReportData data = fetchSalesData();
        formatter.format(data);
    }
}

class InventoryReport extends Report {
    // аналогично
}

// Использование
Report report = new SalesReport(new PDFFormatter());
report.generate(); // отчет по продажам в PDF

report = new InventoryReport(new ExcelFormatter());
report.generate(); // отчет по складу в Excel
```

Это позволяет:
- Добавлять новые типы отчетов без изменения форматтеров
- Добавлять новые форматы без изменения отчетов
- Комбинировать любой отчет с любым форматом
