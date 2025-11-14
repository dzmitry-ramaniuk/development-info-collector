# Обобщения (Generics)


## Содержание

1. [Основы Generics](#основы-generics)
2. [Type Erasure (Стирание типов)](#type-erasure-стирание-типов)
3. [Ограничения Generics](#ограничения-generics)
4. [Обобщённые классы и интерфейсы](#обобщённые-классы-и-интерфейсы)
5. [Обобщённые методы](#обобщённые-методы)
6. [Bounded Type Parameters](#bounded-type-parameters)
7. [Wildcards (Подстановочные символы)](#wildcards-подстановочные-символы)
   - [Unbounded Wildcard (?)](#unbounded-wildcard-)
   - [Upper Bounded Wildcard (? extends T)](#upper-bounded-wildcard--extends-t)
   - [Lower Bounded Wildcard (? super T)](#lower-bounded-wildcard--super-t)
8. [PECS принцип](#pecs-принцип)
9. [Множественные ограничения](#множественные-ограничения)
10. [Best practices](#best-practices)
11. [Практические упражнения](#практические-упражнения)
12. [Вопросы на собеседовании](#вопросы-на-собеседовании)

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

## Best practices

- Используйте generics для типобезопасности и устранения приведений типов
- Применяйте PECS принцип: Producer Extends, Consumer Super
- Избегайте raw types — всегда указывайте типовые параметры
- Используйте bounded wildcards для гибкости API
- Не создавайте массивы обобщённых типов — используйте коллекции
- Предпочитайте обобщённые методы обобщённым классам где возможно
- Документируйте ограничения типовых параметров
- Используйте `@SuppressWarnings("unchecked")` только когда уверены в безопасности

## Практические упражнения

1. Напишите обобщённый метод `swap(List<?> list, int i, int j)`, который меняет местами элементы списка.
2. Реализуйте обобщённый класс `Pair<K, V>` с методами для работы с парами значений.
3. Напишите API, принимающий `List<? extends Number>`, и объясните, почему нельзя добавлять элементы в такую коллекцию.
4. Создайте обобщённый метод `findMax`, который находит максимальный элемент в коллекции, используя bounded type parameter.
5. Реализуйте type-safe heterogeneous container (контейнер для разнородных типов) используя `Map<Class<?>, Object>`.

## Вопросы на собеседовании

1. **Почему generics реализованы через стирание типов?**
   *Ответ:* Для сохранения совместимости со старым байт-кодом и JVM. Старые библиотеки, не использующие generics, продолжают
   работать без изменений, а компилятор обеспечивает проверку типов на этапе компиляции.

2. **Что такое правило PECS?**
   *Ответ:* Producer Extends, Consumer Super. Если коллекция предоставляет элементы (producer), используйте `? extends T`. Если
   коллекция принимает элементы (consumer), используйте `? super T`.

3. **Можно ли создать массив обобщённого типа?**
   *Ответ:* Нет, `new T[]` или `new List<String>[]` невозможны из-за type erasure. После компиляции информация о типе стирается, 
   и JVM не может проверить типобезопасность массива. Решение: использовать `ArrayList<List<String>>` вместо массива или 
   `(T[]) new Object[size]` с предупреждением компилятора.

4. **В чём разница между `List<Object>` и `List<?>`?**
   *Ответ:* `List<Object>` — конкретный типизированный список объектов, можно добавлять любые объекты. `List<?>` — список 
   неизвестного типа, можно только читать элементы как Object, но нельзя добавлять (кроме null). `List<?>` более гибкий для 
   read-only операций.

5. **Почему нельзя добавлять элементы в `List<? extends T>`?**
   *Ответ:* Компилятор не знает точный тип списка. Например, `List<? extends Number>` может быть `List<Integer>`. Попытка 
   добавить `Double` в `List<Integer>` нарушит типобезопасность. Поэтому запрещены все добавления кроме `null`.

6. **Что такое bridge methods в контексте generics?**
   *Ответ:* Bridge methods — синтетические методы, генерируемые компилятором для сохранения полиморфизма после type erasure. 
   Например, при переопределении обобщённого метода компилятор создаёт bridge method с стёртыми типами, который делегирует 
   вызов типизированному методу.

7. **Можно ли использовать примитивные типы в generics?**
   *Ответ:* Нет, generics работают только с reference types. Примитивы автоматически оборачиваются в wrapper классы (autoboxing): 
   `List<int>` недопустим, используйте `List<Integer>`. Это влияет на производительность из-за boxing/unboxing.

8. **В чём разница между `<T extends Comparable<T>>` и `<T extends Comparable>`?**
   *Ответ:* `<T extends Comparable<T>>` более строгий — требует, чтобы тип T был сравним с самим собой. `<T extends Comparable>` 
   (raw type) допускает сравнение с любым типом, что менее безопасно. Первый вариант предпочтительнее.

9. **Что такое type inference и как он работает в Java?**
   *Ответ:* Type inference — автоматический вывод типов компилятором. Примеры: diamond operator `new ArrayList<>()`, обобщённые 
   методы `Collections.<String>emptyList()` или просто `Collections.emptyList()`. В Java 10+ добавлен `var` для локальных переменных.

10. **Можно ли создать экземпляр типового параметра `new T()`?**
    *Ответ:* Нет, из-за type erasure во время выполнения информация о T отсутствует. Решения: 1) Передавать `Class<T>` и использовать 
    рефлексию `clazz.getDeclaredConstructor().newInstance()`; 2) Использовать фабричный метод или Supplier<T>; 3) Передавать уже 
    созданный экземпляр или прототип.

11. **Что такое recursive type bound?**
    *Ответ:* Паттерн, где типовой параметр ссылается на себя в ограничении: `<T extends Comparable<T>>`. Используется для методов 
    сравнения и сортировки, гарантируя, что тип может сравнивать себя с собой. Пример: `public static <T extends Comparable<T>> T max(Collection<T> coll)`.

12. **В чём разница между `Class<T>` и `Class<?>`?**
    *Ответ:* `Class<T>` — параметризованный Class объект конкретного известного типа T. `Class<?>` — Class объект неизвестного типа. 
    `Class<T>` позволяет вызывать `cast(Object)` и `newInstance()` с корректным возвращаемым типом T, в то время как `Class<?>` 
    возвращает Object.

13. **Можно ли использовать instanceof с обобщёнными типами?**
    *Ответ:* Частично. `instanceof List` работает, но `instanceof List<String>` — ошибка компиляции из-за type erasure. Во время 
    выполнения информация о параметре типа недоступна. Можно проверить raw type, но не параметризованный.

14. **Что такое reified types и есть ли они в Java?**
    *Ответ:* Reified types — типы, информация о которых доступна во время выполнения. В Java generics НЕ reified из-за type erasure. 
    Исключение: массивы reified — можно проверить `array instanceof String[]`. Некоторые языки (Scala, Kotlin с `reified`) 
    поддерживают reified generics через inline функции.

15. **Как правильно работать с коллекциями при неизвестном типе элементов?**
    *Ответ:* Используйте wildcards: `List<?>` для чтения (получаем Object), `List<? extends T>` для чтения конкретного супертипа, 
    `List<? super T>` для записи конкретного типа. Для полного доступа можно использовать helper методы с захватом wildcard через 
    обобщённый параметр метода.
