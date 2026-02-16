# Decorator (Декоратор)

Decorator — структурный паттерн проектирования, который позволяет динамически добавлять объектам новую функциональность, оборачивая их в полезные обертки.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Примеры использования](#примеры-использования)
6. [Decorator vs Inheritance](#decorator-vs-inheritance)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Decorator используется когда:
- Нужно добавлять обязанности объектам динамически и прозрачно
- Расширение функциональности через наследование нецелесообразно
- Нужно добавлять несколько независимых расширений
- Невозможно расширить класс через наследование (final класс)

**Типичные примеры использования:**
- Добавление функциональности к потокам ввода-вывода
- Кеширование результатов методов
- Логирование вызовов методов
- Валидация данных
- Добавление эффектов в GUI компоненты

## Проблема, которую решает

### Проблема: Комбинаторный взрыв подклассов

```java
// Базовый кофе
class SimpleCoffee {
    public double cost() {
        return 5.0;
    }
    
    public String description() {
        return "Simple Coffee";
    }
}

// Чтобы добавить молоко, сахар, карамель - нужны подклассы
class CoffeeWithMilk extends SimpleCoffee {
    public double cost() {
        return super.cost() + 1.0;
    }
    
    public String description() {
        return super.description() + ", Milk";
    }
}

class CoffeeWithSugar extends SimpleCoffee {
    public double cost() {
        return super.cost() + 0.5;
    }
    
    public String description() {
        return super.description() + ", Sugar";
    }
}

// А если нужно молоко И сахар? Новый класс!
class CoffeeWithMilkAndSugar extends SimpleCoffee {
    public double cost() {
        return super.cost() + 1.0 + 0.5;
    }
    
    public String description() {
        return super.description() + ", Milk, Sugar";
    }
}

// А молоко, сахар и карамель? Еще один класс!
// А двойная порция молока? И так далее...
// Количество классов растет экспоненциально!
```

**Проблемы:**
- Комбинаторный взрыв количества подклассов
- Невозможность добавлять функции динамически
- Нарушение принципа единственной ответственности
- Жесткая структура наследования

### Решение: Decorator

Оборачивать объект в декораторы, каждый из которых добавляет свою функциональность.

## Структура паттерна

```java
// Компонент - базовый интерфейс
interface Coffee {
    double cost();
    String description();
}

// Конкретный компонент
class SimpleCoffee implements Coffee {
    @Override
    public double cost() {
        return 5.0;
    }
    
    @Override
    public String description() {
        return "Simple Coffee";
    }
}

// Базовый декоратор
abstract class CoffeeDecorator implements Coffee {
    protected Coffee coffee;  // Оборачиваемый объект
    
    public CoffeeDecorator(Coffee coffee) {
        this.coffee = coffee;
    }
    
    @Override
    public double cost() {
        return coffee.cost();
    }
    
    @Override
    public String description() {
        return coffee.description();
    }
}

// Конкретные декораторы
class MilkDecorator extends CoffeeDecorator {
    public MilkDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public double cost() {
        return super.cost() + 1.0;
    }
    
    @Override
    public String description() {
        return super.description() + ", Milk";
    }
}

class SugarDecorator extends CoffeeDecorator {
    public SugarDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public double cost() {
        return super.cost() + 0.5;
    }
    
    @Override
    public String description() {
        return super.description() + ", Sugar";
    }
}

class CaramelDecorator extends CoffeeDecorator {
    public CaramelDecorator(Coffee coffee) {
        super(coffee);
    }
    
    @Override
    public double cost() {
        return super.cost() + 1.5;
    }
    
    @Override
    public String description() {
        return super.description() + ", Caramel";
    }
}

// Использование - гибкое комбинирование
Coffee coffee = new SimpleCoffee();
System.out.println(coffee.description() + " = $" + coffee.cost());
// Simple Coffee = $5.0

coffee = new MilkDecorator(coffee);
System.out.println(coffee.description() + " = $" + coffee.cost());
// Simple Coffee, Milk = $6.0

coffee = new SugarDecorator(coffee);
System.out.println(coffee.description() + " = $" + coffee.cost());
// Simple Coffee, Milk, Sugar = $6.5

coffee = new CaramelDecorator(coffee);
System.out.println(coffee.description() + " = $" + coffee.cost());
// Simple Coffee, Milk, Sugar, Caramel = $8.0

// Или все сразу (fluent style)
Coffee fancyCoffee = new CaramelDecorator(
                        new SugarDecorator(
                            new MilkDecorator(
                                new SimpleCoffee())));
```

## Реализация

### Пример 1: Логирование и кеширование

```java
// Компонент - сервис для получения данных
interface DataService {
    String getData(String key);
}

// Конкретная реализация
class DatabaseDataService implements DataService {
    @Override
    public String getData(String key) {
        System.out.println("Fetching data from database for key: " + key);
        // Имитация запроса к БД
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Data for " + key;
    }
}

// Декоратор с кешированием
class CachingDataService implements DataService {
    private final DataService dataService;
    private final Map<String, String> cache = new HashMap<>();
    
    public CachingDataService(DataService dataService) {
        this.dataService = dataService;
    }
    
    @Override
    public String getData(String key) {
        if (cache.containsKey(key)) {
            System.out.println("Returning cached data for key: " + key);
            return cache.get(key);
        }
        
        String data = dataService.getData(key);
        cache.put(key, data);
        return data;
    }
}

// Декоратор с логированием
class LoggingDataService implements DataService {
    private final DataService dataService;
    
    public LoggingDataService(DataService dataService) {
        this.dataService = dataService;
    }
    
    @Override
    public String getData(String key) {
        long start = System.currentTimeMillis();
        System.out.println("[LOG] Getting data for key: " + key);
        
        String data = dataService.getData(key);
        
        long duration = System.currentTimeMillis() - start;
        System.out.println("[LOG] Data retrieved in " + duration + "ms");
        
        return data;
    }
}

// Декоратор с валидацией
class ValidatingDataService implements DataService {
    private final DataService dataService;
    
    public ValidatingDataService(DataService dataService) {
        this.dataService = dataService;
    }
    
    @Override
    public String getData(String key) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Key cannot be null or empty");
        }
        
        if (key.length() > 100) {
            throw new IllegalArgumentException("Key is too long");
        }
        
        return dataService.getData(key);
    }
}

// Использование - комбинирование декораторов
DataService service = new DatabaseDataService();

// Добавляем валидацию
service = new ValidatingDataService(service);

// Добавляем логирование
service = new LoggingDataService(service);

// Добавляем кеширование
service = new CachingDataService(service);

// Первый вызов - идет в БД
String data1 = service.getData("user123");

// Второй вызов - берется из кеша
String data2 = service.getData("user123");
```

### Пример 2: Обработка текста

```java
// Компонент - текстовый процессор
interface TextProcessor {
    String process(String text);
}

// Базовый процессор - ничего не делает
class PlainTextProcessor implements TextProcessor {
    @Override
    public String process(String text) {
        return text;
    }
}

// Декоратор для конвертации в uppercase
class UpperCaseDecorator implements TextProcessor {
    private final TextProcessor processor;
    
    public UpperCaseDecorator(TextProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public String process(String text) {
        return processor.process(text).toUpperCase();
    }
}

// Декоратор для удаления пробелов
class TrimDecorator implements TextProcessor {
    private final TextProcessor processor;
    
    public TrimDecorator(TextProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public String process(String text) {
        return processor.process(text).trim();
    }
}

// Декоратор для HTML-escape
class HtmlEscapeDecorator implements TextProcessor {
    private final TextProcessor processor;
    
    public HtmlEscapeDecorator(TextProcessor processor) {
        this.processor = processor;
    }
    
    @Override
    public String process(String text) {
        String processed = processor.process(text);
        return processed
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

// Декоратор для добавления префикса/суффикса
class WrapperDecorator implements TextProcessor {
    private final TextProcessor processor;
    private final String prefix;
    private final String suffix;
    
    public WrapperDecorator(TextProcessor processor, String prefix, String suffix) {
        this.processor = processor;
        this.prefix = prefix;
        this.suffix = suffix;
    }
    
    @Override
    public String process(String text) {
        return prefix + processor.process(text) + suffix;
    }
}

// Использование
TextProcessor processor = new PlainTextProcessor();

processor = new TrimDecorator(processor);
processor = new UpperCaseDecorator(processor);
processor = new HtmlEscapeDecorator(processor);
processor = new WrapperDecorator(processor, "<div>", "</div>");

String result = processor.process("  Hello <World>  ");
System.out.println(result);
// <div>HELLO &lt;WORLD&gt;</div>
```

### Пример 3: Оконные компоненты (GUI)

```java
// Компонент - оконный элемент
interface Window {
    void draw();
    String getDescription();
}

// Конкретное окно
class SimpleWindow implements Window {
    @Override
    public void draw() {
        System.out.println("Drawing simple window");
    }
    
    @Override
    public String getDescription() {
        return "Simple Window";
    }
}

// Базовый декоратор окна
abstract class WindowDecorator implements Window {
    protected Window window;
    
    public WindowDecorator(Window window) {
        this.window = window;
    }
    
    @Override
    public void draw() {
        window.draw();
    }
    
    @Override
    public String getDescription() {
        return window.getDescription();
    }
}

// Декоратор для вертикальной прокрутки
class VerticalScrollBarDecorator extends WindowDecorator {
    public VerticalScrollBarDecorator(Window window) {
        super(window);
    }
    
    @Override
    public void draw() {
        super.draw();
        drawVerticalScrollBar();
    }
    
    private void drawVerticalScrollBar() {
        System.out.println("Drawing vertical scrollbar");
    }
    
    @Override
    public String getDescription() {
        return super.getDescription() + " + Vertical Scrollbar";
    }
}

// Декоратор для горизонтальной прокрутки
class HorizontalScrollBarDecorator extends WindowDecorator {
    public HorizontalScrollBarDecorator(Window window) {
        super(window);
    }
    
    @Override
    public void draw() {
        super.draw();
        drawHorizontalScrollBar();
    }
    
    private void drawHorizontalScrollBar() {
        System.out.println("Drawing horizontal scrollbar");
    }
    
    @Override
    public String getDescription() {
        return super.getDescription() + " + Horizontal Scrollbar";
    }
}

// Декоратор для рамки
class BorderDecorator extends WindowDecorator {
    public BorderDecorator(Window window) {
        super(window);
    }
    
    @Override
    public void draw() {
        drawBorder();
        super.draw();
    }
    
    private void drawBorder() {
        System.out.println("Drawing border");
    }
    
    @Override
    public String getDescription() {
        return super.getDescription() + " + Border";
    }
}

// Использование
Window window = new SimpleWindow();
window = new BorderDecorator(window);
window = new VerticalScrollBarDecorator(window);
window = new HorizontalScrollBarDecorator(window);

System.out.println(window.getDescription());
window.draw();
// Simple Window + Border + Vertical Scrollbar + Horizontal Scrollbar
// Drawing border
// Drawing simple window
// Drawing vertical scrollbar
// Drawing horizontal scrollbar
```

## Примеры использования

### Java I/O Streams - классический пример Decorator

```java
// Базовый поток
FileInputStream fileStream = new FileInputStream("file.txt");

// Оборачиваем в BufferedInputStream для буферизации
BufferedInputStream bufferedStream = new BufferedInputStream(fileStream);

// Оборачиваем в DataInputStream для чтения примитивов
DataInputStream dataStream = new DataInputStream(bufferedStream);

// Каждый декоратор добавляет свою функциональность
int value = dataStream.readInt();

// Другой пример
Reader reader = new FileReader("file.txt");
reader = new BufferedReader(reader);
reader = new LineNumberReader(reader);

// Еще пример с выводом
Writer writer = new FileWriter("output.txt");
writer = new BufferedWriter(writer);
writer = new PrintWriter(writer);
```

### Java Collections - unmodifiable decorators

```java
List<String> list = new ArrayList<>();
list.add("item1");
list.add("item2");

// Collections.unmodifiableList - декоратор, запрещающий модификацию
List<String> unmodifiableList = Collections.unmodifiableList(list);

// unmodifiableList.add("item3");  // UnsupportedOperationException

// Другие декораторы коллекций
Set<String> synchronizedSet = Collections.synchronizedSet(new HashSet<>());
List<String> checkedList = Collections.checkedList(new ArrayList<>(), String.class);
```

### Spring Framework

```java
// Spring использует Decorator для транзакций, кеширования, безопасности
@Service
public class UserService {
    @Transactional  // Декоратор для транзакций
    @Cacheable      // Декоратор для кеширования
    @Secured        // Декоратор для безопасности
    public User getUser(Long id) {
        // Бизнес-логика
        return userRepository.findById(id);
    }
}

// Spring создает прокси (вариант Decorator), который оборачивает методы
```

### Servlet Filters

```java
// Servlet Filter - это Decorator для HTTP-запросов
public class LoggingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                        FilterChain chain) {
        // Обработка до
        System.out.println("Request received");
        
        // Вызов следующего фильтра/сервлета
        chain.doFilter(request, response);
        
        // Обработка после
        System.out.println("Response sent");
    }
}
```

## Decorator vs Inheritance

| Аспект | Decorator | Inheritance |
|--------|-----------|-------------|
| **Гибкость** | Динамическое добавление функций | Статическая структура |
| **Комбинирование** | Любые комбинации декораторов | Фиксированная иерархия |
| **Количество классов** | Меньше классов | Комбинаторный взрыв |
| **Изменение во время выполнения** | Можно менять | Нельзя менять |
| **Прозрачность** | Прозрачно для клиента | Клиент знает о подклассе |
| **Множественное наследование** | Эмулирует через композицию | Ограничено языком |

**Пример проблемы с наследованием:**
```java
// Плохо - комбинаторный взрыв
class ScrollableWindow extends Window { }
class BorderedWindow extends Window { }
class ScrollableBorderedWindow extends Window { }  // И так для каждой комбинации!

// Хорошо - Decorator
Window window = new BorderDecorator(new ScrollDecorator(new SimpleWindow()));
```

## Преимущества и недостатки

### Преимущества

✅ **Гибкость**
- Можно добавлять/удалять обязанности динамически
- Различные комбинации декораторов

✅ **Принцип единственной ответственности**
- Каждый декоратор отвечает за одну функцию
- Легко добавлять новые декораторы

✅ **Альтернатива наследованию**
- Избегает комбинаторного взрыва подклассов
- Композиция вместо наследования

✅ **Open/Closed Principle**
- Можно расширять функциональность без изменения существующего кода

✅ **Прозрачность**
- Декораторы прозрачны для клиента
- Работают с тем же интерфейсом

### Недостатки

❌ **Множество мелких объектов**
- Много оберток усложняет код
- Сложность отладки

❌ **Порядок декораторов имеет значение**
- Разный порядок может давать разные результаты
- Нужно следить за последовательностью

❌ **Сложность конфигурации**
- Сложная настройка цепочки декораторов
- Много кода для создания декорированных объектов

❌ **Производительность**
- Каждый декоратор добавляет вызов метода
- Длинные цепочки могут замедлить работу

❌ **Сложность удаления конкретного декоратора**
- Трудно удалить декоратор из середины цепочки
- Обычно нужно пересоздавать всю цепочку

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Decorator и когда его использовать?**

*Ответ:* Decorator — это структурный паттерн, который позволяет динамически добавлять объектам новую функциональность, оборачивая их в декораторы. Используется когда:
- Нужно добавлять функции объектам динамически
- Расширение через наследование нецелесообразно
- Нужны различные комбинации функциональности

**2. Приведите примеры Decorator из JDK**

*Ответ:*
- Java I/O: `BufferedInputStream`, `DataInputStream`, `BufferedReader`
- Collections: `Collections.unmodifiableList()`, `Collections.synchronizedSet()`
- Servlet Filters в Java EE

**3. В чем разница между Decorator и простым наследованием?**

*Ответ:*
- Decorator использует композицию, наследование использует иерархию классов
- Decorator позволяет добавлять функции динамически
- Decorator избегает комбинаторного взрыва подклассов
- Decorator можно комбинировать в различных порядках

**4. Какова структура паттерна Decorator?**

*Ответ:* Основные компоненты:
- **Component** (интерфейс) — общий интерфейс для объектов и декораторов
- **ConcreteComponent** — базовый объект
- **Decorator** — базовый класс декораторов, содержит ссылку на компонент
- **ConcreteDecorator** — конкретные декораторы, добавляющие функциональность

### Продвинутые вопросы

**5. В чем разница между Decorator и Proxy?**

*Ответ:*
- **Decorator** добавляет новую функциональность
- **Proxy** контролирует доступ к объекту
- Decorator может оборачивать несколько раз, Proxy обычно один
- Decorator клиент создает сам, Proxy часто создается прозрачно

**6. Как Decorator связан с принципом Open/Closed?**

*Ответ:* Decorator идеально реализует принцип Open/Closed:
- Класс закрыт для модификации (не меняем исходный класс)
- Открыт для расширения (добавляем декораторы)
- Новая функциональность добавляется без изменения существующего кода

**7. Почему порядок декораторов может быть важен?**

*Ответ:* Потому что каждый декоратор обрабатывает результат предыдущего:
```java
// Разные результаты!
new UpperCaseDecorator(new TrimDecorator(text));  // "HELLO"
new TrimDecorator(new UpperCaseDecorator(text));  // "HELLO  " (пробелы после)
```

**8. Какие недостатки у Decorator?**

*Ответ:*
- Множество мелких объектов усложняет код
- Порядок декораторов имеет значение
- Сложность отладки и трассировки
- Производительность при длинных цепочках
- Сложно удалить конкретный декоратор из цепочки

**9. Как реализовать Decorator для интерфейса с большим количеством методов?**

*Ответ:* Создать абстрактный базовый декоратор, который делегирует все методы:
```java
abstract class BaseDecorator implements LargeInterface {
    protected LargeInterface component;
    
    public BaseDecorator(LargeInterface component) {
        this.component = component;
    }
    
    // Делегировать ВСЕ методы интерфейса
    public void method1() { component.method1(); }
    public void method2() { component.method2(); }
    // ... и т.д.
}

// Конкретные декораторы переопределяют только нужные методы
class ConcreteDecorator extends BaseDecorator {
    public void method1() {
        // Дополнительная логика
        super.method1();
    }
}
```

**10. Можно ли комбинировать Decorator с другими паттернами?**

*Ответ:* Да, часто комбинируется:
- **Strategy** — Decorator может использовать Strategy для выбора алгоритма обработки
- **Factory** — Фабрика для создания цепочек декораторов
- **Composite** — Decorator и Composite часто используются вместе
- **Singleton** — Декораторы могут быть Singleton'ами для экономии памяти

---

[← Назад к разделу Структурные паттерны](README.md)
