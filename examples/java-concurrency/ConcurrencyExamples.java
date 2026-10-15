import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Законченные учебные примеры для JDK 17, без внешних зависимостей. */
public final class ConcurrencyExamples {
    static final class AtomicStack<T> {
        private record Node<T>(T value, Node<T> next) {}
        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        void push(T value) {
            Node<T> oldHead;
            Node<T> newHead;
            do {
                oldHead = head.get();
                newHead = new Node<>(value, oldHead);
            } while (!head.compareAndSet(oldHead, newHead));
        }

        T pop() {
            Node<T> oldHead;
            do {
                oldHead = head.get();
                if (oldHead == null) return null;
            } while (!head.compareAndSet(oldHead, oldHead.next()));
            return oldHead.value();
        }
    }

    static final class LockedCache<K, V> {
        private final Object lock = new Object();
        private final Map<K, V> values = new HashMap<>();

        V get(K key) {
            synchronized (lock) { return values.get(key); }
        }

        void put(K key, V value) {
            synchronized (lock) { values.put(key, value); }
        }

        int size() {
            synchronized (lock) { return values.size(); }
        }
    }

    /** Graceful shutdown: принятые задачи исполняются, новые отклоняются. */
    static final class WorkerPool {
        private final Object lifecycle = new Object();
        private final BlockingQueue<Runnable> queue;
        private final List<Thread> workers = new ArrayList<>();
        private volatile boolean shutdown;

        WorkerPool(int threads, int capacity) {
            if (threads <= 0) throw new IllegalArgumentException("threads must be positive");
            queue = new ArrayBlockingQueue<>(capacity);
            for (int i = 0; i < threads; i++) {
                workers.add(new Thread(this::work, "example-worker-" + i));
            }
            workers.forEach(Thread::start);
        }

        void submit(Runnable task) {
            synchronized (lifecycle) {
                if (shutdown || !queue.offer(task)) {
                    throw new RejectedExecutionException("Pool stopped or queue full");
                }
            }
        }

        private void work() {
            while (!shutdown || !queue.isEmpty()) {
                try {
                    Runnable task = queue.poll(50, TimeUnit.MILLISECONDS);
                    if (task != null) {
                        try { task.run(); }
                        catch (RuntimeException ex) { System.err.println(ex); }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return; // Прерывание означает немедленную отмену.
                }
            }
        }

        void shutdown() {
            synchronized (lifecycle) { shutdown = true; }
        }

        List<Runnable> shutdownNow() {
            synchronized (lifecycle) {
                shutdown = true;
                List<Runnable> pending = new ArrayList<>();
                queue.drainTo(pending);
                workers.forEach(Thread::interrupt);
                return pending;
            }
        }

        boolean awaitTermination(Duration timeout) throws InterruptedException {
            long budget = timeout.toNanos();
            long started = System.nanoTime();
            for (Thread worker : workers) {
                long remaining = budget - (System.nanoTime() - started);
                if (remaining <= 0) return workers.stream().noneMatch(Thread::isAlive);
                TimeUnit.NANOSECONDS.timedJoin(worker, remaining);
            }
            return workers.stream().noneMatch(Thread::isAlive);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void stackAndCache() throws Exception {
        AtomicStack<Integer> stack = new AtomicStack<>();
        stack.push(1);
        stack.push(2);
        check(stack.pop() == 2 && stack.pop() == 1 && stack.pop() == null, "LIFO");
        LockedCache<Integer, Integer> cache = new LockedCache<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<Future<?>> writes = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                int base = t * 1_000;
                writes.add(executor.submit(() -> {
                    for (int i = base; i < base + 1_000; i++) {
                        stack.push(i);
                        cache.put(i, i);
                        check(cache.get(i) == i, "Cache read after write");
                    }
                }));
            }
            for (Future<?> write : writes) write.get(5, TimeUnit.SECONDS);
            List<Future<Set<Integer>>> reads = new ArrayList<>();
            for (int t = 0; t < 4; t++) {
                reads.add(executor.submit(() -> {
                    Set<Integer> local = new HashSet<>();
                    Integer value;
                    while ((value = stack.pop()) != null) {
                        check(local.add(value), "Duplicate pop within reader");
                    }
                    return local;
                }));
            }
            Set<Integer> all = new HashSet<>();
            for (Future<Set<Integer>> read : reads) {
                for (Integer value : read.get(5, TimeUnit.SECONDS)) {
                    check(all.add(value), "Duplicate pop across readers");
                }
            }
            check(all.size() == 4_000 && cache.size() == 4_000, "No lost elements");
        } finally {
            executor.shutdownNow();
            check(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor stopped");
        }
    }

    private static void shutdownChecks() throws Exception {
        WorkerPool empty = new WorkerPool(2, 3);
        empty.shutdown();
        check(empty.awaitTermination(Duration.ofSeconds(5)), "Empty pool stopped");
        WorkerPool full = new WorkerPool(1, 3);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger completed = new AtomicInteger();
        try {
            full.submit(() -> {
                entered.countDown();
                try {
                    release.await();
                    completed.incrementAndGet();
                } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            });
            check(entered.await(5, TimeUnit.SECONDS), "Worker started");
            for (int i = 0; i < 3; i++) full.submit(completed::incrementAndGet);
            try {
                full.submit(() -> {});
                throw new AssertionError("Full queue accepted task");
            } catch (RejectedExecutionException expected) { /* Ожидаемый backpressure. */ }
            full.shutdown();
            try {
                full.submit(() -> {});
                throw new AssertionError("Stopped pool accepted task");
            } catch (RejectedExecutionException expected) { /* Новые задачи запрещены. */ }
            release.countDown();
            check(full.awaitTermination(Duration.ofSeconds(5)), "Full pool drained");
            check(completed.get() == 4, "Every accepted task completed");
        } finally {
            release.countDown();
            full.shutdownNow();
            check(full.awaitTermination(Duration.ofSeconds(5)), "Pool cleaned up");
        }
    }

    private static void timeoutIsNotCancellation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        CompletableFuture<String> result = new CompletableFuture<>();
        Future<?> operation = executor.submit(() -> {
            entered.countDown();
            try {
                release.await();
                result.complete("done");
            } catch (InterruptedException ex) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        });
        try {
            check(entered.await(5, TimeUnit.SECONDS), "Operation started");
            try {
                result.orTimeout(30, TimeUnit.MILLISECONDS).join();
                throw new AssertionError("Expected timeout");
            } catch (CompletionException ex) {
                check(ex.getCause() instanceof TimeoutException, "Timeout cause");
            }
            check(!operation.isDone(), "Timeout left underlying operation running");
            operation.cancel(true);
            check(interrupted.await(5, TimeUnit.SECONDS), "Underlying Future interrupted");
        } finally {
            release.countDown();
            executor.shutdownNow();
            check(executor.awaitTermination(5, TimeUnit.SECONDS), "Timeout executor stopped");
        }
    }

    public static void main(String[] args) throws Exception {
        stackAndCache();
        shutdownChecks();
        timeoutIsNotCancellation();
        System.out.println("OK: CAS, shared lock, shutdown, timeout/cancellation");
    }
}
