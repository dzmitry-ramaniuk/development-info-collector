# Facade (Фасад)

Facade — структурный паттерн проектирования, который предоставляет простой интерфейс к сложной системе классов, библиотеке или фреймворку.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Домашний кинотеатр](#домашний-кинотеатр)
   - [Система онлайн-заказов](#система-онлайн-заказов)
   - [Компилятор](#компилятор)
5. [Примеры в JDK и фреймворках](#примеры-в-jdk-и-фреймворках)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Facade используется когда:
- Необходимо предоставить простой интерфейс к сложной подсистеме
- Нужно уменьшить зависимости между клиентом и реализацией подсистемы
- Требуется разбить подсистему на слои
- Нужен единый интерфейс для работы с набором интерфейсов

**Типичные примеры использования:**
- Упрощение работы со сложными библиотеками
- API обертки над legacy системами
- Единая точка входа в подсистему
- Слои в многоуровневой архитектуре
- Клиентские библиотеки для REST API

## Проблема, которую решает

### Проблема: Сложное взаимодействие с подсистемой

```java
// Без Facade - клиент должен знать о множестве классов

public class Client {
    public void watchMovie(String movie) {
        // Включаем все устройства
        Amplifier amp = new Amplifier();
        DvdPlayer dvd = new DvdPlayer();
        Projector projector = new Projector();
        Screen screen = new Screen();
        TheaterLights lights = new TheaterLights();
        PopcornPopper popper = new PopcornPopper();
        
        // Настраиваем каждое устройство
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.down();
        projector.on();
        projector.setInput(dvd);
        projector.wideScreenMode();
        amp.on();
        amp.setDvd(dvd);
        amp.setSurroundSound();
        amp.setVolume(5);
        dvd.on();
        dvd.play(movie);
        
        // Клиент должен знать о 6+ классах и порядке вызовов!
    }
}
```

**Проблемы:**
- Клиент сильно связан с множеством классов подсистемы
- Сложный и подверженный ошибкам код
- Дублирование логики настройки
- Изменения в подсистеме влияют на всех клиентов

### Решение: Facade

Создать простой интерфейс для сложных операций.

```java
// С Facade - простой интерфейс
public class HomeTheaterFacade {
    private Amplifier amp;
    private DvdPlayer dvd;
    private Projector projector;
    private Screen screen;
    private TheaterLights lights;
    private PopcornPopper popper;
    
    public void watchMovie(String movie) {
        System.out.println("Приготовьтесь смотреть фильм...");
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.down();
        projector.on();
        projector.wideScreenMode();
        amp.on();
        amp.setDvd(dvd);
        amp.setSurroundSound();
        amp.setVolume(5);
        dvd.on();
        dvd.play(movie);
    }
    
    public void endMovie() {
        System.out.println("Выключаем кинотеатр...");
        // упрощенная логика выключения
    }
}

// Клиент
HomeTheaterFacade homeTheater = new HomeTheaterFacade(...);
homeTheater.watchMovie("Матрица");
homeTheater.endMovie();
```

## Структура паттерна

```java
// Сложная подсистема
class SubsystemA {
    public void operationA1() {
        System.out.println("Subsystem A: operation A1");
    }
    
    public void operationA2() {
        System.out.println("Subsystem A: operation A2");
    }
}

class SubsystemB {
    public void operationB1() {
        System.out.println("Subsystem B: operation B1");
    }
}

class SubsystemC {
    public void operationC1() {
        System.out.println("Subsystem C: operation C1");
    }
}

// Facade - упрощенный интерфейс
class Facade {
    private SubsystemA subsystemA;
    private SubsystemB subsystemB;
    private SubsystemC subsystemC;
    
    public Facade() {
        this.subsystemA = new SubsystemA();
        this.subsystemB = new SubsystemB();
        this.subsystemC = new SubsystemC();
    }
    
    // Высокоуровневая операция
    public void operation1() {
        System.out.println("Facade operation 1:");
        subsystemA.operationA1();
        subsystemB.operationB1();
    }
    
    public void operation2() {
        System.out.println("Facade operation 2:");
        subsystemA.operationA2();
        subsystemC.operationC1();
    }
}

// Клиент работает только с Facade
class Client {
    public static void main(String[] args) {
        Facade facade = new Facade();
        facade.operation1();
        facade.operation2();
    }
}
```

## Реализация

### Домашний кинотеатр

Полный пример с кинотеатром показывает, как Facade упрощает управление множеством устройств.

```java
// Усилитель
class Amplifier {
    private String description;
    private DvdPlayer dvd;
    
    public Amplifier(String description) {
        this.description = description;
    }
    
    public void on() {
        System.out.println(description + " включен");
    }
    
    public void off() {
        System.out.println(description + " выключен");
    }
    
    public void setDvd(DvdPlayer dvd) {
        System.out.println(description + " подключен к DVD плееру");
        this.dvd = dvd;
    }
    
    public void setSurroundSound() {
        System.out.println(description + " установлен режим объемного звучания (5 колонок)");
    }
    
    public void setStereoSound() {
        System.out.println(description + " установлен стерео режим");
    }
    
    public void setVolume(int level) {
        System.out.println(description + " громкость установлена на " + level);
    }
}

// DVD проигрыватель
class DvdPlayer {
    private String description;
    private String currentMovie;
    
    public DvdPlayer(String description) {
        this.description = description;
    }
    
    public void on() {
        System.out.println(description + " включен");
    }
    
    public void off() {
        System.out.println(description + " выключен");
    }
    
    public void play(String movie) {
        currentMovie = movie;
        System.out.println(description + " проигрывает \"" + movie + "\"");
    }
    
    public void stop() {
        System.out.println(description + " остановлен");
        currentMovie = null;
    }
    
    public void eject() {
        if (currentMovie != null) {
            System.out.println(description + " извлечение \"" + currentMovie + "\"");
            currentMovie = null;
        }
    }
}

// Проектор
class Projector {
    private String description;
    private DvdPlayer dvdPlayer;
    
    public Projector(String description) {
        this.description = description;
    }
    
    public void on() {
        System.out.println(description + " включен");
    }
    
    public void off() {
        System.out.println(description + " выключен");
    }
    
    public void setInput(DvdPlayer dvd) {
        System.out.println(description + " источник входного сигнала установлен на DVD");
        this.dvdPlayer = dvd;
    }
    
    public void wideScreenMode() {
        System.out.println(description + " установлен широкоэкранный режим (16:9)");
    }
    
    public void tvMode() {
        System.out.println(description + " установлен режим ТВ (4:3)");
    }
}

// Экран
class Screen {
    private String description;
    
    public Screen(String description) {
        this.description = description;
    }
    
    public void up() {
        System.out.println(description + " поднимается");
    }
    
    public void down() {
        System.out.println(description + " опускается");
    }
}

// Освещение
class TheaterLights {
    private String description;
    
    public TheaterLights(String description) {
        this.description = description;
    }
    
    public void on() {
        System.out.println(description + " включено на 100%");
    }
    
    public void off() {
        System.out.println(description + " выключено");
    }
    
    public void dim(int level) {
        System.out.println(description + " затемнено до " + level + "%");
    }
}

// Попкорн машина
class PopcornPopper {
    private String description;
    
    public PopcornPopper(String description) {
        this.description = description;
    }
    
    public void on() {
        System.out.println(description + " включена");
    }
    
    public void off() {
        System.out.println(description + " выключена");
    }
    
    public void pop() {
        System.out.println(description + " готовит попкорн!");
    }
}

// Фасад домашнего кинотеатра
class HomeTheaterFacade {
    private Amplifier amp;
    private DvdPlayer dvd;
    private Projector projector;
    private Screen screen;
    private TheaterLights lights;
    private PopcornPopper popper;
    
    public HomeTheaterFacade(
            Amplifier amp,
            DvdPlayer dvd,
            Projector projector,
            Screen screen,
            TheaterLights lights,
            PopcornPopper popper) {
        this.amp = amp;
        this.dvd = dvd;
        this.projector = projector;
        this.screen = screen;
        this.lights = lights;
        this.popper = popper;
    }
    
    public void watchMovie(String movie) {
        System.out.println("Приготовьтесь смотреть фильм \"" + movie + "\"...");
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.down();
        projector.on();
        projector.setInput(dvd);
        projector.wideScreenMode();
        amp.on();
        amp.setDvd(dvd);
        amp.setSurroundSound();
        amp.setVolume(5);
        dvd.on();
        dvd.play(movie);
    }
    
    public void endMovie() {
        System.out.println("Выключаем домашний кинотеатр...");
        popper.off();
        lights.on();
        screen.up();
        projector.off();
        amp.off();
        dvd.stop();
        dvd.eject();
        dvd.off();
    }
    
    public void listenToRadio(double frequency) {
        System.out.println("Настройка на радио " + frequency + " FM...");
        amp.on();
        amp.setVolume(5);
        System.out.println("Радио настроено на " + frequency);
    }
}

// Демонстрация
class HomeTheaterDemo {
    public static void main(String[] args) {
        // Создаем компоненты
        Amplifier amp = new Amplifier("Усилитель Top-O-Line");
        DvdPlayer dvd = new DvdPlayer("DVD плеер Top-O-Line");
        Projector projector = new Projector("Проектор Top-O-Line");
        Screen screen = new Screen("Экран театра");
        TheaterLights lights = new TheaterLights("Освещение театра");
        PopcornPopper popper = new PopcornPopper("Попкорн машина");
        
        // Создаем фасад
        HomeTheaterFacade homeTheater = new HomeTheaterFacade(
            amp, dvd, projector, screen, lights, popper);
        
        // Простое использование!
        homeTheater.watchMovie("Матрица");
        System.out.println("\n");
        homeTheater.endMovie();
    }
}
```

**Вывод:**
```
Приготовьтесь смотреть фильм "Матрица"...
Попкорн машина включена
Попкорн машина готовит попкорн!
Освещение театра затемнено до 10%
Экран театра опускается
Проектор Top-O-Line включен
Проектор Top-O-Line источник входного сигнала установлен на DVD
Проектор Top-O-Line установлен широкоэкранный режим (16:9)
Усилитель Top-O-Line включен
Усилитель Top-O-Line подключен к DVD плееру
Усилитель Top-O-Line установлен режим объемного звучания (5 колонок)
Усилитель Top-O-Line громкость установлена на 5
DVD плеер Top-O-Line включен
DVD плеер Top-O-Line проигрывает "Матрица"

Выключаем домашний кинотеатр...
Попкорн машина выключена
Освещение театра включено на 100%
Экран театра поднимается
Проектор Top-O-Line выключен
Усилитель Top-O-Line выключен
DVD плеер Top-O-Line остановлен
DVD плеер Top-O-Line извлечение "Матрица"
DVD плеер Top-O-Line выключен
```

### Система онлайн-заказов

Пример показывает, как Facade координирует работу нескольких независимых сервисов.

```java
// Сервис инвентаризации
class InventoryService {
    public boolean checkStock(String productId, int quantity) {
        System.out.println("Проверка наличия товара " + productId + 
                         ", количество: " + quantity);
        // Имитация проверки
        return true;
    }
    
    public void reserveStock(String productId, int quantity) {
        System.out.println("Резервирование товара " + productId + 
                         ", количество: " + quantity);
    }
    
    public void releaseStock(String productId, int quantity) {
        System.out.println("Освобождение резерва товара " + productId);
    }
}

// Сервис платежей
class PaymentService {
    public boolean processPayment(String customerId, double amount) {
        System.out.println("Обработка платежа для клиента " + customerId + 
                         ", сумма: " + amount + " руб.");
        // Имитация обработки платежа
        return true;
    }
    
    public void refund(String transactionId, double amount) {
        System.out.println("Возврат средств по транзакции " + transactionId + 
                         ", сумма: " + amount + " руб.");
    }
}

// Сервис доставки
class ShippingService {
    public String scheduleDelivery(String address, String productId) {
        System.out.println("Планирование доставки по адресу: " + address);
        System.out.println("Товар: " + productId);
        String trackingNumber = "TRACK-" + System.currentTimeMillis();
        System.out.println("Номер отслеживания: " + trackingNumber);
        return trackingNumber;
    }
    
    public void cancelDelivery(String trackingNumber) {
        System.out.println("Отмена доставки: " + trackingNumber);
    }
}

// Сервис уведомлений
class NotificationService {
    public void sendOrderConfirmation(String customerId, String orderId) {
        System.out.println("Отправка подтверждения заказа " + orderId + 
                         " клиенту " + customerId);
    }
    
    public void sendShippingNotification(String customerId, String trackingNumber) {
        System.out.println("Отправка уведомления о доставке клиенту " + customerId);
        System.out.println("Номер отслеживания: " + trackingNumber);
    }
    
    public void sendCancellationNotification(String customerId, String orderId) {
        System.out.println("Отправка уведомления об отмене заказа " + orderId);
    }
}

// Модель заказа
class Order {
    private String orderId;
    private String customerId;
    private String productId;
    private int quantity;
    private double totalAmount;
    private String shippingAddress;
    
    public Order(String customerId, String productId, int quantity, 
                double totalAmount, String shippingAddress) {
        this.orderId = "ORDER-" + System.currentTimeMillis();
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
    }
    
    // Геттеры
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getTotalAmount() { return totalAmount; }
    public String getShippingAddress() { return shippingAddress; }
}

// Фасад системы заказов
class OrderFacade {
    private InventoryService inventoryService;
    private PaymentService paymentService;
    private ShippingService shippingService;
    private NotificationService notificationService;
    
    public OrderFacade() {
        this.inventoryService = new InventoryService();
        this.paymentService = new PaymentService();
        this.shippingService = new ShippingService();
        this.notificationService = new NotificationService();
    }
    
    /**
     * Единый метод для размещения заказа
     * Координирует работу всех сервисов
     */
    public boolean placeOrder(Order order) {
        System.out.println("=== Размещение заказа " + order.getOrderId() + " ===\n");
        
        try {
            // 1. Проверяем наличие товара
            if (!inventoryService.checkStock(order.getProductId(), order.getQuantity())) {
                System.out.println("Товар отсутствует на складе");
                return false;
            }
            
            // 2. Резервируем товар
            inventoryService.reserveStock(order.getProductId(), order.getQuantity());
            
            // 3. Обрабатываем платеж
            if (!paymentService.processPayment(order.getCustomerId(), order.getTotalAmount())) {
                System.out.println("Ошибка обработки платежа");
                inventoryService.releaseStock(order.getProductId(), order.getQuantity());
                return false;
            }
            
            // 4. Планируем доставку
            String trackingNumber = shippingService.scheduleDelivery(
                order.getShippingAddress(), order.getProductId());
            
            // 5. Отправляем уведомления
            notificationService.sendOrderConfirmation(order.getCustomerId(), order.getOrderId());
            notificationService.sendShippingNotification(order.getCustomerId(), trackingNumber);
            
            System.out.println("\n✓ Заказ успешно размещен!");
            return true;
            
        } catch (Exception e) {
            System.out.println("Ошибка при размещении заказа: " + e.getMessage());
            // Откат изменений
            inventoryService.releaseStock(order.getProductId(), order.getQuantity());
            return false;
        }
    }
    
    /**
     * Отмена заказа
     */
    public void cancelOrder(Order order, String trackingNumber) {
        System.out.println("\n=== Отмена заказа " + order.getOrderId() + " ===\n");
        
        inventoryService.releaseStock(order.getProductId(), order.getQuantity());
        paymentService.refund(order.getOrderId(), order.getTotalAmount());
        shippingService.cancelDelivery(trackingNumber);
        notificationService.sendCancellationNotification(order.getCustomerId(), order.getOrderId());
        
        System.out.println("\n✓ Заказ отменен");
    }
}

// Демонстрация
class OrderSystemDemo {
    public static void main(String[] args) {
        OrderFacade orderFacade = new OrderFacade();
        
        // Создаем заказ
        Order order = new Order(
            "CUST-001",
            "PROD-12345",
            2,
            5999.99,
            "Москва, ул. Ленина, д. 10, кв. 5"
        );
        
        // Размещаем заказ - одна простая операция!
        orderFacade.placeOrder(order);
    }
}
```

**Вывод:**
```
=== Размещение заказа ORDER-1701234567890 ===

Проверка наличия товара PROD-12345, количество: 2
Резервирование товара PROD-12345, количество: 2
Обработка платежа для клиента CUST-001, сумма: 5999.99 руб.
Планирование доставки по адресу: Москва, ул. Ленина, д. 10, кв. 5
Товар: PROD-12345
Номер отслеживания: TRACK-1701234567891
Отправка подтверждения заказа ORDER-1701234567890 клиенту CUST-001
Отправка уведомления о доставке клиенту CUST-001
Номер отслеживания: TRACK-1701234567891

✓ Заказ успешно размещен!
```

### Компилятор

Пример Facade для компилятора, координирующего сложные этапы компиляции.

```java
// Лексический анализатор (Scanner)
class Scanner {
    public List<Token> scan(String sourceCode) {
        System.out.println("1. Лексический анализ исходного кода...");
        List<Token> tokens = new ArrayList<>();
        
        // Упрощенная токенизация
        String[] words = sourceCode.split("\\s+");
        for (String word : words) {
            tokens.add(new Token(word));
        }
        
        System.out.println("   Найдено токенов: " + tokens.size());
        return tokens;
    }
}

class Token {
    private String value;
    
    public Token(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return "Token('" + value + "')";
    }
}

// Синтаксический анализатор (Parser)
class Parser {
    public ASTNode parse(List<Token> tokens) {
        System.out.println("2. Синтаксический анализ токенов...");
        
        ASTNode root = new ASTNode("Program");
        for (Token token : tokens) {
            root.addChild(new ASTNode(token.getValue()));
        }
        
        System.out.println("   Построено AST дерево");
        return root;
    }
}

// Узел абстрактного синтаксического дерева
class ASTNode {
    private String value;
    private List<ASTNode> children = new ArrayList<>();
    
    public ASTNode(String value) {
        this.value = value;
    }
    
    public void addChild(ASTNode child) {
        children.add(child);
    }
    
    public String getValue() {
        return value;
    }
    
    public List<ASTNode> getChildren() {
        return children;
    }
}

// Семантический анализатор
class SemanticAnalyzer {
    public void analyze(ASTNode ast) {
        System.out.println("3. Семантический анализ AST...");
        System.out.println("   Проверка типов и областей видимости");
        // Имитация проверки
    }
}

// Оптимизатор
class Optimizer {
    public ASTNode optimize(ASTNode ast) {
        System.out.println("4. Оптимизация кода...");
        System.out.println("   Применение оптимизаций уровня -O2");
        // Возвращаем оптимизированное AST
        return ast;
    }
}

// Генератор кода
class CodeGenerator {
    public String generateCode(ASTNode ast) {
        System.out.println("5. Генерация машинного кода...");
        
        StringBuilder code = new StringBuilder();
        code.append("; Сгенерированный код\n");
        code.append("SECTION .text\n");
        code.append("global _start\n");
        code.append("_start:\n");
        
        for (ASTNode child : ast.getChildren()) {
            code.append("    ; ").append(child.getValue()).append("\n");
        }
        
        code.append("    mov eax, 1\n");
        code.append("    xor ebx, ebx\n");
        code.append("    int 0x80\n");
        
        System.out.println("   Код сгенерирован");
        return code.toString();
    }
}

// Линковщик
class Linker {
    public void link(String objectCode, String outputFile) {
        System.out.println("6. Линковка объектного кода...");
        System.out.println("   Создание исполняемого файла: " + outputFile);
        System.out.println("   Подключение стандартных библиотек");
    }
}

// Фасад компилятора
class CompilerFacade {
    private Scanner scanner;
    private Parser parser;
    private SemanticAnalyzer semanticAnalyzer;
    private Optimizer optimizer;
    private CodeGenerator codeGenerator;
    private Linker linker;
    
    public CompilerFacade() {
        this.scanner = new Scanner();
        this.parser = new Parser();
        this.semanticAnalyzer = new SemanticAnalyzer();
        this.optimizer = new Optimizer();
        this.codeGenerator = new CodeGenerator();
        this.linker = new Linker();
    }
    
    /**
     * Единый метод компиляции - скрывает всю сложность
     */
    public void compile(String sourceCode, String outputFile) {
        System.out.println("=== Компиляция программы ===\n");
        
        try {
            // Весь pipeline компиляции
            List<Token> tokens = scanner.scan(sourceCode);
            ASTNode ast = parser.parse(tokens);
            semanticAnalyzer.analyze(ast);
            ASTNode optimizedAst = optimizer.optimize(ast);
            String objectCode = codeGenerator.generateCode(optimizedAst);
            linker.link(objectCode, outputFile);
            
            System.out.println("\n✓ Компиляция завершена успешно!");
            System.out.println("Выходной файл: " + outputFile);
            
        } catch (Exception e) {
            System.err.println("✗ Ошибка компиляции: " + e.getMessage());
        }
    }
    
    /**
     * Компиляция с настройками
     */
    public void compileWithOptions(String sourceCode, String outputFile, 
                                   boolean optimize, boolean debug) {
        System.out.println("=== Компиляция с опциями ===");
        System.out.println("Оптимизация: " + (optimize ? "включена" : "выключена"));
        System.out.println("Отладка: " + (debug ? "включена" : "выключена"));
        System.out.println();
        
        List<Token> tokens = scanner.scan(sourceCode);
        ASTNode ast = parser.parse(tokens);
        semanticAnalyzer.analyze(ast);
        
        if (optimize) {
            ast = optimizer.optimize(ast);
        }
        
        String objectCode = codeGenerator.generateCode(ast);
        linker.link(objectCode, outputFile);
        
        System.out.println("\n✓ Компиляция завершена!");
    }
}

// Демонстрация
class CompilerDemo {
    public static void main(String[] args) {
        CompilerFacade compiler = new CompilerFacade();
        
        String sourceCode = "int main() { return 0; }";
        
        // Простая компиляция
        compiler.compile(sourceCode, "program.exe");
        
        System.out.println("\n" + "=".repeat(50) + "\n");
        
        // Компиляция с опциями
        compiler.compileWithOptions(sourceCode, "program_optimized.exe", true, false);
    }
}
```

**Вывод:**
```
=== Компиляция программы ===

1. Лексический анализ исходного кода...
   Найдено токенов: 6
2. Синтаксический анализ токенов...
   Построено AST дерево
3. Семантический анализ AST...
   Проверка типов и областей видимости
4. Оптимизация кода...
   Применение оптимизаций уровня -O2
5. Генерация машинного кода...
   Код сгенерирован
6. Линковка объектного кода...
   Создание исполняемого файла: program.exe
   Подключение стандартных библиотек

✓ Компиляция завершена успешно!
Выходной файл: program.exe
```

## Примеры в JDK и фреймворках

### JDBC (Java Database Connectivity)

JDBC использует Facade для упрощения работы с базами данных.

```java
// Без Facade нужно работать с множеством низкоуровневых API

// Driver Manager - Facade для работы с драйверами
import java.sql.*;

public class JdbcFacadeExample {
    
    // DriverManager скрывает сложность регистрации драйверов,
    // установки соединения, управления пулами и т.д.
    public void demonstrateJdbcFacade() {
        String url = "jdbc:postgresql://localhost:5432/mydb";
        String user = "user";
        String password = "password";
        
        // Простой интерфейс вместо сложной работы с драйверами
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            
            while (rs.next()) {
                System.out.println(rs.getString("name"));
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // DriverManager скрывает:
        // - Регистрацию драйверов
        // - Установку TCP соединения
        // - Аутентификацию
        // - Управление протоколом БД
        // - Пул соединений (в некоторых реализациях)
    }
}
```

### SLF4J (Simple Logging Facade for Java)

SLF4J — это фасад для различных logging фреймворков.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jFacadeExample {
    // SLF4J предоставляет единый интерфейс
    private static final Logger logger = LoggerFactory.getLogger(Slf4jFacadeExample.class);
    
    public void demonstrateLogging() {
        // Простой API, скрывающий детали реализации
        logger.info("Информационное сообщение");
        logger.warn("Предупреждение");
        logger.error("Ошибка");
        
        // SLF4J может использовать разные реализации:
        // - Logback
        // - Log4j
        // - java.util.logging
        // - Simple Logger
        // Клиентский код не зависит от конкретной реализации!
    }
    
    public void demonstrateParameterizedLogging() {
        String user = "Иван";
        int attempts = 3;
        
        // Эффективное логирование с параметрами
        logger.info("Пользователь {} выполнил {} попыток входа", user, attempts);
        
        // SLF4J скрывает:
        // - Конкатенацию строк
        // - Проверку уровня логирования
        // - Форматирование
        // - Запись в различные appenders
    }
}
```

### Spring Framework

Spring предоставляет множество фасадов для упрощения работы.

```java
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// JdbcTemplate - Facade для JDBC
@Service
public class SpringJdbcFacade {
    private final JdbcTemplate jdbcTemplate;
    
    public SpringJdbcFacade(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    // Простой запрос вместо сложного JDBC кода
    public List<User> findAllUsers() {
        return jdbcTemplate.query(
            "SELECT * FROM users",
            (rs, rowNum) -> new User(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email")
            )
        );
    }
    
    // JdbcTemplate скрывает:
    // - Получение соединения
    // - Создание PreparedStatement
    // - Обработку ResultSet
    // - Закрытие ресурсов
    // - Обработку исключений
    // - Преобразование SQLException в DataAccessException
}

// RestTemplate - Facade для HTTP клиента
@Service
public class SpringRestFacade {
    private final RestTemplate restTemplate;
    
    public SpringRestFacade(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public User getUserById(Long id) {
        String url = "https://api.example.com/users/" + id;
        
        // Простой вызов вместо низкоуровневого HTTP
        return restTemplate.getForObject(url, User.class);
        
        // RestTemplate скрывает:
        // - Установку HTTP соединения
        // - Формирование запроса
        // - Сериализацию/десериализацию JSON
        // - Обработку ошибок HTTP
        // - Закрытие соединений
    }
}

// TransactionTemplate - Facade для управления транзакциями
@Service
public class SpringTransactionFacade {
    private final TransactionTemplate transactionTemplate;
    
    public SpringTransactionFacade(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }
    
    public void performComplexOperation() {
        transactionTemplate.execute(status -> {
            try {
                // Бизнес-логика в транзакции
                // updateDatabase();
                // sendNotification();
                return null;
            } catch (Exception e) {
                status.setRollbackOnly();
                throw e;
            }
        });
        
        // TransactionTemplate скрывает:
        // - Начало транзакции
        // - Commit/Rollback
        // - Обработку исключений
        // - Управление соединением
    }
}

class User {
    private Long id;
    private String name;
    private String email;
    
    public User(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
    
    // Геттеры и сеттеры
}
```

### EntityManager (JPA)

```java
import javax.persistence.*;

@Service
public class JpaFacadeExample {
    @PersistenceContext
    private EntityManager entityManager;
    
    public void demonstrateJpaFacade() {
        // EntityManager - Facade для ORM операций
        
        // Простое сохранение
        User user = new User("Иван", "ivan@example.com");
        entityManager.persist(user);
        
        // Простой поиск
        User found = entityManager.find(User.class, 1L);
        
        // Простой запрос
        List<User> users = entityManager
            .createQuery("SELECT u FROM User u WHERE u.name LIKE :name", User.class)
            .setParameter("name", "Ив%")
            .getResultList();
        
        // EntityManager скрывает:
        // - SQL генерацию
        // - Маппинг объектов
        // - Кеширование (первого и второго уровня)
        // - Lazy/Eager loading
        // - Dirty checking
        // - Управление транзакциями
        // - Работу с JDBC
    }
}
```

## Преимущества и недостатки

### Преимущества

✅ **Упрощение интерфейса**
```java
// Без Facade - сложный код
Amplifier amp = new Amplifier();
DvdPlayer dvd = new DvdPlayer();
Projector projector = new Projector();
// ... множество строк настройки

// С Facade - простой код
homeTheater.watchMovie("Матрица");
```

✅ **Изоляция от сложности подсистемы**
```java
// Клиент не знает о внутренних компонентах
public class Client {
    private OrderFacade orderFacade;
    
    public void placeOrder(Order order) {
        // Не нужно знать об InventoryService, PaymentService и т.д.
        orderFacade.placeOrder(order);
    }
}
```

✅ **Слабая связанность**
```java
// Изменения в подсистеме не влияют на клиентов
class OrderFacade {
    // Можем добавить новый сервис
    private FraudDetectionService fraudService; // Новый!
    
    public boolean placeOrder(Order order) {
        // Клиенты не изменятся
        fraudService.check(order); // Добавили проверку
        // ... остальная логика
    }
}
```

✅ **Единая точка входа**
```java
// Facade как API gateway
class ApiFacade {
    private UserService userService;
    private OrderService orderService;
    private ProductService productService;
    
    // Единый интерфейс для всех операций
    public Response handleRequest(Request request) {
        switch (request.getType()) {
            case "USER": return userService.handle(request);
            case "ORDER": return orderService.handle(request);
            case "PRODUCT": return productService.handle(request);
            default: throw new IllegalArgumentException();
        }
    }
}
```

✅ **Соблюдение принципа минимального знания (Law of Demeter)**
```java
// Плохо - нарушение принципа
customer.getWallet().getMoney().getAmount();

// Хорошо - через Facade
facade.getCustomerBalance(customer);
```

### Недостатки

❌ **Риск превращения в God Object**
```java
// Антипаттерн - слишком много ответственности
class SuperFacade {
    // Слишком много зависимостей!
    private Service1 s1;
    private Service2 s2;
    // ... 20+ сервисов
    
    // Слишком много методов!
    public void operation1() {}
    public void operation2() {}
    // ... 50+ методов
    
    // Проблема: Facade знает слишком много и делает слишком много
}

// Решение: разбить на несколько Facade
class UserFacade { /* операции с пользователями */ }
class OrderFacade { /* операции с заказами */ }
class ProductFacade { /* операции с товарами */ }
```

❌ **Ограничение гибкости**
```java
// Facade может скрывать полезную функциональность
class SimplifiedFacade {
    private ComplexService service;
    
    // Предоставляет только базовую функциональность
    public void simpleOperation() {
        service.operation();
    }
    
    // Но service имеет много других полезных методов!
    // service.advancedOperation();
    // service.customConfiguration();
    // Клиенты не могут их использовать через Facade
}

// Решение: предоставить доступ к подсистеме для продвинутых случаев
class FlexibleFacade {
    private ComplexService service;
    
    public void simpleOperation() {
        service.operation();
    }
    
    // Для продвинутых пользователей
    public ComplexService getUnderlyingService() {
        return service;
    }
}
```

❌ **Дополнительный уровень абстракции**
```java
// Каждый вызов идет через дополнительный слой
public class PerformanceExample {
    public void directCall() {
        // Прямой вызов - быстрее
        service.operation();
    }
    
    public void facadeCall() {
        // Вызов через Facade - дополнительный уровень
        facade.operation();
        // внутри: service.operation();
    }
}
```

❌ **Сложность тестирования Facade**
```java
// Facade зависит от многих компонентов
class OrderFacade {
    private InventoryService inventory;
    private PaymentService payment;
    private ShippingService shipping;
    private NotificationService notification;
    
    // Для тестирования нужно мокировать все зависимости
    @Test
    public void testPlaceOrder() {
        InventoryService mockInventory = mock(InventoryService.class);
        PaymentService mockPayment = mock(PaymentService.class);
        ShippingService mockShipping = mock(ShippingService.class);
        NotificationService mockNotification = mock(NotificationService.class);
        
        OrderFacade facade = new OrderFacade(
            mockInventory, mockPayment, mockShipping, mockNotification);
        
        // Настройка всех моков...
        // Много работы!
    }
}
```

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Facade и когда его использовать?**

*Ответ:* Facade — это структурный паттерн, который предоставляет упрощенный интерфейс к сложной подсистеме. Используется когда:
- Нужно предоставить простой интерфейс к сложной системе классов
- Требуется уменьшить зависимости между клиентом и подсистемой
- Необходимо разбить систему на слои
- Нужна единая точка входа для работы с набором интерфейсов

**2. Приведите примеры Facade из реального мира**

*Ответ:*
- **Стартер автомобиля** — один поворот ключа запускает двигатель, электронику, топливную систему
- **Колл-центр** — один номер телефона для доступа ко всем отделам компании
- **Ресепшн отеля** — единая точка для бронирования, информации, услуг
- **Оператор экскурсий** — организует транспорт, отель, экскурсии через один интерфейс

**3. В чем разница между Facade и Adapter?**

*Ответ:*

| Критерий | Facade | Adapter |
|----------|---------|---------|
| **Цель** | Упростить интерфейс | Согласовать интерфейсы |
| **Количество классов** | Работает с множеством классов | Обычно с одним классом |
| **Новый интерфейс** | Создает новый упрощенный | Адаптирует к существующему |
| **Сложность** | Скрывает сложность | Преобразует интерфейс |

```java
// Facade - упрощает работу с несколькими классами
class HomeTheaterFacade {
    private Amplifier amp;
    private DvdPlayer dvd;
    private Projector projector;
    
    public void watchMovie(String movie) {
        // Координирует несколько объектов
    }
}

// Adapter - адаптирует один интерфейс к другому
class LegacyPrinterAdapter implements ModernPrinter {
    private LegacyPrinter legacyPrinter;
    
    public void print(Document doc) {
        // Преобразует вызов
        legacyPrinter.printDocument(doc.getContent());
    }
}
```

**4. Какова структура паттерна Facade?**

*Ответ:* Основные компоненты:
- **Facade** — предоставляет упрощенный интерфейс к подсистеме
- **Subsystem Classes** — классы подсистемы, реализующие функциональность
- **Client** — использует Facade вместо прямого обращения к подсистеме

Facade делегирует запросы клиента соответствующим объектам подсистемы.

### Продвинутые вопросы

**5. Может ли быть несколько Facade для одной подсистемы?**

*Ответ:* Да, может быть несколько Facade для разных целей:

```java
// Facade для простых операций
class SimpleDatabaseFacade {
    public void saveUser(User user) {
        // Простое сохранение
    }
    
    public User findUser(Long id) {
        // Простой поиск
    }
}

// Facade для сложных операций
class AdvancedDatabaseFacade {
    public void performBatchImport(List<User> users) {
        // Пакетная обработка
    }
    
    public Statistics generateReport(Criteria criteria) {
        // Сложная аналитика
    }
}

// Facade для администраторов
class AdminDatabaseFacade {
    public void backup() {
        // Резервное копирование
    }
    
    public void optimize() {
        // Оптимизация
    }
}
```

**6. Как Facade связан с принципом единственной ответственности (SRP)?**

*Ответ:* Facade может нарушать SRP, если берет на себя слишком много ответственности. Важно следить, чтобы Facade не превратился в God Object:

```java
// Плохо - нарушение SRP
class ApplicationFacade {
    // Слишком много ответственности!
    public void authenticateUser() {}
    public void processOrder() {}
    public void generateReport() {}
    public void sendEmail() {}
    public void backupDatabase() {}
    // ... еще 20 методов
}

// Хорошо - разделение ответственности
class AuthenticationFacade {
    public void authenticateUser() {}
    public void registerUser() {}
}

class OrderFacade {
    public void processOrder() {}
    public void cancelOrder() {}
}

class ReportFacade {
    public void generateReport() {}
    public void exportReport() {}
}
```

**7. Нужно ли скрывать классы подсистемы от клиента?**

*Ответ:* Не обязательно. Facade не запрещает прямой доступ к подсистеме:

```java
// Вариант 1: Facade как единственный способ доступа
// Классы подсистемы package-private
class OrderFacade {
    private InventoryService inventory;  // недоступен снаружи
    private PaymentService payment;      // недоступен снаружи
}

// Вариант 2: Facade + прямой доступ для продвинутых случаев
public class FlexibleFacade {
    private ComplexService service;
    
    // Простой интерфейс
    public void simpleOperation() {
        service.operation();
    }
    
    // Доступ к подсистеме для продвинутых пользователей
    public ComplexService getService() {
        return service;
    }
}
```

**8. Как тестировать Facade?**

*Ответ:* Есть несколько стратегий:

```java
class OrderFacade {
    private InventoryService inventory;
    private PaymentService payment;
    private ShippingService shipping;
    
    // Конструктор для внедрения зависимостей (хорошо для тестов)
    public OrderFacade(InventoryService inventory, 
                      PaymentService payment,
                      ShippingService shipping) {
        this.inventory = inventory;
        this.payment = payment;
        this.shipping = shipping;
    }
    
    public boolean placeOrder(Order order) {
        return inventory.reserve(order) && 
               payment.process(order) && 
               shipping.schedule(order);
    }
}

// Тест с моками
@Test
public void testPlaceOrder() {
    // Создаем моки
    InventoryService mockInventory = mock(InventoryService.class);
    PaymentService mockPayment = mock(PaymentService.class);
    ShippingService mockShipping = mock(ShippingService.class);
    
    // Настраиваем поведение
    when(mockInventory.reserve(any())).thenReturn(true);
    when(mockPayment.process(any())).thenReturn(true);
    when(mockShipping.schedule(any())).thenReturn(true);
    
    // Создаем Facade с моками
    OrderFacade facade = new OrderFacade(mockInventory, mockPayment, mockShipping);
    
    // Тестируем
    boolean result = facade.placeOrder(new Order());
    assertTrue(result);
    
    // Проверяем вызовы
    verify(mockInventory).reserve(any());
    verify(mockPayment).process(any());
    verify(mockShipping).schedule(any());
}
```

**9. В чем разница между Facade и Mediator?**

*Ответ:*

| Критерий | Facade | Mediator |
|----------|---------|----------|
| **Цель** | Упростить интерфейс | Централизовать взаимодействие |
| **Направление связи** | Однонаправленное (клиент → Facade → подсистема) | Двунаправленное (компоненты ↔ Mediator) |
| **Знание о Facade/Mediator** | Подсистема не знает о Facade | Компоненты знают о Mediator |
| **Изменение поведения** | Не изменяет поведение подсистемы | Инкапсулирует логику взаимодействия |

```java
// Facade - упрощает доступ
class HomeTheaterFacade {
    private Amplifier amp;
    private DvdPlayer dvd;
    
    public void watchMovie() {
        // Amp и DVD не знают о Facade
        amp.on();
        dvd.play();
    }
}

// Mediator - координирует взаимодействие
class DialogMediator {
    private Button button;
    private Textbox textbox;
    
    public void componentChanged(Component c) {
        // Компоненты знают о Mediator и уведомляют его
        if (c == textbox) {
            button.setEnabled(!textbox.isEmpty());
        }
    }
}
```

**10. Можно ли использовать Facade в микросервисной архитектуре?**

*Ответ:* Да, Facade активно используется в микросервисах через паттерн **API Gateway**:

```java
// API Gateway - это Facade для микросервисов
@RestController
public class ApiGatewayFacade {
    private UserServiceClient userService;
    private OrderServiceClient orderService;
    private ProductServiceClient productService;
    
    // Единая точка входа для клиентов
    @GetMapping("/api/user/{id}/orders")
    public UserOrdersResponse getUserOrders(@PathVariable Long id) {
        // Координирует вызовы нескольких микросервисов
        User user = userService.getUser(id);
        List<Order> orders = orderService.getUserOrders(id);
        
        // Агрегирует данные
        return new UserOrdersResponse(user, orders);
    }
    
    // Gateway скрывает:
    // - Детали микросервисов
    // - Различные протоколы
    // - Аутентификацию/авторизацию
    // - Rate limiting
    // - Балансировку нагрузки
}
```

Преимущества API Gateway как Facade:
- Единая точка входа для всех клиентов
- Упрощение клиентского кода
- Централизованная аутентификация и авторизация
- Агрегация данных из нескольких сервисов
- Трансформация протоколов (REST → gRPC, например)

---

[← Назад к разделу Структурные паттерны](README.md)

