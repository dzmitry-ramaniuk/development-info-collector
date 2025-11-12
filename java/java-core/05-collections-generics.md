# Коллекции и обобщения


## Содержание

1. [Каркас коллекций Java](#каркас-коллекций-java)
   - [List](#list)
   - [Set](#set)
   - [Queue и Deque](#queue-и-deque)
   - [Map](#map)
2. [Методы для работы с коллекциями](#методы-для-работы-с-коллекциями)
   - [Утилитные методы класса Collections](#утилитные-методы-класса-collections)
   - [Методы интерфейса Collection](#методы-интерфейса-collection)
   - [Методы интерфейса List](#методы-интерфейса-list)
   - [Методы интерфейса Set](#методы-интерфейса-set)
   - [Методы интерфейса Map](#методы-интерфейса-map)
   - [Фабричные методы для создания коллекций](#фабричные-методы-для-создания-коллекций)
3. [Контракты equals и hashCode](#контракты-equals-и-hashcode)
   - [Контракт equals](#контракт-equals)
   - [Контракт hashCode](#контракт-hashcode)
   - [Связь equals и hashCode в коллекциях](#связь-equals-и-hashcode-в-коллекциях)
   - [Правильная реализация](#правильная-реализация)
   - [Распространённые ошибки](#распространённые-ошибки)
4. [Generics и типобезопасность](#generics-и-типобезопасность)
5. [Специализированные коллекции](#специализированные-коллекции)
6. [Best practices](#best-practices)
7. [Практические упражнения](#практические-упражнения)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

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
Generics появились в Java 5 и реализованы через стирание типов (type erasure). Во время компиляции создаётся специализированный
байт-код, но в рантайме все обобщённые типы приводятся к `Object`. Это позволяет поддерживать обратную совместимость, но влечёт
ограничения: нельзя создавать массивы обобщённых типов (`new T[]`), нельзя использовать примитивы без обёрток.

Используйте ограниченные параметры (`<T extends Comparable<T>>`), wildcard (`?`, `? extends`, `? super`) для ковариантности и
контравариантности. Соблюдайте правило PECS (Producer Extends, Consumer Super) при работе с коллекциями.

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
