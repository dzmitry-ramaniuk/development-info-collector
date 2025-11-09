# Spring Core


## Содержание

1. [Инверсия управления и внедрение зависимостей](#инверсия-управления-и-внедрение-зависимостей)
2. [Конфигурация Spring-приложения](#конфигурация-spring-приложения)
   - [Java-based конфигурация](#java-based-конфигурация)
   - [XML-конфигурация](#xml-конфигурация)
   - [Конфигурация через аннотации](#конфигурация-через-аннотации)
3. [Жизненный цикл бинов](#жизненный-цикл-бинов)
   - [Этапы создания бина](#этапы-создания-бина)
   - [Уничтожение бина](#уничтожение-бина)
4. [Области видимости бинов (Scopes)](#области-видимости-бинов-scopes)
   - [Основные scopes](#основные-scopes)
   - [Пользовательские scopes](#пользовательские-scopes)
5. [Архитектура Spring Container](#архитектура-spring-container)
   - [BeanFactory vs ApplicationContext](#beanfactory-vs-applicationcontext)
   - [Bean Definition и BeanFactoryPostProcessor](#bean-definition-и-beanfactorypostprocessor)
6. [Aspect-Oriented Programming (AOP)](#aspect-oriented-programming-aop)
   - [Основные концепции](#основные-концепции)
   - [Пример логирования через AOP](#пример-логирования-через-aop)
7. [Spring Expression Language (SpEL)](#spring-expression-language-spel)
8. [Профили и условная конфигурация](#профили-и-условная-конфигурация)
   - [Spring Profiles](#spring-profiles)
   - [Условные бины (@Conditional)](#условные-бины-conditional)
9. [Управление ресурсами и окружением](#управление-ресурсами-и-окружением)
   - [Resource abstraction](#resource-abstraction)
   - [Environment и PropertySource](#environment-и-propertysource)
10. [События приложения](#события-приложения)
11. [Интернационализация (i18n)](#интернационализация-i18n)
12. [Тестирование Spring-приложений](#тестирование-spring-приложений)
13. [Инструменты и диагностика](#инструменты-и-диагностика)
14. [Типичные сценарии использования](#типичные-сценарии-использования)
   - [Многомодульное приложение](#многомодульное-приложение)
   - [Внешняя конфигурация для микросервисов](#внешняя-конфигурация-для-микросервисов)
   - [Обработка исключений и валидация](#обработка-исключений-и-валидация)
15. [Практические упражнения](#практические-упражнения)
16. [Вопросы на собеседовании](#вопросы-на-собеседовании)
17. [Дополнительные материалы](#дополнительные-материалы)

## Инверсия управления и внедрение зависимостей

Spring Framework построен вокруг концепции Inversion of Control (IoC) — принципа, при котором фреймворк сам управляет созданием 
объектов и их зависимостями, а не программист вручную. Dependency Injection (DI) — один из способов реализации IoC, когда 
контейнер внедряет зависимости в объекты через конструктор, setter-методы или поля.

**Основные преимущества IoC/DI:**
- Слабая связанность модулей — компоненты зависят от абстракций, а не от конкретных реализаций
- Упрощённое тестирование — можно легко подменить реальные зависимости на mock-объекты
- Централизованное управление жизненным циклом — контейнер сам решает, когда создавать и уничтожать объекты
- Переиспользуемость — бины можно конфигурировать под разные окружения без изменения кода

**Виды внедрения зависимостей:**

1. **Constructor Injection** (рекомендуется) — зависимости передаются через конструктор:
   ```java
   @Service
   public class OrderService {
       private final PaymentService paymentService;
       
       @Autowired
       public OrderService(PaymentService paymentService) {
           this.paymentService = paymentService;
       }
   }
   ```
   Преимущества: обязательные зависимости явны, объект всегда в валидном состоянии, поддержка final-полей.

2. **Setter Injection** — зависимости внедряются через setter-методы:
   ```java
   @Service
   public class OrderService {
       private PaymentService paymentService;
       
       @Autowired
       public void setPaymentService(PaymentService paymentService) {
           this.paymentService = paymentService;
       }
   }
   ```
   Подходит для опциональных зависимостей или реконфигурации объектов.

3. **Field Injection** — прямое внедрение в поля (не рекомендуется для production):
   ```java
   @Service
   public class OrderService {
       @Autowired
       private PaymentService paymentService;
   }
   ```
   Удобно для быстрого прототипирования, но затрудняет тестирование и нарушает инкапсуляцию.

**Историческая справка.** До Spring 2.5 конфигурация выполнялась исключительно через XML. С появлением аннотаций 
(`@Component`, `@Autowired`) код стал компактнее. Spring 3.0 ввёл конфигурацию на Java (`@Configuration`, `@Bean`), 
что позволило полностью отказаться от XML в большинстве проектов.

## Конфигурация Spring-приложения

Spring поддерживает три основных подхода к конфигурации: Java-based, XML и аннотации. В современных проектах 
преобладает Java-конфигурация, но знание всех подходов важно для поддержки legacy-систем.

### Java-based конфигурация

Классы, помеченные `@Configuration`, содержат методы с аннотацией `@Bean`, которые возвращают объекты, 
регистрируемые в контейнере:

```java
@Configuration
@ComponentScan(basePackages = "com.example")
public class AppConfig {
    
    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://localhost:5432/mydb");
        ds.setUsername("user");
        ds.setPassword("pass");
        return ds;
    }
    
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

Преимущества: type-safety, рефакторинг через IDE, условная логика создания бинов (`@Conditional`).

### XML-конфигурация

XML-файлы описывают бины и их зависимости декларативно:

```xml
<beans xmlns="http://www.springframework.org/schema/beans">
    <bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
        <property name="jdbcUrl" value="jdbc:postgresql://localhost:5432/mydb"/>
        <property name="username" value="user"/>
        <property name="password" value="pass"/>
    </bean>
    
    <bean id="jdbcTemplate" class="org.springframework.jdbc.core.JdbcTemplate">
        <constructor-arg ref="dataSource"/>
    </bean>
</beans>
```

Используется в legacy-проектах или при необходимости внешней конфигурации без перекомпиляции.

### Конфигурация через аннотации

Аннотации `@Component`, `@Service`, `@Repository`, `@Controller` автоматически регистрируют классы как бины 
при включённом `@ComponentScan`:

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
```

Дополнительные аннотации: `@Qualifier` (выбор конкретной реализации), `@Primary` (предпочтительный кандидат), 
`@Lazy` (отложенная инициализация).

**Сравнение подходов:**
- Java-конфигурация — максимальная гибкость, type-safety, рекомендуется для новых проектов
- XML — внешняя конфигурация, наследие, сложность рефакторинга
- Аннотации — компактность, быстрое прототипирование, но меньше контроля над созданием бинов

## Жизненный цикл бинов

Spring управляет полным жизненным циклом бинов от создания до уничтожения. Понимание этапов помогает корректно 
инициализировать ресурсы и освобождать их при завершении работы.

### Этапы создания бина

1. **Инстанцирование** — контейнер вызывает конструктор класса или фабричный метод
2. **Заполнение свойств** — внедрение зависимостей через setter или поля
3. **BeanNameAware, BeanFactoryAware, ApplicationContextAware** — если бин реализует эти интерфейсы, Spring вызывает 
   соответствующие методы для передачи контекста
4. **BeanPostProcessor.postProcessBeforeInitialization** — вызов pre-initialization процессоров
5. **@PostConstruct или InitializingBean.afterPropertiesSet** — пользовательская инициализация
6. **init-method** — вызов метода инициализации, указанного в конфигурации
7. **BeanPostProcessor.postProcessAfterInitialization** — вызов post-initialization процессоров (здесь создаются proxy для AOP)
8. **Бин готов к использованию**

### Уничтожение бина

1. **@PreDestroy или DisposableBean.destroy** — пользовательская логика очистки ресурсов
2. **destroy-method** — вызов метода уничтожения, указанного в конфигурации

**Пример с callback-методами:**

```java
@Component
public class DatabaseConnection implements InitializingBean, DisposableBean {
    
    private Connection connection;
    
    @Override
    public void afterPropertiesSet() {
        // Инициализация соединения после внедрения зависимостей
        this.connection = DriverManager.getConnection("...");
        System.out.println("Connection established");
    }
    
    @Override
    public void destroy() {
        // Закрытие соединения при shutdown контейнера
        if (connection != null) {
            connection.close();
            System.out.println("Connection closed");
        }
    }
}
```

Современный подход с `@PostConstruct` и `@PreDestroy` предпочтительнее, так как не требует имплементации интерфейсов Spring.

## Области видимости бинов (Scopes)

Spring поддерживает несколько областей видимости, определяющих жизненный цикл и количество экземпляров бина:

### Основные scopes

1. **singleton** (по умолчанию) — один экземпляр на весь контейнер. Подходит для stateless-сервисов, репозиториев, 
   конфигурации:
   ```java
   @Service
   @Scope("singleton")
   public class UserService { }
   ```

2. **prototype** — новый экземпляр при каждом запросе. Используется для stateful-объектов:
   ```java
   @Component
   @Scope("prototype")
   public class ShoppingCart { }
   ```

3. **request** (только web) — один экземпляр на HTTP-запрос:
   ```java
   @Component
   @Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
   public class RequestContext { }
   ```

4. **session** (только web) — один экземпляр на HTTP-сессию:
   ```java
   @Component
   @Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
   public class UserSession { }
   ```

5. **application** (только web) — один экземпляр на ServletContext

6. **websocket** — один экземпляр на WebSocket-сессию

**Важно:** при внедрении request/session-scope бинов в singleton используйте `proxyMode`, чтобы Spring создавал прокси, 
перенаправляющий вызовы на актуальный экземпляр.

### Пользовательские scopes

Можно создать собственную область видимости, реализовав `org.springframework.beans.factory.config.Scope` и зарегистрировав 
её через `ConfigurableBeanFactory.registerScope`.

## Архитектура Spring Container

### BeanFactory vs ApplicationContext

**BeanFactory** — базовый контейнер IoC, предоставляет механизм конфигурации и управления бинами. Функционал:
- Lazy initialization — бины создаются при первом обращении
- Минимальный набор возможностей (нет поддержки AOP, событий, интернационализации)

**ApplicationContext** — расширенный контейнер, наследующий BeanFactory и добавляющий:
- Eager initialization — бины создаются при запуске контейнера (можно настроить lazy)
- Интеграция с AOP
- Поддержка событий приложения (`ApplicationEvent`, `ApplicationListener`)
- Интернационализация через `MessageSource`
- Доступ к ресурсам (файлы, classpath, URL)
- Автоматическая регистрация `BeanPostProcessor` и `BeanFactoryPostProcessor`

**Реализации ApplicationContext:**
- `AnnotationConfigApplicationContext` — для Java-конфигурации
- `ClassPathXmlApplicationContext` — для XML из classpath
- `FileSystemXmlApplicationContext` — для XML из файловой системы
- `WebApplicationContext` — для web-приложений

Практика: в production всегда используйте `ApplicationContext`. `BeanFactory` актуален только для ограниченных 
окружений (embedded, IoT) с жёсткими требованиями к памяти.

### Bean Definition и BeanFactoryPostProcessor

Метаданные о бинах хранятся в виде `BeanDefinition` (класс, scope, зависимости, init/destroy методы). 
`BeanFactoryPostProcessor` позволяет модифицировать `BeanDefinition` до создания бинов:

```java
@Component
public class CustomBeanFactoryPostProcessor implements BeanFactoryPostProcessor {
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        BeanDefinition bd = beanFactory.getBeanDefinition("userService");
        bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
    }
}
```

Стандартные `BeanFactoryPostProcessor`: `PropertyPlaceholderConfigurer`, `PropertyOverrideConfigurer`.

## Aspect-Oriented Programming (AOP)

AOP дополняет ООП, позволяя выносить сквозную функциональность (логирование, транзакции, безопасность) в отдельные 
модули — аспекты. Spring AOP работает на основе прокси-объектов.

### Основные концепции

- **Aspect** — модуль, объединяющий сквозную логику (класс с `@Aspect`)
- **Join Point** — точка в исполнении программы (вызов метода, создание объекта)
- **Advice** — действие, выполняемое в join point (@Before, @After, @Around, @AfterReturning, @AfterThrowing)
- **Pointcut** — выражение, определяющее набор join points для применения advice
- **Weaving** — процесс связывания аспектов с целевыми объектами (в Spring — runtime через прокси)

### Пример логирования через AOP

```java
@Aspect
@Component
public class LoggingAspect {
    
    @Before("execution(* com.example.service.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        System.out.println("Calling: " + joinPoint.getSignature().getName());
    }
    
    @Around("@annotation(com.example.Timed)")
    public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        Object result = pjp.proceed();
        long duration = System.currentTimeMillis() - start;
        System.out.println(pjp.getSignature() + " took " + duration + "ms");
        return result;
    }
}
```

**Типы прокси:**
- **JDK Dynamic Proxy** — если бин реализует интерфейс, используется по умолчанию
- **CGLIB** — если бин не реализует интерфейсы, создаётся подкласс (требует `@EnableAspectJAutoProxy(proxyTargetClass=true)`)

**Ограничения Spring AOP:** работает только с методами Spring-бинов, нет поддержки pointcut на уровне полей или 
конструкторов. Для full AspectJ используйте load-time или compile-time weaving.

## Spring Expression Language (SpEL)

SpEL — мощный язык выражений для манипулирования графом объектов во время выполнения. Используется в конфигурации, 
аннотациях, шаблонах.

**Синтаксис:**
```java
@Value("#{systemProperties['user.region']}")
private String region;

@Value("#{userService.findById(1).username}")
private String username;

@Value("#{T(java.lang.Math).random() * 100}")
private double randomNumber;
```

**Возможности SpEL:**
- Доступ к properties и environment: `${property.name}`, `#{environment['JAVA_HOME']}`
- Вызов методов и конструкторов: `#{bean.method(arg)}`
- Операторы: арифметические, логические, тернарный, elvis (`?:`)
- Коллекции и фильтрация: `#{list.?[age > 18]}`
- Регулярные выражения: `#{email matches '[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}'}`

Используйте SpEL для динамической конфигурации, но избегайте сложной логики — вынесите её в отдельные бины.

## Профили и условная конфигурация

### Spring Profiles

Профили позволяют активировать различные наборы бинов в зависимости от окружения (dev, test, prod):

```java
@Configuration
@Profile("dev")
public class DevConfig {
    @Bean
    public DataSource dataSource() {
        return new H2DataSource();
    }
}

@Configuration
@Profile("prod")
public class ProdConfig {
    @Bean
    public DataSource dataSource() {
        return new HikariDataSource();
    }
}
```

Активация профилей:
- JVM property: `-Dspring.profiles.active=dev`
- Environment variable: `SPRING_PROFILES_ACTIVE=dev`
- Программно: `context.getEnvironment().setActiveProfiles("dev")`
- application.properties: `spring.profiles.active=dev`

### Условные бины (@Conditional)

`@Conditional` позволяет регистрировать бины на основе произвольных условий:

```java
@Configuration
public class CacheConfig {
    
    @Bean
    @ConditionalOnProperty(name = "cache.enabled", havingValue = "true")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
```

Spring Boot предоставляет готовые условия: `@ConditionalOnClass`, `@ConditionalOnMissingBean`, `@ConditionalOnBean`, 
`@ConditionalOnWebApplication`.

## Управление ресурсами и окружением

### Resource abstraction

Spring унифицирует доступ к ресурсам через `org.springframework.core.io.Resource`:

```java
@Autowired
private ResourceLoader resourceLoader;

public void loadResource() {
    Resource resource = resourceLoader.getResource("classpath:data/users.json");
    Resource urlResource = resourceLoader.getResource("https://example.com/api/data");
    Resource fileResource = resourceLoader.getResource("file:/tmp/config.xml");
}
```

Префиксы: `classpath:`, `file:`, `http:`, `ftp:`.

### Environment и PropertySource

`Environment` предоставляет доступ к системным свойствам, переменным окружения и application properties:

```java
@Autowired
private Environment env;

public void readProperties() {
    String dbUrl = env.getProperty("database.url");
    Integer maxConnections = env.getProperty("database.max-connections", Integer.class, 10);
}
```

Порядок разрешения properties (от высшего приоритета к низшему):
1. System properties (`-D` параметры JVM)
2. Environment variables
3. `application.properties` или `application.yml`
4. `@PropertySource`
5. Default properties

## События приложения

Spring поддерживает механизм событий для слабо связанной коммуникации между компонентами.

**Создание и публикация события:**

```java
public class UserRegisteredEvent extends ApplicationEvent {
    private final String username;
    
    public UserRegisteredEvent(Object source, String username) {
        super(source);
        this.username = username;
    }
    
    public String getUsername() { return username; }
}

@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void registerUser(String username) {
        // регистрация пользователя
        eventPublisher.publishEvent(new UserRegisteredEvent(this, username));
    }
}
```

**Обработка события:**

```java
@Component
public class EmailNotificationListener {
    
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("Sending email to: " + event.getUsername());
    }
    
    @Async
    @EventListener
    public void asyncHandler(UserRegisteredEvent event) {
        // асинхронная обработка
    }
}
```

События могут быть синхронными или асинхронными (через `@Async`), поддерживают условную обработку (`condition` в `@EventListener`), 
транзакционные фазы (`@TransactionalEventListener`).

## Интернационализация (i18n)

Spring интегрирует `MessageSource` для поддержки многоязычности:

```java
@Configuration
public class I18nConfig {
    
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource ms = new ResourceBundleMessageSource();
        ms.setBasename("messages");
        ms.setDefaultEncoding("UTF-8");
        return ms;
    }
}
```

Файлы `messages_en.properties`, `messages_ru.properties`:
```
welcome.message=Welcome, {0}!
```

```java
@Service
public class GreetingService {
    @Autowired
    private MessageSource messageSource;
    
    public String greet(String name, Locale locale) {
        return messageSource.getMessage("welcome.message", new Object[]{name}, locale);
    }
}
```

## Тестирование Spring-приложений

Spring Test предоставляет инструменты для интеграционного и unit-тестирования:

```java
@SpringBootTest
@ActiveProfiles("test")
public class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @MockBean
    private UserRepository userRepository;
    
    @Test
    public void testFindUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User("John")));
        User user = userService.findById(1L);
        assertEquals("John", user.getUsername());
    }
}
```

Аннотации:
- `@SpringBootTest` — загружает полный ApplicationContext
- `@WebMvcTest` — тестирование только web-слоя
- `@DataJpaTest` — тестирование JPA-репозиториев с in-memory БД
- `@MockBean` — создание mock-объектов в контексте Spring
- `@TestConfiguration` — дополнительная конфигурация для тестов

## Инструменты и диагностика

- **Spring Boot Actuator** — эндпоинты для мониторинга (`/actuator/health`, `/actuator/metrics`, `/actuator/beans`)
- **spring-boot-devtools** — hot reload при разработке, автоматический рестарт контекста
- **@EnableMBeanExport** — экспорт бинов в JMX для мониторинга через JConsole/VisualVM
- **Logging** — интеграция с Logback, Log4j2, SLF4J; настройка уровней через `logging.level.*`
- **Debugging Context** — используйте `AnnotationConfigUtils.registerAnnotationConfigProcessors` для отладки регистрации бинов

Практика: включайте Actuator в production с ограниченным доступом, мониторьте метрики JVM и пулы соединений через Micrometer.

## Типичные сценарии использования

### Многомодульное приложение

Разделите проект на модули:
- `core-module` — доменные сущности, бизнес-логика
- `web-module` — REST-контроллеры, зависит от `core-module`
- `integration-module` — клиенты внешних API, очереди сообщений

Каждый модуль имеет свою конфигурацию (`@Configuration`), импортируемую через `@Import` или `@ComponentScan`.

### Внешняя конфигурация для микросервисов

Используйте Spring Cloud Config Server для централизованного управления конфигурацией. Приложения получают properties при 
старте, поддерживается обновление без рестарта через `/actuator/refresh`.

### Обработка исключений и валидация

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getMessage()));
    }
}

@Service
public class UserService {
    
    public User createUser(@Valid UserDto dto) {
        // Валидация через @Valid + JSR-303 аннотации
    }
}
```

## Практические упражнения

1. Создайте простое приложение с Java-конфигурацией, реализуйте сервис с внедрением зависимостей через конструктор.
2. Настройте несколько профилей (dev, prod) с различными источниками данных (H2 для dev, PostgreSQL для prod).
3. Реализуйте аспект для логирования всех вызовов методов сервисного слоя с измерением времени выполнения.
4. Создайте пользовательское событие и обработайте его в нескольких слушателях (синхронно и асинхронно).
5. Напишите интеграционный тест с использованием `@SpringBootTest` и `@MockBean` для подмены зависимостей.

## Вопросы на собеседовании

1. **В чём разница между BeanFactory и ApplicationContext?**
   
   *Ответ:* `BeanFactory` — базовый IoC-контейнер с lazy-инициализацией и минимальным функционалом. `ApplicationContext` 
   расширяет `BeanFactory`, добавляя eager-инициализацию (по умолчанию), поддержку AOP, событий приложения, интернационализации, 
   доступа к ресурсам. В production используется `ApplicationContext`.

2. **Как настроить различные области видимости бинов?**
   
   *Ответ:* Используйте аннотацию `@Scope` с параметрами: `singleton` (по умолчанию), `prototype`, `request`, `session`, 
   `application`, `websocket`. Для web-scopes требуется `proxyMode = ScopedProxyMode.TARGET_CLASS`, чтобы Spring создавал 
   прокси при внедрении в singleton-бины.

3. **Какие способы конфигурации бинов поддерживает Spring?**
   
   *Ответ:* Три основных способа: Java-конфигурация (`@Configuration`, `@Bean`), XML-конфигурация (`<bean>` в XML-файлах), 
   конфигурация через аннотации (`@Component`, `@Service`, `@Repository`, `@Controller` + `@ComponentScan`). Рекомендуется 
   Java-конфигурация для type-safety и удобства рефакторинга.

4. **Объясните жизненный цикл Spring-бина.**
   
   *Ответ:* Создание: инстанцирование → заполнение свойств → вызов Aware-интерфейсов → BeanPostProcessor.before → 
   @PostConstruct/InitializingBean → init-method → BeanPostProcessor.after → бин готов. Уничтожение: @PreDestroy/DisposableBean → 
   destroy-method.

5. **Что такое AOP и как он реализован в Spring?**
   
   *Ответ:* Aspect-Oriented Programming — парадигма для выделения сквозной функциональности в аспекты. Spring AOP работает 
   через runtime-прокси (JDK Dynamic Proxy для интерфейсов, CGLIB для классов). Основные концепции: Aspect, Advice 
   (Before/After/Around), Pointcut, Join Point. Ограничение: работает только с методами Spring-бинов.

6. **Как работает @Autowired и когда возникает NoUniqueBeanDefinitionException?**
   
   *Ответ:* `@Autowired` запрашивает у контейнера бин по типу. Если найдено несколько кандидатов, Spring выбрасывает 
   `NoUniqueBeanDefinitionException`. Решения: использовать `@Qualifier` для указания имени бина, пометить один из бинов как 
   `@Primary`, внедрить `List<Type>` для получения всех реализаций.

7. **В чём преимущества constructor injection перед field injection?**
   
   *Ответ:* Constructor injection делает обязательные зависимости явными, позволяет использовать `final` поля, упрощает 
   unit-тестирование (можно создать объект без Spring), обеспечивает неизменность. Field injection скрывает зависимости, 
   затрудняет тестирование и нарушает инкапсуляцию.

8. **Как Spring обрабатывает циклические зависимости?**
   
   *Ответ:* Для singleton-бинов с setter/field injection Spring использует кеш "ранних ссылок" (early references) — создаёт 
   объект, помещает в кеш, затем заполняет зависимости. Для constructor injection циклические зависимости невозможно разрешить 
   автоматически, Spring выбросит `BeanCurrentlyInCreationException`. Решение: рефакторинг архитектуры или использование 
   setter injection.

9. **Что такое BeanPostProcessor и для чего он используется?**
   
   *Ответ:* `BeanPostProcessor` — интерфейс для кастомизации бинов после создания. Методы `postProcessBeforeInitialization` 
   и `postProcessAfterInitialization` вызываются для каждого бина. Используется для создания прокси (AOP), валидации, 
   инициализации дополнительных полей. Примеры: `AutowiredAnnotationBeanPostProcessor`, `CommonAnnotationBeanPostProcessor`.

10. **Как работает механизм событий в Spring?**
    
    *Ответ:* Spring поддерживает событийную модель через `ApplicationEvent` и `ApplicationListener`. События публикуются 
    через `ApplicationEventPublisher.publishEvent()`, обрабатываются методами с `@EventListener`. Поддерживаются синхронная 
    и асинхронная обработка (`@Async`), условная обработка (`condition`), транзакционные события (`@TransactionalEventListener`).

## Дополнительные материалы

- [Spring Framework Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/)
- [Spring Core Technologies](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html)
- Книга "Spring in Action" by Craig Walls — практическое руководство с примерами
- Книга "Expert Spring MVC and Web Flow" — углублённое изучение архитектуры
- [Baeldung Spring Tutorials](https://www.baeldung.com/spring-tutorial) — подробные статьи и примеры кода
