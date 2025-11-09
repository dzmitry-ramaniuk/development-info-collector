# Spring Data


## Содержание

1. [Введение](#введение)
2. [Ключевые концепции](#ключевые-концепции)
   - [Унифицированная работа с репозиториями](#унифицированная-работа-с-репозиториями)
   - [Автоматическая реализация репозиториев](#автоматическая-реализация-репозиториев)
   - [Query Derivation Mechanism](#query-derivation-mechanism)
3. [Repository интерфейсы](#repository-интерфейсы)
   - [Repository — базовый маркер](#repository-базовый-маркер)
   - [CrudRepository — CRUD операции](#crudrepository-crud-операции)
   - [PagingAndSortingRepository — пагинация и сортировка](#pagingandsortingrepository-пагинация-и-сортировка)
   - [JpaRepository — расширенные возможности JPA](#jparepository-расширенные-возможности-jpa)
4. [Генерация запросов по имени методов (Query Derivation)](#генерация-запросов-по-имени-методов-query-derivation)
   - [Структура имени метода](#структура-имени-метода)
   - [Примеры генерации запросов](#примеры-генерации-запросов)
   - [Вложенные свойства](#вложенные-свойства)
5. [Кастомные запросы](#кастомные-запросы)
   - [@Query — JPQL и native SQL](#query-jpql-и-native-sql)
   - [@Modifying — UPDATE и DELETE запросы](#modifying-update-и-delete-запросы)
   - [@NamedQuery и @NamedNativeQuery](#namedquery-и-namednativequery)
   - [QueryDSL интеграция](#querydsl-интеграция)
6. [Specifications — гибкое построение запросов](#specifications-гибкое-построение-запросов)
   - [Использование Specification](#использование-specification)
   - [Сложные спецификации с Join](#сложные-спецификации-с-join)
7. [Проекции](#проекции)
   - [Interface-based проекции](#interface-based-проекции)
   - [Закрытые проекции (Closed Projections)](#закрытые-проекции-closed-projections)
   - [Открытые проекции (Open Projections)](#открытые-проекции-open-projections)
   - [Class-based проекции (DTO)](#class-based-проекции-dto)
   - [Динамические проекции](#динамические-проекции)
   - [Вложенные проекции](#вложенные-проекции)
8. [Аудит сущностей](#аудит-сущностей)
   - [Включение аудита](#включение-аудита)
   - [Аудируемые сущности](#аудируемые-сущности)
   - [Базовый класс для аудита](#базовый-класс-для-аудита)
9. [Транзакции в Spring Data](#транзакции-в-spring-data)
   - [Транзакции на уровне репозитория](#транзакции-на-уровне-репозитория)
   - [Транзакции на уровне сервиса](#транзакции-на-уровне-сервиса)
   - [Propagation — управление границами транзакций](#propagation-управление-границами-транзакций)
10. [Поддержка NoSQL](#поддержка-nosql)
   - [Spring Data MongoDB](#spring-data-mongodb)
   - [Spring Data Redis](#spring-data-redis)
   - [Spring Data Cassandra](#spring-data-cassandra)
11. [Spring Data REST](#spring-data-rest)
   - [Базовая конфигурация](#базовая-конфигурация)
   - [Автоматически генерируемые эндпоинты](#автоматически-генерируемые-эндпоинты)
   - [Кастомизация REST ресурсов](#кастомизация-rest-ресурсов)
   - [Обработка событий REST](#обработка-событий-rest)
12. [Практические заметки](#практические-заметки)
   - [Выбор правильного интерфейса репозитория](#выбор-правильного-интерфейса-репозитория)
   - [Оптимизация производительности](#оптимизация-производительности)
   - [Обработка больших объёмов данных](#обработка-больших-объёмов-данных)
   - [Проблемы производительности пагинации (Offset/Limit)](#проблемы-производительности-пагинации-offsetlimit)
   - [Интеграция с аудитом и безопасностью](#интеграция-с-аудитом-и-безопасностью)
   - [Кеширование запросов](#кеширование-запросов)
   - [Тестирование репозиториев](#тестирование-репозиториев)
13. [Практические упражнения](#практические-упражнения)
14. [Вопросы на собеседовании](#вопросы-на-собеседовании)
15. [Дополнительные материалы](#дополнительные-материалы)

## Введение

Spring Data — это umbrella-проект, упрощающий доступ к различным системам хранения данных через единообразный API. Основная цель — минимизировать boilerplate-код при работе с базами данных, сохраняя при этом гибкость и производительность.

**Историческая справка.** До Spring Data разработчики писали множество повторяющегося кода для CRUD-операций: DAO-классы с однотипными методами findById, save, delete. Spring Data появился в 2011 году и предложил революционный подход — автоматическую генерацию реализаций репозиториев на основе интерфейсов. Первоначально проект фокусировался на JPA, но быстро расширился для поддержки NoSQL-систем (MongoDB, Redis, Cassandra).

**Семейство Spring Data проектов:**
- **Spring Data JPA** — работа с реляционными БД через JPA
- **Spring Data MongoDB** — доступ к MongoDB
- **Spring Data Redis** — интеграция с Redis
- **Spring Data Cassandra** — поддержка Apache Cassandra
- **Spring Data Elasticsearch** — работа с Elasticsearch
- **Spring Data JDBC** — облегчённый доступ к реляционным БД без JPA
- **Spring Data REST** — автоматическое создание REST API для репозиториев
- **Spring Data R2DBC** — реактивный доступ к реляционным БД

Все проекты следуют единой концепции репозиториев, что позволяет легко переключаться между разными источниками данных.

## Ключевые концепции

### Унифицированная работа с репозиториями

Spring Data предоставляет иерархию интерфейсов-репозиториев, абстрагирующих работу с данными:

```
Repository (marker interface)
    ├── CrudRepository (базовые CRUD операции)
    │       ├── PagingAndSortingRepository (пагинация и сортировка)
    │       │       └── JpaRepository (JPA-специфичные методы)
    │       └── ReactiveCrudRepository (реактивные CRUD)
    └── RxJava3CrudRepository (RxJava интерфейс)
```

### Автоматическая реализация репозиториев

Вместо написания реализаций Spring Data генерирует их во время выполнения на основе сигнатур методов интерфейса.

### Query Derivation Mechanism

Механизм автоматической генерации запросов по имени метода — ключевая фича Spring Data, позволяющая описывать запросы декларативно.

## Repository интерфейсы

### Repository — базовый маркер

`Repository<T, ID>` — пустой интерфейс-маркер, обозначающий, что интерфейс является репозиторием Spring Data. Используется как основа для создания собственных базовых интерфейсов с минимальным набором методов:

```java
public interface MinimalUserRepository extends Repository<User, Long> {
    Optional<User> findById(Long id);
    User save(User user);
}
```

### CrudRepository — CRUD операции

`CrudRepository<T, ID>` предоставляет стандартный набор методов для создания, чтения, обновления и удаления:

```java
public interface UserRepository extends CrudRepository<User, Long> {
    // Методы наследуются автоматически:
    // save(S entity)
    // saveAll(Iterable<S> entities)
    // findById(ID id)
    // existsById(ID id)
    // findAll()
    // findAllById(Iterable<ID> ids)
    // count()
    // deleteById(ID id)
    // delete(T entity)
    // deleteAll(Iterable<? extends T> entities)
    // deleteAll()
}
```

**Использование:**

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public User createUser(User user) {
        return userRepository.save(user);
    }
    
    public Optional<User> getUser(Long id) {
        return userRepository.findById(id);
    }
    
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}
```

### PagingAndSortingRepository — пагинация и сортировка

`PagingAndSortingRepository<T, ID>` расширяет `CrudRepository`, добавляя методы для постраничного вывода и сортировки:

```java
public interface UserRepository extends PagingAndSortingRepository<User, Long> {
    // Дополнительные методы:
    // findAll(Sort sort)
    // findAll(Pageable pageable)
}
```

**Примеры использования:**

```java
// Сортировка
Sort sort = Sort.by(Sort.Direction.DESC, "createdDate")
    .and(Sort.by(Sort.Direction.ASC, "username"));
Iterable<User> users = userRepository.findAll(sort);

// Пагинация
Pageable pageable = PageRequest.of(0, 10, Sort.by("username"));
Page<User> userPage = userRepository.findAll(pageable);

System.out.println("Total elements: " + userPage.getTotalElements());
System.out.println("Total pages: " + userPage.getTotalPages());
System.out.println("Current page: " + userPage.getNumber());
List<User> users = userPage.getContent();
```

### JpaRepository — расширенные возможности JPA

`JpaRepository<T, ID>` — наиболее функциональный интерфейс для работы с JPA, добавляющий batch-операции и flush:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    // Дополнительные методы:
    // flush()
    // saveAndFlush(T entity)
    // saveAllAndFlush(Iterable<S> entities)
    // deleteAllInBatch(Iterable<T> entities)
    // deleteAllByIdInBatch(Iterable<ID> ids)
    // deleteAllInBatch()
    // getById(ID id) // возвращает ленивую ссылку
    // getReferenceById(ID id) // аналог getById
}
```

**Практическое применение:**

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public void updateUsersInBatch(List<User> users) {
        // Эффективное обновление множества сущностей
        userRepository.saveAll(users);
        userRepository.flush(); // Принудительная синхронизация с БД
    }
    
    @Transactional
    public void deleteInactiveUsers(List<Long> ids) {
        // Удаление одним запросом вместо множества DELETE
        userRepository.deleteAllByIdInBatch(ids);
    }
}
```

**Важно:** Методы `*InBatch` выполняют операции одним запросом к БД, минуя кеш первого уровня Hibernate. Это эффективно для массовых операций, но может привести к рассинхронизации кеша.

## Генерация запросов по имени методов (Query Derivation)

Spring Data анализирует имена методов репозитория и автоматически генерирует соответствующие запросы. Это позволяет избежать написания SQL/JPQL для типовых операций.

### Структура имени метода

Имя метода состоит из ключевых слов, определяющих тип операции и условия:

```
<prefix><criteria>By<condition>
```

**Префиксы:**
- `find...By`, `read...By`, `get...By`, `query...By`, `search...By`, `stream...By` — поиск сущностей
- `count...By` — подсчёт количества
- `exists...By` — проверка существования
- `delete...By`, `remove...By` — удаление

**Условия (keywords):**
- `And`, `Or` — логические операторы
- `Is`, `Equals` — равенство (можно опустить)
- `Between`, `LessThan`, `LessThanEqual`, `GreaterThan`, `GreaterThanEqual`
- `After`, `Before` — для дат
- `IsNull`, `IsNotNull`, `NotNull`
- `Like`, `NotLike`, `StartingWith`, `EndingWith`, `Containing`
- `OrderBy` — сортировка
- `Not`, `In`, `NotIn`
- `True`, `False` — для boolean полей
- `IgnoreCase`, `AllIgnoreCase` — игнорирование регистра

### Примеры генерации запросов

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Поиск по одному полю
    List<User> findByUsername(String username);
    // SELECT u FROM User u WHERE u.username = ?1
    
    // Поиск с несколькими условиями
    List<User> findByUsernameAndEmail(String username, String email);
    // SELECT u FROM User u WHERE u.username = ?1 AND u.email = ?2
    
    // Использование операторов сравнения
    List<User> findByAgeGreaterThan(int age);
    // SELECT u FROM User u WHERE u.age > ?1
    
    List<User> findByCreatedDateBetween(LocalDateTime start, LocalDateTime end);
    // SELECT u FROM User u WHERE u.createdDate BETWEEN ?1 AND ?2
    
    // Работа со строками
    List<User> findByUsernameLike(String pattern);
    // SELECT u FROM User u WHERE u.username LIKE ?1
    
    List<User> findByUsernameStartingWith(String prefix);
    // SELECT u FROM User u WHERE u.username LIKE ?1% 
    
    List<User> findByUsernameContaining(String substring);
    // SELECT u FROM User u WHERE u.username LIKE %?1%
    
    // Проверка на null
    List<User> findByEmailIsNotNull();
    // SELECT u FROM User u WHERE u.email IS NOT NULL
    
    // Использование In
    List<User> findByUsernameIn(Collection<String> usernames);
    // SELECT u FROM User u WHERE u.username IN ?1
    
    // Boolean поля
    List<User> findByActiveTrue();
    // SELECT u FROM User u WHERE u.active = TRUE
    
    // Сортировка
    List<User> findByAgeOrderByUsernameAsc(int age);
    // SELECT u FROM User u WHERE u.age = ?1 ORDER BY u.username ASC
    
    // Ограничение результатов
    User findFirstByOrderByCreatedDateDesc();
    List<User> findTop10ByOrderByAgeDesc();
    
    // Подсчёт
    long countByActive(boolean active);
    // SELECT COUNT(u) FROM User u WHERE u.active = ?1
    
    // Проверка существования
    boolean existsByEmail(String email);
    // SELECT CASE WHEN COUNT(u) > 0 THEN TRUE ELSE FALSE END FROM User u WHERE u.email = ?1
    
    // Удаление
    long deleteByActive(boolean active);
    // DELETE FROM User u WHERE u.active = ?1
    
    // Distinct
    List<User> findDistinctByUsername(String username);
    
    // Игнорирование регистра
    List<User> findByUsernameIgnoreCase(String username);
    // SELECT u FROM User u WHERE UPPER(u.username) = UPPER(?1)
}
```

### Вложенные свойства

Spring Data поддерживает навигацию по связанным сущностям через `_` или camelCase:

```java
@Entity
public class User {
    @OneToOne
    private Address address;
}

@Entity
public class Address {
    private String city;
}

public interface UserRepository extends JpaRepository<User, Long> {
    // Оба варианта эквивалентны
    List<User> findByAddressCity(String city);
    List<User> findByAddress_City(String city);
    // SELECT u FROM User u WHERE u.address.city = ?1
}
```

**Важно:** Для разрешения неоднозначности используйте `_`. Например, если есть поля `addressCity` и вложенное `address.city`, Spring не сможет определить, к какому обращаться без подчёркивания.

## Кастомные запросы

Когда генерация по имени метода недостаточна, Spring Data предоставляет несколько способов определения пользовательских запросов.

### @Query — JPQL и native SQL

Аннотация `@Query` позволяет явно указать JPQL или SQL запрос:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // JPQL запрос
    @Query("SELECT u FROM User u WHERE u.email = ?1")
    User findByEmailAddress(String email);
    
    // Именованные параметры
    @Query("SELECT u FROM User u WHERE u.username = :username AND u.active = :active")
    List<User> findActiveUsers(@Param("username") String username, 
                               @Param("active") boolean active);
    
    // Native SQL запрос
    @Query(value = "SELECT * FROM users u WHERE u.email = ?1", nativeQuery = true)
    User findByEmailNative(String email);
    
    // Запрос с сортировкой
    @Query("SELECT u FROM User u WHERE u.age > :age")
    List<User> findUsersByAge(@Param("age") int age, Sort sort);
    
    // Запрос с пагинацией
    @Query("SELECT u FROM User u WHERE u.active = true")
    Page<User> findActiveUsers(Pageable pageable);
    
    // Подсчёт
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdDate > :date")
    long countRecentUsers(@Param("date") LocalDateTime date);
    
    // Join запросы
    @Query("SELECT u FROM User u JOIN FETCH u.orders WHERE u.id = :id")
    User findUserWithOrders(@Param("id") Long id);
}
```

### @Modifying — UPDATE и DELETE запросы

Для запросов, модифицирующих данные, используйте `@Modifying`:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    @Modifying
    @Query("UPDATE User u SET u.active = false WHERE u.lastLoginDate < :date")
    int deactivateInactiveUsers(@Param("date") LocalDateTime date);
    
    @Modifying
    @Query("DELETE FROM User u WHERE u.active = false")
    void deleteInactiveUsers();
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.email = :email WHERE u.id = :id")
    void updateEmail(@Param("id") Long id, @Param("email") String email);
}
```

**Важно:** 
- Методы с `@Modifying` должны возвращать `void`, `int` (количество изменённых строк) или `boolean`
- Требуется `@Transactional` на уровне сервиса или метода репозитория
- После модификации данных может потребоваться очистка контекста: `@Modifying(clearAutomatically = true)`

### @NamedQuery и @NamedNativeQuery

Named queries определяются в Entity-классах и используются в репозиториях:

```java
@Entity
@NamedQuery(
    name = "User.findByActiveStatus",
    query = "SELECT u FROM User u WHERE u.active = :active"
)
@NamedNativeQuery(
    name = "User.findByEmailNative",
    query = "SELECT * FROM users WHERE email = :email",
    resultClass = User.class
)
public class User {
    // ...
}

public interface UserRepository extends JpaRepository<User, Long> {
    // Spring Data автоматически находит named query по имени
    List<User> findByActiveStatus(@Param("active") boolean active);
    
    User findByEmailNative(@Param("email") String email);
}
```

Соглашение об именовании: `<ИмяСущности>.<имяМетода>`.

### QueryDSL интеграция

QueryDSL позволяет строить типобезопасные запросы программно:

```java
// Добавьте зависимость и плагин для генерации Q-классов
// build.gradle:
// implementation 'com.querydsl:querydsl-jpa'
// annotationProcessor 'com.querydsl:querydsl-apt:5.0.0:jpa'

public interface UserRepository extends JpaRepository<User, Long>, 
                                         QuerydslPredicateExecutor<User> {
}

// Использование
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findUsers(String username, Integer minAge) {
        QUser user = QUser.user;
        BooleanBuilder predicate = new BooleanBuilder();
        
        if (username != null) {
            predicate.and(user.username.containsIgnoreCase(username));
        }
        if (minAge != null) {
            predicate.and(user.age.goe(minAge));
        }
        
        return (List<User>) userRepository.findAll(predicate);
    }
}
```

**Преимущества QueryDSL:**
- Type-safety — ошибки на этапе компиляции
- Динамическое построение запросов
- Поддержка автодополнения в IDE
- Единообразный API для разных источников данных

## Specifications — гибкое построение запросов

JPA Criteria API предоставляет программный способ построения запросов. Spring Data JPA оборачивает его в интерфейс `Specification` для удобства.

### Использование Specification

```java
public interface UserRepository extends JpaRepository<User, Long>, 
                                         JpaSpecificationExecutor<User> {
}

// Создание спецификаций
public class UserSpecifications {
    
    public static Specification<User> hasUsername(String username) {
        return (root, query, criteriaBuilder) -> 
            username == null ? null : criteriaBuilder.equal(root.get("username"), username);
    }
    
    public static Specification<User> isActive() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.isTrue(root.get("active"));
    }
    
    public static Specification<User> ageGreaterThan(int age) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.greaterThan(root.get("age"), age);
    }
    
    public static Specification<User> hasEmailDomain(String domain) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.like(root.get("email"), "%" + domain);
    }
}

// Использование в сервисе
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> searchUsers(String username, Integer minAge, boolean activeOnly) {
        Specification<User> spec = Specification.where(null);
        
        if (username != null) {
            spec = spec.and(UserSpecifications.hasUsername(username));
        }
        if (minAge != null) {
            spec = spec.and(UserSpecifications.ageGreaterThan(minAge));
        }
        if (activeOnly) {
            spec = spec.and(UserSpecifications.isActive());
        }
        
        return userRepository.findAll(spec);
    }
    
    // С сортировкой и пагинацией
    public Page<User> searchUsers(Specification<User> spec, Pageable pageable) {
        return userRepository.findAll(spec, pageable);
    }
}
```

### Сложные спецификации с Join

```java
public class UserSpecifications {
    
    // Join с связанной сущностью
    public static Specification<User> hasOrderWithStatus(OrderStatus status) {
        return (root, query, criteriaBuilder) -> {
            Join<User, Order> orders = root.join("orders");
            return criteriaBuilder.equal(orders.get("status"), status);
        };
    }
    
    // Fetch join для предотвращения N+1 проблемы
    public static Specification<User> fetchOrders() {
        return (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("orders", JoinType.LEFT);
            }
            return null;
        };
    }
}
```

**Важно:** При использовании fetch join и count запросов (для пагинации) Spring Data автоматически выполняет два запроса: один для подсчёта, другой для получения данных. В count запросе fetch join игнорируется.

## Проекции

Проекции позволяют получать из БД только необходимые поля вместо полных сущностей, что улучшает производительность.

### Interface-based проекции

Определите интерфейс с getter-методами для требуемых полей:

```java
public interface UserSummary {
    String getUsername();
    String getEmail();
    
    // Вычисляемые свойства с @Value и SpEL
    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();
}

public interface UserRepository extends JpaRepository<User, Long> {
    List<UserSummary> findByActive(boolean active);
    
    // С пагинацией
    Page<UserSummary> findByAgeGreaterThan(int age, Pageable pageable);
}
```

Spring Data создаёт прокси-объект, делегирующий вызовы getter'ов к сущности.

### Закрытые проекции (Closed Projections)

Закрытые проекции извлекают только указанные поля:

```java
public interface UserBasicInfo {
    Long getId();
    String getUsername();
}

// Hibernate выполнит: SELECT u.id, u.username FROM User u WHERE ...
```

### Открытые проекции (Open Projections)

Открытые проекции используют SpEL и загружают всю сущность:

```java
public interface UserFullName {
    @Value("#{target.firstName + ' ' + target.lastName}")
    String getFullName();
}

// Hibernate загрузит всю сущность User
```

### Class-based проекции (DTO)

Используйте обычные классы для проекций:

```java
public class UserDto {
    private final String username;
    private final String email;
    
    public UserDto(String username, String email) {
        this.username = username;
        this.email = email;
    }
    
    // getters
}

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT new com.example.dto.UserDto(u.username, u.email) FROM User u WHERE u.active = true")
    List<UserDto> findActiveUserDtos();
}
```

**Важно:** Конструктор DTO должен принимать параметры в том же порядке, что и в JPQL запросе.

### Динамические проекции

Определите тип проекции как параметр метода:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    <T> List<T> findByUsername(String username, Class<T> type);
}

// Использование
List<UserSummary> summaries = userRepository.findByUsername("john", UserSummary.class);
List<UserDto> dtos = userRepository.findByUsername("john", UserDto.class);
List<User> entities = userRepository.findByUsername("john", User.class);
```

### Вложенные проекции

Проекции поддерживают навигацию по связям:

```java
public interface UserWithAddress {
    String getUsername();
    AddressProjection getAddress();
    
    interface AddressProjection {
        String getCity();
        String getStreet();
    }
}
```

## Аудит сущностей

Spring Data JPA предоставляет встроенную поддержку аудита для автоматического заполнения метаданных о создании и изменении сущностей.

### Включение аудита

```java
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        // Возвращает текущего пользователя из SecurityContext или другого источника
        return () -> Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(Authentication::getName);
    }
}
```

### Аудируемые сущности

```java
@Entity
@EntityListeners(AuditingEntityListener.class)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    
    @CreatedDate
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    private LocalDateTime lastModifiedDate;
    
    @CreatedBy
    private String createdBy;
    
    @LastModifiedBy
    private String lastModifiedBy;
    
    // getters and setters
}
```

**Аннотации аудита:**
- `@CreatedDate` — дата создания (устанавливается один раз при persist)
- `@LastModifiedDate` — дата последнего изменения (обновляется при каждом merge)
- `@CreatedBy` — пользователь, создавший сущность
- `@LastModifiedBy` — пользователь, последний изменивший сущность

### Базовый класс для аудита

Избегайте дублирования, создав абстрактный базовый класс:

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable<U> {
    
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;
    
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private U createdBy;
    
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private U lastModifiedBy;
    
    // getters
}

@Entity
public class User extends Auditable<String> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    // ...
}
```

## Транзакции в Spring Data

Spring Data JPA тесно интегрирован с механизмом транзакций Spring.

### Транзакции на уровне репозитория

По умолчанию методы репозиториев выполняются в транзакции только для чтения:

```java
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Для модифицирующих операций readOnly переопределяется
    @Transactional
    @Override
    <S extends User> S save(S entity);
    
    // Кастомный метод модификации
    @Transactional
    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id = :id")
    void updateActive(@Param("id") Long id, @Param("active") boolean active);
}
```

### Транзакции на уровне сервиса

Рекомендуется управлять транзакциями на уровне сервисного слоя:

```java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    // Метод выполняется в транзакции
    public User createUserWithOrder(User user, Order order) {
        User savedUser = userRepository.save(user);
        order.setUser(savedUser);
        orderRepository.save(order);
        return savedUser;
    }
    
    // Только для чтения
    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    // Разные уровни изоляции
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void criticalOperation() {
        // ...
    }
    
    // Откат только для определённых исключений
    @Transactional(rollbackFor = BusinessException.class, 
                   noRollbackFor = ValidationException.class)
    public void processWithCustomRollback() {
        // ...
    }
}
```

### Propagation — управление границами транзакций

```java
@Service
public class OrderService {
    
    @Autowired
    private AuditService auditService;
    
    @Transactional
    public void createOrder(Order order) {
        // Основная транзакция
        orderRepository.save(order);
        
        // Аудит выполняется в отдельной транзакции
        auditService.logOrderCreation(order);
        
        // Если произойдёт исключение здесь, аудит всё равно сохранится
    }
}

@Service
public class AuditService {
    
    // REQUIRES_NEW — всегда создаёт новую транзакцию
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logOrderCreation(Order order) {
        AuditLog log = new AuditLog("Order created: " + order.getId());
        auditRepository.save(log);
    }
}
```

**Уровни распространения (Propagation):**
- `REQUIRED` (по умолчанию) — использует существующую транзакцию или создаёт новую
- `REQUIRES_NEW` — всегда создаёт новую транзакцию, приостанавливая текущую
- `NESTED` — создаёт вложенную транзакцию (savepoint)
- `SUPPORTS` — выполняется в транзакции, если она есть
- `NOT_SUPPORTED` — выполняется без транзакции
- `MANDATORY` — требует существующую транзакцию, иначе исключение
- `NEVER` — не должно быть транзакции, иначе исключение

## Поддержка NoSQL

Spring Data предоставляет единообразный API для работы с различными NoSQL системами.

### Spring Data MongoDB

```java
@Document(collection = "users")
public class User {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    private String email;
    private List<String> roles;
    private Address address;
    
    // getters and setters
}

public interface UserRepository extends MongoRepository<User, String> {
    List<User> findByUsername(String username);
    List<User> findByRolesContaining(String role);
    
    @Query("{ 'email': { $regex: ?0 } }")
    List<User> findByEmailPattern(String pattern);
}

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public List<User> findAdmins() {
        return userRepository.findByRolesContaining("ADMIN");
    }
}
```

### Spring Data Redis

```java
@RedisHash("users")
public class User {
    @Id
    private String id;
    
    private String username;
    
    @TimeToLive
    private Long ttl; // время жизни в секундах
    
    // getters and setters
}

public interface UserRepository extends CrudRepository<User, String> {
    List<User> findByUsername(String username);
}

// Использование RedisTemplate для более гибкой работы
@Service
public class CacheService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public void cacheUser(User user) {
        redisTemplate.opsForValue().set("user:" + user.getId(), user, 1, TimeUnit.HOURS);
    }
    
    public User getUser(String userId) {
        return (User) redisTemplate.opsForValue().get("user:" + userId);
    }
}
```

### Spring Data Cassandra

```java
@Table("users")
public class User {
    @PrimaryKey
    private UUID id;
    
    @Column("user_name")
    private String username;
    
    private String email;
    
    @Column("created_at")
    private LocalDateTime createdAt;
    
    // getters and setters
}

public interface UserRepository extends CassandraRepository<User, UUID> {
    @Query("SELECT * FROM users WHERE user_name = ?0")
    List<User> findByUsername(String username);
}
```

## Spring Data REST

Spring Data REST автоматически создаёт RESTful API для репозиториев.

### Базовая конфигурация

```java
@Configuration
@EnableJpaRepositories
@Import(RepositoryRestMvcConfiguration.class)
public class RestConfig implements RepositoryRestConfigurer {
    
    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.setBasePath("/api");
        config.exposeIdsFor(User.class);
    }
}

@RepositoryRestResource(collectionResourceRel = "users", path = "users")
public interface UserRepository extends JpaRepository<User, Long> {
    
    List<User> findByUsername(@Param("username") String username);
    
    @RestResource(exported = false) // Скрыть от REST API
    void deleteById(Long id);
}
```

### Автоматически генерируемые эндпоинты

После настройки Spring Data REST доступны следующие операции:

```
GET    /api/users              - список пользователей (с пагинацией)
GET    /api/users/{id}         - конкретный пользователь
POST   /api/users              - создание пользователя
PUT    /api/users/{id}         - полное обновление
PATCH  /api/users/{id}         - частичное обновление
DELETE /api/users/{id}         - удаление
GET    /api/users/search       - список доступных query методов
GET    /api/users/search/findByUsername?username=john - вызов кастомного метода
```

### Кастомизация REST ресурсов

```java
@RepositoryRestResource
public interface UserRepository extends JpaRepository<User, Long> {
    
    @RestResource(path = "by-username", rel = "by-username")
    List<User> findByUsername(@Param("username") String username);
}

// Проекции для REST
@Projection(name = "summary", types = User.class)
public interface UserSummary {
    String getUsername();
    String getEmail();
}

// Доступ: GET /api/users?projection=summary
```

### Обработка событий REST

```java
@RepositoryEventHandler(User.class)
public class UserEventHandler {
    
    @HandleBeforeCreate
    public void handleBeforeCreate(User user) {
        // Валидация перед созданием
        if (user.getUsername() == null) {
            throw new IllegalArgumentException("Username is required");
        }
    }
    
    @HandleAfterCreate
    public void handleAfterCreate(User user) {
        // Логирование или отправка событий
        System.out.println("User created: " + user.getId());
    }
    
    @HandleBeforeDelete
    public void handleBeforeDelete(User user) {
        // Проверка разрешений
    }
}
```

## Практические заметки

### Выбор правильного интерфейса репозитория

- **Repository** — минимальный набор методов, когда нужен полный контроль
- **CrudRepository** — базовые CRUD операции для большинства случаев
- **PagingAndSortingRepository** — когда требуется пагинация
- **JpaRepository** — для JPA с batch операциями и flush
- **Custom Repository** — для специфичной логики, не покрываемой стандартными интерфейсами

### Оптимизация производительности

**N+1 проблема** — частая проблема при работе с ленивыми связями:

```java
// Плохо — вызывает N+1 запросов
List<User> users = userRepository.findAll();
for (User user : users) {
    System.out.println(user.getOrders().size()); // Ленивая загрузка каждый раз
}

// Хорошо — один запрос с JOIN FETCH
@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")
List<User> findAllWithOrders();

// Или используйте @EntityGraph
@EntityGraph(attributePaths = {"orders"})
List<User> findAll();
```

**Batch fetching** для оптимизации загрузки коллекций:

```java
@Entity
public class User {
    @OneToMany(mappedBy = "user")
    @BatchSize(size = 10) // Загружает до 10 коллекций за раз
    private List<Order> orders;
}
```

**Используйте проекции** для выборки только необходимых данных:

```java
// Вместо загрузки всей сущности
List<User> users = userRepository.findAll();

// Загрузите только нужные поля
List<UserSummary> summaries = userRepository.findAllProjectedBy();
```

### Обработка больших объёмов данных

**Stream API** для обработки больших результатов:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = "50"))
    Stream<User> streamAllByActiveTrue();
}

@Service
@Transactional(readOnly = true)
public class UserService {
    public void processAllActiveUsers() {
        try (Stream<User> users = userRepository.streamAllByActiveTrue()) {
            users.forEach(user -> {
                // Обработка по одному, без загрузки всех в память
                processUser(user);
            });
        }
    }
}
```

**Важно:** Stream должен использоваться внутри транзакции и закрываться (try-with-resources).

**Batch processing** для массовых операций:

```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Transactional
    public void importUsers(List<User> users) {
        int batchSize = 50;
        for (int i = 0; i < users.size(); i++) {
            userRepository.save(users.get(i));
            if (i % batchSize == 0 && i > 0) {
                // Flush и очистка контекста для предотвращения OutOfMemoryError
                userRepository.flush();
                entityManager.clear();
            }
        }
    }
}
```

### Проблемы производительности пагинации (Offset/Limit)

Классическая пагинация с использованием `OFFSET` и `LIMIT` хорошо работает для небольших наборов данных, но имеет серьёзные проблемы производительности на больших таблицах.

**Проблема деградации производительности OFFSET**

При использовании стандартной пагинации Spring Data генерирует SQL с `OFFSET`:

```java
// Страница 1: LIMIT 10 OFFSET 0
Page<User> page1 = userRepository.findAll(PageRequest.of(0, 10));

// Страница 1000: LIMIT 10 OFFSET 10000
Page<User> page1000 = userRepository.findAll(PageRequest.of(1000, 10));
```

**Почему это медленно?**

База данных должна:
1. Прочитать все строки от начала до OFFSET (например, 10 000 строк)
2. Отбросить их
3. Вернуть только следующие 10 строк

Чем больше номер страницы, тем медленнее запрос. На больших таблицах (миллионы строк) разница может быть критической:
- Страница 1: ~5 мс
- Страница 100: ~50 мс
- Страница 10 000: ~5 секунд

**Дополнительные проблемы:**

1. **COUNT запрос** — для определения общего количества страниц выполняется `SELECT COUNT(*)`, который сканирует всю таблицу
2. **Нестабильность результатов** — при добавлении/удалении данных между запросами страниц можно пропустить или дважды получить одни и те же записи
3. **Нагрузка на БД** — глубокая пагинация создаёт высокую нагрузку на индексы

**Решение 1: Keyset/Cursor Pagination (Seek Method)**

Вместо OFFSET используйте условие на последний элемент предыдущей страницы:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Первая страница
    @Query("SELECT u FROM User u ORDER BY u.id ASC")
    List<User> findFirstPage(Pageable pageable);
    
    // Следующие страницы по курсору
    @Query("SELECT u FROM User u WHERE u.id > :lastId ORDER BY u.id ASC")
    List<User> findNextPage(@Param("lastId") Long lastId, Pageable pageable);
}

@Service
public class UserService {
    
    public List<User> getFirstPage(int pageSize) {
        return userRepository.findFirstPage(PageRequest.of(0, pageSize));
    }
    
    public List<User> getNextPage(Long lastSeenId, int pageSize) {
        return userRepository.findNextPage(lastSeenId, PageRequest.of(0, pageSize));
    }
}
```

**Преимущества Keyset Pagination:**
- Постоянная производительность независимо от глубины
- Использует индекс эффективно (`WHERE id > :lastId` + `ORDER BY id`)
- Стабильные результаты при параллельных изменениях данных

**Недостатки:**
- Невозможно перейти на произвольную страницу (только вперёд/назад)
- Сложнее с составной сортировкой
- Требует уникальный упорядоченный ключ

**Решение 2: Slice вместо Page**

Если не нужно знать общее количество страниц, используйте `Slice`:

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Slice<User> findByActive(boolean active, Pageable pageable);
}

@Service
public class UserService {
    public Slice<User> getActiveUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findByActive(true, pageable);
    }
}
```

`Slice` не выполняет COUNT запрос, но сообщает, есть ли следующая страница (делает `LIMIT size + 1`).

**Решение 3: Материализованные представления для агрегатов**

Для часто запрашиваемых данных создайте материализованное представление:

```sql
CREATE MATERIALIZED VIEW user_summary AS
SELECT id, username, email, created_date
FROM users
WHERE active = true
ORDER BY created_date DESC;

CREATE INDEX idx_user_summary_created ON user_summary(created_date);
```

Периодически обновляйте представление (например, по расписанию).

**Решение 4: Комбинированный подход с кешированием**

Кешируйте COUNT запрос и первые N страниц:

```java
@Service
public class UserService {
    
    @Cacheable(value = "userCount", unless = "#result == null")
    public long getUserCount() {
        return userRepository.count();
    }
    
    @Cacheable(value = "userPages", key = "#page", unless = "#page > 10")
    public Page<User> getUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size));
    }
}
```

**Решение 5: Ограничение глубины пагинации**

В production системах часто ограничивают максимальную глубину:

```java
@Service
public class UserService {
    private static final int MAX_PAGE = 100;
    private static final int MAX_SIZE = 100;
    
    public Page<User> getUsers(int page, int size) {
        if (page > MAX_PAGE) {
            throw new IllegalArgumentException("Page number exceeds maximum allowed: " + MAX_PAGE);
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        return userRepository.findAll(PageRequest.of(page, size));
    }
}
```

**Рекомендации по выбору подхода:**

| Сценарий | Рекомендация |
|----------|--------------|
| Небольшие таблицы (<10K записей) | Стандартная Page пагинация |
| Infinite scroll в UI | Slice или Keyset pagination |
| API с большими данными | Keyset pagination |
| Поиск с фильтрами | Ограниченная Page + кеширование COUNT |
| Отчёты и экспорт | Stream API без пагинации |
| Публичные API | Cursor-based pagination (RFC 8288) |

**Пример реализации Cursor Pagination для REST API:**

```java
public class CursorPage<T> {
    private List<T> content;
    private String nextCursor;
    private String prevCursor;
    private boolean hasNext;
    private boolean hasPrevious;
    
    // constructors, getters
}

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @GetMapping
    public CursorPage<User> getUsers(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        
        List<User> users;
        if (cursor == null) {
            users = userRepository.findFirstPage(PageRequest.of(0, size + 1));
        } else {
            Long lastId = decodeCursor(cursor);
            users = userRepository.findNextPage(lastId, PageRequest.of(0, size + 1));
        }
        
        boolean hasNext = users.size() > size;
        if (hasNext) {
            users = users.subList(0, size);
        }
        
        String nextCursor = hasNext ? encodeCursor(users.get(size - 1).getId()) : null;
        
        return new CursorPage<>(users, nextCursor, null, hasNext, false);
    }
    
    private String encodeCursor(Long id) {
        return Base64.getEncoder().encodeToString(id.toString().getBytes());
    }
    
    private Long decodeCursor(String cursor) {
        return Long.parseLong(new String(Base64.getDecoder().decode(cursor)));
    }
}
```

**База данных специфичные оптимизации:**

**PostgreSQL:**
```sql
-- Использование индексов для быстрой пагинации
CREATE INDEX idx_users_id ON users(id);

-- Для сложной сортировки
CREATE INDEX idx_users_created_id ON users(created_date DESC, id DESC);
```

**MySQL/MariaDB:**
```sql
-- Избегайте глубокого OFFSET через подзапрос
SELECT u.* FROM users u
INNER JOIN (
    SELECT id FROM users ORDER BY id LIMIT 10 OFFSET 10000
) AS subq ON u.id = subq.id;
```

**SQL Server:**
```sql
-- Используйте OFFSET FETCH для эффективной пагинации
SELECT * FROM Users
ORDER BY Id
OFFSET 10000 ROWS
FETCH NEXT 10 ROWS ONLY;
```

> **Важно:** Всегда тестируйте производительность пагинации на продакшн-подобных объёмах данных. То, что работает на тестовых 1000 записей, может сломаться на миллионах.

### Интеграция с аудитом и безопасностью

Используйте `@PreAuthorize` для контроля доступа на уровне методов:

```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    @PreAuthorize("hasRole('ADMIN')")
    @Override
    void deleteById(Long id);
    
    @PostAuthorize("returnObject.username == authentication.name or hasRole('ADMIN')")
    Optional<User> findById(Long id);
}
```

### Кеширование запросов

Используйте второй уровень кеша Hibernate или Spring Cache:

```java
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class User {
    // ...
}

@Service
public class UserService {
    @Cacheable(value = "users", key = "#id")
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new UserNotFoundException(id));
    }
    
    @CacheEvict(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
```

### Тестирование репозиториев

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserRepositoryTest {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Test
    public void testFindByUsername() {
        // Подготовка данных
        User user = new User("john", "john@example.com");
        entityManager.persist(user);
        entityManager.flush();
        
        // Выполнение
        List<User> found = userRepository.findByUsername("john");
        
        // Проверка
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEmail()).isEqualTo("john@example.com");
    }
    
    @Test
    public void testCustomQuery() {
        User user1 = new User("alice", "alice@example.com");
        User user2 = new User("bob", "bob@example.com");
        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();
        
        List<User> users = userRepository.findActiveUsers(true);
        
        assertThat(users).hasSize(2);
    }
}
```

## Практические упражнения

1. **Создайте репозиторий для сущности Product** с методами поиска по категории, ценовому диапазону и наличию на складе. Используйте генерацию запросов по имени методов.

2. **Реализуйте фильтрацию товаров через Specifications** с возможностью динамического построения запросов по множеству критериев (название, категория, цена, производитель).

3. **Настройте аудит для сущностей Order и OrderItem** с автоматическим заполнением дат создания/изменения и пользователя.

4. **Создайте проекции** для отображения краткой (id, name, price) и полной информации о товаре (включая описание, изображения, отзывы).

5. **Оптимизируйте запрос загрузки заказов с позициями** для устранения N+1 проблемы. Сравните производительность с JPQL JOIN FETCH и @EntityGraph.

6. **Реализуйте пагинацию и сортировку** для списка пользователей с возможностью сортировки по множеству полей.

7. **Напишите интеграционный тест** для репозитория с использованием @DataJpaTest и in-memory H2, проверяющий корректность кастомных запросов.

8. **Создайте кастомную реализацию метода репозитория** для выполнения сложного запроса с использованием Criteria API.

## Вопросы на собеседовании

1. **Как работает генерация запросов по имени метода репозитория?**
   
   *Ответ:* Spring Data анализирует имя метода и разбивает его на ключевые слова (find, By, And, Or, OrderBy и т.д.). На основе этих ключевых слов генерируется JPQL или SQL запрос. Например, `findByUsernameAndEmail` преобразуется в `SELECT u FROM User u WHERE u.username = ?1 AND u.email = ?2`. Механизм поддерживает вложенные свойства, операторы сравнения, сортировку, ограничение результатов.

2. **В чём преимущества Spring Data JPA по сравнению с чистым JPA?**
   
   *Ответ:* Spring Data JPA устраняет boilerplate-код: не нужно писать реализации репозиториев для типовых операций. Предоставляет автоматическую генерацию запросов по имени методов, встроенную пагинацию, проекции, аудит, интеграцию с Spring транзакциями. Упрощает тестирование через @DataJpaTest. При этом не ограничивает возможности JPA — можно использовать Criteria API, JPQL, native SQL.

3. **Как реализовать собственный бэкэнд для Spring Data?**
   
   *Ответ:* Для создания собственного Spring Data модуля нужно: 1) определить marker interface, расширяющий `Repository`, 2) создать `RepositoryFactoryBean`, который создаёт экземпляры репозиториев, 3) реализовать `RepositoryFactory`, генерирующую прокси для интерфейсов репозиториев, 4) реализовать `RepositoryQuery` для выполнения запросов, 5) зарегистрировать фабрику через `@Enable*Repositories`. Spring Data предоставляет базовые классы для упрощения этого процесса.

4. **Что такое проекции и когда их использовать?**
   
   *Ответ:* Проекции позволяют получать из БД только необходимые поля вместо полных сущностей. Типы: interface-based (Spring создаёт прокси), class-based (DTO с конструктором), динамические (тип определяется в runtime). Используйте для оптимизации — уменьшения объёма передаваемых данных, особенно для списков и отчётов. Закрытые проекции (только геттеры) эффективнее открытых (SpEL в @Value).

5. **Как избежать N+1 проблемы в Spring Data JPA?**
   
   *Ответ:* Способы решения: 1) JOIN FETCH в JPQL (`@Query("SELECT u FROM User u LEFT JOIN FETCH u.orders")`), 2) @EntityGraph для декларативной загрузки связей, 3) @BatchSize для загрузки коллекций батчами, 4) изменение стратегии загрузки на EAGER (не рекомендуется для коллекций), 5) использование проекций, загружающих только нужные данные. Предпочтительны JOIN FETCH и @EntityGraph.

6. **В чём разница между @Query с JPQL и native SQL?**
   
   *Ответ:* JPQL работает с Entity-классами (независимость от БД, возможность использовать кеш), native SQL — с таблицами и колонками БД (специфичные функции СУБД, сложные запросы). JPQL преобразуется Hibernate в SQL с учётом диалекта БД. Native SQL быстрее для сложных запросов с множественными join и подзапросами, но привязывает к конкретной БД. Используйте JPQL когда возможно, native SQL — для специфичных оптимизаций.

7. **Как работает Specification API?**
   
   *Ответ:* Specification — функциональный интерфейс, обёртка над JPA Criteria API. Метод `toPredicate()` возвращает условие запроса. Specifications можно комбинировать через `and()`, `or()`, `not()`. Репозиторий должен расширять `JpaSpecificationExecutor`. Преимущество — динамическое построение запросов, type-safety, переиспользование предикатов. Используется для сложных фильтров с множеством опциональных условий.

8. **Какие уровни кеширования поддерживает Hibernate в Spring Data JPA?**
   
   *Ответ:* Первый уровень (Session/EntityManager) — всегда включён, кеширует объекты в рамках транзакции. Второй уровень (SessionFactory) — опциональный, кеширует данные между транзакциями, требует настройки (EhCache, Infinispan). Query cache — кеширует результаты запросов. Для использования второго уровня нужно настроить провайдера, пометить Entity аннотацией `@Cacheable`, настроить стратегию (READ_ONLY, READ_WRITE, TRANSACTIONAL).

9. **Что такое @Modifying и когда её использовать?**
   
   *Ответ:* `@Modifying` используется с `@Query` для UPDATE/DELETE запросов, изменяющих данные. Без неё Spring Data выполнит SELECT вместо модификации. Аннотация сигнализирует, что запрос модифицирует состояние БД. Требуется `@Transactional`. Параметр `clearAutomatically = true` очищает контекст персистентности после выполнения, предотвращая рассинхронизацию кеша. Возвращаемый тип: void, int (кол-во изменённых строк), boolean.

10. **Как настроить и использовать аудит в Spring Data JPA?**
    
    *Ответ:* Шаги: 1) включить `@EnableJpaAuditing` в конфигурации, 2) создать `AuditorAware<T>` bean для определения текущего пользователя, 3) добавить `@EntityListeners(AuditingEntityListener.class)` к Entity, 4) пометить поля аннотациями `@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`. Аудит автоматически заполняет метаданные при persist и merge. Удобно вынести в `@MappedSuperclass` для переиспользования.

11. **Как работает пагинация в Spring Data?**
    
    *Ответ:* Интерфейс `Pageable` определяет номер страницы, размер и сортировку. `PageRequest.of(page, size, sort)` создаёт объект Pageable. Методы репозитория принимают Pageable и возвращают `Page<T>` (с метаданными: total elements, total pages) или `Slice<T>` (знает только о наличии следующей страницы, эффективнее для больших таблиц). Spring Data автоматически добавляет LIMIT/OFFSET в запрос. Page выполняет дополнительный COUNT запрос для определения общего количества.

12. **В чём разница между save() и saveAndFlush()?**
    
    *Ответ:* `save()` помещает сущность в контекст персистентности, но не гарантирует немедленную синхронизацию с БД — изменения отправятся при flush (обычно в конце транзакции). `saveAndFlush()` сохраняет и немедленно вызывает flush, синхронизируя с БД. Используйте saveAndFlush когда нужен сгенерированный БД ID/version сразу после сохранения или для отправки изменений до окончания транзакции (например, перед native SQL запросом).

## Дополнительные материалы

- [Spring Data JPA Documentation](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring Data Common Documentation](https://docs.spring.io/spring-data/commons/docs/current/reference/html/)
- [Query Methods Reference](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods)
- Книга "Spring Data" by Mark Pollack, Oliver Gierke — подробное руководство по всему стеку Spring Data
- [Baeldung Spring Data JPA](https://www.baeldung.com/spring-data-jpa-tutorial) — практические примеры и рецепты
- [Vlad Mihalcea's Blog](https://vladmihalcea.com/) — углублённые статьи по Hibernate и JPA производительности
