# Потоковое локальное состояние и неизменяемость

### ThreadLocal: состояние, привязанное к потоку

`ThreadLocal` позволяет каждому потоку иметь свою независимую копию переменной:

**Базовое использование:**

```java
public class ThreadLocalExample {
    // Каждый поток получит свой SimpleDateFormat
    private static final ThreadLocal<SimpleDateFormat> dateFormat = 
        ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd"));
    
    public String formatDate(Date date) {
        // Безопасно в многопоточном окружении
        return dateFormat.get().format(date);
    }
    
    // Очистка после использования
    public void cleanup() {
        dateFormat.remove(); // Важно для пулов потоков!
    }
}
```

**Практический пример: контекст запроса в веб-приложении:**

```java
public class RequestContext {
    private static final ThreadLocal<UserContext> userContext = new ThreadLocal<>();
    
    public static class UserContext {
        private final String userId;
        private final String requestId;
        private final long startTime;
        
        public UserContext(String userId, String requestId) {
            this.userId = userId;
            this.requestId = requestId;
            this.startTime = System.currentTimeMillis();
        }
        
        public String getUserId() { return userId; }
        public String getRequestId() { return requestId; }
        public long getElapsedTime() { 
            return System.currentTimeMillis() - startTime; 
        }
    }
    
    // Servlet filter устанавливает контекст
    public static void setContext(String userId, String requestId) {
        userContext.set(new UserContext(userId, requestId));
    }
    
    public static UserContext getContext() {
        return userContext.get();
    }
    
    // ОБЯЗАТЕЛЬНО очищать в finally блоке
    public static void clearContext() {
        userContext.remove();
    }
    
    // Пример использования в filter
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
            throws IOException, ServletException {
        try {
            String userId = extractUserId(req);
            String requestId = UUID.randomUUID().toString();
            setContext(userId, requestId);
            
            chain.doFilter(req, res);
        } finally {
            // Критически важно для пулов потоков!
            clearContext();
        }
    }
}
```

**InheritableThreadLocal для дочерних потоков:**

```java
public class InheritableContext {
    // Дочерние потоки наследуют значение от родителя
    private static final InheritableThreadLocal<String> context = 
        new InheritableThreadLocal<>();
    
    public static void demo() {
        context.set("Parent context");
        
        new Thread(() -> {
            // Автоматически получает "Parent context"
            System.out.println("Child sees: " + context.get());
            
            // Может изменить свою копию
            context.set("Child context");
            System.out.println("Child changed to: " + context.get());
        }).start();
        
        // Родитель не видит изменения дочернего потока
        System.out.println("Parent still has: " + context.get());
    }
}
```

**Проблемы ThreadLocal:**

1. **Утечки памяти в пулах потоков**: Потоки переиспользуются, ThreadLocal остаётся
2. **Неявные зависимости**: Код зависит от неявного состояния
3. **Сложность тестирования**: Тесты должны очищать ThreadLocal

**Best practices:**

```java
public class ThreadLocalBestPractices {
    // 1. Используйте static final для ThreadLocal переменных
    private static final ThreadLocal<Context> context = new ThreadLocal<>();
    
    // 2. Всегда очищайте в finally
    public void processRequest() {
        try {
            context.set(new Context());
            // Обработка
        } finally {
            context.remove(); // Предотвращает утечки!
        }
    }
    
    // 3. Рассмотрите альтернативы ThreadLocal
    // - Передача параметров явно
    // - Неизменяемые объекты
    // - Context propagation libraries (например, в Project Reactor)
}
```

### Неизменяемость как альтернатива синхронизации

Неизменяемые (immutable) объекты автоматически потокобезопасны:

**Правильный неизменяемый класс:**

```java
public final class ImmutableUser {
    private final String id;
    private final String name;
    private final List<String> roles;
    
    public ImmutableUser(String id, String name, List<String> roles) {
        this.id = id;
        this.name = name;
        // Защищаемся от изменения через переданную коллекцию
        this.roles = List.copyOf(roles); // Java 10+
        // или Collections.unmodifiableList(new ArrayList<>(roles))
    }
    
    // Только getters, нет setters
    public String getId() { return id; }
    public String getName() { return name; }
    public List<String> getRoles() { return roles; } // Возвращает неизменяемую копию
    
    // "Модификация" через создание нового объекта
    public ImmutableUser withName(String newName) {
        return new ImmutableUser(this.id, newName, this.roles);
    }
}
```

**Использование Records (Java 14+):**

```java
// Record автоматически неизменяем
public record User(String id, String name, List<String> roles) {
    // Каноничный конструктор для валидации
    public User {
        if (id == null || name == null) {
            throw new IllegalArgumentException("ID and name are required");
        }
        // Защищаем от изменений
        roles = List.copyOf(roles);
    }
    
    // Можно добавлять методы
    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
```

**Преимущества неизменяемости:**
- Автоматическая потокобезопасность
- Отсутствие синхронизации
- Простота рассуждения о коде
- Можно безопасно использовать как ключи Map
- Можно безопасно кэшировать

