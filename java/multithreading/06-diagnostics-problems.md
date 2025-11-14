# Диагностика и устранение проблем

## Содержание

1. [Основные проблемы многопоточности](#основные-проблемы-многопоточности)
   - [1. Deadlock (Взаимная блокировка)](#1-deadlock-взаимная-блокировка)
   - [2. Livelock](#2-livelock)
   - [3. Starvation (Голодание)](#3-starvation-голодание)
   - [4. Race Condition (Состояние гонки)](#4-race-condition-состояние-гонки)
2. [Инструменты диагностики](#инструменты-диагностики)
3. [Тестирование многопоточного кода](#тестирование-многопоточного-кода)

## Основные проблемы многопоточности

### 1. Deadlock (Взаимная блокировка)

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

### 2. Livelock

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

### 3. Starvation (Голодание)

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

### 4. Race Condition (Состояние гонки)

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

## Инструменты диагностики

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

## Тестирование многопоточного кода

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

