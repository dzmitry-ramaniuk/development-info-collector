---
---

# Типы ссылок в Java

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Введение в типы ссылок](#введение-в-типы-ссылок)
3. [Strong Reference (Сильные ссылки)](#strong-reference-сильные-ссылки)
4. [Weak Reference (Слабые ссылки)](#weak-reference-слабые-ссылки)
5. [Soft Reference (Мягкие ссылки)](#soft-reference-мягкие-ссылки)
6. [Phantom Reference (Фантомные ссылки)](#phantom-reference-фантомные-ссылки)
7. [ReferenceQueue](#referencequeue)
8. [Сравнение типов ссылок](#сравнение-типов-ссылок)
9. [Практические примеры](#практические-примеры)
   - [1. Canonicalizing String Pool](#1-canonicalizing-string-pool)
   - [2. Image Cache с SoftReference](#2-image-cache-с-softreference)
   - [3. Listener Registry с WeakReference](#3-listener-registry-с-weakreference)
   - [4. Resource Cleanup с PhantomReference](#4-resource-cleanup-с-phantomreference)
10. [Best Practices](#best-practices)
11. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Актуальность материала

- **Дата проверки:** 2026-10-15; проверены контракты ссылок, очередей и очистки по адресным источникам ниже.
- **Целевая версия или диапазон:** API Java SE 17–21; детали StringTable относятся к OpenJDK HotSpot 21.
- **Статус примеров:** `current`; [CoreExamples.java](../../examples/java-core/CoreExamples.java) скомпилирован с `--release 17` и выполнен на OpenJDK 21.0.12.1. Проверки не требуют срабатывания GC; остальные фрагменты служат пояснениями.
- **Первичные источники:** [пакет java.lang.ref](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/package-summary.html), [WeakReference](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/WeakReference.html), [SoftReference](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/SoftReference.html), [PhantomReference](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/PhantomReference.html), [Cleaner](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/Cleaner.html), [WeakHashMap](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/WeakHashMap.html).

## Введение в типы ссылок

**Достижимость (reachability)** описывает пути от живых GC roots к объекту. Важна вся цепочка ссылок:
цикл из сильных ссылок, недостижимый от корней, сам по себе не удерживает объекты.

- **Strongly reachable:** доступен из выполняющегося кода без прохождения через специальные reference-объекты.
- **Softly reachable:** уже не strongly reachable, но доступен через soft reference.
- **Weakly reachable:** не strongly/softly reachable, но доступен через weak reference.
- **Phantom reachable:** достиг стадии, на которой phantom reference может уведомить об очистке; получить объект через неё нельзя.

`SoftReference`, `WeakReference` и `PhantomReference` наследуют `Reference<T>`. Обычная сильная ссылка не является отдельным
подклассом `Reference`. Изменение достижимости, очистка reference и помещение её в очередь — разные события.
Спецификация не задаёт фиксированное время их наступления.

## Strong Reference (Сильные ссылки)

Обычная переменная или поле может содержать сильную ссылку. Пока от живого корня есть сильный путь к объекту, GC его не собирает.

```java
List<byte[]> retained = new ArrayList<>();
retained.add(new byte[1024]); // Пока retained достижим, массив удерживается списком.
```

Безграничный кэш удерживает объекты вполне корректно с точки зрения GC, но может быть утечкой с точки зрения приложения.
Ищите удерживающий путь в heap dump, а не только место создания объекта.

## Weak Reference (Слабые ссылки)

`WeakReference` позволяет получить referent через `get()`, пока ссылка не очищена. Полученное ненулевое значение сохраните
в локальную переменную: два независимых `get()` могут дать разные результаты.

```java
WeakReference<Object> reference = new WeakReference<>(new Object());
Object value = reference.get();
if (value != null) {
    System.out.println(value); // Локальная сильная ссылка удерживает value во время использования.
}
```

Когда GC определит weak reachability, он очистит соответствующие weak references. Это не обещание очистки при «следующем GC»
или через 100 мс. `System.gc()` — запрос к JVM; `sleep` не подтверждает его выполнение.
См. [System.gc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#gc()).

**WeakHashMap** использует слабые ключи, но сильные значения. Если значение прямо или косвенно ссылается на ключ,
ключ остаётся достижимым. Например, `map.put(key, key)` не даёт ожидаемого weak-key поведения.
Map не потокобезопасна; составные операции требуют общей синхронизации.

## Soft Reference (Мягкие ссылки)

`SoftReference` разрешает JVM освобождать softly reachable объекты в ответ на потребность в памяти. Это не TTL, не строгий LRU
и не гарантия удержания объекта до почти полного исчерпания heap. JVM должна очистить soft references к softly reachable
объектам перед соответствующим `OutOfMemoryError`, однако порядок и момент очистки в остальное время не фиксированы.

```java
SoftReference<byte[]> reference = new SoftReference<>(new byte[1024]);
byte[] cached = reference.get();
if (cached == null) {
    cached = new byte[1024]; // Восстановление отсутствующего значения — обязанность приложения.
}
```

Для production-кэша обычно нужна явная граница размера, политика вытеснения и метрики hit/miss.
Soft references могут освободить значения, но не удаляют сильные ключи и оболочки из вашей map автоматически.

HotSpot-параметр `-XX:SoftRefLRUPolicyMSPerMB` влияет на эвристику удержания soft references. Он не задаёт верхнюю границу
времени жизни: вычисление вроде «100 MB × 1000 = ровно 100 секунд» нельзя использовать как TTL.
Параметр зависит от реализации и версии: см. [описание java для JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html#advanced-garbage-collection-options-for-java).

## Phantom Reference (Фантомные ссылки)

`PhantomReference.get()` всегда возвращает `null`, даже если referent ещё удерживается сильной ссылкой.
Reference нужна для уведомления, а не доступа к объекту. Для получения уведомления передайте `ReferenceQueue`.

```java
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object owner = new Object();
PhantomReference<Object> reference = new PhantomReference<>(owner, queue);
System.out.println(reference.get()); // Всегда null.
Reference.reachabilityFence(owner);
```

В Java 17–21 GC сам очищает phantom references при определении phantom reachability и затем может поставить их в очередь.
Нельзя обещать, что native cleanup произойдёт до освобождения heap-памяти referent или в установленный срок.
Состояние для освобождения внешнего ресурса хранится отдельно и не должно удерживать referent.

Предпочитайте `AutoCloseable` и `try-with-resources` для своевременного освобождения. `Cleaner`, основанный на phantom
references, может быть резервной мерой при пропущенном `close()`, но автоматический запуск не гарантирован к определённому
моменту или при завершении JVM. Не освобождайте вручную память `ByteBuffer.allocateDirect` по адресу, добытому reflection:
его жизненным циклом уже управляет реализация JDK.

## ReferenceQueue

Очередь содержит reference-объекты, а не сами referents. Регистрации в очереди недостаточно для удержания оболочки до
уведомления: пока ждёте его, храните `Reference` в своей структуре. `poll()` не блокирует, `remove()` ждёт уведомления;
для управляемого завершения фонового обработчика нужен протокол отмены.

```java
ReferenceQueue<Object> queue = new ReferenceQueue<>();
Object value = new Object();
WeakReference<Object> reference = new WeakReference<>(value, queue);
reference.enqueue(); // Явно очищает reference и ставит её в очередь; не имитирует гарантию запуска GC.
System.out.println(reference.get());    // null
System.out.println(queue.poll() == reference); // true
Reference.reachabilityFence(value); // Referent может при этом оставаться живым.
```

Явный `clear()` очищает reference, но сам по себе не равнозначен `enqueue()`. Уведомление в очереди сообщает о reference,
поэтому не следует трактовать каждое уведомление как доказательство, что объект уже физически освобождён.

## Сравнение типов ссылок

| Ссылка | Доступ к referent | Что разрешает GC | Типичное применение |
|---|---|---|---|
| Strong | Обычное обращение | Не собирать объект при сильной достижимости | Рабочие данные |
| Soft | `get()` до очистки | Очистить по потребности в памяти | Восстанавливаемые данные с оговорками о политике кэша |
| Weak | `get()` до очистки | Очистить при обнаружении weak reachability | Ассоциации и канонизация |
| Phantom | `get()` всегда `null` | Уведомить о phantom reachability | Резервная очистка внешнего ресурса |

Если у одного объекта одновременно есть soft и weak references, soft-путь учитывается при определении достижимости.
Нельзя ожидать обязательного `weak.get()==null`, пока тот же объект ещё softly reachable.

## Практические примеры

### 1. Canonicalizing String Pool

**Задача:** равные строки разделяют экземпляр, пока клиент удерживает его, но пул не становится владельцем всех строк навсегда.

```java
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

final class StringInterner {
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
```

Слабые и ключи, и значения; `synchronized` защищает весь поиск и вставку. Вариант
`ConcurrentHashMap<String, WeakReference<String>>`, в котором ключ и referent — та же строка, удерживал бы её сильным ключом.
Одна потокобезопасная map также не делает последовательность `get`/`put` атомарной канонизацией.

**Проверка:** два вызова с разными `new String("same")` возвращают один объект, пока результат первого вызова удерживается.
Сборка неиспользуемой строки допускается, но не проверяется ожиданием фиксированного числа миллисекунд.

### 2. Image Cache с SoftReference

Путь к изображению может быть сильным ключом, поскольку referent — отдельный объект изображения. Для повторного заполнения
нужна политика обработки промаха. Самая существенная гонка очистки:

1. Значение по пути очищено, его старая reference попала в очередь.
2. По тому же пути записано новое изображение.
3. Обработчик очереди читает старую reference.

Удаление только по пути уничтожило бы новую запись. Нужно удалить пару **ключ + конкретная reference**:

```java
Reference<? extends V> reference;
while ((reference = queue.poll()) != null) {
    @SuppressWarnings("unchecked") // Приватная очередь содержит только Entry данного cache.
    Entry<K, V> entry = (Entry<K, V>) reference;
    entries.remove(entry.key, entry);
}
```

Здесь `Entry<K,V>` наследует `SoftReference<V>` и хранит ключ; `entries` — `ConcurrentHashMap<K, Entry<K,V>>`.
Полная реализация `SoftValueCache` находится в [CoreExamples.java](../../examples/java-core/CoreExamples.java).
Условное удаление опирается на [ConcurrentMap.remove(key, value)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentMap.html#remove(java.lang.Object,java.lang.Object)).

**Проверка:** сохраните старую reference, замените значение, явно вызовите у старой `enqueue()`, затем очистите очередь.
Новое значение остаётся в map. Эта проверка детерминированна и не зависит от решения GC.
Количество wrapper-entries не равно объёму живых изображений; точный учёт памяти требует отдельной модели.

### 3. Listener Registry с WeakReference

```java
private final List<WeakReference<Consumer<String>>> listeners = new CopyOnWriteArrayList<>();

public void addListener(Consumer<String> listener) {
    listeners.add(new WeakReference<>(Objects.requireNonNull(listener)));
}

public void removeListener(Consumer<String> listener) {
    listeners.removeIf(reference -> {
        Consumer<String> current = reference.get();
        return current == null || current == listener;
    });
}

public void fireEvent(String event) {
    for (WeakReference<Consumer<String>> reference : listeners) {
        Consumer<String> listener = reference.get();
        if (listener != null) listener.accept(event);
    }
    listeners.removeIf(reference -> reference.refersTo(null));
}
```

Это методы класса с импортами `java.lang.ref.WeakReference`, `java.util.List`, `java.util.Objects`,
`java.util.concurrent.CopyOnWriteArrayList`, `java.util.function.Consumer`.
Обработчики вызываются вне операции изменения списка. Клиент обязан хранить сильную ссылку на слушателя, пока хочет получать
события. Политику обработки исключений и порядок вызовов нужно определить отдельно; этот пример распространяет исключение вызывающему коду.

### 4. Resource Cleanup с PhantomReference

В прикладном коде удобнее `Cleaner`: он управляет reference-объектами и выполняет action не более одного раза.
Explicit `close()` и резервная очистка должны использовать одну action, чтобы исключить повторное освобождение.

```java
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicInteger;

final class ManagedResource implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private static final class State implements Runnable {
        private final AtomicInteger cleanups;
        State(AtomicInteger cleanups) { this.cleanups = cleanups; }
        public void run() { cleanups.incrementAndGet(); }
    }

    private final Cleaner.Cleanable cleanable;

    ManagedResource(AtomicInteger cleanups) {
        cleanable = CLEANER.register(this, new State(cleanups));
    }

    public void close() { cleanable.clean(); }
}
```

Счётчик представляет освобождение отдельного внешнего ресурса; пример не выделяет native memory.
`State` — статический вложенный класс и не содержит владельца. Лямбда, обращающаяся к полю владельца через `this`, могла бы
удержать его и не допустить автоматическую очистку. Код реального освобождения должен быть коротким и не бросать исключения,
на которые полагается вызывающая сторона. Два явных `close()` увеличивают счётчик один раз.

## Best Practices

1. Для нужного сейчас объекта используйте обычную сильную ссылку; не заменяйте владение ресурсом reference-механизмами.
2. Задавайте границы кэша явно; `SoftReference` не заменяет TTL/maximum size.
3. Обрабатывайте очередь и удаляйте только соответствующую reference. Фоновому обработчику задайте завершение и обработку ошибок.
4. Различайте weak keys и weak values; проверяйте весь граф удержания, включая обратную ссылку из значения на ключ.
5. Предпочитайте `try-with-resources`; Cleaner оставляйте резервной мерой. Не используйте `finalize()` в новом коде.
6. Для WeakHashMap синхронизируйте также составные операции. Обёртка `synchronizedMap` не объединяет произвольные `get`/`put` в одну атомарную операцию.
7. Для диагностики используйте heap dump, retained paths и GC-логи reference processing; сравнивайте одинаковые нагрузки.

## Вопросы на собеседовании

1. **В чём разница между Soft и Weak references?**
   *Ответ:* JVM может очищать soft references по потребности в памяти; weak references очищаются при обнаружении weak reachability. Ни один механизм не даёт TTL.
2. **Почему PhantomReference.get() всегда возвращает null?**
   *Ответ:* Это механизм уведомления и cleanup, не способ вернуть referent в использование. Даже до потери сильных ссылок `get()` уже возвращает `null`.
3. **Как работает WeakHashMap?**
   *Ответ:* Ключи слабые, значения сильные; внутренний механизм очереди удаляет записи с очищенными ключами. Значение не должно удерживать ключ.
4. **Когда использовать каждый тип reference?**
   *Ответ:* Strong — владение рабочими данными; weak — ассоциации/канонизация; soft — восстанавливаемые данные с оговорками; phantom/Cleaner — резервная очистка внешнего ресурса.
5. **Что такое ReferenceQueue и зачем она нужна?**
   *Ответ:* Доставляет reference-объекты для очистки метаданных или внешнего состояния. Время уведомления не фиксировано, а оболочку нужно удерживать до обработки.
6. **Почему finalize() считается плохой практикой?**
   *Ответ:* Нет своевременности, есть проблемы с воскрешением и обработкой ошибок. Для явного освобождения используйте AutoCloseable; резервную очистку можно построить через Cleaner.
7. **Как настроить поведение SoftReference?**
   *Ответ:* В HotSpot есть `SoftRefLRUPolicyMSPerMB`, однако это эвристика конкретной JVM, не контракт TTL. Для TTL нужен прикладной кэш.
8. **Может ли WeakReference предотвратить OutOfMemoryError?**
   *Ответ:* Нет: достижимые объекты, ключи/оболочки, другие утечки и быстрые аллокации могут исчерпать память независимо от слабых значений.
9. **В чём разница между WeakHashMap и ConcurrentHashMap с WeakReference values?**
   *Ответ:* В первой слабые ключи и сильные значения; во второй — сильные ключи и слабые значения. Они управляют разным временем жизни и не взаимозаменяемы.
10. **Какой тип reference использует String.intern()?**
    *Ответ:* Java API задаёт канонизацию строк, а не вечное хранение interned-строки. В OpenJDK HotSpot 21 динамическая StringTable использует weak handles; литералы и другие сильные пути могут продолжать удерживать строку. Нельзя утверждать, что любая interned-строка живёт до конца приложения. См. [StringTable OpenJDK 21](https://github.com/openjdk/jdk21u/blob/master/src/hotspot/share/classfile/stringTable.cpp) и [String.intern](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#intern()).

Самостоятельные проверки канонизации, очистки устаревшей cache entry и повторного `close()`:
[README примеров](../../examples/java-core/README.html).
