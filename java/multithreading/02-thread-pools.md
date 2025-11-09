# Управление потоками и пулами

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

