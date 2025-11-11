# Реактивное программирование в Java

## Содержание

1. [Введение в реактивное программирование](#введение-в-реактивное-программирование)
2. [Reactive Streams спецификация](#reactive-streams-спецификация)
3. [Project Reactor](#project-reactor)
   - [Mono и Flux](#mono-и-flux)
   - [Операторы Reactor](#операторы-reactor)
   - [Обработка ошибок](#обработка-ошибок)
   - [Schedulers и многопоточность](#schedulers-и-многопоточность)
4. [RxJava](#rxjava)
   - [Observable, Single, Maybe, Completable](#observable-single-maybe-completable)
   - [Операторы RxJava](#операторы-rxjava)
   - [Schedulers в RxJava](#schedulers-в-rxjava)
   - [Сравнение RxJava и Reactor](#сравнение-rxjava-и-reactor)
5. [Spring WebFlux](#spring-webflux)
   - [Архитектура WebFlux](#архитектура-webflux)
   - [Функциональные эндпоинты](#функциональные-эндпоинты)
   - [Аннотированные контроллеры](#аннотированные-контроллеры)
6. [WebClient](#webclient)
   - [Основы использования](#основы-использования-webclient)
   - [Конфигурация WebClient](#конфигурация-webclient)
   - [Обработка ответов](#обработка-ответов)
   - [Обработка ошибок в WebClient](#обработка-ошибок-в-webclient)
7. [Reactive Repositories](#reactive-repositories)
8. [Backpressure](#backpressure)
9. [Тестирование реактивного кода](#тестирование-реактивного-кода)
10. [Практические паттерны](#практические-паттерны)
11. [Когда использовать реактивное программирование](#когда-использовать-реактивное-программирование)
12. [Практические упражнения](#практические-упражнения)
13. [Вопросы на собеседовании](#вопросы-на-собеседовании)
14. [Дополнительные материалы](#дополнительные-материалы)

## Введение в реактивное программирование

**Реактивное программирование** — это парадигма программирования, ориентированная на асинхронные потоки данных и распространение изменений. В основе лежит работа с потоками событий (streams), которые могут быть обработаны, трансформированы и объединены.

**Ключевые принципы реактивного программирования:**

1. **Responsive (отзывчивость)** — система отвечает своевременно, насколько это возможно
2. **Resilient (устойчивость)** — система остаётся отзывчивой даже при возникновении ошибок
3. **Elastic (эластичность)** — система масштабируется под нагрузкой
4. **Message Driven (управляется сообщениями)** — асинхронная передача сообщений между компонентами

**Основные преимущества:**
- Эффективное использование ресурсов (меньше потоков, больше работы)
- Естественная поддержка асинхронности
- Композиция асинхронных операций
- Встроенная поддержка backpressure (управление потоком данных)
- Декларативный стиль программирования

**Историческая справка.** Концепция реактивного программирования существовала давно (FRP - Functional Reactive Programming), но популярность в JVM-мире получила с появлением RxJava (2013) от Netflix. В 2015 году появилась спецификация Reactive Streams, а затем Project Reactor, ставший основой Spring WebFlux (2017).

## Reactive Streams спецификация

**Reactive Streams** — это стандарт для асинхронной обработки потоков данных с non-blocking backpressure. Спецификация определяет минимальный набор интерфейсов для взаимодействия реактивных библиотек.

**Основные интерфейсы:**

```java
public interface Publisher<T> {
    void subscribe(Subscriber<? super T> subscriber);
}

public interface Subscriber<T> {
    void onSubscribe(Subscription subscription);
    void onNext(T item);
    void onError(Throwable throwable);
    void onComplete();
}

public interface Subscription {
    void request(long n);  // запрос n элементов
    void cancel();         // отмена подписки
}

public interface Processor<T, R> extends Subscriber<T>, Publisher<R> {
}
```

**Жизненный цикл взаимодействия:**

1. Subscriber подписывается на Publisher через `subscribe()`
2. Publisher вызывает `onSubscribe()`, передавая Subscription
3. Subscriber запрашивает данные через `subscription.request(n)`
4. Publisher отправляет данные через `onNext()` (не больше запрошенного)
5. По завершению или ошибке вызывается `onComplete()` или `onError()`

```java
// Пример базовой реализации
Publisher<String> publisher = subscriber -> {
    subscriber.onSubscribe(new Subscription() {
        private boolean cancelled = false;
        
        @Override
        public void request(long n) {
            if (!cancelled) {
                for (int i = 0; i < n; i++) {
                    subscriber.onNext("Item " + i);
                }
                subscriber.onComplete();
            }
        }
        
        @Override
        public void cancel() {
            cancelled = true;
        }
    });
};
```

> **Важно**: Reactive Streams API с Java 9 включён в JDK как `java.util.concurrent.Flow`. Большинство библиотек предоставляют адаптеры для совместимости.

## Project Reactor

**Project Reactor** — реактивная библиотека от Pivotal (создатели Spring), реализация спецификации Reactive Streams. Это основа Spring WebFlux и рекомендуемый выбор для Spring-приложений.

### Mono и Flux

Два основных типа в Reactor:

**Flux<T>** — поток от 0 до N элементов:
```java
// Создание Flux
Flux<String> flux1 = Flux.just("A", "B", "C");
Flux<Integer> flux2 = Flux.range(1, 5);
Flux<String> flux3 = Flux.fromIterable(Arrays.asList("X", "Y", "Z"));
Flux<Long> flux4 = Flux.interval(Duration.ofSeconds(1)); // бесконечный поток

// Подписка и обработка
flux1.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Completed")
);
```

**Mono<T>** — поток 0 или 1 элемент (аналог Optional или Future):
```java
// Создание Mono
Mono<String> mono1 = Mono.just("Hello");
Mono<String> mono2 = Mono.empty();
Mono<String> mono3 = Mono.error(new RuntimeException("Error"));

// Создание из Callable/Supplier
Mono<String> mono4 = Mono.fromCallable(() -> {
    // Долгая операция
    return "Result";
});

// Получение значения (блокирующая операция!)
String value = mono1.block(); // Только для тестов!
```

> **Важно**: `block()` — блокирующая операция, использовать только в тестах или на границах реактивного кода.

### Операторы Reactor

**Трансформация:**

```java
// map - преобразование каждого элемента
Flux<String> names = Flux.just("alice", "bob", "charlie");
Flux<String> upperNames = names.map(String::toUpperCase);

// flatMap - преобразование в Publisher и слияние
Flux<String> flux = Flux.just("user1", "user2")
    .flatMap(userId -> getUserDetails(userId)); // возвращает Mono<User>

// flatMapSequential - сохраняет порядок
Flux<String> orderedFlux = Flux.just("user1", "user2")
    .flatMapSequential(userId -> getUserDetails(userId));

// concatMap - последовательная обработка (без параллелизма)
Flux<String> sequential = Flux.just("a", "b")
    .concatMap(this::processItem);
```

**Фильтрация:**

```java
// filter - фильтрация элементов
Flux<Integer> evenNumbers = Flux.range(1, 10)
    .filter(n -> n % 2 == 0);

// take - взять первые N элементов
Flux<Integer> firstThree = Flux.range(1, 100)
    .take(3);

// skip - пропустить первые N элементов
Flux<Integer> afterFirst5 = Flux.range(1, 10)
    .skip(5);

// distinct - уникальные элементы
Flux<String> unique = Flux.just("a", "b", "a", "c", "b")
    .distinct();
```

**Комбинирование:**

```java
// merge - слияние нескольких потоков
Flux<String> merged = Flux.merge(flux1, flux2, flux3);

// concat - последовательное объединение
Flux<String> concatenated = Flux.concat(flux1, flux2);

// zip - комбинирование элементов из разных потоков
Flux<Tuple2<String, Integer>> zipped = Flux.zip(
    Flux.just("A", "B", "C"),
    Flux.range(1, 3)
);

// combineLatest - комбинация последних значений
Flux<String> combined = Flux.combineLatest(
    flux1, flux2,
    (a, b) -> a + b
);
```

**Агрегация:**

```java
// collectList - собрать все элементы в список
Mono<List<String>> list = Flux.just("a", "b", "c")
    .collectList();

// reduce - свёртка
Mono<Integer> sum = Flux.range(1, 10)
    .reduce(0, (acc, val) -> acc + val);

// count - подсчёт элементов
Mono<Long> count = flux.count();
```

**Временные операторы:**

```java
// delayElements - задержка между элементами
Flux<Long> delayed = Flux.interval(Duration.ofMillis(100))
    .delayElements(Duration.ofMillis(50));

// timeout - таймаут на получение элемента
Flux<String> withTimeout = flux
    .timeout(Duration.ofSeconds(5));

// buffer - буферизация элементов
Flux<List<Integer>> buffered = Flux.range(1, 10)
    .buffer(3); // [[1,2,3], [4,5,6], [7,8,9], [10]]

// window - создание окон (подпотоков)
Flux<Flux<Integer>> windowed = Flux.range(1, 10)
    .window(3);
```

### Обработка ошибок

```java
// onErrorReturn - вернуть значение по умолчанию
Mono<String> withDefault = mono
    .onErrorReturn("default value");

// onErrorResume - переключиться на альтернативный поток
Mono<String> withFallback = mono
    .onErrorResume(error -> {
        log.error("Error occurred", error);
        return Mono.just("fallback");
    });

// onErrorMap - преобразовать ошибку
Mono<String> withMappedError = mono
    .onErrorMap(IOException.class, 
        ex -> new CustomException("IO failed", ex));

// retry - повторить при ошибке
Flux<String> withRetry = flux
    .retry(3); // максимум 3 попытки

// retryWhen - кастомная логика повтора
Flux<String> withBackoff = flux
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));

// doOnError - side-effect при ошибке
Flux<String> logged = flux
    .doOnError(error -> log.error("Error", error));
```

### Schedulers и многопоточность

По умолчанию Reactor выполняется в том же потоке, где была создана подписка. Schedulers позволяют управлять потоками выполнения.

**Типы Schedulers:**

```java
// Schedulers.immediate() - текущий поток (по умолчанию)
// Schedulers.single() - один переиспользуемый поток
// Schedulers.parallel() - пул потоков (CPU-bound задачи)
// Schedulers.boundedElastic() - пул потоков для I/O операций
// Schedulers.fromExecutorService() - кастомный ExecutorService
```

**Использование:**

```java
// publishOn - переключить выполнение downstream операторов
Flux<String> flux = Flux.just("A", "B", "C")
    .map(s -> {
        System.out.println("Map 1: " + Thread.currentThread().getName());
        return s.toLowerCase();
    })
    .publishOn(Schedulers.parallel())
    .map(s -> {
        System.out.println("Map 2: " + Thread.currentThread().getName());
        return s + "!";
    });

// subscribeOn - переключить выполнение всей цепочки
Flux<String> flux2 = Flux.just("A", "B", "C")
    .subscribeOn(Schedulers.boundedElastic())
    .map(String::toLowerCase);

// Комбинация publishOn и subscribeOn
Mono<String> result = Mono.fromCallable(() -> {
        // Выполнится в boundedElastic
        return fetchDataFromDatabase();
    })
    .subscribeOn(Schedulers.boundedElastic())
    .map(data -> {
        // Выполнится в parallel
        return processData(data);
    })
    .publishOn(Schedulers.parallel());
```

> **Важно**: `subscribeOn` влияет на весь поток с момента подписки, `publishOn` влияет только на операторы после него.

## RxJava

**RxJava** — реактивная библиотека от Netflix, одна из первых и наиболее известных реализаций реактивного программирования в Java. Хотя Spring WebFlux использует Reactor, RxJava остаётся популярной в Android-разработке и legacy-проектах.

### Observable, Single, Maybe, Completable

RxJava предоставляет несколько типов для разных сценариев:

**Observable<T>** — поток от 0 до N элементов:
```java
// Создание Observable
Observable<String> observable = Observable.just("A", "B", "C");
Observable<Integer> range = Observable.range(1, 5);
Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);

// Подписка
observable.subscribe(
    item -> System.out.println("Item: " + item),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Complete")
);
```

**Single<T>** — ровно 1 элемент или ошибка:
```java
Single<String> single = Single.just("Hello");
Single<User> user = Single.fromCallable(() -> fetchUser());

single.subscribe(
    value -> System.out.println("Value: " + value),
    error -> System.err.println("Error: " + error)
);
```

**Maybe<T>** — 0 или 1 элемент:
```java
Maybe<String> maybe = Maybe.just("Value");
Maybe<String> empty = Maybe.empty();

maybe.subscribe(
    value -> System.out.println("Value: " + value),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Empty")
);
```

**Completable** — завершение без значения (только успех/ошибка):
```java
Completable completable = Completable.fromAction(() -> {
    performSideEffect();
});

completable.subscribe(
    () -> System.out.println("Completed"),
    error -> System.err.println("Error: " + error)
);
```

### Операторы RxJava

RxJava предоставляет богатый набор операторов, аналогичных Reactor:

**Трансформация:**
```java
// map
Observable<String> upper = observable.map(String::toUpperCase);

// flatMap
Observable<User> users = Observable.just("id1", "id2")
    .flatMap(id -> fetchUser(id)); // возвращает Observable<User>

// switchMap - отменяет предыдущие, если пришёл новый
Observable<String> search = searchQuery
    .switchMap(query -> performSearch(query));

// scan - накопление с промежуточными результатами
Observable<Integer> runningSum = Observable.range(1, 5)
    .scan((acc, val) -> acc + val);
```

**Фильтрация:**
```java
// filter
Observable<Integer> even = Observable.range(1, 10)
    .filter(n -> n % 2 == 0);

// take, skip
Observable<Integer> first3 = observable.take(3);
Observable<Integer> after5 = observable.skip(5);

// debounce - пропускает элементы, если следующий пришёл слишком быстро
Observable<String> debounced = searchInput
    .debounce(300, TimeUnit.MILLISECONDS);

// throttleFirst - пропускает элементы в течение окна
Observable<Click> throttled = clicks
    .throttleFirst(1, TimeUnit.SECONDS);
```

**Комбинирование:**
```java
// merge
Observable<String> merged = Observable.merge(obs1, obs2);

// zip
Observable<String> zipped = Observable.zip(
    obs1, obs2,
    (a, b) -> a + b
);

// combineLatest
Observable<String> combined = Observable.combineLatest(
    obs1, obs2,
    (a, b) -> a + b
);
```

**Обработка ошибок:**
```java
// onErrorReturn
Observable<String> withDefault = observable
    .onErrorReturn(error -> "default");

// onErrorResumeNext
Observable<String> withFallback = observable
    .onErrorResumeNext(error -> Observable.just("fallback"));

// retry
Observable<String> withRetry = observable
    .retry(3);
```

### Schedulers в RxJava

```java
// Schedulers.io() - для I/O операций
// Schedulers.computation() - для CPU-интенсивных задач
// Schedulers.newThread() - каждый раз новый поток
// Schedulers.single() - один поток для всех операций
// Schedulers.trampoline() - очередь в текущем потоке
// AndroidSchedulers.mainThread() - главный поток Android (только для Android)

Observable<String> observable = Observable.just("Data")
    .subscribeOn(Schedulers.io())        // подписка в IO потоке
    .observeOn(Schedulers.computation())  // обработка в computation
    .map(this::processData);
```

### Сравнение RxJava и Reactor

| Характеристика | RxJava | Project Reactor |
|----------------|---------|-----------------|
| **Типы** | Observable, Single, Maybe, Completable | Flux, Mono |
| **Экосистема** | Независимая, популярна в Android | Интегрирована с Spring |
| **Backpressure** | Flowable (отдельный тип) | Встроен в Flux |
| **Версия** | RxJava 3.x (актуальная) | Reactor 3.x |
| **Reactive Streams** | Реализует (Flowable) | Полностью совместима |
| **Производительность** | Хорошая | Отличная (оптимизирована для Spring) |
| **Операторы** | ~650 операторов | ~300 операторов (но покрывает основное) |

**Когда использовать:**
- **RxJava**: Android-разработка, legacy Java-приложения, нужны специфичные операторы
- **Reactor**: Spring WebFlux приложения, новые проекты на Spring

**Взаимная конвертация:**
```java
// RxJava -> Reactor
Flux<String> flux = Flux.from(observable.toFlowable(BackpressureStrategy.BUFFER));
Mono<String> mono = Mono.from(single.toFlowable());

// Reactor -> RxJava
Observable<String> observable = Flowable.fromPublisher(flux)
    .toObservable();
Single<String> single = Flowable.fromPublisher(mono)
    .singleOrError();
```

## Spring WebFlux

**Spring WebFlux** — реактивный веб-фреймворк в Spring 5+, альтернатива Spring MVC. Построен на Project Reactor и поддерживает полностью неблокирующий стек.

### Архитектура WebFlux

**Основные отличия от Spring MVC:**

| Характеристика | Spring MVC | Spring WebFlux |
|----------------|------------|----------------|
| **Модель** | Blocking I/O | Non-blocking I/O |
| **Сервер** | Servlet API (Tomcat, Jetty) | Netty, Undertow, Servlet 3.1+ |
| **Программирование** | Императивное | Реактивное |
| **Масштабирование** | Пул потоков | Event Loop |
| **Контроллеры** | @Controller | @Controller или функциональные |

**Стек WebFlux:**
```
┌─────────────────────────────────────┐
│   Spring WebFlux Application        │
├─────────────────────────────────────┤
│   Router Functions / @Controller    │
├─────────────────────────────────────┤
│      Spring WebFlux Framework       │
├─────────────────────────────────────┤
│         Reactor (Mono/Flux)         │
├─────────────────────────────────────┤
│   Netty / Undertow / Servlet 3.1+   │
└─────────────────────────────────────┘
```

**Зависимости:**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
```

### Функциональные эндпоинты

Функциональный стиль — альтернативный способ определения endpoints в WebFlux:

```java
@Configuration
public class RouterConfig {
    
    @Bean
    public RouterFunction<ServerResponse> routes(UserHandler handler) {
        return RouterFunctions
            .route(GET("/users"), handler::getAllUsers)
            .andRoute(GET("/users/{id}"), handler::getUserById)
            .andRoute(POST("/users"), handler::createUser)
            .andRoute(PUT("/users/{id}"), handler::updateUser)
            .andRoute(DELETE("/users/{id}"), handler::deleteUser);
    }
}

@Component
public class UserHandler {
    
    private final UserService userService;
    
    public UserHandler(UserService userService) {
        this.userService = userService;
    }
    
    public Mono<ServerResponse> getAllUsers(ServerRequest request) {
        Flux<User> users = userService.findAll();
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(users, User.class);
    }
    
    public Mono<ServerResponse> getUserById(ServerRequest request) {
        String id = request.pathVariable("id");
        Mono<User> user = userService.findById(id);
        
        return user
            .flatMap(u -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(u))
            .switchIfEmpty(ServerResponse.notFound().build());
    }
    
    public Mono<ServerResponse> createUser(ServerRequest request) {
        Mono<User> userMono = request.bodyToMono(User.class);
        
        return userMono
            .flatMap(userService::save)
            .flatMap(savedUser -> ServerResponse
                .created(URI.create("/users/" + savedUser.getId()))
                .bodyValue(savedUser));
    }
}
```

**Вложенные роуты и фильтры:**
```java
@Bean
public RouterFunction<ServerResponse> apiRoutes(UserHandler handler) {
    return RouterFunctions
        .nest(path("/api"),
            RouterFunctions
                .route(GET("/users"), handler::getAllUsers)
                .andRoute(POST("/users"), handler::createUser)
        )
        .filter((request, next) -> {
            // Логирование
            log.info("Request: {} {}", request.method(), request.path());
            return next.handle(request);
        });
}
```

### Аннотированные контроллеры

WebFlux поддерживает привычный стиль с @RestController:

```java
@RestController
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;
    
    public UserController(UserService userService) {
        this.userService = userService;
    }
    
    @GetMapping
    public Flux<User> getAllUsers() {
        return userService.findAll();
    }
    
    @GetMapping("/{id}")
    public Mono<User> getUserById(@PathVariable String id) {
        return userService.findById(id);
    }
    
    @PostMapping
    public Mono<User> createUser(@RequestBody User user) {
        return userService.save(user);
    }
    
    @PutMapping("/{id}")
    public Mono<User> updateUser(@PathVariable String id, 
                                  @RequestBody User user) {
        return userService.update(id, user);
    }
    
    @DeleteMapping("/{id}")
    public Mono<Void> deleteUser(@PathVariable String id) {
        return userService.deleteById(id);
    }
    
    // Server-Sent Events
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<User> streamUsers() {
        return userService.findAll()
            .delayElements(Duration.ofSeconds(1));
    }
}
```

**Обработка ошибок:**
```java
@RestControllerAdvice
public class GlobalErrorHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(UserNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(404, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(500, "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

## WebClient

**WebClient** — неблокирующий HTTP-клиент в Spring WebFlux, реактивная альтернатива RestTemplate.

### Основы использования WebClient

**Создание WebClient:**
```java
// Простое создание
WebClient client = WebClient.create();

// С базовым URL
WebClient client = WebClient.create("https://api.example.com");

// Через builder
WebClient client = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .defaultCookie("session", "...")
    .build();
```

**GET запросы:**
```java
// Простой GET
Mono<User> user = webClient
    .get()
    .uri("/users/{id}", userId)
    .retrieve()
    .bodyToMono(User.class);

// GET с query параметрами
Flux<User> users = webClient
    .get()
    .uri(uriBuilder -> uriBuilder
        .path("/users")
        .queryParam("page", 1)
        .queryParam("size", 10)
        .build())
    .retrieve()
    .bodyToFlux(User.class);

// GET с headers
Mono<String> response = webClient
    .get()
    .uri("/data")
    .header("Authorization", "Bearer " + token)
    .retrieve()
    .bodyToMono(String.class);
```

**POST запросы:**
```java
// POST с телом
Mono<User> createdUser = webClient
    .post()
    .uri("/users")
    .bodyValue(newUser)
    .retrieve()
    .bodyToMono(User.class);

// POST с Mono
Mono<User> result = webClient
    .post()
    .uri("/users")
    .body(userMono, User.class)
    .retrieve()
    .bodyToMono(User.class);

// POST с FormData
MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
formData.add("username", "john");
formData.add("password", "secret");

Mono<String> response = webClient
    .post()
    .uri("/login")
    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
    .bodyValue(formData)
    .retrieve()
    .bodyToMono(String.class);
```

**PUT, DELETE, PATCH:**
```java
// PUT
Mono<User> updated = webClient
    .put()
    .uri("/users/{id}", userId)
    .bodyValue(updatedUser)
    .retrieve()
    .bodyToMono(User.class);

// DELETE
Mono<Void> deleted = webClient
    .delete()
    .uri("/users/{id}", userId)
    .retrieve()
    .bodyToMono(Void.class);

// PATCH
Mono<User> patched = webClient
    .patch()
    .uri("/users/{id}", userId)
    .bodyValue(partialUpdate)
    .retrieve()
    .bodyToMono(User.class);
```

### Конфигурация WebClient

**Настройка таймаутов:**
```java
@Configuration
public class WebClientConfig {
    
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(5))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(5))
                    .addHandlerLast(new WriteTimeoutHandler(5))
            );
        
        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
```

**Настройка пула соединений:**
```java
ConnectionProvider provider = ConnectionProvider.builder("custom")
    .maxConnections(100)
    .maxIdleTime(Duration.ofSeconds(20))
    .maxLifeTime(Duration.ofSeconds(60))
    .pendingAcquireTimeout(Duration.ofSeconds(60))
    .evictInBackground(Duration.ofSeconds(120))
    .build();

HttpClient httpClient = HttpClient.create(provider);

WebClient webClient = WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .build();
```

**Фильтры:**
```java
WebClient webClient = WebClient.builder()
    .filter((request, next) -> {
        // Логирование запроса
        log.info("Request: {} {}", request.method(), request.url());
        return next.exchange(request);
    })
    .filter(ExchangeFilterFunctions.basicAuthentication("user", "password"))
    .build();

// Кастомный фильтр для добавления токена
ExchangeFilterFunction authFilter = (request, next) -> {
    ClientRequest filtered = ClientRequest.from(request)
        .header("Authorization", "Bearer " + getToken())
        .build();
    return next.exchange(filtered);
};

WebClient client = WebClient.builder()
    .filter(authFilter)
    .build();
```

### Обработка ответов

**retrieve() vs exchange():**

```java
// retrieve() - упрощённый способ (рекомендуется)
Mono<User> user = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .bodyToMono(User.class);

// exchange() - полный контроль над ответом (deprecated в WebClient)
// Используйте exchangeToMono/exchangeToFlux вместо exchange()
Mono<User> user = webClient.get()
    .uri("/users/{id}", id)
    .exchangeToMono(response -> {
        if (response.statusCode().equals(HttpStatus.OK)) {
            return response.bodyToMono(User.class);
        } else if (response.statusCode().is4xxClientError()) {
            return Mono.error(new RuntimeException("Client error"));
        } else {
            return response.createException()
                .flatMap(Mono::error);
        }
    });
```

**Получение headers и статуса:**
```java
Mono<ResponseEntity<User>> entity = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .toEntity(User.class);

entity.subscribe(response -> {
    HttpStatus status = response.getStatusCode();
    HttpHeaders headers = response.getHeaders();
    User body = response.getBody();
});
```

### Обработка ошибок в WebClient

```java
// onStatus - обработка по статус-кодам
Mono<User> user = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .onStatus(
        HttpStatus::is4xxClientError,
        response -> Mono.error(new ClientException("Client error"))
    )
    .onStatus(
        HttpStatus::is5xxServerError,
        response -> Mono.error(new ServerException("Server error"))
    )
    .bodyToMono(User.class);

// Детальная обработка с телом ошибки
Mono<User> user = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .onStatus(
        status -> status.value() == 404,
        response -> response.bodyToMono(ErrorResponse.class)
            .flatMap(error -> Mono.error(
                new UserNotFoundException(error.getMessage())
            ))
    )
    .bodyToMono(User.class);

// Обработка в цепочке
Mono<User> user = webClient.get()
    .uri("/users/{id}", id)
    .retrieve()
    .bodyToMono(User.class)
    .onErrorResume(WebClientResponseException.class, ex -> {
        log.error("Error: {}", ex.getResponseBodyAsString());
        return Mono.just(new User()); // fallback
    })
    .timeout(Duration.ofSeconds(5))
    .retry(3);
```

**Глобальная обработка ошибок:**
```java
WebClient webClient = WebClient.builder()
    .filter((request, next) -> 
        next.exchange(request)
            .onErrorResume(WebClientResponseException.class, ex -> {
                log.error("Global error handler: {}", ex.getMessage());
                return Mono.error(new CustomException(ex));
            })
    )
    .build();
```

## Reactive Repositories

Spring Data поддерживает реактивные репозитории для MongoDB, Cassandra, Redis, R2DBC:

**ReactiveCrudRepository:**
```java
public interface UserRepository extends ReactiveCrudRepository<User, String> {
    
    Flux<User> findByLastName(String lastName);
    
    Mono<User> findByEmail(String email);
    
    @Query("SELECT * FROM users WHERE age > :age")
    Flux<User> findUsersOlderThan(int age);
}
```

**Использование:**
```java
@Service
public class UserService {
    
    private final UserRepository userRepository;
    
    public Flux<User> findAll() {
        return userRepository.findAll();
    }
    
    public Mono<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    public Mono<User> save(User user) {
        return userRepository.save(user);
    }
    
    public Mono<Void> deleteById(String id) {
        return userRepository.deleteById(id);
    }
}
```

**R2DBC (Reactive Relational Database Connectivity):**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>
```

```java
public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    
    @Query("SELECT * FROM orders WHERE customer_id = :customerId")
    Flux<Order> findByCustomerId(Long customerId);
    
    @Modifying
    @Query("UPDATE orders SET status = :status WHERE id = :id")
    Mono<Integer> updateStatus(Long id, String status);
}
```

## Backpressure

**Backpressure** — механизм контроля скорости потока данных, когда consumer не успевает обрабатывать данные от producer.

**Стратегии обработки:**

```java
// BUFFER - буферизация (может привести к OutOfMemoryError)
Flux<Integer> buffered = flux.onBackpressureBuffer();

// BUFFER с ограничением
Flux<Integer> limitedBuffer = flux
    .onBackpressureBuffer(100, 
        dropped -> log.warn("Dropped: {}", dropped));

// DROP - отбросить новые элементы
Flux<Integer> dropped = flux.onBackpressureDrop();

// LATEST - хранить только последний элемент
Flux<Integer> latest = flux.onBackpressureLatest();

// ERROR - выбросить ошибку
Flux<Integer> error = flux.onBackpressureError();
```

**Управление запросами:**
```java
// limitRate - ограничение скорости запросов
Flux<Integer> limited = Flux.range(1, 1000)
    .limitRate(10); // запрашивать по 10 элементов

// Ручное управление через BaseSubscriber
flux.subscribe(new BaseSubscriber<Integer>() {
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(1); // запросить 1 элемент
    }
    
    @Override
    protected void hookOnNext(Integer value) {
        process(value);
        request(1); // запросить следующий
    }
});
```

## Тестирование реактивного кода

**StepVerifier** — основной инструмент для тестирования реактивных потоков:

```java
@Test
public void testFlux() {
    Flux<String> flux = Flux.just("A", "B", "C");
    
    StepVerifier.create(flux)
        .expectNext("A")
        .expectNext("B")
        .expectNext("C")
        .expectComplete()
        .verify();
}

@Test
public void testMono() {
    Mono<String> mono = Mono.just("Hello");
    
    StepVerifier.create(mono)
        .expectNext("Hello")
        .verifyComplete();
}

@Test
public void testError() {
    Flux<String> flux = Flux.error(new RuntimeException("Error"));
    
    StepVerifier.create(flux)
        .expectError(RuntimeException.class)
        .verify();
}

@Test
public void testWithVirtualTime() {
    Flux<Long> flux = Flux.interval(Duration.ofHours(1))
        .take(3);
    
    StepVerifier.withVirtualTime(() -> flux)
        .expectSubscription()
        .thenAwait(Duration.ofHours(3))
        .expectNext(0L, 1L, 2L)
        .verifyComplete();
}
```

**Тестирование WebClient:**
```java
@Test
public void testWebClient() {
    // MockWebServer для тестов
    MockResponse mockResponse = new MockResponse()
        .setBody("{\"id\":1,\"name\":\"John\"}")
        .addHeader("Content-Type", "application/json");
    
    mockWebServer.enqueue(mockResponse);
    
    WebClient client = WebClient.create(mockWebServer.url("/").toString());
    
    Mono<User> user = client.get()
        .uri("/users/1")
        .retrieve()
        .bodyToMono(User.class);
    
    StepVerifier.create(user)
        .expectNextMatches(u -> u.getName().equals("John"))
        .verifyComplete();
}
```

**Тестирование контроллеров:**
```java
@WebFluxTest(UserController.class)
class UserControllerTest {
    
    @Autowired
    private WebTestClient webTestClient;
    
    @MockBean
    private UserService userService;
    
    @Test
    void testGetUser() {
        User user = new User("1", "John");
        when(userService.findById("1")).thenReturn(Mono.just(user));
        
        webTestClient.get()
            .uri("/users/1")
            .exchange()
            .expectStatus().isOk()
            .expectBody(User.class)
            .value(u -> assertEquals("John", u.getName()));
    }
}
```

## Практические паттерны

**Параллельная обработка:**
```java
// Параллельная обработка с flatMap
Flux<Result> results = Flux.fromIterable(userIds)
    .flatMap(id -> processUser(id), 10) // concurrency = 10
    .subscribeOn(Schedulers.parallel());

// Параллельный поток
Flux<Result> parallel = Flux.fromIterable(data)
    .parallel()
    .runOn(Schedulers.parallel())
    .map(this::process)
    .sequential();
```

**Кэширование:**
```java
// cache() - кэширование результатов
Mono<User> cachedUser = userService.findById(id)
    .cache(Duration.ofMinutes(5));

// share() - multicast для нескольких подписчиков
Flux<Event> sharedEvents = eventService.getEvents()
    .share();
```

**Retry с exponential backoff:**
```java
Mono<String> result = webClient.get()
    .uri("/api/data")
    .retrieve()
    .bodyToMono(String.class)
    .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
        .maxBackoff(Duration.ofSeconds(10))
        .jitter(0.5)
        .filter(throwable -> throwable instanceof TimeoutException)
    );
```

**Объединение нескольких источников:**
```java
// Параллельный запрос к нескольким API
Mono<CombinedData> combined = Mono.zip(
    webClient.get().uri("/api/users").retrieve().bodyToMono(UserData.class),
    webClient.get().uri("/api/orders").retrieve().bodyToMono(OrderData.class),
    webClient.get().uri("/api/products").retrieve().bodyToMono(ProductData.class)
).map(tuple -> new CombinedData(
    tuple.getT1(),
    tuple.getT2(),
    tuple.getT3()
));
```

**Fallback цепочки:**
```java
Mono<User> user = primaryService.getUser(id)
    .onErrorResume(ex -> secondaryService.getUser(id))
    .onErrorResume(ex -> cacheService.getUser(id))
    .onErrorReturn(User.getDefault());
```

**Batch обработка:**
```java
Flux<List<User>> batches = userFlux
    .buffer(100) // группировка по 100
    .flatMap(batch -> saveBatch(batch));

// Окна по времени
Flux<Flux<Event>> windows = eventFlux
    .window(Duration.ofSeconds(5))
    .flatMap(window -> window.collectList())
    .map(this::processEventBatch);
```

## Когда использовать реактивное программирование

**Используйте реактивный подход когда:**
- ✅ Высокая нагрузка с большим количеством одновременных подключений
- ✅ I/O-bound операции (работа с БД, внешние API, файлы)
- ✅ Streaming данных (Server-Sent Events, WebSocket)
- ✅ Микросервисная архитектура с множеством вызовов между сервисами
- ✅ Нужен полный контроль над backpressure
- ✅ Композиция асинхронных операций

**НЕ используйте реактивный подход когда:**
- ❌ CPU-bound задачи (вычисления, обработка данных в памяти)
- ❌ Простое CRUD-приложение с низкой нагрузкой
- ❌ Команда не знакома с реактивным программированием
- ❌ Блокирующие зависимости (JDBC, некоторые библиотеки)
- ❌ Необходима транзакционность с несколькими ресурсами

**Сравнение производительности:**
```
Традиционный подход (Spring MVC + JDBC):
- 200 потоков
- 10,000 одновременных запросов
- ~2GB памяти

Реактивный подход (Spring WebFlux + R2DBC):
- 8-16 потоков (event loop)
- 10,000 одновременных запросов
- ~500MB памяти
```

> **Важно**: Реактивный стек эффективен только если ВСЯ цепочка неблокирующая. Один блокирующий вызов может свести на нет все преимущества.

## Практические упражнения

1. **Базовые операторы:**
   - Создайте Flux из списка чисел 1-100
   - Отфильтруйте чётные числа
   - Возведите в квадрат
   - Выведите результат

2. **WebClient:**
   - Создайте WebClient для публичного API (например, JSONPlaceholder)
   - Получите список пользователей
   - Для каждого пользователя получите его посты
   - Объедините данные

3. **Обработка ошибок:**
   - Создайте Flux, который периодически выбрасывает ошибку
   - Реализуйте retry с backoff
   - Добавьте fallback значение

4. **Backpressure:**
   - Создайте быстрый producer (Flux.interval)
   - Создайте медленный consumer
   - Примените различные стратегии backpressure

5. **Реактивный REST API:**
   - Создайте WebFlux приложение с CRUD операциями
   - Используйте реактивный репозиторий (R2DBC или MongoDB)
   - Добавьте обработку ошибок
   - Напишите тесты с WebTestClient

## Вопросы на собеседовании

1. **Что такое реактивное программирование и какие проблемы оно решает?**
   - Парадигма работы с асинхронными потоками данных
   - Решает проблемы масштабирования, эффективности использования ресурсов
   - Backpressure, композиция асинхронных операций

2. **В чём разница между Mono и Flux?**
   - Mono: 0 или 1 элемент (аналог Optional/Future)
   - Flux: 0 до N элементов (аналог Stream)

3. **Что такое backpressure и как он работает?**
   - Механизм контроля скорости потока данных
   - Consumer запрашивает определённое количество элементов
   - Стратегии: buffer, drop, latest, error

4. **В чём разница между subscribeOn и publishOn?**
   - subscribeOn: влияет на поток выполнения всей цепочки
   - publishOn: влияет только на downstream операторы

5. **Когда использовать flatMap, а когда concatMap?**
   - flatMap: параллельная обработка, порядок не гарантируется
   - concatMap: последовательная обработка, порядок сохраняется

6. **Чем WebFlux отличается от Spring MVC?**
   - WebFlux: non-blocking, event loop, Netty
   - Spring MVC: blocking, thread-per-request, Servlet API

7. **Что такое холодные и горячие потоки?**
   - Холодные: начинают эмитить данные при подписке (Mono.just)
   - Горячие: эмитят независимо от подписчиков (Flux.interval)

8. **Как тестировать реактивный код?**
   - StepVerifier для проверки потоков
   - WebTestClient для контроллеров
   - Virtual time для временных операторов

9. **В чём разница между retrieve() и exchange() в WebClient?**
   - retrieve(): упрощённый API, автоматическая обработка статусов
   - exchange(): полный контроль (deprecated, использовать exchangeToMono)

10. **Можно ли использовать JDBC в реактивном приложении?**
    - JDBC блокирующий, нарушает реактивность
    - Нужно использовать R2DBC для реактивной работы с БД
    - Или изолировать JDBC вызовы с subscribeOn(Schedulers.boundedElastic())

## Дополнительные материалы

**Официальная документация:**
- [Project Reactor Reference](https://projectreactor.io/docs/core/release/reference/)
- [Spring WebFlux Documentation](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [RxJava Documentation](https://github.com/ReactiveX/RxJava/wiki)
- [Reactive Streams Specification](https://www.reactive-streams.org/)

**Книги:**
- "Reactive Programming with RxJava" - Tomasz Nurkiewicz, Ben Christensen
- "Hands-On Reactive Programming in Spring 5" - Oleh Dokuka, Igor Lozynskyi
- "Reactive Design Patterns" - Roland Kuhn

**Онлайн курсы:**
- [Reactive Programming with Spring WebFlux](https://www.udemy.com/course/reactive-programming-with-spring-framework-5/)
- [RxJava for Android Developers](https://www.udemy.com/course/rxjava-for-android-developers/)

**Полезные ссылки:**
- [Reactor 3 Reference Guide](https://projectreactor.io/docs/core/release/reference/)
- [Spring WebFlux Tutorial](https://www.baeldung.com/spring-webflux)
- [Introduction to RxJava](https://www.baeldung.com/rx-java)
- [Reactive Streams JVM](https://github.com/reactive-streams/reactive-streams-jvm)

---

[← Назад к разделу Spring](README.md)
