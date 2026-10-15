---
---

# Практические упражнения

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Упражнение 1: Producer-Consumer с Rate Limiting](#упражнение-1-producer-consumer-с-rate-limiting)
3. [Упражнение 2: Асинхронные HTTP запросы с таймаутом](#упражнение-2-асинхронные-http-запросы-с-таймаутом)
4. [Упражнение 3: Обнаружение и устранение Deadlock](#упражнение-3-обнаружение-и-устранение-deadlock)
5. [Упражнение 4: Thread-safe LRU Cache](#упражнение-4-thread-safe-lru-cache)
6. [Упражнение 5: Fork/Join для параллельной обработки файлов](#упражнение-5-forkjoin-для-параллельной-обработки-файлов)
7. [Упражнение 6: Реализация ThreadPoolExecutor с метриками](#упражнение-6-реализация-threadpoolexecutor-с-метриками)
8. [Разбор и критерии решений](#разбор-и-критерии-решений)
   - [1. Ограничение скорости и остановка](#1-ограничение-скорости-и-остановка)
   - [2. Общий deadline и частичный успех](#2-общий-deadline-и-частичный-успех)
   - [3. Порядок блокировок](#3-порядок-блокировок)
   - [4. LRU: чтение меняет порядок](#4-lru-чтение-меняет-порядок)
   - [5. Границы файловых фрагментов](#5-границы-файловых-фрагментов)
   - [6. Метрики и исключения](#6-метрики-и-исключения)

## Актуальность материала

- **Дата проверки:** 2026-10-15 — точечная сверка: критерии решений: deadline, locks, LRU, блокирующий I/O и метрики. Остальной материал не проходил повторную полную проверку; прежняя дата — 2026-08-04.
- **Целевая версия или диапазон:** Java SE 17–25; preview-возможности рассматриваются только там, где явно помечены
- **Статус примеров:** исправленные стабильные API — `current` (Java SE 17); проверка исполняемого примера описана в [README примеров](../../examples/java-concurrency/README.html). Фрагменты с зависимостями не считаются автоматически запущенными.
- **Первичные источники:** (точечная сверка) [CompletableFuture](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html); [LinkedHashMap](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/LinkedHashMap.html); [ThreadPoolExecutor](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html); [ManagedBlocker](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ForkJoinPool.ManagedBlocker.html)

## Упражнение 1: Producer-Consumer с Rate Limiting

**Задача**: Реализуйте производитель-потребитель с ограничением скорости через `Semaphore`.

**Требования:**
- Производитель получает максимум 100 разрешений в каждом фиксированном секундном окне; burst на границе окон допустим. Строгий лимит для любого скользящего интервала — дополнительное задание
- Потребители обрабатывают задачи параллельно (4 потока)
- Размер очереди ограничен 50 элементами
- При переполнении очереди производитель должен ждать
- Остановка освобождает ожидающих производителей/потребителей и завершает scheduler

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

## Упражнение 2: Асинхронные HTTP запросы с таймаутом

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
        // TODO: Сохранить handles исходных операций для реальной отмены
        // TODO: Вести общий deadline; allOf/anyOf не отменяют остальные операции
        // TODO: Определить атомарный снимок успешных результатов при завершении
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

## Упражнение 3: Обнаружение и устранение Deadlock

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

## Упражнение 4: Thread-safe LRU Cache

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

## Упражнение 5: Fork/Join для параллельной обработки файлов

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

## Упражнение 6: Реализация ThreadPoolExecutor с метриками

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

## Разбор и критерии решений

### 1. Ограничение скорости и остановка

Отдельно задайте лимит задач в очереди, число параллельных обработчиков и лимит разрешений за время: это разные ограничения. Периодическое пополнение `Semaphore` моделирует фиксированные окна и допускает два burst рядом с границей. Если ограничивается фактическое добавление в очередь, разрешение должно относиться к этому моменту: разрешение, полученное до долгого `queue.put`, не сохраняет строгий лимит отправки. Для скользящего интервала нужен другой алгоритм, например журнал времён отправки.

**Готово, если:** очередь не превышает 50; одновременно работают не более четырёх обработчиков; выбранный контракт лимита проверяется управляемыми часами; остановка на пустой и полной очереди завершается; interruption и отказ не теряют задачи без описанной политики. [Почему одного shutdown недостаточно](07-patterns.html#producer-consumer-pattern).

### 2. Общий deadline и частичный успех

`orTimeout()` меняет результат future; underlying HTTP-операцию останавливает её собственный cancel/timeout. Сохраните исходные handles, используйте один бюджет на весь fan-out и не создавайте новый пятисекундный бюджет для каждого `get`. У каждого запроса настройте I/O timeout. При общем deadline: под общей координацией зафиксируйте успешные ответы, завершите итоговый future этим снимком и запросите отмену остальных handles. Поздние ответы не должны менять уже возвращённый список.

`allOf()` можно использовать, если ошибки отдельных запросов заранее преобразованы в явный результат «успех/ошибка», иначе одна ошибка нарушит простой happy path. `anyOf()` подходит для первого завершения, но не реализует требование «собрать все успешные до deadline». Отмена клиента не гарантирует откат действий удалённого сервера.

**Готово, если:** сценарий «быстрый успех + ошибка + блокирующая операция» возвращает только успех; на общем deadline запрашивается отмена незавершённых операций; проверено освобождение занятых клиентских ресурсов с учётом контракта клиента; пустой список возвращает пустой результат; scheduler и executor закрываются владельцем. Отдельно проверяется гонка ответа с deadline. [Контракт таймаута и отмены](03-async-coordination.html#таймаут-результата-и-отмена-операции).

### 3. Порядок блокировок

Упорядочивайте два счёта по уникальному неизменяемому ID. При использовании `identityHashCode` обязателен общий tie-lock на случай коллизии; идентичный объект обрабатывается отдельно. `tryLock` уменьшает время ожидания одной попытки, но бесконечный retry сам по себе не задаёт общий deadline и не исключает livelock.

**Готово, если:** встречные переводы завершаются в ограниченное время; суммарный баланс сохраняется; есть случай одного и того же счёта, равных ключей порядка и interruption; пример обнаружения показывает цикл «поток → монитор → владелец». [Вариант с tie-lock](10-synchronized.html#5-deadlock-через-неупорядоченные-блокировки).

### 4. LRU: чтение меняет порядок

У `LinkedHashMap` с `accessOrder=true` операция `get` обновляет структуру порядка. Поэтому её нельзя защищать read-lock, разрешающим параллельные вызовы `get`; используйте общий монитор либо write-lock для всех таких операций. Возвращаемое изменяемое значение не становится потокобезопасным вместе с кэшем.

**Готово, если:** `capacity>0`; после `put(A), put(B), get(A), put(C)` при capacity=2 удаляется B; размер не превышает capacity; конкурентные get/put не нарушают структуру; политика `null` и обновления существующего ключа описана.

### 5. Границы файловых фрагментов

Для начала разделяйте работу по файлам, а не произвольным байтовым диапазонам: иначе можно разрезать UTF-8 символ и посчитать одно слово дважды. Если нужны фрагменты большого файла, задайте кодировку и правило владельца слова на границе. Блокирующее чтение не стоит бездумно переносить в общий `ForkJoinPool`; обсудите выделенный I/O executor и последующую CPU-обработку либо `ManagedBlocker`.

**Готово, если:** результат совпадает с последовательным подсчётом для пустых файлов, UTF-8, слова на границе фрагментов; описаны симлинки и ошибки доступа; число одновременно открытых файлов ограничено; streams закрываются.

### 6. Метрики и исключения

Измеряйте интервалы через `System.nanoTime()`. `afterExecute(r, t)` может получить `t=null`, даже если задача, отправленная через `submit`, завершилась с ошибкой: исключение хранится в `Future`. Если `r` — завершённый `Future<?>`, проверьте `get()` и отдельно обработайте отмену, `ExecutionException` и interruption. Время старта удаляйте в `finally`; rejection считайте в `RejectedExecutionHandler` с явно выбранной политикой.

**Готово, если:** отдельно учитываются успех, ошибка, отмена и rejection; задача с исключением через `execute` и через `submit` видна в метриках; нет деления на ноль до первого завершения; карта времён не растёт после завершения задач; при shutdown не остаются служебные потоки. Проверки опираются на latch/barrier и факты завершения, а не на предположение о порядке после `sleep`.
