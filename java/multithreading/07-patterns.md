# Шаблоны и практические приёмы

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

