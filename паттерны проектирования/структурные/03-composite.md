# Composite (Компоновщик)

Composite — структурный паттерн проектирования, который позволяет сгруппировать объекты в древовидную структуру и работать с ними так, как будто это единичный объект.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Файловая система](#файловая-система)
   - [Графический редактор](#графический-редактор)
   - [Организационная структура](#организационная-структура)
   - [Меню ресторана](#меню-ресторана)
5. [Примеры в JDK и фреймворках](#примеры-в-jdk-и-фреймворках)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Composite используется когда:
- Объекты должны быть реализованы в виде иерархической древовидной структуры
- Клиенты должны единообразно трактовать простые и составные объекты
- Нужно представить часть-целое в виде иерархии

**Типичные примеры использования:**
- Файловые системы (файлы и каталоги)
- Графические редакторы (примитивы и группы фигур)
- Организационные структуры компаний
- GUI компоненты (контейнеры и элементы)
- Меню и подменю
- Синтаксические деревья в компиляторах

## Проблема, которую решает

### Проблема: Различная обработка простых и составных объектов

```java
// Без паттерна Composite - дублирование логики

class File {
    private String name;
    
    public long getSize() {
        return 100; // размер файла
    }
}

class Directory {
    private String name;
    private List<File> files;
    private List<Directory> subdirectories;
    
    public long getSize() {
        long size = 0;
        
        // Обработка файлов
        for (File file : files) {
            size += file.getSize();
        }
        
        // Обработка подкаталогов - другая логика!
        for (Directory dir : subdirectories) {
            size += dir.getSize();
        }
        
        return size;
    }
}

// Клиентский код должен различать файлы и каталоги
if (obj instanceof File) {
    File file = (File) obj;
    size = file.getSize();
} else if (obj instanceof Directory) {
    Directory dir = (Directory) obj;
    size = dir.getSize();
}
```

**Проблемы:**
- Клиент должен знать о различиях между простыми и составными объектами
- Дублирование логики обработки
- Сложность добавления новых типов компонентов
- Нарушение Open/Closed Principle

### Решение: Composite

Создать общий интерфейс для простых и составных объектов.

```java
// Общий интерфейс
interface FileSystemComponent {
    long getSize();
    void print();
}

// Простой объект (лист)
class File implements FileSystemComponent {
    private String name;
    private long size;
    
    public long getSize() {
        return size;
    }
}

// Составной объект (контейнер)
class Directory implements FileSystemComponent {
    private String name;
    private List<FileSystemComponent> children = new ArrayList<>();
    
    public void add(FileSystemComponent component) {
        children.add(component);
    }
    
    public long getSize() {
        return children.stream()
                       .mapToLong(FileSystemComponent::getSize)
                       .sum();
    }
}

// Клиентский код работает единообразно
FileSystemComponent component = ...; // файл или каталог
long size = component.getSize(); // работает одинаково!
```

## Структура паттерна

```java
// Component - общий интерфейс
interface Component {
    void operation();
    
    // Методы для управления дочерними элементами
    default void add(Component component) {
        throw new UnsupportedOperationException();
    }
    
    default void remove(Component component) {
        throw new UnsupportedOperationException();
    }
    
    default Component getChild(int index) {
        throw new UnsupportedOperationException();
    }
}

// Leaf - простой объект (лист дерева)
class Leaf implements Component {
    private String name;
    
    public Leaf(String name) {
        this.name = name;
    }
    
    @Override
    public void operation() {
        System.out.println("Leaf: " + name);
    }
}

// Composite - составной объект (контейнер)
class Composite implements Component {
    private String name;
    private List<Component> children = new ArrayList<>();
    
    public Composite(String name) {
        this.name = name;
    }
    
    @Override
    public void add(Component component) {
        children.add(component);
    }
    
    @Override
    public void remove(Component component) {
        children.remove(component);
    }
    
    @Override
    public Component getChild(int index) {
        return children.get(index);
    }
    
    @Override
    public void operation() {
        System.out.println("Composite: " + name);
        for (Component child : children) {
            child.operation();
        }
    }
}
```

## Реализация

### Файловая система

```java
// Компонент файловой системы
interface FileSystemComponent {
    String getName();
    long getSize();
    void print(String indent);
    
    // Методы для составных объектов
    default void add(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot add to a file");
    }
    
    default void remove(FileSystemComponent component) {
        throw new UnsupportedOperationException("Cannot remove from a file");
    }
    
    default List<FileSystemComponent> getChildren() {
        throw new UnsupportedOperationException("File has no children");
    }
}

// Лист - файл
class File implements FileSystemComponent {
    private String name;
    private long size;
    
    public File(String name, long size) {
        this.name = name;
        this.size = size;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
    }
}

// Композит - каталог
class Directory implements FileSystemComponent {
    private String name;
    private List<FileSystemComponent> children = new ArrayList<>();
    
    public Directory(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public long getSize() {
        return children.stream()
                       .mapToLong(FileSystemComponent::getSize)
                       .sum();
    }
    
    @Override
    public void add(FileSystemComponent component) {
        children.add(component);
    }
    
    @Override
    public void remove(FileSystemComponent component) {
        children.remove(component);
    }
    
    @Override
    public List<FileSystemComponent> getChildren() {
        return new ArrayList<>(children);
    }
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "📁 " + name + "/ (" + getSize() + " bytes total)");
        for (FileSystemComponent child : children) {
            child.print(indent + "  ");
        }
    }
}

// Использование
class FileSystemDemo {
    public static void main(String[] args) {
        // Создаем структуру каталогов
        Directory root = new Directory("root");
        
        Directory home = new Directory("home");
        Directory user = new Directory("user");
        
        File bashrc = new File(".bashrc", 1024);
        File profile = new File(".profile", 512);
        
        user.add(bashrc);
        user.add(profile);
        
        Directory documents = new Directory("documents");
        File resume = new File("resume.pdf", 50000);
        File letter = new File("cover_letter.docx", 25000);
        
        documents.add(resume);
        documents.add(letter);
        user.add(documents);
        
        home.add(user);
        
        Directory etc = new Directory("etc");
        File hosts = new File("hosts", 256);
        File fstab = new File("fstab", 384);
        
        etc.add(hosts);
        etc.add(fstab);
        
        root.add(home);
        root.add(etc);
        
        // Вывод структуры
        root.print("");
        
        System.out.println("\nОбщий размер root: " + root.getSize() + " bytes");
        System.out.println("Размер documents: " + documents.getSize() + " bytes");
    }
}
```

### Графический редактор

```java
// Компонент графического объекта
interface GraphicComponent {
    void draw();
    void move(int x, int y);
    
    default void add(GraphicComponent component) {
        throw new UnsupportedOperationException();
    }
    
    default void remove(GraphicComponent component) {
        throw new UnsupportedOperationException();
    }
}

// Простые графические объекты (листья)
class Circle implements GraphicComponent {
    private int x, y, radius;
    
    public Circle(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }
    
    @Override
    public void draw() {
        System.out.println("Рисуем круг в (" + x + "," + y + ") радиус " + radius);
    }
    
    @Override
    public void move(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;
        System.out.println("Перемещаем круг на (" + deltaX + "," + deltaY + ")");
    }
}

class Rectangle implements GraphicComponent {
    private int x, y, width, height;
    
    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    
    @Override
    public void draw() {
        System.out.println("Рисуем прямоугольник в (" + x + "," + y + 
                         ") размер " + width + "x" + height);
    }
    
    @Override
    public void move(int deltaX, int deltaY) {
        x += deltaX;
        y += deltaY;
        System.out.println("Перемещаем прямоугольник на (" + deltaX + "," + deltaY + ")");
    }
}

class Line implements GraphicComponent {
    private int x1, y1, x2, y2;
    
    public Line(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    @Override
    public void draw() {
        System.out.println("Рисуем линию от (" + x1 + "," + y1 + ") до (" + x2 + "," + y2 + ")");
    }
    
    @Override
    public void move(int deltaX, int deltaY) {
        x1 += deltaX;
        y1 += deltaY;
        x2 += deltaX;
        y2 += deltaY;
        System.out.println("Перемещаем линию на (" + deltaX + "," + deltaY + ")");
    }
}

// Композит - группа объектов
class GraphicGroup implements GraphicComponent {
    private String name;
    private List<GraphicComponent> components = new ArrayList<>();
    
    public GraphicGroup(String name) {
        this.name = name;
    }
    
    @Override
    public void add(GraphicComponent component) {
        components.add(component);
    }
    
    @Override
    public void remove(GraphicComponent component) {
        components.remove(component);
    }
    
    @Override
    public void draw() {
        System.out.println("=== Группа: " + name + " ===");
        for (GraphicComponent component : components) {
            component.draw();
        }
        System.out.println("=== Конец группы: " + name + " ===");
    }
    
    @Override
    public void move(int deltaX, int deltaY) {
        System.out.println("Перемещаем группу " + name + " на (" + deltaX + "," + deltaY + ")");
        for (GraphicComponent component : components) {
            component.move(deltaX, deltaY);
        }
    }
}

// Использование
class GraphicsDemo {
    public static void main(String[] args) {
        // Создаем простые фигуры
        Circle circle = new Circle(10, 10, 5);
        Rectangle rect = new Rectangle(20, 20, 30, 15);
        Line line = new Line(0, 0, 50, 50);
        
        // Создаем группу "Дом"
        GraphicGroup house = new GraphicGroup("Дом");
        Rectangle base = new Rectangle(100, 200, 80, 60);
        Rectangle door = new Rectangle(130, 220, 20, 40);
        Rectangle window = new Rectangle(150, 220, 15, 15);
        
        house.add(base);
        house.add(door);
        house.add(window);
        
        // Создаем группу "Пейзаж"
        GraphicGroup landscape = new GraphicGroup("Пейзаж");
        landscape.add(circle); // солнце
        landscape.add(line);   // горизонт
        landscape.add(house);  // дом (группа внутри группы!)
        
        // Рисуем всю сцену
        landscape.draw();
        
        System.out.println("\nПеремещаем весь пейзаж:");
        landscape.move(10, 10);
    }
}
```

### Организационная структура

```java
// Компонент организационной структуры
interface OrganizationComponent {
    String getName();
    double getSalary();
    void print(String indent);
    
    default void add(OrganizationComponent component) {
        throw new UnsupportedOperationException();
    }
    
    default void remove(OrganizationComponent component) {
        throw new UnsupportedOperationException();
    }
}

// Лист - сотрудник
class Employee implements OrganizationComponent {
    private String name;
    private String position;
    private double salary;
    
    public Employee(String name, String position, double salary) {
        this.name = name;
        this.position = position;
        this.salary = salary;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public double getSalary() {
        return salary;
    }
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "👤 " + name + " - " + position + 
                         " ($" + salary + ")");
    }
}

// Композит - отдел
class Department implements OrganizationComponent {
    private String name;
    private List<OrganizationComponent> members = new ArrayList<>();
    
    public Department(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public double getSalary() {
        return members.stream()
                     .mapToDouble(OrganizationComponent::getSalary)
                     .sum();
    }
    
    @Override
    public void add(OrganizationComponent component) {
        members.add(component);
    }
    
    @Override
    public void remove(OrganizationComponent component) {
        members.remove(component);
    }
    
    @Override
    public void print(String indent) {
        System.out.println(indent + "🏢 " + name + " (Бюджет: $" + getSalary() + ")");
        for (OrganizationComponent member : members) {
            member.print(indent + "  ");
        }
    }
}

// Использование
class OrganizationDemo {
    public static void main(String[] args) {
        // Создаем организационную структуру
        Department company = new Department("TechCorp");
        
        // Отдел разработки
        Department engineering = new Department("Engineering");
        Employee cto = new Employee("Иван Петров", "CTO", 15000);
        Employee dev1 = new Employee("Мария Сидорова", "Senior Developer", 8000);
        Employee dev2 = new Employee("Петр Иванов", "Developer", 6000);
        Employee qa = new Employee("Анна Смирнова", "QA Engineer", 5000);
        
        engineering.add(cto);
        engineering.add(dev1);
        engineering.add(dev2);
        engineering.add(qa);
        
        // Отдел продаж
        Department sales = new Department("Sales");
        Employee salesDir = new Employee("Ольга Козлова", "Sales Director", 12000);
        Employee sales1 = new Employee("Дмитрий Попов", "Sales Manager", 7000);
        Employee sales2 = new Employee("Елена Новикова", "Sales Manager", 7000);
        
        sales.add(salesDir);
        sales.add(sales1);
        sales.add(sales2);
        
        // HR отдел
        Department hr = new Department("HR");
        Employee hrDir = new Employee("Светлана Морозова", "HR Director", 10000);
        Employee recruiter = new Employee("Александр Соколов", "Recruiter", 5500);
        
        hr.add(hrDir);
        hr.add(recruiter);
        
        // Генеральный директор
        Employee ceo = new Employee("Сергей Волков", "CEO", 20000);
        
        // Собираем компанию
        company.add(ceo);
        company.add(engineering);
        company.add(sales);
        company.add(hr);
        
        // Вывод структуры
        company.print("");
        
        System.out.println("\nОбщий фонд оплаты труда: $" + company.getSalary());
        System.out.println("Бюджет отдела разработки: $" + engineering.getSalary());
    }
}
```

### Меню ресторана

```java
// Компонент меню
interface MenuComponent {
    String getName();
    double getPrice();
    String getDescription();
    boolean isVegetarian();
    void print();
    
    default void add(MenuComponent component) {
        throw new UnsupportedOperationException();
    }
    
    default void remove(MenuComponent component) {
        throw new UnsupportedOperationException();
    }
}

// Лист - блюдо
class MenuItem implements MenuComponent {
    private String name;
    private String description;
    private double price;
    private boolean vegetarian;
    
    public MenuItem(String name, String description, double price, boolean vegetarian) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.vegetarian = vegetarian;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public double getPrice() {
        return price;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isVegetarian() {
        return vegetarian;
    }
    
    @Override
    public void print() {
        System.out.print("  " + name);
        if (vegetarian) {
            System.out.print(" (V)");
        }
        System.out.println(", $" + price);
        System.out.println("     -- " + description);
    }
}

// Композит - раздел меню или подменю
class Menu implements MenuComponent {
    private String name;
    private String description;
    private List<MenuComponent> components = new ArrayList<>();
    
    public Menu(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public double getPrice() {
        return components.stream()
                        .mapToDouble(MenuComponent::getPrice)
                        .sum();
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isVegetarian() {
        return components.stream()
                        .allMatch(MenuComponent::isVegetarian);
    }
    
    @Override
    public void add(MenuComponent component) {
        components.add(component);
    }
    
    @Override
    public void remove(MenuComponent component) {
        components.remove(component);
    }
    
    @Override
    public void print() {
        System.out.println("\n" + name);
        System.out.println("  " + description);
        System.out.println("-----------------------");
        
        for (MenuComponent component : components) {
            component.print();
        }
    }
}

// Использование
class RestaurantDemo {
    public static void main(String[] args) {
        // Главное меню
        Menu mainMenu = new Menu("ГЛАВНОЕ МЕНЮ", "Меню ресторана 'Вкусная еда'");
        
        // Завтраки
        Menu breakfastMenu = new Menu("ЗАВТРАКИ", "Подаются до 11:00");
        breakfastMenu.add(new MenuItem(
            "Омлет с беконом",
            "Три яйца с беконом и тостами",
            7.99,
            false
        ));
        breakfastMenu.add(new MenuItem(
            "Овсяная каша",
            "С ягодами и медом",
            5.99,
            true
        ));
        breakfastMenu.add(new MenuItem(
            "Блинчики",
            "С кленовым сиропом",
            6.99,
            true
        ));
        
        // Обеды
        Menu lunchMenu = new Menu("ОБЕДЫ", "Подаются с 11:00 до 16:00");
        
        // Подменю - супы
        Menu soupsMenu = new Menu("Супы", "Домашние супы");
        soupsMenu.add(new MenuItem(
            "Борщ",
            "Традиционный украинский суп",
            8.99,
            false
        ));
        soupsMenu.add(new MenuItem(
            "Овощной суп",
            "Легкий овощной суп",
            6.99,
            true
        ));
        
        // Подменю - основные блюда
        Menu mainCourses = new Menu("Основные блюда", "Горячие блюда");
        mainCourses.add(new MenuItem(
            "Стейк",
            "Говяжий стейк с овощами",
            24.99,
            false
        ));
        mainCourses.add(new MenuItem(
            "Паста Примавера",
            "Паста с овощами",
            14.99,
            true
        ));
        
        lunchMenu.add(soupsMenu);
        lunchMenu.add(mainCourses);
        
        // Напитки
        Menu drinksMenu = new Menu("НАПИТКИ", "Холодные и горячие напитки");
        drinksMenu.add(new MenuItem(
            "Кофе",
            "Свежесваренный кофе",
            2.99,
            true
        ));
        drinksMenu.add(new MenuItem(
            "Свежевыжатый сок",
            "Апельсиновый сок",
            4.99,
            true
        ));
        
        // Собираем меню
        mainMenu.add(breakfastMenu);
        mainMenu.add(lunchMenu);
        mainMenu.add(drinksMenu);
        
        // Выводим меню
        mainMenu.print();
        
        // Вегетарианские блюда
        System.out.println("\n\n=== ВЕГЕТАРИАНСКОЕ МЕНЮ ===");
        printVegetarianMenu(mainMenu);
    }
    
    private static void printVegetarianMenu(MenuComponent component) {
        try {
            // Если это меню, проверяем его элементы
            for (int i = 0; ; i++) {
                // Попытка получить дочерний элемент вызовет исключение у листьев
                MenuComponent child = component.getChild(i);
                printVegetarianMenu(child);
            }
        } catch (UnsupportedOperationException e) {
            // Это лист - проверяем, вегетарианское ли
            if (component.isVegetarian()) {
                component.print();
            }
        }
    }
}
```

## Примеры в JDK и фреймворках

### AWT/Swing Components

```java
// java.awt.Container и Component - классический Composite

import java.awt.*;
import javax.swing.*;

JFrame frame = new JFrame("Composite Example");
JPanel panel = new JPanel(); // Composite

// Добавляем компоненты (листья и другие композиты)
panel.add(new JButton("OK"));      // Leaf
panel.add(new JLabel("Text"));     // Leaf
panel.add(new JTextField(20));     // Leaf

JPanel innerPanel = new JPanel();  // Nested Composite
innerPanel.add(new JCheckBox("Option"));
panel.add(innerPanel);

frame.add(panel);
```

### Java Collections

```java
// Collection и его реализации
List<Object> list = new ArrayList<>();
list.add("String");           // простой объект
list.add(Arrays.asList(1,2)); // вложенная коллекция

// Обработка единообразна
list.forEach(System.out::println);
```

### XML DOM

```java
import org.w3c.dom.*;

// Node - Component
// Element - Composite
// Text, Attribute - Leaf

Document doc = ...;
Element root = doc.getDocumentElement(); // Composite
NodeList children = root.getChildNodes(); // получаем дочерние узлы
```

### JavaFX Scene Graph

```java
import javafx.scene.Group;
import javafx.scene.shape.*;

// Group - Composite
// Shape - Component
// Circle, Rectangle - Leaf

Group group = new Group();
group.getChildren().add(new Circle(50));
group.getChildren().add(new Rectangle(100, 100));
```

## Преимущества и недостатки

### Преимущества

**1. Единообразная работа с простыми и составными объектами**
```java
// Один и тот же код для файла и каталога
FileSystemComponent component = ...;
long size = component.getSize(); // работает для обоих!
```

**2. Упрощение клиентского кода**
- Не нужны проверки типов
- Нет дублирования логики обработки
- Код становится чище и понятнее

**3. Легко добавлять новые типы компонентов**
```java
// Добавляем симлинк - не нужно менять существующий код
class SymLink implements FileSystemComponent {
    // реализация
}
```

**4. Удобство работы с древовидными структурами**
- Рекурсивная обработка естественна
- Легко реализовать операции над деревом

**5. Соблюдение Open/Closed Principle**
- Открыт для расширения (новые компоненты)
- Закрыт для модификации (существующий код не меняется)

### Недостатки

**1. Может сделать дизайн слишком общим**
```java
// Иногда нужны специфичные операции
interface FileSystemComponent {
    void changePermissions(int mode); // не применимо к файлам в памяти
}
```

**2. Сложность ограничения типов компонентов**
```java
// Как запретить добавление файла в файл?
File file = new File("test.txt", 100);
file.add(new File("inner.txt", 50)); // должно быть запрещено!
```

**3. Нарушение принципа разделения интерфейсов**
- Leaf вынужден реализовывать методы управления дочерними элементами
- Обычно бросают UnsupportedOperationException

**4. Производительность при глубоких иерархиях**
```java
// Рекурсивный обход может быть медленным
public long getSize() {
    return children.stream()
                  .mapToLong(Component::getSize) // каждый элемент обходит своих детей
                  .sum();
}
```

## Вопросы на собеседовании

**1. В чем основное отличие Composite от Decorator?**

*Ответ:*
- **Composite** управляет множеством дочерних объектов и представляет часть-целое. Фокус на структуре дерева.
- **Decorator** добавляет обязанности одному объекту. Фокус на расширении функциональности.

```java
// Composite - много детей
class Directory implements FileSystem {
    List<FileSystem> children; // 0..N детей
}

// Decorator - один обернутый объект
class CompressedFile implements FileSystem {
    FileSystem wrapped; // ровно 1 объект
}
```

**2. Как реализовать безопасный Composite (без UnsupportedOperationException)?**

*Ответ:*
Разделить интерфейсы для листьев и композитов:

```java
// Базовый интерфейс - только общие операции
interface Component {
    String getName();
    void operation();
}

// Интерфейс для композитов
interface CompositeComponent extends Component {
    void add(Component component);
    void remove(Component component);
    List<Component> getChildren();
}

// Лист - не имеет методов управления детьми
class Leaf implements Component {
    @Override
    public void operation() { }
}

// Композит - имеет методы управления
class Composite implements CompositeComponent {
    private List<Component> children = new ArrayList<>();
    
    @Override
    public void add(Component component) {
        children.add(component);
    }
    // остальные методы
}

// Клиент проверяет тип, если нужно добавить
if (component instanceof CompositeComponent) {
    ((CompositeComponent) component).add(newComponent);
}
```

**3. Как обходить дерево Composite? Какие есть стратегии?**

*Ответ:*
Есть несколько стратегий обхода:

```java
// 1. Depth-First (в глубину)
void traverseDepthFirst(Component component) {
    component.operation();
    if (component instanceof Composite) {
        for (Component child : ((Composite) component).getChildren()) {
            traverseDepthFirst(child); // рекурсивно
        }
    }
}

// 2. Breadth-First (в ширину)
void traverseBreadthFirst(Component root) {
    Queue<Component> queue = new LinkedList<>();
    queue.add(root);
    
    while (!queue.isEmpty()) {
        Component component = queue.poll();
        component.operation();
        
        if (component instanceof Composite) {
            queue.addAll(((Composite) component).getChildren());
        }
    }
}

// 3. Visitor Pattern для сложной обработки
interface ComponentVisitor {
    void visitLeaf(Leaf leaf);
    void visitComposite(Composite composite);
}
```

**4. Как реализовать кэширование в Composite для оптимизации?**

*Ответ:*
```java
class CachedDirectory implements FileSystemComponent {
    private List<FileSystemComponent> children = new ArrayList<>();
    private Long cachedSize = null;
    private boolean dirty = true;
    
    @Override
    public void add(FileSystemComponent component) {
        children.add(component);
        dirty = true; // инвалидируем кэш
    }
    
    @Override
    public long getSize() {
        if (dirty || cachedSize == null) {
            cachedSize = children.stream()
                                .mapToLong(FileSystemComponent::getSize)
                                .sum();
            dirty = false;
        }
        return cachedSize;
    }
}
```

**5. Можно ли использовать Composite с другими паттернами?**

*Ответ:*
Да, Composite часто комбинируется с:

- **Iterator** для обхода дерева
```java
class CompositeIterator implements Iterator<Component> {
    private Stack<Iterator<Component>> stack = new Stack<>();
    // реализация обхода
}
```

- **Visitor** для операций над деревом
```java
interface Visitor {
    void visit(Leaf leaf);
    void visit(Composite composite);
}
```

- **Chain of Responsibility** для передачи запросов вверх/вниз по иерархии
- **Decorator** для добавления функциональности компонентам
- **Factory/Builder** для создания сложных структур

**6. Как обеспечить thread-safety в Composite?**

*Ответ:*
```java
class ThreadSafeComposite implements Component {
    private final List<Component> children = 
        Collections.synchronizedList(new ArrayList<>());
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public void add(Component component) {
        lock.writeLock().lock();
        try {
            children.add(component);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void operation() {
        lock.readLock().lock();
        try {
            for (Component child : children) {
                child.operation();
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}

// Или использовать CopyOnWriteArrayList для read-heavy сценариев
private final List<Component> children = new CopyOnWriteArrayList<>();
```

**7. Какие проблемы могут возникнуть при использовании Composite?**

*Ответ:*
- **Циклические ссылки**: компонент может быть добавлен в своего потомка
```java
public void add(Component component) {
    if (isDescendant(component)) {
        throw new IllegalArgumentException("Циклическая ссылка!");
    }
    children.add(component);
}
```

- **Memory leaks**: большие деревья могут занимать много памяти
- **Performance**: глубокая рекурсия может вызвать StackOverflowError
- **Неопределенность порядка**: порядок обхода может быть важен

**8. Как реализовать поиск в дереве Composite?**

*Ответ:*
```java
interface SearchableComponent extends Component {
    Component find(String name);
}

class SearchableComposite implements SearchableComponent {
    private String name;
    private List<SearchableComponent> children = new ArrayList<>();
    
    @Override
    public Component find(String searchName) {
        if (this.name.equals(searchName)) {
            return this;
        }
        
        for (SearchableComponent child : children) {
            Component found = child.find(searchName);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
}

class SearchableLeaf implements SearchableComponent {
    private String name;
    
    @Override
    public Component find(String searchName) {
        return this.name.equals(searchName) ? this : null;
    }
}
```

**9. Как получить родительский элемент в Composite?**

*Ответ:*
```java
interface Component {
    void setParent(Component parent);
    Component getParent();
}

class Composite implements Component {
    private Component parent;
    private List<Component> children = new ArrayList<>();
    
    @Override
    public void add(Component component) {
        children.add(component);
        component.setParent(this); // устанавливаем родителя
    }
    
    @Override
    public void setParent(Component parent) {
        this.parent = parent;
    }
    
    @Override
    public Component getParent() {
        return parent;
    }
    
    // Путь до корня
    public List<Component> getPath() {
        List<Component> path = new ArrayList<>();
        Component current = this;
        while (current != null) {
            path.add(0, current);
            current = current.getParent();
        }
        return path;
    }
}
```

**10. Приведите real-world пример Composite в enterprise приложениях**

*Ответ:*
```java
// Система управления правами доступа

interface Permission {
    boolean hasAccess(User user, Resource resource);
}

// Лист - простое разрешение
class SimplePermission implements Permission {
    private String resourceType;
    private String action; // read, write, delete
    
    @Override
    public boolean hasAccess(User user, Resource resource) {
        return user.hasRole(resourceType, action);
    }
}

// Композит - группа разрешений
class CompositePermission implements Permission {
    private List<Permission> permissions = new ArrayList<>();
    private LogicalOperator operator; // AND или OR
    
    public void add(Permission permission) {
        permissions.add(permission);
    }
    
    @Override
    public boolean hasAccess(User user, Resource resource) {
        if (operator == LogicalOperator.AND) {
            return permissions.stream()
                             .allMatch(p -> p.hasAccess(user, resource));
        } else {
            return permissions.stream()
                             .anyMatch(p -> p.hasAccess(user, resource));
        }
    }
}

// Использование
Permission readPermission = new SimplePermission("document", "read");
Permission writePermission = new SimplePermission("document", "write");

// Пользователь должен иметь И чтение, И запись
CompositePermission fullAccess = new CompositePermission(LogicalOperator.AND);
fullAccess.add(readPermission);
fullAccess.add(writePermission);

// Или админ, или владелец
CompositePermission adminOrOwner = new CompositePermission(LogicalOperator.OR);
adminOrOwner.add(new RolePermission("ADMIN"));
adminOrOwner.add(new OwnershipPermission());

if (fullAccess.hasAccess(currentUser, document)) {
    // разрешить доступ
}
```

Это позволяет:
- Создавать сложные правила доступа из простых
- Комбинировать разрешения с AND/OR логикой
- Легко добавлять новые типы разрешений
- Вычислять права доступа рекурсивно
