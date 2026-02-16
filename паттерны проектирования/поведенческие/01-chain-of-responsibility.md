# Chain of Responsibility (Цепочка обязанностей)

Chain of Responsibility — поведенческий паттерн проектирования, который позволяет передавать запросы последовательно по цепочке обработчиков. Каждый последующий обработчик решает, может ли он обработать запрос сам и стоит ли передавать запрос дальше по цепи.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Пример 1: Система обработки тикетов поддержки](#пример-1-система-обработки-тикетов-поддержки)
   - [Пример 2: Цепочка аутентификации и авторизации](#пример-2-цепочка-аутентификации-и-авторизации)
   - [Пример 3: Логирование с уровнями важности](#пример-3-логирование-с-уровнями-важности)
   - [Пример 4: Система согласования покупок](#пример-4-система-согласования-покупок)
5. [Примеры из JDK](#примеры-из-jdk)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Chain of Responsibility используется когда:
- Существует более одного обработчика для запроса
- Обработчик не известен заранее
- Набор обработчиков должен задаваться динамически
- Не требуется гарантия обработки запроса

**Типичные примеры использования:**
- Системы обработки запросов (HTTP middleware, фильтры сервлетов)
- Системы логирования с разными уровнями
- Цепочки валидации и авторизации
- Обработка событий в GUI
- Системы согласования и эскалации

## Проблема, которую решает

### Проблема: Жесткая связанность обработчиков

```java
public class SupportTicketProcessor {
    public void processTicket(Ticket ticket) {
        // Жесткая привязка к конкретной логике обработки
        if (ticket.getPriority() == Priority.LOW) {
            System.out.println("Level 1 Support handles: " + ticket.getDescription());
        } else if (ticket.getPriority() == Priority.MEDIUM) {
            System.out.println("Level 2 Support handles: " + ticket.getDescription());
        } else if (ticket.getPriority() == Priority.HIGH) {
            System.out.println("Manager handles: " + ticket.getDescription());
        } else {
            System.out.println("Director handles: " + ticket.getDescription());
        }
        // Добавление нового уровня требует изменения этого метода!
    }
}
```

**Проблемы:**
- Нарушение принципа открытости/закрытости (Open/Closed Principle)
- Сложная логика с множественными условиями
- Невозможно динамически изменить последовательность обработки
- Жесткая зависимость от всех обработчиков

### Решение: Chain of Responsibility

Создать цепочку обработчиков, где каждый обработчик может обработать запрос или передать его следующему.

## Структура паттерна

```java
// Абстрактный обработчик
abstract class Handler {
    protected Handler nextHandler;
    
    public void setNext(Handler handler) {
        this.nextHandler = handler;
    }
    
    public abstract void handleRequest(Request request);
    
    protected void passToNext(Request request) {
        if (nextHandler != null) {
            nextHandler.handleRequest(request);
        }
    }
}

// Конкретный обработчик
class ConcreteHandler extends Handler {
    @Override
    public void handleRequest(Request request) {
        if (canHandle(request)) {
            // Обработать запрос
            process(request);
        } else {
            // Передать следующему
            passToNext(request);
        }
    }
    
    private boolean canHandle(Request request) {
        // Логика проверки
        return true;
    }
    
    private void process(Request request) {
        // Логика обработки
    }
}
```

## Реализация

### Пример 1: Система обработки тикетов поддержки

Реалистичная система технической поддержки с уровнями эскалации.

```java
// Запрос - тикет поддержки
class SupportTicket {
    private final String id;
    private final String description;
    private final Priority priority;
    private final String customerEmail;
    private boolean resolved;
    
    public SupportTicket(String id, String description, Priority priority, String customerEmail) {
        this.id = id;
        this.description = description;
        this.priority = priority;
        this.customerEmail = customerEmail;
        this.resolved = false;
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public String getCustomerEmail() { return customerEmail; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}

enum Priority {
    LOW, MEDIUM, HIGH, CRITICAL
}

// Абстрактный обработчик
abstract class SupportHandler {
    protected SupportHandler nextHandler;
    protected String handlerName;
    
    public SupportHandler(String handlerName) {
        this.handlerName = handlerName;
    }
    
    public void setNext(SupportHandler handler) {
        this.nextHandler = handler;
    }
    
    public void handleTicket(SupportTicket ticket) {
        if (canHandle(ticket)) {
            process(ticket);
            ticket.setResolved(true);
        } else {
            escalate(ticket);
        }
    }
    
    protected abstract boolean canHandle(SupportTicket ticket);
    
    protected abstract void process(SupportTicket ticket);
    
    protected void escalate(SupportTicket ticket) {
        if (nextHandler != null) {
            System.out.println(handlerName + " escalating ticket " + ticket.getId() 
                + " to next level");
            nextHandler.handleTicket(ticket);
        } else {
            System.out.println("ALERT: Ticket " + ticket.getId() 
                + " could not be handled by anyone!");
        }
    }
}

// Уровень 1: Базовая поддержка
class Level1Support extends SupportHandler {
    public Level1Support() {
        super("Level 1 Support");
    }
    
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getPriority() == Priority.LOW;
    }
    
    @Override
    protected void process(SupportTicket ticket) {
        System.out.println(handlerName + " handling ticket " + ticket.getId());
        System.out.println("  Issue: " + ticket.getDescription());
        System.out.println("  Solution: Sent standard troubleshooting guide");
        System.out.println("  Email sent to: " + ticket.getCustomerEmail());
    }
}

// Уровень 2: Продвинутая поддержка
class Level2Support extends SupportHandler {
    public Level2Support() {
        super("Level 2 Support");
    }
    
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getPriority() == Priority.MEDIUM;
    }
    
    @Override
    protected void process(SupportTicket ticket) {
        System.out.println(handlerName + " handling ticket " + ticket.getId());
        System.out.println("  Issue: " + ticket.getDescription());
        System.out.println("  Solution: Remote session scheduled, detailed analysis performed");
        System.out.println("  Follow-up email sent to: " + ticket.getCustomerEmail());
    }
}

// Менеджер: Критические проблемы
class SupportManager extends SupportHandler {
    public SupportManager() {
        super("Support Manager");
    }
    
    @Override
    protected boolean canHandle(SupportTicket ticket) {
        return ticket.getPriority() == Priority.HIGH || 
               ticket.getPriority() == Priority.CRITICAL;
    }
    
    @Override
    protected void process(SupportTicket ticket) {
        System.out.println(handlerName + " handling ticket " + ticket.getId());
        System.out.println("  Issue: " + ticket.getDescription());
        
        if (ticket.getPriority() == Priority.CRITICAL) {
            System.out.println("  CRITICAL: Immediate response required!");
            System.out.println("  Solution: Direct call to customer, emergency patch deployed");
        } else {
            System.out.println("  Solution: Personal consultation, custom solution provided");
        }
        
        System.out.println("  Personal response sent to: " + ticket.getCustomerEmail());
    }
}

// Использование
class SupportSystem {
    public static void main(String[] args) {
        // Создаем цепочку обработчиков
        SupportHandler level1 = new Level1Support();
        SupportHandler level2 = new Level2Support();
        SupportHandler manager = new SupportManager();
        
        level1.setNext(level2);
        level2.setNext(manager);
        
        // Обрабатываем тикеты
        SupportTicket ticket1 = new SupportTicket(
            "T001", 
            "Cannot login to account", 
            Priority.LOW, 
            "user@example.com"
        );
        
        SupportTicket ticket2 = new SupportTicket(
            "T002", 
            "System performance degradation", 
            Priority.MEDIUM, 
            "admin@company.com"
        );
        
        SupportTicket ticket3 = new SupportTicket(
            "T003", 
            "Complete service outage for all users!", 
            Priority.CRITICAL, 
            "cto@company.com"
        );
        
        System.out.println("=== Processing Support Tickets ===\n");
        
        level1.handleTicket(ticket1);
        System.out.println();
        
        level1.handleTicket(ticket2);
        System.out.println();
        
        level1.handleTicket(ticket3);
    }
}
```

### Пример 2: Цепочка аутентификации и авторизации

Система безопасности с последовательными проверками.

```java
// HTTP запрос
class HttpRequest {
    private final String username;
    private final String password;
    private final String token;
    private final String ipAddress;
    private final String requestedResource;
    private int requestCount;
    
    public HttpRequest(String username, String password, String token, 
                      String ipAddress, String requestedResource) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.ipAddress = ipAddress;
        this.requestedResource = requestedResource;
        this.requestCount = 0;
    }
    
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getToken() { return token; }
    public String getIpAddress() { return ipAddress; }
    public String getRequestedResource() { return requestedResource; }
    public int getRequestCount() { return requestCount; }
    public void incrementRequestCount() { requestCount++; }
}

// Абстрактный обработчик безопасности
abstract class SecurityHandler {
    protected SecurityHandler next;
    
    public void setNext(SecurityHandler handler) {
        this.next = handler;
    }
    
    public boolean handle(HttpRequest request) {
        if (!check(request)) {
            return false;
        }
        
        if (next != null) {
            return next.handle(request);
        }
        
        return true;
    }
    
    protected abstract boolean check(HttpRequest request);
}

// Проверка аутентификации
class AuthenticationHandler extends SecurityHandler {
    private static final Map<String, String> USERS = Map.of(
        "admin", "admin123",
        "user", "user123"
    );
    
    @Override
    protected boolean check(HttpRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        
        if (username == null || password == null) {
            System.out.println("❌ Authentication failed: Missing credentials");
            return false;
        }
        
        if (!USERS.containsKey(username) || !USERS.get(username).equals(password)) {
            System.out.println("❌ Authentication failed: Invalid credentials for user: " + username);
            return false;
        }
        
        System.out.println("✓ Authentication passed for user: " + username);
        return true;
    }
}

// Проверка авторизации
class AuthorizationHandler extends SecurityHandler {
    private static final Map<String, List<String>> PERMISSIONS = Map.of(
        "admin", List.of("/admin", "/users", "/reports", "/public"),
        "user", List.of("/users", "/public")
    );
    
    @Override
    protected boolean check(HttpRequest request) {
        String username = request.getUsername();
        String resource = request.getRequestedResource();
        
        List<String> userPermissions = PERMISSIONS.getOrDefault(username, List.of());
        
        boolean hasAccess = userPermissions.stream()
            .anyMatch(permission -> resource.startsWith(permission));
        
        if (!hasAccess) {
            System.out.println("❌ Authorization failed: User " + username 
                + " has no access to " + resource);
            return false;
        }
        
        System.out.println("✓ Authorization passed for " + username + " to access " + resource);
        return true;
    }
}

// Валидация токена
class TokenValidationHandler extends SecurityHandler {
    private static final String VALID_TOKEN = "secure-token-12345";
    
    @Override
    protected boolean check(HttpRequest request) {
        String token = request.getToken();
        
        if (token == null || token.isEmpty()) {
            System.out.println("❌ Token validation failed: Missing token");
            return false;
        }
        
        if (!token.equals(VALID_TOKEN)) {
            System.out.println("❌ Token validation failed: Invalid token");
            return false;
        }
        
        System.out.println("✓ Token validation passed");
        return true;
    }
}

// Rate limiting
class RateLimitHandler extends SecurityHandler {
    private static final int MAX_REQUESTS = 10;
    private final Map<String, Integer> requestCounts = new HashMap<>();
    
    @Override
    protected boolean check(HttpRequest request) {
        String ipAddress = request.getIpAddress();
        int count = requestCounts.getOrDefault(ipAddress, 0) + 1;
        requestCounts.put(ipAddress, count);
        
        if (count > MAX_REQUESTS) {
            System.out.println("❌ Rate limit exceeded for IP: " + ipAddress);
            return false;
        }
        
        System.out.println("✓ Rate limit check passed (" + count + "/" + MAX_REQUESTS + ")");
        return true;
    }
}

// Использование
class SecurityChainDemo {
    public static void main(String[] args) {
        // Создаем цепочку безопасности
        SecurityHandler authentication = new AuthenticationHandler();
        SecurityHandler authorization = new AuthorizationHandler();
        SecurityHandler tokenValidation = new TokenValidationHandler();
        SecurityHandler rateLimit = new RateLimitHandler();
        
        authentication.setNext(tokenValidation);
        tokenValidation.setNext(authorization);
        authorization.setNext(rateLimit);
        
        // Тест 1: Успешный запрос
        System.out.println("=== Test 1: Valid Admin Request ===");
        HttpRequest request1 = new HttpRequest(
            "admin", 
            "admin123", 
            "secure-token-12345", 
            "192.168.1.1", 
            "/admin/users"
        );
        boolean result1 = authentication.handle(request1);
        System.out.println("Result: " + (result1 ? "✅ ACCESS GRANTED" : "❌ ACCESS DENIED"));
        
        System.out.println("\n=== Test 2: Unauthorized User ===");
        HttpRequest request2 = new HttpRequest(
            "user", 
            "user123", 
            "secure-token-12345", 
            "192.168.1.2", 
            "/admin/users"
        );
        boolean result2 = authentication.handle(request2);
        System.out.println("Result: " + (result2 ? "✅ ACCESS GRANTED" : "❌ ACCESS DENIED"));
        
        System.out.println("\n=== Test 3: Invalid Token ===");
        HttpRequest request3 = new HttpRequest(
            "admin", 
            "admin123", 
            "invalid-token", 
            "192.168.1.1", 
            "/admin/users"
        );
        boolean result3 = authentication.handle(request3);
        System.out.println("Result: " + (result3 ? "✅ ACCESS GRANTED" : "❌ ACCESS DENIED"));
    }
}
```

### Пример 3: Логирование с уровнями важности

Система логирования с разными обработчиками для разных уровней.

```java
// Уровни логирования
enum LogLevel {
    DEBUG(1), INFO(2), WARNING(3), ERROR(4), FATAL(5);
    
    private final int priority;
    
    LogLevel(int priority) {
        this.priority = priority;
    }
    
    public int getPriority() {
        return priority;
    }
}

// Сообщение лога
class LogMessage {
    private final LogLevel level;
    private final String message;
    private final String timestamp;
    private final String source;
    
    public LogMessage(LogLevel level, String message, String source) {
        this.level = level;
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now().toString();
        this.source = source;
    }
    
    public LogLevel getLevel() { return level; }
    public String getMessage() { return message; }
    public String getTimestamp() { return timestamp; }
    public String getSource() { return source; }
    
    @Override
    public String toString() {
        return String.format("[%s] [%s] [%s] %s", 
            timestamp, level, source, message);
    }
}

// Абстрактный логгер
abstract class Logger {
    protected LogLevel level;
    protected Logger nextLogger;
    
    public Logger(LogLevel level) {
        this.level = level;
    }
    
    public void setNext(Logger logger) {
        this.nextLogger = logger;
    }
    
    public void log(LogMessage message) {
        if (message.getLevel().getPriority() >= level.getPriority()) {
            write(message);
        }
        
        if (nextLogger != null) {
            nextLogger.log(message);
        }
    }
    
    protected abstract void write(LogMessage message);
}

// Консольный логгер (для всех уровней)
class ConsoleLogger extends Logger {
    public ConsoleLogger(LogLevel level) {
        super(level);
    }
    
    @Override
    protected void write(LogMessage message) {
        System.out.println("CONSOLE: " + message);
    }
}

// Файловый логгер (для WARNING и выше)
class FileLogger extends Logger {
    private final String filename;
    
    public FileLogger(LogLevel level, String filename) {
        super(level);
        this.filename = filename;
    }
    
    @Override
    protected void write(LogMessage message) {
        System.out.println("FILE (" + filename + "): " + message);
        // Реальная запись в файл:
        // Files.write(Paths.get(filename), message.toString().getBytes(), 
        //             StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}

// Email логгер (только для ERROR и FATAL)
class EmailLogger extends Logger {
    private final String emailAddress;
    
    public EmailLogger(LogLevel level, String emailAddress) {
        super(level);
        this.emailAddress = emailAddress;
    }
    
    @Override
    protected void write(LogMessage message) {
        System.out.println("EMAIL to " + emailAddress + ": " + message);
        // Реальная отправка email:
        // emailService.send(emailAddress, "Error Alert", message.toString());
    }
}

// SMS логгер (только для FATAL)
class SmsLogger extends Logger {
    private final String phoneNumber;
    
    public SmsLogger(LogLevel level, String phoneNumber) {
        super(level);
        this.phoneNumber = phoneNumber;
    }
    
    @Override
    protected void write(LogMessage message) {
        System.out.println("SMS to " + phoneNumber + ": " + message.getMessage());
        // Реальная отправка SMS:
        // smsService.send(phoneNumber, "CRITICAL: " + message.getMessage());
    }
}

// Использование
class LoggingChainDemo {
    public static void main(String[] args) {
        // Создаем цепочку логгеров
        Logger consoleLogger = new ConsoleLogger(LogLevel.DEBUG);
        Logger fileLogger = new FileLogger(LogLevel.WARNING, "app.log");
        Logger emailLogger = new EmailLogger(LogLevel.ERROR, "admin@company.com");
        Logger smsLogger = new SmsLogger(LogLevel.FATAL, "+1-234-567-8900");
        
        consoleLogger.setNext(fileLogger);
        fileLogger.setNext(emailLogger);
        emailLogger.setNext(smsLogger);
        
        // Логируем сообщения разных уровней
        System.out.println("=== Logging DEBUG message ===");
        consoleLogger.log(new LogMessage(LogLevel.DEBUG, 
            "User entered the login page", "LoginController"));
        
        System.out.println("\n=== Logging INFO message ===");
        consoleLogger.log(new LogMessage(LogLevel.INFO, 
            "User authenticated successfully", "AuthService"));
        
        System.out.println("\n=== Logging WARNING message ===");
        consoleLogger.log(new LogMessage(LogLevel.WARNING, 
            "Database connection pool is 80% full", "DatabasePool"));
        
        System.out.println("\n=== Logging ERROR message ===");
        consoleLogger.log(new LogMessage(LogLevel.ERROR, 
            "Failed to connect to payment gateway", "PaymentService"));
        
        System.out.println("\n=== Logging FATAL message ===");
        consoleLogger.log(new LogMessage(LogLevel.FATAL, 
            "Database server is unreachable! System shutting down", "DatabaseConnection"));
    }
}
```

### Пример 4: Система согласования покупок

Workflow согласования покупок в зависимости от суммы.

```java
// Заявка на покупку
class PurchaseRequest {
    private final String id;
    private final String description;
    private final double amount;
    private final String requester;
    private boolean approved;
    private String approvedBy;
    
    public PurchaseRequest(String id, String description, double amount, String requester) {
        this.id = id;
        this.description = description;
        this.amount = amount;
        this.requester = requester;
        this.approved = false;
    }
    
    public String getId() { return id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getRequester() { return requester; }
    public boolean isApproved() { return approved; }
    public String getApprovedBy() { return approvedBy; }
    
    public void approve(String approver) {
        this.approved = true;
        this.approvedBy = approver;
    }
}

// Абстрактный утверждающий
abstract class Approver {
    protected Approver nextApprover;
    protected String name;
    protected double approvalLimit;
    
    public Approver(String name, double approvalLimit) {
        this.name = name;
        this.approvalLimit = approvalLimit;
    }
    
    public void setNext(Approver approver) {
        this.nextApprover = approver;
    }
    
    public void processRequest(PurchaseRequest request) {
        if (canApprove(request)) {
            approve(request);
        } else {
            escalate(request);
        }
    }
    
    protected boolean canApprove(PurchaseRequest request) {
        return request.getAmount() <= approvalLimit;
    }
    
    protected void approve(PurchaseRequest request) {
        System.out.println("\n✅ " + name + " approved request " + request.getId());
        System.out.println("   Description: " + request.getDescription());
        System.out.println("   Amount: $" + String.format("%.2f", request.getAmount()));
        System.out.println("   Requested by: " + request.getRequester());
        request.approve(name);
    }
    
    protected void escalate(PurchaseRequest request) {
        if (nextApprover != null) {
            System.out.println("⬆️  " + name + " escalating request " + request.getId() 
                + " (amount $" + String.format("%.2f", request.getAmount()) 
                + " exceeds limit $" + String.format("%.2f", approvalLimit) + ")");
            nextApprover.processRequest(request);
        } else {
            System.out.println("\n❌ Request " + request.getId() 
                + " rejected: Amount $" + String.format("%.2f", request.getAmount()) 
                + " exceeds all approval limits!");
        }
    }
}

// Супервайзер (до $1000)
class Supervisor extends Approver {
    public Supervisor(String name) {
        super(name + " (Supervisor)", 1000.0);
    }
}

// Менеджер (до $5000)
class Manager extends Approver {
    public Manager(String name) {
        super(name + " (Manager)", 5000.0);
    }
}

// Директор (до $25000)
class Director extends Approver {
    public Director(String name) {
        super(name + " (Director)", 25000.0);
    }
}

// Вице-президент (до $100000)
class VicePresident extends Approver {
    public VicePresident(String name) {
        super(name + " (VP)", 100000.0);
    }
}

// CEO (любая сумма)
class CEO extends Approver {
    public CEO(String name) {
        super(name + " (CEO)", Double.MAX_VALUE);
    }
    
    @Override
    protected void approve(PurchaseRequest request) {
        System.out.println("\n🔥 " + name + " personally approved request " + request.getId());
        System.out.println("   Description: " + request.getDescription());
        System.out.println("   Amount: $" + String.format("%.2f", request.getAmount()));
        System.out.println("   Requested by: " + request.getRequester());
        System.out.println("   Note: High-value purchase requires board notification");
        request.approve(name);
    }
}

// Использование
class PurchaseApprovalDemo {
    public static void main(String[] args) {
        // Создаем цепочку утверждения
        Approver supervisor = new Supervisor("John Smith");
        Approver manager = new Manager("Jane Doe");
        Approver director = new Director("Bob Johnson");
        Approver vp = new VicePresident("Alice Williams");
        Approver ceo = new CEO("Richard Brown");
        
        supervisor.setNext(manager);
        manager.setNext(director);
        director.setNext(vp);
        vp.setNext(ceo);
        
        // Создаем заявки на покупку
        PurchaseRequest[] requests = {
            new PurchaseRequest("PR001", "Office supplies", 500.0, "Marketing Team"),
            new PurchaseRequest("PR002", "New laptops (5 units)", 7500.0, "IT Department"),
            new PurchaseRequest("PR003", "Conference sponsorship", 15000.0, "Sales Team"),
            new PurchaseRequest("PR004", "Company cars (3 units)", 75000.0, "Executive Team"),
            new PurchaseRequest("PR005", "Small office renovation", 250.0, "Facility Manager")
        };
        
        System.out.println("=== Purchase Approval Workflow ===");
        
        for (PurchaseRequest request : requests) {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Processing request: " + request.getId());
            supervisor.processRequest(request);
        }
        
        // Отчет
        System.out.println("\n\n=== Approval Summary ===");
        for (PurchaseRequest request : requests) {
            System.out.println(request.getId() + ": " + 
                (request.isApproved() ? "✅ Approved by " + request.getApprovedBy() 
                                      : "❌ Rejected"));
        }
    }
}
```

## Примеры из JDK

### 1. Java Servlet Filters

```java
// Servlet фильтры образуют цепочку обработки запросов
public class LoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) throws IOException, ServletException {
        // Обработка до
        System.out.println("Request received");
        
        // Передача следующему в цепочке
        chain.doFilter(request, response);
        
        // Обработка после
        System.out.println("Response sent");
    }
}
```

### 2. Exception Handling

```java
// try-catch блоки образуют цепочку обработки исключений
try {
    // код
} catch (FileNotFoundException e) {
    // обработка FileNotFoundException
} catch (IOException e) {
    // обработка IOException
} catch (Exception e) {
    // обработка всех остальных исключений
}
```

### 3. java.util.logging

```java
// Логгеры образуют иерархию с передачей сообщений вверх
Logger logger = Logger.getLogger("com.example.app");
logger.setLevel(Level.INFO);

// Добавляем несколько обработчиков
ConsoleHandler consoleHandler = new ConsoleHandler();
FileHandler fileHandler = new FileHandler("app.log");

logger.addHandler(consoleHandler);
logger.addHandler(fileHandler);

// Сообщение будет обработано всеми подходящими обработчиками
logger.info("Application started");
```

### 4. ClassLoader Delegation

```java
// ClassLoader использует цепочку для загрузки классов
// Bootstrap ClassLoader → Extension ClassLoader → Application ClassLoader

ClassLoader classLoader = MyClass.class.getClassLoader();
// Сначала проверяет родительский ClassLoader, затем свой
```

## Преимущества и недостатки

### Преимущества

| Преимущество | Описание |
|--------------|----------|
| **Слабая связанность** | Отправитель и получатель не зависят друг от друга |
| **Гибкость** | Можно динамически изменять цепочку обработчиков |
| **Единственная ответственность** | Каждый обработчик отвечает только за свою логику |
| **Расширяемость** | Легко добавлять новые обработчики без изменения существующих |
| **Управление обработкой** | Контроль над порядком обработки запросов |

### Недостатки

| Недостаток | Описание |
|------------|----------|
| **Нет гарантии обработки** | Запрос может остаться необработанным, если цепочка настроена неправильно |
| **Производительность** | Длинная цепочка может негативно влиять на производительность |
| **Отладка** | Сложнее отследить путь выполнения через цепочку |
| **Неявное поведение** | Не всегда очевидно, какой обработчик обработает запрос |

## Вопросы на собеседовании

### 1. Что такое паттерн Chain of Responsibility?

**Ответ:** Chain of Responsibility — поведенческий паттерн проектирования, который позволяет передавать запросы последовательно по цепочке обработчиков. Каждый обработчик решает, может ли он обработать запрос, и стоит ли передавать запрос дальше по цепи. Это позволяет избежать жесткой привязки отправителя запроса к его получателю и дает возможность нескольким объектам обработать запрос.

### 2. В чем отличие Chain of Responsibility от Decorator?

**Ответ:** Хотя оба паттерна используют композицию и цепочку объектов, у них разные цели:
- **Chain of Responsibility**: Один из обработчиков обрабатывает запрос ИЛИ передает дальше. Обработка может остановиться на любом звене.
- **Decorator**: Каждый декоратор добавляет свою функциональность И всегда вызывает следующий. Все декораторы выполняются.

Пример: В Chain of Responsibility логгер уровня ERROR не будет вызван для DEBUG сообщения. В Decorator каждый декоратор добавит свою функциональность.

### 3. Когда следует использовать паттерн Chain of Responsibility?

**Ответ:** Паттерн следует использовать когда:
- Существует более одного обработчика для запроса
- Обработчик не известен заранее и должен определяться динамически
- Набор обработчиков должен задаваться динамически
- Порядок обработки важен
- Хотите избежать жесткой привязки отправителя к получателю

Типичные примеры: системы фильтрации, middleware, обработка событий, валидация, логирование.

### 4. Какие недостатки у паттерна Chain of Responsibility?

**Ответ:** Основные недостатки:
- **Отсутствие гарантии обработки**: Если цепочка настроена неправильно, запрос может остаться необработанным
- **Производительность**: Запрос может пройти через всю цепочку, что влияет на производительность
- **Сложность отладки**: Трудно отследить, какой обработчик обработал запрос
- **Скрытые зависимости**: Порядок обработчиков в цепочке может быть критичным, но не очевиден из кода

### 5. Как реализовать обязательную обработку запроса?

**Ответ:** Есть несколько подходов:

```java
// 1. Обработчик по умолчанию в конце цепочки
class DefaultHandler extends Handler {
    @Override
    public void handleRequest(Request request) {
        System.out.println("Default handling for: " + request);
    }
}

// 2. Проверка результата
boolean handled = firstHandler.handle(request);
if (!handled) {
    throw new IllegalStateException("Request was not handled");
}

// 3. Использовать Optional
Optional<Response> result = handler.handle(request);
Response response = result.orElseThrow(() -> 
    new IllegalStateException("No handler processed the request"));
```

### 6. В чем разница между Chain of Responsibility и Command?

**Ответ:**
- **Chain of Responsibility**: Фокусируется на передаче запроса через цепочку обработчиков, пока один из них не обработает. Обработчики связаны друг с другом.
- **Command**: Инкапсулирует запрос как объект, позволяя параметризовать клиентов с различными запросами, ставить запросы в очередь, логировать их. Команды независимы друг от друга.

Chain of Responsibility — о делегировании обработки. Command — об инкапсуляции действий.

### 7. Как паттерн реализован в Java Servlet Filters?

**Ответ:** Servlet фильтры — классический пример Chain of Responsibility:

```java
public void doFilter(ServletRequest request, ServletResponse response, 
                    FilterChain chain) throws IOException, ServletException {
    // Обработка до
    if (shouldContinue(request)) {
        // Передача следующему фильтру в цепочке
        chain.doFilter(request, response);
    }
    // Обработка после
}
```

Особенности:
- `FilterChain` представляет остальную часть цепочки
- Каждый фильтр решает, передавать ли запрос дальше
- Можно модифицировать запрос/ответ до и после
- Порядок фильтров определяется в web.xml или аннотациях

### 8. Можно ли комбинировать Chain of Responsibility с другими паттернами?

**Ответ:** Да, часто комбинируется с:

**Composite**: Обработчики могут быть композитными объектами
```java
class CompositeHandler extends Handler {
    private List<Handler> handlers = new ArrayList<>();
    
    @Override
    public void handle(Request request) {
        for (Handler handler : handlers) {
            if (handler.canHandle(request)) {
                handler.handle(request);
                return;
            }
        }
        passToNext(request);
    }
}
```

**Factory/Builder**: Для создания цепочки обработчиков
```java
class HandlerChainBuilder {
    public Handler buildAuthChain() {
        Handler auth = new AuthenticationHandler();
        Handler authz = new AuthorizationHandler();
        Handler validation = new ValidationHandler();
        
        auth.setNext(authz);
        authz.setNext(validation);
        
        return auth;
    }
}
```

**Strategy**: Обработчики могут использовать разные стратегии

### 9. Как тестировать код с Chain of Responsibility?

**Ответ:** Стратегии тестирования:

```java
@Test
public void testHandlerProcessesRequest() {
    Handler handler = new Level1Support();
    SupportTicket ticket = new SupportTicket("T001", "Issue", Priority.LOW, "user@test.com");
    
    handler.handleTicket(ticket);
    
    assertTrue(ticket.isResolved());
}

@Test
public void testChainEscalation() {
    Handler level1 = new Level1Support();
    Handler level2 = new Level2Support();
    level1.setNext(level2);
    
    SupportTicket highPriorityTicket = new SupportTicket("T002", "Critical", Priority.MEDIUM, "user@test.com");
    
    level1.handleTicket(highPriorityTicket);
    
    // Проверяем, что обработал level2
    assertTrue(highPriorityTicket.isResolved());
}

@Test
public void testUnhandledRequest() {
    Handler handler = new Level1Support();
    // Не добавляем следующий обработчик
    
    SupportTicket ticket = new SupportTicket("T003", "High priority", Priority.CRITICAL, "user@test.com");
    
    handler.handleTicket(ticket);
    
    assertFalse(ticket.isResolved()); // Должен остаться необработанным
}
```

### 10. Как реализовать цепочку с возможностью множественной обработки?

**Ответ:** Иногда требуется, чтобы несколько обработчиков обработали один запрос:

```java
abstract class MultiHandler {
    protected MultiHandler next;
    
    public void setNext(MultiHandler handler) {
        this.next = handler;
    }
    
    public void handle(Request request) {
        // Всегда обрабатываем, если можем
        if (canHandle(request)) {
            process(request);
        }
        
        // И всегда передаем дальше (отличие от классической реализации)
        if (next != null) {
            next.handle(request);
        }
    }
    
    protected abstract boolean canHandle(Request request);
    protected abstract void process(Request request);
}

// Пример: все логгеры обрабатывают сообщение
class ConsoleLogger extends MultiHandler {
    @Override
    protected boolean canHandle(Request request) {
        return true; // Логирует все
    }
    
    @Override
    protected void process(Request request) {
        System.out.println("Console: " + request);
    }
}

class FileLogger extends MultiHandler {
    @Override
    protected boolean canHandle(Request request) {
        return request.getPriority() >= Priority.WARNING;
    }
    
    @Override
    protected void process(Request request) {
        // Записать в файл
    }
}
```

Это похоже на паттерн Observer, но с упорядоченной обработкой и возможностью прервать цепочку.
