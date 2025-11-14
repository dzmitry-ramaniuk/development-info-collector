# Функциональные возможности и современные фичи Java


## Содержание

1. [Lambda-выражения и функциональные интерфейсы](#lambda-выражения-и-функциональные-интерфейсы)
   - [Что такое функциональный интерфейс](#что-такое-функциональный-интерфейс)
   - [Аннотация @FunctionalInterface](#аннотация-functionalinterface)
   - [Стандартные функциональные интерфейсы](#стандартные-функциональные-интерфейсы)
     - [Function<T, R>](#functiont-r)
     - [BiFunction<T, U, R>](#bifunctiont-u-r)
     - [Consumer<T>](#consumert)
     - [BiConsumer<T, U>](#biconsumert-u)
     - [Supplier<T>](#suppliert)
     - [Predicate<T>](#predicatet)
     - [BiPredicate<T, U>](#bipredicatet-u)
     - [UnaryOperator<T>](#unaryoperatort)
     - [BinaryOperator<T>](#binaryoperatort)
   - [Таблица сравнения функциональных интерфейсов](#таблица-сравнения-функциональных-интерфейсов)
   - [Лучшие практики использования функциональных интерфейсов](#лучшие-практики-использования-функциональных-интерфейсов)
   - [Создание собственных функциональных интерфейсов](#создание-собственных-функциональных-интерфейсов)
   - [Замыкания и область видимости](#замыкания-и-область-видимости)
2. [Stream API](09-stream-api.md) — подробное описание в отдельном разделе
3. [Optional и null-safety](#optional-и-null-safety)
4. [Современные фичи языка](#современные-фичи-языка)
5. [Реактивное программирование и асинхронность](#реактивное-программирование-и-асинхронность)
6. [Инструменты для функционального стиля](#инструменты-для-функционального-стиля)
7. [Практические упражнения](#практические-упражнения)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Lambda-выражения и функциональные интерфейсы

### Что такое функциональный интерфейс

**Функциональный интерфейс** (Functional Interface) — это интерфейс, содержащий ровно один абстрактный метод. Такие интерфейсы могут быть реализованы через lambda-выражения или ссылки на методы, что делает код более компактным и выразительным.

Функциональные интерфейсы являются основой функционального программирования в Java и широко используются в Stream API, асинхронной обработке, обработке событий и других случаях, где нужно передать поведение как параметр.

#### Аннотация @FunctionalInterface

Аннотация `@FunctionalInterface` является опциональной, но рекомендуемой практикой. Она:
- Явно обозначает намерение использовать интерфейс как функциональный
- Заставляет компилятор проверить, что интерфейс содержит ровно один абстрактный метод
- Помогает избежать ошибок при модификации интерфейса в будущем

```java
@FunctionalInterface
public interface MyFunction {
    int apply(int x);
    
    // Можно иметь default методы
    default void log() {
        System.out.println("Function called");
    }
    
    // Можно иметь static методы
    static MyFunction identity() {
        return x -> x;
    }
}
```

> **Важно**: Функциональный интерфейс может иметь несколько default и static методов, но только один абстрактный метод.

### Стандартные функциональные интерфейсы

Java предоставляет богатый набор готовых функциональных интерфейсов в пакете `java.util.function`. Эти интерфейсы покрывают большинство типичных сценариев использования.

#### Function<T, R>

Принимает аргумент типа `T` и возвращает результат типа `R`. Базовый интерфейс для преобразования одного значения в другое.

```java
@FunctionalInterface
public interface Function<T, R> {
    R apply(T t);
    
    // Композиция функций
    default <V> Function<V, R> compose(Function<? super V, ? extends T> before);
    default <V> Function<T, V> andThen(Function<? super R, ? extends V> after);
}
```

**Примеры использования:**

```java
// Преобразование строки в её длину
Function<String, Integer> stringLength = String::length;
Integer length = stringLength.apply("Hello"); // 5

// Композиция функций
Function<String, String> trim = String::trim;
Function<String, Integer> trimAndLength = trim.andThen(String::length);
Integer result = trimAndLength.apply("  Hello  "); // 5

// Использование в Stream API
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
List<Integer> lengths = names.stream()
    .map(String::length)
    .collect(Collectors.toList());
```

**Специализированные версии:**
- `IntFunction<R>` — принимает `int`, возвращает `R`
- `LongFunction<R>` — принимает `long`, возвращает `R`
- `DoubleFunction<R>` — принимает `double`, возвращает `R`
- `ToIntFunction<T>` — принимает `T`, возвращает `int`
- `ToLongFunction<T>` — принимает `T`, возвращает `long`
- `ToDoubleFunction<T>` — принимает `T`, возвращает `double`

#### BiFunction<T, U, R>

Принимает два аргумента типов `T` и `U`, возвращает результат типа `R`.

```java
@FunctionalInterface
public interface BiFunction<T, U, R> {
    R apply(T t, U u);
}
```

**Примеры использования:**

```java
// Конкатенация строк с разделителем
BiFunction<String, String, String> concat = (a, b) -> a + " " + b;
String result = concat.apply("Hello", "World"); // "Hello World"

// Математические операции
BiFunction<Integer, Integer, Integer> multiply = (a, b) -> a * b;
Integer product = multiply.apply(5, 3); // 15

// Использование в коллекциях
Map<String, Integer> map = new HashMap<>();
map.put("key", 1);
map.merge("key", 5, (oldVal, newVal) -> oldVal + newVal); // key -> 6
```

#### Consumer<T>

Принимает аргумент типа `T` и не возвращает результат (выполняет побочный эффект).

```java
@FunctionalInterface
public interface Consumer<T> {
    void accept(T t);
    
    default Consumer<T> andThen(Consumer<? super T> after);
}
```

**Примеры использования:**

```java
// Вывод в консоль
Consumer<String> printer = System.out::println;
printer.accept("Hello, World!");

// Изменение объекта
Consumer<List<String>> addElement = list -> list.add("новый элемент");
List<String> myList = new ArrayList<>();
addElement.accept(myList);

// Цепочка consumers
Consumer<String> log = s -> System.out.println("LOG: " + s);
Consumer<String> save = s -> saveToDatabase(s);
Consumer<String> logAndSave = log.andThen(save);
logAndSave.accept("Important message");

// В Stream API
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
names.forEach(System.out::println);
```

**Специализированные версии:**
- `IntConsumer` — принимает `int`
- `LongConsumer` — принимает `long`
- `DoubleConsumer` — принимает `double`

#### BiConsumer<T, U>

Принимает два аргумента и не возвращает результат.

```java
@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);
}
```

**Примеры использования:**

```java
// Работа с Map
BiConsumer<String, Integer> mapPrinter = (key, value) -> 
    System.out.println(key + " = " + value);

Map<String, Integer> map = Map.of("one", 1, "two", 2);
map.forEach(mapPrinter);

// Использование в Stream API
Map<String, List<Person>> groupedByCity = persons.stream()
    .collect(Collectors.groupingBy(Person::getCity));
    
groupedByCity.forEach((city, people) -> 
    System.out.println(city + ": " + people.size() + " persons"));
```

#### Supplier<T>

Не принимает аргументов, возвращает результат типа `T`. Используется для отложенного вычисления или генерации значений.

```java
@FunctionalInterface
public interface Supplier<T> {
    T get();
}
```

**Примеры использования:**

```java
// Генерация случайных чисел
Supplier<Integer> randomInt = () -> new Random().nextInt(100);
Integer random = randomInt.get();

// Ленивая инициализация
Supplier<ExpensiveObject> lazyObject = ExpensiveObject::new;
// Объект создастся только при вызове get()
ExpensiveObject obj = lazyObject.get();

// Использование с Optional
String result = Optional.ofNullable(getUserName())
    .orElseGet(() -> "Anonymous"); // Supplier вычисляется только если значение отсутствует

// Генерация бесконечного потока
Stream<UUID> uuidStream = Stream.generate(UUID::randomUUID);
List<UUID> uuids = uuidStream.limit(10).collect(Collectors.toList());
```

**Специализированные версии:**
- `BooleanSupplier` — возвращает `boolean`
- `IntSupplier` — возвращает `int`
- `LongSupplier` — возвращает `long`
- `DoubleSupplier` — возвращает `double`

#### Predicate<T>

Принимает аргумент типа `T` и возвращает `boolean`. Используется для проверки условий.

```java
@FunctionalInterface
public interface Predicate<T> {
    boolean test(T t);
    
    // Логические операции
    default Predicate<T> and(Predicate<? super T> other);
    default Predicate<T> or(Predicate<? super T> other);
    default Predicate<T> negate();
    static <T> Predicate<T> isEqual(Object targetRef);
}
```

**Примеры использования:**

```java
// Простая проверка
Predicate<String> isEmpty = String::isEmpty;
boolean result = isEmpty.test(""); // true

// Фильтрация коллекций
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

// Композиция предикатов
Predicate<String> startsWithA = s -> s.startsWith("A");
Predicate<String> longerThan5 = s -> s.length() > 5;
Predicate<String> combined = startsWithA.and(longerThan5);

List<String> names = Arrays.asList("Alice", "Alexander", "Bob", "Anna");
List<String> filtered = names.stream()
    .filter(combined)
    .collect(Collectors.toList()); // ["Alexander"]

// Отрицание
Predicate<String> notEmpty = isEmpty.negate();

// Сложные условия
Predicate<Person> isAdult = p -> p.getAge() >= 18;
Predicate<Person> hasDriverLicense = Person::hasDriverLicense;
Predicate<Person> canDrive = isAdult.and(hasDriverLicense);
```

**Специализированные версии:**
- `IntPredicate` — принимает `int`
- `LongPredicate` — принимает `long`
- `DoublePredicate` — принимает `double`

#### BiPredicate<T, U>

Принимает два аргумента и возвращает `boolean`.

```java
@FunctionalInterface
public interface BiPredicate<T, U> {
    boolean test(T t, U u);
}
```

**Примеры использования:**

```java
// Сравнение двух значений
BiPredicate<String, String> equals = String::equals;
boolean result = equals.test("hello", "hello"); // true

// Фильтрация Map
BiPredicate<String, Integer> ageCheck = (name, age) -> age >= 18;
Map<String, Integer> people = Map.of("Alice", 25, "Bob", 15, "Charlie", 30);

Map<String, Integer> adults = people.entrySet().stream()
    .filter(entry -> ageCheck.test(entry.getKey(), entry.getValue()))
    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
```

#### UnaryOperator<T>

Специализация `Function<T, T>` — принимает и возвращает значение одного типа.

```java
@FunctionalInterface
public interface UnaryOperator<T> extends Function<T, T> {
    static <T> UnaryOperator<T> identity();
}
```

**Примеры использования:**

```java
// Преобразование строки
UnaryOperator<String> toUpperCase = String::toUpperCase;
String result = toUpperCase.apply("hello"); // "HELLO"

// Математические операции
UnaryOperator<Integer> square = x -> x * x;
Integer squared = square.apply(5); // 25

// В List.replaceAll
List<String> words = Arrays.asList("hello", "world");
words.replaceAll(String::toUpperCase); // ["HELLO", "WORLD"]

// Композиция операторов
UnaryOperator<Integer> addTwo = x -> x + 2;
UnaryOperator<Integer> multiplyByThree = x -> x * 3;
UnaryOperator<Integer> combined = addTwo.andThen(multiplyByThree);
Integer result = combined.apply(5); // (5 + 2) * 3 = 21
```

**Специализированные версии:**
- `IntUnaryOperator` — для `int`
- `LongUnaryOperator` — для `long`
- `DoubleUnaryOperator` — для `double`

#### BinaryOperator<T>

Специализация `BiFunction<T, T, T>` — принимает два аргумента одного типа и возвращает значение того же типа.

```java
@FunctionalInterface
public interface BinaryOperator<T> extends BiFunction<T, T, T> {
    static <T> BinaryOperator<T> minBy(Comparator<? super T> comparator);
    static <T> BinaryOperator<T> maxBy(Comparator<? super T> comparator);
}
```

**Примеры использования:**

```java
// Математические операции
BinaryOperator<Integer> sum = (a, b) -> a + b;
Integer result = sum.apply(5, 3); // 8

// Конкатенация строк
BinaryOperator<String> concat = (a, b) -> a + b;
String combined = concat.apply("Hello", "World"); // "HelloWorld"

// Использование в reduce
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
Integer total = numbers.stream()
    .reduce(0, (a, b) -> a + b); // 15

// Поиск максимума
BinaryOperator<String> maxBy = BinaryOperator.maxBy(String::compareTo);
Optional<String> max = Stream.of("apple", "banana", "cherry")
    .reduce(maxBy); // "cherry"

// Слияние Map
Map<String, Integer> map1 = new HashMap<>(Map.of("a", 1, "b", 2));
Map<String, Integer> map2 = Map.of("b", 3, "c", 4);
map2.forEach((key, value) -> 
    map1.merge(key, value, Integer::sum)); // {a=1, b=5, c=4}
```

**Специализированные версии:**
- `IntBinaryOperator` — для `int`
- `LongBinaryOperator` — для `long`
- `DoubleBinaryOperator` — для `double`

### Таблица сравнения функциональных интерфейсов

| Интерфейс | Параметры | Возвращаемое значение | Абстрактный метод | Типичное использование |
|-----------|-----------|----------------------|-------------------|------------------------|
| `Function<T, R>` | T | R | `R apply(T t)` | Преобразование значений |
| `BiFunction<T, U, R>` | T, U | R | `R apply(T t, U u)` | Преобразование двух значений |
| `Consumer<T>` | T | void | `void accept(T t)` | Побочные эффекты |
| `BiConsumer<T, U>` | T, U | void | `void accept(T t, U u)` | Побочные эффекты с двумя аргументами |
| `Supplier<T>` | нет | T | `T get()` | Генерация/предоставление значений |
| `Predicate<T>` | T | boolean | `boolean test(T t)` | Проверка условий |
| `BiPredicate<T, U>` | T, U | boolean | `boolean test(T t, U u)` | Проверка условий с двумя аргументами |
| `UnaryOperator<T>` | T | T | `T apply(T t)` | Преобразование того же типа |
| `BinaryOperator<T>` | T, T | T | `T apply(T t1, T t2)` | Объединение двух значений одного типа |

### Лучшие практики использования функциональных интерфейсов

1. **Используйте стандартные интерфейсы вместо создания своих**
   ```java
   // Плохо: создание собственного интерфейса
   @FunctionalInterface
   interface StringProcessor {
       String process(String s);
   }
   
   // Хорошо: использование стандартного Function
   Function<String, String> processor = String::toUpperCase;
   ```

2. **Предпочитайте ссылки на методы лямбдам, где это улучшает читаемость**
   ```java
   // Можно
   list.forEach(s -> System.out.println(s));
   
   // Лучше
   list.forEach(System.out::println);
   ```

3. **Используйте специализированные примитивные версии для производительности**
   ```java
   // Менее эффективно: boxing/unboxing
   Function<Integer, Integer> square = x -> x * x;
   
   // Более эффективно: без autoboxing
   IntUnaryOperator square = x -> x * x;
   ```

4. **Избегайте побочных эффектов в чистых функциях**
   ```java
   // Плохо: побочный эффект в map
   List<Integer> externalList = new ArrayList<>();
   numbers.stream()
       .map(x -> {
           externalList.add(x); // Побочный эффект!
           return x * 2;
       })
       .collect(Collectors.toList());
   
   // Хорошо: используйте peek для побочных эффектов или Consumer
   numbers.stream()
       .peek(externalList::add)
       .map(x -> x * 2)
       .collect(Collectors.toList());
   ```

5. **Используйте композицию для построения сложной логики**
   ```java
   Predicate<String> startsWithA = s -> s.startsWith("A");
   Predicate<String> longerThan5 = s -> s.length() > 5;
   Predicate<String> combined = startsWithA.and(longerThan5);
   
   Function<String, String> trim = String::trim;
   Function<String, String> toLowerCase = String::toLowerCase;
   Function<String, String> normalize = trim.andThen(toLowerCase);
   ```

6. **Для Optional используйте Supplier для ленивого вычисления**
   ```java
   // Плохо: дорогостоящая операция выполняется всегда
   String result = optional.orElse(expensiveComputation());
   
   // Хорошо: вычисляется только при необходимости
   String result = optional.orElseGet(() -> expensiveComputation());
   ```

### Создание собственных функциональных интерфейсов

Хотя стандартная библиотека покрывает большинство случаев, иногда имеет смысл создать собственный функциональный интерфейс для улучшения читаемости и типобезопасности в специфичных доменах.

```java
// Доменно-специфичный интерфейс
@FunctionalInterface
public interface PriceCalculator {
    BigDecimal calculate(Product product, Customer customer);
    
    default PriceCalculator andThen(PriceCalculator after) {
        return (product, customer) -> 
            after.calculate(product, customer);
    }
}

// Использование
PriceCalculator basePrice = (product, customer) -> product.getBasePrice();
PriceCalculator withDiscount = (product, customer) -> {
    BigDecimal price = product.getBasePrice();
    return customer.isPremium() 
        ? price.multiply(BigDecimal.valueOf(0.9))
        : price;
};
PriceCalculator withTax = (product, customer) -> 
    product.getBasePrice().multiply(BigDecimal.valueOf(1.2));

// Можно заменить на BiFunction<Product, Customer, BigDecimal>
// но доменное название делает код более выразительным
```

### Замыкания и область видимости
Лямбда захватывает переменные из внешнего контекста, если они effectively final. Это означает, что переменные не должны
изменяться после присваивания. Такая модель упрощает потокобезопасность и JIT-оптимизации.

```java
// Effectively final переменная
String prefix = "Hello, ";
Function<String, String> greeter = name -> prefix + name;

// Ошибка компиляции: переменная не effectively final
int counter = 0;
Consumer<String> printer = s -> {
    // counter++; // Ошибка компиляции!
    System.out.println(s);
};
counter++; // Изменение делает переменную не effectively final

// Правильный подход: использовать изменяемый контейнер
AtomicInteger atomicCounter = new AtomicInteger(0);
Consumer<String> counterPrinter = s -> {
    atomicCounter.incrementAndGet(); // OK
    System.out.println(s);
};
```

## Stream API

**Stream API** — мощный инструмент для декларативной обработки последовательностей данных, появившийся в Java 8. Stream представляет собой ленивое представление последовательности данных, поддерживающее различные операции для фильтрации, трансформации и агрегации данных.

**Основные концепции:**
- **Ленивость**: промежуточные операции выполняются только при вызове терминальной операции
- **Одноразовость**: stream можно использовать только один раз
- **Функциональный подход**: операции не изменяют исходную коллекцию

**Базовый пример:**
```java
List<String> result = names.stream()
    .filter(name -> name.length() > 3)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());
```

> **Подробное описание Stream API вынесено в отдельный раздел:** [Stream API](09-stream-api.md)
> 
> В нём вы найдёте:
> - Детальное описание всех промежуточных и терминальных операций
> - Работу с коллекторами (Collectors)
> - Примитивные стримы (IntStream, LongStream, DoubleStream)
> - Параллельные стримы и особенности их использования
> - Лучшие практики и типичные ошибки
> - Практические примеры и вопросы для собеседований

## Optional и null-safety
`Optional` помогает явно выразить отсутствие значения. Используйте `Optional.of`, `Optional.ofNullable`, `Optional.empty`.
Методы `map`, `flatMap`, `orElse`, `orElseGet`, `orElseThrow` упрощают обработку. Не злоупотребляйте `Optional` в полях и
сериализации — объект весит больше и не предназначен для коллекций как ключ.

## Современные фичи языка
- **var (Java 10)** — вывод типа локальных переменных.
- **Records (Java 16)** — компактные классы с автоматической генерацией конструктора, `equals`, `hashCode`, `toString`.
- **Pattern Matching**: `instanceof` с привязкой переменной (Java 16), `switch`-паттерны (preview в Java 19+).
- **Text Blocks (Java 15)** — многострочные литералы.
- **Sealed классы (Java 17)** — ограничение наследования.
- **Virtual Threads (Project Loom, Java 21)** — лёгкие потоки для масштабируемости.

## Реактивное программирование и асинхронность
- **CompletableFuture**: предоставляет комбинаторы (`thenApply`, `thenCompose`, `allOf`, `exceptionally`). Используйте кастомные
  `Executor`, чтобы контролировать потоки.
- **Reactive Streams**: Flow API (Java 9) содержит `Publisher`, `Subscriber`, `Processor`. Популярные реализации — Project
  Reactor, RxJava.
- **Structured Concurrency** (Incubator в Java 21) — координация подзадач и автоматическое управление отменой.

## Инструменты для функционального стиля
- Используйте `Collectors.teeing`, `Collectors.flatMapping` для сложных агрегатов.
- Применяйте `Map.of`, `Set.of` для неизменяемых структур.
- Лямбды и ссылки на методы (`String::toUpperCase`) улучшают читаемость.
- Для pattern matching полезны библиотеки Vavr, позволяющие писать выражения `match` и работать с персистентными коллекциями.

## Практические упражнения
1. Обработайте CSV-файл с помощью Stream API: сгруппируйте данные, вычислите агрегаты, выведите JSON.
2. Напишите цепочку `CompletableFuture`, выполняющую запросы к REST API параллельно и объединяющую результаты.
3. Реализуйте собственный коллектор, собирающий статистику (минимум, максимум, среднее) по потоку чисел.

## Вопросы на собеседовании

1. **Что такое функциональный интерфейс? Обязательна ли аннотация @FunctionalInterface?**
   *Ответ:* Функциональный интерфейс — это интерфейс с ровно одним абстрактным методом. Аннотация `@FunctionalInterface` не обязательна, но рекомендуется: она явно показывает намерение и заставляет компилятор проверить, что интерфейс соответствует требованиям. Интерфейс может содержать любое количество default и static методов.

2. **В чём разница между Function и UnaryOperator?**
   *Ответ:* `UnaryOperator<T>` — это специализация `Function<T, T>`, где входной и выходной типы совпадают. `UnaryOperator` семантически яснее для операций типа "преобразование значения того же типа" и имеет статический метод `identity()`.

3. **Зачем нужны специализированные примитивные версии (IntFunction, IntPredicate и т.д.)?**
   *Ответ:* Они избегают autoboxing/unboxing примитивов в wrapper-классы (например, `int` → `Integer`), что значительно улучшает производительность при интенсивных вычислениях. Также снижается потребление памяти и нагрузка на GC.

4. **В чём разница между Consumer и Function?**
   *Ответ:* `Consumer<T>` принимает аргумент и не возвращает результат (void), используется для побочных эффектов (печать, изменение состояния). `Function<T, R>` принимает аргумент и возвращает результат, используется для преобразований данных.

5. **Когда использовать BiFunction вместо Function?**
   *Ответ:* `BiFunction<T, U, R>` используется, когда нужно принять два аргумента разных типов и вернуть результат. Например, при объединении двух значений в одно или в операциях типа `Map.merge()`, `Map.compute()`.

6. **Чем отличаются `map` и `flatMap` в Stream API?**
   *Ответ:* `map` применяет функцию к каждому элементу и возвращает поток результатов, `flatMap` раскрывает вложенные потоки или
   коллекции, превращая их в единый поток.

7. **Как работает `Optional.orElse` в сравнении с `orElseGet`?**
   *Ответ:* `orElse` вычисляет аргумент всегда, даже если значение присутствует. `orElseGet` принимает `Supplier` и вычисляет
   значение лениво, только при отсутствии данных.

8. **Почему не стоит использовать `Optional` в полях сущностей?**
   *Ответ:* `Optional` предназначен для возвращаемых значений. В полях он добавляет накладные расходы (дополнительный объект),
   усложняет сериализацию и JPA/JSON-биндинг.

9. **В чём преимущества `record` по сравнению с обычными классами?**
   *Ответ:* `record` автоматически генерирует конструктор, `equals`, `hashCode`, `toString`, обеспечивает иммутабельность и чётко
   описывает компоненты. Подходит для DTO и value-объектов.

10. **Зачем нужны виртуальные потоки (Project Loom)?**
    *Ответ:* Virtual threads позволяют запускать тысячи легковесных задач поверх небольшого пула carrier threads. Они упрощают
    масштабирование I/O-нагруженных приложений, не требуя сложной реактивной архитектуры.

11. **Можно ли использовать лямбда-выражение, если переменная захватывается и изменяется?**
    *Ответ:* Нет, захватываемые переменные должны быть effectively final. Если нужно изменять состояние, используйте изменяемые контейнеры (`AtomicInteger`, `AtomicReference`, массивы) или переменные экземпляра класса.

12. **В чём разница между Predicate и Function<T, Boolean>?**
    *Ответ:* Семантически `Predicate<T>` предназначен для проверки условий и имеет метод `test()`, а также удобные методы композиции (`and`, `or`, `negate`). `Function<T, Boolean>` возвращает объект `Boolean` (wrapper), а `Predicate` — примитив `boolean`, что эффективнее. `Predicate` более выразителен для фильтрации и проверок условий.

13. **Когда стоит создавать собственный функциональный интерфейс вместо использования стандартных?**
    *Ответ:* Создавайте собственный интерфейс, когда:
    - Требуется доменно-специфичное имя для улучшения читаемости кода
    - Нужны специальные default методы с бизнес-логикой
    - Интерфейс будет широко использоваться в API и его имя важно для понимания
    - Стандартные интерфейсы не выражают намерение достаточно ясно
    Во всех остальных случаях предпочитайте стандартные интерфейсы из `java.util.function`.

14. **Что произойдёт, если в функциональном интерфейсе добавить второй абстрактный метод?**
    *Ответ:* Если интерфейс помечен `@FunctionalInterface`, компилятор выдаст ошибку. Без аннотации интерфейс просто перестанет быть функциональным, и существующие лямбда-выражения, использующие его, перестанут компилироваться.

15. **Как композиция функций влияет на производительность?**
    *Ответ:* Композиция через `andThen` и `compose` создаёт цепочку вызовов функций. При умеренном использовании влияние на производительность минимально благодаря JIT-оптимизации (inline). Однако очень длинные цепочки могут усложнить отладку stack traces и создать небольшие накладные расходы. Для критичных по производительности участков кода измеряйте с помощью JMH бенчмарков.
