# Proxy (Заместитель)

Proxy — структурный паттерн проектирования, который предоставляет объект-заместитель вместо реального объекта. Прокси контролирует доступ к оригинальному объекту, позволяя выполнять что-то до или после передачи запроса оригиналу.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Типы прокси](#типы-прокси)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Динамические прокси в Java](#динамические-прокси-в-java)
6. [Примеры использования](#примеры-использования)
7. [Proxy vs Decorator](#proxy-vs-decorator)
8. [Преимущества и недостатки](#преимущества-и-недостатки)
9. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Proxy используется когда:
- Нужен контроль доступа к объекту
- Требуется отложенная инициализация (lazy loading)
- Необходимо логирование обращений к объекту
- Нужна работа с удаленными объектами
- Требуется кеширование результатов

**Типичные примеры использования:**
- Ленивая загрузка тяжелых объектов
- Контроль доступа и права
- Логирование и мониторинг
- Кеширование данных
- Удаленные вызовы (RMI, REST)

## Типы прокси

### 1. Виртуальный прокси (Virtual Proxy)
Откладывает создание дорогостоящего объекта до момента реального использования.

### 2. Защищающий прокси (Protection Proxy)
Контролирует доступ к объекту на основе прав доступа.

### 3. Удаленный прокси (Remote Proxy)
Представляет объект, находящийся в другом адресном пространстве.

### 4. Кеширующий прокси (Caching Proxy)
Кеширует результаты операций для повторного использования.

### 5. Умная ссылка (Smart Reference)
Выполняет дополнительные действия при обращении к объекту.

## Структура паттерна

```java
// Общий интерфейс
interface Image {
    void display();
}

// Реальный объект
class RealImage implements Image {
    private String filename;
    
    public RealImage(String filename) {
        this.filename = filename;
        loadFromDisk();
    }
    
    private void loadFromDisk() {
        System.out.println("Loading image: " + filename);
        // Долгая операция загрузки
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Override
    public void display() {
        System.out.println("Displaying image: " + filename);
    }
}

// Прокси
class ImageProxy implements Image {
    private RealImage realImage;
    private String filename;
    
    public ImageProxy(String filename) {
        this.filename = filename;
    }
    
    @Override
    public void display() {
        // Ленивая инициализация - создаем объект только при необходимости
        if (realImage == null) {
            realImage = new RealImage(filename);
        }
        realImage.display();
    }
}

// Использование
Image image1 = new ImageProxy("photo1.jpg");  // Быстро - изображение не загружается
Image image2 = new ImageProxy("photo2.jpg");  // Быстро
System.out.println("Images created");

image1.display();  // Загружается и отображается
image1.display();  // Только отображается (уже загружено)
```

## Реализация

### Пример 1: Защищающий прокси (Access Control)

```java
// Интерфейс документа
interface Document {
    void read();
    void write(String content);
}

// Реальный документ
class RealDocument implements Document {
    private String content;
    
    public RealDocument(String content) {
        this.content = content;
    }
    
    @Override
    public void read() {
        System.out.println("Reading document: " + content);
    }
    
    @Override
    public void write(String content) {
        this.content = content;
        System.out.println("Writing to document: " + content);
    }
}

// Прокси с контролем доступа
class ProtectedDocumentProxy implements Document {
    private RealDocument realDocument;
    private String userRole;
    
    public ProtectedDocumentProxy(String content, String userRole) {
        this.realDocument = new RealDocument(content);
        this.userRole = userRole;
    }
    
    @Override
    public void read() {
        // Чтение доступно всем
        realDocument.read();
    }
    
    @Override
    public void write(String content) {
        // Запись только для админов
        if ("ADMIN".equals(userRole)) {
            realDocument.write(content);
        } else {
            System.out.println("Access denied: Only admins can write");
        }
    }
}

// Использование
Document adminDoc = new ProtectedDocumentProxy("Confidential data", "ADMIN");
adminDoc.read();                    // OK
adminDoc.write("Updated data");     // OK

Document userDoc = new ProtectedDocumentProxy("Public data", "USER");
userDoc.read();                     // OK
userDoc.write("Try to update");     // Access denied
```

### Пример 2: Кеширующий прокси

```java
// Интерфейс сервиса
interface DataService {
    String getData(String key);
}

// Реальный сервис (медленный)
class DatabaseService implements DataService {
    @Override
    public String getData(String key) {
        System.out.println("Fetching from database: " + key);
        // Имитация медленного запроса к БД
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Data for " + key;
    }
}

// Кеширующий прокси
class CachingServiceProxy implements DataService {
    private DatabaseService databaseService;
    private Map<String, String> cache;
    
    public CachingServiceProxy() {
        this.databaseService = new DatabaseService();
        this.cache = new HashMap<>();
    }
    
    @Override
    public String getData(String key) {
        // Проверяем кеш
        if (cache.containsKey(key)) {
            System.out.println("Returning from cache: " + key);
            return cache.get(key);
        }
        
        // Запрашиваем из БД и кешируем
        String data = databaseService.getData(key);
        cache.put(key, data);
        return data;
    }
    
    public void clearCache() {
        cache.clear();
    }
}

// Использование
DataService service = new CachingServiceProxy();

String data1 = service.getData("user123");  // Запрос к БД (медленно)
String data2 = service.getData("user123");  // Из кеша (быстро)
String data3 = service.getData("user456");  // Запрос к БД (медленно)
```

### Пример 3: Логирующий прокси

```java
// Интерфейс калькулятора
interface Calculator {
    int add(int a, int b);
    int subtract(int a, int b);
    int multiply(int a, int b);
}

// Реальная реализация
class SimpleCalculator implements Calculator {
    @Override
    public int add(int a, int b) {
        return a + b;
    }
    
    @Override
    public int subtract(int a, int b) {
        return a - b;
    }
    
    @Override
    public int multiply(int a, int b) {
        return a * b;
    }
}

// Логирующий прокси
class LoggingCalculatorProxy implements Calculator {
    private Calculator calculator;
    
    public LoggingCalculatorProxy(Calculator calculator) {
        this.calculator = calculator;
    }
    
    @Override
    public int add(int a, int b) {
        System.out.println("Calling add(" + a + ", " + b + ")");
        long start = System.currentTimeMillis();
        
        int result = calculator.add(a, b);
        
        long duration = System.currentTimeMillis() - start;
        System.out.println("Result: " + result + ", Duration: " + duration + "ms");
        
        return result;
    }
    
    @Override
    public int subtract(int a, int b) {
        System.out.println("Calling subtract(" + a + ", " + b + ")");
        int result = calculator.subtract(a, b);
        System.out.println("Result: " + result);
        return result;
    }
    
    @Override
    public int multiply(int a, int b) {
        System.out.println("Calling multiply(" + a + ", " + b + ")");
        int result = calculator.multiply(a, b);
        System.out.println("Result: " + result);
        return result;
    }
}

// Использование
Calculator calc = new LoggingCalculatorProxy(new SimpleCalculator());
int sum = calc.add(5, 3);
int diff = calc.subtract(10, 4);
```

### Пример 4: Умная ссылка (Reference Counting)

```java
// Тяжелый ресурс
class HeavyResource {
    public void process() {
        System.out.println("Processing with heavy resource");
    }
    
    public void cleanup() {
        System.out.println("Cleaning up heavy resource");
    }
}

// Прокси с подсчетом ссылок
class SmartReferenceProxy {
    private static HeavyResource sharedResource;
    private static int referenceCount = 0;
    
    public SmartReferenceProxy() {
        if (sharedResource == null) {
            System.out.println("Creating heavy resource");
            sharedResource = new HeavyResource();
        }
        referenceCount++;
        System.out.println("Reference count: " + referenceCount);
    }
    
    public void process() {
        sharedResource.process();
    }
    
    public void release() {
        referenceCount--;
        System.out.println("Reference count: " + referenceCount);
        
        if (referenceCount == 0) {
            System.out.println("No more references, cleaning up");
            sharedResource.cleanup();
            sharedResource = null;
        }
    }
}

// Использование
SmartReferenceProxy proxy1 = new SmartReferenceProxy();  // Создает ресурс
SmartReferenceProxy proxy2 = new SmartReferenceProxy();  // Использует существующий

proxy1.process();
proxy2.process();

proxy1.release();  // Уменьшает счетчик
proxy2.release();  // Освобождает ресурс
```

## Динамические прокси в Java

### JDK Dynamic Proxy

```java
// Интерфейс сервиса
interface UserService {
    User getUser(Long id);
    void saveUser(User user);
}

// Реальная реализация
class UserServiceImpl implements UserService {
    @Override
    public User getUser(Long id) {
        System.out.println("Getting user: " + id);
        return new User(id, "User" + id);
    }
    
    @Override
    public void saveUser(User user) {
        System.out.println("Saving user: " + user.getName());
    }
}

// InvocationHandler для динамического прокси
class LoggingInvocationHandler implements InvocationHandler {
    private Object target;
    
    public LoggingInvocationHandler(Object target) {
        this.target = target;
    }
    
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("Before method: " + method.getName());
        
        Object result = method.invoke(target, args);
        
        System.out.println("After method: " + method.getName());
        
        return result;
    }
}

// Создание динамического прокси
UserService realService = new UserServiceImpl();

UserService proxyService = (UserService) Proxy.newProxyInstance(
    UserService.class.getClassLoader(),
    new Class[] { UserService.class },
    new LoggingInvocationHandler(realService)
);

// Использование
User user = proxyService.getUser(1L);
proxyService.saveUser(user);
```

### CGLIB Proxy (для классов без интерфейсов)

```java
// Класс без интерфейса
class ConcreteService {
    public String getData(String key) {
        return "Data for " + key;
    }
}

// CGLIB MethodInterceptor
class CglibLoggingInterceptor implements MethodInterceptor {
    @Override
    public Object intercept(Object obj, Method method, Object[] args, 
                          MethodProxy proxy) throws Throwable {
        System.out.println("Before: " + method.getName());
        Object result = proxy.invokeSuper(obj, args);
        System.out.println("After: " + method.getName());
        return result;
    }
}

// Создание CGLIB прокси
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(ConcreteService.class);
enhancer.setCallback(new CglibLoggingInterceptor());

ConcreteService proxy = (ConcreteService) enhancer.create();
String data = proxy.getData("test");
```

## Примеры использования

### Spring AOP

```java
// Spring использует прокси для реализации AOP
@Service
public class UserService {
    
    @Transactional  // Spring создает прокси для управления транзакциями
    public void saveUser(User user) {
        // Бизнес-логика
    }
    
    @Cacheable("users")  // Spring создает прокси для кеширования
    public User getUser(Long id) {
        // Бизнес-логика
        return userRepository.findById(id);
    }
}

// Spring создает динамический прокси, который:
// - Начинает транзакцию перед методом
// - Вызывает метод
// - Коммитит или откатывает транзакцию
```

### Hibernate Lazy Loading

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany(fetch = FetchType.LAZY)  // Hibernate использует прокси
    private List<Order> orders;
    
    // При обращении к orders Hibernate создает прокси,
    // который загружает данные только при реальном использовании
}
```

### Java RMI

```java
// Удаленный интерфейс
public interface RemoteService extends Remote {
    String getData(String key) throws RemoteException;
}

// RMI автоматически создает прокси для удаленных вызовов
RemoteService service = (RemoteService) Naming.lookup("//localhost/Service");
String data = service.getData("test");  // Прокси делает сетевой вызов
```

## Proxy vs Decorator

| Аспект | Proxy | Decorator |
|--------|-------|-----------|
| **Цель** | Контроль доступа к объекту | Добавление функциональности |
| **Создание объекта** | Прокси может создавать объект | Декоратор получает готовый объект |
| **Знание о реальном объекте** | Прокси знает о реальном объекте | Декоратор работает с интерфейсом |
| **Множественность** | Обычно один прокси | Можно комбинировать декораторы |
| **Видимость клиенту** | Прозрачен для клиента | Клиент явно создает декораторы |

```java
// Proxy - клиент не знает о реальном объекте
Image image = new ImageProxy("photo.jpg");  // Прокси создает RealImage

// Decorator - клиент явно оборачивает
InputStream stream = new BufferedInputStream(
                        new FileInputStream("file.txt"));
```

## Преимущества и недостатки

### Преимущества

✅ **Контроль доступа**
- Безопасное управление доступом к объекту

✅ **Ленивая инициализация**
- Откладывает создание дорогих объектов

✅ **Логирование и мониторинг**
- Прозрачное добавление логирования

✅ **Кеширование**
- Повышение производительности через кеш

✅ **Удаленный доступ**
- Работа с удаленными объектами как с локальными

### Недостатки

❌ **Усложнение кода**
- Дополнительный уровень абстракции

❌ **Производительность**
- Дополнительные вызовы методов

❌ **Задержка ответа**
- При ленивой инициализации первый вызов медленнее

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Proxy?**

*Ответ:* Proxy — это структурный паттерн, который предоставляет объект-заместитель, контролирующий доступ к другому объекту. Прокси имеет тот же интерфейс, что и реальный объект.

**2. Какие типы прокси существуют?**

*Ответ:*
- **Виртуальный** — ленивая инициализация
- **Защищающий** — контроль доступа
- **Удаленный** — представление удаленного объекта
- **Кеширующий** — кеширование результатов
- **Умная ссылка** — дополнительные действия при обращении

**3. В чем разница между Proxy и Decorator?**

*Ответ:*
- **Proxy** контролирует доступ, **Decorator** добавляет функциональность
- Proxy может создавать объект, Decorator получает готовый
- Proxy обычно один, Decorator можно комбинировать

**4. Приведите примеры Proxy из Java**

*Ответ:*
- JDK Dynamic Proxy
- Hibernate lazy loading
- Spring AOP (@Transactional, @Cacheable)
- Java RMI

### Продвинутые вопросы

**5. Как работает JDK Dynamic Proxy?**

*Ответ:* JDK Dynamic Proxy:
- Работает только с интерфейсами
- Создает прокси во время выполнения через `Proxy.newProxyInstance()`
- Использует `InvocationHandler` для перехвата вызовов
- Reflection для вызова методов реального объекта

```java
UserService proxy = (UserService) Proxy.newProxyInstance(
    classLoader,
    new Class[] { UserService.class },
    invocationHandler
);
```

**6. В чем разница между JDK Dynamic Proxy и CGLIB?**

*Ответ:*
- **JDK Proxy**: работает только с интерфейсами, использует reflection
- **CGLIB**: работает с классами, создает подклассы, использует bytecode generation
- JDK быстрее при создании, CGLIB быстрее при вызове
- Spring использует JDK для интерфейсов, CGLIB для классов

**7. Как реализовать thread-safe ленивую инициализацию в прокси?**

*Ответ:*
```java
class LazyProxy implements Service {
    private volatile Service realService;
    
    @Override
    public void execute() {
        if (realService == null) {
            synchronized (this) {
                if (realService == null) {
                    realService = new RealService();
                }
            }
        }
        realService.execute();
    }
}
```

**8. Какие проблемы могут возникнуть с Proxy в Spring?**

*Ответ:*
- **Self-invocation**: внутренние вызовы не проходят через прокси
- **Final методы**: нельзя переопределить (CGLIB)
- **Private методы**: не проксируются
- **this vs прокси**: `this` ссылается на реальный объект, не на прокси

```java
@Service
class MyService {
    @Transactional
    public void outer() {
        this.inner();  // НЕ проходит через прокси!
    }
    
    @Transactional
    public void inner() {
        // Транзакция НЕ работает при вызове через this
    }
}
```

**9. Как Proxy используется в кешировании?**

*Ответ:* Прокси перехватывает вызовы, проверяет кеш и возвращает закешированное значение:
```java
@Cacheable("users")
public User getUser(Long id) {
    // Spring создает прокси:
    // 1. Проверяет кеш по ключу "users::id"
    // 2. Если есть - возвращает из кеша
    // 3. Если нет - вызывает метод и кеширует результат
}
```

**10. Можно ли комбинировать несколько прокси?**

*Ответ:* Да, можно создать цепочку прокси:
```java
// Создаем цепочку: Логирование -> Кеширование -> Реальный сервис
DataService service = new RealService();
service = new CachingProxy(service);
service = new LoggingProxy(service);

// Вызов пройдет через: Логирование -> Кеширование -> Реальный сервис
service.getData("key");
```

---

[← Назад к разделу Структурные паттерны](README.md)
