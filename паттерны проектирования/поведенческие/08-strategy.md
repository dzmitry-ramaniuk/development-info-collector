# Strategy (Стратегия)

Strategy — поведенческий паттерн проектирования, который определяет семейство алгоритмов, инкапсулирует каждый из них и делает их взаимозаменяемыми. Паттерн позволяет выбирать алгоритм во время выполнения программы.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Примеры использования](#примеры-использования)
6. [Strategy vs State](#strategy-vs-state)
7. [Современный подход с лямбдами](#современный-подход-с-лямбдами)
8. [Преимущества и недостатки](#преимущества-и-недостатки)
9. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Strategy используется когда:
- Есть несколько родственных классов, отличающихся только поведением
- Нужны разные варианты алгоритма
- Алгоритм использует данные, о которых клиент не должен знать
- В классе определено множество поведений в виде условных операторов

**Типичные примеры использования:**
- Алгоритмы сортировки (QuickSort, MergeSort, BubbleSort)
- Стратегии валидации данных
- Способы сжатия файлов
- Алгоритмы маршрутизации
- Стратегии ценообразования
- Способы оплаты в e-commerce

## Проблема, которую решает

### Проблема: Множественные условные операторы

```java
public class PaymentProcessor {
    public void processPayment(String paymentType, double amount) {
        if (paymentType.equals("creditCard")) {
            // Логика обработки кредитной карты
            System.out.println("Processing credit card payment: $" + amount);
            // Проверка CVV, срока действия и т.д.
        } else if (paymentType.equals("paypal")) {
            // Логика обработки PayPal
            System.out.println("Processing PayPal payment: $" + amount);
            // Проверка аккаунта, OAuth и т.д.
        } else if (paymentType.equals("cryptocurrency")) {
            // Логика обработки криптовалюты
            System.out.println("Processing cryptocurrency payment: $" + amount);
            // Проверка кошелька, подтверждение транзакции и т.д.
        }
        // ... ещё 10 способов оплаты
    }
}
```

**Проблемы этого подхода:**
- Нарушение Open/Closed Principle — при добавлении нового способа оплаты нужно изменять существующий код
- Сложность тестирования — нужно тестировать все ветки условий
- Сложность понимания — большие методы с множеством условий
- Дублирование кода — похожая логика в разных ветках

### Решение: Паттерн Strategy

Выделить каждый алгоритм в отдельный класс и сделать их взаимозаменяемыми.

## Структура паттерна

```java
// Интерфейс стратегии
interface PaymentStrategy {
    void pay(double amount);
}

// Конкретные стратегии
class CreditCardStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Processing credit card payment: $" + amount);
        // Логика обработки кредитной карты
    }
}

class PayPalStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Processing PayPal payment: $" + amount);
        // Логика обработки PayPal
    }
}

class CryptocurrencyStrategy implements PaymentStrategy {
    @Override
    public void pay(double amount) {
        System.out.println("Processing cryptocurrency payment: $" + amount);
        // Логика обработки криптовалюты
    }
}

// Контекст
class PaymentProcessor {
    private PaymentStrategy strategy;
    
    public void setStrategy(PaymentStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void processPayment(double amount) {
        if (strategy == null) {
            throw new IllegalStateException("Payment strategy not set");
        }
        strategy.pay(amount);
    }
}

// Использование
PaymentProcessor processor = new PaymentProcessor();
processor.setStrategy(new CreditCardStrategy());
processor.processPayment(100.0);

processor.setStrategy(new PayPalStrategy());
processor.processPayment(50.0);
```

**Компоненты паттерна:**
1. **Strategy (PaymentStrategy)** — общий интерфейс для всех алгоритмов
2. **ConcreteStrategy (CreditCardStrategy, PayPalStrategy)** — конкретные реализации алгоритмов
3. **Context (PaymentProcessor)** — использует Strategy для выполнения операций

## Реализация

### Пример 1: Сортировка

```java
// Интерфейс стратегии сортировки
interface SortStrategy {
    void sort(int[] array);
}

// Быстрая сортировка
class QuickSortStrategy implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("Sorting using QuickSort");
        quickSort(array, 0, array.length - 1);
    }
    
    private void quickSort(int[] arr, int low, int high) {
        if (low < high) {
            int pi = partition(arr, low, high);
            quickSort(arr, low, pi - 1);
            quickSort(arr, pi + 1, high);
        }
    }
    
    private int partition(int[] arr, int low, int high) {
        int pivot = arr[high];
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (arr[j] < pivot) {
                i++;
                int temp = arr[i];
                arr[i] = arr[j];
                arr[j] = temp;
            }
        }
        int temp = arr[i + 1];
        arr[i + 1] = arr[high];
        arr[high] = temp;
        return i + 1;
    }
}

// Сортировка слиянием
class MergeSortStrategy implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("Sorting using MergeSort");
        mergeSort(array, 0, array.length - 1);
    }
    
    private void mergeSort(int[] arr, int left, int right) {
        if (left < right) {
            int mid = (left + right) / 2;
            mergeSort(arr, left, mid);
            mergeSort(arr, mid + 1, right);
            merge(arr, left, mid, right);
        }
    }
    
    private void merge(int[] arr, int left, int mid, int right) {
        // Реализация слияния
    }
}

// Сортировка пузырьком
class BubbleSortStrategy implements SortStrategy {
    @Override
    public void sort(int[] array) {
        System.out.println("Sorting using BubbleSort");
        for (int i = 0; i < array.length - 1; i++) {
            for (int j = 0; j < array.length - i - 1; j++) {
                if (array[j] > array[j + 1]) {
                    int temp = array[j];
                    array[j] = array[j + 1];
                    array[j + 1] = temp;
                }
            }
        }
    }
}

// Контекст сортировщика
class Sorter {
    private SortStrategy strategy;
    
    public Sorter(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void setStrategy(SortStrategy strategy) {
        this.strategy = strategy;
    }
    
    public void sort(int[] array) {
        strategy.sort(array);
    }
}

// Использование
int[] data = {64, 34, 25, 12, 22, 11, 90};

Sorter sorter = new Sorter(new QuickSortStrategy());
sorter.sort(data); // QuickSort

sorter.setStrategy(new MergeSortStrategy());
sorter.sort(data); // MergeSort

// Выбор стратегии в зависимости от размера данных
if (data.length < 10) {
    sorter.setStrategy(new BubbleSortStrategy());
} else if (data.length < 1000) {
    sorter.setStrategy(new QuickSortStrategy());
} else {
    sorter.setStrategy(new MergeSortStrategy());
}
```

### Пример 2: Валидация данных

```java
// Интерфейс стратегии валидации
interface ValidationStrategy {
    boolean validate(String value);
    String getErrorMessage();
}

// Валидация email
class EmailValidationStrategy implements ValidationStrategy {
    private static final String EMAIL_PATTERN = 
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    
    @Override
    public boolean validate(String value) {
        return value != null && value.matches(EMAIL_PATTERN);
    }
    
    @Override
    public String getErrorMessage() {
        return "Invalid email format";
    }
}

// Валидация телефона
class PhoneValidationStrategy implements ValidationStrategy {
    private static final String PHONE_PATTERN = "^\\+?[1-9]\\d{1,14}$";
    
    @Override
    public boolean validate(String value) {
        return value != null && value.matches(PHONE_PATTERN);
    }
    
    @Override
    public String getErrorMessage() {
        return "Invalid phone number format";
    }
}

// Валидация пароля
class PasswordValidationStrategy implements ValidationStrategy {
    @Override
    public boolean validate(String value) {
        if (value == null || value.length() < 8) {
            return false;
        }
        boolean hasUpper = value.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = value.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = value.chars().anyMatch(Character::isDigit);
        return hasUpper && hasLower && hasDigit;
    }
    
    @Override
    public String getErrorMessage() {
        return "Password must be at least 8 characters with upper, lower and digit";
    }
}

// Валидатор
class Validator {
    private ValidationStrategy strategy;
    
    public Validator(ValidationStrategy strategy) {
        this.strategy = strategy;
    }
    
    public boolean validate(String value) {
        if (!strategy.validate(value)) {
            System.out.println(strategy.getErrorMessage());
            return false;
        }
        return true;
    }
}

// Использование
Validator validator = new Validator(new EmailValidationStrategy());
validator.validate("user@example.com"); // true
validator.validate("invalid-email"); // false

validator = new Validator(new PasswordValidationStrategy());
validator.validate("Weak123"); // true
validator.validate("weak"); // false
```

### Пример 3: Стратегии сжатия

```java
interface CompressionStrategy {
    byte[] compress(byte[] data);
    byte[] decompress(byte[] data);
}

class ZipCompressionStrategy implements CompressionStrategy {
    @Override
    public byte[] compress(byte[] data) {
        System.out.println("Compressing using ZIP");
        // Реализация ZIP компрессии
        return data;
    }
    
    @Override
    public byte[] decompress(byte[] data) {
        System.out.println("Decompressing using ZIP");
        // Реализация ZIP декомпрессии
        return data;
    }
}

class GzipCompressionStrategy implements CompressionStrategy {
    @Override
    public byte[] compress(byte[] data) {
        System.out.println("Compressing using GZIP");
        // Реализация GZIP компрессии
        return data;
    }
    
    @Override
    public byte[] decompress(byte[] data) {
        System.out.println("Decompressing using GZIP");
        // Реализация GZIP декомпрессии
        return data;
    }
}

class Compressor {
    private CompressionStrategy strategy;
    
    public Compressor(CompressionStrategy strategy) {
        this.strategy = strategy;
    }
    
    public byte[] compress(byte[] data) {
        return strategy.compress(data);
    }
    
    public byte[] decompress(byte[] data) {
        return strategy.decompress(data);
    }
}
```

## Примеры использования

### Из Java Collections Framework

Классический пример Strategy в JDK — интерфейс `Comparator`:

```java
List<String> names = Arrays.asList("John", "Alice", "Bob", "Charlie");

// Стратегия: сортировка по алфавиту
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return s1.compareTo(s2);
    }
});

// Стратегия: сортировка по длине строки
Collections.sort(names, new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
});

// С Java 8 — через лямбды
names.sort((s1, s2) -> s1.compareTo(s2));
names.sort(Comparator.comparingInt(String::length));
```

### Из javax.servlet

`Filter` в Servlet API — это Strategy для обработки HTTP-запросов:

```java
public class AuthenticationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) {
        // Стратегия аутентификации
        // ...
        chain.doFilter(request, response);
    }
}
```

### Из LayoutManager в AWT/Swing

```java
JPanel panel = new JPanel();

// Разные стратегии размещения компонентов
panel.setLayout(new FlowLayout()); // Стратегия 1
panel.setLayout(new BorderLayout()); // Стратегия 2
panel.setLayout(new GridLayout(3, 2)); // Стратегия 3
```

## Strategy vs State

Strategy и State очень похожи по структуре, но отличаются назначением:

| Аспект | Strategy | State |
|--------|----------|-------|
| **Назначение** | Выбор алгоритма | Изменение поведения при изменении состояния |
| **Кто выбирает** | Клиент выбирает стратегию | Объект сам меняет состояние |
| **Знание о других** | Стратегии независимы | Состояния могут знать друг о друге |
| **Изменяемость** | Стратегия может меняться извне | Состояние меняется внутри объекта |
| **Количество** | Множество стратегий | Ограниченное число состояний |

**Пример Strategy:**
```java
// Клиент выбирает стратегию
PaymentProcessor processor = new PaymentProcessor();
processor.setStrategy(new CreditCardStrategy()); // Выбор клиента
processor.processPayment(100);
```

**Пример State:**
```java
// Объект сам меняет состояние
TCPConnection connection = new TCPConnection();
connection.open(); // Переход в состояние "Открыто"
connection.send(data); // Переход в состояние "Отправка"
connection.close(); // Переход в состояние "Закрыто"
```

## Современный подход с лямбдами

С появлением Java 8 многие случаи использования Strategy можно упростить с помощью лямбда-выражений:

### Старый подход (до Java 8)

```java
interface DiscountStrategy {
    double applyDiscount(double price);
}

class NoDiscountStrategy implements DiscountStrategy {
    public double applyDiscount(double price) {
        return price;
    }
}

class PercentDiscountStrategy implements DiscountStrategy {
    private double percent;
    
    public PercentDiscountStrategy(double percent) {
        this.percent = percent;
    }
    
    public double applyDiscount(double price) {
        return price * (1 - percent / 100);
    }
}

// Использование
DiscountStrategy strategy = new PercentDiscountStrategy(10);
double finalPrice = strategy.applyDiscount(100); // 90
```

### Новый подход (Java 8+)

```java
@FunctionalInterface
interface DiscountStrategy {
    double applyDiscount(double price);
}

// Использование с лямбдами
DiscountStrategy noDiscount = price -> price;
DiscountStrategy tenPercent = price -> price * 0.9;
DiscountStrategy twentyPercent = price -> price * 0.8;

double finalPrice = tenPercent.applyDiscount(100); // 90

// Или даже проще — использовать Function<Double, Double>
Function<Double, Double> discount = price -> price * 0.9;
double result = discount.apply(100.0);
```

### Пример: Фильтрация списка

```java
// До Java 8
interface FilterStrategy<T> {
    boolean test(T item);
}

class EvenNumberFilter implements FilterStrategy<Integer> {
    public boolean test(Integer number) {
        return number % 2 == 0;
    }
}

List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);
List<Integer> evenNumbers = filter(numbers, new EvenNumberFilter());

// Java 8+ с Predicate
List<Integer> evenNumbers = numbers.stream()
    .filter(n -> n % 2 == 0)
    .collect(Collectors.toList());

List<Integer> greaterThanThree = numbers.stream()
    .filter(n -> n > 3)
    .collect(Collectors.toList());
```

## Преимущества и недостатки

### Преимущества

✅ **Open/Closed Principle**
- Можно добавлять новые стратегии без изменения существующего кода

✅ **Single Responsibility**
- Изолирует детали реализации алгоритмов от кода, который их использует

✅ **Замена наследования композицией**
- Вместо множества подклассов используется композиция

✅ **Гибкость**
- Можно менять алгоритмы на лету во время выполнения

✅ **Тестируемость**
- Легко тестировать каждую стратегию независимо

✅ **Устранение условных операторов**
- Избавляет от больших switch/if-else блоков

### Недостатки

❌ **Увеличение количества классов**
- Для каждого алгоритма нужен отдельный класс

❌ **Клиент должен знать о стратегиях**
- Клиент должен понимать, чем различаются стратегии, чтобы выбрать подходящую

❌ **Усложнение простых случаев**
- Для простых алгоритмов может быть избыточным

❌ **Коммуникация между контекстом и стратегией**
- Некоторым стратегиям могут понадобиться данные, которые не нужны другим

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Strategy и когда его использовать?**

*Ответ:* Strategy — это поведенческий паттерн, который определяет семейство алгоритмов, инкапсулирует каждый и делает их взаимозаменяемыми. Используется когда:
- Есть несколько способов выполнения операции
- Нужно выбирать алгоритм во время выполнения
- Хотим избавиться от множественных условных операторов
- Алгоритмы должны быть независимыми от клиентов

**2. Какие компоненты входят в паттерн Strategy?**

*Ответ:* Три основных компонента:
- **Strategy** (интерфейс стратегии) — объявляет общий интерфейс для всех алгоритмов
- **ConcreteStrategy** (конкретная стратегия) — реализует конкретный алгоритм
- **Context** (контекст) — хранит ссылку на стратегию и делегирует ей выполнение

**3. Приведите примеры Strategy из JDK**

*Ответ:* 
- `java.util.Comparator` — стратегия сравнения объектов
- `java.io.FileFilter` — стратегия фильтрации файлов
- `javax.servlet.Filter` — стратегия обработки HTTP-запросов
- `java.awt.LayoutManager` — стратегия размещения компонентов

**4. В чём разница между Strategy и State?**

*Ответ:* 
- **Strategy**: Клиент выбирает стратегию, стратегии независимы, цель — выбор алгоритма
- **State**: Объект сам меняет состояние, состояния могут знать друг о друге, цель — изменение поведения при изменении состояния

### Продвинутые вопросы

**5. Как лямбды Java 8 упрощают использование Strategy?**

*Ответ:* Для простых стратегий с одним методом можно использовать лямбда-выражения вместо создания отдельных классов:

```java
// Старый подход
Comparator<String> byLength = new Comparator<String>() {
    public int compare(String s1, String s2) {
        return Integer.compare(s1.length(), s2.length());
    }
};

// С лямбдами
Comparator<String> byLength = (s1, s2) -> Integer.compare(s1.length(), s2.length());
// Или ещё проще
Comparator<String> byLength = Comparator.comparingInt(String::length);
```

**6. Когда НЕ стоит использовать Strategy?**

*Ответ:* 
- Когда алгоритмов всего 1-2 и они редко меняются
- Когда алгоритмы очень простые (несколько строк кода)
- Когда стратегии сильно различаются по требуемым данным
- В простых случаях лучше использовать лямбды или даже простые if-else

**7. Как передавать данные между Context и Strategy?**

*Ответ:* Есть несколько подходов:
- **Через параметры метода** — самый простой способ
- **Через конструктор стратегии** — для постоянных данных
- **Передача всего контекста** — когда нужно много данных
- **Callback-функции** — для двусторонней коммуникации

**8. Как реализовать выбор стратегии на основе конфигурации?**

*Ответ:* Использовать фабрику стратегий:

```java
class StrategyFactory {
    private static Map<String, Supplier<PaymentStrategy>> strategies = Map.of(
        "creditCard", CreditCardStrategy::new,
        "paypal", PayPalStrategy::new,
        "crypto", CryptocurrencyStrategy::new
    );
    
    public static PaymentStrategy getStrategy(String type) {
        Supplier<PaymentStrategy> supplier = strategies.get(type);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown strategy: " + type);
        }
        return supplier.get();
    }
}
```

**9. Может ли Strategy иметь состояние?**

*Ответ:* Да, стратегия может иметь состояние:
- **Stateless** — стратегия без состояния, можно переиспользовать экземпляр
- **Stateful** — стратегия с состоянием, нужно создавать новый экземпляр для каждого использования

Пример со состоянием:
```java
class DiscountStrategy {
    private double discountPercent;
    
    public DiscountStrategy(double percent) {
        this.discountPercent = percent;
    }
    
    public double apply(double price) {
        return price * (1 - discountPercent / 100);
    }
}
```

**10. Как комбинировать несколько стратегий?**

*Ответ:* Можно использовать паттерн Composite или Chain of Responsibility:

```java
class CompositeStrategy implements Strategy {
    private List<Strategy> strategies;
    
    public CompositeStrategy(Strategy... strategies) {
        this.strategies = Arrays.asList(strategies);
    }
    
    public void execute() {
        strategies.forEach(Strategy::execute);
    }
}

// Использование
Strategy composite = new CompositeStrategy(
    new ValidationStrategy(),
    new LoggingStrategy(),
    new CachingStrategy()
);
```

---

[← Назад к разделу Поведенческие паттерны](README.md)
