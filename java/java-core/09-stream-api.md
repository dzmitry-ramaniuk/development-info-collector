# Stream API

## Содержание

1. [Введение в Stream API](#введение-в-stream-api)
   - [Что такое Stream](#что-такое-stream)
   - [Основные характеристики](#основные-характеристики)
   - [Отличия от коллекций](#отличия-от-коллекций)
2. [Создание стримов](#создание-стримов)
   - [Из коллекций](#из-коллекций)
   - [Из массивов](#из-массивов)
   - [Генерация стримов](#генерация-стримов)
   - [Из других источников](#из-других-источников)
3. [Промежуточные операции](#промежуточные-операции)
   - [map](#map)
   - [filter](#filter)
   - [flatMap](#flatmap)
   - [distinct](#distinct)
   - [sorted](#sorted)
   - [peek](#peek)
   - [limit и skip](#limit-и-skip)
4. [Терминальные операции](#терминальные-операции)
   - [collect](#collect)
   - [forEach и forEachOrdered](#foreach-и-foreachordered)
   - [reduce](#reduce)
   - [count, min, max](#count-min-max)
   - [anyMatch, allMatch, noneMatch](#anymatch-allmatch-nonematch)
   - [findFirst и findAny](#findfirst-и-findany)
   - [toArray](#toarray)
5. [Коллекторы (Collectors)](#коллекторы-collectors)
   - [Базовые коллекторы](#базовые-коллекторы)
   - [Группировка и разбиение](#группировка-и-разбиение)
   - [Объединение и агрегация](#объединение-и-агрегация)
   - [Пользовательские коллекторы](#пользовательские-коллекторы)
6. [Примитивные стримы](#примитивные-стримы)
   - [IntStream, LongStream, DoubleStream](#intstream-longstream-doublestream)
   - [Преобразование между стримами](#преобразование-между-стримами)
   - [Статистические операции](#статистические-операции)
7. [Параллельные стримы](#параллельные-стримы)
   - [Создание параллельных стримов](#создание-параллельных-стримов)
   - [Fork/Join Framework](#forkjoin-framework)
   - [Когда использовать параллельные стримы](#когда-использовать-параллельные-стримы)
   - [Производительность и подводные камни](#производительность-и-подводные-камни)
8. [Лучшие практики](#лучшие-практики)
9. [Типичные ошибки](#типичные-ошибки)
10. [Практические примеры](#практические-примеры)
11. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Введение в Stream API

### Что такое Stream

**Stream API** (появился в Java 8) — это мощный инструмент для декларативной обработки последовательностей данных. Stream представляет собой абстракцию потока элементов, поддерживающую различные операции для фильтрации, трансформации, агрегации и других видов обработки данных.

```java
// Императивный подход (до Java 8)
List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David");
List<String> result = new ArrayList<>();
for (String name : names) {
    if (name.length() > 3) {
        result.add(name.toUpperCase());
    }
}
Collections.sort(result);

// Декларативный подход с Stream API
List<String> result = names.stream()
    .filter(name -> name.length() > 3)
    .map(String::toUpperCase)
    .sorted()
    .collect(Collectors.toList());
```

### Основные характеристики

1. **Ленивость (Lazy evaluation)**: Промежуточные операции не выполняются до вызова терминальной операции. Это позволяет оптимизировать цепочки операций.

2. **Одноразовость**: Stream можно использовать только один раз. После выполнения терминальной операции stream закрывается.

```java
Stream<String> stream = names.stream();
long count = stream.count(); // OK
// stream.forEach(System.out::println); // IllegalStateException!
```

3. **Неизменяемость источника**: Stream не модифицирует исходную коллекцию, а создаёт новую структуру данных.

4. **Композитность**: Операции можно объединять в цепочки для создания сложных преобразований.

5. **Внутренняя итерация**: В отличие от внешней итерации (for-each, iterator), Stream API использует внутреннюю итерацию, что позволяет библиотеке контролировать процесс обхода и оптимизировать его.

### Отличия от коллекций

| Характеристика | Коллекции | Stream |
|----------------|-----------|--------|
| **Хранение данных** | Хранят элементы в памяти | Не хранят элементы, работают с источником |
| **Модификация** | Можно изменять | Неизменяемы (функциональный подход) |
| **Итерация** | Внешняя (явная) | Внутренняя (управляется библиотекой) |
| **Использование** | Многократное | Одноразовое |
| **Вычисление** | Eager (немедленное) | Lazy (отложенное) |
| **Размер** | Всегда конечный | Может быть бесконечным |

## Создание стримов

### Из коллекций

Самый распространённый способ создания stream — из существующей коллекции:

```java
// Из List
List<String> list = Arrays.asList("a", "b", "c");
Stream<String> streamFromList = list.stream();

// Из Set
Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
Stream<Integer> streamFromSet = set.stream();

// Из Map (через entrySet)
Map<String, Integer> map = new HashMap<>();
Stream<Map.Entry<String, Integer>> streamFromMap = map.entrySet().stream();

// Параллельный stream
Stream<String> parallelStream = list.parallelStream();
```

### Из массивов

```java
// Из массива объектов
String[] array = {"a", "b", "c"};
Stream<String> streamFromArray = Arrays.stream(array);
Stream<String> streamFromArray2 = Stream.of(array);

// Из части массива
Stream<String> partialStream = Arrays.stream(array, 1, 3); // "b", "c"

// Из примитивного массива
int[] intArray = {1, 2, 3, 4, 5};
IntStream intStream = Arrays.stream(intArray);
```

### Генерация стримов

```java
// Создание из отдельных элементов
Stream<String> stream1 = Stream.of("a", "b", "c");
Stream<Integer> stream2 = Stream.of(1, 2, 3, 4, 5);

// Пустой stream
Stream<String> emptyStream = Stream.empty();

// Stream.generate - бесконечный stream с Supplier
Stream<Double> randomNumbers = Stream.generate(Math::random);
Stream<String> constants = Stream.generate(() -> "constant");

// Stream.iterate - бесконечный stream с начальным значением и функцией
Stream<Integer> evenNumbers = Stream.iterate(0, n -> n + 2); // 0, 2, 4, 6...
Stream<Integer> fibonacci = Stream.iterate(new int[]{0, 1}, 
    arr -> new int[]{arr[1], arr[0] + arr[1]})
    .map(arr -> arr[0]); // 0, 1, 1, 2, 3, 5, 8...

// Stream.iterate с предикатом (Java 9+)
Stream<Integer> limitedStream = Stream.iterate(0, n -> n < 100, n -> n + 1);
```

### Из других источников

```java
// Из строки (символы)
IntStream charsStream = "Hello".chars();

// Из строки (строки)
Stream<String> linesStream = "line1\nline2\nline3".lines();

// Из файла
try (Stream<String> lines = Files.lines(Paths.get("file.txt"))) {
    lines.forEach(System.out::println);
}

// Из диапазона чисел
IntStream range = IntStream.range(1, 10);        // 1..9
IntStream rangeClosed = IntStream.rangeClosed(1, 10); // 1..10

// Из Pattern
Pattern pattern = Pattern.compile(" ");
Stream<String> words = pattern.splitAsStream("Hello World Stream");

// Конкатенация стримов
Stream<String> stream1 = Stream.of("a", "b");
Stream<String> stream2 = Stream.of("c", "d");
Stream<String> combined = Stream.concat(stream1, stream2);

// Stream.Builder
Stream.Builder<String> builder = Stream.builder();
builder.add("a").add("b").add("c");
Stream<String> builtStream = builder.build();
```

## Промежуточные операции

Промежуточные операции (intermediate operations) возвращают новый stream и выполняются лениво. Они не начинают обработку данных до вызова терминальной операции.

### map

Преобразует каждый элемент stream с помощью функции:

```java
// Преобразование строк в их длины
List<String> words = Arrays.asList("Java", "Stream", "API");
List<Integer> lengths = words.stream()
    .map(String::length)
    .collect(Collectors.toList()); // [4, 6, 3]

// Преобразование объектов
List<Person> people = getPeople();
List<String> names = people.stream()
    .map(Person::getName)
    .collect(Collectors.toList());

// Цепочка преобразований
List<String> upperNames = people.stream()
    .map(Person::getName)
    .map(String::toUpperCase)
    .collect(Collectors.toList());

// Преобразование с использованием lambda
List<Integer> doubled = numbers.stream()
    .map(n -> n * 2)
    .collect(Collectors.toList());
```

**Специализированные версии:**
- `mapToInt()` — преобразует в IntStream
- `mapToLong()` — преобразует в LongStream
- `mapToDouble()` — преобразует в DoubleStream

```java
// Вычисление среднего возраста
double averageAge = people.stream()
    .mapToInt(Person::getAge)
    .average()
    .orElse(0.0);
```

### filter

Фильтрует элементы stream по заданному предикату:

```java
// Простая фильтрация
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList()); // [2, 4, 6]

// Фильтрация объектов
List<Person> adults = people.stream()
    .filter(person -> person.getAge() >= 18)
    .collect(Collectors.toList());

// Множественные условия
List<Person> filtered = people.stream()
    .filter(person -> person.getAge() >= 18)
    .filter(person -> person.getCity().equals("Moscow"))
    .filter(person -> person.getSalary() > 50000)
    .collect(Collectors.toList());

// Использование method reference
List<String> nonEmptyStrings = strings.stream()
    .filter(s -> !s.isEmpty())
    .collect(Collectors.toList());

// Фильтрация null значений
List<String> nonNulls = strings.stream()
    .filter(Objects::nonNull)
    .collect(Collectors.toList());
```

### flatMap

Преобразует каждый элемент в stream и объединяет все получившиеся streams в один:

```java
// Разворачивание вложенных списков
List<List<Integer>> nestedList = Arrays.asList(
    Arrays.asList(1, 2),
    Arrays.asList(3, 4),
    Arrays.asList(5, 6)
);
List<Integer> flatList = nestedList.stream()
    .flatMap(List::stream)
    .collect(Collectors.toList()); // [1, 2, 3, 4, 5, 6]

// Разбиение строк на слова
List<String> sentences = Arrays.asList(
    "Hello World",
    "Stream API",
    "Java Programming"
);
List<String> words = sentences.stream()
    .flatMap(sentence -> Arrays.stream(sentence.split(" ")))
    .collect(Collectors.toList());
// ["Hello", "World", "Stream", "API", "Java", "Programming"]

// Получение всех книг от всех авторов
List<Author> authors = getAuthors();
List<Book> allBooks = authors.stream()
    .flatMap(author -> author.getBooks().stream())
    .collect(Collectors.toList());

// Комбинирование элементов
List<String> first = Arrays.asList("A", "B");
List<String> second = Arrays.asList("1", "2");
List<String> combinations = first.stream()
    .flatMap(a -> second.stream().map(b -> a + b))
    .collect(Collectors.toList()); // ["A1", "A2", "B1", "B2"]
```

**Специализированные версии:**
- `flatMapToInt()` — разворачивает в IntStream
- `flatMapToLong()` — разворачивает в LongStream
- `flatMapToDouble()` — разворачивает в DoubleStream

```java
List<String> numbers = Arrays.asList("1,2,3", "4,5,6");
int sum = numbers.stream()
    .flatMapToInt(s -> Arrays.stream(s.split(","))
        .mapToInt(Integer::parseInt))
    .sum(); // 21
```

### distinct

Удаляет дубликаты (использует `equals()` и `hashCode()`):

```java
// Уникальные элементы
List<Integer> numbers = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 5, 5);
List<Integer> unique = numbers.stream()
    .distinct()
    .collect(Collectors.toList()); // [1, 2, 3, 4, 5]

// Уникальные объекты
List<Person> uniquePeople = people.stream()
    .distinct() // Требует правильной реализации equals/hashCode
    .collect(Collectors.toList());

// Уникальные значения по полю
List<String> uniqueCities = people.stream()
    .map(Person::getCity)
    .distinct()
    .collect(Collectors.toList());
```

### sorted

Сортирует элементы stream:

```java
// Естественная сортировка
List<Integer> sorted = numbers.stream()
    .sorted()
    .collect(Collectors.toList());

// Обратная сортировка
List<Integer> reverseSorted = numbers.stream()
    .sorted(Comparator.reverseOrder())
    .collect(Collectors.toList());

// Сортировка объектов с Comparator
List<Person> sortedByAge = people.stream()
    .sorted(Comparator.comparing(Person::getAge))
    .collect(Collectors.toList());

// Сложная сортировка
List<Person> sorted = people.stream()
    .sorted(Comparator
        .comparing(Person::getCity)
        .thenComparing(Person::getAge)
        .thenComparing(Person::getName))
    .collect(Collectors.toList());

// Сортировка с null-safe comparator
List<Person> sortedNullSafe = people.stream()
    .sorted(Comparator.comparing(Person::getName, 
        Comparator.nullsLast(String::compareTo)))
    .collect(Collectors.toList());
```

> **Важно**: `sorted()` — это состоятельная операция (stateful operation), требующая обработки всех элементов перед передачей дальше. Это может снизить производительность на больших stream.

### peek

Выполняет действие для каждого элемента без изменения stream (используется для отладки и логирования):

```java
// Отладка цепочки операций
List<String> result = words.stream()
    .peek(word -> System.out.println("Original: " + word))
    .map(String::toUpperCase)
    .peek(word -> System.out.println("Uppercase: " + word))
    .filter(word -> word.length() > 3)
    .peek(word -> System.out.println("Filtered: " + word))
    .collect(Collectors.toList());

// Изменение состояния объектов (антипаттерн для stream, но технически возможно)
people.stream()
    .peek(person -> person.setProcessed(true))
    .forEach(System.out::println);

// Логирование
List<Integer> processed = numbers.stream()
    .peek(n -> logger.debug("Processing: {}", n))
    .map(n -> n * 2)
    .peek(n -> logger.debug("After mapping: {}", n))
    .collect(Collectors.toList());
```

> **Важно**: `peek()` — промежуточная операция, она не запустится без терминальной операции. Не полагайтесь на побочные эффекты в `peek()` для критичной логики.

### limit и skip

`limit()` ограничивает количество элементов, `skip()` пропускает первые N элементов:

```java
// Первые 5 элементов
List<Integer> first5 = numbers.stream()
    .limit(5)
    .collect(Collectors.toList());

// Пропустить первые 3 элемента
List<Integer> afterSkip = numbers.stream()
    .skip(3)
    .collect(Collectors.toList());

// Пагинация: страница 2, размер 10
int page = 2;
int pageSize = 10;
List<Item> pageItems = items.stream()
    .skip((long) (page - 1) * pageSize)
    .limit(pageSize)
    .collect(Collectors.toList());

// Генерация ограниченного количества случайных чисел
List<Double> randomList = Stream.generate(Math::random)
    .limit(10)
    .collect(Collectors.toList());

// Пропуск и ограничение для бесконечного stream
List<Integer> numbers = Stream.iterate(1, n -> n + 1)
    .skip(10)    // Пропустить 1..10
    .limit(5)    // Взять следующие 5
    .collect(Collectors.toList()); // [11, 12, 13, 14, 15]
```

## Терминальные операции

Терминальные операции (terminal operations) запускают обработку stream и возвращают результат или побочный эффект. После выполнения терминальной операции stream закрывается.

### collect

Самая мощная терминальная операция, собирающая элементы stream в коллекцию или другую структуру данных:

```java
// Сбор в List
List<String> list = stream.collect(Collectors.toList());

// Сбор в Set
Set<String> set = stream.collect(Collectors.toSet());

// Сбор в конкретную коллекцию
ArrayList<String> arrayList = stream.collect(Collectors.toCollection(ArrayList::new));
TreeSet<String> treeSet = stream.collect(Collectors.toCollection(TreeSet::new));

// Сбор в Map
Map<Integer, Person> peopleById = people.stream()
    .collect(Collectors.toMap(Person::getId, person -> person));

// Map с обработкой дубликатов ключей
Map<String, Person> peopleByName = people.stream()
    .collect(Collectors.toMap(
        Person::getName, 
        person -> person,
        (existing, replacement) -> existing // Оставить существующий
    ));

// Map с преобразованием значения
Map<Integer, String> idToName = people.stream()
    .collect(Collectors.toMap(Person::getId, Person::getName));
```

### forEach и forEachOrdered

Выполняет действие для каждого элемента:

```java
// Простой вывод
stream.forEach(System.out::println);

// Выполнение действия с побочным эффектом
people.forEach(person -> person.incrementVisitCount());

// forEach vs forEachOrdered
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Порядок не гарантируется в параллельном stream
numbers.parallelStream().forEach(System.out::println);

// Порядок гарантируется (но теряется преимущество параллелизма)
numbers.parallelStream().forEachOrdered(System.out::println);
```

> **Важно**: Предпочитайте `collect()` вместо `forEach()` для накопления результатов, так как `forEach()` с побочными эффектами может привести к проблемам с многопоточностью.

### reduce

Сворачивает stream в одно значение с помощью ассоциативной функции:

```java
// Сумма чисел
int sum = numbers.stream()
    .reduce(0, (a, b) -> a + b);
// или
int sum = numbers.stream()
    .reduce(0, Integer::sum);

// Произведение чисел
int product = numbers.stream()
    .reduce(1, (a, b) -> a * b);

// Максимальное значение
Optional<Integer> max = numbers.stream()
    .reduce((a, b) -> a > b ? a : b);
// или
Optional<Integer> max = numbers.stream()
    .reduce(Integer::max);

// Конкатенация строк
String concatenated = words.stream()
    .reduce("", (a, b) -> a + b);

// Более сложный пример: сумма длин строк
int totalLength = words.stream()
    .map(String::length)
    .reduce(0, Integer::sum);

// Reduce с combiner (для параллельных stream)
int parallelSum = numbers.parallelStream()
    .reduce(
        0,                    // identity
        (a, b) -> a + b,      // accumulator
        (a, b) -> a + b       // combiner
    );
```

**Три формы reduce:**

1. `Optional<T> reduce(BinaryOperator<T> accumulator)` — без начального значения
2. `T reduce(T identity, BinaryOperator<T> accumulator)` — с начальным значением
3. `<U> U reduce(U identity, BiFunction<U, T, U> accumulator, BinaryOperator<U> combiner)` — с преобразованием типа

### count, min, max

```java
// Подсчёт элементов
long count = stream.count();

// Минимальное значение
Optional<Integer> min = numbers.stream()
    .min(Integer::compareTo);

// Максимальное значение
Optional<Integer> max = numbers.stream()
    .max(Integer::compareTo);

// Самый молодой человек
Optional<Person> youngest = people.stream()
    .min(Comparator.comparing(Person::getAge));

// Самая длинная строка
Optional<String> longest = words.stream()
    .max(Comparator.comparing(String::length));
```

### anyMatch, allMatch, noneMatch

Проверяют условие на элементах stream:

```java
// Есть ли хотя бы один элемент, удовлетворяющий условию
boolean hasAdults = people.stream()
    .anyMatch(person -> person.getAge() >= 18);

// Все ли элементы удовлетворяют условию
boolean allAdults = people.stream()
    .allMatch(person -> person.getAge() >= 18);

// Ни один элемент не удовлетворяет условию
boolean noMinors = people.stream()
    .noneMatch(person -> person.getAge() < 18);

// Короткая цепочка: проверка заканчивается при первом несовпадении
boolean result = numbers.stream()
    .filter(n -> n > 0)
    .anyMatch(n -> n > 100); // Остановится на первом найденном
```

> **Важно**: Эти операции являются short-circuiting — они могут завершиться досрочно, не обрабатывая все элементы.

### findFirst и findAny

Находят элемент в stream:

```java
// Первый элемент
Optional<String> first = stream.findFirst();

// Любой элемент (полезно для параллельных stream)
Optional<String> any = stream.findAny();

// Первый элемент, удовлетворяющий условию
Optional<Person> firstAdult = people.stream()
    .filter(person -> person.getAge() >= 18)
    .findFirst();

// Использование с orElse
Person person = people.stream()
    .filter(p -> p.getId() == targetId)
    .findFirst()
    .orElse(null);

// Использование с orElseThrow
Person person = people.stream()
    .filter(p -> p.getId() == targetId)
    .findFirst()
    .orElseThrow(() -> new PersonNotFoundException(targetId));
```

> **Важно**: `findFirst()` гарантирует возврат первого элемента в порядке encounter order. `findAny()` может вернуть любой элемент и более эффективна в параллельных stream.

### toArray

Собирает stream в массив:

```java
// Массив Object[]
Object[] array = stream.toArray();

// Массив конкретного типа
String[] stringArray = stream.toArray(String[]::new);

// Типизированный массив с явным указанием размера
Integer[] intArray = numbers.stream()
    .toArray(size -> new Integer[size]);

// Преобразование с фильтрацией
String[] filtered = words.stream()
    .filter(w -> w.length() > 3)
    .toArray(String[]::new);
```

## Коллекторы (Collectors)

Класс `Collectors` предоставляет готовые реализации `Collector` для различных операций сбора данных.

### Базовые коллекторы

```java
// Сбор в List
List<String> list = stream.collect(Collectors.toList());

// Сбор в неизменяемый List (Java 10+)
List<String> immutableList = stream.collect(Collectors.toUnmodifiableList());

// Сбор в Set
Set<String> set = stream.collect(Collectors.toSet());

// Сбор в неизменяемый Set
Set<String> immutableSet = stream.collect(Collectors.toUnmodifiableSet());

// Сбор в конкретную коллекцию
LinkedList<String> linkedList = stream
    .collect(Collectors.toCollection(LinkedList::new));

// Объединение строк
String joined = words.stream()
    .collect(Collectors.joining());

// Объединение с разделителем
String joinedWithComma = words.stream()
    .collect(Collectors.joining(", "));

// Объединение с префиксом и суффиксом
String html = words.stream()
    .collect(Collectors.joining(", ", "[", "]")); // "[word1, word2, word3]"
```

### Группировка и разбиение

```java
// Группировка по полю
Map<String, List<Person>> byCity = people.stream()
    .collect(Collectors.groupingBy(Person::getCity));

// Группировка с подсчётом
Map<String, Long> countByCity = people.stream()
    .collect(Collectors.groupingBy(
        Person::getCity,
        Collectors.counting()
    ));

// Группировка с преобразованием значений
Map<String, List<String>> namesByCity = people.stream()
    .collect(Collectors.groupingBy(
        Person::getCity,
        Collectors.mapping(Person::getName, Collectors.toList())
    ));

// Многоуровневая группировка
Map<String, Map<Integer, List<Person>>> byCityAndAge = people.stream()
    .collect(Collectors.groupingBy(
        Person::getCity,
        Collectors.groupingBy(Person::getAge)
    ));

// Группировка с сортировкой ключей
Map<String, List<Person>> sortedByCity = people.stream()
    .collect(Collectors.groupingBy(
        Person::getCity,
        TreeMap::new,
        Collectors.toList()
    ));

// Разбиение (partitioning) по предикату
Map<Boolean, List<Person>> partitioned = people.stream()
    .collect(Collectors.partitioningBy(person -> person.getAge() >= 18));

List<Person> adults = partitioned.get(true);
List<Person> minors = partitioned.get(false);

// Разбиение с подсчётом
Map<Boolean, Long> count = people.stream()
    .collect(Collectors.partitioningBy(
        person -> person.getAge() >= 18,
        Collectors.counting()
    ));
```

### Объединение и агрегация

```java
// Суммирование
int totalAge = people.stream()
    .collect(Collectors.summingInt(Person::getAge));

// Среднее значение
double averageAge = people.stream()
    .collect(Collectors.averagingInt(Person::getAge));

// Максимум/минимум
Optional<Person> oldest = people.stream()
    .collect(Collectors.maxBy(Comparator.comparing(Person::getAge)));

// Статистика
IntSummaryStatistics stats = people.stream()
    .collect(Collectors.summarizingInt(Person::getAge));
System.out.println("Count: " + stats.getCount());
System.out.println("Sum: " + stats.getSum());
System.out.println("Min: " + stats.getMin());
System.out.println("Max: " + stats.getMax());
System.out.println("Average: " + stats.getAverage());

// Собственная редукция
Optional<Person> result = people.stream()
    .collect(Collectors.reducing(
        (p1, p2) -> p1.getAge() > p2.getAge() ? p1 : p2
    ));

// Коллектор с преобразованием (mapping)
List<String> upperNames = people.stream()
    .collect(Collectors.mapping(
        person -> person.getName().toUpperCase(),
        Collectors.toList()
    ));

// Фильтрация в коллекторе (Java 9+)
List<Person> adults = people.stream()
    .collect(Collectors.filtering(
        person -> person.getAge() >= 18,
        Collectors.toList()
    ));

// flatMapping (Java 9+)
List<String> allPhoneNumbers = people.stream()
    .collect(Collectors.flatMapping(
        person -> person.getPhoneNumbers().stream(),
        Collectors.toList()
    ));

// teeing - объединение двух коллекторов (Java 12+)
record MinMax(Person youngest, Person oldest) {}

MinMax minMax = people.stream()
    .collect(Collectors.teeing(
        Collectors.minBy(Comparator.comparing(Person::getAge)),
        Collectors.maxBy(Comparator.comparing(Person::getAge)),
        (min, max) -> new MinMax(min.orElse(null), max.orElse(null))
    ));
```

### Пользовательские коллекторы

```java
// Создание пользовательского коллектора
Collector<Person, ?, Map<String, Integer>> customCollector = 
    Collector.of(
        HashMap::new,                           // supplier
        (map, person) -> map.merge(             // accumulator
            person.getCity(), 
            1, 
            Integer::sum
        ),
        (map1, map2) -> {                       // combiner
            map2.forEach((city, count) -> 
                map1.merge(city, count, Integer::sum));
            return map1;
        }
    );

Map<String, Integer> cityCount = people.stream()
    .collect(customCollector);

// Коллектор с финализацией
Collector<Person, ?, String> namesCollector = Collector.of(
    StringBuilder::new,                         // supplier
    (sb, person) -> {                          // accumulator
        if (sb.length() > 0) sb.append(", ");
        sb.append(person.getName());
    },
    (sb1, sb2) -> sb1.append(", ").append(sb2), // combiner
    StringBuilder::toString                     // finisher
);

String allNames = people.stream().collect(namesCollector);
```

## Примитивные стримы

### IntStream, LongStream, DoubleStream

Специализированные стримы для примитивных типов, избегающие boxing/unboxing:

```java
// Создание IntStream
IntStream intStream1 = IntStream.of(1, 2, 3, 4, 5);
IntStream intStream2 = IntStream.range(1, 10);      // 1..9
IntStream intStream3 = IntStream.rangeClosed(1, 10); // 1..10
IntStream intStream4 = Arrays.stream(new int[]{1, 2, 3});

// LongStream
LongStream longStream = LongStream.range(1L, 1000000L);

// DoubleStream
DoubleStream doubleStream = DoubleStream.of(1.0, 2.0, 3.0);

// Генерация
IntStream randomInts = new Random().ints(10, 0, 100); // 10 чисел от 0 до 100
DoubleStream randomDoubles = new Random().doubles(5);
```

### Преобразование между стримами

```java
// Stream<Integer> -> IntStream
IntStream intStream = Stream.of(1, 2, 3, 4, 5)
    .mapToInt(Integer::intValue);

// IntStream -> Stream<Integer>
Stream<Integer> stream = IntStream.of(1, 2, 3, 4, 5)
    .boxed();

// IntStream -> LongStream
LongStream longStream = IntStream.of(1, 2, 3)
    .asLongStream();

// IntStream -> DoubleStream
DoubleStream doubleStream = IntStream.of(1, 2, 3)
    .asDoubleStream();

// Из коллекции объектов в примитивный stream
List<Person> people = getPeople();
IntStream ages = people.stream()
    .mapToInt(Person::getAge);

DoubleStream salaries = people.stream()
    .mapToDouble(Person::getSalary);
```

### Статистические операции

```java
// Сумма
int sum = IntStream.of(1, 2, 3, 4, 5).sum(); // 15

// Среднее
OptionalDouble average = IntStream.of(1, 2, 3, 4, 5).average();
double avg = average.orElse(0.0); // 3.0

// Минимум и максимум
OptionalInt min = IntStream.of(1, 2, 3, 4, 5).min(); // 1
OptionalInt max = IntStream.of(1, 2, 3, 4, 5).max(); // 5

// Статистика за один проход
IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5)
    .summaryStatistics();

System.out.println("Count: " + stats.getCount());     // 5
System.out.println("Sum: " + stats.getSum());         // 15
System.out.println("Min: " + stats.getMin());         // 1
System.out.println("Max: " + stats.getMax());         // 5
System.out.println("Average: " + stats.getAverage()); // 3.0

// Для коллекций
DoubleSummaryStatistics salaryStats = people.stream()
    .mapToDouble(Person::getSalary)
    .summaryStatistics();
```

## Параллельные стримы

### Создание параллельных стримов

```java
// Из коллекции
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
Stream<Integer> parallelStream = numbers.parallelStream();

// Преобразование последовательного в параллельный
Stream<Integer> parallel = numbers.stream().parallel();

// Обратное преобразование
Stream<Integer> sequential = parallelStream.sequential();

// Проверка параллельности
boolean isParallel = stream.isParallel();
```

### Fork/Join Framework

Параллельные стримы используют общий `ForkJoinPool.commonPool()`:

```java
// Настройка размера пула (глобально для всех параллельных стримов)
System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "4");

// Использование собственного пула
ForkJoinPool customPool = new ForkJoinPool(8);
List<Integer> result = customPool.submit(() ->
    numbers.parallelStream()
        .map(n -> heavyComputation(n))
        .collect(Collectors.toList())
).join();
```

### Когда использовать параллельные стримы

**Подходящие случаи:**

1. **CPU-intensive операции** с независимыми задачами
2. **Большие объёмы данных** (обычно > 10000 элементов)
3. **Простые операции** без состояния и побочных эффектов
4. **Легко разделяемые источники данных** (ArrayList, массивы, IntStream.range)

```java
// Хороший пример: вычислительно сложная операция на большом массиве
List<BigInteger> primes = IntStream.rangeClosed(1, 1_000_000)
    .parallel()
    .mapToObj(BigInteger::valueOf)
    .filter(n -> n.isProbablePrime(100))
    .collect(Collectors.toList());

// Хороший пример: обработка изображений
List<Image> processed = images.parallelStream()
    .map(image -> applyFilter(image))
    .collect(Collectors.toList());
```

**Неподходящие случаи:**

1. **I/O операции** (блокирующие вызовы)
2. **Малые объёмы данных** (overhead > выигрыш)
3. **Операции с общим состоянием** (требуют синхронизации)
4. **Трудноделимые источники** (LinkedList, Stream.iterate)

```java
// Плохой пример: I/O операции
List<String> badExample = urls.parallelStream()
    .map(url -> httpClient.fetch(url)) // Блокирующий вызов
    .collect(Collectors.toList());

// Плохой пример: операции с общим состоянием
List<Integer> numbers = new ArrayList<>();
IntStream.range(0, 1000)
    .parallel()
    .forEach(numbers::add); // Race condition!
```

### Производительность и подводные камни

```java
// 1. Правильный порядок операций
// Плохо: сортировка в параллельном stream
long count = numbers.parallelStream()
    .sorted()           // Дорогостоящая операция
    .filter(n -> n > 100)
    .count();

// Хорошо: фильтрация перед сортировкой
long count = numbers.parallelStream()
    .filter(n -> n > 100)
    .sorted()
    .count();

// 2. Источники данных
// Отлично разделяется
ArrayList<Integer> arrayList = new ArrayList<>();
arrayList.parallelStream(); // Хорошая производительность

// Плохо разделяется
LinkedList<Integer> linkedList = new LinkedList<>();
linkedList.parallelStream(); // Плохая производительность

// 3. Измерение производительности
// Всегда измеряйте!
long start = System.nanoTime();
long result = numbers.parallelStream()
    .filter(n -> n > 0)
    .count();
long end = System.nanoTime();
System.out.println("Time: " + (end - start) / 1_000_000 + "ms");

// 4. Avoid boxing
// Плохо
long sum = numbers.parallelStream()
    .reduce(0, Integer::sum); // Boxing

// Хорошо
long sum = numbers.parallelStream()
    .mapToInt(Integer::intValue)
    .sum(); // Без boxing
```

## Лучшие практики

1. **Предпочитайте декларативный стиль императивному**
   ```java
   // Плохо
   List<String> result = new ArrayList<>();
   for (Person person : people) {
       if (person.getAge() >= 18) {
           result.add(person.getName().toUpperCase());
       }
   }
   
   // Хорошо
   List<String> result = people.stream()
       .filter(person -> person.getAge() >= 18)
       .map(Person::getName)
       .map(String::toUpperCase)
       .collect(Collectors.toList());
   ```

2. **Используйте method references вместо лямбд где возможно**
   ```java
   // Можно
   list.stream().map(s -> s.toUpperCase())
   
   // Лучше
   list.stream().map(String::toUpperCase)
   ```

3. **Избегайте побочных эффектов в операциях stream**
   ```java
   // Плохо: изменение внешнего состояния
   List<String> results = new ArrayList<>();
   stream.filter(s -> s.length() > 3)
       .forEach(results::add);
   
   // Хорошо: используйте collect
   List<String> results = stream
       .filter(s -> s.length() > 3)
       .collect(Collectors.toList());
   ```

4. **Используйте примитивные стримы для производительности**
   ```java
   // Менее эффективно
   int sum = numbers.stream()
       .reduce(0, Integer::sum);
   
   // Более эффективно
   int sum = numbers.stream()
       .mapToInt(Integer::intValue)
       .sum();
   ```

5. **Порядок операций имеет значение**
   ```java
   // Неоптимально: сортировка всех элементов
   List<String> result = words.stream()
       .sorted()
       .filter(w -> w.length() > 5)
       .limit(10)
       .collect(Collectors.toList());
   
   // Оптимально: фильтрация перед сортировкой
   List<String> result = words.stream()
       .filter(w -> w.length() > 5)
       .sorted()
       .limit(10)
       .collect(Collectors.toList());
   ```

6. **Используйте `Optional.orElseGet()` вместо `orElse()` для дорогих вычислений**
   ```java
   // Плохо: вычисляется всегда
   String result = optional.orElse(expensiveComputation());
   
   // Хорошо: вычисляется только при необходимости
   String result = optional.orElseGet(() -> expensiveComputation());
   ```

7. **Не переиспользуйте stream**
   ```java
   // Ошибка!
   Stream<String> stream = list.stream();
   long count = stream.count();
   stream.forEach(System.out::println); // IllegalStateException!
   
   // Правильно: создать новый stream
   long count = list.stream().count();
   list.stream().forEach(System.out::println);
   ```

8. **Осторожно с бесконечными стримами**
   ```java
   // Правильно: ограничение бесконечного stream
   List<Integer> numbers = Stream.iterate(0, n -> n + 1)
       .limit(100)
       .collect(Collectors.toList());
   
   // Ошибка: бесконечный цикл!
   // Stream.iterate(0, n -> n + 1).forEach(System.out::println);
   ```

## Типичные ошибки

1. **Изменение источника данных во время работы stream**
   ```java
   // ОШИБКА: ConcurrentModificationException
   list.stream()
       .forEach(item -> list.remove(item));
   
   // Правильно: создать новую коллекцию
   List<Item> toRemove = list.stream()
       .filter(predicate)
       .collect(Collectors.toList());
   list.removeAll(toRemove);
   ```

2. **Неправильная обработка null значений**
   ```java
   // Может выбросить NullPointerException
   list.stream()
       .map(Person::getName)
       .collect(Collectors.toList());
   
   // Правильно: фильтрация null
   list.stream()
       .filter(Objects::nonNull)
       .map(Person::getName)
       .collect(Collectors.toList());
   ```

3. **Использование состоятельных операций в неправильном порядке**
   ```java
   // Неэффективно
   stream.sorted().limit(10)
   
   // Всё равно неэффективно (sorted требует все элементы)
   stream.limit(10).sorted()
   ```

4. **Неправильное использование `flatMap`**
   ```java
   // ОШИБКА: возвращает Stream<Stream<String>>
   Stream<Stream<String>> wrong = lists.stream()
       .map(List::stream);
   
   // Правильно: flatMap разворачивает вложенные стримы
   Stream<String> correct = lists.stream()
       .flatMap(List::stream);
   ```

5. **Игнорирование Optional результата**
   ```java
   // Может выбросить NoSuchElementException
   String name = people.stream()
       .filter(p -> p.getId() == id)
       .findFirst()
       .get(); // Опасно!
   
   // Правильно
   String name = people.stream()
       .filter(p -> p.getId() == id)
       .findFirst()
       .orElse("Unknown");
   ```

## Практические примеры

### Пример 1: Обработка CSV данных

```java
public class CsvProcessor {
    public Map<String, DoubleSummaryStatistics> processSales(String csvContent) {
        return csvContent.lines()
            .skip(1) // Пропустить заголовок
            .map(line -> line.split(","))
            .filter(parts -> parts.length >= 3)
            .collect(Collectors.groupingBy(
                parts -> parts[0], // Группировка по категории
                Collectors.summarizingDouble(parts -> 
                    Double.parseDouble(parts[2])) // Статистика по сумме
            ));
    }
}
```

### Пример 2: Топ-N элементов

```java
public List<Product> getTopSellingProducts(List<Order> orders, int n) {
    return orders.stream()
        .flatMap(order -> order.getItems().stream())
        .collect(Collectors.groupingBy(
            OrderItem::getProduct,
            Collectors.summingInt(OrderItem::getQuantity)
        ))
        .entrySet().stream()
        .sorted(Map.Entry.<Product, Integer>comparingByValue().reversed())
        .limit(n)
        .map(Map.Entry::getKey)
        .collect(Collectors.toList());
}
```

### Пример 3: Валидация и фильтрация

```java
public List<User> getValidActiveUsers(List<User> users) {
    return users.stream()
        .filter(Objects::nonNull)
        .filter(User::isActive)
        .filter(user -> user.getEmail() != null)
        .filter(user -> user.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$"))
        .filter(user -> user.getAge() >= 18)
        .distinct()
        .collect(Collectors.toList());
}
```

### Пример 4: Построение иерархии

```java
public Map<Department, Map<String, List<Employee>>> buildHierarchy(
        List<Employee> employees) {
    return employees.stream()
        .collect(Collectors.groupingBy(
            Employee::getDepartment,
            Collectors.groupingBy(
                Employee::getPosition,
                Collectors.toList()
            )
        ));
}
```

### Пример 5: Агрегация с преобразованием

```java
public String generateReport(List<Transaction> transactions) {
    Map<String, Double> categoryTotals = transactions.stream()
        .collect(Collectors.groupingBy(
            Transaction::getCategory,
            Collectors.summingDouble(Transaction::getAmount)
        ));
    
    return categoryTotals.entrySet().stream()
        .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
        .map(entry -> String.format("%s: $%.2f", 
            entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("\n", 
            "=== Transaction Report ===\n", 
            "\n======================"));
}
```

## Вопросы на собеседовании

1. **В чём разница между промежуточными и терминальными операциями?**
   
   *Ответ:* Промежуточные операции (intermediate) возвращают новый stream и выполняются лениво — только при вызове терминальной операции. Примеры: `map`, `filter`, `sorted`. Терминальные операции (terminal) запускают обработку stream и возвращают результат или выполняют побочный эффект. После терминальной операции stream закрывается. Примеры: `collect`, `forEach`, `reduce`.

2. **Чем отличаются `map` и `flatMap`?**
   
   *Ответ:* `map` преобразует каждый элемент в один новый элемент (`Stream<T>` → `Stream<R>`). `flatMap` преобразует каждый элемент в stream и затем "разворачивает" все получившиеся стримы в один плоский stream (`Stream<T>` → `Stream<Stream<R>>` → `Stream<R>`). Используется для работы с вложенными структурами.

3. **Можно ли переиспользовать stream после терминальной операции?**
   
   *Ответ:* Нет, stream можно использовать только один раз. После выполнения терминальной операции попытка использовать stream снова приведёт к `IllegalStateException`. Нужно создать новый stream.

4. **Что такое ленивое вычисление в контексте Stream API?**
   
   *Ответ:* Промежуточные операции не выполняются сразу при вызове, а формируют pipeline операций. Реальная обработка начинается только при вызове терминальной операции. Это позволяет оптимизировать выполнение (например, пропустить ненужные элементы при использовании `limit`).

5. **Когда стоит использовать параллельные стримы?**
   
   *Ответ:* Параллельные стримы эффективны для CPU-intensive операций на больших объёмах данных (обычно > 10000 элементов) с независимыми задачами. Источник данных должен хорошо разделяться (ArrayList, массивы). Не стоит использовать для I/O операций, малых объёмов данных или операций с общим состоянием.

6. **В чём преимущество примитивных стримов (IntStream, LongStream, DoubleStream)?**
   
   *Ответ:* Примитивные стримы избегают autoboxing/unboxing, что значительно улучшает производительность и снижает потребление памяти при работе с примитивами. Также предоставляют специализированные методы (`sum`, `average`, `summaryStatistics`).

7. **Что делает метод `peek` и когда его использовать?**
   
   *Ответ:* `peek` — промежуточная операция, выполняющая действие для каждого элемента без изменения stream. Используется в основном для отладки (логирование промежуточных результатов). Не следует полагаться на побочные эффекты в `peek` для бизнес-логики.

8. **В чём разница между `findFirst` и `findAny`?**
   
   *Ответ:* `findFirst` гарантированно возвращает первый элемент в порядке encounter order. `findAny` может вернуть любой элемент из stream и более эффективна в параллельных стримах, так как не требует соблюдения порядка.

9. **Как работает метод `reduce`?**
   
   *Ответ:* `reduce` сворачивает stream в одно значение, последовательно применяя ассоциативную функцию. Принимает начальное значение (identity), аккумулятор (объединяет текущий результат с новым элементом) и опционально combiner (для параллельных стримов).

10. **Что такое Collector и зачем он нужен?**
    
    *Ответ:* `Collector` — это объект, описывающий как собрать элементы stream в результирующую структуру. Класс `Collectors` предоставляет готовые реализации для типовых задач (`toList`, `groupingBy`, `joining`). Можно создавать собственные коллекторы через `Collector.of`.

11. **В чём разница между `anyMatch`, `allMatch` и `noneMatch`?**
    
    *Ответ:* 
    - `anyMatch` — возвращает `true`, если хотя бы один элемент удовлетворяет условию
    - `allMatch` — возвращает `true`, если все элементы удовлетворяют условию
    - `noneMatch` — возвращает `true`, если ни один элемент не удовлетворяет условию
    
    Все три — short-circuiting операции, могут завершиться досрочно.

12. **Почему порядок операций в stream важен для производительности?**
    
    *Ответ:* Разный порядок может значительно влиять на количество обрабатываемых элементов. Например, `filter` перед `map` обрабатывает меньше элементов, чем `map` перед `filter`. `limit` после `sorted` требует сортировки всех элементов, а фильтрация перед `sorted` уменьшает объём сортируемых данных.

13. **Как обработать исключения в Stream API?**
    
    *Ответ:* Stream API не предоставляет встроенных средств для обработки checked exceptions. Можно:
    - Обернуть в unchecked exception
    - Использовать try-catch внутри лямбды
    - Создать wrapper-метод, обрабатывающий исключения
    - Использовать библиотеки типа Vavr
    
    ```java
    list.stream()
        .map(item -> {
            try {
                return processItem(item);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
        .collect(Collectors.toList());
    ```

14. **Чем отличается `Collectors.toList()` от `Collectors.toUnmodifiableList()`?**
    
    *Ответ:* `toList()` создаёт изменяемый список (обычно ArrayList), к которому можно добавлять или удалять элементы. `toUnmodifiableList()` (Java 10+) создаёт неизменяемый список — попытка модификации вызовет `UnsupportedOperationException`. Неизменяемые коллекции более безопасны и могут быть оптимизированы JVM.

15. **Что такое ForkJoinPool и как он используется в параллельных стримах?**
    
    *Ответ:* `ForkJoinPool` — это framework для параллельного выполнения задач, использующий алгоритм work-stealing. Параллельные стримы по умолчанию используют общий `ForkJoinPool.commonPool()`. Можно настроить размер пула через системное свойство `java.util.concurrent.ForkJoinPool.common.parallelism` или использовать собственный пул через `submit()`.

---

[← Назад к разделу Java Core](README.md)
