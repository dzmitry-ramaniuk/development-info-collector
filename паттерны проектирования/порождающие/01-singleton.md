# Singleton (Одиночка)

Singleton — порождающий паттерн проектирования, который гарантирует, что у класса есть только один экземпляр, и предоставляет глобальную точку доступа к этому экземпляру.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Структура паттерна](#структура-паттерна)
3. [Способы реализации](#способы-реализации)
   - [Eager Initialization](#eager-initialization)
   - [Lazy Initialization](#lazy-initialization)
   - [Thread-Safe Singleton](#thread-safe-singleton)
   - [Double-Checked Locking](#double-checked-locking)
   - [Bill Pugh Singleton](#bill-pugh-singleton)
   - [Enum Singleton](#enum-singleton)
4. [Проблемы и решения](#проблемы-и-решения)
5. [Примеры использования](#примеры-использования)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Singleton используется когда:
- Необходим строго один экземпляр класса
- Нужна глобальная точка доступа к этому экземпляру
- Экземпляр должен расширяться путём наследования без изменения кода клиентов

**Типичные примеры использования:**
- Логгеры
- Конфигурационные менеджеры
- Пулы соединений с БД
- Кэши
- Фабрики и билдеры объектов
- Драйверы устройств

## Структура паттерна

```java
public class Singleton {
    // Статическая переменная для хранения единственного экземпляра
    private static Singleton instance;
    
    // Приватный конструктор предотвращает создание экземпляров извне
    private Singleton() {
        // Инициализация
    }
    
    // Публичный метод для получения экземпляра
    public static Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
    
    // Бизнес-логика
    public void doSomething() {
        // ...
    }
}
```

## Способы реализации

### Eager Initialization

Самый простой способ — создание экземпляра при загрузке класса.

```java
public class EagerSingleton {
    // Экземпляр создаётся при загрузке класса
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    
    private EagerSingleton() {
        // Инициализация
    }
    
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}
```

**Преимущества:**
- Простота реализации
- Потокобезопасность (JVM гарантирует)
- Не требует синхронизации

**Недостатки:**
- Экземпляр создаётся всегда, даже если не используется
- Нет контроля над моментом создания
- Невозможна обработка исключений при создании

### Lazy Initialization

Экземпляр создаётся только при первом обращении.

```java
public class LazySingleton {
    private static LazySingleton instance;
    
    private LazySingleton() {
        // Инициализация
    }
    
    public static LazySingleton getInstance() {
        if (instance == null) {
            instance = new LazySingleton();
        }
        return instance;
    }
}
```

**Преимущества:**
- Экземпляр создаётся только при необходимости
- Экономия ресурсов

**Недостатки:**
- **НЕ потокобезопасен!** Может создать несколько экземпляров в многопоточной среде

### Thread-Safe Singleton

Синхронизация метода для обеспечения потокобезопасности.

```java
public class ThreadSafeSingleton {
    private static ThreadSafeSingleton instance;
    
    private ThreadSafeSingleton() {
        // Инициализация
    }
    
    // Синхронизированный метод
    public static synchronized ThreadSafeSingleton getInstance() {
        if (instance == null) {
            instance = new ThreadSafeSingleton();
        }
        return instance;
    }
}
```

**Преимущества:**
- Потокобезопасен
- Ленивая инициализация

**Недостатки:**
- Снижение производительности из-за синхронизации при каждом вызове
- Блокировка нужна только при первом создании

### Double-Checked Locking

Оптимизация Thread-Safe подхода — проверка перед синхронизацией.

```java
public class DoubleCheckedSingleton {
    // volatile обязателен для корректной работы!
    private static volatile DoubleCheckedSingleton instance;
    
    private DoubleCheckedSingleton() {
        // Инициализация
    }
    
    public static DoubleCheckedSingleton getInstance() {
        // Первая проверка без синхронизации
        if (instance == null) {
            synchronized (DoubleCheckedSingleton.class) {
                // Вторая проверка с синхронизацией
                if (instance == null) {
                    instance = new DoubleCheckedSingleton();
                }
            }
        }
        return instance;
    }
}
```

**Важно:** Ключевое слово `volatile` обязательно! Без него возможны проблемы из-за reordering инструкций.

**Преимущества:**
- Потокобезопасен
- Ленивая инициализация
- Высокая производительность после инициализации

**Недостатки:**
- Более сложная реализация
- Работает корректно только в Java 5+ (с правильной реализацией volatile)

### Bill Pugh Singleton

Использование вложенного статического класса (Initialization-on-demand holder idiom).

```java
public class BillPughSingleton {
    
    private BillPughSingleton() {
        // Инициализация
    }
    
    // Вложенный статический класс
    private static class SingletonHolder {
        private static final BillPughSingleton INSTANCE = new BillPughSingleton();
    }
    
    public static BillPughSingleton getInstance() {
        return SingletonHolder.INSTANCE;
    }
}
```

**Как это работает:**
- Внутренний класс `SingletonHolder` не загружается при загрузке внешнего класса
- Загрузка происходит только при первом обращении к `getInstance()`
- JVM гарантирует потокобезопасность при инициализации статических полей

**Преимущества:**
- Потокобезопасен (гарантия JVM)
- Ленивая инициализация
- Высокая производительность
- Не требует синхронизации
- Рекомендуемый подход для большинства случаев

### Enum Singleton

Использование enum для создания Singleton (рекомендация Joshua Bloch).

```java
public enum EnumSingleton {
    INSTANCE;
    
    // Поля и методы
    private int value;
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public void doSomething() {
        System.out.println("Doing something: " + value);
    }
}

// Использование
EnumSingleton.INSTANCE.setValue(42);
EnumSingleton.INSTANCE.doSomething();
```

**Преимущества:**
- Самая простая и безопасная реализация
- Потокобезопасен (гарантия языка)
- Защита от рефлексии
- Защита от сериализации/десериализации
- Невозможно создать второй экземпляр

**Недостатки:**
- Не поддерживает ленивую инициализацию (создаётся при загрузке)
- Нельзя наследовать enum
- Может выглядеть непривычно

**Рекомендация:** Joshua Bloch в книге "Effective Java" называет enum-подход лучшим способом реализации Singleton.

## Проблемы и решения

### Проблема 1: Нарушение через Reflection

```java
// Обход приватного конструктора через рефлексию
Constructor<Singleton> constructor = Singleton.class.getDeclaredConstructor();
constructor.setAccessible(true);
Singleton instance1 = constructor.newInstance();
Singleton instance2 = constructor.newInstance(); // Разные экземпляры!
```

**Решение:** Защита в конструкторе или использование enum.

```java
public class ReflectionProofSingleton {
    private static final ReflectionProofSingleton INSTANCE = new ReflectionProofSingleton();
    
    private ReflectionProofSingleton() {
        // Защита от создания второго экземпляра через рефлексию
        if (INSTANCE != null) {
            throw new IllegalStateException("Instance already exists!");
        }
    }
    
    public static ReflectionProofSingleton getInstance() {
        return INSTANCE;
    }
}
```

### Проблема 2: Нарушение при сериализации

При десериализации создаётся новый экземпляр.

```java
// Сериализация и десериализация создают новый объект
ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("singleton.ser"));
oos.writeObject(Singleton.getInstance());

ObjectInputStream ois = new ObjectInputStream(new FileInputStream("singleton.ser"));
Singleton deserializedInstance = (Singleton) ois.readObject();
// deserializedInstance != Singleton.getInstance()
```

**Решение:** Реализация метода `readResolve()`.

```java
public class SerializableSingleton implements Serializable {
    private static final SerializableSingleton INSTANCE = new SerializableSingleton();
    
    private SerializableSingleton() {}
    
    public static SerializableSingleton getInstance() {
        return INSTANCE;
    }
    
    // Предотвращает создание нового экземпляра при десериализации
    protected Object readResolve() {
        return INSTANCE;
    }
}
```

### Проблема 3: Проблемы с ClassLoader

В приложениях с несколькими загрузчиками классов могут создаваться разные экземпляры.

**Решение:** Использование контекстного ClassLoader или контейнера зависимостей (Spring).

### Проблема 4: Клонирование

Можно создать копию через клонирование, если Singleton реализует Cloneable.

**Решение:** Не реализовывать Cloneable или переопределить `clone()` с выбросом исключения.

```java
@Override
protected Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException("Singleton cannot be cloned");
}
```

## Примеры использования

### Пример 1: Configuration Manager

```java
public class ConfigurationManager {
    private static volatile ConfigurationManager instance;
    private Properties config;
    
    private ConfigurationManager() {
        config = new Properties();
        try {
            config.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }
    
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }
    
    public String getProperty(String key) {
        return config.getProperty(key);
    }
}
```

### Пример 2: Database Connection Pool

```java
public class DatabaseConnectionPool {
    
    private DatabaseConnectionPool() {
        initializePool();
    }
    
    private static class Holder {
        private static final DatabaseConnectionPool INSTANCE = new DatabaseConnectionPool();
    }
    
    public static DatabaseConnectionPool getInstance() {
        return Holder.INSTANCE;
    }
    
    private void initializePool() {
        // Инициализация пула соединений
    }
    
    public Connection getConnection() {
        // Получение соединения из пула
        return null;
    }
}
```

### Пример 3: Logger

```java
public enum Logger {
    INSTANCE;
    
    private final java.util.logging.Logger logger;
    
    Logger() {
        logger = java.util.logging.Logger.getLogger("ApplicationLogger");
    }
    
    public void log(String message) {
        logger.info(message);
    }
    
    public void error(String message, Exception e) {
        logger.log(Level.SEVERE, message, e);
    }
}

// Использование
Logger.INSTANCE.log("Application started");
```

### Примеры из JDK

- `java.lang.Runtime` — получение через `Runtime.getRuntime()`
- `java.awt.Desktop` — получение через `Desktop.getDesktop()`
- `java.lang.System` — хотя не классический Singleton, но использует похожий паттерн

### Примеры из Spring Framework

В Spring Framework Singleton является областью видимости по умолчанию для бинов:

```java
@Component
public class MyService {
    // По умолчанию создаётся один экземпляр на контекст
}

// Явное указание scope
@Component
@Scope("singleton")
public class AnotherService {
    // ...
}
```

**Важное отличие:** Spring Singleton — это один экземпляр на ApplicationContext, а не на JVM.

## Преимущества и недостатки

### Преимущества

✅ **Контролируемый доступ к единственному экземпляру**
- Строгий контроль над тем, как и когда клиенты получают доступ

✅ **Уменьшение загрязнения пространства имён**
- Не нужны глобальные переменные

✅ **Разрешает наследование**
- Можно создать подклассы Singleton

✅ **Ленивая инициализация**
- Экземпляр создаётся только при необходимости (для некоторых реализаций)

✅ **Экономия ресурсов**
- Один экземпляр вместо множества

### Недостатки

❌ **Глобальное состояние**
- Создаёт скрытые зависимости между классами
- Усложняет понимание и тестирование кода

❌ **Сложность тестирования**
- Трудно подменить mock-объектом
- Может влиять на изоляцию тестов

❌ **Нарушение Single Responsibility Principle**
- Класс отвечает и за бизнес-логику, и за управление своим экземпляром

❌ **Проблемы в многопоточных приложениях**
- Требует дополнительного кода для потокобезопасности
- Может стать узким местом при высокой конкуренции

❌ **Скрытие зависимостей**
- Зависимость не видна в сигнатуре конструктора или метода

## Современные альтернативы

### Dependency Injection

Вместо Singleton лучше использовать DI-контейнер:

```java
// Вместо Singleton
ConfigurationManager config = ConfigurationManager.getInstance();

// Лучше использовать DI
@Service
public class MyService {
    private final ConfigurationManager config;
    
    @Autowired
    public MyService(ConfigurationManager config) {
        this.config = config; // Внедрение зависимости
    }
}
```

**Преимущества DI:**
- Явные зависимости
- Легче тестировать (можно подставить mock)
- Лучше контроль жизненного цикла
- Соответствие принципам SOLID

### Когда всё-таки использовать Singleton

- Работа с ресурсами операционной системы (драйверы, файлы)
- Кеширование и пулы соединений
- Фабрики и реестры объектов
- Утилитарные классы без состояния

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Singleton и зачем он нужен?**

*Ответ:* Singleton — это порождающий паттерн проектирования, который гарантирует существование только одного экземпляра класса и предоставляет глобальную точку доступа к нему. Используется для объектов, которые должны существовать в единственном экземпляре: логгеры, конфигурации, пулы соединений.

**2. Как реализовать потокобезопасный Singleton?**

*Ответ:* Есть несколько способов:
- Eager initialization (создание при загрузке класса)
- Double-checked locking с volatile
- Bill Pugh Singleton (вложенный статический класс)
- Enum Singleton
- Синхронизированный метод getInstance()

**3. Зачем нужно ключевое слово volatile в Double-Checked Locking?**

*Ответ:* volatile предотвращает reordering инструкций и гарантирует, что все потоки видят полностью инициализированный объект. Без volatile возможна ситуация, когда ссылка на объект уже присвоена, но сам объект ещё не полностью сконструирован.

**4. В чём разница между Eager и Lazy инициализацией?**

*Ответ:* 
- **Eager** — экземпляр создаётся при загрузке класса, всегда потокобезопасен, но тратит ресурсы даже если не используется
- **Lazy** — экземпляр создаётся при первом обращении, экономит ресурсы, но требует дополнительных мер для потокобезопасности

### Продвинутые вопросы

**5. Как нарушить Singleton и как от этого защититься?**

*Ответ:* Способы нарушения:
- **Reflection** — можно получить доступ к приватному конструктору. Защита: проверка в конструкторе или использование enum
- **Serialization** — при десериализации создаётся новый объект. Защита: метод `readResolve()`
- **Cloneable** — можно клонировать. Защита: не реализовывать Cloneable или бросать исключение в `clone()`
- **ClassLoader** — разные загрузчики могут создать разные экземпляры. Защита: использование DI-контейнера

**6. Какой способ реализации Singleton рекомендует Joshua Bloch и почему?**

*Ответ:* Enum Singleton. Причины:
- Автоматическая защита от reflection
- Автоматическая защита от serialization
- Потокобезопасность гарантируется языком
- Самая лаконичная реализация
- Невозможно создать второй экземпляр

**7. В чём разница между Singleton в классическом понимании и Spring Singleton?**

*Ответ:* 
- **Классический Singleton** — один экземпляр на JVM
- **Spring Singleton** — один экземпляр на ApplicationContext (может быть несколько контекстов)
- Spring управляет жизненным циклом и зависимостями
- Spring Singleton легче тестировать

**8. Почему Singleton считается анти-паттерном? Какие альтернативы?**

*Ответ:* Проблемы:
- Глобальное состояние усложняет понимание кода
- Скрытые зависимости
- Сложность тестирования
- Нарушение Single Responsibility Principle
- Проблемы в многопоточности

Альтернативы:
- Dependency Injection (Spring, Guice)
- Монады (в функциональном программировании)
- Фабрики с кешированием

**9. Как правильно тестировать код, использующий Singleton?**

*Ответ:* 
- Использовать DI вместо прямого обращения к `getInstance()`
- Создавать фасадные методы для доступа к Singleton
- Использовать PowerMock для подмены статических методов (крайний случай)
- Рефакторинг на интерфейсы и внедрение зависимостей

**10. Как работает Bill Pugh Singleton и почему он потокобезопасен?**

*Ответ:* Использует вложенный статический класс (holder idiom). Вложенный класс загружается только при первом обращении к `getInstance()`. JVM гарантирует потокобезопасность при инициализации статических полей классов (specification гарантирует это через механизм class initialization lock).

---

[← Назад к разделу Порождающие паттерны](README.md)
