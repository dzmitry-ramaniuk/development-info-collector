# Observer (Наблюдатель)

Observer — поведенческий паттерн проектирования, который создает механизм подписки, позволяющий одним объектам следить и реагировать на события, происходящие в других объектах.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Примеры использования](#примеры-использования)
6. [Reactive Programming](#reactive-programming)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Observer используется когда:
- Изменение одного объекта требует изменения других, но неизвестно сколько объектов нужно изменить
- Объект должен уведомлять другие объекты, не делая предположений об этих объектах
- Нужна слабая связь между объектами

**Типичные примеры использования:**
- Системы событий (event listeners)
- Model-View-Controller (MVC)
- Реактивное программирование (RxJava, Project Reactor)
- Push-уведомления
- Подписка на изменения данных

## Проблема, которую решает

### Проблема: Жесткая связанность объектов

```java
public class WeatherStation {
    private double temperature;
    private Display display;
    private StatisticsCollector stats;
    private Logger logger;
    
    public WeatherStation(Display display, StatisticsCollector stats, Logger logger) {
        this.display = display;
        this.stats = stats;
        this.logger = logger;
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        
        // Жесткая привязка к конкретным классам
        display.updateDisplay(temperature);
        stats.updateStatistics(temperature);
        logger.log("Temperature changed: " + temperature);
        
        // Чтобы добавить новый обработчик, нужно менять этот код!
    }
}
```

**Проблемы:**
- Жесткая связь между WeatherStation и всеми обработчиками
- Невозможно добавить/удалить обработчики динамически
- Нарушение Open/Closed Principle
- WeatherStation должен знать о всех обработчиках

### Решение: Observer

Создать механизм подписки, где наблюдатели могут подписываться и отписываться.

## Структура паттерна

```java
// Наблюдатель (Observer)
interface Observer {
    void update(double temperature);
}

// Субъект (Subject/Observable)
interface Subject {
    void attach(Observer observer);
    void detach(Observer observer);
    void notifyObservers();
}

// Конкретный субъект
class WeatherStation implements Subject {
    private double temperature;
    private List<Observer> observers = new ArrayList<>();
    
    @Override
    public void attach(Observer observer) {
        observers.add(observer);
    }
    
    @Override
    public void detach(Observer observer) {
        observers.remove(observer);
    }
    
    @Override
    public void notifyObservers() {
        for (Observer observer : observers) {
            observer.update(temperature);
        }
    }
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        notifyObservers();  // Уведомляем всех наблюдателей
    }
    
    public double getTemperature() {
        return temperature;
    }
}

// Конкретные наблюдатели
class Display implements Observer {
    @Override
    public void update(double temperature) {
        System.out.println("Display: Temperature is " + temperature + "°C");
    }
}

class StatisticsCollector implements Observer {
    private List<Double> temperatures = new ArrayList<>();
    
    @Override
    public void update(double temperature) {
        temperatures.add(temperature);
        double avg = temperatures.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        System.out.println("Statistics: Average temperature is " + avg + "°C");
    }
}

class AlertSystem implements Observer {
    private static final double HIGH_TEMP = 30.0;
    private static final double LOW_TEMP = 0.0;
    
    @Override
    public void update(double temperature) {
        if (temperature > HIGH_TEMP) {
            System.out.println("ALERT: High temperature! " + temperature + "°C");
        } else if (temperature < LOW_TEMP) {
            System.out.println("ALERT: Low temperature! " + temperature + "°C");
        }
    }
}

// Использование
WeatherStation station = new WeatherStation();

Observer display = new Display();
Observer stats = new StatisticsCollector();
Observer alerts = new AlertSystem();

// Подписываем наблюдателей
station.attach(display);
station.attach(stats);
station.attach(alerts);

// Изменяем температуру - все наблюдатели получат уведомление
station.setTemperature(25.0);
station.setTemperature(32.0);

// Можем отписать наблюдателя
station.detach(alerts);
station.setTemperature(15.0);  // alerts не получит уведомление
```

## Реализация

### Пример 1: Система событий с типизированными событиями

```java
// Базовое событие
abstract class Event {
    private final long timestamp;
    
    protected Event() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}

// Конкретные события
class UserCreatedEvent extends Event {
    private final String username;
    private final String email;
    
    public UserCreatedEvent(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}

class UserDeletedEvent extends Event {
    private final String username;
    
    public UserDeletedEvent(String username) {
        this.username = username;
    }
    
    public String getUsername() { return username; }
}

// Обобщенный интерфейс слушателя событий
@FunctionalInterface
interface EventListener<T extends Event> {
    void onEvent(T event);
}

// Event Bus - диспетчер событий
class EventBus {
    private final Map<Class<? extends Event>, List<EventListener>> listeners = new HashMap<>();
    
    public <T extends Event> void subscribe(Class<T> eventType, EventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }
    
    public <T extends Event> void unsubscribe(Class<T> eventType, EventListener<T> listener) {
        List<EventListener> eventListeners = listeners.get(eventType);
        if (eventListeners != null) {
            eventListeners.remove(listener);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Event> void publish(T event) {
        List<EventListener> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (EventListener listener : eventListeners) {
                listener.onEvent(event);
            }
        }
    }
}

// Слушатели
class EmailNotificationService {
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("Sending welcome email to " + event.getEmail());
    }
}

class AuditLogger {
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("AUDIT: User created - " + event.getUsername());
    }
    
    public void handleUserDeleted(UserDeletedEvent event) {
        System.out.println("AUDIT: User deleted - " + event.getUsername());
    }
}

// Использование
EventBus eventBus = new EventBus();

EmailNotificationService emailService = new EmailNotificationService();
AuditLogger auditLogger = new AuditLogger();

// Подписываемся на события
eventBus.subscribe(UserCreatedEvent.class, emailService::handleUserCreated);
eventBus.subscribe(UserCreatedEvent.class, auditLogger::handleUserCreated);
eventBus.subscribe(UserDeletedEvent.class, auditLogger::handleUserDeleted);

// Публикуем события
eventBus.publish(new UserCreatedEvent("john_doe", "john@example.com"));
eventBus.publish(new UserDeletedEvent("old_user"));
```

### Пример 2: Property Change Listener (из Java Beans)

```java
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

class User {
    private String name;
    private String email;
    private int age;
    
    // Поддержка property change
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
    
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }
    
    // Методы с уведомлением об изменениях
    public void setName(String name) {
        String oldName = this.name;
        this.name = name;
        support.firePropertyChange("name", oldName, name);
    }
    
    public void setEmail(String email) {
        String oldEmail = this.email;
        this.email = email;
        support.firePropertyChange("email", oldEmail, email);
    }
    
    public void setAge(int age) {
        int oldAge = this.age;
        this.age = age;
        support.firePropertyChange("age", oldAge, age);
    }
    
    // Геттеры
    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getAge() { return age; }
}

// Использование
User user = new User();

// Подписка на все изменения
user.addPropertyChangeListener(evt -> {
    System.out.println("Property changed: " + evt.getPropertyName() +
                      " from " + evt.getOldValue() + " to " + evt.getNewValue());
});

// Подписка на конкретное свойство
user.addPropertyChangeListener("email", evt -> {
    System.out.println("Email changed to: " + evt.getNewValue());
    // Отправить email подтверждение
});

user.setName("John Doe");
user.setEmail("john@example.com");
user.setAge(30);
```

### Пример 3: Push vs Pull модели

```java
// PUSH модель - субъект отправляет данные

interface PushObserver {
    void update(String data);
}

class PushSubject {
    private List<PushObserver> observers = new ArrayList<>();
    
    public void attach(PushObserver observer) {
        observers.add(observer);
    }
    
    public void notifyObservers(String data) {
        for (PushObserver observer : observers) {
            observer.update(data);  // Отправляем данные
        }
    }
}

// PULL модель - наблюдатель запрашивает данные

interface PullObserver {
    void update(PullSubject subject);
}

class PullSubject {
    private String data;
    private List<PullObserver> observers = new ArrayList<>();
    
    public void attach(PullObserver observer) {
        observers.add(observer);
    }
    
    public void setData(String data) {
        this.data = data;
        notifyObservers();
    }
    
    public String getData() {
        return data;  // Наблюдатель может получить данные
    }
    
    public void notifyObservers() {
        for (PullObserver observer : observers) {
            observer.update(this);  // Отправляем ссылку на субъект
        }
    }
}

// Использование Pull модели
class PullObserverImpl implements PullObserver {
    @Override
    public void update(PullSubject subject) {
        String data = subject.getData();  // Запрашиваем нужные данные
        System.out.println("Received: " + data);
    }
}
```

## Примеры использования

### Java Swing/AWT Event Listeners

```java
JButton button = new JButton("Click me");

// Добавляем слушателей (Observer)
button.addActionListener(e -> {
    System.out.println("Button clicked!");
});

button.addActionListener(e -> {
    System.out.println("Another listener");
});

// Можно удалить слушателя
ActionListener listener = e -> System.out.println("Temp listener");
button.addActionListener(listener);
button.removeActionListener(listener);
```

### JavaFX Properties

```java
StringProperty name = new SimpleStringProperty();

// Подписка на изменения
name.addListener((observable, oldValue, newValue) -> {
    System.out.println("Name changed from " + oldValue + " to " + newValue);
});

name.set("John");
name.set("Jane");
```

### Spring Framework Events

```java
// Событие
public class UserRegisteredEvent extends ApplicationEvent {
    private final String username;
    
    public UserRegisteredEvent(Object source, String username) {
        super(source);
        this.username = username;
    }
    
    public String getUsername() {
        return username;
    }
}

// Слушатель
@Component
public class EmailService {
    @EventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        System.out.println("Sending email to " + event.getUsername());
    }
}

// Публикация события
@Service
public class UserService {
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public void registerUser(String username) {
        // Регистрация пользователя
        // ...
        
        // Публикуем событие
        eventPublisher.publishEvent(new UserRegisteredEvent(this, username));
    }
}
```

### Observable/Observer в Java (deprecated с Java 9)

```java
import java.util.Observable;
import java.util.Observer;

// Устаревший способ (deprecated)
class WeatherData extends Observable {
    private double temperature;
    
    public void setTemperature(double temperature) {
        this.temperature = temperature;
        setChanged();
        notifyObservers(temperature);
    }
}

Observer display = (o, arg) -> {
    System.out.println("Temperature: " + arg);
};

WeatherData weather = new WeatherData();
weather.addObserver(display);
weather.setTemperature(25.0);
```

## Reactive Programming

Observer является основой реактивного программирования:

### RxJava пример

```java
// Observable - источник событий
Observable<String> observable = Observable.create(emitter -> {
    emitter.onNext("Event 1");
    emitter.onNext("Event 2");
    emitter.onNext("Event 3");
    emitter.onComplete();
});

// Observer - подписчик
observable.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Complete!")
);

// Операторы трансформации
Observable.just(1, 2, 3, 4, 5)
    .filter(n -> n % 2 == 0)
    .map(n -> n * 10)
    .subscribe(n -> System.out.println(n));
```

### Project Reactor (Spring WebFlux)

```java
// Flux - поток множества элементов
Flux<String> flux = Flux.just("A", "B", "C", "D");

flux.subscribe(
    item -> System.out.println("Received: " + item),
    error -> System.err.println("Error: " + error),
    () -> System.out.println("Complete!")
);

// Mono - поток одного элемента
Mono<String> mono = Mono.just("Hello");

mono.subscribe(System.out::println);

// Реактивная обработка данных
Flux.range(1, 10)
    .filter(n -> n % 2 == 0)
    .map(n -> "Number: " + n)
    .subscribe(System.out::println);
```

## Преимущества и недостатки

### Преимущества

✅ **Слабая связанность**
- Субъект не знает о конкретных наблюдателях
- Наблюдатели могут добавляться/удаляться динамически

✅ **Open/Closed Principle**
- Можно добавлять новых наблюдателей без изменения субъекта
- Расширяемость системы

✅ **Динамическая подписка**
- Наблюдатели могут подписываться и отписываться во время выполнения
- Гибкость в управлении зависимостями

✅ **Broadcast коммуникация**
- Один субъект может уведомлять множество наблюдателей
- Эффективная рассылка уведомлений

### Недостатки

❌ **Непредсказуемый порядок уведомлений**
- Наблюдатели уведомляются в порядке подписки
- Может привести к проблемам, если порядок важен

❌ **Утечки памяти**
- Если не отписаться, объект остается в памяти
- Нужно явно управлять подписками

❌ **Производительность**
- Уведомление множества наблюдателей может быть медленным
- Особенно если наблюдатели выполняют тяжелые операции

❌ **Сложность отладки**
- Непонятно, кто и когда изменил состояние
- Цепочки уведомлений сложно отслеживать

❌ **Неожиданные обновления**
- Наблюдатели могут получать обновления, когда не ожидают
- Может привести к каскадным обновлениям

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Observer и когда его использовать?**

*Ответ:* Observer — это поведенческий паттерн, который создает механизм подписки для оповещения множества объектов о событиях в наблюдаемом объекте. Используется когда:
- Изменение одного объекта требует изменения других
- Неизвестно заранее, сколько объектов нужно уведомить
- Нужна слабая связь между объектами

**2. Какие компоненты входят в паттерн Observer?**

*Ответ:*
- **Subject (Observable)** — объект, за которым наблюдают, хранит список наблюдателей
- **Observer** — интерфейс для объектов, которые должны быть уведомлены
- **ConcreteObserver** — конкретные наблюдатели, реализующие интерфейс Observer

**3. Приведите примеры Observer из Java**

*Ответ:*
- Event Listeners в Swing/AWT (`ActionListener`, `MouseListener`)
- `PropertyChangeListener` в JavaBeans
- `Observable`/`Observer` (deprecated с Java 9)
- Reactive Streams (RxJava, Project Reactor)
- Spring Framework Events

**4. В чем разница между Push и Pull моделями Observer?**

*Ответ:*
- **Push**: Субъект отправляет данные наблюдателям (`update(data)`)
- **Pull**: Субъект отправляет только уведомление, наблюдатели сами запрашивают данные (`update(subject)`)
- Push проще, Pull более гибкий (наблюдатель выбирает нужные данные)

### Продвинутые вопросы

**5. Как избежать утечек памяти при использовании Observer?**

*Ответ:*
```java
// Проблема: наблюдатель не отписался
subject.attach(observer);  // observer остается в памяти

// Решение: явная отписка
subject.detach(observer);

// Или использовать WeakReference
class WeakObserverList {
    private List<WeakReference<Observer>> observers = new ArrayList<>();
    
    public void attach(Observer observer) {
        observers.add(new WeakReference<>(observer));
    }
    
    public void notifyObservers() {
        observers.removeIf(ref -> ref.get() == null);  // Удаляем мертвые ссылки
        for (WeakReference<Observer> ref : observers) {
            Observer observer = ref.get();
            if (observer != null) {
                observer.update();
            }
        }
    }
}
```

**6. Какие проблемы могут возникнуть с многопоточностью в Observer?**

*Ответ:*
- **Race conditions** при одновременной подписке/отписке и уведомлении
- **ConcurrentModificationException** при изменении списка наблюдателей во время итерации
- **Блокировки** если наблюдатели выполняют долгие операции

Решение:
```java
class ThreadSafeSubject {
    private final List<Observer> observers = new CopyOnWriteArrayList<>();
    
    public void notifyObservers() {
        for (Observer observer : observers) {  // Безопасная итерация
            observer.update();
        }
    }
}
```

**7. В чем разница между Observer и Mediator?**

*Ответ:*
- **Observer**: Один-ко-многим, односторонняя связь (Subject → Observers)
- **Mediator**: Много-ко-многим, двусторонняя связь через посредника
- Observer для broadcast уведомлений, Mediator для координации взаимодействий

**8. Как Observer используется в MVC?**

*Ответ:* В MVC Observer связывает Model и View:
- **Model** — Subject, содержит данные
- **View** — Observer, отображает данные
- Когда Model меняется, View автоматически обновляется
```java
model.addObserver(view);
model.setData("new data");  // View автоматически обновится
```

**9. Что такое Reactive Streams и как они связаны с Observer?**

*Ответ:* Reactive Streams — это спецификация для асинхронной обработки потоков данных с поддержкой backpressure. Основа — паттерн Observer:
- **Publisher** — Observable, источник данных
- **Subscriber** — Observer, потребитель данных
- **Subscription** — управление подпиской
- Добавляет контроль потока данных (backpressure)

**10. Как реализовать Observer с фильтрацией событий?**

*Ответ:*
```java
interface EventFilter {
    boolean shouldNotify(Event event);
}

class FilteredObserver implements Observer {
    private final EventFilter filter;
    private final Consumer<Event> handler;
    
    public FilteredObserver(EventFilter filter, Consumer<Event> handler) {
        this.filter = filter;
        this.handler = handler;
    }
    
    @Override
    public void update(Event event) {
        if (filter.shouldNotify(event)) {
            handler.accept(event);
        }
    }
}

// Использование
subject.attach(new FilteredObserver(
    event -> event.getType() == EventType.USER_CREATED,
    event -> System.out.println("User created: " + event)
));
```

---

[← Назад к разделу Поведенческие паттерны](README.md)
