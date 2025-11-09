# Практические упражнения

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

