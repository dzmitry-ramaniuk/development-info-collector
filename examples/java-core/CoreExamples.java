import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Самостоятельные сценарии Java 17 без внешних зависимостей и ожидания GC. */
public final class CoreExamples {
    record Playlist(List<String> tracks) {
        Playlist {
            tracks = List.copyOf(tracks);
        }
    }

    record Person(long id, String name) {}

    static long price(long subtotal, LongUnaryOperator discount) {
        if (subtotal < 0) throw new IllegalArgumentException("negative subtotal");
        long result = discount.applyAsLong(subtotal);
        if (result < 0 || result > subtotal) {
            throw new IllegalArgumentException("discount outside [0, subtotal]");
        }
        return result;
    }

    static void change(List<String> names, int count) {
        names.add("added");
        names = new ArrayList<>(); // Меняется только локальная копия ссылки.
        count++;
    }

    static Map<String, Long> countNames(Path file) throws IOException {
        try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return lines.map(String::strip)
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.groupingBy(
                            name -> name, TreeMap::new, Collectors.counting()));
        }
    }

    static List<Integer> decodeInts(InputStream input) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        List<Integer> result = new ArrayList<>();
        while (true) {
            // Намеренно не более 3 байт: int может прийти несколькими чтениями.
            int read = input.read(buffer.array(), buffer.position(),
                    Math.min(3, buffer.remaining()));
            if (read == -1) break;
            buffer.position(buffer.position() + read);
            buffer.flip();
            while (buffer.remaining() >= Integer.BYTES) result.add(buffer.getInt());
            buffer.compact(); // Неполная запись остаётся в начале буфера.
        }
        if (buffer.position() != 0) throw new EOFException("incomplete int");
        return result;
    }

    static final class StringInterner {
        private final Map<String, WeakReference<String>> pool = new WeakHashMap<>();

        public synchronized String intern(String value) {
            Objects.requireNonNull(value);
            WeakReference<String> reference = pool.get(value);
            String existing = reference == null ? null : reference.get();
            if (existing != null) return existing;
            pool.put(value, new WeakReference<>(value));
            return value;
        }
    }

    static final class SoftValueCache<K, V> {
        private static final class Entry<K, V> extends SoftReference<V> {
            private final K key;

            Entry(K key, V value, ReferenceQueue<V> queue) {
                super(value, queue);
                this.key = key;
            }
        }

        private final Map<K, Entry<K, V>> entries = new ConcurrentHashMap<>();
        private final ReferenceQueue<V> queue = new ReferenceQueue<>();

        public V get(K key) {
            cleanUp();
            Entry<K, V> reference = entries.get(key);
            return reference == null ? null : reference.get();
        }

        public void put(K key, V value) {
            cleanUp();
            entries.put(key, new Entry<>(key, Objects.requireNonNull(value), queue));
        }

        public void cleanUp() {
            Reference<? extends V> reference;
            while ((reference = queue.poll()) != null) {
                // Все reference в этой приватной очереди созданы данным cache.
                @SuppressWarnings("unchecked")
                Entry<K, V> entry = (Entry<K, V>) reference;
                entries.remove(entry.key, entry); // Не удалить более новую запись.
            }
        }
    }

    static final class ManagedResource implements AutoCloseable {
        private static final Cleaner CLEANER = Cleaner.create();

        private static final class State implements Runnable {
            private final AtomicInteger cleanups;

            State(AtomicInteger cleanups) {
                this.cleanups = cleanups;
            }

            @Override
            public void run() {
                cleanups.incrementAndGet(); // Здесь освобождался бы отдельный native handle.
            }
        }

        private final Cleaner.Cleanable cleanable;

        ManagedResource(AtomicInteger cleanups) {
            cleanable = CLEANER.register(this, new State(cleanups));
        }

        @Override
        public void close() {
            cleanable.clean(); // Explicit close и fallback используют одну at-most-once action.
        }
    }

    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }

    private static void checkThrows(Class<? extends Throwable> type, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw failure;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    public static void main(String[] args) throws IOException {
        List<String> names = new ArrayList<>(List.of("initial"));
        int count = 1;
        change(names, count);
        check(names.equals(List.of("initial", "added")) && count == 1, "pass by value");

        List<String> source = new ArrayList<>(List.of("A"));
        Playlist playlist = new Playlist(source);
        source.add("B");
        check(playlist.tracks().equals(List.of("A")), "defensive copy of record component");
        checkThrows(UnsupportedOperationException.class, () -> playlist.tracks().add("C"));
        check(price(200, value -> value) == 200, "regular price");
        check(price(200, value -> value - value / 10) == 180, "discount strategy");
        checkThrows(IllegalArgumentException.class, () -> price(200, value -> -1));

        Path input = Files.createTempFile("core-examples-", ".txt");
        try {
            Files.writeString(input, " apple \n\npear\napple\n", StandardCharsets.UTF_8);
            check(countNames(input).equals(Map.of("apple", 2L, "pear", 1L)), "UTF-8 counts");
        } finally {
            Files.deleteIfExists(input);
        }

        byte[] payload = "bulk I/O".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        try (InputStream inputStream = new ByteArrayInputStream(payload)) {
            byte[] block = new byte[3];
            int read;
            while ((read = inputStream.read(block)) != -1) copy.write(block, 0, read);
        }
        check(java.util.Arrays.equals(payload, copy.toByteArray()), "bulk read/write");
        byte[] ints = ByteBuffer.allocate(8).putInt(42).putInt(99).array();
        check(decodeInts(new ByteArrayInputStream(ints)).equals(List.of(42, 99)), "partial records");
        check(decodeInts(new ByteArrayInputStream(new byte[0])).isEmpty(), "empty input");
        try {
            decodeInts(new ByteArrayInputStream(new byte[3]));
            throw new AssertionError("Expected truncated-record error");
        } catch (EOFException expected) {
            // Неполная запись не должна молча исчезнуть.
        }

        List<Person> people = List.of(new Person(1, "Alice"));
        String name = people.stream().filter(person -> person.id() == 1)
                .map(Person::name).findFirst().orElse("Unknown");
        check(name.equals("Alice"), "Optional<String>");
        check(people.stream().filter(person -> person.id() == 2)
                .map(Person::name).findFirst().orElse("Unknown").equals("Unknown"), "absent person");
        List<Integer> values = List.of(3, 1, 2);
        check(values.stream().sorted().limit(2).toList().equals(List.of(1, 2)), "global top N");
        check(values.stream().limit(2).sorted().toList().equals(List.of(1, 3)), "sorted prefix");

        List<Integer> typed = new ArrayList<>(List.of(2, 1));
        check(typed instanceof ArrayList<Integer>, "legal generic instanceof refinement");
        String[] generated = List.of("a", "b").toArray(String[]::new);
        check(generated.getClass() == String[].class, "typed array factory");
        List<Integer> mutable = Stream.of(1).collect(Collectors.toCollection(ArrayList::new));
        mutable.add(2);
        check(mutable.equals(List.of(1, 2)), "explicit mutable collector");
        List<Integer> unmodifiable = Stream.of(1).toList();
        checkThrows(UnsupportedOperationException.class, () -> unmodifiable.add(2));
        int[] array = {3, 1, 2};
        java.util.PriorityQueue<Integer> queue = new java.util.PriorityQueue<>(
                java.util.Arrays.stream(array).boxed().toList());
        check(queue.poll() == 1 && queue.poll() == 2 && queue.poll() == 3,
                "priority queue accepts collection, not int array");

        StringInterner interner = new StringInterner();
        String first = interner.intern(new String("same"));
        String second = interner.intern(new String("same"));
        check(first == second, "atomic canonicalization while reference is live");

        SoftValueCache<String, Object> cache = new SoftValueCache<>();
        Object oldValue = new Object();
        Object newValue = new Object();
        cache.put("image", oldValue);
        Reference<?> oldReference = cache.entries.get("image");
        cache.put("image", newValue);
        oldReference.enqueue(); // Управляемое уведомление вместо ожидания GC.
        cache.cleanUp();
        check(cache.get("image") == newValue, "stale queue entry preserves replacement");
        Reference<?> currentReference = cache.entries.get("image");
        currentReference.enqueue();
        cache.cleanUp();
        check(cache.get("image") == null && cache.entries.isEmpty(), "current queue entry removed");
        Reference.reachabilityFence(oldValue);
        Reference.reachabilityFence(newValue);

        AtomicInteger cleanups = new AtomicInteger();
        ManagedResource resource = new ManagedResource(cleanups);
        resource.close();
        resource.close();
        check(cleanups.get() == 1, "cleanup at most once on explicit close");
        System.out.println("Core examples: all checks passed");
    }
}
