# Multithreading

## Историческая справка и эволюция многопоточности в Java

Java изначально проектировалась как язык с встроенной поддержкой многопоточности. В Java 1.0 (1996) появились классы `Thread`, `Runnable` и ключевое слово `synchronized`. Это был значительный шаг вперёд по сравнению с C/C++, где многопоточность зависела от платформы.

**Основные вехи эволюции:**
- **Java 1.0-1.4**: Базовые примитивы (`Thread`, `synchronized`, `wait/notify`). Модель памяти имела серьёзные недостатки.
- **Java 5 (2004)**: Революционное обновление — появился пакет `java.util.concurrent` (JSR-166) от Doug Lea. Добавлены `ExecutorService`, `Lock`, `Semaphore`, `CountDownLatch`, `ConcurrentHashMap`, атомарные классы. Переработана Java Memory Model (JSR-133).
- **Java 7 (2011)**: `ForkJoinPool` и `RecursiveTask` для параллельных вычислений.
- **Java 8 (2014)**: `CompletableFuture` для асинхронного программирования, параллельные стримы.
- **Java 9-17**: Улучшения `CompletableFuture`, reactive streams (Flow API), оптимизации `ConcurrentHashMap`.
- **Java 19-21 (2023)**: Виртуальные потоки (Project Loom), structured concurrency — революция в асинхронном программировании.

## Java Memory Model и гарантии видимости

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

## Управление потоками и пулами

### Проблемы создания потоков напрямую

Создание потоков через `new Thread()` имеет серьёзные недостатки:

1. **Высокая стоимость**: Каждый поток выделяет стек (по умолчанию 1 МБ на Linux, 2 МБ на Windows)
2. **Переключение контекста**: При большом количестве потоков процессор тратит время на переключение
3. **Отсутствие управления**: Нет централизованного контроля над количеством и жизненным циклом потоков
4. **Сложность отмены**: Нет встроенного механизма отмены и таймаутов

**Антипаттерн:**

```java
// Плохой код - создаёт неконтролируемое количество потоков
public void handleRequest(Request request) {
    new Thread(() -> {
        processRequest(request);
    }).start(); // Каждый запрос = новый поток!
}
```

### ExecutorService: пулы потоков

`ExecutorService` решает эти проблемы, предоставляя управляемый пул потоков:

**1. FixedThreadPool — для CPU-bound задач:**

```java
// Создаём пул с фиксированным количеством потоков
ExecutorService executor = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors()
);

// Отправляем задачи
for (int i = 0; i < 100; i++) {
    final int taskId = i;
    executor.submit(() -> {
        System.out.println("Task " + taskId + " on " + 
                         Thread.currentThread().getName());
        // CPU-intensive работа
        return computeResult(taskId);
    });
}

// Корректное завершение
executor.shutdown();
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow(); // Принудительная остановка
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            System.err.println("Pool did not terminate");
        }
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

**2. CachedThreadPool — для I/O-bound задач:**

```java
// Создаёт потоки по требованию, переиспользует простаивающие
ExecutorService executor = Executors.newCachedThreadPool();

// Хорошо подходит для коротких асинхронных задач
executor.submit(() -> {
    // I/O операция (запрос к БД, HTTP запрос)
    return fetchDataFromAPI();
});
```

**3. ScheduledThreadPool — для отложенных и периодических задач:**

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

// Однократное выполнение с задержкой
scheduler.schedule(() -> {
    System.out.println("Executed after 5 seconds");
}, 5, TimeUnit.SECONDS);

// Периодическое выполнение с фиксированной частотой
scheduler.scheduleAtFixedRate(() -> {
    System.out.println("Heartbeat: " + System.currentTimeMillis());
}, 0, 1, TimeUnit.SECONDS);

// Периодическое выполнение с фиксированной задержкой между завершениями
scheduler.scheduleWithFixedDelay(() -> {
    System.out.println("Process batch");
    processBatch(); // Выполнение может занять разное время
}, 0, 10, TimeUnit.SECONDS);
```

**4. ForkJoinPool — для divide-and-conquer алгоритмов:**

```java
public class ParallelSum extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000;
    private final long[] array;
    private final int start, end;
    
    public ParallelSum(long[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected Long compute() {
        int length = end - start;
        if (length <= THRESHOLD) {
            // Базовый случай: вычисляем напрямую
            long sum = 0;
            for (int i = start; i < end; i++) {
                sum += array[i];
            }
            return sum;
        } else {
            // Рекурсивный случай: делим задачу
            int mid = start + length / 2;
            ParallelSum leftTask = new ParallelSum(array, start, mid);
            ParallelSum rightTask = new ParallelSum(array, mid, end);
            
            leftTask.fork(); // Асинхронно выполняем левую часть
            long rightResult = rightTask.compute(); // Выполняем правую часть в текущем потоке
            long leftResult = leftTask.join(); // Ждём результат левой части
            
            return leftResult + rightResult;
        }
    }
    
    public static void main(String[] args) {
        long[] array = new long[1_000_000];
        // Заполняем массив...
        
        ForkJoinPool pool = ForkJoinPool.commonPool();
        ParallelSum task = new ParallelSum(array, 0, array.length);
        long result = pool.invoke(task);
        
        System.out.println("Sum: " + result);
    }
}
```

### Кастомизация пулов с ThreadPoolExecutor

Для тонкой настройки используйте `ThreadPoolExecutor` напрямую:

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // corePoolSize - минимальное количество потоков
    10,                     // maximumPoolSize - максимальное количество
    60L,                    // keepAliveTime - время жизни простаивающих потоков
    TimeUnit.SECONDS,       // единица времени
    new LinkedBlockingQueue<>(100), // очередь задач
    new ThreadPoolExecutor.CallerRunsPolicy() // политика отклонения
);

// Настройка имён потоков для отладки
ThreadFactory namedThreadFactory = new ThreadFactory() {
    private final AtomicInteger counter = new AtomicInteger(0);
    
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName("worker-" + counter.incrementAndGet());
        thread.setDaemon(false); // Не daemon для корректного завершения
        return thread;
    }
};

executor.setThreadFactory(namedThreadFactory);
```

### Обработка прерываний

Корректная обработка `InterruptedException` критически важна:

```java
public void processWithInterruption() {
    ExecutorService executor = Executors.newFixedThreadPool(2);
    
    Future<?> future = executor.submit(() -> {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Работа с проверкой прерывания
                doWork();
                
                // Методы, бросающие InterruptedException
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            // Восстанавливаем флаг прерывания
            Thread.currentThread().interrupt();
            System.out.println("Task interrupted, cleaning up...");
            cleanup();
        }
    });
    
    // Отмена задачи
    future.cancel(true); // true = interrupt if running
    
    executor.shutdown();
}
```

### Виртуальные потоки (Java 19+)

Project Loom ввёл виртуальные потоки — легковесные потоки, управляемые JVM:

```java
// Создание виртуального потока (Java 21+)
Thread.startVirtualThread(() -> {
    System.out.println("Running in virtual thread");
    // I/O операции автоматически не блокируют carrier thread
});

// Executor для виртуальных потоков
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Можно создавать миллионы виртуальных потоков
for (int i = 0; i < 1_000_000; i++) {
    executor.submit(() -> {
        Thread.sleep(Duration.ofSeconds(1));
        return "Result";
    });
}
```

**Преимущества виртуальных потоков:**
- Очень низкая стоимость создания (десятки байт вместо мегабайт)
- Можно создавать миллионы потоков
- Упрощают написание асинхронного кода (можно писать синхронный код)
- Автоматическое управление блокировками I/O

## Асинхронные вычисления и координация

### CompletableFuture: асинхронные конвейеры

`CompletableFuture` (Java 8+) предоставляет мощный API для построения асинхронных вычислений:

**Базовое создание и получение результата:**

```java
// Асинхронное выполнение
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // Выполняется в ForkJoinPool.commonPool()
    sleep(1000);
    return "Result from async computation";
});

// Получение результата (блокирующая операция)
String result = future.get(); // Бросает ExecutionException, InterruptedException

// Получение с таймаутом (Java 9+)
String resultWithTimeout = future.get(5, TimeUnit.SECONDS); // TimeoutException

// Неблокирующее получение
String resultNow = future.getNow("default value"); // Вернёт default, если не готово
```

**Цепочки трансформаций:**

```java
CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> {
        // Первая асинхронная операция
        return fetchUserId();
    })
    .thenApply(userId -> {
        // Синхронная трансформация (выполняется в том же потоке)
        return "User-" + userId;
    })
    .thenApplyAsync(userName -> {
        // Асинхронная трансформация (новый поток из пула)
        return fetchUserDetails(userName);
    })
    .thenApply(userDetails -> {
        return userDetails.toJson();
    });
```

**Обработка ошибок:**

```java
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> {
        if (Math.random() > 0.5) {
            throw new RuntimeException("Random failure");
        }
        return "Success";
    })
    .exceptionally(ex -> {
        // Обработка исключения, возврат значения по умолчанию
        System.err.println("Error: " + ex.getMessage());
        return "Fallback value";
    })
    .handle((result, ex) -> {
        // Обработка как успеха, так и ошибки
        if (ex != null) {
            return "Error: " + ex.getMessage();
        }
        return "Success: " + result;
    });
```

**Комбинирование нескольких futures:**

```java
// Параллельное выполнение независимых задач
CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Result1";
});

CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
    sleep(1000);
    return "Result2";
});

// Комбинирование двух результатов
CompletableFuture<String> combined = future1.thenCombine(future2, (r1, r2) -> {
    return r1 + " + " + r2;
});

// Ожидание всех futures
CompletableFuture<Void> allOf = CompletableFuture.allOf(future1, future2);
allOf.thenRun(() -> {
    System.out.println("All tasks completed");
});

// Ожидание первого завершённого future
CompletableFuture<Object> anyOf = CompletableFuture.anyOf(future1, future2);
anyOf.thenAccept(result -> {
    System.out.println("First completed: " + result);
});
```

**Практический пример: параллельные HTTP запросы**

```java
public class AsyncHttpService {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public CompletableFuture<List<String>> fetchAllUserData(List<String> userIds) {
        List<CompletableFuture<String>> futures = userIds.stream()
            .map(userId -> CompletableFuture.supplyAsync(() -> {
                return httpClient.get("/users/" + userId);
            }, executor))
            .collect(Collectors.toList());
        
        // Ожидаем все запросы и собираем результаты
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join) // join не бросает checked exceptions
                .collect(Collectors.toList()));
    }
    
    public CompletableFuture<String> fetchWithTimeout(String url, Duration timeout) {
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return httpClient.get(url);
        }, executor);
        
        // Timeout с Java 9+
        return future.orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
            .exceptionally(ex -> {
                if (ex instanceof TimeoutException) {
                    return "Request timed out";
                }
                return "Error: " + ex.getMessage();
            });
    }
}
```

### Structured Concurrency (Java 21+)

Structured concurrency упрощает управление группами задач:

```java
import java.util.concurrent.StructuredTaskScope;

public String fetchUserData(String userId) throws Exception {
    // Все задачи в scope автоматически отменяются при выходе
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        
        // Запускаем несколько параллельных задач
        Future<String> user = scope.fork(() -> fetchUser(userId));
        Future<String> orders = scope.fork(() -> fetchOrders(userId));
        Future<String> preferences = scope.fork(() -> fetchPreferences(userId));
        
        // Ждём завершения всех или первой ошибки
        scope.join();
        scope.throwIfFailed(); // Бросает исключение, если была ошибка
        
        // Все задачи успешно завершены
        return combineResults(
            user.resultNow(),
            orders.resultNow(),
            preferences.resultNow()
        );
    } // Автоматическая очистка и отмена незавершённых задач
}

// ShutdownOnSuccess - завершается при первом успехе
public String fetchFromMultipleSources(List<String> urls) throws Exception {
    try (var scope = new StructuredTaskScope.ShutdownOnSuccess<String>()) {
        for (String url : urls) {
            scope.fork(() -> httpClient.get(url));
        }
        
        scope.join();
        return scope.result(); // Первый успешный результат
    }
}
```

### Future и его ограничения

До `CompletableFuture` существовал интерфейс `Future` с серьёзными ограничениями:

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

// Старый подход с Future
Future<String> future = executor.submit(() -> {
    Thread.sleep(1000);
    return "Result";
});

// Проблемы Future:
// 1. Блокирующее ожидание
String result = future.get(); // Блокирует поток!

// 2. Нет комбинирования
Future<String> f1 = executor.submit(() -> "A");
Future<String> f2 = executor.submit(() -> "B");
// Нет способа объединить f1 и f2 без блокировки

// 3. Нет обработки ошибок
try {
    result = future.get();
} catch (ExecutionException e) {
    // Приходится ловить исключения вручную
}

// 4. Нет отмены с callback
future.cancel(true); // Отменяет, но нет способа узнать об этом асинхронно
```

## Синхронизаторы и конкурентные структуры данных

### Locks: явная блокировка

**ReentrantLock** предоставляет более гибкую альтернативу `synchronized`:

```java
public class BankAccount {
    private final ReentrantLock lock = new ReentrantLock();
    private double balance;
    
    // Базовое использование
    public void deposit(double amount) {
        lock.lock();
        try {
            balance += amount;
        } finally {
            lock.unlock(); // ВСЕГДА в finally!
        }
    }
    
    // tryLock - попытка захвата без ожидания
    public boolean tryDeposit(double amount) {
        if (lock.tryLock()) {
            try {
                balance += amount;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false; // Блокировка занята
    }
    
    // tryLock с таймаутом
    public boolean depositWithTimeout(double amount, long timeout, TimeUnit unit) 
            throws InterruptedException {
        if (lock.tryLock(timeout, unit)) {
            try {
                balance += amount;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    // lockInterruptibly - можно прервать во время ожидания
    public void depositInterruptibly(double amount) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            balance += amount;
        } finally {
            lock.unlock();
        }
    }
}
```

**Справедливые (fair) блокировки:**

```java
// Справедливая блокировка - потоки получают доступ в порядке запроса
ReentrantLock fairLock = new ReentrantLock(true);

// Несправедливая (по умолчанию) - быстрее, но может приводить к starvation
ReentrantLock unfairLock = new ReentrantLock(false);
```

**ReadWriteLock для reader-writer паттерна:**

```java
public class CachedData {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    
    private Map<String, Object> cache = new HashMap<>();
    
    // Много читателей могут работать параллельно
    public Object get(String key) {
        readLock.lock();
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    // Писатель получает эксклюзивный доступ
    public void put(String key, Object value) {
        writeLock.lock();
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
    
    // Обновление с lock downgrading
    public void updateCache(Map<String, Object> newData) {
        writeLock.lock();
        try {
            cache.putAll(newData);
            
            // Понижение до read lock (не рекомендуется в общем случае)
            readLock.lock();
        } finally {
            writeLock.unlock();
        }
        
        try {
            // Теперь держим read lock
            processCache(cache);
        } finally {
            readLock.unlock();
        }
    }
}
```

**StampedLock: оптимистичное чтение (Java 8+):**

```java
public class Point {
    private final StampedLock sl = new StampedLock();
    private double x, y;
    
    // Оптимистичное чтение - самое быстрое
    public double distanceFromOrigin() {
        long stamp = sl.tryOptimisticRead(); // Не блокирует!
        double currentX = x;
        double currentY = y;
        
        if (!sl.validate(stamp)) {
            // Кто-то изменил данные, переходим к пессимистичному чтению
            stamp = sl.readLock();
            try {
                currentX = x;
                currentY = y;
            } finally {
                sl.unlockRead(stamp);
            }
        }
        
        return Math.sqrt(currentX * currentX + currentY * currentY);
    }
    
    // Пессимистичное чтение
    public double getX() {
        long stamp = sl.readLock();
        try {
            return x;
        } finally {
            sl.unlockRead(stamp);
        }
    }
    
    // Запись
    public void move(double deltaX, double deltaY) {
        long stamp = sl.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            sl.unlockWrite(stamp);
        }
    }
    
    // Условное обновление с lock upgrading
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = sl.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // Пытаемся повысить до write lock
                long ws = sl.tryConvertToWriteLock(stamp);
                if (ws != 0L) {
                    stamp = ws;
                    x = newX;
                    y = newY;
                    break;
                } else {
                    // Не получилось повысить, пробуем заново
                    sl.unlockRead(stamp);
                    stamp = sl.writeLock();
                }
            }
        } finally {
            sl.unlock(stamp);
        }
    }
}
```

### Синхронизаторы для координации потоков

**CountDownLatch - ожидание N событий:**

```java
public class ServiceInitializer {
    private final int serviceCount = 3;
    private final CountDownLatch startSignal = new CountDownLatch(1);
    private final CountDownLatch doneSignal = new CountDownLatch(serviceCount);
    
    public void startServices() throws InterruptedException {
        // Запускаем все сервисы
        for (int i = 0; i < serviceCount; i++) {
            final int serviceId = i;
            new Thread(() -> {
                try {
                    System.out.println("Service " + serviceId + " waiting to start");
                    startSignal.await(); // Ждём сигнала старта
                    
                    // Инициализация сервиса
                    initializeService(serviceId);
                    
                    System.out.println("Service " + serviceId + " initialized");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneSignal.countDown(); // Сигнализируем о завершении
                }
            }).start();
        }
        
        System.out.println("Waiting before starting services...");
        Thread.sleep(1000);
        
        startSignal.countDown(); // Даём сигнал старта всем
        doneSignal.await(); // Ждём инициализации всех сервисов
        
        System.out.println("All services initialized");
    }
}
```

**CyclicBarrier - синхронизация фаз:**

```java
public class ParallelComputation {
    private final int threadCount = 4;
    private final CyclicBarrier barrier = new CyclicBarrier(
        threadCount,
        () -> {
            // Действие при достижении барьера всеми потоками
            System.out.println("Barrier reached, merging results");
            mergeResults();
        }
    );
    
    public void compute() {
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int phase = 0; phase < 3; phase++) {
                        System.out.println("Thread " + threadId + " phase " + phase);
                        processPhase(threadId, phase);
                        
                        // Ждём остальных потоков
                        barrier.await();
                    }
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }
}
```

**Semaphore - ограничение доступа к ресурсу:**

```java
public class ConnectionPool {
    private final Semaphore available;
    private final List<Connection> connections;
    
    public ConnectionPool(int poolSize) {
        available = new Semaphore(poolSize, true); // fair = true
        connections = new ArrayList<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            connections.add(createConnection());
        }
    }
    
    public Connection getConnection() throws InterruptedException {
        available.acquire(); // Блокируется, если все разрешения заняты
        return getNextAvailableConnection();
    }
    
    public Connection tryGetConnection(long timeout, TimeUnit unit) 
            throws InterruptedException {
        if (available.tryAcquire(timeout, unit)) {
            return getNextAvailableConnection();
        }
        return null;
    }
    
    public void releaseConnection(Connection conn) {
        if (markAsAvailable(conn)) {
            available.release();
        }
    }
    
    // Ограничение rate limiting
    public static class RateLimiter {
        private final Semaphore semaphore;
        private final ScheduledExecutorService scheduler;
        
        public RateLimiter(int permitsPerSecond) {
            this.semaphore = new Semaphore(permitsPerSecond);
            this.scheduler = Executors.newScheduledThreadPool(1);
            
            // Восстанавливаем разрешения каждую секунду
            scheduler.scheduleAtFixedRate(() -> {
                semaphore.release(permitsPerSecond - semaphore.availablePermits());
            }, 1, 1, TimeUnit.SECONDS);
        }
        
        public void acquire() throws InterruptedException {
            semaphore.acquire();
        }
    }
}
```

**Phaser - динамические барьеры (Java 7+):**

```java
public class PhasedTasks {
    private final Phaser phaser = new Phaser(1); // Регистрируем main thread
    
    public void runTasks() {
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            phaser.register(); // Регистрируем новую задачу
            
            new Thread(() -> {
                for (int phase = 0; phase < 5; phase++) {
                    System.out.println("Task " + taskId + " phase " + phase);
                    processTask(taskId, phase);
                    
                    phaser.arriveAndAwaitAdvance(); // Ждём других
                }
                phaser.arriveAndDeregister(); // Завершаем участие
            }).start();
        }
        
        // Main thread ждёт завершения всех фаз
        for (int phase = 0; phase < 5; phase++) {
            phaser.arriveAndAwaitAdvance();
            System.out.println("Phase " + phase + " completed");
        }
        
        phaser.arriveAndDeregister(); // Main thread завершает
    }
}
```

### Атомарные типы

**Базовые атомарные классы:**

```java
public class AtomicCounter {
    // Атомарный счётчик
    private final AtomicInteger counter = new AtomicInteger(0);
    
    public int increment() {
        return counter.incrementAndGet(); // Атомарно: read-increment-write
    }
    
    public int decrement() {
        return counter.decrementAndGet();
    }
    
    // Compare-And-Set (CAS)
    public boolean setIfEquals(int expect, int update) {
        return counter.compareAndSet(expect, update);
    }
    
    // Update with function
    public int updateWithFunction(IntUnaryOperator updateFunction) {
        return counter.updateAndGet(updateFunction);
    }
    
    // Accumulate with binary operator
    public int accumulateWith(int value, IntBinaryOperator accumulatorFunction) {
        return counter.accumulateAndGet(value, accumulatorFunction);
    }
}

// AtomicReference для объектов
public class AtomicStack<T> {
    private static class Node<T> {
        final T value;
        final Node<T> next;
        
        Node(T value, Node<T> next) {
            this.value = value;
            this.next = next;
        }
    }
    
    private final AtomicReference<Node<T>> head = new AtomicReference<>();
    
    public void push(T value) {
        Node<T> newHead = new Node<>(value, null);
        Node<T> oldHead;
        do {
            oldHead = head.get();
            newHead.next = oldHead;
        } while (!head.compareAndSet(oldHead, newHead)); // Retry на конкуренцию
    }
    
    public T pop() {
        Node<T> oldHead;
        Node<T> newHead;
        do {
            oldHead = head.get();
            if (oldHead == null) {
                return null;
            }
            newHead = oldHead.next;
        } while (!head.compareAndSet(oldHead, newHead));
        
        return oldHead.value;
    }
}
```

**LongAdder для высококонкурентных счётчиков:**

```java
public class HighThroughputCounter {
    // LongAdder быстрее AtomicLong при высокой конкуренции
    private final LongAdder counter = new LongAdder();
    
    public void increment() {
        counter.increment(); // Очень быстро даже при конкуренции
    }
    
    public void add(long value) {
        counter.add(value);
    }
    
    public long sum() {
        return counter.sum(); // Агрегирует значения из всех ячеек
    }
    
    // Для метрик и статистики
    public long sumThenReset() {
        return counter.sumThenReset();
    }
}
```

### Конкурентные коллекции

**ConcurrentHashMap:**

```java
public class UserCache {
    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
    
    // Базовые операции - потокобезопасны
    public void put(String id, User user) {
        cache.put(id, user);
    }
    
    public User get(String id) {
        return cache.get(id);
    }
    
    // Атомарные операции
    public User getOrCreate(String id) {
        return cache.computeIfAbsent(id, key -> {
            // Вычисление выполнится только если ключа нет
            return loadUserFromDB(key);
        });
    }
    
    public void updateUser(String id, User newData) {
        cache.compute(id, (key, oldValue) -> {
            // Атомарное чтение и обновление
            if (oldValue == null) {
                return newData;
            }
            return mergeUsers(oldValue, newData);
        });
    }
    
    public void incrementCounter(String id) {
        cache.merge(id, new User(id, 1), (oldUser, newUser) -> {
            // Атомарное слияние значений
            oldUser.incrementCounter();
            return oldUser;
        });
    }
    
    // Bulk операции
    public void processAll() {
        cache.forEach(10, (key, value) -> {
            // Параллельная обработка с заданным parallelism threshold
            processUser(value);
        });
    }
    
    public long countActiveUsers() {
        return cache.reduceValues(10, 
            user -> user.isActive() ? 1L : 0L,
            Long::sum
        );
    }
}
```

**Блокирующие очереди:**

```java
// Producer-Consumer с BlockingQueue
public class TaskProcessor {
    private final BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);
    private final ExecutorService consumers = Executors.newFixedThreadPool(4);
    
    public void start() {
        // Запускаем потребителей
        for (int i = 0; i < 4; i++) {
            consumers.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Task task = queue.take(); // Блокируется, если пусто
                        processTask(task);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    
    public void submitTask(Task task) throws InterruptedException {
        queue.put(task); // Блокируется, если очередь полна
    }
    
    public boolean trySubmitTask(Task task, long timeout, TimeUnit unit) 
            throws InterruptedException {
        return queue.offer(task, timeout, unit);
    }
}

// Priority queue для задач с приоритетом
public class PriorityTaskProcessor {
    private final PriorityBlockingQueue<PriorityTask> queue = 
        new PriorityBlockingQueue<>(100, 
            Comparator.comparingInt(PriorityTask::getPriority).reversed()
        );
    
    public void submitTask(PriorityTask task) {
        queue.offer(task); // Неблокирующая операция (unbounded queue)
    }
    
    public PriorityTask getNextTask() throws InterruptedException {
        return queue.take(); // Всегда берёт задачу с наивысшим приоритетом
    }
}
```

**CopyOnWriteArrayList для read-heavy сценариев:**

```java
public class EventListeners {
    // Хорошо для listeners, где чтение частое, запись редкая
    private final CopyOnWriteArrayList<EventListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    public void addListener(EventListener listener) {
        listeners.add(listener); // Создаёт новую копию массива
    }
    
    public void removeListener(EventListener listener) {
        listeners.remove(listener); // Создаёт новую копию массива
    }
    
    public void fireEvent(Event event) {
        // Итерация не требует блокировок
        for (EventListener listener : listeners) {
            listener.onEvent(event);
        }
    }
}
```

**ConcurrentLinkedQueue - неблокирующая очередь:**

```java
public class LockFreeQueue {
    // Wait-free операции через CAS
    private final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
    
    public void enqueue(String item) {
        queue.offer(item); // Всегда успешна, не блокируется
    }
    
    public String dequeue() {
        return queue.poll(); // null если пусто, не блокируется
    }
    
    public int size() {
        return queue.size(); // Может быть неточным в многопоточном окружении
    }
}
```

## Потоковое локальное состояние и неизменяемость

### ThreadLocal: состояние, привязанное к потоку

`ThreadLocal` позволяет каждому потоку иметь свою независимую копию переменной:

**Базовое использование:**

```java
public class ThreadLocalExample {
    // Каждый поток получит свой SimpleDateFormat
    private static final ThreadLocal<SimpleDateFormat> dateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    public String formatDate(Date date) {
        // Безопасно в многопоточном окружении
        return dateFormat.get().format(date);
    }
    
    // Очистка после использования
    public void cleanup() {
        dateFormat.remove(); // Важно для пулов потоков!
    }
}
```

**Практический пример: контекст запроса в веб-приложении:**

```java
public class RequestContext {
    private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();
    
    public static class UserContext {
        private final String userId;
        private final String requestId;
        private final long startTime;
        
        public UserContext(String userId, String requestId) {
            this.userId = userId;
            this.requestId = requestId;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getUserId() { return userId; }
        public String getRequestId() { return requestId; }
        public long getElapsedTime() { 
            return System.currentTimeMillis() - startTime; 
        }
    }
    
    // Servlet filter устанавливает контекст
    public static void setContext(String userId, String requestId) {
        userContext.set(new UserContext(userId, requestId));
    }
    
    public static UserContext getContext() {
        return userContext.get();
    }
    
    // ОБЯЗАТЕЛЬНО очищать в finally блоке
    public static void clearContext() {
        userContext.remove();
    }
    
    // Пример использования в filter
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        try {
            String userId = extractUserId(req);
            String requestId = UUID.randomUUID().toString();
            setContext(userId, requestId);
            
            chain.doFilter(req, res);
        } finally {
            // Критически важно для пулов потоков!
            clearContext();
        }
    }
}
```

**InheritableThreadLocal для дочерних потоков:**

```java
public class InheritableContext {
    // Дочерние потоки наследуют значение от родителя
    private static final InheritableThreadLocal<String> context = 
        new InheritableThreadLocal<>();
    
    public static void demo() {
        context.set("Parent context");
        
        new Thread(() -> {
            // Автоматически получает "Parent context"
            System.out.println("Child sees: " + context.get());
            
            // Может изменить свою копию
            context.set("Child context");
            System.out.println("Child changed to: " + context.get());
        }).start();
        
        // Родитель не видит изменения дочернего потока
        System.out.println("Parent still has: " + context.get());
    }
}
```

**Проблемы ThreadLocal:**

1. **Утечки памяти в пулах потоков**: Потоки переиспользуются, ThreadLocal остаётся
2. **Неявные зависимости**: Код зависит от неявного состояния
3. **Сложность тестирования**: Тесты должны очищать ThreadLocal

**Best practices:**

```java
public class ThreadLocalBestPractices {
    // 1. Используйте static final для ThreadLocal переменных
    private static final ThreadLocal<Context> context = new ThreadLocal<>();
    
    // 2. Всегда очищайте в finally
    public void processRequest() {
        try {
            context.set(new Context());
            // Обработка
        } finally {
            context.remove(); // Предотвращает утечки!
        }
    }
    
    // 3. Рассмотрите альтернативы ThreadLocal
    // - Передача параметров явно
    // - Неизменяемые объекты
    // - Context propagation libraries (например, в Project Reactor)
}
```

### Неизменяемость как альтернатива синхронизации

Неизменяемые (immutable) объекты автоматически потокобезопасны:

**Правильный неизменяемый класс:**

```java
public final class ImmutableUser {
    private final String id;
    private final String name;
    private final List<String> roles;
    
    public ImmutableUser(String id, String name, List<String> roles) {
        this.id = id;
        this.name = name;
        // Защищаемся от изменения через переданную коллекцию
        this.roles = List.copyOf(roles); // Java 10+
        // или Collections.unmodifiableList(new ArrayList<>(roles))
    }
    
    // Только getters, нет setters
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getRoles() { return roles; } // Возвращает неизменяемую копию
    
    // "Модификация" через создание нового объекта
    public ImmutableUser withName(String newName) {
        return new ImmutableUser(this.id, newName, this.roles);
    }
}
```

**Использование Records (Java 14+):**

```java
// Record автоматически неизменяем
public record User(String id, String name, List<String> roles) {
    // Каноничный конструктор для валидации
    public User {
        if (id == null || name == null) {
            throw new IllegalArgumentException("ID and name are required");
        }
        // Защищаем от изменений
        roles = List.copyOf(roles);
    }
    
    // Можно добавлять методы
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
```

**Преимущества неизменяемости:**
- Автоматическая потокобезопасность
- Отсутствие синхронизации
- Простота рассуждения о коде
- Можно безопасно использовать как ключи Map
- Можно безопасно кэшировать

## Диагностика и устранение проблем

### Основные проблемы многопоточности

#### 1. Deadlock (Взаимная блокировка)

Deadlock возникает, когда два или более потока ждут ресурсы друг друга и никто не может продолжить.

**Пример deadlock:**

```java
public class DeadlockExample {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    public void method1() {
        synchronized (lock1) {
            System.out.println("Method1: holding lock1");
            sleep(100);
            
            synchronized (lock2) { // Ждёт lock2
                System.out.println("Method1: holding lock1 & lock2");
            }
        }
    }
    
    public void method2() {
        synchronized (lock2) {
            System.out.println("Method2: holding lock2");
            sleep(100);
            
            synchronized (lock1) { // Ждёт lock1
                System.out.println("Method2: holding lock2 & lock1");
            }
        }
    }
    
    public static void main(String[] args) {
        DeadlockExample example = new DeadlockExample();
        
        // Thread 1 захватит lock1, затем будет ждать lock2
        new Thread(example::method1).start();
        
        // Thread 2 захватит lock2, затем будет ждать lock1
        new Thread(example::method2).start();
        
        // DEADLOCK!
    }
}
```

**Решение 1: Фиксированный порядок захвата:**

```java
public class DeadlockSolution {
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    
    // Всегда захватываем в порядке lock1 -> lock2
    public void method1() {
        synchronized (lock1) {
            synchronized (lock2) {
                // Работа
            }
        }
    }
    
    public void method2() {
        synchronized (lock1) { // Тот же порядок!
            synchronized (lock2) {
                // Работа
            }
        }
    }
}
```

**Решение 2: Таймауты с tryLock:**

```java
public class DeadlockSolutionWithTimeout {
    private final ReentrantLock lock1 = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    
    public boolean transferMoney(Account from, Account to, double amount) {
        ReentrantLock firstLock = from.getLock();
        ReentrantLock secondLock = to.getLock();
        
        while (true) {
            boolean gotFirstLock = false;
            boolean gotSecondLock = false;
            
            try {
                gotFirstLock = firstLock.tryLock(100, TimeUnit.MILLISECONDS);
                if (!gotFirstLock) {
                    continue; // Retry
                }
                
                gotSecondLock = secondLock.tryLock(100, TimeUnit.MILLISECONDS);
                if (!gotSecondLock) {
                    continue; // Retry
                }
                
                // Обе блокировки захвачены
                from.withdraw(amount);
                to.deposit(amount);
                return true;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } finally {
                if (gotSecondLock) {
                    secondLock.unlock();
                }
                if (gotFirstLock) {
                    firstLock.unlock();
                }
            }
        }
    }
}
```

**Обнаружение deadlock:**

```java
public class DeadlockDetector {
    public static void detectDeadlocks() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
            System.out.println("Detected " + deadlockedThreads.length + " deadlocked threads:");
            
            for (ThreadInfo info : threadInfos) {
                System.out.println("Thread: " + info.getThreadName());
                System.out.println("  Locked on: " + info.getLockName());
                System.out.println("  Owned by: " + info.getLockOwnerName());
                System.out.println("  Stack trace:");
                
                for (StackTraceElement element : info.getStackTrace()) {
                    System.out.println("    " + element);
                }
            }
        }
    }
}
```

#### 2. Livelock

Livelock — потоки постоянно меняют состояние в ответ на действия других потоков, но не продвигаются вперёд.

**Пример livelock:**

```java
public class LivelockExample {
    static class Spoon {
        private Diner owner;
        
        public synchronized void use() {
            System.out.println(owner.name + " is eating");
        }
        
        public synchronized void setOwner(Diner diner) {
            this.owner = diner;
        }
        
        public synchronized Diner getOwner() {
            return owner;
        }
    }
    
    static class Diner {
        private String name;
        private boolean isHungry;
        
        public Diner(String name) {
            this.name = name;
            this.isHungry = true;
        }
        
        public void eatWith(Spoon spoon, Diner spouse) {
            while (isHungry) {
                // Если ложка не моя, жду
                if (spoon.getOwner() != this) {
                    sleep(1);
                    continue;
                }
                
                // Если супруг голоден, передаю ложку (вежливость!)
                if (spouse.isHungry) {
                    System.out.println(name + ": You eat first, dear " + spouse.name);
                    spoon.setOwner(spouse);
                    continue; // LIVELOCK - постоянно передают друг другу!
                }
                
                // Ем
                spoon.use();
                isHungry = false;
                spoon.setOwner(spouse);
            }
        }
    }
}
```

**Решение livelock: случайные задержки (backoff):**

```java
public class LivelockSolution {
    public void eatWith(Spoon spoon, Diner spouse) {
        Random random = new Random();
        
        while (isHungry) {
            if (spoon.getOwner() != this) {
                sleep(1);
                continue;
            }
            
            if (spouse.isHungry) {
                System.out.println(name + ": You eat first");
                spoon.setOwner(spouse);
                
                // Случайная задержка нарушает синхронность
                sleep(random.nextInt(100));
                continue;
            }
            
            spoon.use();
            isHungry = false;
        }
    }
}
```

#### 3. Starvation (Голодание)

Поток никогда не получает доступ к ресурсу из-за приоритетов или несправедливого планирования.

**Пример starvation:**

```java
public class StarvationExample {
    private final Object lock = new Object();
    
    public void highPriorityTask() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        while (true) {
            synchronized (lock) {
                // Постоянно захватывает блокировку
                doWork();
            }
        }
    }
    
    public void lowPriorityTask() {
        Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        synchronized (lock) {
            // Может никогда не получить доступ!
            doWork();
        }
    }
}
```

**Решение: справедливые блокировки:**

```java
public class StarvationSolution {
    // Fair lock - потоки получают доступ в порядке запроса
    private final ReentrantLock fairLock = new ReentrantLock(true);
    
    public void task() {
        fairLock.lock();
        try {
            doWork();
        } finally {
            fairLock.unlock();
        }
    }
    
    // Разделение пулов для разных типов задач
    private final ExecutorService highPriorityPool = 
        Executors.newFixedThreadPool(2);
    private final ExecutorService lowPriorityPool = 
        Executors.newFixedThreadPool(2);
    
    public void submitTask(Task task) {
        if (task.getPriority() == Priority.HIGH) {
            highPriorityPool.submit(task);
        } else {
            lowPriorityPool.submit(task);
        }
    }
}
```

#### 4. Race Condition (Состояние гонки)

Результат зависит от недетерминированного порядка выполнения потоков.

**Пример race condition:**

```java
public class RaceConditionExample {
    private int counter = 0;
    
    public void increment() {
        counter++; // НЕ атомарно: read -> increment -> write
    }
    
    public static void main(String[] args) throws InterruptedException {
        RaceConditionExample example = new RaceConditionExample();
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                example.increment();
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 10000; i++) {
                example.increment();
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Ожидаем 20000, но получим меньше из-за race condition
        System.out.println("Counter: " + example.counter);
    }
}
```

**Решения:**

```java
public class RaceConditionSolutions {
    // Решение 1: synchronized
    private int counter1 = 0;
    
    public synchronized void incrementSync() {
        counter1++;
    }
    
    // Решение 2: AtomicInteger
    private final AtomicInteger counter2 = new AtomicInteger(0);
    
    public void incrementAtomic() {
        counter2.incrementAndGet();
    }
    
    // Решение 3: Lock
    private int counter3 = 0;
    private final ReentrantLock lock = new ReentrantLock();
    
    public void incrementWithLock() {
        lock.lock();
        try {
            counter3++;
        } finally {
            lock.unlock();
        }
    }
    
    // Решение 4: volatile (только для простых случаев)
    private volatile boolean flag = false;
    
    public void setFlag() {
        flag = true; // Атомарная операция
    }
}
```

### Инструменты диагностики

**1. jstack - дамп потоков:**

```bash
# Получить PID процесса
jps

# Дамп потоков
jstack <pid> > thread-dump.txt

# Поиск deadlock
jstack <pid> | grep -A 10 "Found one Java-level deadlock"
```

**2. jcmd - многофункциональный инструмент:**

```bash
# Дамп потоков
jcmd <pid> Thread.print

# Статистика GC
jcmd <pid> GC.heap_info

# VM флаги
jcmd <pid> VM.flags
```

**3. Java Flight Recorder (JFR):**

```bash
# Запуск записи
jcmd <pid> JFR.start name=myrecording duration=60s filename=recording.jfr

# Остановка
jcmd <pid> JFR.stop name=myrecording

# Анализ в JDK Mission Control
```

**4. Программное мониторинг:**

```java
public class ThreadMonitoring {
    public static void monitorThreads() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Общая информация
        System.out.println("Thread count: " + threadMXBean.getThreadCount());
        System.out.println("Peak thread count: " + threadMXBean.getPeakThreadCount());
        System.out.println("Daemon thread count: " + threadMXBean.getDaemonThreadCount());
        
        // Информация о конкретных потоках
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds, Integer.MAX_VALUE);
        
        for (ThreadInfo info : threadInfos) {
            if (info == null) continue;
            
            System.out.println("\nThread: " + info.getThreadName());
            System.out.println("  State: " + info.getThreadState());
            System.out.println("  Blocked time: " + info.getBlockedTime());
            System.out.println("  Blocked count: " + info.getBlockedCount());
            System.out.println("  Waited time: " + info.getWaitedTime());
            System.out.println("  Waited count: " + info.getWaitedCount());
            
            if (info.getLockName() != null) {
                System.out.println("  Waiting on: " + info.getLockName());
            }
            
            // Stack trace
            StackTraceElement[] stack = info.getStackTrace();
            for (int i = 0; i < Math.min(5, stack.length); i++) {
                System.out.println("    " + stack[i]);
            }
        }
    }
    
    // Мониторинг contention
    public static void monitorContention() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        
        // Включить мониторинг contention (может быть дорого!)
        if (threadMXBean.isThreadContentionMonitoringSupported()) {
            threadMXBean.setThreadContentionMonitoringEnabled(true);
            
            // Статистика
            long[] threadIds = threadMXBean.getAllThreadIds();
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);
            
            for (ThreadInfo info : threadInfos) {
                if (info == null) continue;
                
                System.out.println("Thread: " + info.getThreadName());
                System.out.println("  Total blocked time: " + info.getBlockedTime() + "ms");
                System.out.println("  Total blocked count: " + info.getBlockedCount());
            }
        }
    }
}
```

**5. async-profiler для lock profiling:**

```bash
# Профилирование блокировок
./profiler.sh -e lock -d 30 -f locks.html <pid>
```

### Тестирование многопоточного кода

**1. jcstress - Concurrency Stress Tests:**

```java
@JCStressTest
@Outcome(id = "0, 0", expect = Expect.ACCEPTABLE, desc = "Both threads see 0")
@Outcome(id = "1, 1", expect = Expect.ACCEPTABLE, desc = "Both threads see 1")
@Outcome(id = "0, 1", expect = Expect.ACCEPTABLE, desc = "T1 sees 0, T2 sees 1")
@Outcome(id = "1, 0", expect = Expect.FORBIDDEN, desc = "Impossible with proper sync")
@State
public class CounterTest {
    private volatile int value = 0;
    
    @Actor
    public void actor1(II_Result r) {
        r.r1 = value;
        value = 1;
    }
    
    @Actor
    public void actor2(II_Result r) {
        r.r2 = value;
    }
}
```

**2. Повторные запуски:**

```java
@Test
public void testConcurrency() {
    // Повторяем тест много раз для выявления редких гонок
    for (int i = 0; i < 1000; i++) {
        runConcurrentTest();
    }
}
```

**3. Thread.yield() для увеличения вероятности гонок:**

```java
public void increment() {
    int current = counter;
    Thread.yield(); // Повышает шанс конкуренции
    counter = current + 1;
}
```

## Шаблоны и практические приёмы

### Producer-Consumer Pattern

Классический паттерн для обмена данными между потоками через очередь:

**Базовая реализация:**

```java
public class ProducerConsumerExample {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
    private final ExecutorService producers = Executors.newFixedThreadPool(2);
    private final ExecutorService consumers = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;
    
    public void start() {
        // Запускаем производителей
        for (int i = 0; i < 2; i++) {
            final int producerId = i;
            producers.submit(() -> {
                while (running) {
                    try {
                        Task task = generateTask(producerId);
                        queue.put(task); // Блокируется при переполнении
                        System.out.println("Producer " + producerId + " created task");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Запускаем потребителей
        for (int i = 0; i < 4; i++) {
            final int consumerId = i;
            consumers.submit(() -> {
                while (running) {
                    try {
                        Task task = queue.take(); // Блокируется при пустой очереди
                        processTask(task, consumerId);
                        System.out.println("Consumer " + consumerId + " processed task");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
    }
    
    public void stop() {
        running = false;
        producers.shutdown();
        consumers.shutdown();
    }
}
```

**С контролем backpressure через Semaphore:**

```java
public class BackpressureProducerConsumer {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);
    private final Semaphore rateLimiter = new Semaphore(10); // Max 10 задач в секунду
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public BackpressureProducerConsumer() {
        // Восстанавливаем разрешения каждую секунду
        scheduler.scheduleAtFixedRate(() -> {
            int toRelease = 10 - rateLimiter.availablePermits();
            if (toRelease > 0) {
                rateLimiter.release(toRelease);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    
    public void submitTask(Task task) throws InterruptedException {
        // Ждём разрешения на отправку
        rateLimiter.acquire();
        queue.put(task);
    }
    
    public boolean trySubmitTask(Task task, long timeout, TimeUnit unit) 
            throws InterruptedException {
        if (rateLimiter.tryAcquire(timeout, unit)) {
            return queue.offer(task, timeout, unit);
        }
        return false;
    }
}
```

### Worker Pool Pattern

Центральная очередь задач с группой рабочих потоков:

```java
public class WorkerPool {
    private final BlockingQueue<Runnable> taskQueue;
    private final List<Worker> workers = new ArrayList<>();
    private volatile boolean isShutdown = false;
    
    public WorkerPool(int poolSize, int queueCapacity) {
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        
        // Создаём рабочие потоки
        for (int i = 0; i < poolSize; i++) {
            Worker worker = new Worker(i);
            workers.add(worker);
            worker.start();
        }
    }
    
    private class Worker extends Thread {
        private final int id;
        
        Worker(int id) {
            super("Worker-" + id);
            this.id = id;
        }
        
        @Override
        public void run() {
            while (!isShutdown || !taskQueue.isEmpty()) {
                try {
                    Runnable task = taskQueue.poll(1, TimeUnit.SECONDS);
                    if (task != null) {
                        System.out.println("Worker " + id + " processing task");
                        task.run();
                    }
                } catch (InterruptedException e) {
                    if (isShutdown) {
                        break;
                    }
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("Worker " + id + " error: " + e.getMessage());
                }
            }
            System.out.println("Worker " + id + " shutting down");
        }
    }
    
    public void submit(Runnable task) throws InterruptedException {
        if (isShutdown) {
            throw new IllegalStateException("Pool is shutdown");
        }
        taskQueue.put(task);
    }
    
    public void shutdown() {
        isShutdown = true;
        // Прерываем все worker threads
        workers.forEach(Thread::interrupt);
    }
    
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        for (Worker worker : workers) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }
            worker.join(remaining);
        }
    }
}
```

### Fork/Join Pattern

Рекурсивное разделение задач для параллельных вычислений:

**Параллельная фильтрация и маппинг:**

```java
public class ParallelMapFilter<T, R> extends RecursiveTask<List<R>> {
    private static final int THRESHOLD = 1000;
    private final List<T> source;
    private final Predicate<T> filter;
    private final Function<T, R> mapper;
    private final int start;
    private final int end;
    
    public ParallelMapFilter(List<T> source, Predicate<T> filter, 
                            Function<T, R> mapper) {
        this(source, filter, mapper, 0, source.size());
    }
    
    private ParallelMapFilter(List<T> source, Predicate<T> filter,
                             Function<T, R> mapper, int start, int end) {
        this.source = source;
        this.filter = filter;
        this.mapper = mapper;
        this.start = start;
        this.end = end;
    }
    
    @Override
    protected List<R> compute() {
        int length = end - start;
        
        if (length <= THRESHOLD) {
            // Базовый случай: обрабатываем напрямую
            List<R> result = new ArrayList<>();
            for (int i = start; i < end; i++) {
                T item = source.get(i);
                if (filter.test(item)) {
                    result.add(mapper.apply(item));
                }
            }
            return result;
        } else {
            // Рекурсивный случай: делим задачу
            int mid = start + length / 2;
            
            ParallelMapFilter<T, R> leftTask = 
                new ParallelMapFilter<>(source, filter, mapper, start, mid);
            ParallelMapFilter<T, R> rightTask = 
                new ParallelMapFilter<>(source, filter, mapper, mid, end);
            
            // Запускаем левую задачу асинхронно
            leftTask.fork();
            
            // Выполняем правую задачу в текущем потоке
            List<R> rightResult = rightTask.compute();
            
            // Ждём результат левой задачи
            List<R> leftResult = leftTask.join();
            
            // Объединяем результаты
            leftResult.addAll(rightResult);
            return leftResult;
        }
    }
    
    public static void main(String[] args) {
        List<Integer> numbers = IntStream.range(0, 1_000_000)
            .boxed()
            .collect(Collectors.toList());
        
        ForkJoinPool pool = ForkJoinPool.commonPool();
        
        ParallelMapFilter<Integer, String> task = new ParallelMapFilter<>(
            numbers,
            n -> n % 2 == 0,           // Фильтр: только чётные
            n -> "Number: " + n        // Маппинг в строку
        );
        
        List<String> result = pool.invoke(task);
        System.out.println("Processed " + result.size() + " items");
    }
}
```

### Reactive Streams Pattern

Асинхронная обработка потоков событий с контролем скорости (backpressure):

**Пример с Flow API (Java 9+):**

```java
public class ReactiveExample {
    // Publisher генерирует данные
    static class DataPublisher implements Flow.Publisher<Integer> {
        @Override
        public void subscribe(Flow.Subscriber<? super Integer> subscriber) {
            subscriber.onSubscribe(new Flow.Subscription() {
                private int current = 0;
                private boolean cancelled = false;
                
                @Override
                public void request(long n) {
                    // Отправляем n элементов
                    for (int i = 0; i < n && !cancelled; i++) {
                        if (current < 100) {
                            subscriber.onNext(current++);
                        } else {
                            subscriber.onComplete();
                            break;
                        }
                    }
                }
                
                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
    }
    
    // Subscriber потребляет данные
    static class DataSubscriber implements Flow.Subscriber<Integer> {
        private Flow.Subscription subscription;
        private final int batchSize;
        
        DataSubscriber(int batchSize) {
            this.batchSize = batchSize;
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            // Запрашиваем первую порцию
            subscription.request(batchSize);
        }
        
        @Override
        public void onNext(Integer item) {
            System.out.println("Processing: " + item);
            
            // Имитация обработки
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Запрашиваем следующий элемент
            subscription.request(1);
        }
        
        @Override
        public void onError(Throwable throwable) {
            System.err.println("Error: " + throwable.getMessage());
        }
        
        @Override
        public void onComplete() {
            System.out.println("Processing complete");
        }
    }
    
    public static void main(String[] args) {
        DataPublisher publisher = new DataPublisher();
        DataSubscriber subscriber = new DataSubscriber(10);
        
        publisher.subscribe(subscriber);
    }
}
```

### Double-Checked Locking Pattern

Ленивая инициализация с минимизацией синхронизации:

```java
public class Singleton {
    // volatile обязателен для корректности!
    private static volatile Singleton instance;
    
    private Singleton() {
        // Приватный конструктор
    }
    
    public static Singleton getInstance() {
        // Первая проверка без блокировки
        if (instance == null) {
            synchronized (Singleton.class) {
                // Вторая проверка под блокировкой
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
    
    // Альтернатива: Initialization-on-demand holder idiom (проще и безопаснее)
    private static class Holder {
        static final Singleton INSTANCE = new Singleton();
    }
    
    public static Singleton getInstanceSafe() {
        return Holder.INSTANCE; // Thread-safe благодаря class loader
    }
}
```

### Thread-Per-Message Pattern

Создание нового потока для каждого сообщения/запроса:

```java
// Старый подход (плохо - слишком много потоков)
public class ThreadPerMessageOld {
    public void handleRequest(Request request) {
        new Thread(() -> {
            processRequest(request);
        }).start();
    }
}

// Современный подход с ExecutorService
public class ThreadPerMessageModern {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    
    public void handleRequest(Request request) {
        executor.submit(() -> {
            processRequest(request);
        });
    }
}

// С виртуальными потоками (Java 21+)
public class ThreadPerMessageVirtual {
    public void handleRequest(Request request) {
        Thread.startVirtualThread(() -> {
            processRequest(request);
        });
    }
    
    // Или с executor
    private final ExecutorService executor = 
        Executors.newVirtualThreadPerTaskExecutor();
    
    public void handleRequestWithExecutor(Request request) {
        executor.submit(() -> {
            processRequest(request);
        });
    }
}
```

### Параллельные коллекции и Stream API

```java
public class ParallelCollectionsExample {
    // Параллельная сортировка
    public void parallelSort() {
        int[] array = new int[1_000_000];
        // Заполняем массив...
        
        Arrays.parallelSort(array); // Использует ForkJoinPool
    }
    
    // Параллельные стримы
    public long countActiveUsers(List<User> users) {
        return users.parallelStream()
            .filter(User::isActive)
            .count();
    }
    
    // Параллельное преобразование
    public List<String> processInParallel(List<Data> data) {
        return data.parallelStream()
            .map(this::expensiveOperation)
            .collect(Collectors.toList());
    }
    
    // ВАЖНО: parallel() эффективен только для:
    // - Больших коллекций (> 10000 элементов)
    // - CPU-bound операций
    // - Ассоциативных операций (порядок не важен)
    
    // Плохой пример (overhead > benefit):
    public long badParallel(List<Integer> numbers) {
        return numbers.parallelStream()
            .filter(n -> n > 0)  // Слишком простая операция
            .count();
    }
    
    // Хороший пример:
    public List<Result> goodParallel(List<Data> data) {
        return data.parallelStream()
            .map(this::complexComputation) // CPU-intensive
            .collect(Collectors.toList());
    }
}
```

## Практические упражнения

### Упражнение 1: Producer-Consumer с Rate Limiting

**Задача**: Реализуйте производитель-потребитель с ограничением скорости через `Semaphore`.

**Требования:**
- Производитель генерирует максимум 100 задач в секунду
- Потребители обрабатывают задачи параллельно (4 потока)
- Размер очереди ограничен 50 элементами
- При переполнении очереди производитель должен ждать

**Подсказки:**
```java
public class RateLimitedProducerConsumer {
    private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>(50);
    private final Semaphore rateLimiter = new Semaphore(100);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public RateLimitedProducerConsumer() {
        // TODO: Восстановление разрешений каждую секунду
    }
    
    public void produce(Task task) throws InterruptedException {
        // TODO: Acquire разрешение и добавить в очередь
    }
    
    public void consume() {
        // TODO: Взять из очереди и обработать
    }
}
```

### Упражнение 2: Асинхронные HTTP запросы с таймаутом

**Задача**: Реализуйте сервис, который выполняет несколько HTTP-запросов параллельно с использованием `CompletableFuture` и обрабатывает отмену при тайм-ауте.

**Требования:**
- Запросы выполняются параллельно
- Общий таймаут 5 секунд
- При таймауте все незавершённые запросы отменяются
- Возвращаются только успешные результаты

**Подсказки:**
```java
public class AsyncHttpService {
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    public CompletableFuture<List<String>> fetchAll(List<String> urls, Duration timeout) {
        // TODO: Создать CompletableFuture для каждого URL
        // TODO: Использовать CompletableFuture.allOf() или anyOf()
        // TODO: Добавить обработку таймаута через orTimeout()
        // TODO: Собрать успешные результаты
    }
    
    private CompletableFuture<String> fetchUrl(String url) {
        return CompletableFuture.supplyAsync(() -> {
            // Имитация HTTP запроса
            return httpClient.get(url);
        }, executor);
    }
}
```

### Упражнение 3: Обнаружение и устранение Deadlock

**Задача**: В предоставленном коде есть deadlock. Найдите и исправьте его.

**Проблемный код:**
```java
public class BankTransfer {
    public static class Account {
        private final String id;
        private double balance;
        private final Object lock = new Object();
        
        public Account(String id, double balance) {
            this.id = id;
            this.balance = balance;
        }
        
        public void withdraw(double amount) {
            balance -= amount;
        }
        
        public void deposit(double amount) {
            balance += amount;
        }
        
        public Object getLock() {
            return lock;
        }
    }
    
    public static void transfer(Account from, Account to, double amount) {
        synchronized (from.getLock()) {
            synchronized (to.getLock()) {
                from.withdraw(amount);
                to.deposit(amount);
            }
        }
    }
    
    public static void main(String[] args) {
        Account account1 = new Account("A", 1000);
        Account account2 = new Account("B", 1000);
        
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transfer(account1, account2, 10);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                transfer(account2, account1, 10); // DEADLOCK!
            }
        });
        
        t1.start();
        t2.start();
    }
}
```

**Задания:**
1. Объясните, почему возникает deadlock
2. Исправьте код, используя фиксированный порядок захвата блокировок
3. Реализуйте альтернативное решение с `tryLock()` и таймаутами
4. Напишите метод для программного обнаружения deadlock

### Упражнение 4: Thread-safe LRU Cache

**Задача**: Реализуйте потокобезопасный LRU (Least Recently Used) кэш.

**Требования:**
- Фиксированный размер (capacity)
- При превышении размера удаляется наименее использованный элемент
- Операции get и put должны быть потокобезопасными
- Операция get обновляет "время последнего использования"

**Подсказки:**
```java
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, V> cache;
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        // TODO: Используйте LinkedHashMap с accessOrder=true
        // TODO: Добавьте синхронизацию (synchronized или ReadWriteLock)
    }
    
    public V get(K key) {
        // TODO: Потокобезопасное чтение
    }
    
    public void put(K key, V value) {
        // TODO: Потокобезопасная запись с удалением старых элементов
    }
}
```

### Упражнение 5: Fork/Join для параллельной обработки файлов

**Задача**: Реализуйте параллельный подсчёт слов во всех файлах в директории.

**Требования:**
- Используйте ForkJoinPool
- Рекурсивно обходите поддиректории
- Для каждого файла считайте количество слов
- Верните общее количество слов во всех файлах

**Подсказки:**
```java
public class ParallelWordCount extends RecursiveTask<Long> {
    private final File file;
    private static final long THRESHOLD = 1024 * 1024; // 1MB
    
    public ParallelWordCount(File file) {
        this.file = file;
    }
    
    @Override
    protected Long compute() {
        if (file.isFile()) {
            // TODO: Если файл маленький, считаем напрямую
            // TODO: Если большой, делим на части
        } else if (file.isDirectory()) {
            // TODO: Создаём subtask для каждого файла/поддиректории
            // TODO: Используйте invokeAll() или fork/join
        }
        return 0L;
    }
}
```

### Упражнение 6: Реализация ThreadPoolExecutor с метриками

**Задача**: Создайте кастомный ThreadPoolExecutor с мониторингом.

**Требования:**
- Отслеживайте количество выполненных задач
- Измеряйте среднее время выполнения задач
- Подсчитывайте количество отклонённых задач
- Логируйте длинные задачи (> 5 секунд)

**Подсказки:**
```java
public class MonitoredThreadPool extends ThreadPoolExecutor {
    private final AtomicLong completedTasks = new AtomicLong();
    private final AtomicLong totalExecutionTime = new AtomicLong();
    private final AtomicLong rejectedTasks = new AtomicLong();
    private final ConcurrentHashMap<Runnable, Long> taskStartTimes = new ConcurrentHashMap<>();
    
    public MonitoredThreadPool(int corePoolSize, int maximumPoolSize,
                              long keepAliveTime, TimeUnit unit,
                              BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }
    
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        // TODO: Запомнить время старта
    }
    
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        // TODO: Посчитать время выполнения
        // TODO: Обновить метрики
    }
    
    public ExecutionMetrics getMetrics() {
        // TODO: Вернуть текущие метрики
    }
}
```

## Вопросы на собеседовании

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
