# Hibernate и JPA

## Содержание

1. [Введение](#введение)
2. [JPA и Hibernate — отношение](#jpa-и-hibernate-отношение)
   - [Что такое JPA](#что-такое-jpa)
   - [Что такое Hibernate](#что-такое-hibernate)
   - [Другие реализации JPA](#другие-реализации-jpa)
3. [Основы работы с сущностями](#основы-работы-с-сущностями)
   - [Entity класс](#entity-класс)
   - [Первичные ключи](#первичные-ключи)
   - [Стратегии генерации ID](#стратегии-генерации-id)
4. [Маппинг полей и типов данных](#маппинг-полей-и-типов-данных)
   - [Базовые типы](#базовые-типы)
   - [Временные типы](#временные-типы)
   - [Enum типы](#enum-типы)
   - [LOB типы](#lob-типы)
5. [Связи между сущностями](#связи-между-сущностями)
   - [@OneToOne](#onetoone)
   - [@OneToMany и @ManyToOne](#onetomany-и-manytoone)
   - [@ManyToMany](#manytomany)
   - [Каскадные операции](#каскадные-операции)
   - [FetchType — стратегии загрузки](#fetchtype-стратегии-загрузки)
6. [Контекст персистентности (Persistence Context)](#контекст-персистентности-persistence-context)
   - [Состояния сущностей](#состояния-сущностей)
   - [EntityManager и его методы](#entitymanager-и-его-методы)
   - [Flush и синхронизация](#flush-и-синхронизация)
7. [Кеширование](#кеширование)
   - [Кеш первого уровня](#кеш-первого-уровня)
   - [Кеш второго уровня](#кеш-второго-уровня)
   - [Query Cache](#query-cache)
8. [Запросы к данным](#запросы-к-данным)
   - [HQL (Hibernate Query Language)](#hql-hibernate-query-language)
   - [JPQL (Java Persistence Query Language)](#jpql-java-persistence-query-language)
   - [Criteria API](#criteria-api)
   - [Native SQL](#native-sql)
   - [Named Queries](#named-queries)
9. [Транзакции и блокировки](#транзакции-и-блокировки)
   - [Управление транзакциями](#управление-транзакциями)
   - [Оптимистичные блокировки](#оптимистичные-блокировки)
   - [Пессимистичные блокировки](#пессимистичные-блокировки)
10. [Производительность и оптимизация](#производительность-и-оптимизация)
   - [Проблема N+1](#проблема-n1)
   - [Batch fetching](#batch-fetching)
   - [Entity graphs](#entity-graphs)
   - [Статистика Hibernate](#статистика-hibernate)
11. [Наследование](#наследование)
   - [SINGLE_TABLE](#single_table)
   - [JOINED](#joined)
   - [TABLE_PER_CLASS](#table_per_class)
12. [Дополнительные возможности](#дополнительные-возможности)
   - [Embeddable классы](#embeddable-классы)
   - [Composite keys](#composite-keys)
   - [Lifecycle callbacks](#lifecycle-callbacks)
   - [Entity listeners](#entity-listeners)
13. [Практические заметки](#практические-заметки)
   - [Распространённые ошибки](#распространённые-ошибки)
   - [Best practices](#best-practices)
   - [Миграция схемы БД](#миграция-схемы-бд)
14. [Практические упражнения](#практические-упражнения)
15. [Вопросы на собеседовании](#вопросы-на-собеседовании)
16. [Дополнительные материалы](#дополнительные-материалы)

## Введение

Hibernate — это ORM (Object-Relational Mapping) фреймворк для Java, который автоматизирует маппинг между объектами приложения и таблицами реляционной базы данных. Hibernate избавляет разработчиков от необходимости писать большое количество JDBC-кода, позволяя работать с данными в объектно-ориентированном стиле.

**Историческая справка.** Hibernate был создан Гэвином Кингом (Gavin King) в 2001 году как альтернатива громоздким Entity Beans из спецификации EJB 2.x. Фреймворк быстро завоевал популярность благодаря простоте использования и мощным возможностям. В 2006 году Hibernate стал основой для спецификации JPA (Java Persistence API), которая стандартизировала подходы к ORM в Java. С тех пор Hibernate остаётся наиболее популярной реализацией JPA.

**Ключевые возможности Hibernate:**
- Автоматический маппинг объектов на таблицы БД
- Управление связями между сущностями (один-к-одному, один-ко-многим, многие-ко-многим)
- Автоматическая генерация SQL-запросов
- Кеширование первого и второго уровней
- Ленивая (lazy) и активная (eager) загрузка связанных данных
- HQL/JPQL — объектно-ориентированные языки запросов
- Criteria API — type-safe построение запросов
- Управление транзакциями и блокировками
- Поддержка различных стратегий наследования

## JPA и Hibernate — отношение

### Что такое JPA

**JPA (Java Persistence API)** — это спецификация Java, определяющая стандартный способ работы с реляционными данными через ORM. JPA не является реализацией, а только набором интерфейсов и аннотаций, описывающих, как должна работать персистентность объектов.

JPA включает:
- Набор аннотаций для маппинга (`@Entity`, `@Table`, `@Column`, `@Id` и т.д.)
- Интерфейс `EntityManager` для управления сущностями
- JPQL — язык запросов
- Criteria API для программного построения запросов
- Описание жизненного цикла сущностей

### Что такое Hibernate

**Hibernate** — это конкретная реализация JPA, но при этом фреймворк предоставляет дополнительные возможности, выходящие за рамки спецификации:

**Возможности только Hibernate (не входят в JPA):**
- HQL (Hibernate Query Language) — расширение JPQL
- Дополнительные аннотации (`@Formula`, `@WhereJoinTable`, `@Filter` и др.)
- Расширенные стратегии кеширования
- Дополнительные генераторы ID
- Batch processing и статистика
- Специфические типы данных

**Практический совет:** Рекомендуется придерживаться стандарта JPA везде, где возможно, используя специфичные возможности Hibernate только когда JPA не предоставляет нужного функционала. Это облегчает миграцию на другие реализации JPA при необходимости.

### Другие реализации JPA

Помимо Hibernate существуют и другие реализации JPA:
- **EclipseLink** — референсная реализация JPA, используется в GlassFish
- **Apache OpenJPA** — реализация от Apache Foundation
- **DataNucleus** — поддерживает не только JPA, но и JDO

Hibernate остаётся наиболее популярным выбором благодаря зрелости, производительности и богатому функционалу.

## Основы работы с сущностями

### Entity класс

Сущность (entity) — это Java-класс, представляющий таблицу в базе данных. Каждый экземпляр сущности соответствует одной строке таблицы.

**Требования к entity-классу:**
- Помечен аннотацией `@Entity`
- Имеет первичный ключ (поле с `@Id`)
- Имеет конструктор без параметров (может быть protected или package-private)
- Не является final классом
- Не содержит final полей (для персистентных атрибутов)

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Конструктор по умолчанию обязателен
    protected User() {}
    
    public User(String username, String email) {
        this.username = username;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters и Setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    // ... остальные getters/setters
}
```

**Аннотации маппинга:**
- `@Table(name = "...")` — указывает имя таблицы (если отличается от имени класса)
- `@Column` — настройки колонки (имя, nullable, unique, length и т.д.)

### Первичные ключи

Каждая сущность должна иметь первичный ключ, помеченный аннотацией `@Id`:

```java
@Id
private Long id;
```

Для составных ключей используются `@EmbeddedId` или `@IdClass`.

### Стратегии генерации ID

Hibernate поддерживает несколько стратегий генерации первичных ключей:

```java
// 1. AUTO - Hibernate выбирает стратегию автоматически
@Id
@GeneratedValue(strategy = GenerationType.AUTO)
private Long id;

// 2. IDENTITY - автоинкремент БД (MySQL, PostgreSQL)
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;

// 3. SEQUENCE - использование последовательности БД (PostgreSQL, Oracle)
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "user_id_seq", allocationSize = 1)
private Long id;

// 4. TABLE - отдельная таблица для генерации ключей (универсально, но медленно)
@Id
@GeneratedValue(strategy = GenerationType.TABLE)
private Long id;

// 5. UUID (Hibernate-специфично)
@Id
@GeneratedValue(generator = "UUID")
@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
private UUID id;
```

**Рекомендации:**
- Для PostgreSQL и Oracle используйте `SEQUENCE`
- Для MySQL используйте `IDENTITY`
- Для распределённых систем рассмотрите UUID
- Избегайте `TABLE` из-за низкой производительности

## Маппинг полей и типов данных

### Базовые типы

Hibernate автоматически маппит стандартные Java-типы на соответствующие SQL-типы:

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    private String name;           // VARCHAR
    private int quantity;          // INTEGER
    private BigDecimal price;      // DECIMAL/NUMERIC
    private boolean available;     // BOOLEAN
    private double weight;         // DOUBLE
}
```

### Временные типы

Для работы с датами и временем:

```java
// Java 8+ Date/Time API (рекомендуется)
@Column(name = "created_at")
private LocalDateTime createdAt;

@Column(name = "birth_date")
private LocalDate birthDate;

@Column(name = "order_time")
private LocalTime orderTime;

@Column(name = "last_modified")
private Instant lastModified;

// Legacy java.util.Date (не рекомендуется)
@Temporal(TemporalType.TIMESTAMP)
private Date createdDate;

@Temporal(TemporalType.DATE)
private Date birthDate;
```

### Enum типы

```java
public enum Status {
    ACTIVE, INACTIVE, PENDING
}

@Entity
public class Order {
    @Id
    private Long id;
    
    // Сохраняет имя enum как строку (рекомендуется)
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status;
    
    // Сохраняет порядковый номер enum (не рекомендуется - хрупко при изменении порядка)
    @Enumerated(EnumType.ORDINAL)
    private Status oldStatus;
}
```

**Важно:** Всегда используйте `EnumType.STRING` для избежания проблем при изменении порядка значений enum.

### LOB типы

Для больших объектов (Large Objects):

```java
@Entity
public class Document {
    @Id
    private Long id;
    
    // Текстовый LOB
    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;
    
    // Бинарный LOB
    @Lob
    @Column(columnDefinition = "BLOB")
    private byte[] fileData;
}
```

## Связи между сущностями

### @OneToOne

Связь один-к-одному, когда одной записи в таблице A соответствует ровно одна запись в таблице B:

```java
@Entity
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    // Владеющая сторона связи (имеет внешний ключ)
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id", referencedColumnName = "id")
    private UserProfile profile;
}

@Entity
public class UserProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bio;
    private String avatarUrl;
    
    // Обратная сторона связи (mappedBy указывает на поле в User)
    @OneToOne(mappedBy = "profile")
    private User user;
}
```

**Важно:** Используйте `mappedBy` на обратной стороне, чтобы избежать дублирования внешнего ключа.

### @OneToMany и @ManyToOne

Связь один-ко-многим — наиболее распространённый тип связи:

```java
@Entity
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    // Один департамент имеет много сотрудников
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Employee> employees = new ArrayList<>();
    
    // Вспомогательный метод для управления двунаправленной связью
    public void addEmployee(Employee employee) {
        employees.add(employee);
        employee.setDepartment(this);
    }
    
    public void removeEmployee(Employee employee) {
        employees.remove(employee);
        employee.setDepartment(null);
    }
}

@Entity
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    // Много сотрудников принадлежат одному департаменту
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
}
```

**Best practice:** Всегда управляйте двунаправленными связями через вспомогательные методы, чтобы гарантировать консистентность.

### @ManyToMany

Связь многие-ко-многим требует промежуточной таблицы:

```java
@Entity
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    @ManyToMany
    @JoinTable(
        name = "student_course",
        joinColumns = @JoinColumn(name = "student_id"),
        inverseJoinColumns = @JoinColumn(name = "course_id")
    )
    private Set<Course> courses = new HashSet<>();
}

@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    
    @ManyToMany(mappedBy = "courses")
    private Set<Student> students = new HashSet<>();
}
```

**Альтернатива:** Для гибкости часто предпочитают заменить `@ManyToMany` на две связи `@OneToMany` с явной сущностью для промежуточной таблицы:

```java
@Entity
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;
    
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;
    
    private LocalDateTime enrolledAt;
    private String grade;
}
```

### Каскадные операции

Каскадные операции определяют, какие действия автоматически применяются к связанным сущностям:

```java
@OneToMany(
    mappedBy = "parent",
    cascade = {CascadeType.PERSIST, CascadeType.MERGE},
    orphanRemoval = true
)
private List<Child> children;
```

**Типы каскадирования:**
- `CascadeType.PERSIST` — при сохранении родителя сохраняются дети
- `CascadeType.MERGE` — при обновлении родителя обновляются дети
- `CascadeType.REMOVE` — при удалении родителя удаляются дети
- `CascadeType.REFRESH` — при обновлении из БД обновляются дети
- `CascadeType.DETACH` — при отсоединении родителя отсоединяются дети
- `CascadeType.ALL` — все вышеперечисленные операции

**orphanRemoval = true** — автоматически удаляет дочерние сущности, которые больше не связаны с родителем.

### FetchType — стратегии загрузки

```java
// LAZY - загрузка по требованию (рекомендуется для коллекций)
@OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
private List<Employee> employees;

// EAGER - немедленная загрузка (осторожно, может вызвать N+1)
@ManyToOne(fetch = FetchType.EAGER)
private Department department;
```

**Стратегии по умолчанию:**
- `@OneToOne`, `@ManyToOne` → `EAGER`
- `@OneToMany`, `@ManyToMany` → `LAZY`

**Рекомендация:** Используйте `LAZY` везде, где возможно, и явно указывайте `fetch` для ясности.

## Контекст персистентности (Persistence Context)

### Состояния сущностей

Сущность в Hibernate может находиться в одном из четырёх состояний:

```
  New (Transient)
       ↓ persist()
  Managed (Persistent) ←→ merge()
       ↓ remove()           ↓ detach()
   Removed         Detached
```

1. **Transient (New)** — новый объект, не связанный с persistence context и БД:
   ```java
   User user = new User("john", "john@example.com");
   // user в состоянии Transient
   ```

2. **Persistent (Managed)** — объект управляется EntityManager, изменения отслеживаются:
   ```java
   entityManager.persist(user);
   // user теперь в состоянии Persistent
   user.setEmail("newemail@example.com");
   // Изменение будет автоматически сохранено при commit
   ```

3. **Detached** — объект был persistent, но потерял связь с context:
   ```java
   entityManager.detach(user);
   // или после закрытия EntityManager
   // Изменения detached объекта НЕ синхронизируются с БД
   ```

4. **Removed** — объект помечен для удаления:
   ```java
   entityManager.remove(user);
   // user будет удалён из БД при commit
   ```

### EntityManager и его методы

`EntityManager` — основной интерфейс для работы с сущностями:

```java
// Создание и сохранение новой сущности
User user = new User("john", "john@example.com");
entityManager.persist(user);  // Transient → Persistent

// Поиск по первичному ключу
User found = entityManager.find(User.class, 1L);

// Ленивый поиск (возвращает прокси, запрос к БД при первом обращении)
User reference = entityManager.getReference(User.class, 1L);

// Обновление detached сущности
User detached = // получен из другой сессии
User merged = entityManager.merge(detached);  // Detached → Persistent

// Удаление
entityManager.remove(user);  // Persistent → Removed

// Отсоединение от контекста
entityManager.detach(user);  // Persistent → Detached

// Обновление из БД (откат изменений)
entityManager.refresh(user);

// Проверка состояния
boolean contains = entityManager.contains(user);
```

### Flush и синхронизация

`flush()` синхронизирует изменения persistence context с базой данных:

```java
User user = entityManager.find(User.class, 1L);
user.setEmail("newemail@example.com");

// Изменения пока только в памяти
entityManager.flush();  // SQL UPDATE будет выполнен немедленно

// Без flush изменения будут отправлены в БД автоматически:
// - При commit транзакции
// - Перед выполнением запроса, который может зависеть от изменений
// - При явном вызове flush()
```

**FlushMode:**
- `FlushModeType.AUTO` (по умолчанию) — flush перед запросами и при commit
- `FlushModeType.COMMIT` — flush только при commit

## Кеширование

Hibernate использует многоуровневое кеширование для повышения производительности.

### Кеш первого уровня

**Session Cache (L1 Cache)** — автоматический кеш на уровне EntityManager/Session, всегда включён:

```java
// Первый запрос к БД
User user1 = entityManager.find(User.class, 1L);

// Второй find с тем же ID НЕ делает запрос к БД
User user2 = entityManager.find(User.class, 1L);

// user1 == user2 (один и тот же объект)
assert user1 == user2;
```

Кеш первого уровня:
- Гарантирует уникальность объектов в рамках сессии
- Автоматически очищается при закрытии EntityManager
- Нельзя отключить

**Очистка кеша:**
```java
entityManager.clear();  // Очистить весь кеш
entityManager.detach(user);  // Удалить конкретную сущность
```

### Кеш второго уровня

**Second Level Cache (L2 Cache)** — опциональный кеш на уровне SessionFactory, разделяемый между сессиями:

```java
// 1. Подключить провайдер кеша (например, Ehcache)
// hibernate.properties:
// hibernate.cache.use_second_level_cache=true
// hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory

// 2. Пометить сущность как cacheable
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Product {
    @Id
    private Long id;
    private String name;
    
    // Кешировать также коллекцию
    @OneToMany(mappedBy = "product")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Review> reviews;
}
```

**Стратегии согласованности (CacheConcurrencyStrategy):**
- `READ_ONLY` — для immutable данных (максимальная производительность)
- `NONSTRICT_READ_WRITE` — мягкая согласованность, допускаются stale данные
- `READ_WRITE` — строгая согласованность через блокировки (по умолчанию)
- `TRANSACTIONAL` — полная ACID согласованность (требует JTA)

### Query Cache

Кеширование результатов запросов:

```java
// Включить query cache
// hibernate.cache.use_query_cache=true

// Кешировать конкретный запрос
List<Product> products = entityManager
    .createQuery("SELECT p FROM Product p WHERE p.category = :cat", Product.class)
    .setParameter("cat", "Electronics")
    .setHint("org.hibernate.cacheable", true)
    .getResultList();
```

**Важно:** Query cache хранит только ID сущностей, сами сущности берутся из L2 cache. Поэтому L2 cache должен быть включён.

## Запросы к данным

### HQL (Hibernate Query Language)

HQL — объектно-ориентированный язык запросов Hibernate, работающий с сущностями, а не таблицами:

```java
// Простой SELECT
String hql = "FROM User u WHERE u.username = :username";
User user = session.createQuery(hql, User.class)
    .setParameter("username", "john")
    .uniqueResult();

// JOIN
String hql = "SELECT u FROM User u JOIN u.orders o WHERE o.total > :amount";
List<User> users = session.createQuery(hql, User.class)
    .setParameter("amount", new BigDecimal("1000"))
    .list();

// Агрегация
String hql = "SELECT u.department, COUNT(u) FROM User u GROUP BY u.department";
List<Object[]> result = session.createQuery(hql).list();

// UPDATE
String hql = "UPDATE User u SET u.active = false WHERE u.lastLogin < :date";
int updated = session.createQuery(hql)
    .setParameter("date", cutoffDate)
    .executeUpdate();

// DELETE
String hql = "DELETE FROM Order o WHERE o.status = :status";
int deleted = session.createQuery(hql)
    .setParameter("status", OrderStatus.CANCELLED)
    .executeUpdate();
```

### JPQL (Java Persistence Query Language)

JPQL — стандартизированная версия HQL, часть спецификации JPA. Синтаксис очень похож:

```java
String jpql = "SELECT u FROM User u WHERE u.email LIKE :pattern";
List<User> users = entityManager
    .createQuery(jpql, User.class)
    .setParameter("pattern", "%@example.com")
    .getResultList();

// Пагинация
jpql = "SELECT u FROM User u ORDER BY u.createdAt DESC";
List<User> page = entityManager
    .createQuery(jpql, User.class)
    .setFirstResult(20)  // offset
    .setMaxResults(10)   // limit
    .getResultList();
```

### Criteria API

Type-safe построение запросов программно:

```java
CriteriaBuilder cb = entityManager.getCriteriaBuilder();
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> user = query.from(User.class);

// WHERE username = ? AND active = true
query.select(user)
    .where(
        cb.and(
            cb.equal(user.get("username"), "john"),
            cb.isTrue(user.get("active"))
        )
    );

List<User> results = entityManager.createQuery(query).getResultList();

// JOIN и сложные условия
CriteriaQuery<User> query = cb.createQuery(User.class);
Root<User> user = query.from(User.class);
Join<User, Order> orders = user.join("orders");

query.select(user)
    .where(cb.greaterThan(orders.get("total"), new BigDecimal("500")))
    .distinct(true);

List<User> richCustomers = entityManager.createQuery(query).getResultList();
```

**Преимущества Criteria API:**
- Проверка типов на этапе компиляции
- Удобно для динамических запросов
- Рефакторинг через IDE

**Недостатки:**
- Более verbose код
- Менее читаемо, чем HQL/JPQL

### Native SQL

Выполнение нативных SQL-запросов:

```java
// Простой native query
String sql = "SELECT * FROM users WHERE username = :username";
User user = (User) entityManager
    .createNativeQuery(sql, User.class)
    .setParameter("username", "john")
    .getSingleResult();

// Native query с маппингом на результат
String sql = "SELECT u.id, u.username, COUNT(o.id) as order_count " +
             "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
             "GROUP BY u.id, u.username";

@SqlResultSetMapping(
    name = "UserOrderCountMapping",
    classes = @ConstructorResult(
        targetClass = UserOrderCount.class,
        columns = {
            @ColumnResult(name = "id", type = Long.class),
            @ColumnResult(name = "username", type = String.class),
            @ColumnResult(name = "order_count", type = Long.class)
        }
    )
)
// Использование
List<UserOrderCount> results = entityManager
    .createNativeQuery(sql, "UserOrderCountMapping")
    .getResultList();
```

### Named Queries

Предопределённые запросы, описанные в аннотациях:

```java
@Entity
@NamedQuery(
    name = "User.findByEmail",
    query = "SELECT u FROM User u WHERE u.email = :email"
)
@NamedQuery(
    name = "User.findActive",
    query = "SELECT u FROM User u WHERE u.active = true"
)
public class User {
    // ...
}

// Использование
User user = entityManager
    .createNamedQuery("User.findByEmail", User.class)
    .setParameter("email", "john@example.com")
    .getSingleResult();
```

**Преимущества Named Queries:**
- Проверка синтаксиса при старте приложения
- Переиспользование запросов
- Централизация

## Транзакции и блокировки

### Управление транзакциями

```java
EntityManager em = entityManagerFactory.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();
    
    User user = new User("john", "john@example.com");
    em.persist(user);
    
    // Выполнить операции...
    
    tx.commit();
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback();
    }
    throw e;
} finally {
    em.close();
}
```

**В Spring используется декларативное управление:**
```java
@Service
public class UserService {
    @Autowired
    private EntityManager entityManager;
    
    @Transactional
    public void createUser(String username, String email) {
        User user = new User(username, email);
        entityManager.persist(user);
        // Транзакция commit автоматически
    }
}
```

### Оптимистичные блокировки

Используются для обнаружения конфликтов при параллельных изменениях:

```java
@Entity
public class Product {
    @Id
    private Long id;
    
    private String name;
    
    @Version
    private Long version;  // Hibernate автоматически управляет версией
}

// Использование
Product product = em.find(Product.class, 1L);
product.setName("New Name");

// Если другая транзакция изменила product между find и commit,
// будет выброшен OptimisticLockException
em.getTransaction().commit();
```

**Явное указание lock mode:**
```java
Product product = em.find(Product.class, 1L, LockModeType.OPTIMISTIC);
```

### Пессимистичные блокировки

Блокируют строки в БД на время транзакции:

```java
// Эксклюзивная блокировка (FOR UPDATE)
Product product = em.find(Product.class, 1L, LockModeType.PESSIMISTIC_WRITE);

// Разделяемая блокировка (FOR SHARE)
Product product = em.find(Product.class, 1L, LockModeType.PESSIMISTIC_READ);

// С таймаутом
em.find(Product.class, 1L, LockModeType.PESSIMISTIC_WRITE, 
    Collections.singletonMap("javax.persistence.lock.timeout", 5000));
```

**Типы пессимистичных блокировок:**
- `PESSIMISTIC_READ` — разделяемая блокировка (другие могут читать)
- `PESSIMISTIC_WRITE` — эксклюзивная блокировка (никто не может изменять)
- `PESSIMISTIC_FORCE_INCREMENT` — эксклюзивная + инкремент версии

## Производительность и оптимизация

### Проблема N+1

Классическая проблема производительности ORM:

```java
// BAD: вызовет 1 запрос для users + N запросов для orders каждого user
List<User> users = em.createQuery("SELECT u FROM User u", User.class).getResultList();
for (User user : users) {
    System.out.println(user.getOrders().size());  // Ленивая загрузка
}

// GOOD: один запрос с JOIN FETCH
List<User> users = em.createQuery(
    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.orders", 
    User.class
).getResultList();
```

### Batch fetching

Уменьшает количество запросов при ленивой загрузке:

```java
@Entity
public class User {
    @Id
    private Long id;
    
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @BatchSize(size = 10)  // Загружать orders для 10 users одним запросом
    private List<Order> orders;
}
```

### Entity graphs

Декларативное указание, что загружать:

```java
@Entity
@NamedEntityGraph(
    name = "User.orders",
    attributeNodes = @NamedAttributeNode("orders")
)
public class User {
    // ...
}

// Использование
EntityGraph<?> graph = em.getEntityGraph("User.orders");
Map<String, Object> hints = new HashMap<>();
hints.put("javax.persistence.fetchgraph", graph);
User user = em.find(User.class, 1L, hints);
```

**Динамический entity graph:**
```java
EntityGraph<User> graph = em.createEntityGraph(User.class);
graph.addAttributeNodes("orders", "profile");

Map<String, Object> hints = new HashMap<>();
hints.put("javax.persistence.fetchgraph", graph);
User user = em.find(User.class, 1L, hints);
```

### Статистика Hibernate

Анализ производительности:

```java
// Включить статистику
// hibernate.generate_statistics=true

Statistics stats = sessionFactory.getStatistics();
stats.setStatisticsEnabled(true);

// Выполнить операции...

// Анализ
System.out.println("Queries: " + stats.getQueryExecutionCount());
System.out.println("Cache hits: " + stats.getSecondLevelCacheHitCount());
System.out.println("Cache misses: " + stats.getSecondLevelCacheMissCount());
```

## Наследование

Hibernate поддерживает три стратегии маппинга наследования:

### SINGLE_TABLE

Все классы иерархии в одной таблице (стратегия по умолчанию):

```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.STRING)
public abstract class Payment {
    @Id
    @GeneratedValue
    private Long id;
    
    private BigDecimal amount;
}

@Entity
@DiscriminatorValue("CREDIT_CARD")
public class CreditCardPayment extends Payment {
    private String cardNumber;
}

@Entity
@DiscriminatorValue("BANK_TRANSFER")
public class BankTransferPayment extends Payment {
    private String bankAccount;
}
```

**Плюсы:** Быстрые запросы, простая структура
**Минусы:** Много NULL-полей, нет NOT NULL constraints для подклассов

### JOINED

Каждый класс в отдельной таблице, связанной JOIN:

```java
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Payment {
    @Id
    @GeneratedValue
    private Long id;
    
    private BigDecimal amount;
}

@Entity
@Table(name = "credit_card_payment")
public class CreditCardPayment extends Payment {
    private String cardNumber;
}

@Entity
@Table(name = "bank_transfer_payment")
public class BankTransferPayment extends Payment {
    private String bankAccount;
}
```

**Плюсы:** Нормализованная структура, нет NULL-полей
**Минусы:** Медленнее из-за JOIN

### TABLE_PER_CLASS

Каждый конкретный класс в отдельной таблице:

```java
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE)
    private Long id;
    
    private BigDecimal amount;
}

@Entity
@Table(name = "credit_card_payment")
public class CreditCardPayment extends Payment {
    private String cardNumber;
}
```

**Плюсы:** Простая структура, нет NULL
**Минусы:** Сложные полиморфные запросы, дублирование колонок

## Дополнительные возможности

### Embeddable классы

Value objects, встраиваемые в другие сущности:

```java
@Embeddable
public class Address {
    private String street;
    private String city;
    private String zipCode;
    private String country;
    
    // Конструкторы, getters, setters
}

@Entity
public class User {
    @Id
    private Long id;
    
    private String name;
    
    @Embedded
    private Address address;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "billing_street")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city"))
    })
    private Address billingAddress;
}
```

### Composite keys

Составные первичные ключи:

```java
// Вариант 1: @EmbeddedId
@Embeddable
public class OrderItemId implements Serializable {
    private Long orderId;
    private Long productId;
    
    // equals, hashCode обязательны
}

@Entity
public class OrderItem {
    @EmbeddedId
    private OrderItemId id;
    
    private Integer quantity;
}

// Вариант 2: @IdClass
@IdClass(OrderItemId.class)
@Entity
public class OrderItem {
    @Id
    private Long orderId;
    
    @Id
    private Long productId;
    
    private Integer quantity;
}
```

### Lifecycle callbacks

Методы, выполняемые в определённые моменты жизненного цикла:

```java
@Entity
public class User {
    @Id
    private Long id;
    
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    @PostLoad
    protected void onLoad() {
        // Выполнится после загрузки из БД
    }
}
```

**Доступные callbacks:**
- `@PrePersist` — перед сохранением
- `@PostPersist` — после сохранения
- `@PreUpdate` — перед обновлением
- `@PostUpdate` — после обновления
- `@PreRemove` — перед удалением
- `@PostRemove` — после удаления
- `@PostLoad` — после загрузки

### Entity listeners

Вынесение логики в отдельные классы:

```java
public class AuditListener {
    @PrePersist
    public void setCreatedDate(Object entity) {
        if (entity instanceof Auditable) {
            ((Auditable) entity).setCreatedAt(LocalDateTime.now());
        }
    }
    
    @PreUpdate
    public void setUpdatedDate(Object entity) {
        if (entity instanceof Auditable) {
            ((Auditable) entity).setUpdatedAt(LocalDateTime.now());
        }
    }
}

@Entity
@EntityListeners(AuditListener.class)
public class User implements Auditable {
    // ...
}
```

## Практические заметки

### Распространённые ошибки

**1. LazyInitializationException:**
```java
// BAD: попытка доступа к lazy коллекции вне сессии
User user = userRepository.findById(1L);
// Сессия закрыта
user.getOrders().size();  // LazyInitializationException!

// GOOD: загрузить заранее
User user = em.createQuery(
    "SELECT u FROM User u LEFT JOIN FETCH u.orders WHERE u.id = :id",
    User.class
).setParameter("id", 1L).getSingleResult();
```

**2. Детальные сущности в API responses:**
```java
// BAD: возврат entities с циклическими ссылками
@GetMapping("/users/{id}")
public User getUser(@PathVariable Long id) {
    return userRepository.findById(id);  // Может вызвать бесконечную сериализацию!
}

// GOOD: использовать DTO
@GetMapping("/users/{id}")
public UserDTO getUser(@PathVariable Long id) {
    User user = userRepository.findById(id);
    return UserDTO.fromEntity(user);
}
```

**3. Использование entity как ключа в HashMap:**
```java
// BAD: если id ещё не назначен
User user = new User("john", "john@example.com");
map.put(user, "value");  // hashCode основан на id=null
em.persist(user);  // id теперь назначен
map.get(user);  // может не найти! hashCode изменился
```

### Best practices

1. **Всегда используйте конструкторную инъекцию для зависимостей:**
   ```java
   @Service
   public class UserService {
       private final UserRepository repository;
       
       public UserService(UserRepository repository) {
           this.repository = repository;
       }
   }
   ```

2. **Предпочитайте LAZY loading, загружайте EAGER только когда точно нужно:**
   ```java
   @OneToMany(fetch = FetchType.LAZY)
   private List<Order> orders;
   ```

3. **Используйте @Transactional на уровне сервиса, а не репозитория:**
   ```java
   @Service
   public class OrderService {
       @Transactional
       public void processOrder(Long orderId) {
           // Вся логика в одной транзакции
       }
   }
   ```

4. **Не возвращайте entities из REST API, используйте DTO:**
   ```java
   public class UserDTO {
       private Long id;
       private String username;
       
       public static UserDTO fromEntity(User user) {
           UserDTO dto = new UserDTO();
           dto.setId(user.getId());
           dto.setUsername(user.getUsername());
           return dto;
       }
   }
   ```

5. **Используйте оптимистичные блокировки для большинства случаев:**
   ```java
   @Version
   private Long version;
   ```

6. **Включайте show_sql только для разработки:**
   ```properties
   # Development
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.format_sql=true
   
   # Production
   spring.jpa.show-sql=false
   ```

### Миграция схемы БД

Hibernate может автоматически управлять схемой, но в production используйте инструменты миграции:

```properties
# Только для development!
spring.jpa.hibernate.ddl-auto=update

# Production - никогда не используйте auto DDL!
spring.jpa.hibernate.ddl-auto=validate

# Используйте Flyway или Liquibase для миграций
spring.flyway.enabled=true
```

**Значения ddl-auto:**
- `none` — ничего не делать
- `validate` — проверить соответствие схемы (recommended для production)
- `update` — обновить схему (опасно, может потерять данные!)
- `create` — пересоздать схему при старте
- `create-drop` — создать при старте, удалить при остановке

## Практические упражнения

1. **Создайте модель интернет-магазина:**
   - Сущности: User, Product, Order, OrderItem
   - Реализуйте все связи
   - Добавьте оптимистичные блокировки
   - Настройте кеш второго уровня

2. **Оптимизируйте запросы:**
   - Найдите и устраните проблему N+1
   - Используйте batch fetching
   - Примените entity graphs

3. **Реализуйте audit trail:**
   - Создайте @MappedSuperclass для аудита
   - Используйте @EntityListeners
   - Храните историю изменений

4. **Реализуйте soft delete:**
   - Добавьте поле deleted
   - Используйте @Where для фильтрации
   - Создайте custom repository методы

## Вопросы на собеседовании

1. **В чём разница между JPA и Hibernate?**
   
   *Ответ:* JPA — это спецификация (набор интерфейсов и аннотаций), определяющая стандарт работы с ORM в Java. Hibernate — конкретная реализация JPA, которая также предоставляет дополнительные возможности, не входящие в спецификацию (HQL расширения, специфичные аннотации, расширенное кеширование и др.). Другие реализации JPA: EclipseLink, OpenJPA.

2. **Какие состояния может иметь entity в Hibernate?**
   
   *Ответ:* Transient (new) — новый объект, не связан с БД; Persistent (managed) — управляется EntityManager, изменения синхронизируются; Detached — был persistent, но потерял связь с контекстом; Removed — помечен для удаления.

3. **Что такое проблема N+1 и как её решить?**
   
   *Ответ:* N+1 возникает, когда загружается N сущностей одним запросом, а затем для каждой выполняется дополнительный запрос для загрузки связанных данных (итого 1+N запросов). Решения: JOIN FETCH в HQL/JPQL, @BatchSize, entity graphs, или eager fetching (с осторожностью).

4. **В чём разница между FetchType.LAZY и FetchType.EAGER?**
   
   *Ответ:* LAZY — связанные данные загружаются только при первом обращении к ним (ленивая загрузка). EAGER — связанные данные загружаются немедленно вместе с основной сущностью. По умолчанию @OneToMany и @ManyToMany — LAZY, @OneToOne и @ManyToOne — EAGER. Рекомендуется использовать LAZY везде, где возможно.

5. **Объясните разницу между persist() и merge().**
   
   *Ответ:* persist() — делает transient объект persistent, объект начинает отслеживаться EntityManager. merge() — копирует состояние detached объекта в persistent объект (или создаёт новый persistent), возвращает managed копию. persist() используется для новых объектов, merge() — для обновления detached.

6. **Что такое Persistence Context и зачем он нужен?**
   
   *Ответ:* Persistence Context — это кеш первого уровня, содержащий managed сущности. Он гарантирует уникальность объектов (одна строка БД = один объект в памяти), отслеживает изменения для автоматической синхронизации с БД, откладывает SQL до момента flush/commit для оптимизации.

7. **Какие стратегии наследования поддерживает JPA?**
   
   *Ответ:* SINGLE_TABLE (все классы в одной таблице с discriminator колонкой), JOINED (каждый класс в своей таблице, связанной через JOIN), TABLE_PER_CLASS (каждый конкретный класс в отдельной таблице). Выбор зависит от требований: SINGLE_TABLE — быстрее, JOINED — нормализованнее.

8. **Как работает оптимистичная блокировка?**
   
   *Ответ:* Используется поле с @Version (обычно Long или Timestamp). При каждом обновлении версия инкрементируется. Перед UPDATE Hibernate проверяет, что версия в БД совпадает с версией в объекте. Если не совпадает — значит данные были изменены другой транзакцией, выбрасывается OptimisticLockException.

9. **В чём разница между save() и persist() в Hibernate?**
   
   *Ответ:* persist() — JPA-стандартный метод, не возвращает значение, гарантирует, что объект будет persistent внутри транзакции. save() — Hibernate-специфичный, возвращает сгенерированный ID, может вызывать INSERT немедленно. В современном коде предпочтительнее persist().

10. **Что такое LazyInitializationException и как её избежать?**
    
    *Ответ:* Возникает при попытке доступа к lazy-коллекции или proxy после закрытия Session. Решения: загрузить данные внутри транзакции (JOIN FETCH), использовать Open Session In View (антипаттерн для production), использовать DTO вместо entities, настроить eager fetching (с осторожностью).

11. **Объясните работу кеша второго уровня.**
    
    *Ответ:* L2 cache — опциональный кеш на уровне SessionFactory, разделяемый между сессиями. Хранит сущности по ID. Требует внешний провайдер (Ehcache, Infinispan). Поддерживает разные стратегии согласованности: READ_ONLY, READ_WRITE, NONSTRICT_READ_WRITE, TRANSACTIONAL. Требует careful configuration для избежания stale data.

12. **Когда использовать HQL/JPQL, а когда Criteria API?**
    
    *Ответ:* HQL/JPQL — для статических запросов, более читаемы, проще поддерживать. Criteria API — для динамических запросов с условиями, определяемыми в runtime, type-safe, удобен для сложной фильтрации. Также Criteria API помогает избежать SQL injection.

13. **Что такое cascade и orphanRemoval?**
    
    *Ответ:* cascade определяет, какие операции (PERSIST, MERGE, REMOVE и т.д.) автоматически применяются к связанным сущностям. orphanRemoval=true автоматически удаляет дочерние сущности, которые больше не связаны с родителем. Например, при удалении элемента из коллекции с orphanRemoval=true, элемент будет удалён из БД.

14. **Как настроить Hibernate для показа сгенерированного SQL?**
    
    *Ответ:* 
    ```properties
    spring.jpa.show-sql=true
    spring.jpa.properties.hibernate.format_sql=true
    spring.jpa.properties.hibernate.use_sql_comments=true
    logging.level.org.hibernate.SQL=DEBUG
    logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE
    ```

15. **В чём разница между @JoinColumn и mappedBy?**
    
    *Ответ:* @JoinColumn определяет владеющую сторону связи и указывает имя внешнего ключа в БД. mappedBy используется на обратной (non-owning) стороне и указывает на поле владеющей стороны. Только владеющая сторона может изменять связь в БД. mappedBy избегает дублирования внешних ключей.

## Дополнительные материалы

- [Hibernate ORM Documentation](https://hibernate.org/orm/documentation/) — официальная документация
- [Java Persistence API (JPA) Specification](https://jakarta.ee/specifications/persistence/) — спецификация JPA
- [Vlad Mihalcea's Blog](https://vladmihalcea.com/) — углублённые статьи по Hibernate и производительности
- [Baeldung Hibernate Tutorials](https://www.baeldung.com/hibernate-5) — практические туториалы
- [Thorben Janssen's Blog](https://thorben-janssen.com/) — best practices и advanced topics
- Книга: "Java Persistence with Hibernate" by Christian Bauer and Gavin King
- Книга: "High-Performance Java Persistence" by Vlad Mihalcea

---

[← Назад к Java](README.md)
