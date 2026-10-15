---
---

# Обобщения (Generics)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Generics и типобезопасность](#generics-и-типобезопасность)
   - [Основы Generics](#основы-generics)
   - [Type Erasure (Стирание типов)](#type-erasure-стирание-типов)
   - [Bounded Type Parameters (Ограниченные параметры типа)](#bounded-type-parameters-ограниченные-параметры-типа)
   - [Wildcards (Подстановочные символы)](#wildcards-подстановочные-символы)
   - [PECS (Producer Extends, Consumer Super)](#pecs-producer-extends-consumer-super)
   - [Generic Methods (Обобщённые методы)](#generic-methods-обобщённые-методы)
   - [Ограничения Generics](#ограничения-generics)
   - [Best Practices для Generics](#best-practices-для-generics)
3. [Best practices](#best-practices)
4. [Практические упражнения](#практические-упражнения)
5. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Актуальность материала

- **Дата проверки:** 2026-10-15 — точечная сверка устранения дублей, JLS-ограничений instanceof и создания generic arrays; старые фрагменты целиком не пересобирались.
- **Целевая версия или диапазон:** Java SE 17–21.
- **Статус примеров:** `current`; намеренные ошибки компиляции обозначены в комментариях, допустимое instanceof-уточнение проверено в CoreExamples.
- **Первичные источники:** [JLS 4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html); [JLS 15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2); [Class API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html).

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

Сведения о специализированных коллекциях, выборе реализации, `equals/hashCode`, условных обновлениях map и навигации
собраны в [главе «Коллекции»](05-collections.html). Здесь оставлены упражнения и вопросы, проверяющие именно систему типов.

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
   фабрику `IntFunction<T[]>` (например, `String[]::new`). Приведение `(T[]) new Object[size]` не создаёт массив с runtime-компонентом T и может вызвать `ClassCastException` у клиента.

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
    *Ответ:* `Class<T>` связывает token класса с известным параметром T, а `Class<?>` представляет неизвестный класс. `Class<T>.cast(value)` возвращает T и выполняет runtime-проверку. Для создания экземпляров используйте `getDeclaredConstructor().newInstance()` с обработкой reflective exceptions; старый `Class.newInstance()` не используйте как современный образец.

13. **Можно ли использовать instanceof с обобщёнными типами?**
    *Ответ:* Для переменной типа Object можно проверить `instanceof List<?>`, но нельзя проверить, что все элементы имеют тип String через `instanceof List<String>`. С Java 16 разрешены некоторые уточнения параметризованного типа без unchecked cast: например, для `List<Integer> x` допустимо `x instanceof ArrayList<Integer>`. Это проверяет runtime-класс, а не содержимое списка. См. [JLS 15.20.2](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.20.2).

14. **Что такое reified types и есть ли они в Java?**
    *Ответ:* Reified types — типы, информация о которых доступна во время выполнения. В Java generics НЕ reified из-за type erasure.
    Исключение: массивы reified — можно проверить `array instanceof String[]`. Некоторые языки (Scala, Kotlin с `reified`)
    поддерживают reified generics через inline функции.

15. **Как правильно работать с коллекциями при неизвестном типе элементов?**
    *Ответ:* Используйте wildcards: `List<?>` для чтения (получаем Object), `List<? extends T>` для чтения конкретного супертипа,
    `List<? super T>` для записи конкретного типа. Для полного доступа можно использовать helper методы с захватом wildcard через
    обобщённый параметр метода.
