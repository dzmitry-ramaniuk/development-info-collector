# Коллекции и обобщения


## Содержание

1. [Каркас коллекций Java](#каркас-коллекций-java)
   - [List](#list)
   - [Set](#set)
   - [Queue и Deque](#queue-и-deque)
   - [Map](#map)
2. [Детали реализации коллекций](#детали-реализации-коллекций)
   - [Внутреннее устройство ArrayList](#внутреннее-устройство-arraylist)
   - [Внутреннее устройство LinkedList](#внутреннее-устройство-linkedlist)
   - [Внутреннее устройство HashMap](#внутреннее-устройство-hashmap)
   - [Внутреннее устройство TreeMap](#внутреннее-устройство-treemap)
   - [LinkedHashMap и LRU-кэш](#linkedhashmap-и-lru-кэш)
3. [Итераторы](#итераторы)
   - [Iterator и ListIterator](#iterator-и-listiterator)
   - [Fail-Fast vs Fail-Safe итераторы](#fail-fast-vs-fail-safe-итераторы)
   - [Spliterator](#spliterator)
4. [Queue подробно](#queue-подробно)
   - [PriorityQueue](#priorityqueue)
   - [ArrayDeque](#arraydeque)
   - [LinkedList как Deque](#linkedlist-как-deque)
   - [Блокирующие очереди](#блокирующие-очереди)
   - [Сравнение реализаций Queue](#сравнение-реализаций-queue)
5. [Сортировка в коллекциях](#сортировка-в-коллекциях)
   - [Comparable и Comparator](#comparable-и-comparator)
   - [Алгоритмы сортировки](#алгоритмы-сортировки)
   - [Сортировка списков](#сортировка-списков)
   - [Сортировка с помощью Stream API](#сортировка-с-помощью-stream-api)
6. [Методы для работы с коллекциями](#методы-для-работы-с-коллекциями)
   - [Утилитные методы класса Collections](#утилитные-методы-класса-collections)
   - [Методы интерфейса Collection](#методы-интерфейса-collection)
   - [Методы интерфейса List](#методы-интерфейса-list)
   - [Методы интерфейса Set](#методы-интерфейса-set)
   - [Методы интерфейса Map](#методы-интерфейса-map)
   - [Фабричные методы для создания коллекций](#фабричные-методы-для-создания-коллекций)
7. [Контракты equals и hashCode](#контракты-equals-и-hashcode)
   - [Контракт equals](#контракт-equals)
   - [Контракт hashCode](#контракт-hashcode)
   - [Связь equals и hashCode в коллекциях](#связь-equals-и-hashcode-в-коллекциях)
   - [Правильная реализация](#правильная-реализация)
   - [Распространённые ошибки](#распространённые-ошибки)
8. [Generics и типобезопасность](#generics-и-типобезопасность)
9. [Специализированные коллекции](#специализированные-коллекции)
10. [Best practices](#best-practices)
11. [Практические упражнения](#практические-упражнения)
12. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Каркас коллекций Java
Стандартный пакет `java.util` задаёт каркас через интерфейсы `Collection`, `List`, `Set`, `Queue`, `Deque`, `Map`. Каждый
интерфейс описывает контракт по операциям, а конкретные структуры выбираются в зависимости от требуемых сложностей,
гарантий порядка и потокобезопасности.

### List
**List** обеспечивает упорядоченный набор элементов с доступом по индексу и поддержкой дубликатов.

- **`ArrayList`**. Хранит данные в динамическом массиве. При переполнении ёмкость увеличивается примерно на 50 %, копируя
  элементы в новый массив. Вставка и удаление из середины требуют сдвига элементов, но чтение по индексу выполняется за O(1).
  В многопоточном коде не потокобезопасен: требуется внешняя синхронизация (`Collections.synchronizedList`) или использование
  неизменяемой копии. Итератор fail-fast — при структурных изменениях в другом потоке он бросает `ConcurrentModificationException`.
- **`LinkedList`**. Построен на двусвязных узлах `Node` с полями `item`, `prev`, `next`. Добавление и удаление с концов
  выполняются за O(1), поиск по индексу — за O(n). В многопоточном окружении также требует синхронизации; кроме того,
  итерация небезопасна без внешних блокировок.
- **`CopyOnWriteArrayList`**. Для каждой структурной модификации создаёт новый массив. Чтения выполняются без блокировок и не
  видят незавершённых изменений. Подходит для сценариев «много чтений, мало записей». Итераторы не fail-fast: они работают с
  моментальным снимком. Записи дорогие по памяти и времени, поэтому в многопоточности подходит для конфигурационных списков,
  событийных слушателей и т.п.

### Set
**Set** хранит уникальные элементы без дубликатов. Гарантии по порядку зависят от реализации.

- **`HashSet`**. Реализован поверх `HashMap`, где элементы выступают ключами, а значением служит заглушка. Использует массив
  бакетов с цепочками или сбалансированными деревьями при длинных цепях. В многопоточности не потокобезопасен; необходимо
  внешнее блокирование. Fail-fast итераторы ведут себя аналогично `ArrayList`.
- **`LinkedHashSet`**. Сохраняет порядок вставки благодаря двусвязному списку поверх `HashSet`. Потокобезопасность аналогична
  `HashSet` — требуется внешняя синхронизация.
- **`TreeSet`**. Использует `NavigableMap` (по умолчанию `TreeMap`) с красно-чёрным деревом. Поддерживает сортированный порядок,
  операции навигации (`higher`, `lower`, `subSet`). Не потокобезопасен; при совместном доступе необходимы блокировки либо
  замена на `ConcurrentSkipListSet`.
- **`EnumSet`**. Внутренне — битовый набор (один или несколько `long`), поэтому операции выполняются очень быстро. Потокобезопасность
  не обеспечивается, но благодаря неизменяемости набора перечислений (фиксированное количество значений) легко создавать
  неизменяемые снапшоты и использовать `Collections.synchronizedSet` при необходимости.

### Queue и Deque
**Queue/Deque** предоставляют операции FIFO или двустороннего доступа.

- **`ArrayDeque`**. Кольцевой буфер на массиве. Поддерживает операции стека и очереди за O(1). Не блокирующая и
  непотокобезопасная: синхронизируйте при совместном доступе или используйте `ConcurrentLinkedDeque`.
- **`PriorityQueue`**. Реализует двоичную кучу (min-heap). Операции `offer`/`poll` выполняются за O(log n), доступ к минимальному
  элементу — O(1). Итератор не гарантирует порядок. Потокобезопасность отсутствует; для многопоточности используйте `PriorityBlockingQueue`
  или внешние блокировки.
- **`ConcurrentLinkedQueue`**. Неблокирующая очередь FIFO на связном списке с атомарными операциями (CAS). Поддерживает высокую
  конкуренцию без блокировок, но не подходит для структур, требующих ограниченной ёмкости. Итерации слабосогласованные — могут
  пропускать или повторять элементы, если очередь изменяется во время обхода.
- **`BlockingQueue`** (например, `ArrayBlockingQueue`, `LinkedBlockingQueue`). Предоставляют операции, ожидающие появления
  свободного места или элемента. Используют внутренние блокировки (`ReentrantLock`) и условные переменные. Потокобезопасны, но
  возможна конкуренция между производителями/потребителями; важно выбирать правильную ёмкость и стратегию обработки прерываний.

### Map
**Map** отображает ключи в значения, может поддерживать порядок или особенности хранения ссылок.

- **`HashMap`**. Массив бакетов с цепочками (`Node<K,V>`). При длинных цепочках (>=8) и достаточной ёмкости бакет превращается
  в красно-чёрное дерево. Рехеширование происходит при превышении `loadFactor`. Потокобезопасность отсутствует; конкурентный
  доступ может приводить к потерям данных. При необходимости используйте `Collections.synchronizedMap` или другие реализации.
- **`LinkedHashMap`**. Наследует `HashMap`, добавляя двусвязный список для порядка вставки или доступа (`accessOrder=true`). Часто
  применяется для LRU-кэшей. Потокобезопасность требует внешней синхронизации.
- **`TreeMap`**. Красно-чёрное дерево, реализующее `NavigableMap`. Обеспечивает отсортированные ключи и диапазонные операции.
  В многопоточности следует применять `Collections.synchronizedSortedMap` или `ConcurrentSkipListMap`.
- **`ConcurrentHashMap`**. В Java 8 использует массив бакетов с узлами и синхронизацию на уровне цепочек с помощью CAS и
  `synchronized` на отдельных бакетах при рехешировании. Обеспечивает неблокирующие чтения и ограниченную блокировку при записях.
  Итераторы слабосогласованные: отражают состояние на момент запуска и могут видеть некоторые последующие изменения. Поддерживает
  атомарные операции (`compute`, `merge`, `forEach`) с учётом многопоточности.
- **`WeakHashMap`**. Хранит ключи через `WeakReference`. Когда на ключ больше нет сильных ссылок, запись удаляется GC. Использует
  `ReferenceQueue` для очистки. Потокобезопасности нет; для многопоточности применяют внешнюю синхронизацию или `ConcurrentHashMap`
  с обёртками на слабых ссылках из сторонних библиотек.

## Детали реализации коллекций

Понимание внутреннего устройства коллекций критично для выбора правильной структуры данных и оптимизации производительности. 
Рассмотрим детали реализации наиболее популярных коллекций.

### Внутреннее устройство ArrayList

`ArrayList` построен на основе динамического массива `Object[] elementData`. Основные характеристики:

**Структура данных:**
```java
public class ArrayList<E> {
    private transient Object[] elementData;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
}
```

**Механизм роста:**
- При создании без параметров начальная ёмкость = 0 (с Java 7+)
- При первом добавлении элемента ёмкость становится DEFAULT_CAPACITY (10)
- При превышении ёмкости создаётся новый массив размером `oldCapacity + (oldCapacity >> 1)` (рост на ~50%)
- Все элементы копируются в новый массив через `System.arraycopy()`

**Временная сложность операций:**
- `get(index)`: O(1) — прямой доступ по индексу
- `add(element)`: амортизированная O(1), O(n) при расширении массива
- `add(index, element)`: O(n) — требуется сдвиг всех элементов справа
- `remove(index)`: O(n) — требуется сдвиг элементов для заполнения пустоты
- `contains(element)`: O(n) — линейный поиск

**Оптимизация памяти:**
```java
// Уменьшение размера массива до текущего size
list.trimToSize();

// Предварительное выделение ёмкости для избежания многократных расширений
List<String> list = new ArrayList<>(1000);
```

**Проблемы при многопоточности:**
```java
// Проблема: одновременное изменение может привести к ArrayIndexOutOfBoundsException
// или потере данных из-за race condition при расширении массива
List<String> list = new ArrayList<>();
// Поток 1: list.add("A");
// Поток 2: list.add("B");  // Может возникнуть конфликт

// Решение 1: Синхронизация
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// Решение 2: CopyOnWriteArrayList для сценария "много чтений"
List<String> cowList = new CopyOnWriteArrayList<>();
```

### Внутреннее устройство LinkedList

`LinkedList` — двусвязный список с узлами, содержащими ссылки на предыдущий и следующий элементы.

**Структура данных:**
```java
public class LinkedList<E> {
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
    }
    
    transient Node<E> first;  // Ссылка на первый узел
    transient Node<E> last;   // Ссылка на последний узел
    transient int size = 0;
}
```

**Временная сложность операций:**
- `addFirst()`, `addLast()`: O(1) — добавление в начало/конец
- `removeFirst()`, `removeLast()`: O(1) — удаление с концов
- `get(index)`: O(n) — требуется проход от начала или конца (оптимизация: ближайший конец)
- `add(index, element)`: O(n) — поиск позиции + вставка
- `contains(element)`: O(n) — линейный поиск

**Использование как Deque:**
```java
Deque<String> deque = new LinkedList<>();
deque.addFirst("A");   // [A]
deque.addLast("B");    // [A, B]
deque.removeFirst();   // [B]
deque.removeLast();    // []
```

**Потребление памяти:**
- Каждый элемент требует дополнительно 2 ссылки (prev, next) ≈ 16 байт на 64-битной JVM
- Для хранения 1000 элементов Integer: ArrayList ≈ 4 КБ, LinkedList ≈ 24 КБ

**Когда использовать LinkedList:**
- Частые вставки/удаления в начале или конце списка
- Реализация очереди (Queue) или двусторонней очереди (Deque)
- Редкий доступ по индексу

**Когда НЕ использовать:**
- Частый произвольный доступ по индексу (используйте ArrayList)
- Минимизация потребления памяти

### Внутреннее устройство HashMap

`HashMap` использует массив бакетов (buckets) и хеш-функцию для быстрого доступа к элементам.

**Структура данных:**
```java
public class HashMap<K,V> {
    transient Node<K,V>[] table;  // Массив бакетов
    transient int size;           // Количество пар ключ-значение
    int threshold;                // Порог для рехеширования (capacity * loadFactor)
    final float loadFactor;       // Коэффициент загрузки (по умолчанию 0.75)
    
    static class Node<K,V> {
        final int hash;
        final K key;
        V value;
        Node<K,V> next;  // Следующий узел в цепочке (при коллизии)
    }
}
```

**Механизм работы:**

1. **Вычисление индекса бакета:**
```java
int hash = key.hashCode();
int bucket = (hash ^ (hash >>> 16)) & (table.length - 1);
```

2. **Обработка коллизий (до Java 8):**
- Коллизии разрешались только через связные списки
- При большом количестве коллизий деградация до O(n)

3. **Оптимизация в Java 8+:**
- Если цепочка в бакете превышает TREEIFY_THRESHOLD (8 элементов), она превращается в красно-чёрное дерево
- Требуется минимальная ёмкость MIN_TREEIFY_CAPACITY (64)
- Уменьшает worst-case с O(n) до O(log n)

**Рехеширование (resize):**
```java
// Происходит когда size > threshold
// Новая ёмкость = старая ёмкость * 2
// Все элементы перераспределяются по новым бакетам
```

**Пример рехеширования:**
```java
Map<String, Integer> map = new HashMap<>(4, 0.75f);
// Начальная ёмкость: 4, порог: 4 * 0.75 = 3
map.put("A", 1);  // size=1
map.put("B", 2);  // size=2
map.put("C", 3);  // size=3
map.put("D", 4);  // size=4, triggers resize to capacity=8, threshold=6
```

**Временная сложность:**
- `put()`, `get()`, `remove()`: средняя O(1), worst-case O(log n) после Java 8
- `containsKey()`: средняя O(1)

**Настройка производительности:**
```java
// Выбор правильной начальной ёмкости для известного количества элементов
int expectedSize = 1000;
int capacity = (int) (expectedSize / 0.75 + 1);
Map<String, Integer> map = new HashMap<>(capacity);

// Снижение loadFactor уменьшает коллизии, но увеличивает потребление памяти
Map<String, Integer> lowCollisionMap = new HashMap<>(16, 0.5f);
```

### Внутреннее устройство TreeMap

`TreeMap` реализован на основе красно-чёрного дерева — самобалансирующегося бинарного дерева поиска.

**Структура данных:**
```java
public class TreeMap<K,V> {
    private transient Entry<K,V> root;
    private int size = 0;
    private final Comparator<? super K> comparator;
    
    static final class Entry<K,V> {
        K key;
        V value;
        Entry<K,V> left;
        Entry<K,V> right;
        Entry<K,V> parent;
        boolean color = BLACK;
    }
}
```

**Свойства красно-чёрного дерева:**
1. Каждый узел красный или чёрный
2. Корень всегда чёрный
3. Красные узлы не могут иметь красных детей
4. Все пути от корня до листьев содержат одинаковое количество чёрных узлов
5. Высота дерева ≤ 2 * log₂(n + 1)

**Балансировка:**
- При вставке/удалении выполняются вращения (rotations) для сохранения свойств
- Гарантирует worst-case O(log n) для всех операций

**Временная сложность:**
- `put()`, `get()`, `remove()`: O(log n)
- `firstKey()`, `lastKey()`: O(log n) — поиск крайних элементов
- `subMap()`, `headMap()`, `tailMap()`: O(log n) — создание view

**Сравнение элементов:**
```java
// Используется Comparator (если предоставлен) или natural ordering (Comparable)
TreeMap<String, Integer> map1 = new TreeMap<>();  // Natural ordering
TreeMap<String, Integer> map2 = new TreeMap<>(Comparator.reverseOrder());  // Обратный порядок
TreeMap<String, Integer> map3 = new TreeMap<>((a, b) -> a.length() - b.length());  // По длине строки
```

**Когда использовать TreeMap:**
- Нужны отсортированные ключи
- Требуются диапазонные операции (subMap, headMap, tailMap)
- Необходима навигация (ceilingKey, floorKey, higherKey, lowerKey)

### LinkedHashMap и LRU-кэш

`LinkedHashMap` расширяет `HashMap`, добавляя двусвязный список для поддержания порядка элементов.

**Два режима работы:**

1. **Insertion-order (по умолчанию):**
```java
Map<String, Integer> map = new LinkedHashMap<>();
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
// Итерация: A -> B -> C (порядок вставки)
```

2. **Access-order (для LRU):**
```java
Map<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
map.put("A", 1);
map.put("B", 2);
map.put("C", 3);
map.get("A");  // A перемещается в конец
// Итерация: B -> C -> A (порядок доступа)
```

**Реализация LRU-кэша:**

```java
public class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxEntries;
    
    public LRUCache(int maxEntries) {
        super(16, 0.75f, true);  // access-order = true
        this.maxEntries = maxEntries;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxEntries;  // Автоматическое удаление старейшего элемента
    }
}

// Использование
LRUCache<String, String> cache = new LRUCache<>(3);
cache.put("1", "one");
cache.put("2", "two");
cache.put("3", "three");
cache.get("1");  // "1" становится самым свежим
cache.put("4", "four");  // "2" удаляется как самый старый
// Кэш: [3=three, 1=one, 4=four]
```

**Особенности LRU-кэша:**
- Метод `removeEldestEntry()` вызывается после каждого `put()`
- Автоматическое удаление наименее использованных элементов
- Потокобезопасность требует внешней синхронизации

**Потокобезопасный LRU-кэш:**
```java
public class ThreadSafeLRUCache<K, V> {
    private final int maxEntries;
    private final Map<K, V> cache;
    
    public ThreadSafeLRUCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.cache = Collections.synchronizedMap(
            new LinkedHashMap<K, V>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > maxEntries;
                }
            }
        );
    }
    
    public V get(K key) {
        synchronized (cache) {
            return cache.get(key);
        }
    }
    
    public void put(K key, V value) {
        synchronized (cache) {
            cache.put(key, value);
        }
    }
}
```

**Альтернативы для production:**
- **Caffeine** — высокопроизводительная библиотека кэширования с LRU, LFU и другими политиками
- **Guava Cache** — кэш от Google с поддержкой expiration, weighing, refresh
- **Ehcache** — enterprise-уровневое решение с распределённым кэшированием

## Итераторы

Итераторы предоставляют унифицированный способ обхода элементов коллекций без раскрытия внутренней структуры.

### Iterator и ListIterator

**Iterator** — базовый интерфейс для обхода коллекций:

```java
public interface Iterator<E> {
    boolean hasNext();  // Есть ли следующий элемент
    E next();          // Получить следующий элемент и переместить курсор
    void remove();     // Удалить текущий элемент (optional operation)
}
```

**Использование Iterator:**
```java
List<String> list = Arrays.asList("A", "B", "C", "D");
Iterator<String> iterator = list.iterator();

while (iterator.hasNext()) {
    String element = iterator.next();
    System.out.println(element);
    
    // Безопасное удаление во время итерации
    if (element.equals("B")) {
        iterator.remove();  // Удаляет "B"
    }
}
```

> **Важно**: Удаление через `iterator.remove()` — единственный безопасный способ удаления элемента во время итерации.

**Проблема удаления напрямую:**
```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
for (String element : list) {
    if (element.equals("B")) {
        list.remove(element);  // ConcurrentModificationException!
    }
}
```

**ListIterator** — расширенный итератор для списков с двусторонним обходом:

```java
public interface ListIterator<E> extends Iterator<E> {
    boolean hasPrevious();        // Есть ли предыдущий элемент
    E previous();                 // Получить предыдущий элемент
    int nextIndex();              // Индекс следующего элемента
    int previousIndex();          // Индекс предыдущего элемента
    void set(E e);               // Заменить текущий элемент
    void add(E e);               // Вставить элемент перед текущей позицией
}
```

**Пример использования ListIterator:**
```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
ListIterator<String> iterator = list.listIterator();

// Прямой обход
while (iterator.hasNext()) {
    System.out.println(iterator.next());
}

// Обратный обход
while (iterator.hasPrevious()) {
    System.out.println(iterator.previous());
}

// Модификация во время итерации
ListIterator<String> it = list.listIterator();
while (it.hasNext()) {
    String element = it.next();
    if (element.equals("B")) {
        it.set("B_modified");  // Замена элемента
        it.add("B_new");       // Вставка нового элемента после текущего
    }
}
// Результат: [A, B_modified, B_new, C, D]
```

### Fail-Fast vs Fail-Safe итераторы

Коллекции в Java используют два разных подхода к обработке конкурентных модификаций.

#### Fail-Fast итераторы

**Определение:** Fail-fast итераторы немедленно бросают `ConcurrentModificationException` при обнаружении структурной модификации коллекции после создания итератора.

**Механизм работы:**
```java
public class ArrayList<E> {
    transient int modCount = 0;  // Счётчик модификаций
    
    public boolean add(E e) {
        modCount++;  // Увеличивается при каждом изменении структуры
        // ...
    }
    
    private class Itr implements Iterator<E> {
        int expectedModCount = modCount;  // Запоминается при создании итератора
        
        public E next() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            // ...
        }
    }
}
```

**Примеры fail-fast коллекций:**
- `ArrayList`, `LinkedList`, `HashSet`, `HashMap`, `TreeMap`

**Пример проблемы:**
```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));

for (String item : list) {
    list.add("D");  // ConcurrentModificationException!
}

// Решение 1: Использовать Iterator.remove()
Iterator<String> it = list.iterator();
while (it.hasNext()) {
    String item = it.next();
    if (someCondition(item)) {
        it.remove();  // Безопасно
    }
}

// Решение 2: Собрать элементы для удаления отдельно
List<String> toRemove = new ArrayList<>();
for (String item : list) {
    if (someCondition(item)) {
        toRemove.add(item);
    }
}
list.removeAll(toRemove);

// Решение 3: Использовать removeIf (Java 8+)
list.removeIf(item -> someCondition(item));
```

#### Fail-Safe итераторы

**Определение:** Fail-safe итераторы работают с копией данных или используют специальные механизмы синхронизации, позволяя безопасно модифицировать коллекцию во время итерации.

**Механизм работы:**

1. **Copy-on-Write подход (CopyOnWriteArrayList):**
```java
public class CopyOnWriteArrayList<E> {
    private volatile Object[] array;
    
    public boolean add(E e) {
        synchronized (lock) {
            Object[] newArray = Arrays.copyOf(array, array.length + 1);
            newArray[array.length] = e;
            array = newArray;  // Атомарная замена ссылки
        }
        return true;
    }
    
    public Iterator<E> iterator() {
        return new COWIterator<>(array, 0);  // Работает с snapshot
    }
}
```

2. **Weakly consistent итераторы (ConcurrentHashMap):**
- Отражают состояние на момент создания или позже
- Могут пропустить или увидеть новые элементы
- Никогда не бросают ConcurrentModificationException

**Примеры fail-safe коллекций:**
- `CopyOnWriteArrayList`, `CopyOnWriteArraySet`
- `ConcurrentHashMap`, `ConcurrentSkipListMap`, `ConcurrentSkipListSet`
- `ConcurrentLinkedQueue`, `ConcurrentLinkedDeque`

**Пример использования:**
```java
List<String> list = new CopyOnWriteArrayList<>(Arrays.asList("A", "B", "C"));

for (String item : list) {
    list.add("D");  // Безопасно! Итератор работает со старым snapshot
    list.remove("A");  // Безопасно! Не влияет на текущую итерацию
}

// ConcurrentHashMap
Map<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

for (Map.Entry<String, Integer> entry : map.entrySet()) {
    map.put("C", 3);  // Безопасно! Может быть видно или не видно в текущей итерации
}
```

**Сравнение подходов:**

| Характеристика | Fail-Fast | Fail-Safe |
|----------------|-----------|-----------|
| Исключение | Бросает ConcurrentModificationException | Не бросает исключений |
| Производительность | Высокая для чтения | Может быть медленнее (копирование) |
| Гарантии | Обнаруживает модификации | Weakly consistent |
| Память | Экономна | Дополнительные копии (COW) |
| Использование | Однопоточные приложения | Многопоточные приложения |
| Примеры | ArrayList, HashMap | CopyOnWriteArrayList, ConcurrentHashMap |

**Рекомендации по выбору:**
- **Fail-fast**: Для однопоточных приложений или когда модификации контролируются
- **Fail-safe**: Для многопоточных приложений с частыми чтениями и редкими записями
- **CopyOnWrite**: Когда чтений значительно больше, чем записей (listeners, observers)
- **Concurrent**: Для высоконагруженных многопоточных сценариев

### Spliterator

**Spliterator** (Splitable Iterator) — итератор для параллельной обработки коллекций, введённый в Java 8 для Stream API.

**Основные методы:**
```java
public interface Spliterator<T> {
    boolean tryAdvance(Consumer<? super T> action);  // Обработать следующий элемент
    Spliterator<T> trySplit();                       // Разделить на две части
    long estimateSize();                             // Оценка количества элементов
    int characteristics();                           // Характеристики (ORDERED, SIZED, etc.)
}
```

**Характеристики Spliterator:**
- `ORDERED` — элементы имеют определённый порядок
- `DISTINCT` — все элементы уникальны
- `SORTED` — элементы отсортированы
- `SIZED` — размер известен
- `NONNULL` — элементы не могут быть null
- `IMMUTABLE` — коллекция неизменяема
- `CONCURRENT` — коллекция потокобезопасна
- `SUBSIZED` — все spliterator'ы после split имеют известный размер

**Пример использования:**
```java
List<String> list = Arrays.asList("A", "B", "C", "D", "E", "F");
Spliterator<String> spliterator = list.spliterator();

// Разделение для параллельной обработки
Spliterator<String> split1 = spliterator.trySplit();  // Первая половина
Spliterator<String> split2 = spliterator;              // Вторая половина

// Обработка в двух потоках
new Thread(() -> split1.forEachRemaining(System.out::println)).start();
new Thread(() -> split2.forEachRemaining(System.out::println)).start();
```

**Использование в Stream API:**
```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

// Stream автоматически использует Spliterator для параллелизации
int sum = numbers.parallelStream()
                 .filter(n -> n % 2 == 0)
                 .mapToInt(Integer::intValue)
                 .sum();
```

**Пользовательский Spliterator:**
```java
public class RangeSpliterator implements Spliterator<Integer> {
    private int current;
    private final int end;
    
    public RangeSpliterator(int start, int end) {
        this.current = start;
        this.end = end;
    }
    
    @Override
    public boolean tryAdvance(Consumer<? super Integer> action) {
        if (current < end) {
            action.accept(current++);
            return true;
        }
        return false;
    }
    
    @Override
    public Spliterator<Integer> trySplit() {
        int mid = (current + end) / 2;
        if (mid - current < 10) return null;  // Не делим маленькие диапазоны
        
        Spliterator<Integer> split = new RangeSpliterator(current, mid);
        current = mid;
        return split;
    }
    
    @Override
    public long estimateSize() {
        return end - current;
    }
    
    @Override
    public int characteristics() {
        return ORDERED | SIZED | SUBSIZED | NONNULL | IMMUTABLE;
    }
}

// Использование
StreamSupport.stream(new RangeSpliterator(0, 100), true)  // parallel=true
             .forEach(System.out::println);
```

## Queue подробно

Queue и его подинтерфейсы предоставляют различные реализации очередей для специфических сценариев использования.

### PriorityQueue

**PriorityQueue** — очередь с приоритетами, реализованная на основе двоичной кучи (binary heap).

**Структура данных:**
```java
public class PriorityQueue<E> {
    transient Object[] queue;  // Массив-куча
    private int size = 0;
    private final Comparator<? super E> comparator;
}
```

**Свойства кучи:**
- Для узла с индексом `i`:
  - Родитель: `(i - 1) / 2`
  - Левый ребёнок: `2 * i + 1`
  - Правый ребёнок: `2 * i + 2`
- Min-heap: родитель ≤ детей (по умолчанию)
- Max-heap: родитель ≥ детей (с Comparator.reverseOrder())

**Временная сложность:**
- `offer(e)`: O(log n) — вставка с просеиванием вверх (sift-up)
- `poll()`: O(log n) — удаление корня с просеиванием вниз (sift-down)
- `peek()`: O(1) — доступ к минимальному элементу
- `remove(o)`: O(n) — требуется поиск + удаление
- `contains(o)`: O(n) — линейный поиск

**Примеры использования:**

1. **Natural ordering (min-heap):**
```java
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.offer(5);
minHeap.offer(2);
minHeap.offer(8);
minHeap.offer(1);

while (!minHeap.isEmpty()) {
    System.out.println(minHeap.poll());  // 1, 2, 5, 8
}
```

2. **Max-heap с Comparator:**
```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
maxHeap.offer(5);
maxHeap.offer(2);
maxHeap.offer(8);
maxHeap.offer(1);

while (!maxHeap.isEmpty()) {
    System.out.println(maxHeap.poll());  // 8, 5, 2, 1
}
```

3. **Пользовательские объекты:**
```java
record Task(String name, int priority) {}

PriorityQueue<Task> taskQueue = new PriorityQueue<>(
    Comparator.comparingInt(Task::priority)
);

taskQueue.offer(new Task("Low", 3));
taskQueue.offer(new Task("High", 1));
taskQueue.offer(new Task("Medium", 2));

while (!taskQueue.isEmpty()) {
    Task task = taskQueue.poll();
    System.out.println(task.name() + ": " + task.priority());
}
// High: 1
// Medium: 2
// Low: 3
```

4. **Топ-K элементов (min-heap размера K):**
```java
public List<Integer> topK(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);
    
    for (int num : nums) {
        minHeap.offer(num);
        if (minHeap.size() > k) {
            minHeap.poll();  // Удаляем минимум
        }
    }
    
    return new ArrayList<>(minHeap);
}
```

**Важные особенности:**
- Итератор не гарантирует порядок элементов (итерация по массиву, не по приоритету)
- Не потокобезопасна (используйте `PriorityBlockingQueue` для многопоточности)
- Не допускает `null` элементы
- Начальная ёмкость по умолчанию: 11

### ArrayDeque

**ArrayDeque** — двусторонняя очередь на основе кольцевого буфера (circular buffer).

**Структура данных:**
```java
public class ArrayDeque<E> {
    transient Object[] elements;
    transient int head;  // Индекс первого элемента
    transient int tail;  // Индекс следующей позиции для вставки
}
```

**Механизм кольцевого буфера:**
```
Начальное состояние: [_, _, _, _]  head=0, tail=0

addLast(A):  [A, _, _, _]  head=0, tail=1
addLast(B):  [A, B, _, _]  head=0, tail=2
addFirst(C): [A, B, _, C]  head=3, tail=2

pollFirst(): [A, B, _, _]  head=0, tail=2  (C удалён)
```

**Временная сложность:**
- `addFirst()`, `addLast()`: амортизированная O(1)
- `removeFirst()`, `removeLast()`: O(1)
- `getFirst()`, `getLast()`: O(1)
- `remove(o)`: O(n) — линейный поиск
- `contains(o)`: O(n)

**Рост ёмкости:**
- При переполнении ёмкость удваивается
- Элементы копируются в новый массив с нормализацией индексов

**Использование как Stack:**
```java
Deque<String> stack = new ArrayDeque<>();
stack.push("A");
stack.push("B");
stack.push("C");

while (!stack.isEmpty()) {
    System.out.println(stack.pop());  // C, B, A (LIFO)
}
```

**Использование как Queue:**
```java
Deque<String> queue = new ArrayDeque<>();
queue.offer("A");
queue.offer("B");
queue.offer("C");

while (!queue.isEmpty()) {
    System.out.println(queue.poll());  // A, B, C (FIFO)
}
```

**Преимущества ArrayDeque:**
- Быстрее LinkedList для операций на концах (нет overhead на Node)
- Меньше потребление памяти (нет ссылок prev/next)
- Лучшая locality of reference (элементы в массиве)
- Рекомендуется для реализации Stack вместо устаревшего класса Stack

**Ограничения:**
- Не потокобезопасна
- Не допускает `null` элементы
- Нет доступа по индексу (в отличие от LinkedList)

### LinkedList как Deque

LinkedList реализует интерфейс Deque, что делает его полноценной двусторонней очередью.

**Методы Deque в LinkedList:**
```java
LinkedList<String> deque = new LinkedList<>();

// Операции в начале
deque.addFirst("A");      // Вставка в начало
deque.offerFirst("B");    // Вставка в начало (не бросает исключение при ограничении)
String first = deque.getFirst();     // Получить первый (бросает NoSuchElementException если пуст)
String first2 = deque.peekFirst();   // Получить первый (возвращает null если пуст)
deque.removeFirst();      // Удалить первый
deque.pollFirst();        // Удалить первый (null если пуст)

// Операции в конце
deque.addLast("C");
deque.offerLast("D");
String last = deque.getLast();
String last2 = deque.peekLast();
deque.removeLast();
deque.pollLast();

// Stack операции
deque.push("E");          // = addFirst()
String top = deque.pop(); // = removeFirst()

// Queue операции
deque.offer("F");         // = addLast()
String element = deque.poll();  // = removeFirst()
```

**Когда использовать LinkedList как Deque:**
- Нужен доступ по индексу (хотя медленный O(n))
- Реализация алгоритмов с частыми вставками/удалениями в середине
- Совместимость с legacy кодом

**Когда использовать ArrayDeque:**
- Только операции на концах (Stack/Queue)
- Важна производительность
- Минимизация потребления памяти

### Блокирующие очереди

**BlockingQueue** — интерфейс для очередей с блокирующими операциями, используемых в producer-consumer паттерне.

**Типы операций:**

| Операция | Бросает исключение | Возвращает null/false | Блокируется | С таймаутом |
|----------|-------------------|----------------------|-------------|-------------|
| Вставка | `add(e)` | `offer(e)` | `put(e)` | `offer(e, timeout)` |
| Удаление | `remove()` | `poll()` | `take()` | `poll(timeout)` |
| Просмотр | `element()` | `peek()` | - | - |

**Основные реализации:**

1. **ArrayBlockingQueue** — ограниченная очередь на массиве:
```java
BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);  // Ёмкость 10

// Producer
new Thread(() -> {
    try {
        for (int i = 0; i < 20; i++) {
            queue.put("Item" + i);  // Блокируется если очередь полна
            System.out.println("Produced: Item" + i);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

// Consumer
new Thread(() -> {
    try {
        while (true) {
            String item = queue.take();  // Блокируется если очередь пуста
            System.out.println("Consumed: " + item);
            Thread.sleep(100);  // Имитация обработки
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();
```

2. **LinkedBlockingQueue** — опционально ограниченная очередь на связном списке:
```java
// Неограниченная (capacity = Integer.MAX_VALUE)
BlockingQueue<String> unbounded = new LinkedBlockingQueue<>();

// Ограниченная
BlockingQueue<String> bounded = new LinkedBlockingQueue<>(100);
```

3. **PriorityBlockingQueue** — неограниченная очередь с приоритетами:
```java
BlockingQueue<Task> queue = new PriorityBlockingQueue<>(10,
    Comparator.comparingInt(Task::priority)
);
```

4. **DelayQueue** — очередь с задержкой:
```java
class DelayedTask implements Delayed {
    private final String name;
    private final long startTime;
    
    public DelayedTask(String name, long delayMs) {
        this.name = name;
        this.startTime = System.currentTimeMillis() + delayMs;
    }
    
    @Override
    public long getDelay(TimeUnit unit) {
        long diff = startTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS),
                           o.getDelay(TimeUnit.MILLISECONDS));
    }
}

DelayQueue<DelayedTask> queue = new DelayQueue<>();
queue.put(new DelayedTask("Task1", 1000));  // Выполнится через 1 секунду
queue.put(new DelayedTask("Task2", 500));   // Выполнится через 0.5 секунды

DelayedTask task = queue.take();  // Блокируется до истечения delay
```

5. **SynchronousQueue** — очередь без внутренней ёмкости:
```java
// Каждая вставка блокируется до получения элемента другим потоком
BlockingQueue<String> queue = new SynchronousQueue<>();

// Producer блокируется в put() пока Consumer не вызовет take()
new Thread(() -> {
    try {
        queue.put("Item");  // Блокируется здесь
        System.out.println("Produced");
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}).start();

Thread.sleep(1000);  // Имитация задержки

// Consumer разблокирует Producer
String item = queue.take();
System.out.println("Consumed: " + item);
```

**Обработка InterruptedException:**
```java
try {
    queue.put(item);
} catch (InterruptedException e) {
    // Восстановить флаг прерывания
    Thread.currentThread().interrupt();
    // Логирование или другая обработка
    throw new RuntimeException("Interrupted", e);
}
```

### Сравнение реализаций Queue

| Реализация | Ограничена | Порядок | Потокобезопасна | Null | Производительность |
|------------|------------|---------|-----------------|------|-------------------|
| `ArrayDeque` | Нет | FIFO | Нет | Нет | Высокая |
| `LinkedList` | Нет | FIFO | Нет | Да | Средняя |
| `PriorityQueue` | Нет | Priority | Нет | Нет | O(log n) операции |
| `ArrayBlockingQueue` | Да | FIFO | Да | Нет | Высокая |
| `LinkedBlockingQueue` | Опц. | FIFO | Да | Нет | Средняя |
| `PriorityBlockingQueue` | Нет | Priority | Да | Нет | O(log n) операции |
| `ConcurrentLinkedQueue` | Нет | FIFO | Да | Нет | Очень высокая (lock-free) |
| `SynchronousQueue` | 0 | N/A | Да | Нет | Специфична |
| `DelayQueue` | Нет | Delay | Да | Нет | O(log n) операции |

**Рекомендации по выбору:**

**Однопоточные сценарии:**
- Stack: `ArrayDeque` (вместо `Stack`)
- Queue: `ArrayDeque` (вместо `LinkedList`)
- Priority Queue: `PriorityQueue`

**Многопоточные сценарии:**
- Producer-Consumer с ограничением: `ArrayBlockingQueue`
- Producer-Consumer без ограничения: `LinkedBlockingQueue`
- Высокая конкуренция: `ConcurrentLinkedQueue`
- Синхронизация потоков: `SynchronousQueue`
- Планирование задач: `DelayQueue` или `PriorityBlockingQueue`

## Сортировка в коллекциях

Java предоставляет богатый набор инструментов для сортировки коллекций.

### Comparable и Comparator

**Comparable** — интерфейс для определения natural ordering объектов:

```java
public interface Comparable<T> {
    int compareTo(T o);
    // Возвращает: отрицательное (this < o), 0 (this == o), положительное (this > o)
}
```

**Пример реализации Comparable:**
```java
public class Person implements Comparable<Person> {
    private String name;
    private int age;
    
    @Override
    public int compareTo(Person other) {
        // Natural ordering по возрасту
        return Integer.compare(this.age, other.age);
    }
}

List<Person> people = Arrays.asList(
    new Person("Alice", 30),
    new Person("Bob", 25),
    new Person("Charlie", 35)
);

Collections.sort(people);  // Сортировка по возрасту
```

**Comparator** — функциональный интерфейс для пользовательского сравнения:

```java
@FunctionalInterface
public interface Comparator<T> {
    int compare(T o1, T o2);
}
```

**Создание Comparator:**

1. **Анонимный класс:**
```java
Collections.sort(people, new Comparator<Person>() {
    @Override
    public int compare(Person p1, Person p2) {
        return p1.getName().compareTo(p2.getName());
    }
});
```

2. **Lambda (Java 8+):**
```java
Collections.sort(people, (p1, p2) -> p1.getName().compareTo(p2.getName()));
```

3. **Method reference:**
```java
people.sort(Comparator.comparing(Person::getName));
```

4. **Комбинирование Comparator:**
```java
// Сортировка по возрасту, затем по имени
people.sort(
    Comparator.comparing(Person::getAge)
              .thenComparing(Person::getName)
);

// Обратный порядок
people.sort(Comparator.comparing(Person::getAge).reversed());

// Null-safe сортировка
people.sort(Comparator.nullsFirst(Comparator.comparing(Person::getName)));
people.sort(Comparator.nullsLast(Comparator.comparing(Person::getName)));
```

5. **Специальные методы Comparator:**
```java
// Для int, long, double
Comparator.comparingInt(Person::getAge);
Comparator.comparingLong(Person::getId);
Comparator.comparingDouble(Person::getSalary);

// Natural order
Comparator.naturalOrder();
Comparator.reverseOrder();
```

### Алгоритмы сортировки

**Collections.sort() и Arrays.sort():**

1. **Для объектов (до Java 7):**
- Использовался Merge Sort
- Стабильная сортировка: сохраняет относительный порядок равных элементов
- Временная сложность: O(n log n) в худшем случае
- Пространственная сложность: O(n)

2. **Для объектов (Java 7+):**
- Использует TimSort (гибрид Merge Sort и Insertion Sort)
- Разработан Tim Peters для Python
- Эффективен на частично отсортированных данных
- Временная сложность: O(n log n) в худшем, O(n) на отсортированных данных

3. **Для примитивов:**
- Dual-Pivot Quicksort (Java 7+)
- Нестабильная сортировка
- Быстрее на случайных данных
- Временная сложность: O(n log n) в среднем, O(n²) в худшем

**Параллельная сортировка (Java 8+):**
```java
int[] array = new int[1_000_000];
// ... заполнение массива

// Последовательная сортировка
Arrays.sort(array);

// Параллельная сортировка (использует Fork/Join framework)
Arrays.parallelSort(array);

// Для списков параллельная сортировка через Stream
List<Integer> list = new ArrayList<>();
list = list.parallelStream()
           .sorted()
           .collect(Collectors.toList());
```

### Сортировка списков

**1. Collections.sort():**
```java
List<String> list = new ArrayList<>(Arrays.asList("Charlie", "Alice", "Bob"));

// Natural ordering
Collections.sort(list);
System.out.println(list);  // [Alice, Bob, Charlie]

// С компаратором
Collections.sort(list, Comparator.reverseOrder());
System.out.println(list);  // [Charlie, Bob, Alice]

// По длине строки
Collections.sort(list, Comparator.comparingInt(String::length));
```

**2. List.sort() (Java 8+):**
```java
List<String> list = new ArrayList<>(Arrays.asList("Charlie", "Alice", "Bob"));

// Более идиоматичный способ
list.sort(Comparator.naturalOrder());
list.sort(Comparator.comparingInt(String::length));
list.sort(null);  // Natural ordering (если элементы Comparable)
```

**3. Сортировка с сохранением исходного списка:**
```java
List<String> original = Arrays.asList("C", "A", "B");
List<String> sorted = new ArrayList<>(original);
sorted.sort(Comparator.naturalOrder());
// original: [C, A, B]
// sorted: [A, B, C]
```

**4. Частичная сортировка (топ-N элементов):**
```java
public <T> List<T> topN(List<T> list, int n, Comparator<T> comparator) {
    return list.stream()
               .sorted(comparator)
               .limit(n)
               .collect(Collectors.toList());
}

List<Integer> numbers = Arrays.asList(5, 2, 8, 1, 9, 3, 7);
List<Integer> top3 = topN(numbers, 3, Comparator.naturalOrder());
// top3: [1, 2, 3]
```

### Сортировка с помощью Stream API

**1. Базовая сортировка:**
```java
List<String> sorted = list.stream()
                          .sorted()
                          .collect(Collectors.toList());

// С компаратором
List<String> sorted = list.stream()
                          .sorted(Comparator.reverseOrder())
                          .collect(Collectors.toList());
```

**2. Сложная сортировка:**
```java
List<Person> people = // ...

// Сортировка по нескольким полям
List<Person> sorted = people.stream()
    .sorted(Comparator.comparing(Person::getLastName)
                      .thenComparing(Person::getFirstName)
                      .thenComparingInt(Person::getAge))
    .collect(Collectors.toList());

// Сортировка с фильтрацией
List<Person> adults = people.stream()
    .filter(p -> p.getAge() >= 18)
    .sorted(Comparator.comparing(Person::getAge))
    .collect(Collectors.toList());
```

**3. Сортировка Map:**
```java
Map<String, Integer> map = new HashMap<>();
map.put("Charlie", 30);
map.put("Alice", 25);
map.put("Bob", 35);

// Сортировка по ключу
Map<String, Integer> sortedByKey = map.entrySet().stream()
    .sorted(Map.Entry.comparingByKey())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new  // Сохраняет порядок
    ));

// Сортировка по значению
Map<String, Integer> sortedByValue = map.entrySet().stream()
    .sorted(Map.Entry.comparingByValue())
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
    ));

// Топ-N пар по значению
Map<String, Integer> top2 = map.entrySet().stream()
    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
    .limit(2)
    .collect(Collectors.toMap(
        Map.Entry::getKey,
        Map.Entry::getValue,
        (e1, e2) -> e1,
        LinkedHashMap::new
    ));
```

**4. Сортировка с группировкой:**
```java
List<Person> people = // ...

// Группировка по возрасту и сортировка внутри групп
Map<Integer, List<Person>> groupedAndSorted = people.stream()
    .collect(Collectors.groupingBy(
        Person::getAge,
        Collectors.collectingAndThen(
            Collectors.toList(),
            list -> list.stream()
                       .sorted(Comparator.comparing(Person::getName))
                       .collect(Collectors.toList())
        )
    ));
```

**5. Производительность:**
```java
// Для больших коллекций используйте parallelStream()
List<Integer> sorted = largeList.parallelStream()
    .sorted()
    .collect(Collectors.toList());

// Но будьте осторожны: параллелизм добавляет overhead
// Эффективен только для больших коллекций (обычно > 10_000 элементов)
```

**Рекомендации по сортировке:**
- Для примитивных типов используйте `Arrays.sort()` (DualPivot Quicksort)
- Для объектов используйте `Collections.sort()` или `List.sort()` (TimSort)
- Для параллельной сортировки больших массивов используйте `Arrays.parallelSort()`
- Для топ-N элементов используйте `PriorityQueue` или Stream с `limit()`
- Для сортировки с фильтрацией/трансформацией используйте Stream API
- Всегда проверяйте стабильность сортировки для вашего use case

## Методы для работы с коллекциями

Java предоставляет богатый набор методов для работы с коллекциями как в самих интерфейсах коллекций, так и в утилитном классе 
`Collections`. Знание этих методов критично для эффективной работы с данными и является частым предметом вопросов на собеседованиях.

### Утилитные методы класса Collections

Класс `java.util.Collections` содержит статические методы для операций над коллекциями.

#### Сортировка и поиск

```java
// Сортировка списка (требует Comparable или Comparator)
Collections.sort(list);
Collections.sort(list, comparator);

// Бинарный поиск (список должен быть отсортирован)
int index = Collections.binarySearch(list, key);
int index = Collections.binarySearch(list, key, comparator);

// Перемешивание элементов
Collections.shuffle(list);
Collections.shuffle(list, random);

// Реверс списка
Collections.reverse(list);
```

**Пример:**
```java
List<Integer> numbers = new ArrayList<>(Arrays.asList(5, 2, 8, 1, 9));
Collections.sort(numbers);  // [1, 2, 5, 8, 9]
int index = Collections.binarySearch(numbers, 5);  // 2
```

#### Модификация и заполнение

```java
// Заполнение всех элементов одним значением
Collections.fill(list, element);

// Копирование элементов из одного списка в другой
Collections.copy(dest, src);

// Замена всех вхождений одного элемента другим
Collections.replaceAll(list, oldVal, newVal);

// Циклический сдвиг элементов
Collections.rotate(list, distance);

// Обмен двух элементов
Collections.swap(list, i, j);
```

**Пример:**
```java
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
Collections.rotate(list, 2);  // [c, d, a, b]
Collections.swap(list, 0, 3);  // [b, d, a, c]
```

#### Поиск экстремумов

```java
// Максимальный и минимальный элементы
T max = Collections.max(collection);
T max = Collections.max(collection, comparator);
T min = Collections.min(collection);
T min = Collections.min(collection, comparator);

// Частота встречаемости элемента
int frequency = Collections.frequency(collection, element);
```

**Пример:**
```java
List<Integer> numbers = Arrays.asList(3, 7, 2, 9, 2, 5, 2);
int max = Collections.max(numbers);  // 9
int frequency = Collections.frequency(numbers, 2);  // 3
```

#### Создание неизменяемых и синхронизированных коллекций

```java
// Неизменяемые обёртки (read-only view)
List<T> unmodifiableList = Collections.unmodifiableList(list);
Set<T> unmodifiableSet = Collections.unmodifiableSet(set);
Map<K,V> unmodifiableMap = Collections.unmodifiableMap(map);

// Синхронизированные обёртки (потокобезопасные)
List<T> syncList = Collections.synchronizedList(list);
Set<T> syncSet = Collections.synchronizedSet(set);
Map<K,V> syncMap = Collections.synchronizedMap(map);

// Проверенные коллекции (type-safe в runtime)
List<String> checkedList = Collections.checkedList(list, String.class);
```

> **Важно**: Неизменяемые обёртки не создают копию коллекции, а лишь запрещают модификацию через wrapper. 
> Если исходная коллекция изменяется, изменения видны через неизменяемую обёртку.

**Пример потокобезопасности:**
```java
List<String> list = new ArrayList<>();
List<String> syncList = Collections.synchronizedList(list);

// Итерация требует ручной синхронизации
synchronized(syncList) {
    for (String item : syncList) {
        System.out.println(item);
    }
}
```

#### Специальные коллекции

```java
// Пустые неизменяемые коллекции (singleton instances)
List<T> emptyList = Collections.emptyList();
Set<T> emptySet = Collections.emptySet();
Map<K,V> emptyMap = Collections.emptyMap();

// Singleton коллекции (один элемент, неизменяемые)
Set<T> singleton = Collections.singleton(element);
List<T> singletonList = Collections.singletonList(element);
Map<K,V> singletonMap = Collections.singletonMap(key, value);

// N копий одного элемента (неизменяемый список)
List<T> nCopies = Collections.nCopies(n, element);
```

**Пример:**
```java
// Эффективная инициализация с повторяющимися значениями
List<String> tenZeros = Collections.nCopies(10, "0");
// ["0", "0", "0", "0", "0", "0", "0", "0", "0", "0"]
```

#### Проверка несвязанности коллекций

```java
// Проверка, что две коллекции не имеют общих элементов
boolean disjoint = Collections.disjoint(collection1, collection2);
```

**Пример:**
```java
Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
Set<Integer> set2 = new HashSet<>(Arrays.asList(4, 5, 6));
boolean noCommon = Collections.disjoint(set1, set2);  // true
```

### Методы интерфейса Collection

Базовый интерфейс `Collection<E>` определяет общие операции для всех коллекций (кроме Map).

#### Добавление и удаление элементов

```java
// Добавление одного элемента
boolean add(E element);

// Добавление всех элементов из другой коллекции
boolean addAll(Collection<? extends E> c);

// Удаление одного элемента
boolean remove(Object element);

// Удаление всех элементов из коллекции c
boolean removeAll(Collection<?> c);

// Удаление элементов, удовлетворяющих условию (Java 8+)
boolean removeIf(Predicate<? super E> filter);

// Сохранение только элементов, присутствующих в коллекции c
boolean retainAll(Collection<?> c);

// Удаление всех элементов
void clear();
```

**Примеры:**
```java
List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6));

// Удаление чётных чисел
numbers.removeIf(n -> n % 2 == 0);  // [1, 3, 5]

// Сохранение только элементов из другого списка
List<Integer> keep = Arrays.asList(1, 3, 7);
numbers.retainAll(keep);  // [1, 3]
```

#### Проверка содержимого

```java
// Проверка наличия элемента
boolean contains(Object element);

// Проверка наличия всех элементов
boolean containsAll(Collection<?> c);

// Проверка на пустоту
boolean isEmpty();

// Размер коллекции
int size();
```

#### Преобразование в массив

```java
// Массив Object[]
Object[] toArray();

// Типизированный массив
<T> T[] toArray(T[] array);

// Java 11+: функциональная версия
<T> T[] toArray(IntFunction<T[]> generator);
```

**Примеры:**
```java
List<String> list = Arrays.asList("A", "B", "C");

// Старый способ
String[] array1 = list.toArray(new String[0]);

// Java 11+: более идиоматичный
String[] array2 = list.toArray(String[]::new);
```

#### Итерация и потоковая обработка

```java
// Итератор
Iterator<E> iterator();

// forEach с лямбдой (Java 8+)
void forEach(Consumer<? super E> action);

// Получение стрима (Java 8+)
Stream<E> stream();
Stream<E> parallelStream();
```

**Пример:**
```java
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

// forEach
names.forEach(name -> System.out.println(name));

// Stream API
names.stream()
     .filter(name -> name.startsWith("A"))
     .map(String::toUpperCase)
     .forEach(System.out::println);
```

### Методы интерфейса List

Интерфейс `List<E>` расширяет `Collection<E>` и добавляет методы для работы с индексированным доступом.

#### Доступ по индексу

```java
// Получение элемента по индексу
E get(int index);

// Установка элемента по индексу (возвращает старое значение)
E set(int index, E element);

// Добавление элемента по индексу
void add(int index, E element);

// Добавление коллекции элементов начиная с индекса
boolean addAll(int index, Collection<? extends E> c);

// Удаление элемента по индексу (возвращает удалённый элемент)
E remove(int index);
```

**Пример:**
```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "D"));
list.add(2, "C");  // ["A", "B", "C", "D"]
String removed = list.set(0, "Z");  // ["Z", "B", "C", "D"], removed = "A"
```

#### Поиск элементов

```java
// Индекс первого вхождения элемента (-1 если не найден)
int indexOf(Object element);

// Индекс последнего вхождения элемента
int lastIndexOf(Object element);
```

**Пример:**
```java
List<String> list = Arrays.asList("A", "B", "A", "C", "A");
int first = list.indexOf("A");      // 0
int last = list.lastIndexOf("A");   // 4
```

#### Работа с подсписками

```java
// Создание view на подсписок [fromIndex, toIndex)
List<E> subList(int fromIndex, int toIndex);
```

> **Важно**: `subList()` возвращает **view**, а не копию. Изменения в подсписке отражаются на исходном списке и наоборот.

**Пример:**
```java
List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
List<Integer> subList = numbers.subList(1, 4);  // [2, 3, 4]

subList.set(0, 10);  // Изменяет исходный список
// numbers: [1, 10, 3, 4, 5]

subList.clear();  // Удаляет элементы из исходного списка
// numbers: [1, 5]
```

#### Специализированные методы (Java 8+)

```java
// Замена всех элементов результатом применения функции
void replaceAll(UnaryOperator<E> operator);

// Сортировка с использованием компаратора
void sort(Comparator<? super E> c);
```

**Пример:**
```java
List<String> words = new ArrayList<>(Arrays.asList("hello", "world", "java"));
words.replaceAll(String::toUpperCase);  // ["HELLO", "WORLD", "JAVA"]
words.sort(String::compareTo);  // ["HELLO", "JAVA", "WORLD"]
```

#### Итерация в обратном порядке

```java
// ListIterator поддерживает двустороннюю итерацию
ListIterator<E> listIterator();
ListIterator<E> listIterator(int index);
```

**Пример:**
```java
List<String> list = Arrays.asList("A", "B", "C");
ListIterator<String> iter = list.listIterator(list.size());

while (iter.hasPrevious()) {
    System.out.println(iter.previous());  // C, B, A
}
```

### Методы интерфейса Set

Интерфейс `Set<E>` наследует методы `Collection<E>`, но не добавляет специфических методов на уровне базового интерфейса. 
Однако подинтерфейсы `SortedSet` и `NavigableSet` предоставляют дополнительные возможности.

#### Методы SortedSet

```java
// Первый (наименьший) элемент
E first();

// Последний (наибольший) элемент
E last();

// Подмножество [fromElement, toElement)
SortedSet<E> subSet(E fromElement, E toElement);

// Элементы строго меньше toElement
SortedSet<E> headSet(E toElement);

// Элементы >= fromElement
SortedSet<E> tailSet(E fromElement);

// Компаратор (null если natural ordering)
Comparator<? super E> comparator();
```

#### Методы NavigableSet

```java
// Наименьший элемент >= заданного (или null)
E ceiling(E element);

// Наибольший элемент <= заданного
E floor(E element);

// Наименьший элемент > заданного
E higher(E element);

// Наибольший элемент < заданного
E lower(E element);

// Удаление и возврат первого элемента
E pollFirst();

// Удаление и возврат последнего элемента
E pollLast();

// Обратный view на множество
NavigableSet<E> descendingSet();

// Итератор в обратном порядке
Iterator<E> descendingIterator();
```

**Пример с TreeSet:**
```java
NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 3, 5, 7, 9));

int ceiling = set.ceiling(4);   // 5 (наименьший >= 4)
int floor = set.floor(4);       // 3 (наибольший <= 4)
int higher = set.higher(5);     // 7 (наименьший > 5)
int lower = set.lower(5);       // 3 (наибольший < 5)

// Обратная итерация
set.descendingSet().forEach(System.out::println);  // 9, 7, 5, 3, 1
```

### Методы интерфейса Map

Интерфейс `Map<K,V>` не является частью иерархии `Collection`, но предоставляет свои методы для работы с парами ключ-значение.

#### Базовые операции

```java
// Добавление пары ключ-значение (возвращает предыдущее значение или null)
V put(K key, V value);

// Добавление всех пар из другой Map
void putAll(Map<? extends K, ? extends V> m);

// Получение значения по ключу (null если нет)
V get(Object key);

// Удаление по ключу (возвращает удалённое значение)
V remove(Object key);

// Проверка наличия ключа
boolean containsKey(Object key);

// Проверка наличия значения
boolean containsValue(Object value);

// Размер карты
int size();

// Проверка на пустоту
boolean isEmpty();

// Удаление всех элементов
void clear();
```

#### Методы для получения представлений (Java 8+)

```java
// Множество ключей (view)
Set<K> keySet();

// Коллекция значений (view)
Collection<V> values();

// Множество пар ключ-значение (view)
Set<Map.Entry<K,V>> entrySet();
```

> **Важно**: Эти методы возвращают **view** на Map. Изменения в view отражаются на исходной Map и наоборот.

**Пример:**
```java
Map<String, Integer> map = new HashMap<>();
map.put("A", 1);
map.put("B", 2);

// Удаление через keySet
map.keySet().remove("A");  // Удаляет пару из Map

// Итерация через entrySet
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
```

#### Атомарные операции (Java 8+)

```java
// Добавить, если ключа нет (возвращает текущее значение)
V putIfAbsent(K key, V value);

// Удалить только если ключ связан с указанным значением
boolean remove(Object key, Object value);

// Заменить, если ключ присутствует
V replace(K key, V value);

// Заменить только если текущее значение совпадает
boolean replace(K key, V oldValue, V newValue);

// Получить значение или вернуть defaultValue
V getOrDefault(Object key, V defaultValue);
```

**Примеры:**
```java
Map<String, Integer> map = new HashMap<>();
map.put("count", 1);

// Безопасное получение с дефолтом
int value = map.getOrDefault("missing", 0);  // 0

// Добавление только если отсутствует
map.putIfAbsent("count", 10);  // Не изменит, вернёт 1
map.putIfAbsent("new", 10);    // Добавит новую пару

// Условная замена
map.replace("count", 1, 2);  // true, изменит на 2
map.replace("count", 1, 3);  // false, не изменит (текущее значение 2)
```

#### Функциональные операции (Java 8+)

```java
// Применить функцию к каждой паре
void forEach(BiConsumer<? super K, ? super V> action);

// Заменить все значения результатом функции
void replaceAll(BiFunction<? super K, ? super V, ? extends V> function);

// Вычислить значение, если ключа нет
V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction);

// Вычислить новое значение, если ключ присутствует
V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

// Вычислить новое значение (независимо от наличия ключа)
V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction);

// Слияние значений при наличии конфликта
V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction);
```

**Примеры практического применения:**

**1. Подсчёт частоты слов:**
```java
Map<String, Integer> wordCount = new HashMap<>();
String[] words = {"apple", "banana", "apple", "cherry", "banana", "apple"};

for (String word : words) {
    wordCount.merge(word, 1, Integer::sum);
}
// {apple=3, banana=2, cherry=1}
```

**2. Группировка с computeIfAbsent:**
```java
Map<String, List<String>> grouping = new HashMap<>();
grouping.computeIfAbsent("fruits", k -> new ArrayList<>()).add("apple");
grouping.computeIfAbsent("fruits", k -> new ArrayList<>()).add("banana");
// {fruits=[apple, banana]}
```

**3. Обновление значений с replaceAll:**
```java
Map<String, Integer> prices = new HashMap<>();
prices.put("apple", 100);
prices.put("banana", 50);

// Повышение всех цен на 10%
prices.replaceAll((k, v) -> (int)(v * 1.1));
// {apple=110, banana=55}
```

**4. Условное удаление с compute:**
```java
Map<String, Integer> inventory = new HashMap<>();
inventory.put("apple", 5);

// Уменьшить количество на 1, удалить если стало 0
inventory.compute("apple", (k, v) -> v == null || v <= 1 ? null : v - 1);
```

#### Методы SortedMap и NavigableMap

Для `TreeMap` доступны дополнительные методы:

```java
// SortedMap методы (аналогично SortedSet)
K firstKey();
K lastKey();
SortedMap<K,V> subMap(K fromKey, K toKey);
SortedMap<K,V> headMap(K toKey);
SortedMap<K,V> tailMap(K fromKey);

// NavigableMap методы (аналогично NavigableSet)
Map.Entry<K,V> ceilingEntry(K key);
Map.Entry<K,V> floorEntry(K key);
Map.Entry<K,V> higherEntry(K key);
Map.Entry<K,V> lowerEntry(K key);
K ceilingKey(K key);
K floorKey(K key);
K higherKey(K key);
K lowerKey(K key);
Map.Entry<K,V> pollFirstEntry();
Map.Entry<K,V> pollLastEntry();
NavigableMap<K,V> descendingMap();
```

**Пример:**
```java
NavigableMap<Integer, String> map = new TreeMap<>();
map.put(1, "one");
map.put(3, "three");
map.put(5, "five");
map.put(7, "seven");

Map.Entry<Integer, String> entry = map.ceilingEntry(4);  // (5, "five")
Integer key = map.lowerKey(5);  // 3
```

### Фабричные методы для создания коллекций

Java 9+ предоставляет удобные фабричные методы для создания неизменяемых коллекций.

#### List.of() (Java 9+)

```java
// Создание неизменяемого списка
List<String> list = List.of("A", "B", "C");

// Пустой неизменяемый список
List<String> empty = List.of();

// List.copyOf() создаёт неизменяемую копию
List<String> copy = List.copyOf(mutableList);
```

> **Важно**: `List.of()` не допускает `null` элементы и бросает `NullPointerException` при попытке их добавления.

#### Set.of() (Java 9+)

```java
// Создание неизменяемого множества
Set<Integer> set = Set.of(1, 2, 3, 4, 5);

// Set.of() бросает исключение при дубликатах
Set<Integer> invalid = Set.of(1, 2, 2);  // IllegalArgumentException

// Set.copyOf() создаёт неизменяемую копию
Set<Integer> copy = Set.copyOf(mutableSet);
```

#### Map.of() и Map.ofEntries() (Java 9+)

```java
// Для небольших Map (до 10 пар)
Map<String, Integer> map = Map.of(
    "A", 1,
    "B", 2,
    "C", 3
);

// Для больших Map используйте Map.ofEntries()
Map<String, Integer> largeMap = Map.ofEntries(
    Map.entry("A", 1),
    Map.entry("B", 2),
    Map.entry("C", 3),
    Map.entry("D", 4)
    // ... можно добавить сколько угодно пар
);

// Map.copyOf() создаёт неизменяемую копию
Map<String, Integer> copy = Map.copyOf(mutableMap);
```

**Пример использования:**
```java
// Старый способ (до Java 9)
Map<String, Integer> oldWay = new HashMap<>();
oldWay.put("A", 1);
oldWay.put("B", 2);
Map<String, Integer> unmodifiable = Collections.unmodifiableMap(oldWay);

// Новый способ (Java 9+)
Map<String, Integer> newWay = Map.of("A", 1, "B", 2);
```

#### Сравнение подходов создания неизменяемых коллекций

| Подход | Преимущества | Недостатки |
|--------|-------------|-----------|
| `Collections.unmodifiableXxx()` | Работает во всех версиях Java, создаёт view | Исходная коллекция может быть изменена, более многословный |
| `List.of()`, `Set.of()`, `Map.of()` | Компактный синтаксис, истинно неизменяемые | Только Java 9+, не допускает null |
| `Arrays.asList()` | Простое создание списка из элементов | Список изменяемый (можно вызывать set), фиксированного размера |
| `Stream.collect(toUnmodifiableList())` | Удобно при работе со Stream API | Java 10+, более многословный для простых случаев |

**Рекомендации по выбору:**
- Для новых проектов на Java 9+ используйте `List.of()`, `Set.of()`, `Map.of()`
- Для совместимости со старыми версиями используйте `Collections.unmodifiableXxx()`
- Для создания изменяемых коллекций с начальными значениями используйте конструкторы или `new ArrayList<>(Arrays.asList(...))`

## Контракты equals и hashCode

Методы `equals()` и `hashCode()` являются фундаментальными для работы с коллекциями на основе хеширования (`HashSet`, `HashMap`, 
`Hashtable`, `LinkedHashSet`, `LinkedHashMap`) и определяют, как объекты сравниваются и размещаются в этих структурах данных. 
Неправильная реализация этих методов приводит к непредсказуемому поведению коллекций: дубликаты в `Set`, невозможность найти 
объект в `Map`, утечки памяти и нарушение бизнес-логики.

### Контракт equals

Метод `equals(Object obj)` определён в классе `Object` и по умолчанию сравнивает ссылки (аналогично `==`). При переопределении 
необходимо соблюдать **пять обязательных свойств**, описанных в спецификации Java:

#### 1. Рефлексивность (Reflexive)
Объект всегда должен быть равен самому себе:
```java
x.equals(x) == true  // для любого ненулевого x
```

#### 2. Симметричность (Symmetric)
Если `x` равен `y`, то `y` должен быть равен `x`:
```java
x.equals(y) == y.equals(x)  // для любых ненулевых x и y
```

> **Важно**: Нарушение симметричности часто возникает при сравнении объектов разных классов или при некорректной работе с 
> наследованием.

#### 3. Транзитивность (Transitive)
Если `x` равен `y`, и `y` равен `z`, то `x` должен быть равен `z`:
```java
if (x.equals(y) && y.equals(z)) {
    x.equals(z) == true
}
```

#### 4. Согласованность (Consistent)
Множественные вызовы `equals()` должны возвращать одинаковый результат, если объекты не изменялись:
```java
x.equals(y) == x.equals(y)  // при повторном вызове
```

> **Важно**: Не используйте изменяемые или недетерминированные поля (например, `Random`, текущее время) в реализации `equals()`.

#### 5. Сравнение с null
Любой ненулевой объект не должен быть равен `null`:
```java
x.equals(null) == false  // для любого ненулевого x
```

### Контракт hashCode

Метод `hashCode()` возвращает целочисленное значение, используемое для размещения объектов в хеш-таблицах. Контракт `hashCode()` 
жёстко связан с `equals()` и определяет три обязательных правила:

#### 1. Согласованность
При многократном вызове `hashCode()` для неизменённого объекта должно возвращаться одно и то же значение:
```java
int hash1 = obj.hashCode();
int hash2 = obj.hashCode();
// hash1 == hash2, если obj не изменялся
```

#### 2. Равные объекты должны иметь одинаковый hash-код
Это **критическое правило**:
```java
if (x.equals(y)) {
    x.hashCode() == y.hashCode()  // ОБЯЗАТЕЛЬНО
}
```

> **Важно**: Нарушение этого правила приводит к тому, что равные объекты попадают в разные бакеты хеш-таблицы, и коллекция 
> не может их найти.

#### 3. Разные объекты могут иметь одинаковый hash-код (но желательно разный)
Обратное не обязательно — неравные объекты **могут** иметь одинаковый `hashCode()`:
```java
if (x.hashCode() == y.hashCode()) {
    x.equals(y)  // не обязательно true
}
```

Однако для эффективности желательно минимизировать **коллизии** (ситуации, когда разные объекты имеют одинаковый hash-код). 
Большое количество коллизий деградирует производительность `HashMap`/`HashSet` до O(n).

### Связь equals и hashCode в коллекциях

#### HashMap и HashSet

`HashMap` использует `hashCode()` для определения индекса бакета:
```java
int bucket = (hash & (table.length - 1));
```

Внутри бакета элементы сравниваются через `equals()`. Процесс добавления элемента:

1. Вычисляется `hashCode()` ключа
2. Определяется бакет на основе hash-кода
3. Если бакет пустой — элемент добавляется
4. Если бакет не пустой — перебираются элементы и сравниваются через `equals()`
5. Если найден равный элемент — значение заменяется (для `Map`) или элемент отклоняется (для `Set`)
6. Если равного нет — элемент добавляется в цепочку/дерево

**Пример проблемы при нарушении контракта:**
```java
class BadKey {
    private int value;
    
    public BadKey(int value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BadKey)) return false;
        return this.value == ((BadKey) obj).value;
    }
    
    // hashCode() НЕ переопределён! Используется дефолтная реализация из Object
}

Map<BadKey, String> map = new HashMap<>();
map.put(new BadKey(1), "one");
String result = map.get(new BadKey(1));  // result == null!
```

Даже если два объекта `BadKey` с `value=1` равны по `equals()`, они имеют разные hash-коды и попадают в разные бакеты. 
Метод `get()` не найдёт значение.

#### TreeSet и TreeMap

`TreeSet` и `TreeMap` не используют `hashCode()`, полагаясь на `Comparator` или `Comparable`. Однако важно, чтобы реализация 
`compareTo()` была **согласована с `equals()`**:
```java
// Правильно: если compareTo возвращает 0, то equals возвращает true
(x.compareTo(y) == 0) == x.equals(y)
```

Нарушение этого правила приводит к противоречивому поведению при переносе объектов между разными типами коллекций.

### Правильная реализация

#### Шаблон реализации equals

```java
public class Person {
    private final String name;
    private final int age;
    private final String email;
    
    @Override
    public boolean equals(Object obj) {
        // 1. Проверка на идентичность (оптимизация)
        if (this == obj) {
            return true;
        }
        
        // 2. Проверка на null и тип
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        
        // 3. Приведение типа
        Person other = (Person) obj;
        
        // 4. Сравнение значимых полей
        return age == other.age &&
               Objects.equals(name, other.name) &&
               Objects.equals(email, other.email);
    }
}
```

> **Совет**: Используйте `Objects.equals()` для сравнения полей-ссылок, чтобы корректно обрабатывать `null`.

#### Шаблон реализации hashCode

```java
@Override
public int hashCode() {
    return Objects.hash(name, age, email);
}
```

Метод `Objects.hash()` (Java 7+) автоматически вычисляет hash-код на основе переданных полей. Эквивалентная ручная реализация:

```java
@Override
public int hashCode() {
    int result = 17;  // Начальное простое число
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + age;
    result = 31 * result + (email != null ? email.hashCode() : 0);
    return result;
}
```

> **Почему 31?** Это простое число, достаточно большое для минимизации коллизий, но достаточно маленькое, чтобы избежать 
> переполнения. Кроме того, умножение на 31 оптимизируется JVM как `(i << 5) - i`.

#### Использование IDE или утилит

Современные IDE (IntelliJ IDEA, Eclipse) и библиотеки (Lombok, Apache Commons) могут автоматически генерировать `equals()` 
и `hashCode()`:

```java
// Lombok
@EqualsAndHashCode
public class Person {
    private final String name;
    private final int age;
}

// Java 16+ Record (автоматически генерирует equals, hashCode, toString)
public record Person(String name, int age) {}
```

### Распространённые ошибки

#### 1. Переопределение только equals без hashCode

```java
class WrongPerson {
    private String name;
    
    @Override
    public boolean equals(Object obj) {
        // ... корректная реализация
    }
    
    // hashCode() НЕ переопределён!
}

Set<WrongPerson> set = new HashSet<>();
set.add(new WrongPerson("Alice"));
set.add(new WrongPerson("Alice"));  // Добавится второй раз!
System.out.println(set.size());  // 2 вместо ожидаемого 1
```

#### 2. Использование изменяемых полей

```java
class MutableKey {
    private String value;  // изменяемое поле!
    
    public void setValue(String value) {
        this.value = value;
    }
    
    @Override
    public boolean equals(Object obj) {
        return value.equals(((MutableKey) obj).value);
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
}

Map<MutableKey, String> map = new HashMap<>();
MutableKey key = new MutableKey();
key.setValue("original");
map.put(key, "data");

key.setValue("modified");  // Изменение ключа!
String result = map.get(key);  // null! Ключ попал в другой бакет
```

> **Совет**: Используйте только **неизменяемые поля** в реализации `equals()` и `hashCode()`. Для ключей `Map` и элементов 
> `Set` делайте объекты полностью иммутабельными (final поля, отсутствие setter-ов).

#### 3. Несимметричность при работе с наследованием

```java
class Point {
    private int x, y;
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point)) return false;
        Point p = (Point) obj;
        return x == p.x && y == p.y;
    }
}

class ColorPoint extends Point {
    private String color;
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ColorPoint)) return false;
        ColorPoint cp = (ColorPoint) obj;
        return super.equals(cp) && color.equals(cp.color);
    }
}

Point p = new Point(1, 2);
ColorPoint cp = new ColorPoint(1, 2, "red");

p.equals(cp);   // true (Point игнорирует color)
cp.equals(p);   // false (ColorPoint требует color)
// Нарушение симметричности!
```

**Решение**: Используйте композицию вместо наследования для изменяемой логики `equals()`, или сравнивайте `getClass()` 
вместо `instanceof`:

```java
@Override
public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) return false;
    // ...
}
```

#### 4. Игнорирование null в полях

```java
class BadEquals {
    private String name;
    
    @Override
    public boolean equals(Object obj) {
        BadEquals other = (BadEquals) obj;
        return name.equals(other.name);  // NullPointerException если name == null!
    }
}
```

**Решение**: Используйте `Objects.equals()`:
```java
return Objects.equals(name, other.name);
```

#### 5. Использование недетерминированных значений

```java
class WrongHash {
    @Override
    public int hashCode() {
        return new Random().nextInt();  // НЕПРАВИЛЬНО! Каждый раз новое значение
    }
}
```

## Generics и типобезопасность

Generics (обобщения) добавлены в Java 5 для обеспечения типобезопасности на этапе компиляции и устранения необходимости 
явного приведения типов.

### Основы Generics

**До Generics (Java 1.4 и ранее):**
```java
List list = new ArrayList();
list.add("String");
list.add(42);  // Можно добавить любой тип!

String s = (String) list.get(0);  // Явное приведение
Integer i = (Integer) list.get(1);  // Runtime ошибка если тип не совпадает
```

**С Generics (Java 5+):**
```java
List<String> list = new ArrayList<>();
list.add("String");
list.add(42);  // Ошибка компиляции!

String s = list.get(0);  // Приведение не требуется
```

**Преимущества Generics:**
1. **Типобезопасность**: Ошибки типов обнаруживаются на этапе компиляции
2. **Устранение приведений**: Не нужны явные casts
3. **Переиспользование кода**: Один класс/метод работает с разными типами
4. **Документация**: Типовые параметры делают код самодокументируемым

### Type Erasure (Стирание типов)

Generics в Java реализованы через **type erasure** — механизм, при котором информация о типе-параметре стирается во время компиляции.

**Процесс type erasure:**

1. **Замена параметров типа:**
```java
// Исходный код
public class Box<T> {
    private T value;
    public T getValue() { return value; }
}

// После компиляции (type erasure)
public class Box {
    private Object value;
    public Object getValue() { return value; }
}
```

2. **С ограниченными параметрами:**
```java
// Исходный код
public class Box<T extends Number> {
    private T value;
    public T getValue() { return value; }
}

// После компиляции
public class Box {
    private Number value;
    public Number getValue() { return value; }
}
```

3. **Вставка приведений типов:**
```java
// Исходный код
Box<Integer> box = new Box<>();
Integer value = box.getValue();

// После компиляции
Box box = new Box();
Integer value = (Integer) box.getValue();  // Автоматическая вставка cast
```

**Последствия type erasure:**

1. **Невозможно создать массив обобщённого типа:**
```java
// Ошибка компиляции
T[] array = new T[10];
List<String>[] lists = new ArrayList<String>[10];

// Обходной путь
@SuppressWarnings("unchecked")
T[] array = (T[]) new Object[10];

// Или используйте коллекции
List<T> list = new ArrayList<>();
```

2. **Невозможно использовать instanceof с параметризованными типами:**
```java
// Ошибка компиляции
if (obj instanceof List<String>) { }

// Правильно
if (obj instanceof List) { }
```

3. **Невозможно создать экземпляр параметра типа:**
```java
// Ошибка компиляции
T instance = new T();

// Обходной путь через Class
public class Factory<T> {
    private final Class<T> type;
    
    public Factory(Class<T> type) {
        this.type = type;
    }
    
    public T create() throws Exception {
        return type.getDeclaredConstructor().newInstance();
    }
}

Factory<String> factory = new Factory<>(String.class);
String s = factory.create();
```

4. **Статические члены не могут использовать параметры типа класса:**
```java
public class Box<T> {
    // Ошибка компиляции
    private static T value;
    
    // Ошибка компиляции
    public static T getValue() { }
}
```

**Причина type erasure:**
- Обратная совместимость с кодом до Java 5
- Отсутствие необходимости изменять JVM
- Сохранение совместимости байт-кода

### Bounded Type Parameters (Ограниченные параметры типа)

Параметры типа можно ограничивать для доступа к методам суперкласса.

**Upper bound (? extends):**
```java
// T должен быть подтипом Number
public class NumberBox<T extends Number> {
    private T value;
    
    public double doubleValue() {
        return value.doubleValue();  // Можем вызывать методы Number
    }
}

NumberBox<Integer> intBox = new NumberBox<>();  // OK
NumberBox<Double> doubleBox = new NumberBox<>();  // OK
NumberBox<String> stringBox = new NumberBox<>();  // Ошибка компиляции
```

**Множественные границы:**
```java
// T должен реализовывать Comparable и Serializable
public class SortableBox<T extends Comparable<T> & Serializable> {
    private T value;
    
    public int compareTo(T other) {
        return value.compareTo(other);
    }
}
```

> **Важно**: Первым в списке должен быть класс (если есть), затем интерфейсы.

```java
// Правильно
<T extends Number & Comparable<T>>

// Неправильно
<T extends Comparable<T> & Number>  // Ошибка если Number не интерфейс
```

### Wildcards (Подстановочные символы)

Wildcards позволяют работать с неизвестными типами и обеспечивают гибкость при работе с обобщёнными типами.

#### Unbounded Wildcard (?)

```java
// Принимает список любого типа
public void printList(List<?> list) {
    for (Object elem : list) {
        System.out.println(elem);
    }
}

printList(new ArrayList<Integer>());
printList(new ArrayList<String>());
```

**Ограничения unbounded wildcard:**
```java
List<?> list = new ArrayList<String>();
Object obj = list.get(0);  // OK: читаем как Object
list.add("String");  // Ошибка компиляции! Не можем добавлять
list.add(null);  // OK: null можно добавить в любой тип
```

#### Upper Bounded Wildcard (? extends T)

**Producer Extends**: Коллекция предоставляет (produces) элементы определённого типа или его подтипов.

```java
// Принимает List<Number> или List<Integer> или List<Double> и т.д.
public double sumOfList(List<? extends Number> list) {
    double sum = 0.0;
    for (Number num : list) {
        sum += num.doubleValue();  // Читаем как Number
    }
    return sum;
}

List<Integer> integers = Arrays.asList(1, 2, 3);
List<Double> doubles = Arrays.asList(1.0, 2.0, 3.0);

sumOfList(integers);  // OK
sumOfList(doubles);   // OK
```

**Ковариантность для чтения:**
```java
List<? extends Number> list = new ArrayList<Integer>();
Number num = list.get(0);  // OK: читаем как Number
list.add(42);  // Ошибка компиляции! Не можем добавлять
list.add(new Integer(42));  // Ошибка компиляции!
list.add(null);  // OK
```

**Почему нельзя добавлять?**
```java
List<? extends Number> list = new ArrayList<Integer>();
list.add(new Double(3.14));  // Если бы было можно, Integer список содержал бы Double!
```

#### Lower Bounded Wildcard (? super T)

**Consumer Super**: Коллекция принимает (consumes) элементы определённого типа или его супертипов.

```java
// Принимает List<Integer> или List<Number> или List<Object>
public void addIntegers(List<? super Integer> list) {
    list.add(42);        // OK: добавляем Integer
    list.add(100);       // OK
    Object obj = list.get(0);  // Читаем только как Object
}

List<Integer> integers = new ArrayList<>();
List<Number> numbers = new ArrayList<>();
List<Object> objects = new ArrayList<>();

addIntegers(integers);  // OK
addIntegers(numbers);   // OK
addIntegers(objects);   // OK
```

**Контравариантность для записи:**
```java
List<? super Integer> list = new ArrayList<Number>();
list.add(42);  // OK: добавляем Integer
list.add(new Integer(100));  // OK
Integer i = list.get(0);  // Ошибка компиляции!
Object obj = list.get(0);  // OK: читаем только как Object
```

### PECS (Producer Extends, Consumer Super)

**Правило PECS** — мнемоника для выбора между `extends` и `super`:

- **Producer Extends**: Если метод читает (produces) элементы из коллекции → используйте `? extends T`
- **Consumer Super**: Если метод записывает (consumes) элементы в коллекцию → используйте `? super T`

**Примеры из стандартной библиотеки:**

```java
// Collections.copy - источник produces, назначение consumes
public static <T> void copy(
    List<? super T> dest,      // Consumer: принимает элементы
    List<? extends T> src      // Producer: предоставляет элементы
) { ... }

// Collections.max - коллекция produces элементы для сравнения
public static <T extends Object & Comparable<? super T>> T max(
    Collection<? extends T> coll  // Producer: предоставляет элементы
) { ... }
```

**Практический пример:**
```java
public class Collections {
    // Копирование из source в destination
    public static <T> void copy(List<? super T> dest, List<? extends T> src) {
        for (int i = 0; i < src.size(); i++) {
            dest.set(i, src.get(i));
        }
    }
}

List<Integer> integers = Arrays.asList(1, 2, 3);
List<Number> numbers = new ArrayList<>();

// integers является producer (? extends Number)
// numbers является consumer (? super Integer)
Collections.copy(numbers, integers);
```

**Таблица применения PECS:**

| Сценарий | Wildcard | Пример |
|----------|----------|--------|
| Только чтение из коллекции | `? extends T` | `List<? extends Number>` |
| Только запись в коллекцию | `? super T` | `List<? super Integer>` |
| Чтение и запись | Точный тип `T` | `List<Integer>` |
| Неизвестный тип | `?` | `List<?>` |

### Generic Methods (Обобщённые методы)

Методы могут иметь собственные параметры типа, независимые от параметров класса.

**Синтаксис:**
```java
public class Utils {
    // Обобщённый метод
    public static <T> T getMiddle(T... args) {
        return args[args.length / 2];
    }
}

// Использование
String middle = Utils.<String>getMiddle("A", "B", "C");  // Явное указание типа
String middle2 = Utils.getMiddle("A", "B", "C");  // Type inference (вывод типа)
```

**С ограничениями:**
```java
public static <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;
}

Integer maxInt = max(10, 20);  // 20
String maxStr = max("Alice", "Bob");  // "Bob"
```

**Несколько параметров типа:**
```java
public static <K, V> Map<K, V> newHashMap() {
    return new HashMap<>();
}

public static <T, U> Pair<T, U> makePair(T first, U second) {
    return new Pair<>(first, second);
}

Pair<String, Integer> pair = makePair("Age", 30);
```

### Ограничения Generics

1. **Примитивные типы нельзя использовать:**
```java
List<int> list;  // Ошибка компиляции
List<Integer> list;  // Правильно (используйте wrapper)
```

2. **Невозможно создавать массивы параметризованных типов:**
```java
List<String>[] arrays = new ArrayList<String>[10];  // Ошибка компиляции

// Обходной путь
@SuppressWarnings("unchecked")
List<String>[] arrays = (List<String>[]) new ArrayList[10];
```

3. **Невозможно использовать static контекст с параметрами типа класса:**
```java
public class Box<T> {
    private static T instance;  // Ошибка компиляции
}
```

4. **Невозможно использовать instanceof с конкретным параметром:**
```java
if (obj instanceof List<String>) { }  // Ошибка компиляции
if (obj instanceof List<?>) { }  // OK
```

5. **Перегрузка может быть ограничена из-за type erasure:**
```java
public class Overload {
    public void method(List<String> list) { }
    public void method(List<Integer> list) { }  // Ошибка! После erasure оба метода идентичны
}
```

### Best Practices для Generics

1. **Используйте generics везде, где возможно:**
```java
// Плохо
List list = new ArrayList();

// Хорошо
List<String> list = new ArrayList<>();
```

2. **Применяйте PECS:**
```java
// Метод читает из коллекции
public void processNumbers(List<? extends Number> numbers) { }

// Метод пишет в коллекцию
public void addNumbers(List<? super Integer> list) { }
```

3. **Предпочитайте List<T> вместо T[]:**
```java
// Избегайте
public <T> T[] toArray(Collection<T> collection) { }

// Предпочитайте
public <T> List<T> toList(Collection<T> collection) { }
```

4. **Ограничивайте параметры типа, если нужен доступ к методам:**
```java
public <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;
}
```

5. **Используйте @SuppressWarnings("unchecked") с осторожностью:**
```java
@SuppressWarnings("unchecked")
public <T> T[] toArray(Class<T> type, List<T> list) {
    T[] array = (T[]) Array.newInstance(type, list.size());
    return list.toArray(array);
}
```

6. **Документируйте параметры типа:**
```java
/**
 * @param <K> тип ключа
 * @param <V> тип значения
 */
public class Cache<K, V> {
    // ...
}
```

## Специализированные коллекции
- **Immutable Collections**: `List.of`, `Set.of`, `Map.of` — неизменяемые структуры. В многопоточном окружении безопасны для
  совместного чтения, но не допускают изменений.
- **EnumSet/EnumMap**: оптимизированы для перечислений, используют битовые наборы. Для потокобезопасности — внешняя
  синхронизация либо создание неизменяемых копий.
- **Concurrent Collections**: `ConcurrentSkipListMap`, `ConcurrentLinkedDeque`, `CopyOnWriteArraySet`. Предоставляют готовые
  механизмы синхронизации или неблокирующие алгоритмы, снижая количество ручных блокировок.

## Best practices
- Выбирайте интерфейсы в сигнатурах (`List` вместо `ArrayList`).
- Используйте `Collections.unmodifiableList` или неизменяемые коллекции для защиты API.
- Всегда переопределяйте `equals()` и `hashCode()` в паре для ключей `Map` и элементов `Set` (см. раздел [Контракты equals и hashCode](#контракты-equals-и-hashcode)).
- Для крупных коллекций контролируйте initial capacity (`new HashMap<>(capacity, loadFactor)`).

## Практические упражнения
1. Реализуйте собственный `LRUCache` на основе `LinkedHashMap` и протестируйте его потокобезопасность.
2. Сравните время выполнения операций `get`/`put` для `HashMap`, `ConcurrentHashMap`, `TreeMap` при разных объёмах данных.
3. Напишите API, принимающий `List<? extends Number>`, и объясните, почему нельзя добавлять элементы в такую коллекцию.
4. Создайте класс `Employee` с полями `id`, `name`, `department`. Реализуйте корректные `equals()` и `hashCode()`, затем 
   продемонстрируйте проблему, возникающую при изменении поля после добавления объекта в `HashSet`.

## Вопросы на собеседовании
1. **Почему generics реализованы через стирание типов?**
   *Ответ:* Для сохранения совместимости со старым байт-кодом и JVM. Старые библиотеки, не использующие generics, продолжают
   работать без изменений, а компилятор обеспечивает проверку типов на этапе компиляции.
2. **Что такое правило PECS?**
   *Ответ:* Producer Extends, Consumer Super. Если коллекция предоставляет элементы (producer), используйте `? extends T`. Если
   коллекция принимает элементы (consumer), используйте `? super T`.
3. **Как `HashMap` справляется с коллизиями?**
   *Ответ:* Коллизии сначала формируют связный список. Если цепочка превышает порог (по умолчанию 8) и размер массива >= 64,
   структура превращается в красно-чёрное дерево, что уменьшает сложность операций до O(log n).
4. **Чем отличаются `HashMap` и `ConcurrentHashMap`?**
   *Ответ:* `ConcurrentHashMap` поддерживает одновременные операции без глобальной блокировки, деля таблицу на сегменты и
   используя CAS. Он запрещает `null` ключи/значения и предоставляет дополнительные атомарные операции (`computeIfAbsent`).
5. **Когда стоит предпочесть неизменяемые коллекции?**
   *Ответ:* Когда нужно обеспечить потокобезопасность и защиту от случайной модификации. Иммутабельные коллекции сокращают
   количество защитных копий и упрощают reasoning.
6. **Какие свойства должен соблюдать контракт equals?**
   *Ответ:* Пять обязательных свойств: рефлексивность (x.equals(x) == true), симметричность (x.equals(y) == y.equals(x)), 
   транзитивность (если x.equals(y) и y.equals(z), то x.equals(z)), согласованность (повторные вызовы возвращают тот же результат) 
   и x.equals(null) == false для любого ненулевого x.
7. **Что произойдёт, если переопределить equals, но не переопределить hashCode?**
   *Ответ:* Нарушится контракт: равные по equals объекты могут иметь разные hash-коды, что приведёт к некорректной работе 
   HashMap/HashSet. Например, элемент не будет найден методом get(), даже если он есть в коллекции, или Set будет содержать дубликаты.
8. **Почему в реализации hashCode часто используется число 31?**
   *Ответ:* 31 — простое число, достаточно большое для минимизации коллизий, но при этом умножение на 31 эффективно оптимизируется 
   JVM как побитовый сдвиг: `31 * i == (i << 5) - i`. Это обеспечивает баланс между производительностью и качеством распределения.
9. **Можно ли использовать изменяемые поля в equals и hashCode?**
   *Ответ:* Технически можно, но это плохая практика. Если объект используется как ключ в Map или элемент в Set, изменение его 
   состояния после добавления нарушит инварианты коллекции: элемент попадёт в неправильный бакет и станет недоступен для поиска. 
   Рекомендуется использовать только неизменяемые (final) поля или делать такие объекты полностью иммутабельными.
10. **Как правильно реализовать equals при наследовании?**
    *Ответ:* Наследование создаёт проблему с симметричностью equals. Рекомендации: 1) Использовать композицию вместо наследования 
    для классов с разной логикой сравнения; 2) Сравнивать через getClass() вместо instanceof, чтобы объекты разных классов не 
    считались равными; 3) Или делать базовый класс абстрактным и запрещать сравнение между подклассами. Идеальное решение — 
    использовать record (Java 16+) или иммутабельные классы, где наследование не требуется.
11. **В чём разница между Collections.unmodifiableList() и List.of()?**
    *Ответ:* `Collections.unmodifiableList()` создаёт read-only view поверх исходной коллекции — если исходная коллекция изменяется, 
    изменения видны через wrapper. `List.of()` (Java 9+) создаёт истинно неизменяемую коллекцию, не допускает `null` элементы и 
    бросает `NullPointerException` при попытке их добавления. `List.of()` также более компактен в использовании и эффективен по памяти.
12. **Что возвращает метод subList() и какие у него особенности?**
    *Ответ:* `subList()` возвращает **view** (представление) на подсписок исходного списка, а не копию. Все изменения в подсписке 
    отражаются на исходном списке и наоборот. Если исходный список структурно модифицируется вне subList (например, через другой 
    метод), subList становится невалидным и бросает `ConcurrentModificationException`. Это полезно для эффективной работы с диапазонами 
    без создания копий, но требует осторожности.
13. **Для чего используются методы compute(), computeIfAbsent() и computeIfPresent() в Map?**
    *Ответ:* Эти атомарные методы (Java 8+) позволяют безопасно вычислять и обновлять значения в Map в многопоточной среде. 
    `computeIfAbsent()` добавляет новое значение, только если ключа нет (используется для ленивой инициализации); `computeIfPresent()` 
    обновляет значение, если ключ присутствует; `compute()` всегда вычисляет новое значение. Они атомарны для `ConcurrentHashMap` и 
    часто используются для группировки данных, подсчёта частот и кэширования.
14. **Чем метод merge() в Map отличается от put()?**
    *Ответ:* `merge(key, value, remappingFunction)` объединяет новое значение с существующим, используя функцию слияния. Если ключа нет, 
    добавляется новое значение. Если ключ существует, вызывается функция слияния для объединения старого и нового значений. Если функция 
    возвращает `null`, запись удаляется. Это атомарная операция, особенно полезная для подсчёта частот: 
    `map.merge(word, 1, Integer::sum)`.
15. **Почему Collections.sort() работает только со списками, а не со всеми коллекциями?**
    *Ответ:* Сортировка требует произвольного доступа к элементам (random access) для эффективности. List предоставляет метод `get(index)`, 
    что позволяет использовать эффективные алгоритмы сортировки (например, TimSort с O(n log n)). Set и другие коллекции не гарантируют 
    порядок или не поддерживают индексный доступ. Для сортировки других коллекций их можно преобразовать в List, отсортировать и 
    преобразовать обратно.
16. **В каких случаях следует использовать NavigableSet/NavigableMap вместо обычных Set/Map?**
    *Ответ:* `NavigableSet` (TreeSet) и `NavigableMap` (TreeMap) следует использовать, когда нужны отсортированные данные и операции 
    навигации: поиск ближайших элементов (ceiling, floor, higher, lower), получение подмножеств (subSet, headSet, tailSet), 
    обратная итерация (descendingSet). Они медленнее HashMap/HashSet (O(log n) vs O(1)), но предоставляют упорядоченность и богатый 
    API для диапазонных запросов.
