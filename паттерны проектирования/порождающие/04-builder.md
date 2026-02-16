# Builder (Строитель)

Builder — порождающий паттерн проектирования, который позволяет создавать сложные объекты пошагово. Паттерн позволяет использовать один и тот же код строительства для получения разных представлений объектов.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
5. [Fluent Interface](#fluent-interface)
6. [Примеры использования](#примеры-использования)
7. [Lombok @Builder](#lombok-builder)
8. [Преимущества и недостатки](#преимущества-и-недостатки)
9. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Builder используется когда:
- У объекта много параметров (более 3-4), особенно если многие из них необязательны
- Нужно создавать разные представления одного и того же объекта
- Процесс создания объекта сложный и требует нескольких шагов
- Нужно создавать неизменяемые (immutable) объекты с множеством полей

**Типичные примеры использования:**
- Построение сложных конфигураций
- Создание SQL-запросов
- Построение HTTP-запросов
- Создание HTML/XML документов
- Создание объектов с многими опциональными параметрами

## Проблема, которую решает

### Проблема: Телескопический конструктор

```java
public class User {
    private String firstName;      // обязательный
    private String lastName;       // обязательный
    private String email;          // обязательный
    private String phone;          // опциональный
    private String address;        // опциональный
    private int age;               // опциональный
    private String company;        // опциональный
    
    // Конструктор с обязательными параметрами
    public User(String firstName, String lastName, String email) {
        this(firstName, lastName, email, null);
    }
    
    // Конструктор с 1 опциональным параметром
    public User(String firstName, String lastName, String email, String phone) {
        this(firstName, lastName, email, phone, null);
    }
    
    // Конструктор с 2 опциональными параметрами
    public User(String firstName, String lastName, String email, 
                String phone, String address) {
        this(firstName, lastName, email, phone, address, 0);
    }
    
    // Конструктор с 3 опциональными параметрами
    public User(String firstName, String lastName, String email, 
                String phone, String address, int age) {
        this(firstName, lastName, email, phone, address, age, null);
    }
    
    // Полный конструктор
    public User(String firstName, String lastName, String email, 
                String phone, String address, int age, String company) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.age = age;
        this.company = company;
    }
}

// Использование - неудобно и непонятно
User user = new User("John", "Doe", "john@example.com", null, null, 25, "Acme");
//                   ^^^^   ^^^^    ^^^^^^^^^^^^^^^^^^   ^^^^  ^^^^  ^^  ^^^^^^
//                   Что это? Сложно понять без документации
```

**Проблемы:**
- Множество конструкторов (телескопический паттерн)
- Непонятно, что означает каждый параметр
- Невозможно пропустить опциональные параметры посередине
- Легко перепутать порядок параметров одного типа
- Код трудно читать и поддерживать

### Решение: Builder

Использовать строитель для пошагового создания объекта.

## Структура паттерна

```java
public class User {
    // Все поля final для создания immutable объекта
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String address;
    private final int age;
    private final String company;
    
    // Приватный конструктор - только Builder может создавать объекты
    private User(Builder builder) {
        this.firstName = builder.firstName;
        this.lastName = builder.lastName;
        this.email = builder.email;
        this.phone = builder.phone;
        this.address = builder.address;
        this.age = builder.age;
        this.company = builder.company;
    }
    
    // Геттеры (сеттеров нет - объект immutable)
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public int getAge() { return age; }
    public String getCompany() { return company; }
    
    // Статический вложенный класс Builder
    public static class Builder {
        // Обязательные параметры
        private final String firstName;
        private final String lastName;
        private final String email;
        
        // Опциональные параметры с значениями по умолчанию
        private String phone = "";
        private String address = "";
        private int age = 0;
        private String company = "";
        
        // Конструктор билдера принимает обязательные параметры
        public Builder(String firstName, String lastName, String email) {
            this.firstName = firstName;
            this.lastName = lastName;
            this.email = email;
        }
        
        // Методы для установки опциональных параметров
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public Builder address(String address) {
            this.address = address;
            return this;
        }
        
        public Builder age(int age) {
            this.age = age;
            return this;
        }
        
        public Builder company(String company) {
            this.company = company;
            return this;
        }
        
        // Метод build() создает финальный объект
        public User build() {
            return new User(this);
        }
    }
}

// Использование - понятно и читаемо
User user = new User.Builder("John", "Doe", "john@example.com")
        .age(25)
        .company("Acme")
        .build();

// Можно опустить любые опциональные параметры
User user2 = new User.Builder("Jane", "Smith", "jane@example.com")
        .phone("+1234567890")
        .address("123 Main St")
        .build();
```

## Реализация

### Пример 1: HTTP Request Builder

```java
public class HttpRequest {
    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final String body;
    private final int timeout;
    
    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.url = builder.url;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.queryParams = Collections.unmodifiableMap(new HashMap<>(builder.queryParams));
        this.body = builder.body;
        this.timeout = builder.timeout;
    }
    
    public void execute() {
        System.out.println("Executing " + method + " request to " + url);
        System.out.println("Headers: " + headers);
        System.out.println("Query params: " + queryParams);
        System.out.println("Body: " + body);
        System.out.println("Timeout: " + timeout + "ms");
    }
    
    public static class Builder {
        // Обязательные параметры
        private final String method;
        private final String url;
        
        // Опциональные параметры
        private Map<String, String> headers = new HashMap<>();
        private Map<String, String> queryParams = new HashMap<>();
        private String body = "";
        private int timeout = 30000; // 30 секунд по умолчанию
        
        public Builder(String method, String url) {
            this.method = method;
            this.url = url;
        }
        
        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        
        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }
        
        public Builder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }
        
        public Builder queryParams(Map<String, String> params) {
            this.queryParams.putAll(params);
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public HttpRequest build() {
            validate();
            return new HttpRequest(this);
        }
        
        private void validate() {
            if (method == null || method.isEmpty()) {
                throw new IllegalStateException("Method is required");
            }
            if (url == null || url.isEmpty()) {
                throw new IllegalStateException("URL is required");
            }
        }
    }
}

// Использование
HttpRequest request = new HttpRequest.Builder("POST", "https://api.example.com/users")
        .header("Content-Type", "application/json")
        .header("Authorization", "Bearer token123")
        .queryParam("page", "1")
        .queryParam("size", "10")
        .body("{\"name\": \"John Doe\"}")
        .timeout(5000)
        .build();

request.execute();
```

### Пример 2: SQL Query Builder

```java
public class SqlQuery {
    private final String table;
    private final List<String> columns;
    private final List<String> whereClauses;
    private final List<String> orderBy;
    private final Integer limit;
    private final Integer offset;
    
    private SqlQuery(Builder builder) {
        this.table = builder.table;
        this.columns = List.copyOf(builder.columns);
        this.whereClauses = List.copyOf(builder.whereClauses);
        this.orderBy = List.copyOf(builder.orderBy);
        this.limit = builder.limit;
        this.offset = builder.offset;
    }
    
    public String toSql() {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        if (columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(String.join(", ", columns));
        }
        
        sql.append(" FROM ").append(table);
        
        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", whereClauses));
        }
        
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(String.join(", ", orderBy));
        }
        
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return sql.toString();
    }
    
    public static class Builder {
        private final String table;
        private List<String> columns = new ArrayList<>();
        private List<String> whereClauses = new ArrayList<>();
        private List<String> orderBy = new ArrayList<>();
        private Integer limit;
        private Integer offset;
        
        public Builder(String table) {
            this.table = table;
        }
        
        public Builder select(String... columns) {
            this.columns.addAll(Arrays.asList(columns));
            return this;
        }
        
        public Builder where(String condition) {
            this.whereClauses.add(condition);
            return this;
        }
        
        public Builder orderBy(String column, String direction) {
            this.orderBy.add(column + " " + direction);
            return this;
        }
        
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }
        
        public SqlQuery build() {
            return new SqlQuery(this);
        }
    }
}

// Использование
SqlQuery query = new SqlQuery.Builder("users")
        .select("id", "name", "email")
        .where("age > 18")
        .where("active = true")
        .orderBy("name", "ASC")
        .limit(10)
        .offset(20)
        .build();

System.out.println(query.toSql());
// SELECT id, name, email FROM users WHERE age > 18 AND active = true ORDER BY name ASC LIMIT 10 OFFSET 20
```

### Пример 3: Pizza Builder (классический пример)

```java
public class Pizza {
    private final int size;
    private final boolean cheese;
    private final boolean pepperoni;
    private final boolean mushrooms;
    private final boolean olives;
    private final boolean bacon;
    
    private Pizza(Builder builder) {
        this.size = builder.size;
        this.cheese = builder.cheese;
        this.pepperoni = builder.pepperoni;
        this.mushrooms = builder.mushrooms;
        this.olives = builder.olives;
        this.bacon = builder.bacon;
    }
    
    @Override
    public String toString() {
        return "Pizza{" +
                "size=" + size +
                ", cheese=" + cheese +
                ", pepperoni=" + pepperoni +
                ", mushrooms=" + mushrooms +
                ", olives=" + olives +
                ", bacon=" + bacon +
                '}';
    }
    
    public static class Builder {
        // Обязательный параметр
        private final int size;
        
        // Опциональные топпинги
        private boolean cheese = false;
        private boolean pepperoni = false;
        private boolean mushrooms = false;
        private boolean olives = false;
        private boolean bacon = false;
        
        public Builder(int size) {
            this.size = size;
        }
        
        public Builder addCheese() {
            this.cheese = true;
            return this;
        }
        
        public Builder addPepperoni() {
            this.pepperoni = true;
            return this;
        }
        
        public Builder addMushrooms() {
            this.mushrooms = true;
            return this;
        }
        
        public Builder addOlives() {
            this.olives = true;
            return this;
        }
        
        public Builder addBacon() {
            this.bacon = true;
            return this;
        }
        
        public Pizza build() {
            return new Pizza(this);
        }
    }
}

// Использование
Pizza margherita = new Pizza.Builder(12)
        .addCheese()
        .build();

Pizza supremePizza = new Pizza.Builder(16)
        .addCheese()
        .addPepperoni()
        .addMushrooms()
        .addOlives()
        .addBacon()
        .build();

System.out.println(margherita);
System.out.println(supremePizza);
```

## Fluent Interface

Builder естественным образом поддерживает Fluent Interface (текучий интерфейс):

```java
public class Email {
    private final String from;
    private final List<String> to;
    private final List<String> cc;
    private final List<String> bcc;
    private final String subject;
    private final String body;
    private final List<String> attachments;
    
    private Email(Builder builder) {
        this.from = builder.from;
        this.to = List.copyOf(builder.to);
        this.cc = List.copyOf(builder.cc);
        this.bcc = List.copyOf(builder.bcc);
        this.subject = builder.subject;
        this.body = builder.body;
        this.attachments = List.copyOf(builder.attachments);
    }
    
    public void send() {
        System.out.println("Sending email:");
        System.out.println("From: " + from);
        System.out.println("To: " + to);
        System.out.println("CC: " + cc);
        System.out.println("Subject: " + subject);
        System.out.println("Body: " + body);
    }
    
    public static class Builder {
        private String from;
        private List<String> to = new ArrayList<>();
        private List<String> cc = new ArrayList<>();
        private List<String> bcc = new ArrayList<>();
        private String subject;
        private String body;
        private List<String> attachments = new ArrayList<>();
        
        public Builder from(String from) {
            this.from = from;
            return this;
        }
        
        public Builder to(String... recipients) {
            this.to.addAll(Arrays.asList(recipients));
            return this;
        }
        
        public Builder cc(String... recipients) {
            this.cc.addAll(Arrays.asList(recipients));
            return this;
        }
        
        public Builder bcc(String... recipients) {
            this.bcc.addAll(Arrays.asList(recipients));
            return this;
        }
        
        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }
        
        public Builder body(String body) {
            this.body = body;
            return this;
        }
        
        public Builder attach(String... files) {
            this.attachments.addAll(Arrays.asList(files));
            return this;
        }
        
        public Email build() {
            validate();
            return new Email(this);
        }
        
        private void validate() {
            if (from == null || from.isEmpty()) {
                throw new IllegalStateException("Sender email is required");
            }
            if (to.isEmpty()) {
                throw new IllegalStateException("At least one recipient is required");
            }
        }
    }
}

// Использование - читается как естественный язык
Email email = new Email.Builder()
        .from("sender@example.com")
        .to("recipient1@example.com", "recipient2@example.com")
        .cc("manager@example.com")
        .subject("Project Update")
        .body("Hello, here is the project update...")
        .attach("report.pdf", "presentation.pptx")
        .build();

email.send();
```

## Примеры использования

### Из Java Core - StringBuilder

```java
String result = new StringBuilder()
        .append("Hello")
        .append(" ")
        .append("World")
        .append("!")
        .toString();

// StringBuilder - это Builder для String
```

### Из Java 8+ Stream API

```java
List<String> result = Stream.of("apple", "banana", "cherry")
        .filter(s -> s.length() > 5)
        .map(String::toUpperCase)
        .sorted()
        .collect(Collectors.toList());

// Stream API использует паттерн Builder для создания цепочек операций
```

### Из Java HttpClient (Java 11+)

```java
HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/users"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{\"name\": \"John\"}"))
        .build();
```

### Из Spring Framework

```java
// RestTemplate Builder
RestTemplate restTemplate = new RestTemplateBuilder()
        .rootUri("https://api.example.com")
        .setConnectTimeout(Duration.ofSeconds(5))
        .setReadTimeout(Duration.ofSeconds(5))
        .build();

// WebClient Builder
WebClient client = WebClient.builder()
        .baseUrl("https://api.example.com")
        .defaultHeader("User-Agent", "MyApp/1.0")
        .build();
```

## Lombok @Builder

Lombok автоматически генерирует Builder:

```java
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private int age;
    private String company;
}

// Использование - Lombok автоматически создает Builder
User user = User.builder()
        .firstName("John")
        .lastName("Doe")
        .email("john@example.com")
        .age(25)
        .company("Acme")
        .build();
```

**Дополнительные возможности Lombok @Builder:**

```java
@Builder(toBuilder = true)  // Позволяет создавать копии с изменениями
public class User {
    private String name;
    private int age;
}

User original = User.builder().name("John").age(25).build();
User modified = original.toBuilder().age(26).build();  // Копия с измененным age

// @Builder с @Singular для коллекций
@Builder
public class Team {
    @Singular
    private List<String> members;  // Lombok создаст метод member(String) и members(Collection)
}

Team team = Team.builder()
        .member("Alice")
        .member("Bob")
        .member("Charlie")
        .build();
```

## Преимущества и недостатки

### Преимущества

✅ **Читаемость кода**
- Понятно, какой параметр устанавливается
- Код читается как естественный язык

✅ **Гибкость**
- Легко добавлять новые опциональные параметры
- Можно устанавливать параметры в любом порядке

✅ **Immutable объекты**
- Идеально подходит для создания неизменяемых объектов
- Все поля final, установка через конструктор

✅ **Валидация**
- Можно проверить корректность всех параметров перед созданием объекта
- Все проверки в одном месте (метод build)

✅ **Вариативность**
- Можно создавать различные представления одного объекта
- Легко опускать необязательные параметры

✅ **Поддержка IDE**
- Автодополнение показывает доступные методы
- Легко найти все возможные опции

### Недостатки

❌ **Увеличение объема кода**
- Требуется написать дополнительный класс Builder
- Больше кода для простых объектов

❌ **Дублирование полей**
- Поля дублируются в основном классе и в Builder

❌ **Производительность**
- Создание дополнительного объекта Builder
- Незначительный overhead (обычно не критично)

❌ **Может быть избыточным**
- Для простых объектов с 1-2 параметрами обычный конструктор проще

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Builder и когда его использовать?**

*Ответ:* Builder — это порождающий паттерн, который позволяет создавать сложные объекты пошагово. Используется когда:
- У объекта много параметров (особенно опциональных)
- Нужна гибкость в создании объекта
- Нужно создавать immutable объекты
- Телескопический конструктор становится неуправляемым

**2. В чем преимущества Builder над телескопическими конструкторами?**

*Ответ:*
- Читаемость - понятно, какой параметр устанавливается
- Гибкость - можно устанавливать параметры в любом порядке
- Безопасность - валидация перед созданием объекта
- Поддержка опциональных параметров без создания множества конструкторов

**3. Приведите примеры Builder из JDK**

*Ответ:*
- `StringBuilder` / `StringBuffer`
- `Stream.Builder`
- `HttpClient.Builder` и `HttpRequest.Builder` (Java 11+)
- `ProcessBuilder`
- `Calendar.Builder` (Java 8+)

**4. Как Builder помогает создавать immutable объекты?**

*Ответ:*
- Все поля объекта делаются final
- Устанавливаются только в конструкторе
- Builder собирает все параметры, затем передает их в приватный конструктор
- После создания объект нельзя изменить

### Продвинутые вопросы

**5. В чем разница между Builder и Factory Method?**

*Ответ:*
- **Builder**: Пошаговое создание сложного объекта, фокус на конфигурации
- **Factory Method**: Одношаговое создание объекта, фокус на выборе типа
- Builder лучше для объектов с многими параметрами
- Factory Method лучше для выбора между различными классами

**6. Как реализовать валидацию в Builder?**

*Ответ:*
```java
public User build() {
    validate();
    return new User(this);
}

private void validate() {
    if (email == null || !email.contains("@")) {
        throw new IllegalStateException("Invalid email");
    }
    if (age < 0 || age > 150) {
        throw new IllegalStateException("Invalid age");
    }
}
```

**7. Что такое Fluent Interface и как он связан с Builder?**

*Ответ:* Fluent Interface — это паттерн проектирования API, при котором методы возвращают `this` для создания цепочек вызовов. Builder естественно использует Fluent Interface:
```java
builder.setName("John").setAge(25).setEmail("john@example.com").build();
```
Это делает код более читаемым и похожим на естественный язык.

**8. Можно ли сделать Builder обобщенным (Generic)?**

*Ответ:* Да, можно создать абстрактный Builder:
```java
public abstract class Builder<T> {
    public abstract T build();
}

public class UserBuilder extends Builder<User> {
    @Override
    public User build() {
        return new User(this);
    }
}
```

**9. Как Lombok упрощает создание Builder?**

*Ответ:* Lombok с аннотацией `@Builder` автоматически генерирует:
- Статический вложенный класс Builder
- Все методы для установки полей
- Метод build()
- Опционально toBuilder() для создания копий
- @Singular для удобной работы с коллекциями

**10. Когда НЕ стоит использовать Builder?**

*Ответ:*
- Объект имеет мало параметров (1-3)
- Все параметры обязательны
- Объект простой и не требует сложной конфигурации
- Производительность критична (хотя overhead обычно незначителен)
- Для таких случаев достаточно обычного конструктора или статического фабричного метода

---

[← Назад к разделу Порождающие паттерны](README.md)
