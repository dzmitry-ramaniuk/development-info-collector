# Вопросы на собеседовании

### Базовые вопросы

**1. Что такое правило happens-before и почему оно важно?**

*Ответ:* Happens-before — это отношение порядка между операциями в Java Memory Model, которое гарантирует видимость изменений памяти. Если действие A happens-before B, то все изменения памяти, сделанные в A, гарантированно видны при выполнении B. 

Это позволяет рассуждать о корректности многопоточного кода, игнорируя внутренние переупорядочивания компилятора и процессора. Основные источники happens-before: `volatile` чтение/запись, `synchronized` вход/выход, `Thread.start()`, `Thread.join()`.

**Пример:** Без happens-before запись в одном потоке может быть не видна в другом:
```java
// Thread 1
x = 42;
ready = true;

// Thread 2  
while (!ready) {}
System.out.println(x); // Может вывести 0 без volatile на ready!
```

**2. Чем отличается `volatile` от `synchronized`?**

*Ответ:* 

**volatile:**
- Обеспечивает видимость изменений между потоками
- Запрещает переупорядочивание операций вокруг volatile переменной
- Гарантирует атомарность чтения/записи (даже для long/double)
- НЕ обеспечивает атомарность составных операций (например, `i++`)
- Легковесный механизм без блокировок
- Подходит для флагов состояния и публикации неизменяемых объектов

**synchronized:**
- Обеспечивает взаимное исключение (mutual exclusion)
- Гарантирует видимость всех изменений памяти внутри блока
- Обеспечивает атомарность всего синхронизированного блока
- Создаёт блокировку, возможна конкуренция
- Может приводить к deadlock
- Подходит для защиты критических секций с несколькими операциями

**Пример:**
```java
// volatile - только для простых флагов
private volatile boolean running = true;
public void stop() { running = false; }

// synchronized - для сложных операций
private int count = 0;
public synchronized void increment() { count++; }
```

**3. Как правильно завершать работу `ExecutorService`?**

*Ответ:* Правильное завершение включает три шага:

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

try {
    // Используем executor
    executor.submit(task);
} finally {
    // Шаг 1: Запрещаем новые задачи
    executor.shutdown();
    
    try {
        // Шаг 2: Ждём завершения текущих задач с таймаутом
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            // Шаг 3: Принудительно останавливаем при таймауте
            List<Runnable> notExecuted = executor.shutdownNow();
            System.out.println("Cancelled tasks: " + notExecuted.size());
            
            // Шаг 4: Ждём ещё немного после interrupt
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Pool did not terminate");
            }
        }
    } catch (InterruptedException e) {
        // Прерваны во время ожидания
        executor.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

**Важно:** Задачи должны корректно обрабатывать `InterruptedException` и проверять флаг прерывания.

**4. Что такое deadlock и как его обнаружить?**

*Ответ:* Deadlock — ситуация, когда два или более потока бесконечно ждут ресурсы, удерживаемые друг другом. Необходимые условия для deadlock:
1. Взаимное исключение (mutual exclusion)
2. Удержание и ожидание (hold and wait)
3. Отсутствие вытеснения (no preemption)
4. Циклическое ожидание (circular wait)

**Обнаружение:**

1. **Визуально через jstack:**
```bash
jstack <pid> | grep -A 20 "Found one Java-level deadlock"
```

2. **Программно через ThreadMXBean:**
```java
ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();

if (deadlockedThreads != null) {
    ThreadInfo[] infos = threadMXBean.getThreadInfo(deadlockedThreads);
    for (ThreadInfo info : infos) {
        System.out.println("Deadlocked thread: " + info.getThreadName());
        System.out.println("  Waiting on: " + info.getLockName());
        System.out.println("  Owned by: " + info.getLockOwnerName());
    }
}
```

**Предотвращение:**
- Фиксированный порядок захвата блокировок
- Использование `tryLock()` с таймаутами
- Избегание вложенных блокировок
- Минимизация времени удержания блокировок

**5. Как `CompletableFuture` помогает строить асинхронные конвейеры?**

*Ответ:* `CompletableFuture` предоставляет композиционный API для асинхронных вычислений:

**Основные возможности:**

1. **Цепочки трансформаций:**
```java
CompletableFuture.supplyAsync(() -> fetchUserId())
    .thenApply(id -> "User-" + id)
    .thenApplyAsync(name -> fetchUserDetails(name))
    .thenAccept(details -> saveToCache(details));
```

2. **Комбинирование независимых задач:**
```java
CompletableFuture<String> future1 = fetchData1Async();
CompletableFuture<String> future2 = fetchData2Async();

CompletableFuture<String> combined = future1.thenCombine(future2, 
    (data1, data2) -> data1 + data2);
```

3. **Обработка ошибок:**
```java
future.exceptionally(ex -> "Fallback value")
      .handle((result, ex) -> ex != null ? "Error" : result);
```

4. **Таймауты и отмена (Java 9+):**
```java
future.orTimeout(5, TimeUnit.SECONDS)
      .completeOnTimeout("Default", 3, TimeUnit.SECONDS);
```

**Преимущества над Future:**
- Неблокирующее комбинирование результатов
- Явная обработка ошибок
- Контроль над Executor
- Цепочки трансформаций
- Возможность отмены с callback

### Продвинутые вопросы

**6. Объясните разницу между `ReentrantLock` и `synchronized`. Когда использовать каждый?**

*Ответ:*

**synchronized:**
- **Плюсы:** Простой синтаксис, автоматическое освобождение при исключении, оптимизирован JVM (biased locking, lock coarsening)
- **Минусы:** Нельзя прервать ожидание, нет таймаутов, всегда блокирует, нет справедливости
- **Когда использовать:** Для простых критических секций, когда достаточно базовой функциональности

**ReentrantLock:**
- **Плюсы:** 
  - `tryLock()` — попытка без блокировки
  - `tryLock(timeout)` — ожидание с таймаутом
  - `lockInterruptibly()` — можно прервать
  - Справедливые блокировки (`fair = true`)
  - Можно проверить состояние (`isLocked()`, `getQueueLength()`)
- **Минусы:** Нужно явно вызывать `unlock()` в `finally`, более сложный код
- **Когда использовать:** Когда нужна гибкость (таймауты, отмена, справедливость)

**Пример:**
```java
// synchronized - простой случай
public synchronized void simpleMethod() {
    // Автоматическое освобождение
}

// ReentrantLock - сложный случай
ReentrantLock lock = new ReentrantLock();
public void complexMethod() {
    if (lock.tryLock(1, TimeUnit.SECONDS)) {
        try {
            // Получили блокировку
        } finally {
            lock.unlock(); // Обязательно!
        }
    } else {
        // Не получили блокировку за таймаут
    }
}
```

**7. Как работает ConcurrentHashMap и чем отличается от Hashtable и Collections.synchronizedMap?**

*Ответ:*

**Hashtable:**
- Все методы `synchronized`
- Блокировка на уровне всей таблицы
- Низкая конкуренция — один поток за раз
- Legacy класс, не рекомендуется

**Collections.synchronizedMap:**
- Wrapper вокруг обычной Map
- Блокировка на уровне всей map для каждой операции
- Итератор требует внешней синхронизации
- Простой, но неэффективный в многопоточности

**ConcurrentHashMap:**
- **Java 7 и раньше:** Сегментированная блокировка (lock striping) — map разделена на сегменты
- **Java 8+:** Блокировка на уровне отдельных бакетов через CAS и `synchronized` на узлах
- Неблокирующие чтения (lock-free reads)
- Атомарные операции: `computeIfAbsent`, `merge`, `compute`
- Слабосогласованные итераторы (не fail-fast)
- Поддержка параллельных операций: `forEach`, `reduce`, `search`

**Архитектура Java 8+:**
```java
// Внутри ConcurrentHashMap
transient volatile Node<K,V>[] table;

// Чтение - не требует блокировки
public V get(Object key) {
    // Использует volatile reads
}

// Запись - блокировка только на бакет
final V putVal(K key, V value, boolean onlyIfAbsent) {
    // CAS для пустого бакета
    // synchronized на Node для занятого бакета
}
```

**Когда использовать:**
- **ConcurrentHashMap:** Высококонкурентные сценарии, частые чтения
- **synchronizedMap:** Низкая конкуренция, простота важнее производительности
- **Hashtable:** Не использовать (legacy)

**8. Объясните паттерн Double-Checked Locking. Почему volatile обязателен?**

*Ответ:* Double-Checked Locking — паттерн для ленивой инициализации с минимизацией синхронизации:

```java
public class Singleton {
    private static volatile Singleton instance; // volatile ОБЯЗАТЕЛЕН!
    
    public static Singleton getInstance() {
        if (instance == null) {              // Проверка 1 (без блокировки)
            synchronized (Singleton.class) {
                if (instance == null) {      // Проверка 2 (под блокировкой)
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**Почему volatile обязателен:**

Без `volatile` конструкция объекта может быть переупорядочена:
```java
// Создание объекта состоит из трёх шагов:
instance = new Singleton();
// Компилятор может переупорядочить так:
// 1. Выделить память для Singleton
// 2. instance = адрес памяти (instance != null, но объект не инициализирован!)
// 3. Вызвать конструктор Singleton

// Другой поток может увидеть instance != null, но получить неинициализированный объект!
```

**volatile запрещает это переупорядочивание:**
- Запись в `volatile` поле создаёт happens-before барьер
- Все операции до записи гарантированно выполнятся до неё
- Чтение `volatile` увидит полностью инициализированный объект

**Альтернативы (более безопасные):**

1. **Initialization-on-demand holder idiom:**
```java
public class Singleton {
    private Singleton() {}
    
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstance() {
        return Holder.INSTANCE; // Thread-safe благодаря class loader
    }
}
```

2. **Enum (лучшее решение):**
```java
public enum Singleton {
    INSTANCE;
    
    public void doSomething() {
        // ...
    }
}
```

**9. Как работает ForkJoinPool и когда его использовать?**

*Ответ:* `ForkJoinPool` — специализированный пул для параллельных рекурсивных задач, использующий work-stealing алгоритм.

**Архитектура:**
- Каждый рабочий поток имеет свою очередь (deque)
- Поток берёт задачи из головы своей очереди (LIFO)
- Когда своя очередь пуста, "крадёт" задачи из хвоста очередей других потоков (FIFO)
- Минимизирует конкуренцию за задачи

**Работа fork/join:**
```java
public class FibonacciTask extends RecursiveTask<Integer> {
    private final int n;
    
    @Override
    protected Integer compute() {
        if (n <= 1) return n;
        
        FibonacciTask f1 = new FibonacciTask(n - 1);
        f1.fork(); // Асинхронный запуск в другом потоке
        
        FibonacciTask f2 = new FibonacciTask(n - 2);
        int result2 = f2.compute(); // Выполняем в текущем потоке
        int result1 = f1.join(); // Ждём результат f1
        
        return result1 + result2;
    }
}
```

**Когда использовать:**
- ✅ Рекурсивные divide-and-conquer алгоритмы
- ✅ Обработка больших массивов/коллекций
- ✅ Параллельные стримы (используют ForkJoinPool.commonPool())
- ✅ Задачи с неравномерным распределением работы (work-stealing помогает)

**Когда НЕ использовать:**
- ❌ Блокирующие I/O операции (забивает пул)
- ❌ Независимые задачи (лучше обычный ExecutorService)
- ❌ Мелкие задачи (overhead > benefit)

**10. Что такое виртуальные потоки (Project Loom) и как они меняют многопоточное программирование?**

*Ответ:* Виртуальные потоки (Java 19+, production в Java 21) — это легковесные потоки, управляемые JVM, а не ОС.

**Традиционные (platform) потоки:**
- Один Java thread = один OS thread
- Стоимость: 1-2 МБ памяти на стек
- Переключение контекста через ОС (дорого)
- Ограничение: тысячи потоков максимум

**Виртуальные потоки:**
- Множество виртуальных потоков на одном OS thread (carrier thread)
- Стоимость: несколько КБ памяти
- Переключение управляется JVM (дёшево)
- Можно создавать миллионы потоков

**Использование:**
```java
// Создание виртуального потока
Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
});

// ExecutorService с виртуальными потоками
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Можно обрабатывать миллионы конкурентных задач
for (int i = 0; i < 1_000_000; i++) {
    executor.submit(() -> {
        // I/O операция - не блокирует carrier thread!
        String data = httpClient.get(url);
        processData(data);
    });
}
```

**Ключевые преимущества:**
1. **Упрощение кода:** Можно писать простой синхронный код вместо асинхронного
2. **Масштабируемость:** Миллионы конкурентных операций
3. **Совместимость:** Используют тот же Thread API
4. **I/O оптимизация:** Блокирующие I/O не блокируют carrier thread

**Structured Concurrency (Java 21+):**
```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Future<String> user = scope.fork(() -> fetchUser(userId));
    Future<String> orders = scope.fork(() -> fetchOrders(userId));
    
    scope.join();           // Ждём все задачи
    scope.throwIfFailed();  // Проверяем ошибки
    
    return combineResults(user.resultNow(), orders.resultNow());
} // Автоматическая очистка и отмена незавершённых задач
```

**Когда использовать виртуальные потоки:**
- ✅ I/O-bound задачи (HTTP requests, DB queries)
- ✅ Высокая конкуренция (много одновременных операций)
- ✅ Упрощение асинхронного кода

**Когда НЕ использовать:**
- ❌ CPU-bound задачи (используйте ForkJoinPool)
- ❌ Pinning ситуации (synchronized блоки с долгими операциями)
- ❌ Критичные по латентности задачи (могут быть unmounted)
