# Проксирование бинов в Spring

## Содержание

1. [Введение в проксирование](#введение-в-проксирование)
2. [Зачем Spring использует прокси](#зачем-spring-использует-прокси)
3. [Типы прокси в Spring](#типы-прокси-в-spring)
   - [JDK Dynamic Proxy](#jdk-dynamic-proxy)
   - [CGLIB Proxy](#cglib-proxy)
   - [Сравнение подходов](#сравнение-подходов)
4. [Проксирование в Spring AOP](#проксирование-в-spring-aop)
   - [Создание прокси для аспектов](#создание-прокси-для-аспектов)
   - [Конфигурация типа прокси](#конфигурация-типа-прокси)
5. [Проксирование для Scoped бинов](#проксирование-для-scoped-бинов)
6. [Ленивая инициализация через прокси](#ленивая-инициализация-через-прокси)
7. [Транзакционные прокси](#транзакционные-прокси)
8. [Ограничения и особенности прокси](#ограничения-и-особенности-прокси)
   - [Self-invocation problem](#self-invocation-problem)
   - [Final методы и классы](#final-методы-и-классы)
   - [Private методы](#private-методы)
9. [Производительность прокси](#производительность-прокси)
10. [Отладка и диагностика прокси](#отладка-и-диагностика-прокси)
11. [Практические примеры](#практические-примеры)
12. [Вопросы на собеседовании](#вопросы-на-собеседовании)
13. [Дополнительные материалы](#дополнительные-материалы)

## Введение в проксирование

**Прокси** (Proxy) — это объект-посредник, который оборачивает целевой объект и перехватывает вызовы его методов. Прокси позволяет добавлять дополнительное поведение (логирование, транзакции, безопасность, кэширование) до или после выполнения оригинального метода, не изменяя код самого объекта.

В Spring Framework проксирование — это ключевой механизм реализации таких функций как:
- **AOP (Aspect-Oriented Programming)** — добавление сквозной функциональности
- **Транзакционное управление** — `@Transactional`
- **Безопасность** — `@PreAuthorize`, `@Secured`
- **Кэширование** — `@Cacheable`
- **Асинхронное выполнение** — `@Async`
- **Scoped бины** — request, session, custom scopes
- **Ленивая инициализация** — `@Lazy`

> **Важно**: Проксирование работает только для бинов, управляемых Spring-контейнером. Если вы создаёте объект через `new`, прокси не будет создан, и дополнительная функциональность работать не будет.

## Зачем Spring использует прокси

Spring использует прокси для реализации принципа **разделения ответственности** (Separation of Concerns). Вместо того чтобы смешивать бизнес-логику с технической (логирование, транзакции, безопасность), Spring выносит техническую логику в отдельные компоненты (аспекты, interceptors) и применяет их через прокси.

**Преимущества подхода на основе прокси:**
- **Декларативность** — добавление функциональности через аннотации, без изменения кода
- **Переиспользуемость** — одни и те же аспекты применяются к разным бинам
- **Тестируемость** — можно тестировать бизнес-логику отдельно от инфраструктурных concerns
- **Гибкость** — легко включать/выключать функциональность через конфигурацию

**Альтернативы прокси:**
- **Compile-time weaving** (AspectJ) — внедрение кода на этапе компиляции
- **Load-time weaving** (AspectJ LTW) — модификация байт-кода при загрузке классов
- **Ручная реализация** — явное добавление кода (template method, decorator pattern)

Spring AOP выбрал прокси-подход как компромисс между простотой использования и производительностью.

## Типы прокси в Spring

Spring поддерживает два механизма создания прокси: **JDK Dynamic Proxy** и **CGLIB Proxy**. Выбор механизма зависит от того, реализует ли целевой класс интерфейсы.

### JDK Dynamic Proxy

**JDK Dynamic Proxy** — встроенный механизм Java (пакет `java.lang.reflect`), который создаёт прокси для интерфейсов.

**Принцип работы:**
1. Целевой класс должен реализовывать хотя бы один интерфейс
2. Spring создаёт прокси-объект, реализующий те же интерфейсы
3. При вызове метода через прокси, вызов перехватывается `InvocationHandler`
4. `InvocationHandler` может выполнить дополнительную логику и делегировать вызов целевому объекту

**Пример:**

```java
// Интерфейс
public interface UserService {
    User findById(Long id);
    void save(User user);
}

// Реализация
@Service
public class UserServiceImpl implements UserService {
    @Override
    public User findById(Long id) {
        return repository.findById(id);
    }
    
    @Override
    public void save(User user) {
        repository.save(user);
    }
}

// Spring создаст JDK Dynamic Proxy для UserService
// Прокси будет реализовывать интерфейс UserService
```

**Структура прокси:**
```
UserService proxy = Proxy.newProxyInstance(
    classLoader,
    new Class[] { UserService.class },
    invocationHandler
);
```

**Преимущества JDK Dynamic Proxy:**
- Встроен в JDK, не требует дополнительных библиотек
- Производительность немного выше CGLIB
- Поддерживается всеми версиями Java

**Недостатки:**
- Требует наличия интерфейса
- Не может проксировать классы напрямую

### CGLIB Proxy

**CGLIB (Code Generation Library)** — библиотека для генерации байт-кода, которая создаёт прокси через наследование.

**Принцип работы:**
1. CGLIB создаёт подкласс целевого класса во время выполнения
2. Все не-final методы переопределяются в подклассе
3. В переопределённых методах добавляется логика перехвата и делегирования

**Пример:**

```java
// Класс без интерфейса
@Service
public class OrderService {
    
    public void createOrder(Order order) {
        // бизнес-логика
    }
    
    public Order findOrder(Long id) {
        return repository.findById(id);
    }
}

// Spring создаст CGLIB прокси:
// OrderService$$EnhancerBySpringCGLIB$$a1b2c3d4 extends OrderService
```

**Структура прокси:**
```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(OrderService.class);
enhancer.setCallback(methodInterceptor);
OrderService proxy = (OrderService) enhancer.create();
```

**Преимущества CGLIB:**
- Не требует интерфейсов
- Может проксировать конкретные классы

**Недостатки:**
- Требует библиотеку CGLIB (включена в Spring)
- Немного медленнее JDK Dynamic Proxy
- Не работает с `final` классами и методами
- Требует конструктор без параметров (или параметризованный конструктор + callback)

### Сравнение подходов

| Характеристика | JDK Dynamic Proxy | CGLIB Proxy |
|----------------|-------------------|-------------|
| **Требования** | Наличие интерфейса | Не-final класс |
| **Механизм** | Реализация интерфейса | Наследование класса |
| **Производительность** | Немного быстрее | Немного медленнее |
| **Зависимости** | Встроен в JDK | Требует CGLIB (в Spring) |
| **Final методы** | N/A | Не проксируются |
| **Private методы** | N/A | Не проксируются |
| **Конструктор** | Не вызывается | Вызывается дважды |

**Рекомендации:**
- Предпочитайте программирование к интерфейсам — используйте JDK Dynamic Proxy по умолчанию
- Для legacy-кода без интерфейсов используйте CGLIB
- В Spring Boot CGLIB используется по умолчанию с версии 4.0 для AOP

## Проксирование в Spring AOP

### Создание прокси для аспектов

Spring AOP автоматически создаёт прокси для бинов, на которые применяются аспекты. Процесс происходит на этапе `BeanPostProcessor.postProcessAfterInitialization`:

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Around("@annotation(com.example.Loggable)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long executionTime = System.currentTimeMillis() - start;
        
        System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
        return result;
    }
}

@Service
public class PaymentService {
    
    @Loggable
    public void processPayment(Payment payment) {
        // бизнес-логика
    }
}
```

**Что происходит внутри:**
1. Spring сканирует бины и находит методы, соответствующие pointcut выражениям
2. Если найдены совпадения, создаётся прокси:
   - JDK Dynamic Proxy, если класс реализует интерфейсы
   - CGLIB, если класс не реализует интерфейсы
3. Прокси оборачивает оригинальный бин
4. При вызове метода через прокси:
   - Выполняется advice (Before, Around, After)
   - Вызывается оригинальный метод
   - Выполняется оставшаяся часть advice

### Конфигурация типа прокси

По умолчанию Spring выбирает тип прокси автоматически. Можно явно указать использование CGLIB:

**Через аннотацию:**
```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AopConfig {
    // proxyTargetClass = true -> всегда использовать CGLIB
    // proxyTargetClass = false -> JDK Dynamic Proxy, если есть интерфейс
}
```

**Через application.properties (Spring Boot):**
```properties
spring.aop.proxy-target-class=true
```

**Глобальная конфигурация:**
```xml
<aop:aspectj-autoproxy proxy-target-class="true"/>
```

**Когда использовать `proxyTargetClass=true`:**
- Нужно проксировать методы, объявленные в классе, но не в интерфейсе
- Работа с legacy-кодом без интерфейсов
- Требуется инжектировать прокси по типу класса, а не интерфейса

## Проксирование для Scoped бинов

При внедрении бинов с узкой областью видимости (request, session, custom) в singleton-бины, Spring создаёт **scoped proxy**.

**Проблема без прокси:**
```java
@Service // singleton scope
public class OrderService {
    
    @Autowired
    private ShoppingCart cart; // session scope
    
    // Проблема: OrderService создаётся один раз при старте приложения,
    // и в него внедряется один экземпляр ShoppingCart.
    // Для всех пользователей будет использоваться одна корзина!
}
```

**Решение через scoped proxy:**
```java
@Component
@Scope(value = WebApplicationContext.SCOPE_SESSION, 
       proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ShoppingCart {
    private List<Item> items = new ArrayList<>();
    // ...
}
```

**Как работает scoped proxy:**
1. Spring создаёт CGLIB прокси для `ShoppingCart`
2. Прокси внедряется в singleton `OrderService`
3. При каждом вызове метода прокси:
   - Определяет текущую HTTP-сессию
   - Получает актуальный экземпляр `ShoppingCart` для этой сессии
   - Делегирует вызов этому экземпляру

**Типы proxyMode:**
- `ScopedProxyMode.NO` — прокси не создаётся (по умолчанию)
- `ScopedProxyMode.INTERFACES` — JDK Dynamic Proxy (требует интерфейс)
- `ScopedProxyMode.TARGET_CLASS` — CGLIB (работает с классами)

## Ленивая инициализация через прокси

Аннотация `@Lazy` может создавать прокси для отложенной инициализации бинов.

**Проблема eager инициализации:**
```java
@Service
public class EmailService {
    
    @Autowired
    private HeavyResourceService heavyService; // создаётся сразу при старте
    
    public void sendEmail(String email) {
        // heavyService используется редко
    }
}
```

**Решение через @Lazy:**
```java
@Service
public class EmailService {
    
    @Autowired
    @Lazy
    private HeavyResourceService heavyService; // прокси вместо реального объекта
    
    public void sendEmail(String email) {
        heavyService.doWork(); // реальный бин создаётся только здесь
    }
}
```

**Как работает @Lazy прокси:**
1. При внедрении создаётся лёгкий прокси-объект
2. При первом вызове любого метода прокси инициализирует реальный бин
3. Все последующие вызовы идут напрямую к реальному бину

**Применение:**
- Оптимизация времени старта приложения
- Бины с тяжёлой инициализацией (подключения к БД, файловые ресурсы)
- Опциональные зависимости

## Транзакционные прокси

Аннотация `@Transactional` работает через прокси, которые управляют транзакциями.

**Пример:**
```java
@Service
public class BankService {
    
    @Transactional
    public void transfer(Account from, Account to, BigDecimal amount) {
        from.withdraw(amount);
        to.deposit(amount);
        // Spring автоматически создаёт транзакцию перед методом
        // и коммитит её после успешного выполнения
        // или откатывает при исключении
    }
}
```

**Как работает транзакционный прокси:**
1. Spring создаёт прокси для `BankService`
2. При вызове `transfer()` через прокси:
   - `TransactionInterceptor` открывает транзакцию
   - Вызывается оригинальный метод
   - При нормальном завершении — commit
   - При unchecked exception — rollback
   - Закрытие транзакции

**Типичная цепочка обработки:**
```
Клиент -> Прокси -> TransactionInterceptor -> Оригинальный метод
         <-       <-                        <-
```

> **Важно**: `@Transactional` не работает при self-invocation (вызове метода внутри того же класса), так как вызов идёт напрямую, минуя прокси.

## Ограничения и особенности прокси

### Self-invocation problem

**Проблема внутренних вызовов** — одна из самых частых ошибок при работе с прокси в Spring.

**Проблемный код:**
```java
@Service
public class UserService {
    
    @Transactional
    public void updateUser(User user) {
        // ...
    }
    
    public void updateMultipleUsers(List<User> users) {
        for (User user : users) {
            updateUser(user); // Внутренний вызов! Прокси НЕ работает!
        }
    }
}
```

**Почему не работает:**
Когда вы вызываете `updateMultipleUsers()` извне, вызов идёт через прокси. Но внутри метода `updateMultipleUsers`, вызов `updateUser()` происходит через `this`, то есть напрямую к реальному объекту, минуя прокси. Транзакция для каждого `updateUser()` не создаётся.

**Решения:**

**1. Вынос в отдельный бин:**
```java
@Service
public class UserService {
    
    @Autowired
    private UserTransactionalService transactionalService;
    
    public void updateMultipleUsers(List<User> users) {
        for (User user : users) {
            transactionalService.updateUser(user); // Вызов через прокси!
        }
    }
}

@Service
class UserTransactionalService {
    @Transactional
    public void updateUser(User user) {
        // ...
    }
}
```

**2. Self-injection (Spring 4.3+):**
```java
@Service
public class UserService {
    
    @Autowired
    private UserService self; // Spring внедрит прокси, а не this
    
    public void updateMultipleUsers(List<User> users) {
        for (User user : users) {
            self.updateUser(user); // Вызов через прокси
        }
    }
    
    @Transactional
    public void updateUser(User user) {
        // ...
    }
}
```

**3. Получение прокси через ApplicationContext:**
```java
@Service
public class UserService {
    
    @Autowired
    private ApplicationContext context;
    
    public void updateMultipleUsers(List<User> users) {
        UserService proxy = context.getBean(UserService.class);
        for (User user : users) {
            proxy.updateUser(user);
        }
    }
    
    @Transactional
    public void updateUser(User user) {
        // ...
    }
}
```

**4. AspectJ weaving (рекомендуется для сложных случаев):**
```java
@EnableAspectJAutoProxy(mode = AdviceMode.ASPECTJ)
@EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
```

AspectJ модифицирует байт-код класса, поэтому внутренние вызовы тоже работают корректно.

### Final методы и классы

CGLIB не может проксировать `final` классы и методы, так как использует наследование.

**Проблемный код:**
```java
@Service
public final class PaymentService { // final класс!
    
    public void processPayment(Payment payment) {
        // ...
    }
}

// Или

@Service
public class PaymentService {
    
    @Transactional
    public final void processPayment(Payment payment) { // final метод!
        // ...
    }
}
```

**Результат:**
- Для `final` класса: Spring не сможет создать прокси, выбросит исключение
- Для `final` метода: Прокси создастся, но `@Transactional` не будет работать для этого метода

**Решения:**
1. Уберите `final` модификатор
2. Используйте JDK Dynamic Proxy через интерфейс:
```java
public interface PaymentService {
    void processPayment(Payment payment);
}

@Service
public final class PaymentServiceImpl implements PaymentService {
    @Override
    @Transactional
    public void processPayment(Payment payment) {
        // JDK Dynamic Proxy будет работать
    }
}
```
3. Используйте AspectJ weaving

### Private методы

Ни JDK Dynamic Proxy, ни CGLIB не могут перехватывать вызовы `private` методов.

**Не работает:**
```java
@Service
public class UserService {
    
    @Transactional // НЕ РАБОТАЕТ!
    private void updateUserInternal(User user) {
        // ...
    }
}
```

**Решение:** Сделайте метод `public` или `protected`.

## Производительность прокси

**Накладные расходы на создание прокси:**
- JDK Dynamic Proxy: ~1-2 мс на создание одного прокси
- CGLIB: ~3-5 мс на создание одного прокси

Создание прокси происходит один раз при старте приложения, поэтому влияние на общую производительность минимально.

**Накладные расходы на вызов методов:**
- Прямой вызов: ~1 ns
- JDK Dynamic Proxy: ~10-20 ns (overhead ~10-20x)
- CGLIB: ~15-30 ns (overhead ~15-30x)

Для большинства приложений это незаметно, так как бизнес-логика занимает гораздо больше времени.

**Бенчмарк пример:**
```java
// Прямой вызов: 1,000,000 ops/ms
// JDK Proxy:     50,000-100,000 ops/ms
// CGLIB Proxy:   30,000-60,000 ops/ms
```

**Оптимизация:**
1. Избегайте ненужных прокси:
   ```java
   @EnableAspectJAutoProxy(proxyTargetClass = false) // JDK по умолчанию
   ```

2. Используйте `@Lazy` для тяжёлых бинов

3. Для критичных по производительности участков рассмотрите AspectJ compile-time weaving

4. Кэшируйте результаты вместо создания множества прокси

## Отладка и диагностика прокси

### Определение типа прокси

**Проверка через класс:**
```java
@Autowired
private UserService userService;

public void checkProxy() {
    Class<?> clazz = userService.getClass();
    System.out.println("Class: " + clazz.getName());
    
    if (clazz.getName().contains("$$EnhancerBySpringCGLIB$$")) {
        System.out.println("CGLIB proxy");
    } else if (Proxy.isProxyClass(clazz)) {
        System.out.println("JDK Dynamic Proxy");
    } else {
        System.out.println("Not a proxy");
    }
}
```

**Получение оригинального объекта:**
```java
import org.springframework.aop.framework.AopProxyUtils;

Object target = AopProxyUtils.getSingletonTarget(proxy);
```

### Логирование создания прокси

**application.properties:**
```properties
logging.level.org.springframework.aop=DEBUG
logging.level.org.springframework.beans.factory.support=DEBUG
```

**Вывод в логе:**
```
Creating JDK dynamic proxy for [com.example.service.UserService]
Creating CGLIB proxy for [com.example.service.OrderService]
```

### Просмотр цепочки advisors

```java
import org.springframework.aop.framework.Advised;

if (proxy instanceof Advised) {
    Advised advised = (Advised) proxy;
    for (Advisor advisor : advised.getAdvisors()) {
        System.out.println("Advisor: " + advisor);
    }
}
```

### Типичные ошибки и их диагностика

**1. BeanNotOfRequiredTypeException:**
```
Bean named 'userService' is expected to be of type 'com.example.UserServiceImpl' 
but was actually of type 'com.sun.proxy.$Proxy42'
```

**Причина:** JDK Dynamic Proxy создал прокси для интерфейса, но вы пытаетесь инжектировать по типу класса.

**Решение:**
```java
// Было:
@Autowired
private UserServiceImpl userService; // Ошибка!

// Стало:
@Autowired
private UserService userService; // OK - инжект по интерфейсу
```

**2. IllegalArgumentException: object is not an instance of declaring class:**

**Причина:** Попытка вызвать метод через Reflection на прокси-объекте.

**Решение:** Используйте `AopUtils.getTargetClass()` или `AopProxyUtils.getSingletonTarget()`.

**3. @Transactional не работает:**

**Чек-лист:**
- [ ] Метод `public`? (private/protected/package не работают с прокси)
- [ ] Вызов извне класса? (self-invocation не работает)
- [ ] Метод не `final`? (CGLIB не переопределяет final)
- [ ] `@EnableTransactionManagement` включён?
- [ ] Правильный PlatformTransactionManager сконфигурирован?

## Практические примеры

### Пример 1: Кастомный аннотационный прокси

Создадим собственную аннотацию для измерения времени выполнения:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timed {
}

@Aspect
@Component
public class TimingAspect {
    
    @Around("@annotation(Timed)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } finally {
            long executionTime = System.currentTimeMillis() - start;
            System.out.println(joinPoint.getSignature() + " executed in " + executionTime + "ms");
        }
    }
}

@Service
public class DataService {
    
    @Timed
    public List<Data> loadData() {
        // бизнес-логика
        return data;
    }
}
```

### Пример 2: Условное создание прокси

```java
@Configuration
public class ConditionalProxyConfig {
    
    @Bean
    @ConditionalOnProperty(name = "monitoring.enabled", havingValue = "true")
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    public MonitoringService monitoringService() {
        return new MonitoringServiceImpl();
    }
    
    @Bean
    @ConditionalOnProperty(name = "monitoring.enabled", havingValue = "false", matchIfMissing = true)
    public MonitoringService noOpMonitoringService() {
        return new NoOpMonitoringService(); // Без прокси
    }
}
```

### Пример 3: Программное создание прокси

```java
import org.springframework.aop.framework.ProxyFactory;

public class ManualProxyExample {
    
    public static void main(String[] args) {
        UserService target = new UserServiceImpl();
        
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.addAdvice(new MethodInterceptor() {
            @Override
            public Object invoke(MethodInvocation invocation) throws Throwable {
                System.out.println("Before: " + invocation.getMethod().getName());
                Object result = invocation.proceed();
                System.out.println("After: " + invocation.getMethod().getName());
                return result;
            }
        });
        
        UserService proxy = (UserService) proxyFactory.getProxy();
        proxy.findUser(1L); // Логирование сработает
    }
}
```

## Вопросы на собеседовании

1. **Какие типы прокси использует Spring и в чём их разница?**

   *Ответ:* Spring использует два типа прокси:
   - **JDK Dynamic Proxy** — для классов, реализующих интерфейсы. Создаёт прокси, реализующий те же интерфейсы через `java.lang.reflect.Proxy`.
   - **CGLIB Proxy** — для классов без интерфейсов. Создаёт подкласс целевого класса с переопределёнными методами.
   
   JDK Proxy немного быстрее, но требует интерфейс. CGLIB гибче, но не работает с `final` классами/методами.

2. **Почему @Transactional не работает при вызове метода внутри того же класса?**

   *Ответ:* Это проблема **self-invocation**. Spring создаёт прокси для класса, который перехватывает внешние вызовы. Но когда метод вызывается внутри класса через `this`, вызов идёт напрямую к реальному объекту, минуя прокси. Решения: вынос в отдельный бин, self-injection, AspectJ weaving.

3. **Как работает scoped proxy и зачем он нужен?**

   *Ответ:* Scoped proxy нужен для внедрения бинов с узкой областью видимости (request, session) в singleton-бины. Прокси создаётся один раз и внедряется в singleton. При каждом вызове метода прокси определяет текущий scope (например, HTTP-сессию) и делегирует вызов актуальному экземпляру для этого scope.

4. **Можно ли проксировать final класс или final метод?**

   *Ответ:* CGLIB не может проксировать `final` классы и методы, так как использует наследование. Для `final` класса Spring выбросит исключение. Для `final` метода прокси создастся, но аннотации типа `@Transactional` не будут работать для этого метода. Решение: использовать интерфейсы (JDK Proxy) или AspectJ.

5. **В чём разница между proxy-target-class=true и false?**

   *Ответ:* 
   - `proxy-target-class=false` (по умолчанию до Spring 4): Spring использует JDK Dynamic Proxy, если класс реализует интерфейс, иначе CGLIB.
   - `proxy-target-class=true`: Spring всегда использует CGLIB, даже если есть интерфейсы.
   
   В Spring Boot по умолчанию `true` с версии 2.0.

6. **Как определить, является ли объект прокси?**

   *Ответ:* Несколько способов:
   ```java
   // Для JDK Proxy
   if (Proxy.isProxyClass(obj.getClass())) { ... }
   
   // Для CGLIB
   if (obj.getClass().getName().contains("$$EnhancerBySpringCGLIB$$")) { ... }
   
   // Универсальный способ (Spring AOP)
   if (AopUtils.isAopProxy(obj)) { ... }
   if (AopUtils.isCglibProxy(obj)) { ... }
   if (AopUtils.isJdkDynamicProxy(obj)) { ... }
   ```

7. **Какие есть альтернативы Spring AOP прокси?**

   *Ответ:* Основные альтернативы:
   - **AspectJ compile-time weaving** — внедрение кода на этапе компиляции через AspectJ компилятор
   - **AspectJ load-time weaving (LTW)** — модификация байт-кода при загрузке классов через Java agent
   - **ByteBuddy** — библиотека для генерации и модификации байт-кода
   
   AspectJ позволяет обходить ограничения прокси (self-invocation, final методы, private методы).

8. **Влияет ли проксирование на производительность?**

   *Ответ:* Да, но незначительно. Создание прокси добавляет 1-5 мс на старте приложения. Вызов через прокси медленнее в ~10-30 раз (10-30 наносекунд vs 1 наносекунда), но для большинства приложений это незаметно, так как бизнес-логика (БД, сеть, вычисления) занимает гораздо больше времени. Критичным может быть для высоконагруженных систем с микросервисными вызовами — тогда стоит рассмотреть compile-time weaving.

9. **Что такое @Lazy прокси и как он работает?**

   *Ответ:* `@Lazy` прокси — это лёгкий прокси-объект, который откладывает создание реального бина до первого вызова его метода. При внедрении создаётся прокси, а реальный бин инициализируется только при первом использовании. Это оптимизирует время старта приложения для тяжёлых бинов.

10. **Как получить оригинальный объект из прокси?**

    *Ответ:* Использовать утилиты Spring AOP:
    ```java
    import org.springframework.aop.framework.AopProxyUtils;
    import org.springframework.aop.support.AopUtils;
    
    if (AopUtils.isAopProxy(proxy)) {
        Object target = AopProxyUtils.getSingletonTarget(proxy);
    }
    
    // Или через Advised
    if (proxy instanceof Advised) {
        Object target = ((Advised) proxy).getTargetSource().getTarget();
    }
    ```

## Дополнительные материалы

- [Spring AOP Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop) — официальная документация по AOP и прокси
- [Spring Framework Reference: Proxying Mechanisms](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop-proxying) — детальное описание механизмов прокси
- [CGLIB GitHub](https://github.com/cglib/cglib) — документация и исходники CGLIB
- [AspectJ Documentation](https://www.eclipse.org/aspectj/doc/released/progguide/index.html) — руководство по AspectJ для альтернатив прокси
- Книга "Spring in Action" by Craig Walls — глава про AOP и прокси
- Статья [Baeldung: Introduction to Spring AOP](https://www.baeldung.com/spring-aop) — практические примеры работы с прокси
- Статья [DZone: Understanding Spring Proxies](https://dzone.com/articles/understanding-spring-proxies) — глубокое погружение в механизмы
