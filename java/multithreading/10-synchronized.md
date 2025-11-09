# Synchronized в Java: теория и практика

Ключевое слово `synchronized` — это встроенный механизм синхронизации в Java, обеспечивающий взаимное исключение (mutual exclusion) и гарантии видимости изменений между потоками. Это один из фундаментальных инструментов для создания потокобезопасного кода.

## Содержание

1. [Основные концепции](#основные-концепции)
   - [Мониторы и внутренние блокировки](#мониторы-и-внутренние-блокировки)
   - [Взаимное исключение](#взаимное-исключение)
   - [Видимость изменений](#видимость-изменений)
2. [Синтаксис и формы использования](#синтаксис-и-формы-использования)
   - [Синхронизированные методы](#синхронизированные-методы)
   - [Синхронизированные блоки](#синхронизированные-блоки)
   - [Статические синхронизированные методы](#статические-синхронизированные-методы)
3. [Семантика happens-before](#семантика-happens-before)
4. [Реентерабельность synchronized](#реентерабельность-synchronized)
5. [Взаимодействие с wait, notify и notifyAll](#взаимодействие-с-wait-notify-и-notifyall)
6. [Внутреннее устройство](#внутреннее-устройство)
   - [Object Header и Mark Word](#object-header-и-mark-word)
   - [Оптимизации JVM](#оптимизации-jvm)
7. [Производительность и overhead](#производительность-и-overhead)
8. [Сравнение с другими механизмами](#сравнение-с-другими-механизмами)
9. [Best Practices](#best-practices)
10. [Типичные ошибки и антипаттерны](#типичные-ошибки-и-антипаттерны)
11. [Практические примеры](#практические-примеры)
12. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Основные концепции

### Мониторы и внутренние блокировки

В Java каждый объект имеет связанный с ним **монитор** (intrinsic lock или monitor lock). Монитор — это механизм синхронизации, который позволяет только одному потоку одновременно выполнять код, защищённый этим монитором.

```java
public class Counter {
    private int count = 0;
    
    // Монитор объекта this защищает метод
    public synchronized void increment() {
        count++; // Только один поток может выполнять этот код
    }
    
    // Эквивалентно:
    public void incrementExplicit() {
        synchronized (this) { // Явное указание монитора
            count++;
        }
    }
}
```

**Ключевые свойства монитора:**
- **Взаимное исключение**: Только один поток может владеть монитором в конкретный момент времени
- **Реентерабельность**: Поток, владеющий монитором, может повторно войти в синхронизированный блок того же объекта
- **Автоматическое освобождение**: Монитор автоматически освобождается при выходе из synchronized блока

### Взаимное исключение

`synchronized` гарантирует, что критическая секция кода выполняется атомарно относительно других потоков, использующих тот же монитор:

```java
public class BankAccount {
    private double balance;
    
    // Без synchronized - race condition!
    public void transferUnsafe(BankAccount to, double amount) {
        this.balance -= amount;     // Шаг 1
        to.balance += amount;        // Шаг 2
        // Другой поток может выполниться между шагами 1 и 2
    }
    
    // С synchronized - атомарная операция
    public synchronized void transfer(BankAccount to, double amount) {
        if (this.balance >= amount) {
            this.balance -= amount;
            to.deposit(amount); // to.deposit тоже должен быть synchronized
        }
    }
    
    public synchronized void deposit(double amount) {
        this.balance += amount;
    }
}
```

### Видимость изменений

`synchronized` обеспечивает **happens-before** отношение, гарантируя видимость изменений между потоками:

```java
public class VisibilityExample {
    private int value = 0;
    private boolean ready = false;
    
    // Поток-писатель
    public synchronized void writer() {
        value = 42;
        ready = true;
        // При выходе из synchronized все изменения сбрасываются в основную память
    }
    
    // Поток-читатель
    public synchronized void reader() {
        // При входе в synchronized читается актуальное состояние из основной памяти
        if (ready) {
            System.out.println(value); // Гарантированно увидит 42
        }
    }
}
```

**Важно**: synchronized обеспечивает видимость не только для переменных внутри блока, но и для всех изменений, выполненных до входа в блок.

## Синтаксис и формы использования

### Синхронизированные методы

**Синхронизированный метод экземпляра** использует монитор объекта `this`:

```java
public class Counter {
    private int count = 0;
    
    // Монитор: this
    public synchronized void increment() {
        count++;
    }
    
    // Монитор: this
    public synchronized int getCount() {
        return count;
    }
    
    // Эквивалентная форма с явным блоком:
    public void incrementExplicit() {
        synchronized (this) {
            count++;
        }
    }
}
```

**Преимущества:**
- Простой и читаемый синтаксис
- Автоматическое управление блокировкой

**Недостатки:**
- Блокируется весь метод, даже если синхронизация нужна только части кода
- Нельзя использовать разные объекты-мониторы для разных операций

### Синхронизированные блоки

**Синхронизированный блок** позволяет явно указать объект-монитор и ограничить область синхронизации:

```java
public class FlexibleSync {
    private final Object lock = new Object(); // Приватный объект-монитор
    private int count = 0;
    
    public void increment() {
        // Несинхронизированная работа
        doSomethingNotRequiringSync();
        
        // Только критическая секция синхронизирована
        synchronized (lock) {
            count++;
        }
        
        // Несинхронизированная работа
        doSomethingElse();
    }
    
    // Можно использовать разные мониторы для разных данных
    private final Object readLock = new Object();
    private final Object writeLock = new Object();
    private List<String> data = new ArrayList<>();
    
    public void read() {
        synchronized (readLock) {
            // Операции чтения
            System.out.println(data);
        }
    }
    
    public void write(String item) {
        synchronized (writeLock) {
            // Операции записи
            data.add(item);
        }
    }
}
```

**Преимущества:**
- Более гранулярный контроль над областью синхронизации
- Можно использовать приватные объекты-мониторы (лучше для инкапсуляции)
- Можно использовать разные мониторы для независимых операций

**Best Practice**: Используйте приватный объект как монитор вместо `this`:

```java
public class GoodPractice {
    // Хорошо: приватный монитор
    private final Object lock = new Object();
    private int count = 0;
    
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}

public class BadPractice {
    private int count = 0;
    
    // Плохо: использование this позволяет внешнему коду захватить монитор
    public synchronized void increment() {
        count++;
    }
}

// Внешний код может нарушить синхронизацию:
BadPractice bad = new BadPractice();
synchronized (bad) { // Захватили монитор извне!
    // Теперь increment() не может выполниться
    Thread.sleep(10000);
}
```

### Статические синхронизированные методы

**Статический синхронизированный метод** использует монитор объекта `Class`:

```java
public class StaticSync {
    private static int globalCounter = 0;
    
    // Монитор: StaticSync.class
    public static synchronized void incrementGlobal() {
        globalCounter++;
    }
    
    // Эквивалентно:
    public static void incrementGlobalExplicit() {
        synchronized (StaticSync.class) {
            globalCounter++;
        }
    }
    
    // Разные мониторы!
    private int instanceCounter = 0;
    
    // Монитор: this
    public synchronized void incrementInstance() {
        instanceCounter++;
    }
}
```

**Важно**: Статические и нестатические synchronized методы используют **разные мониторы** и не блокируют друг друга:

```java
StaticSync obj1 = new StaticSync();
StaticSync obj2 = new StaticSync();

// Эти вызовы используют разные мониторы и могут выполняться параллельно:
Thread t1 = new Thread(() -> StaticSync.incrementGlobal());    // Монитор: StaticSync.class
Thread t2 = new Thread(() -> obj1.incrementInstance());        // Монитор: obj1
Thread t3 = new Thread(() -> obj2.incrementInstance());        // Монитор: obj2
```

## Семантика happens-before

`synchronized` создаёт важные гарантии happens-before в Java Memory Model:

**Правило synchronized для happens-before:**
> Разблокировка монитора (выход из synchronized) **happens-before** любой последующей блокировки того же монитора (входа в synchronized).

```java
public class HappensBeforeExample {
    private int x = 0;
    private int y = 0;
    private final Object lock = new Object();
    
    // Поток 1
    public void writer() {
        x = 1;                    // Действие 1
        y = 2;                    // Действие 2
        synchronized (lock) {     // Действие 3: блокировка
            // критическая секция
        }                         // Действие 4: разблокировка
    }
    
    // Поток 2
    public void reader() {
        synchronized (lock) {     // Действие 5: блокировка
            // критическая секция
        }                         // Действие 6: разблокировка
        int a = x;                // Действие 7
        int b = y;                // Действие 8
    }
}
```

**Гарантии:**
1. Действия 1-2 happens-before действия 3 (программный порядок в потоке 1)
2. Действие 4 happens-before действия 5 (правило synchronized)
3. Действия 5-6 happens-before действий 7-8 (программный порядок в потоке 2)
4. **Транзитивность**: Действия 1-2 happens-before действий 7-8

**Вывод**: Если поток 2 входит в synchronized блок после того, как поток 1 вышел из него, то поток 2 гарантированно увидит все изменения, сделанные потоком 1 до выхода из synchronized.

## Реентерабельность synchronized

Мониторы Java являются **реентерабельными** (reentrant): поток, владеющий монитором, может повторно входить в synchronized блок того же объекта без блокировки.

```java
public class ReentrantExample {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
        if (count < 10) {
            increment(); // Рекурсивный вызов - не блокируется!
        }
    }
    
    public synchronized void method1() {
        System.out.println("Method 1");
        method2(); // Вызов другого synchronized метода - не блокируется!
    }
    
    public synchronized void method2() {
        System.out.println("Method 2");
        method3(); // Ещё один вызов - не блокируется!
    }
    
    public synchronized void method3() {
        System.out.println("Method 3");
    }
}
```

**Как это работает:**

JVM отслеживает для каждого монитора:
1. **Владелец** (owner): поток, владеющий монитором, или null
2. **Счётчик входов** (entry count): количество раз, которое владелец вошёл в монитор

```
Поток пытается войти в synchronized:
  - Если owner == null:
      owner = текущий_поток
      entry_count = 1
      -> Вход разрешён
  - Если owner == текущий_поток:
      entry_count++
      -> Вход разрешён (реентерабельность!)
  - Если owner == другой_поток:
      -> Блокировка (ждём освобождения)

Поток выходит из synchronized:
  entry_count--
  Если entry_count == 0:
      owner = null
      -> Монитор освобождён
```

**Практический пример:**

```java
public class Stack {
    private Object[] elements;
    private int size = 0;
    
    public synchronized void push(Object item) {
        ensureCapacity();
        elements[size++] = item;
    }
    
    public synchronized void pop() {
        if (size == 0) {
            throw new EmptyStackException();
        }
        elements[--size] = null;
    }
    
    // Вызывается из synchronized метода push
    private synchronized void ensureCapacity() {
        // Реентерабельность: поток уже владеет монитором this
        if (elements.length == size) {
            elements = Arrays.copyOf(elements, 2 * size + 1);
        }
    }
}
```

## Взаимодействие с wait, notify и notifyAll

`synchronized` обязателен для использования методов `wait()`, `notify()` и `notifyAll()`:

```java
public class WaitNotifyExample {
    private final Queue<Task> queue = new LinkedList<>();
    private final int capacity = 10;
    
    // Producer
    public void produce(Task task) throws InterruptedException {
        synchronized (queue) {
            // Ждём, пока очередь не освободится
            while (queue.size() == capacity) {
                queue.wait(); // Освобождает монитор и ждёт
            }
            
            queue.add(task);
            queue.notifyAll(); // Пробуждаем ожидающие потоки
        }
    }
    
    // Consumer
    public Task consume() throws InterruptedException {
        synchronized (queue) {
            // Ждём, пока очередь не заполнится
            while (queue.isEmpty()) {
                queue.wait(); // Освобождает монитор и ждёт
            }
            
            Task task = queue.remove();
            queue.notifyAll(); // Пробуждаем ожидающие потоки
            return task;
        }
    }
}
```

**Важные правила:**

1. **wait() должен вызываться в цикле**, а не в if:
   ```java
   // ПЛОХО: spurious wakeup может нарушить условие
   synchronized (lock) {
       if (condition) {
           lock.wait();
       }
   }
   
   // ХОРОШО: проверяем условие после пробуждения
   synchronized (lock) {
       while (!condition) {
           lock.wait();
       }
   }
   ```

2. **wait() освобождает монитор** и переводит поток в состояние ожидания

3. **notify() пробуждает один** случайный ожидающий поток

4. **notifyAll() пробуждает все** ожидающие потоки (обычно безопаснее)

5. **IllegalMonitorStateException**: Вызов wait/notify/notifyAll без владения монитором приводит к исключению

## Внутреннее устройство

### Object Header и Mark Word

Каждый объект в Java имеет заголовок (object header), содержащий метаданные, включая информацию о блокировке:

```
Object Layout в памяти:
+------------------+
|   Mark Word      | <- Информация о блокировке, GC, hashcode
+------------------+
|   Class Pointer  | <- Указатель на метаданные класса
+------------------+
|   Fields         | <- Поля объекта
+------------------+
```

**Mark Word** (64-bit JVM):
```
Без блокировки:
| unused:25 | hashcode:31 | unused:1 | age:4 | biased:1 | lock:2 |

Тонкая блокировка (thin lock):
| thread_id:54 | epoch:2 | unused:1 | age:4 | biased:1 | lock:2 |

Толстая блокировка (fat lock):
| monitor_pointer:62 | lock:2 |
```

**Последние 2 бита (lock):**
- `01` - unlocked (без блокировки)
- `00` - thin lock (лёгкая блокировка)
- `10` - fat lock (тяжёлая блокировка)
- `11` - marked for GC

### Оптимизации JVM

JVM применяет несколько оптимизаций для повышения производительности synchronized:

#### 1. Biased Locking (смещённая блокировка)

Если объект блокируется почти всегда одним и тем же потоком, JVM "смещает" блокировку к этому потоку:

```java
// Если только один поток работает с объектом:
public class BiasedLockExample {
    private int count = 0;
    
    public synchronized void increment() {
        // При первой блокировке JVM смещает монитор к текущему потоку
        // Последующие блокировки этим потоком почти бесплатны
        count++;
    }
}
```

**Преимущества:**
- Почти нулевой overhead для однопоточного использования
- Автоматически активируется JVM

**Отключение:**
```bash
-XX:-UseBiasedLocking  # Отключить biased locking
```

#### 2. Thin Locking (тонкая блокировка)

Когда несколько потоков конкурируют, но конкуренция низкая, JVM использует CAS (Compare-And-Swap) вместо полноценного монитора:

```java
synchronized (obj) {
    // JVM пытается захватить монитор через CAS
    // Если успешно - тонкая блокировка
    // Если неудачно (конкуренция) - раздувается до толстой блокировки
}
```

#### 3. Lock Coarsening (укрупнение блокировок)

JIT-компилятор может объединить несколько последовательных блокировок одного объекта:

```java
// Исходный код:
public void example() {
    synchronized (obj) {
        operation1();
    }
    synchronized (obj) {
        operation2();
    }
    synchronized (obj) {
        operation3();
    }
}

// После оптимизации компилятором:
public void exampleOptimized() {
    synchronized (obj) {
        operation1();
        operation2();
        operation3();
    }
}
```

#### 4. Lock Elimination (устранение блокировок)

Если JIT определяет, что объект недоступен другим потокам (escape analysis), блокировка может быть полностью устранена:

```java
public String concatenate(String s1, String s2) {
    // StringBuffer использует synchronized, но...
    StringBuffer sb = new StringBuffer();
    sb.append(s1);
    sb.append(s2);
    return sb.toString();
    // JIT видит, что sb локален -> устраняет synchronized!
}
```

#### 5. Adaptive Spinning (адаптивное вращение)

Вместо немедленного перехода потока в состояние ожидания, JVM может попробовать "покрутиться" в цикле, ожидая освобождения блокировки:

```java
synchronized (obj) {
    // Если блокировка занята, поток может:
    // 1. Покрутиться в цикле (spin) несколько итераций
    // 2. Если не освободилась - перейти в ожидание (block)
}
```

**Параметры:**
```bash
-XX:+UseSpinning              # Включить spinning (по умолчанию вкл.)
-XX:PreBlockSpin=10           # Количество итераций spinning
```

## Производительность и overhead

### Стоимость synchronized

Производительность synchronized зависит от нескольких факторов:

| Сценарий | Overhead | Комментарий |
|----------|----------|-------------|
| Biased к текущему потоку | ~2-3 ns | Почти бесплатно |
| Thin lock без конкуренции | ~10-20 ns | Одна CAS операция |
| Thin lock с конкуренцией | ~50-100 ns | Spinning + возможная блокировка |
| Fat lock | ~200-500 ns | Полноценный монитор ОС |
| Вход в wait/notify | ~1-10 μs | Переключение контекста |

```java
public class PerformanceBenchmark {
    private int counter = 0;
    private final Object lock = new Object();
    
    // Вариант 1: Без синхронизации (небезопасно!)
    public void incrementUnsafe() {
        counter++; // ~1-2 ns
    }
    
    // Вариант 2: С synchronized
    public synchronized void incrementSafe() {
        counter++; // ~2-500 ns (зависит от конкуренции)
    }
    
    // Вариант 3: AtomicInteger (альтернатива)
    private final AtomicInteger atomicCounter = new AtomicInteger();
    
    public void incrementAtomic() {
        atomicCounter.incrementAndGet(); // ~15-30 ns
    }
}
```

### Рекомендации по производительности

1. **Минимизируйте область синхронизации:**
   ```java
   // ПЛОХО: блокировка на всё время обработки
   public synchronized void processLargeData(Data data) {
       Data processed = heavyComputation(data);  // Долго
       this.result = processed;                   // Быстро
   }
   
   // ХОРОШО: синхронизация только критической секции
   public void processLargeDataOptimized(Data data) {
       Data processed = heavyComputation(data);  // Несинхронизировано
       
       synchronized (this) {
           this.result = processed;               // Только это синхронизировано
       }
   }
   ```

2. **Используйте приватные объекты-мониторы:**
   ```java
   public class BetterSync {
       private final Object lock = new Object();
       private int value;
       
       public void update(int newValue) {
           synchronized (lock) {  // Приватный монитор
               value = newValue;
           }
       }
   }
   ```

3. **Рассмотрите альтернативы для высококонкурентных сценариев:**
   ```java
   // Для простых счётчиков
   AtomicInteger counter = new AtomicInteger();
   
   // Для коллекций
   ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
   
   // Для сложных операций с таймаутами
   ReentrantLock lock = new ReentrantLock();
   ```

4. **Избегайте вложенных блокировок:**
   ```java
   // ПЛОХО: риск deadlock, сложность анализа
   synchronized (lock1) {
       synchronized (lock2) {
           // операции
       }
   }
   
   // ХОРОШО: по возможности используйте одну блокировку
   synchronized (compositeLock) {
       // операции
   }
   ```

## Сравнение с другими механизмами

### synchronized vs ReentrantLock

| Характеристика | synchronized | ReentrantLock |
|----------------|--------------|---------------|
| **Синтаксис** | Встроенный, простой | Явное управление |
| **Автоматическое освобождение** | Да (всегда) | Нет (требуется finally) |
| **Trylock** | Нет | Да (tryLock()) |
| **Таймауты** | Нет | Да (tryLock(timeout)) |
| **Прерываемость** | Нет | Да (lockInterruptibly()) |
| **Честность (fairness)** | Нет | Опционально |
| **Условные переменные** | wait/notify | Condition objects |
| **Производительность** | Сравнимая | Сравнимая |
| **Мониторинг** | Поддержка в JVM tools | Ограничена |

**Пример с ReentrantLock:**

```java
public class ReentrantLockExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // Обязательно в finally!
        }
    }
    
    // Фичи, недоступные в synchronized:
    
    public boolean tryIncrement() {
        if (lock.tryLock()) { // Попытка без блокировки
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    public boolean tryIncrementWithTimeout() throws InterruptedException {
        if (lock.tryLock(100, TimeUnit.MILLISECONDS)) { // С таймаутом
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
}
```

**Когда использовать synchronized:**
- Простая синхронизация методов/блоков
- Не требуются расширенные возможности (trylock, таймауты)
- Важна простота и читаемость кода

**Когда использовать ReentrantLock:**
- Нужны таймауты или tryLock
- Требуется прерываемая блокировка
- Нужна честная (fair) очередь потоков
- Требуются множественные условные переменные (Condition)

### synchronized vs volatile

| Характеристика | synchronized | volatile |
|----------------|--------------|----------|
| **Атомарность операций** | Да | Нет (только для чтения/записи) |
| **Видимость** | Да | Да |
| **Блокировка** | Да | Нет |
| **Overhead** | Средний/высокий | Низкий |
| **Сложные операции** | Да | Нет |

```java
public class SyncVsVolatile {
    // volatile - для простых флагов
    private volatile boolean shutdownRequested = false;
    
    public void requestShutdown() {
        shutdownRequested = true; // Атомарная запись
    }
    
    public boolean isShutdownRequested() {
        return shutdownRequested; // Атомарное чтение
    }
    
    // synchronized - для сложных операций
    private int counter = 0;
    
    public synchronized void increment() {
        counter++; // Неатомарно без synchronized!
    }
}
```

### synchronized vs Atomic классы

```java
public class SyncVsAtomic {
    // С synchronized
    private int syncCounter = 0;
    
    public synchronized void incrementSync() {
        syncCounter++;
    }
    
    public synchronized int getSync() {
        return syncCounter;
    }
    
    // С AtomicInteger
    private final AtomicInteger atomicCounter = new AtomicInteger(0);
    
    public void incrementAtomic() {
        atomicCounter.incrementAndGet(); // Lock-free!
    }
    
    public int getAtomic() {
        return atomicCounter.get();
    }
}
```

**AtomicInteger лучше для:**
- Простые атомарные операции (increment, compareAndSet)
- Высококонкурентные счётчики
- Lock-free алгоритмы

**synchronized лучше для:**
- Координация нескольких переменных
- Сложная бизнес-логика
- Использование wait/notify

## Best Practices

### 1. Используйте приватные final объекты-мониторы

```java
// ХОРОШО
public class GoodLocking {
    private final Object lock = new Object();
    private int value;
    
    public void update(int newValue) {
        synchronized (lock) {
            value = newValue;
        }
    }
}

// ПЛОХО
public class BadLocking {
    public void update(int newValue) {
        synchronized (this) { // this доступен извне!
            // ...
        }
    }
}
```

### 2. Минимизируйте область синхронизации

```java
// ПЛОХО
public synchronized void processOrder(Order order) {
    validateOrder(order);        // Долго, не требует синхронизации
    calculateTotal(order);        // Долго, не требует синхронизации
    this.total += order.getTotal(); // Быстро, требует синхронизации
}

// ХОРОШО
public void processOrder(Order order) {
    validateOrder(order);
    calculateTotal(order);
    
    synchronized (this) {
        this.total += order.getTotal();
    }
}
```

### 3. Документируйте стратегию блокировки

```java
/**
 * Thread-safe cache with separate locks for read and write operations.
 * 
 * Locking strategy:
 * - readLock: protects all read operations (get, containsKey)
 * - writeLock: protects all write operations (put, remove, clear)
 */
public class DocumentedCache<K, V> {
    private final Object readLock = new Object();
    private final Object writeLock = new Object();
    private final Map<K, V> cache = new HashMap<>();
    
    public V get(K key) {
        synchronized (readLock) {
            return cache.get(key);
        }
    }
    
    public void put(K key, V value) {
        synchronized (writeLock) {
            cache.put(key, value);
        }
    }
}
```

### 4. Избегайте блокировки на публичных объектах

```java
// ПЛОХО
synchronized (somePublicObject) {
    // Другой код может тоже блокировать этот объект!
}

// ПЛОХО
public static final Object LOCK = new Object(); // Публичный!
synchronized (MyClass.LOCK) {
    // ...
}

// ХОРОШО
private final Object lock = new Object();
synchronized (lock) {
    // Только наш класс контролирует этот монитор
}
```

### 5. Соблюдайте единый порядок блокировок

```java
// ПЛОХО: может привести к deadlock
public class Account {
    public void transfer(Account to, double amount) {
        synchronized (this) {
            synchronized (to) {
                // Если два потока вызывают:
                // account1.transfer(account2, 100)
                // account2.transfer(account1, 200)
                // -> DEADLOCK!
            }
        }
    }
}

// ХОРОШО: упорядоченная блокировка
public class SafeAccount {
    private final int id;
    
    public void transfer(SafeAccount to, double amount) {
        SafeAccount first = this.id < to.id ? this : to;
        SafeAccount second = this.id < to.id ? to : this;
        
        synchronized (first) {
            synchronized (second) {
                // Всегда блокируем в одном порядке -> нет deadlock
            }
        }
    }
}
```

### 6. Не вызывайте чужие методы под блокировкой

```java
// ПЛОХО
public class BadCallback {
    private final List<Listener> listeners = new ArrayList<>();
    
    public synchronized void notifyListeners(Event event) {
        for (Listener listener : listeners) {
            listener.onEvent(event); // Чужой код под нашей блокировкой!
            // Риски:
            // 1. Долгое выполнение -> плохая производительность
            // 2. Обратный вызов в наш объект -> deadlock
            // 3. Бросит исключение -> состояние нарушено
        }
    }
}

// ХОРОШО
public class GoodCallback {
    private final List<Listener> listeners = new ArrayList<>();
    
    public void notifyListeners(Event event) {
        List<Listener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(listeners); // Копия под блокировкой
        }
        
        // Вызовы вне блокировки
        for (Listener listener : snapshot) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Обработка ошибок
            }
        }
    }
}
```

### 7. Используйте synchronized для простых случаев

```java
// Простой счётчик
public class SimpleCounter {
    private int count = 0;
    
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int get() {
        return count;
    }
}

// Не усложняйте без необходимости!
// Это излишне:
public class OverEngineered {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
}
```

## Типичные ошибки и антипаттерны

### 1. Блокировка на изменяемых объектах

```java
// ПЛОХО: строки интернируются!
public class StringLocking {
    private String lock = "lock"; // НИКОГДА так не делайте!
    
    public void doSomething() {
        synchronized (lock) {
            // Другой код в JVM может использовать ту же строку!
        }
    }
}

// ПЛОХО: блокировка на изменяемом объекте
public class MutableLock {
    private Object lock = new Object();
    
    public void changeLock() {
        lock = new Object(); // Теперь старые и новые блокировки независимы!
    }
}

// ХОРОШО
public class CorrectLocking {
    private final Object lock = new Object(); // final!
}
```

### 2. Блокировка на примитивных обёртках

```java
// ПЛОХО
public class BoxedLocking {
    private Integer count = 0;
    
    public void increment() {
        synchronized (count) { // count меняется при каждом инкременте!
            count++;
            // Теперь блокируем другой объект!
        }
    }
}

// ХОРОШО
public class CorrectBoxedLocking {
    private final Object lock = new Object();
    private Integer count = 0;
    
    public void increment() {
        synchronized (lock) {
            count++;
        }
    }
}
```

### 3. Забытая синхронизация в геттерах

```java
// ПЛОХО
public class InconsistentSync {
    private int value;
    
    public synchronized void setValue(int newValue) {
        value = newValue;
    }
    
    public int getValue() { // Не synchronized!
        return value; // Может вернуть устаревшее значение!
    }
}

// ХОРОШО
public class ConsistentSync {
    private int value;
    
    public synchronized void setValue(int newValue) {
        value = newValue;
    }
    
    public synchronized int getValue() {
        return value;
    }
}
```

### 4. Использование wait() без цикла

```java
// ПЛОХО
synchronized (lock) {
    if (!condition) {
        lock.wait(); // Spurious wakeup может нарушить условие!
    }
    // Условие может не выполняться!
}

// ХОРОШО
synchronized (lock) {
    while (!condition) { // Цикл!
        lock.wait();
    }
    // Условие гарантированно выполнено
}
```

### 5. Deadlock через неупорядоченные блокировки

```java
// ПЛОХО
class Account {
    synchronized void transferTo(Account other, int amount) {
        this.balance -= amount;
        synchronized (other) {
            other.balance += amount;
        }
    }
}

// account1.transferTo(account2, 100) и
// account2.transferTo(account1, 200)
// одновременно -> DEADLOCK!

// ХОРОШО
class SafeAccount {
    void transferTo(SafeAccount other, int amount) {
        Object lock1 = System.identityHashCode(this) < System.identityHashCode(other) ? this : other;
        Object lock2 = lock1 == this ? other : this;
        
        synchronized (lock1) {
            synchronized (lock2) {
                this.balance -= amount;
                other.balance += amount;
            }
        }
    }
}
```

### 6. Синхронизация на null или на разных объектах

```java
// ПЛОХО
public class NullSync {
    private Object lock = null;
    
    public void doSomething() {
        synchronized (lock) { // NullPointerException!
            // ...
        }
    }
}

// ПЛОХО
public class InconsistentLock {
    public void method1() {
        synchronized (new Object()) { // Каждый раз новый объект!
            // ...
        }
    }
}
```

## Практические примеры

### Пример 1: Thread-safe Cache

```java
public class SimpleCache<K, V> {
    private final Map<K, V> cache = new HashMap<>();
    private final int maxSize;
    
    public SimpleCache(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public synchronized V get(K key) {
        return cache.get(key);
    }
    
    public synchronized void put(K key, V value) {
        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            // Простая стратегия: удаляем первый элемент
            K firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
        }
        cache.put(key, value);
    }
    
    public synchronized void clear() {
        cache.clear();
    }
    
    public synchronized int size() {
        return cache.size();
    }
}
```

### Пример 2: Producer-Consumer с wait/notify

```java
public class BoundedBuffer<T> {
    private final Queue<T> queue = new LinkedList<>();
    private final int capacity;
    
    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
    }
    
    public void produce(T item) throws InterruptedException {
        synchronized (queue) {
            while (queue.size() == capacity) {
                queue.wait(); // Ждём, пока освободится место
            }
            
            queue.add(item);
            queue.notifyAll(); // Пробуждаем consumers
        }
    }
    
    public T consume() throws InterruptedException {
        synchronized (queue) {
            while (queue.isEmpty()) {
                queue.wait(); // Ждём, пока появятся данные
            }
            
            T item = queue.remove();
            queue.notifyAll(); // Пробуждаем producers
            return item;
        }
    }
}
```

### Пример 3: Ленивая инициализация (Double-Checked Locking)

```java
public class LazyInit {
    // volatile обязателен!
    private volatile ExpensiveObject instance;
    
    public ExpensiveObject getInstance() {
        // Первая проверка без блокировки (быстро)
        if (instance == null) {
            synchronized (this) {
                // Вторая проверка под блокировкой
                if (instance == null) {
                    instance = new ExpensiveObject();
                }
            }
        }
        return instance;
    }
    
    // Альтернатива: Initialization-on-demand holder
    private static class Holder {
        static final ExpensiveObject INSTANCE = new ExpensiveObject();
    }
    
    public static ExpensiveObject getInstanceStatic() {
        return Holder.INSTANCE; // Thread-safe, ленивая, без synchronized
    }
}
```

### Пример 4: Счётчик с отдельными блокировками для чтения и записи

```java
public class ReadWriteCounter {
    private final Object readLock = new Object();
    private final Object writeLock = new Object();
    private volatile int count = 0;
    
    public void increment() {
        synchronized (writeLock) {
            count++;
        }
    }
    
    public int get() {
        // volatile обеспечивает видимость без блокировки для чтения
        return count;
    }
    
    // Для сложных операций нужна блокировка
    public void incrementIfPositive() {
        synchronized (writeLock) {
            if (count > 0) {
                count++;
            }
        }
    }
}
```

### Пример 5: Thread-safe Singleton

```java
// Способ 1: Eager initialization
public class EagerSingleton {
    private static final EagerSingleton INSTANCE = new EagerSingleton();
    
    private EagerSingleton() {}
    
    public static EagerSingleton getInstance() {
        return INSTANCE;
    }
}

// Способ 2: Holder pattern (рекомендуется)
public class HolderSingleton {
    private HolderSingleton() {}
    
    private static class Holder {
        static final HolderSingleton INSTANCE = new HolderSingleton();
    }
    
    public static HolderSingleton getInstance() {
        return Holder.INSTANCE;
    }
}

// Способ 3: Double-checked locking
public class DCLSingleton {
    private static volatile DCLSingleton instance;
    
    private DCLSingleton() {}
    
    public static DCLSingleton getInstance() {
        if (instance == null) {
            synchronized (DCLSingleton.class) {
                if (instance == null) {
                    instance = new DCLSingleton();
                }
            }
        }
        return instance;
    }
}

// Способ 4: Enum (самый безопасный)
public enum EnumSingleton {
    INSTANCE;
    
    public void doSomething() {
        // ...
    }
}
```

## Вопросы для самопроверки

### Базовый уровень

1. **Что гарантирует ключевое слово synchronized?**
   <details>
   <summary>Ответ</summary>
   
   - Взаимное исключение (mutual exclusion): только один поток может выполнять synchronized блок/метод в один момент времени
   - Видимость изменений (visibility): изменения, сделанные одним потоком в synchronized блоке, видны другим потокам при входе в synchronized блок того же монитора
   - Гарантия happens-before: выход из synchronized happens-before следующего входа в synchronized того же объекта
   </details>

2. **В чём разница между синхронизированным методом экземпляра и статическим синхронизированным методом?**
   <details>
   <summary>Ответ</summary>
   
   - Синхронизированный метод экземпляра использует монитор объекта `this`
   - Статический синхронизированный метод использует монитор объекта `Class` (например, `MyClass.class`)
   - Они используют разные мониторы и не блокируют друг друга
   </details>

3. **Что такое реентерабельность в контексте synchronized?**
   <details>
   <summary>Ответ</summary>
   
   Реентерабельность означает, что поток, уже владеющий монитором, может повторно входить в synchronized блок того же объекта без блокировки. JVM отслеживает счётчик входов для каждого монитора.
   </details>

4. **Почему нельзя вызывать wait(), notify() или notifyAll() вне synchronized блока?**
   <details>
   <summary>Ответ</summary>
   
   Эти методы требуют, чтобы вызывающий поток владел монитором объекта. Без этого выбрасывается `IllegalMonitorStateException`. Это гарантирует атомарность операций проверки условия и ожидания.
   </details>

### Средний уровень

5. **Почему volatile необходим в Double-Checked Locking паттерне?**
   <details>
   <summary>Ответ</summary>
   
   ```java
   private static volatile Singleton instance;
   
   public static Singleton getInstance() {
       if (instance == null) {
           synchronized (Singleton.class) {
               if (instance == null) {
                   instance = new Singleton(); // (1)
               }
           }
       }
       return instance; // (2)
   }
   ```
   
   Без `volatile` возможна ситуация, когда в точке (2) поток увидит частично сконструированный объект из-за переупорядочивания инструкций. `volatile` запрещает это переупорядочивание и обеспечивает видимость полностью сконструированного объекта.
   </details>

6. **В чём проблема следующего кода?**
   ```java
   public class StringLock {
       private String lock = "lock";
       
       public void method() {
           synchronized (lock) {
               // ...
           }
       }
   }
   ```
   <details>
   <summary>Ответ</summary>
   
   Строковые литералы интернируются в JVM. Это означает, что строка "lock" может использоваться как монитор в другом месте программы, что приведёт к непредсказуемым блокировкам и возможным deadlock. Всегда используйте уникальные объекты как мониторы:
   ```java
   private final Object lock = new Object();
   ```
   </details>

7. **Какие оптимизации JVM применяет к synchronized?**
   <details>
   <summary>Ответ</summary>
   
   - **Biased Locking**: Смещение блокировки к потоку, который чаще всего её использует
   - **Thin Locking**: Лёгкая блокировка через CAS при низкой конкуренции
   - **Lock Coarsening**: Объединение нескольких последовательных блокировок
   - **Lock Elimination**: Устранение блокировки для локальных объектов (escape analysis)
   - **Adaptive Spinning**: "Вращение" вместо немедленной блокировки потока
   </details>

8. **Когда следует предпочесть ReentrantLock вместо synchronized?**
   <details>
   <summary>Ответ</summary>
   
   - Нужны таймауты при попытке захвата блокировки (`tryLock(timeout)`)
   - Требуется прерываемая блокировка (`lockInterruptibly()`)
   - Нужна честная (fair) очередь потоков
   - Требуются множественные условные переменные (Condition)
   - Нужна возможность попробовать захватить блокировку без ожидания (`tryLock()`)
   
   Для простых случаев предпочтительнее synchronized из-за простоты и автоматического освобождения.
   </details>

### Продвинутый уровень

9. **Объясните, как избежать deadlock при блокировке нескольких объектов.**
   <details>
   <summary>Ответ</summary>
   
   Основная стратегия — всегда захватывать блокировки в одном и том же порядке:
   
   ```java
   public void transfer(Account from, Account to, double amount) {
       // Упорядочиваем блокировки по идентификатору
       Account first = from.id < to.id ? from : to;
       Account second = first == from ? to : from;
       
       synchronized (first) {
           synchronized (second) {
               from.balance -= amount;
               to.balance += amount;
           }
       }
   }
   ```
   
   Другие стратегии:
   - Использовать один глобальный монитор (снижает конкурентность)
   - Использовать `tryLock()` с откатом (требует ReentrantLock)
   - Избегать вложенных блокировок
   </details>

10. **Почему wait() должен вызываться в цикле, а не в if?**
    <details>
    <summary>Ответ</summary>
    
    Причины:
    1. **Spurious wakeups**: Поток может проснуться без явного notify/notifyAll
    2. **Конкуренция**: Между пробуждением и захватом монитора условие может измениться
    3. **notifyAll()**: Просыпаются все потоки, но условие выполнено только для некоторых
    
    ```java
    // ПРАВИЛЬНО
    synchronized (lock) {
        while (!condition) {
            lock.wait();
        }
        // Условие гарантированно выполнено
    }
    
    // НЕПРАВИЛЬНО
    synchronized (lock) {
        if (!condition) {
            lock.wait();
        }
        // Условие может не выполняться!
    }
    ```
    </details>

11. **В чём разница между notify() и notifyAll()? Когда использовать каждый?**
    <details>
    <summary>Ответ</summary>
    
    - **notify()**: Пробуждает один случайный ожидающий поток
    - **notifyAll()**: Пробуждает все ожидающие потоки
    
    **Используйте notify() только если:**
    - Все ожидающие потоки ждут одинакового условия
    - Пробуждение одного потока освобождает ресурс только для него
    - Вы уверены в корректности (сложно!)
    
    **Используйте notifyAll() (рекомендуется в большинстве случаев):**
    - Ожидающие потоки ждут разных условий
    - Изменение может затронуть несколько потоков
    - Вы хотите быть в безопасности
    
    ```java
    // notifyAll() безопаснее
    synchronized (queue) {
        queue.add(item);
        queue.notifyAll(); // Все потоки проверят условие
    }
    ```
    </details>

12. **Как работает биасед блокировка (biased locking) и почему она может быть отключена в некоторых сценариях?**
    <details>
    <summary>Ответ</summary>
    
    Biased locking оптимизирует случай, когда объект блокируется почти всегда одним потоком. JVM "смещает" блокировку к этому потоку, записывая его ID в заголовок объекта. Последующие блокировки этим потоком почти бесплатны (проверка ID).
    
    **Отключается автоматически:**
    - При конкуренции между потоками (revocation)
    - Для классов, где наблюдается высокая конкуренция
    
    **Можно отключить вручную:**
    ```bash
    -XX:-UseBiasedLocking
    ```
    
    **Когда отключать:**
    - В высококонкурентных приложениях (может быть overhead от revocation)
    - При использовании object pooling (объекты переходят между потоками)
    - В некоторых версиях JDK с багами в biased locking
    </details>

---

## Дополнительные ресурсы

- [Java Language Specification: Threads and Locks](https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html)
- [Java Concurrency in Practice](https://jcip.net/) - Brian Goetz
- [The Art of Multiprocessor Programming](https://www.elsevier.com/books/the-art-of-multiprocessor-programming/herlihy/978-0-12-415950-1)
- [JEP 374: Disable and Deprecate Biased Locking](https://openjdk.java.net/jeps/374)

---

[← Назад к разделу Multithreading](README.md)
