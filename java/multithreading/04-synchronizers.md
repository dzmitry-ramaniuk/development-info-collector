# Синхронизаторы и конкурентные структуры данных

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

