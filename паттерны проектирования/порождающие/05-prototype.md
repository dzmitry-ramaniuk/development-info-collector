# Prototype (Прототип)

Prototype — порождающий паттерн проектирования, который позволяет копировать объекты, не вдаваясь в подробности их реализации.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Глубокое и поверхностное копирование](#глубокое-и-поверхностное-копирование)
5. [Реализация](#реализация)
6. [Примеры использования](#примеры-использования)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Prototype используется когда:
- Создание объекта требует больших ресурсов или сложных операций
- Нужно избежать построения иерархии фабрик, параллельной иерархии продуктов
- Экземпляры класса могут находиться в одном из небольшого числа различных состояний
- Требуется создать много объектов, отличающихся незначительно

**Типичные примеры использования:**
- Клонирование сложных объектов
- Создание прототипов для тестирования
- Копирование конфигураций
- Реализация функции отмены (undo)

## Проблема, которую решает

### Проблема: Сложное создание объекта

```java
// Сложный объект с множеством зависимостей
public class GameCharacter {
    private String name;
    private int level;
    private List<Item> inventory;
    private Equipment equipment;
    private Skills skills;
    private Position position;
    
    public GameCharacter(String name) {
        this.name = name;
        this.level = 1;
        this.inventory = new ArrayList<>();
        this.equipment = new Equipment();
        this.skills = new Skills();
        // ... много инициализации
        
        // Загрузка базовых предметов
        inventory.add(new Item("Меч", 10));
        inventory.add(new Item("Зелье", 5));
        
        // Настройка начальных навыков
        skills.add(new Skill("Удар", 1));
        // ... еще много кода
    }
}

// Проблема: каждый раз создавать нового персонажа с нуля дорого
// Особенно если нужны персонажи-шаблоны (воин, маг, лучник)
```

**Проблемы:**
- Дорогостоящее создание объекта
- Дублирование кода инициализации
- Сложность создания вариаций объектов
- Невозможность создать копию без знания всех деталей

### Решение: Prototype

Создать прототип и клонировать его для получения новых объектов.

## Структура паттерна

```java
// Интерфейс прототипа
interface Prototype extends Cloneable {
    Prototype clone();
}

// Конкретный прототип
class GameCharacter implements Prototype {
    private String name;
    private int level;
    private List<Item> inventory;
    private Equipment equipment;
    
    public GameCharacter(String name, int level) {
        this.name = name;
        this.level = level;
        this.inventory = new ArrayList<>();
        this.equipment = new Equipment();
    }
    
    // Конструктор копирования
    private GameCharacter(GameCharacter source) {
        this.name = source.name;
        this.level = source.level;
        // Глубокое копирование инвентаря
        this.inventory = new ArrayList<>();
        for (Item item : source.inventory) {
            this.inventory.add(new Item(item));
        }
        // Глубокое копирование снаряжения
        this.equipment = new Equipment(source.equipment);
    }
    
    @Override
    public GameCharacter clone() {
        return new GameCharacter(this);
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void addItem(Item item) {
        inventory.add(item);
    }
}

// Использование
// Создаем прототип воина
GameCharacter warriorPrototype = new GameCharacter("Warrior Template", 1);
warriorPrototype.addItem(new Item("Меч", 10));
warriorPrototype.addItem(new Item("Щит", 5));

// Клонируем для создания новых персонажей
GameCharacter warrior1 = warriorPrototype.clone();
warrior1.setName("Воин Артур");

GameCharacter warrior2 = warriorPrototype.clone();
warrior2.setName("Воин Ланселот");
```

## Глубокое и поверхностное копирование

### Поверхностное копирование (Shallow Copy)

```java
class ShallowCopyExample implements Cloneable {
    private int value;
    private int[] array;
    
    public ShallowCopyExample(int value, int[] array) {
        this.value = value;
        this.array = array;
    }
    
    @Override
    public ShallowCopyExample clone() {
        try {
            // Стандартный clone() делает поверхностное копирование
            return (ShallowCopyExample) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

// Проблема поверхностного копирования
int[] originalArray = {1, 2, 3};
ShallowCopyExample original = new ShallowCopyExample(10, originalArray);
ShallowCopyExample copy = original.clone();

// Изменение массива в копии влияет на оригинал!
copy.array[0] = 999;
System.out.println(original.array[0]); // 999 - оба объекта ссылаются на один массив!
```

### Глубокое копирование (Deep Copy)

```java
class DeepCopyExample implements Cloneable {
    private int value;
    private int[] array;
    private List<String> list;
    
    public DeepCopyExample(int value, int[] array, List<String> list) {
        this.value = value;
        this.array = array;
        this.list = list;
    }
    
    @Override
    public DeepCopyExample clone() {
        try {
            DeepCopyExample clone = (DeepCopyExample) super.clone();
            
            // Глубокое копирование массива
            clone.array = array.clone();
            
            // Глубокое копирование списка
            clone.list = new ArrayList<>(this.list);
            
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

// Использование
int[] originalArray = {1, 2, 3};
List<String> originalList = new ArrayList<>(Arrays.asList("a", "b", "c"));

DeepCopyExample original = new DeepCopyExample(10, originalArray, originalList);
DeepCopyExample copy = original.clone();

// Изменения в копии не влияют на оригинал
copy.array[0] = 999;
copy.list.add("d");

System.out.println(original.array[0]); // 1 - оригинал не изменился
System.out.println(original.list.size()); // 3 - оригинал не изменился
```

## Реализация

### Пример 1: Реестр прототипов

```java
// Прототип фигуры
abstract class Shape implements Cloneable {
    protected String color;
    protected int x;
    protected int y;
    
    public Shape() {}
    
    public Shape(Shape source) {
        this.color = source.color;
        this.x = source.x;
        this.y = source.y;
    }
    
    public abstract Shape clone();
    public abstract void draw();
}

// Конкретные фигуры
class Circle extends Shape {
    private int radius;
    
    public Circle() {}
    
    public Circle(Circle source) {
        super(source);
        this.radius = source.radius;
    }
    
    @Override
    public Circle clone() {
        return new Circle(this);
    }
    
    @Override
    public void draw() {
        System.out.println("Circle: color=" + color + ", radius=" + radius);
    }
    
    public void setRadius(int radius) {
        this.radius = radius;
    }
}

class Rectangle extends Shape {
    private int width;
    private int height;
    
    public Rectangle() {}
    
    public Rectangle(Rectangle source) {
        super(source);
        this.width = source.width;
        this.height = source.height;
    }
    
    @Override
    public Rectangle clone() {
        return new Rectangle(this);
    }
    
    @Override
    public void draw() {
        System.out.println("Rectangle: color=" + color + 
                          ", width=" + width + ", height=" + height);
    }
    
    public void setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
    }
}

// Реестр прототипов
class ShapeRegistry {
    private Map<String, Shape> prototypes = new HashMap<>();
    
    public void addPrototype(String key, Shape prototype) {
        prototypes.put(key, prototype);
    }
    
    public Shape getPrototype(String key) {
        Shape prototype = prototypes.get(key);
        if (prototype != null) {
            return prototype.clone();
        }
        return null;
    }
}

// Использование
ShapeRegistry registry = new ShapeRegistry();

// Создаем и регистрируем прототипы
Circle circlePrototype = new Circle();
circlePrototype.color = "red";
circlePrototype.setRadius(10);
registry.addPrototype("redCircle", circlePrototype);

Rectangle rectPrototype = new Rectangle();
rectPrototype.color = "blue";
rectPrototype.setDimensions(20, 30);
registry.addPrototype("blueRectangle", rectPrototype);

// Клонируем прототипы
Shape shape1 = registry.getPrototype("redCircle");
Shape shape2 = registry.getPrototype("blueRectangle");
Shape shape3 = registry.getPrototype("redCircle");

shape1.draw();
shape2.draw();
shape3.draw();
```

### Пример 2: Копирование сложного объекта

```java
// Сложный объект с вложенными структурами
class Document implements Cloneable {
    private String title;
    private String content;
    private Author author;
    private List<Comment> comments;
    private Map<String, String> metadata;
    
    public Document(String title, String content, Author author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.comments = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    // Конструктор копирования для глубокого копирования
    private Document(Document source) {
        this.title = source.title;
        this.content = source.content;
        
        // Глубокое копирование автора
        this.author = new Author(source.author);
        
        // Глубокое копирование комментариев
        this.comments = new ArrayList<>();
        for (Comment comment : source.comments) {
            this.comments.add(new Comment(comment));
        }
        
        // Глубокое копирование метаданных
        this.metadata = new HashMap<>(source.metadata);
    }
    
    @Override
    public Document clone() {
        return new Document(this);
    }
    
    public void addComment(Comment comment) {
        comments.add(comment);
    }
    
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
}

class Author implements Cloneable {
    private String name;
    private String email;
    
    public Author(String name, String email) {
        this.name = name;
        this.email = email;
    }
    
    public Author(Author source) {
        this.name = source.name;
        this.email = source.email;
    }
}

class Comment implements Cloneable {
    private String text;
    private String author;
    private Date timestamp;
    
    public Comment(String text, String author) {
        this.text = text;
        this.author = author;
        this.timestamp = new Date();
    }
    
    public Comment(Comment source) {
        this.text = source.text;
        this.author = source.author;
        this.timestamp = new Date(source.timestamp.getTime());
    }
}

// Использование
Author author = new Author("John Doe", "john@example.com");
Document originalDoc = new Document("My Document", "Content here", author);
originalDoc.addComment(new Comment("Great!", "Reader1"));
originalDoc.setMetadata("version", "1.0");

// Создаем копию
Document copiedDoc = originalDoc.clone();

// Изменения в копии не влияют на оригинал
copiedDoc.setMetadata("version", "2.0");
copiedDoc.addComment(new Comment("Updated", "Reader2"));
```

### Пример 3: Prototype с сериализацией

```java
// Глубокое копирование через сериализацию
class SerializablePrototype implements Serializable {
    private String data;
    private List<String> items;
    private Map<String, Integer> counters;
    
    public SerializablePrototype(String data) {
        this.data = data;
        this.items = new ArrayList<>();
        this.counters = new HashMap<>();
    }
    
    // Глубокое копирование через сериализацию
    public SerializablePrototype deepClone() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this);
            
            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bis);
            
            return (SerializablePrototype) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to clone object", e);
        }
    }
    
    public void addItem(String item) {
        items.add(item);
    }
    
    public void incrementCounter(String key) {
        counters.put(key, counters.getOrDefault(key, 0) + 1);
    }
}

// Использование
SerializablePrototype original = new SerializablePrototype("Original");
original.addItem("item1");
original.incrementCounter("views");

SerializablePrototype clone = original.deepClone();
clone.addItem("item2");
clone.incrementCounter("views");

// Оригинал и клон независимы
System.out.println(original.items.size()); // 1
System.out.println(clone.items.size()); // 2
```

## Примеры использования

### Java Cloneable

```java
// ArrayList реализует Cloneable
ArrayList<String> original = new ArrayList<>();
original.add("a");
original.add("b");

ArrayList<String> clone = (ArrayList<String>) original.clone();
clone.add("c");

System.out.println(original.size()); // 2
System.out.println(clone.size()); // 3
```

### Object.clone()

```java
class Person implements Cloneable {
    private String name;
    private int age;
    
    @Override
    public Person clone() {
        try {
            return (Person) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
```

### Копирование через конструктор (рекомендуется)

```java
class Person {
    private String name;
    private int age;
    
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // Конструктор копирования
    public Person(Person other) {
        this.name = other.name;
        this.age = other.age;
    }
}

// Использование
Person original = new Person("John", 30);
Person copy = new Person(original);
```

## Преимущества и недостатки

### Преимущества

✅ **Производительность**
- Клонирование может быть быстрее создания объекта с нуля
- Особенно для сложных объектов

✅ **Избавление от подклассов**
- Не нужно создавать фабрики для каждого типа объекта

✅ **Создание сложных объектов**
- Упрощает создание объектов со сложной конфигурацией

✅ **Альтернатива наследованию**
- Можно клонировать объекты без знания их классов

### Недостатки

❌ **Сложность глубокого копирования**
- Трудно реализовать для объектов с циклическими ссылками
- Нужно копировать все вложенные объекты

❌ **Cloneable считается плохим дизайном**
- Не является интерфейсом в полном смысле
- Может нарушить инварианты класса
- CloneNotSupportedException - проверяемое исключение

❌ **Проблемы с final полями**
- clone() не может изменить final поля

❌ **Неявное создание объектов**
- Обходит конструктор, что может быть небезопасно

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Prototype?**

*Ответ:* Prototype — это порождающий паттерн, который позволяет копировать объекты без привязки к их конкретным классам. Вместо создания нового объекта с нуля, клонируется существующий прототип.

**2. В чем разница между глубоким и поверхностным копированием?**

*Ответ:*
- **Поверхностное (Shallow)**: копируются значения примитивов и ссылки на объекты. Вложенные объекты остаются общими.
- **Глубокое (Deep)**: копируются и примитивы, и все вложенные объекты рекурсивно. Получается полностью независимая копия.

**3. Как реализовать Prototype в Java?**

*Ответ:* Три основных способа:
1. Реализовать `Cloneable` и переопределить `clone()`
2. Конструктор копирования (рекомендуется)
3. Сериализация/десериализация

**4. Когда следует использовать Prototype?**

*Ответ:*
- Создание объекта дорогостоящее
- Нужно избежать множества подклассов
- Объекты мало отличаются друг от друга
- Нужно скрыть детали создания объекта

### Продвинутые вопросы

**5. Почему Cloneable считается плохим дизайном?**

*Ответ:*
- Не содержит методов (marker interface)
- `clone()` определен в Object, а не в Cloneable
- Бросает проверяемое исключение CloneNotSupportedException
- Создает объект без вызова конструктора
- Сложно правильно реализовать для наследников
- Joshua Bloch рекомендует избегать Cloneable

**6. Как правильно реализовать clone() для наследуемых классов?**

*Ответ:*
```java
class Parent implements Cloneable {
    private int value;
    
    @Override
    public Parent clone() {
        try {
            return (Parent) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

class Child extends Parent {
    private String data;
    
    @Override
    public Child clone() {
        Child clone = (Child) super.clone();
        // Глубокое копирование специфичных полей
        return clone;
    }
}
```

**7. Как клонировать объекты с циклическими ссылками?**

*Ответ:* Нужно отслеживать уже клонированные объекты:
```java
class Node implements Cloneable {
    private String data;
    private Node next;
    
    public Node clone(Map<Node, Node> clonedNodes) {
        if (clonedNodes.containsKey(this)) {
            return clonedNodes.get(this);
        }
        
        Node clone = new Node();
        clone.data = this.data;
        clonedNodes.put(this, clone);
        
        if (this.next != null) {
            clone.next = this.next.clone(clonedNodes);
        }
        
        return clone;
    }
}
```

**8. В чем преимущества конструктора копирования над clone()?**

*Ответ:*
- Явный и понятный API
- Не нужен Cloneable
- Нет проверяемых исключений
- Работает с final полями
- Вызывает конструктор
- Безопаснее для типов (нет приведения типов)

```java
class Person {
    private final String name;
    
    public Person(String name) {
        this.name = name;
    }
    
    // Конструктор копирования
    public Person(Person other) {
        this.name = other.name;
    }
}
```

**9. Как Prototype связан с другими паттернами?**

*Ответ:*
- **Abstract Factory**: может хранить прототипы вместо создания объектов
- **Composite**: часто используется с Prototype для клонирования деревьев
- **Decorator**: можно клонировать декорированные объекты
- **Memento**: использует клонирование для сохранения состояния

**10. Когда НЕ стоит использовать Prototype?**

*Ответ:*
- Объект простой и дешевый в создании
- Требуется только поверхностное копирование примитивов
- Объект имеет сложные циклические зависимости
- Нужен вызов конструктора для валидации
- Класс использует много final полей

---

[← Назад к разделу Порождающие паттерны](README.md)
