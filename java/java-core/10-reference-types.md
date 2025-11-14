# Типы ссылок в Java


## Содержание

1. [Введение в типы ссылок](#введение-в-типы-ссылок)
2. [Strong Reference (Сильные ссылки)](#strong-reference-сильные-ссылки)
3. [Weak Reference (Слабые ссылки)](#weak-reference-слабые-ссылки)
4. [Soft Reference (Мягкие ссылки)](#soft-reference-мягкие-ссылки)
5. [Phantom Reference (Фантомные ссылки)](#phantom-reference-фантомные-ссылки)
6. [ReferenceQueue](#referencequeue)
7. [Сравнение типов ссылок](#сравнение-типов-ссылок)
8. [Практические примеры](#практические-примеры)
9. [Best Practices](#best-practices)
10. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Введение в типы ссылок

Java предоставляет четыре типа ссылок на объекты с различной силой связи между ссылкой и объектом. Это позволяет более 
тонко управлять поведением сборщика мусора и реализовывать различные паттерны кэширования и управления ресурсами.

**Иерархия ссылок:**
```
Object
   └── java.lang.ref.Reference<T> (абстрактный)
       ├── SoftReference<T>
       ├── WeakReference<T>
       └── PhantomReference<T>
```

**Основные концепции:**
- **Reachability (Достижимость)**: Объект достижим, если к нему существует цепочка ссылок от GC roots
- **GC roots**: Корни сборки мусора (локальные переменные, статические поля, активные потоки, JNI ссылки)
- **Eligibility for GC**: Право на сборку мусора зависит от типа самой сильной ссылки на объект

**Типы достижимости:**
1. **Strongly reachable**: Достижим через strong references
2. **Softly reachable**: Достижим только через soft references (но не через strong)
3. **Weakly reachable**: Достижим только через weak references
4. **Phantom reachable**: Объект finalized, достижим только через phantom references
5. **Unreachable**: Недостижим вообще

## Strong Reference (Сильные ссылки)

**Strong Reference** — обычная ссылка на объект в Java, используемая по умолчанию.

**Характеристики:**
- Объект не будет собран GC пока существует хотя бы одна strong reference
- Используется по умолчанию при любом присваивании
- Может привести к утечкам памяти при неправильном использовании

**Примеры:**
```java
// Все это strong references
Object obj = new Object();
String str = "Hello";
List<Integer> list = new ArrayList<>();
MyClass instance = new MyClass();
```

**Жизненный цикл:**
```java
public class StrongReferenceExample {
    public static void main(String[] args) {
        // Создание strong reference
        Object strongRef = new Object();  // Объект создан
        
        // Объект НЕ будет собран GC пока существует strongRef
        System.gc();  // Явный вызов GC
        
        // Объект всё ещё жив
        System.out.println("Object is alive: " + strongRef);
        
        // Удаление strong reference
        strongRef = null;  // Теперь объект eligible for GC
        
        System.gc();  // Теперь объект может быть собран
    }
}
```

**Проблема утечки памяти:**
```java
public class CacheLeakExample {
    // Плохая практика: неограниченный кэш с strong references
    private static Map<String, byte[]> cache = new HashMap<>();
    
    public void cacheData(String key, byte[] data) {
        cache.put(key, data);  // Strong reference!
        // Объект никогда не будет собран, даже если больше не используется
    }
}

// Решение: использовать WeakHashMap или Soft/Weak references
```

## Weak Reference (Слабые ссылки)

**Weak Reference** — ссылка, которая не препятствует сборке мусора объекта.

**Характеристики:**
- Объект с только weak references будет собран при следующем GC
- Используется через `java.lang.ref.WeakReference<T>`
- Полезен для ассоциативных структур данных (canonicalizing mappings)

**Синтаксис:**
```java
import java.lang.ref.WeakReference;

Object referent = new Object();
WeakReference<Object> weakRef = new WeakReference<>(referent);

// Получение объекта
Object obj = weakRef.get();  // Возвращает объект или null если собран
```

**Пример жизненного цикла:**
```java
public class WeakReferenceExample {
    public static void main(String[] args) {
        Object referent = new Object();
        WeakReference<Object> weakRef = new WeakReference<>(referent);
        
        System.out.println("Before GC: " + weakRef.get());  // Объект доступен
        
        // Удаляем strong reference
        referent = null;
        
        // Вызываем GC
        System.gc();
        
        // Ждём завершения GC
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        System.out.println("After GC: " + weakRef.get());  // null - объект собран
    }
}
```

**WeakHashMap:**
```java
// WeakHashMap использует weak references для ключей
import java.util.WeakHashMap;

public class WeakHashMapExample {
    public static void main(String[] args) {
        Map<Key, String> map = new WeakHashMap<>();
        
        Key key1 = new Key("key1");
        Key key2 = new Key("key2");
        
        map.put(key1, "value1");
        map.put(key2, "value2");
        
        System.out.println("Size before GC: " + map.size());  // 2
        
        // Удаляем strong reference на key1
        key1 = null;
        
        System.gc();
        try { Thread.sleep(100); } catch (InterruptedException e) {}
        
        System.out.println("Size after GC: " + map.size());  // 1
        // key1 был собран, entry удалена из map
    }
    
    static class Key {
        String name;
        Key(String name) { this.name = name; }
    }
}
```

**Канонизирующая Map (Canonicalizing Map):**
```java
// Паттерн для sharing объектов с одинаковыми значениями
public class StringPool {
    private final Map<String, WeakReference<String>> pool = new HashMap<>();
    
    public String intern(String str) {
        // Проверяем, есть ли уже такая строка в пуле
        WeakReference<String> ref = pool.get(str);
        String pooled = (ref != null) ? ref.get() : null;
        
        if (pooled != null) {
            return pooled;  // Возвращаем существующую строку
        }
        
        // Добавляем новую строку в пул
        pool.put(str, new WeakReference<>(str));
        return str;
    }
}
```

**Когда использовать WeakReference:**
- Canonicalizing mappings (пулы объектов)
- Observers/listeners, которые не должны препятствовать GC
- Metadata хранилища (ассоциативные данные с объектами)
- Кэши, где важно освобождение памяти

## Soft Reference (Мягкие ссылки)

**Soft Reference** — ссылка, которая собирается GC только при необходимости освободить память перед `OutOfMemoryError`.

**Характеристики:**
- Объект с только soft references будет собран только когда JVM нужна память
- Используется через `java.lang.ref.SoftReference<T>`
- Идеален для memory-sensitive кэшей
- GC собирает soft references по алгоритму LRU (least-recently-used)

**Синтаксис:**
```java
import java.lang.ref.SoftReference;

Object referent = new Object();
SoftReference<Object> softRef = new SoftReference<>(referent);

// Получение объекта
Object obj = softRef.get();  // Возвращает объект или null если собран
```

**Пример жизненного цикла:**
```java
public class SoftReferenceExample {
    public static void main(String[] args) {
        // Создаём большой объект
        byte[] bigObject = new byte[10 * 1024 * 1024];  // 10 MB
        SoftReference<byte[]> softRef = new SoftReference<>(bigObject);
        
        System.out.println("Before GC: " + (softRef.get() != null));  // true
        
        // Удаляем strong reference
        bigObject = null;
        
        // Вызываем GC - объект НЕ будет собран (достаточно памяти)
        System.gc();
        System.out.println("After GC (enough memory): " + (softRef.get() != null));  // true
        
        // Создаём memory pressure
        try {
            List<byte[]> memoryFiller = new ArrayList<>();
            while (true) {
                memoryFiller.add(new byte[10 * 1024 * 1024]);  // Выделяем память
            }
        } catch (OutOfMemoryError e) {
            // При нехватке памяти soft references будут собраны
            System.out.println("After memory pressure: " + (softRef.get() != null));  // false
        }
    }
}
```

**Memory-sensitive кэш:**
```java
public class ImageCache {
    private final Map<String, SoftReference<Image>> cache = new HashMap<>();
    
    public Image getImage(String path) {
        // Проверяем кэш
        SoftReference<Image> ref = cache.get(path);
        Image image = (ref != null) ? ref.get() : null;
        
        if (image != null) {
            System.out.println("Cache hit: " + path);
            return image;
        }
        
        // Загружаем из диска
        System.out.println("Cache miss: " + path);
        image = loadImageFromDisk(path);
        
        // Сохраняем в кэш через SoftReference
        cache.put(path, new SoftReference<>(image));
        return image;
    }
    
    private Image loadImageFromDisk(String path) {
        // Имитация загрузки
        return new Image(path);
    }
    
    static class Image {
        String path;
        byte[] data = new byte[1024 * 1024];  // 1 MB image
        
        Image(String path) { this.path = path; }
    }
}
```

**Настройка поведения SoftReference:**
```bash
# Управление временем жизни soft references
# Значение в миллисекундах на MB свободного heap
-XX:SoftRefLRUPolicyMSPerMB=1000

# Default: 1000ms per MB
# Пример: при 10 MB свободной памяти, soft reference живёт max 10 секунд
# При меньшем значении - более агрессивная очистка
# При большем значении - soft references живут дольше
```

**Когда использовать SoftReference:**
- Image кэши
- Parsed data кэши (XML, JSON)
- Computed results кэши
- Любой memory-sensitive кэш

## Phantom Reference (Фантомные ссылки)

**Phantom Reference** — самая слабая ссылка, используется для post-mortem cleanup.

**Характеристики:**
- Объект с phantom reference уже недоступен (метод `get()` всегда возвращает `null`)
- Используется только с `ReferenceQueue`
- Позволяет выполнить cleanup ПОСЛЕ finalization но ДО освобождения памяти
- Более предсказуем и безопасен чем `finalize()`

**Синтаксис:**
```java
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object referent = new Object();
PhantomReference<Object> phantomRef = new PhantomReference<>(referent, queue);

// get() всегда возвращает null
Object obj = phantomRef.get();  // ВСЕГДА null!
```

**Жизненный цикл объекта с PhantomReference:**
```
1. Объект создан, есть strong reference
2. Strong reference удалена -> объект finalized
3. После finalization -> PhantomReference помещается в ReferenceQueue
4. Приложение читает из ReferenceQueue и выполняет cleanup
5. Приложение вызывает phantomRef.clear()
6. Память объекта освобождается
```

**Пример использования:**
```java
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class PhantomReferenceExample {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<Object> queue = new ReferenceQueue<>();
        
        Object referent = new Object() {
            @Override
            public String toString() {
                return "Resource object";
            }
        };
        
        PhantomReference<Object> phantomRef = new PhantomReference<>(referent, queue);
        
        System.out.println("Referent: " + referent);
        System.out.println("Phantom get(): " + phantomRef.get());  // null
        
        // Удаляем strong reference
        referent = null;
        
        // Вызываем GC и finalization
        System.gc();
        System.runFinalization();
        Thread.sleep(100);
        
        // Проверяем ReferenceQueue
        if (queue.poll() != null) {
            System.out.println("Phantom reference enqueued - object is phantom reachable");
            
            // Выполняем cleanup
            performCleanup();
            
            // Очищаем phantom reference
            phantomRef.clear();
            
            // Теперь память может быть освобождена
        }
    }
    
    private static void performCleanup() {
        System.out.println("Performing cleanup...");
        // Закрытие ресурсов, удаление файлов, и т.д.
    }
}
```

**Практический пример: Управление off-heap памятью**
```java
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class DirectBufferCleaner {
    // Отслеживание всех PhantomReferences
    private static final Map<PhantomReference<ByteBuffer>, Long> references = 
        new ConcurrentHashMap<>();
    
    private static final ReferenceQueue<ByteBuffer> queue = new ReferenceQueue<>();
    
    static {
        // Поток для обработки phantom references
        Thread cleanerThread = new Thread(() -> {
            while (true) {
                try {
                    PhantomReference<?> ref = (PhantomReference<?>) queue.remove();
                    Long address = references.remove(ref);
                    
                    if (address != null) {
                        // Освобождаем native память
                        freeNativeMemory(address);
                        System.out.println("Freed native memory at: " + address);
                    }
                    
                    ref.clear();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }
    
    public static ByteBuffer allocateDirectBuffer(int capacity) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(capacity);
        long address = getBufferAddress(buffer);
        
        // Регистрируем PhantomReference
        PhantomReference<ByteBuffer> ref = new PhantomReference<>(buffer, queue);
        references.put(ref, address);
        
        return buffer;
    }
    
    private static long getBufferAddress(ByteBuffer buffer) {
        // Упрощённо: получение native адреса через reflection
        return 0xDEADBEEF;  // Placeholder
    }
    
    private static void freeNativeMemory(long address) {
        // Упрощённо: вызов native метода для освобождения памяти
        // Unsafe.freeMemory(address);
    }
}
```

**Когда использовать PhantomReference:**
- Управление off-heap памятью (DirectByteBuffer cleanup)
- Cleanup native ресурсов (JNI, файловые дескрипторы)
- Post-mortem logging/monitoring
- Альтернатива `finalize()` (более предсказуемая)

**PhantomReference vs finalize():**

| Характеристика | PhantomReference | finalize() |
|----------------|------------------|------------|
| Предсказуемость | Высокая | Низкая |
| Производительность | Хорошая | Плохая |
| Доступ к объекту | Нет (get() = null) | Да (можно воскресить) |
| Очерёдность | Контролируемая | Непредсказуемая |
| Рекомендация | ✅ Используйте | ❌ Избегайте |

## ReferenceQueue

**ReferenceQueue** — очередь для отслеживания объектов, собранных GC.

**Назначение:**
- Уведомление о сборке мусора referenced объектов
- Возможность выполнить post-GC cleanup
- Работает с WeakReference, SoftReference, PhantomReference

**Процесс работы:**
```
1. Создаётся Reference с ReferenceQueue
2. Объект становится eligible for GC
3. GC собирает объект
4. Reference помещается в ReferenceQueue
5. Приложение читает из queue и выполняет cleanup
```

**Синтаксис:**
```java
ReferenceQueue<MyClass> queue = new ReferenceQueue<>();

MyClass obj = new MyClass();
WeakReference<MyClass> ref = new WeakReference<>(obj, queue);  // С очередью

obj = null;  // Убираем strong reference
System.gc();

// Проверка очереди
Reference<? extends MyClass> polledRef = queue.poll();
if (polledRef != null) {
    System.out.println("Object was garbage collected");
}
```

**Методы ReferenceQueue:**
```java
// Неблокирующая проверка
Reference<? extends T> poll()  // Возвращает reference или null

// Блокирующее ожидание
Reference<? extends T> remove()  // Ждёт пока reference не появится
Reference<? extends T> remove(long timeout)  // Ждёт с таймаутом
```

**Пример с cleanup:**
```java
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class ResourceTracker {
    private static class Resource {
        String name;
        Resource(String name) { this.name = name; }
        
        void cleanup() {
            System.out.println("Cleaning up resource: " + name);
        }
    }
    
    // Расширяем WeakReference чтобы хранить дополнительную информацию
    private static class ResourceReference extends WeakReference<Resource> {
        final String resourceName;
        
        ResourceReference(Resource referent, ReferenceQueue<Resource> queue) {
            super(referent, queue);
            this.resourceName = referent.name;
        }
    }
    
    private final Map<String, ResourceReference> resources = new HashMap<>();
    private final ReferenceQueue<Resource> queue = new ReferenceQueue<>();
    
    public ResourceTracker() {
        // Поток для обработки собранных ресурсов
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    ResourceReference ref = (ResourceReference) queue.remove();
                    System.out.println("Resource collected: " + ref.resourceName);
                    resources.remove(ref.resourceName);
                    // Здесь можно выполнить cleanup
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    public void track(Resource resource) {
        ResourceReference ref = new ResourceReference(resource, queue);
        resources.put(resource.name, ref);
    }
}
```

**Практический пример: Кэш с автоочисткой**
```java
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class SelfCleaningCache<K, V> {
    private static class ValueReference<K, V> extends SoftReference<V> {
        final K key;
        
        ValueReference(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }
    }
    
    private final Map<K, ValueReference<K, V>> cache = new HashMap<>();
    private final ReferenceQueue<V> queue = new ReferenceQueue<>();
    
    public V get(K key) {
        // Очистка собранных entries
        cleanUp();
        
        ValueReference<K, V> ref = cache.get(key);
        return (ref != null) ? ref.get() : null;
    }
    
    public void put(K key, V value) {
        // Очистка собранных entries
        cleanUp();
        
        ValueReference<K, V> ref = new ValueReference<>(key, value, queue);
        cache.put(key, ref);
    }
    
    private void cleanUp() {
        ValueReference<K, V> ref;
        while ((ref = (ValueReference<K, V>) queue.poll()) != null) {
            cache.remove(ref.key);
            System.out.println("Removed from cache: " + ref.key);
        }
    }
    
    public int size() {
        cleanUp();
        return cache.size();
    }
}
```

## Сравнение типов ссылок

### Таблица сравнения

| Характеристика | Strong | Soft | Weak | Phantom |
|----------------|--------|------|------|---------|
| **get() возвращает объект** | Да | Да (до GC) | Да (до GC) | Нет (всегда null) |
| **Когда собирается** | Никогда | При нехватке памяти | При следующем GC | После finalization |
| **ReferenceQueue** | Нет | Опционально | Опционально | Обязательно |
| **Основное применение** | Обычные ссылки | Memory-sensitive кэши | Canonicalizing maps | Post-mortem cleanup |
| **Производительность** | Лучшая | Хорошая | Хорошая | Средняя |
| **GC overhead** | Нет | Небольшой | Небольшой | Средний |

### Иерархия силы ссылок

```
Strong Reference (самая сильная)
        ↓
Soft Reference
        ↓
Weak Reference
        ↓
Phantom Reference (самая слабая)
```

### Жизненный цикл с разными типами ссылок

```java
public class ReferencesLifecycle {
    public static void main(String[] args) throws InterruptedException {
        ReferenceQueue<byte[]> queue = new ReferenceQueue<>();
        
        byte[] data = new byte[1024];
        
        SoftReference<byte[]> softRef = new SoftReference<>(data, queue);
        WeakReference<byte[]> weakRef = new WeakReference<>(data, queue);
        PhantomReference<byte[]> phantomRef = new PhantomReference<>(data, queue);
        
        System.out.println("Strong ref exists:");
        System.out.println("  Soft.get(): " + (softRef.get() != null));    // true
        System.out.println("  Weak.get(): " + (weakRef.get() != null));    // true
        System.out.println("  Phantom.get(): " + (phantomRef.get() != null)); // false
        
        data = null;  // Удаляем strong reference
        System.gc();
        Thread.sleep(100);
        
        System.out.println("\nAfter GC (no memory pressure):");
        System.out.println("  Soft.get(): " + (softRef.get() != null));    // true (не собран)
        System.out.println("  Weak.get(): " + (weakRef.get() != null));    // false (собран)
        System.out.println("  Weak in queue: " + (queue.poll() != null));  // true
        
        // Создание memory pressure для soft reference
        try {
            List<byte[]> list = new ArrayList<>();
            for (int i = 0; i < 100000; i++) {
                list.add(new byte[10000]);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("\nAfter memory pressure:");
            System.out.println("  Soft.get(): " + (softRef.get() != null));  // false (собран)
        }
    }
}
```

## Практические примеры

### 1. Canonicalizing String Pool

```java
public class StringInterner {
    private final Map<String, WeakReference<String>> pool = new ConcurrentHashMap<>();
    
    public String intern(String str) {
        if (str == null) return null;
        
        // Проверяем существующую entry
        WeakReference<String> ref = pool.get(str);
        String interned = (ref != null) ? ref.get() : null;
        
        if (interned != null) {
            return interned;
        }
        
        // Добавляем новую entry
        pool.put(str, new WeakReference<>(str));
        
        // Периодическая очистка мёртвых entries
        if (pool.size() > 10000) {
            cleanUp();
        }
        
        return str;
    }
    
    private void cleanUp() {
        pool.entrySet().removeIf(entry -> entry.getValue().get() == null);
    }
}
```

### 2. Image Cache с SoftReference

```java
public class ImageCache {
    private static class ImageReference extends SoftReference<BufferedImage> {
        final String path;
        final long size;
        
        ImageReference(String path, BufferedImage image, ReferenceQueue<BufferedImage> queue) {
            super(image, queue);
            this.path = path;
            this.size = estimateSize(image);
        }
        
        private long estimateSize(BufferedImage image) {
            return (long) image.getWidth() * image.getHeight() * 4; // ARGB
        }
    }
    
    private final Map<String, ImageReference> cache = new ConcurrentHashMap<>();
    private final ReferenceQueue<BufferedImage> queue = new ReferenceQueue<>();
    private final AtomicLong cacheSize = new AtomicLong(0);
    
    public BufferedImage get(String path) {
        cleanUp();
        
        ImageReference ref = cache.get(path);
        BufferedImage image = (ref != null) ? ref.get() : null;
        
        if (image != null) {
            return image;
        }
        
        // Load from disk
        image = loadImage(path);
        if (image != null) {
            put(path, image);
        }
        
        return image;
    }
    
    private void put(String path, BufferedImage image) {
        ImageReference ref = new ImageReference(path, image, queue);
        ImageReference old = cache.put(path, ref);
        
        if (old != null) {
            cacheSize.addAndGet(-old.size);
        }
        cacheSize.addAndGet(ref.size);
    }
    
    private void cleanUp() {
        ImageReference ref;
        while ((ref = (ImageReference) queue.poll()) != null) {
            cache.remove(ref.path);
            cacheSize.addAndGet(-ref.size);
        }
    }
    
    private BufferedImage loadImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException e) {
            return null;
        }
    }
    
    public long getCacheSize() {
        cleanUp();
        return cacheSize.get();
    }
}
```

### 3. Listener Registry с WeakReference

```java
public class EventSource {
    private final List<WeakReference<EventListener>> listeners = new CopyOnWriteArrayList<>();
    
    public void addListener(EventListener listener) {
        listeners.add(new WeakReference<>(listener));
    }
    
    public void removeListener(EventListener listener) {
        listeners.removeIf(ref -> {
            EventListener l = ref.get();
            return l == null || l == listener;
        });
    }
    
    public void fireEvent(String event) {
        // Очистка мёртвых listeners и уведомление живых
        listeners.removeIf(ref -> {
            EventListener listener = ref.get();
            if (listener == null) {
                return true;  // Remove dead reference
            }
            listener.onEvent(event);
            return false;
        });
    }
    
    interface EventListener {
        void onEvent(String event);
    }
}
```

### 4. Resource Cleanup с PhantomReference

```java
public class ManagedResource implements AutoCloseable {
    private static final ReferenceQueue<ManagedResource> queue = new ReferenceQueue<>();
    private static final Set<ResourcePhantomReference> references = ConcurrentHashMap.newKeySet();
    
    static {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    ResourcePhantomReference ref = (ResourcePhantomReference) queue.remove();
                    ref.cleanup();
                    references.remove(ref);
                    ref.clear();
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.setName("Resource-Cleanup-Thread");
        cleanupThread.start();
    }
    
    private static class ResourcePhantomReference extends PhantomReference<ManagedResource> {
        private final Runnable cleanupAction;
        
        ResourcePhantomReference(ManagedResource referent, Runnable cleanupAction) {
            super(referent, queue);
            this.cleanupAction = cleanupAction;
        }
        
        void cleanup() {
            cleanupAction.run();
        }
    }
    
    private final String name;
    private boolean closed = false;
    
    public ManagedResource(String name) {
        this.name = name;
        
        // Регистрируем cleanup через PhantomReference
        Runnable cleanup = () -> {
            System.out.println("Phantom cleanup for: " + name);
            // Cleanup native resources, close connections, etc.
        };
        
        references.add(new ResourcePhantomReference(this, cleanup));
    }
    
    @Override
    public void close() {
        if (!closed) {
            System.out.println("Explicit close for: " + name);
            closed = true;
            // Cleanup resources
        }
    }
}
```

## Best Practices

### 1. Выбор правильного типа ссылки

**Strong Reference:**
```java
// Используйте для обычных объектов
String name = "John";
List<Integer> numbers = new ArrayList<>();
```

**Weak Reference:**
```java
// Используйте для:
// - Canonicalizing maps (пулы объектов)
// - Listeners/observers
// - Metadata associations

// Хорошо
Map<Object, Metadata> metadata = new WeakHashMap<>();

// Плохо (утечка памяти)
Map<Object, Metadata> metadata = new HashMap<>();
```

**Soft Reference:**
```java
// Используйте для:
// - Image caches
// - Parsed data caches
// - Computed results

// Хорошо
Map<String, SoftReference<Image>> imageCache = new HashMap<>();

// Плохо для кэша (может вызвать OOM)
Map<String, Image> imageCache = new HashMap<>();
```

**Phantom Reference:**
```java
// Используйте для:
// - Post-mortem cleanup
// - Native resource management
// - Замена finalize()

// Хорошо
PhantomReference<Resource> ref = new PhantomReference<>(resource, queue);

// Плохо (deprecated)
@Override protected void finalize() { cleanup(); }
```

### 2. Всегда очищайте ReferenceQueue

```java
// Плохо: накопление мёртвых references
Map<String, WeakReference<Object>> cache = new HashMap<>();

// Хорошо: периодическая очистка
public void cleanUp() {
    Reference<?> ref;
    while ((ref = queue.poll()) != null) {
        cache.remove(((MyReference) ref).key);
    }
}
```

### 3. Используйте try-with-resources вместо PhantomReference где возможно

```java
// Предпочтительно
try (FileInputStream fis = new FileInputStream("file.txt")) {
    // Use resource
}

// Только если try-with-resources невозможен
PhantomReference<FileInputStream> phantom = new PhantomReference<>(fis, queue);
```

### 4. Мониторинг reference types

```java
// Логирование для отладки
-XX:+PrintReferenceGC

// Пример вывода:
// [GC (Allocation Failure) [SoftReference, 0 refs, 0.0000123 secs]
// [WeakReference, 42 refs, 0.0000456 secs]
```

### 5. Избегайте воскрешения объектов

```java
// Плохо: воскрешение через finalize()
@Override
protected void finalize() {
    globalList.add(this);  // Воскрешение!
}

// Хорошо: используйте PhantomReference для cleanup
```

### 6. Thread-safety с WeakHashMap

```java
// Плохо: WeakHashMap не thread-safe
Map<Key, Value> map = new WeakHashMap<>();

// Хорошо: синхронизация
Map<Key, Value> map = Collections.synchronizedMap(new WeakHashMap<>());

// Или используйте ConcurrentHashMap с WeakReference values
Map<Key, WeakReference<Value>> map = new ConcurrentHashMap<>();
```

## Вопросы на собеседовании

### 1. В чём разница между Soft и Weak references?

**Ответ:** 
- **WeakReference**: Объект будет собран при следующем GC, независимо от количества доступной памяти
- **SoftReference**: Объект будет собран только когда JVM нужна память (перед OutOfMemoryError)
- **Использование**: WeakReference для metadata/listeners, SoftReference для кэшей

### 2. Почему PhantomReference.get() всегда возвращает null?

**Ответ:** 
PhantomReference предназначен для post-mortem cleanup, когда объект уже finalized и недоступен. Если бы `get()` 
возвращал объект, это позволило бы "воскресить" объект после finalization, что нарушает гарантии GC. PhantomReference 
используется только для уведомления о том, что объект был собран, чтобы можно было выполнить cleanup.

### 3. Как работает WeakHashMap?

**Ответ:**
WeakHashMap использует WeakReference для ключей. Когда ключ больше не имеет strong references, он становится eligible 
for GC. После сборки ключа, WeakHashMap автоматически удаляет соответствующую entry. Это реализовано через ReferenceQueue: 
при каждой операции (get/put) WeakHashMap проверяет queue и удаляет entries с собранными ключами.

### 4. Когда использовать каждый тип reference?

**Ответ:**
- **Strong**: Обычные объекты, которые должны жить пока используются
- **Weak**: Canonicalizing maps, listeners, metadata associations (не препятствуют GC)
- **Soft**: Memory-sensitive кэши (изображения, parsed data)
- **Phantom**: Post-mortem cleanup, native resource management, замена finalize()

### 5. Что такое ReferenceQueue и зачем она нужна?

**Ответ:**
ReferenceQueue — это очередь для уведомлений о сборке объектов. Когда объект с Weak/Soft/Phantom reference собирается GC, 
reference помещается в queue. Это позволяет приложению:
- Узнать о сборке объекта
- Выполнить cleanup действия
- Удалить мёртвые entries из кэшей/maps
- Освободить связанные ресурсы

### 6. Почему finalize() считается плохой практикой?

**Ответ:**
Проблемы с finalize():
- Непредсказуемое время вызова
- Может не вызваться вообще
- Замедляет GC (объекты с finalize() требуют двух GC циклов)
- Возможность воскрешения объектов
- Нет гарантий порядка выполнения

**Альтернативы:** PhantomReference, try-with-resources, Cleaner API (Java 9+)

### 7. Как настроить поведение SoftReference?

**Ответ:**
```bash
-XX:SoftRefLRUPolicyMSPerMB=1000  # Миллисекунды жизни на MB свободной памяти

# Пример расчёта:
# При 100 MB свободной памяти:
# Lifetime = 100 * 1000 = 100,000 ms = 100 секунд

# Более агрессивная очистка (меньше lifetime):
-XX:SoftRefLRUPolicyMSPerMB=100

# Менее агрессивная (больше lifetime):
-XX:SoftRefLRUPolicyMSPerMB=5000
```

### 8. Может ли WeakReference предотвратить OutOfMemoryError?

**Ответ:**
Нет напрямую. WeakReference собирается при каждом GC, но:
- Если объекты создаются быстрее, чем GC успевает работать
- Если есть strong references на объекты
- Если memory leak в другом месте

Тогда OOM всё равно возможен. WeakReference только помогает GC освободить память от неиспользуемых объектов.

### 9. В чём разница между WeakHashMap и ConcurrentHashMap с WeakReference values?

**Ответ:**
- **WeakHashMap**: 
  - Weak references для ключей
  - Не thread-safe
  - Автоматическое удаление entries при сборке ключей
  
- **ConcurrentHashMap с WeakReference values**:
  - Strong references для ключей, weak для values
  - Thread-safe
  - Требует ручную очистку мёртвых entries

Выбор зависит от того, что должно контролировать lifetime: ключи или значения.

### 10. Какой тип reference использует String.intern()?

**Ответ:**
До Java 7: Strong references в PermGen (могло привести к OutOfMemoryError: PermGen space)
С Java 7+: Strong references в Heap, но можно создать аналог с WeakReference:

```java
// Аналог String.intern() с WeakReference
Map<String, WeakReference<String>> pool = new WeakHashMap<>();
```

Стандартный String pool использует strong references, поэтому interned строки живут до конца приложения.
