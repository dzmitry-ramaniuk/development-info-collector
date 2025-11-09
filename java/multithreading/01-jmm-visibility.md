# Java Memory Model и гарантии видимости

Java Memory Model (JMM) определяет правила взаимодействия потоков через разделяемую память. JMM описывает, какие значения переменных могут наблюдать потоки и какие переупорядочивания инструкций допустимы компилятором и процессором.

### Отношение happens-before

Отношение **happens-before** — это центральная концепция JMM. Если действие A happens-before действия B, то все изменения памяти, выполненные в A, гарантированно видны при выполнении B.

**Ключевые источники happens-before:**
- **Программный порядок**: Каждое действие в потоке happens-before каждого последующего действия в том же потоке.
- **Монитор блокировки**: Разблокировка монитора happens-before любой последующей блокировки того же монитора.
- **Volatile переменные**: Запись в `volatile` поле happens-before любого последующего чтения этого поля.
- **Thread.start()**: Вызов `start()` happens-before любого действия в запущенном потоке.
- **Thread.join()**: Завершение потока happens-before возврата из `join()` этого потока.
- **Инициализация**: Конструктор объекта завершается happens-before начала финализатора.

**Пример проблемы видимости без синхронизации:**

```java
public class VisibilityProblem {
    private static boolean ready = false;
    private static int number = 0;
    
    public static void main(String[] args) {
        new Thread(() -> {
            while (!ready) {
                Thread.yield();
            }
            System.out.println(number); // Может вывести 0!
        }).start();
        
        number = 42;
        ready = true;
    }
}
```

В этом примере без гарантий happens-before читающий поток может:
1. Никогда не увидеть изменение `ready` (бесконечный цикл)
2. Увидеть `ready = true`, но `number = 0` (переупорядочивание)

**Решение с volatile:**

```java
public class VisibilitySolution {
    private static volatile boolean ready = false;
    private static int number = 0;
    
    public static void main(String[] args) {
        new Thread(() -> {
            while (!ready) {
                Thread.yield();
            }
            System.out.println(number); // Всегда выведет 42
        }).start();
        
        number = 42;
        ready = true; // volatile запись создаёт happens-before
    }
}
```

### Проблемы без синхронизации

Без правильной синхронизации возможны следующие проблемы:

1. **Torn reads/writes** — чтение/запись `long` и `double` не атомарны (могут быть разделены на две 32-битные операции)
2. **Кэширование в регистрах** — поток может читать устаревшее значение из локального кэша
3. **Переупорядочивание инструкций** — компилятор и процессор могут менять порядок операций для оптимизации

**Пример torn reads:**

```java
public class TornReads {
    private long value = 0; // Не volatile!
    
    public void writer() {
        value = 0x0000000100000001L; // 64-bit запись
    }
    
    public void reader() {
        long v = value; // Может прочитать 0x0000000000000001 или 0x0000000100000000
        System.out.println(Long.toHexString(v));
    }
}
```

### Volatile: видимость и упорядочивание

Ключевое слово `volatile` обеспечивает:
1. **Видимость**: Запись в `volatile` переменную немедленно видна всем потокам
2. **Упорядочивание**: Запрещает переупорядочивание операций вокруг `volatile` чтения/записи
3. **Атомарность примитивов**: Чтение и запись `volatile` переменных атомарны (даже для `long` и `double`)

**Важно**: `volatile` НЕ делает сложные операции атомарными:

```java
public class VolatileCounter {
    private volatile int count = 0;
    
    public void increment() {
        count++; // НЕ атомарно! Это: read -> increment -> write
    }
    
    // Правильный вариант:
    private final AtomicInteger atomicCount = new AtomicInteger(0);
    
    public void incrementCorrectly() {
        atomicCount.incrementAndGet(); // Атомарно
    }
}
```

**Когда использовать volatile:**
- Флаги состояния (shutdown flags, status flags)
- Публикация неизменяемых объектов после инициализации
- Паттерн double-checked locking (с осторожностью)

**Пример корректного использования:**

```java
public class SafetyPublisher {
    private volatile Helper helper;
    
    public Helper getHelper() {
        Helper h = helper; // Локальная копия для производительности
        if (h == null) {
            synchronized (this) {
                h = helper;
                if (h == null) {
                    helper = h = new Helper();
                }
            }
        }
        return h;
    }
}
```

