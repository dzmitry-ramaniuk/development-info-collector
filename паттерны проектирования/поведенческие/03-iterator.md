# Iterator (Итератор)

Iterator — поведенческий паттерн проектирования, который предоставляет способ последовательного доступа ко всем элементам составного объекта, не раскрывая его внутреннего представления.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Пример 1: Пользовательская коллекция с двунаправленным итератором](#пример-1-пользовательская-коллекция-с-двунаправленным-итератором)
   - [Пример 2: Обход дерева (DFS и BFS)](#пример-2-обход-дерева-dfs-и-bfs)
   - [Пример 3: Итератор для Composite (обход каталогов)](#пример-3-итератор-для-composite-обход-каталогов)
   - [Пример 4: Фильтрующий итератор](#пример-4-фильтрующий-итератор)
5. [Внутренние vs Внешние итераторы](#внутренние-vs-внешние-итераторы)
6. [Примеры из JDK](#примеры-из-jdk)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Iterator используется когда:
- Нужен последовательный доступ к элементам коллекции без раскрытия её внутренней структуры
- Необходимо поддерживать несколько способов обхода одной коллекции
- Нужен единый интерфейс для обхода различных структур данных
- Необходимо несколько одновременных обходов одной коллекции

**Типичные примеры использования:**
- Обход коллекций (списки, деревья, графы)
- Чтение потоков данных
- Парсинг структурированных данных
- Обход файловой системы
- Навигация по результатам запросов

## Проблема, которую решает

### Проблема: Раскрытие внутренней структуры

```java
public class BookCollection {
    private List<Book> books = new ArrayList<>();
    
    // Проблема 1: Раскрывает внутреннюю структуру
    public List<Book> getBooks() {
        return books; // Клиент получает прямой доступ к внутренней коллекции
    }
    
    // Проблема 2: Жесткая привязка к способу обхода
    public void printAllBooks() {
        for (Book book : books) {
            System.out.println(book);
        }
        // Что если нужен обход в обратном порядке?
        // Что если нужно фильтровать элементы?
    }
}

// Клиентский код привязан к конкретной реализации
BookCollection collection = new BookCollection();
List<Book> books = collection.getBooks();
for (Book book : books) {
    // Если изменится внутренняя структура, код сломается
}
```

**Проблемы:**
- Раскрытие внутренней реализации коллекции
- Невозможность изменить способ обхода
- Невозможность иметь несколько одновременных обходов
- Нарушение инкапсуляции
- Жесткая связь между клиентом и коллекцией

### Решение: Iterator

Предоставить единый интерфейс для последовательного доступа к элементам без раскрытия внутренней структуры.

## Структура паттерна

```java
// Интерфейс итератора
interface Iterator<T> {
    boolean hasNext();
    T next();
    void remove(); // Опционально
}

// Интерфейс коллекции
interface Aggregate<T> {
    Iterator<T> createIterator();
}

// Конкретная коллекция
class ConcreteAggregate<T> implements Aggregate<T> {
    private List<T> items = new ArrayList<>();
    
    public void add(T item) {
        items.add(item);
    }
    
    @Override
    public Iterator<T> createIterator() {
        return new ConcreteIterator();
    }
    
    // Вложенный класс итератора
    private class ConcreteIterator implements Iterator<T> {
        private int position = 0;
        
        @Override
        public boolean hasNext() {
            return position < items.size();
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return items.get(position++);
        }
        
        @Override
        public void remove() {
            items.remove(--position);
        }
    }
}
```

## Реализация

### Пример 1: Пользовательская коллекция с двунаправленным итератором

Реализация коллекции книг с возможностью обхода в обоих направлениях.

```java
// ============= Модель данных =============

class Book {
    private String title;
    private String author;
    private int year;
    private String isbn;
    
    public Book(String title, String author, int year, String isbn) {
        this.title = title;
        this.author = author;
        this.year = year;
        this.isbn = isbn;
    }
    
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getYear() { return year; }
    public String getIsbn() { return isbn; }
    
    @Override
    public String toString() {
        return String.format("\"%s\" by %s (%d) [ISBN: %s]", 
            title, author, year, isbn);
    }
}

// ============= Интерфейсы =============

// Двунаправленный итератор
interface BidirectionalIterator<T> {
    boolean hasNext();
    boolean hasPrevious();
    T next();
    T previous();
    int currentIndex();
    void reset();
}

// Агрегат с итераторами
interface IterableCollection<T> {
    BidirectionalIterator<T> createForwardIterator();
    BidirectionalIterator<T> createBackwardIterator();
    int size();
}

// ============= Коллекция книг =============

class BookCollection implements IterableCollection<Book> {
    private Book[] books;
    private int size;
    private static final int DEFAULT_CAPACITY = 10;
    
    public BookCollection() {
        this(DEFAULT_CAPACITY);
    }
    
    public BookCollection(int capacity) {
        books = new Book[capacity];
        size = 0;
    }
    
    public void addBook(Book book) {
        if (size == books.length) {
            resize();
        }
        books[size++] = book;
    }
    
    private void resize() {
        Book[] newBooks = new Book[books.length * 2];
        System.arraycopy(books, 0, newBooks, 0, books.length);
        books = newBooks;
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public BidirectionalIterator<Book> createForwardIterator() {
        return new ForwardIterator();
    }
    
    @Override
    public BidirectionalIterator<Book> createBackwardIterator() {
        return new BackwardIterator();
    }
    
    // ============= Итераторы (вложенные классы) =============
    
    // Прямой итератор
    private class ForwardIterator implements BidirectionalIterator<Book> {
        private int currentIndex = 0;
        
        @Override
        public boolean hasNext() {
            return currentIndex < size;
        }
        
        @Override
        public boolean hasPrevious() {
            return currentIndex > 0;
        }
        
        @Override
        public Book next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            return books[currentIndex++];
        }
        
        @Override
        public Book previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException("No previous elements");
            }
            return books[--currentIndex];
        }
        
        @Override
        public int currentIndex() {
            return currentIndex;
        }
        
        @Override
        public void reset() {
            currentIndex = 0;
        }
    }
    
    // Обратный итератор
    private class BackwardIterator implements BidirectionalIterator<Book> {
        private int currentIndex;
        
        public BackwardIterator() {
            currentIndex = size - 1;
        }
        
        @Override
        public boolean hasNext() {
            return currentIndex >= 0;
        }
        
        @Override
        public boolean hasPrevious() {
            return currentIndex < size - 1;
        }
        
        @Override
        public Book next() {
            if (!hasNext()) {
                throw new NoSuchElementException("No more elements");
            }
            return books[currentIndex--];
        }
        
        @Override
        public Book previous() {
            if (!hasPrevious()) {
                throw new NoSuchElementException("No previous elements");
            }
            return books[++currentIndex];
        }
        
        @Override
        public int currentIndex() {
            return currentIndex;
        }
        
        @Override
        public void reset() {
            currentIndex = size - 1;
        }
    }
}

// ============= Использование =============

class BookCollectionDemo {
    public static void main(String[] args) {
        BookCollection library = new BookCollection();
        
        library.addBook(new Book("Effective Java", "Joshua Bloch", 2018, "978-0134685991"));
        library.addBook(new Book("Clean Code", "Robert Martin", 2008, "978-0132350884"));
        library.addBook(new Book("Design Patterns", "Gang of Four", 1994, "978-0201633612"));
        library.addBook(new Book("Refactoring", "Martin Fowler", 2018, "978-0134757599"));
        library.addBook(new Book("Head First Design Patterns", "Freeman & Freeman", 2004, "978-0596007126"));
        
        System.out.println("=== Library Collection (" + library.size() + " books) ===\n");
        
        // Прямой обход
        System.out.println("--- Forward Iteration ---");
        BidirectionalIterator<Book> forward = library.createForwardIterator();
        while (forward.hasNext()) {
            System.out.println((forward.currentIndex() + 1) + ". " + forward.next());
        }
        
        // Обратный обход
        System.out.println("\n--- Backward Iteration ---");
        BidirectionalIterator<Book> backward = library.createBackwardIterator();
        while (backward.hasNext()) {
            System.out.println((backward.currentIndex() + 1) + ". " + backward.next());
        }
        
        // Двунаправленная навигация
        System.out.println("\n--- Bidirectional Navigation ---");
        forward.reset();
        System.out.println("First book: " + forward.next());
        System.out.println("Second book: " + forward.next());
        System.out.println("Going back: " + forward.previous());
        System.out.println("Going forward again: " + forward.next());
    }
}
```

### Пример 2: Обход дерева (DFS и BFS)

Различные способы обхода древовидной структуры.

```java
// ============= Модель дерева =============

class TreeNode<T> {
    private T data;
    private List<TreeNode<T>> children;
    
    public TreeNode(T data) {
        this.data = data;
        this.children = new ArrayList<>();
    }
    
    public void addChild(TreeNode<T> child) {
        children.add(child);
    }
    
    public T getData() {
        return data;
    }
    
    public List<TreeNode<T>> getChildren() {
        return new ArrayList<>(children); // Защитная копия
    }
    
    public boolean hasChildren() {
        return !children.isEmpty();
    }
}

// ============= Интерфейс итератора дерева =============

interface TreeIterator<T> extends java.util.Iterator<T> {
    String getTraversalType();
}

// ============= Дерево =============

class Tree<T> implements Iterable<T> {
    private TreeNode<T> root;
    
    public Tree(TreeNode<T> root) {
        this.root = root;
    }
    
    public TreeNode<T> getRoot() {
        return root;
    }
    
    @Override
    public java.util.Iterator<T> iterator() {
        return createDFSIterator(); // По умолчанию DFS
    }
    
    public TreeIterator<T> createDFSIterator() {
        return new DFSIterator();
    }
    
    public TreeIterator<T> createBFSIterator() {
        return new BFSIterator();
    }
    
    // ============= DFS итератор (поиск в глубину) =============
    
    private class DFSIterator implements TreeIterator<T> {
        private Stack<TreeNode<T>> stack = new Stack<>();
        
        public DFSIterator() {
            if (root != null) {
                stack.push(root);
            }
        }
        
        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            TreeNode<T> current = stack.pop();
            
            // Добавляем детей в обратном порядке для корректного обхода
            List<TreeNode<T>> children = current.getChildren();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
            
            return current.getData();
        }
        
        @Override
        public String getTraversalType() {
            return "Depth-First Search (DFS)";
        }
    }
    
    // ============= BFS итератор (поиск в ширину) =============
    
    private class BFSIterator implements TreeIterator<T> {
        private Queue<TreeNode<T>> queue = new LinkedList<>();
        
        public BFSIterator() {
            if (root != null) {
                queue.offer(root);
            }
        }
        
        @Override
        public boolean hasNext() {
            return !queue.isEmpty();
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            TreeNode<T> current = queue.poll();
            
            // Добавляем всех детей в очередь
            queue.addAll(current.getChildren());
            
            return current.getData();
        }
        
        @Override
        public String getTraversalType() {
            return "Breadth-First Search (BFS)";
        }
    }
}

// ============= Использование =============

class TreeIteratorDemo {
    public static void main(String[] args) {
        // Создаем организационную структуру компании
        TreeNode<String> ceo = new TreeNode<>("CEO");
        
        TreeNode<String> cto = new TreeNode<>("CTO");
        TreeNode<String> cfo = new TreeNode<>("CFO");
        TreeNode<String> coo = new TreeNode<>("COO");
        
        ceo.addChild(cto);
        ceo.addChild(cfo);
        ceo.addChild(coo);
        
        TreeNode<String> devManager = new TreeNode<>("Dev Manager");
        TreeNode<String> qaManager = new TreeNode<>("QA Manager");
        cto.addChild(devManager);
        cto.addChild(qaManager);
        
        TreeNode<String> seniorDev = new TreeNode<>("Senior Developer");
        TreeNode<String> juniorDev = new TreeNode<>("Junior Developer");
        devManager.addChild(seniorDev);
        devManager.addChild(juniorDev);
        
        TreeNode<String> qaLead = new TreeNode<>("QA Lead");
        TreeNode<String> tester = new TreeNode<>("Tester");
        qaManager.addChild(qaLead);
        qaManager.addChild(tester);
        
        TreeNode<String> accountant = new TreeNode<>("Accountant");
        TreeNode<String> analyst = new TreeNode<>("Financial Analyst");
        cfo.addChild(accountant);
        cfo.addChild(analyst);
        
        Tree<String> orgChart = new Tree<>(ceo);
        
        System.out.println("=== Organization Chart Traversal ===\n");
        
        // DFS обход
        TreeIterator<String> dfs = orgChart.createDFSIterator();
        System.out.println("--- " + dfs.getTraversalType() + " ---");
        int position = 1;
        while (dfs.hasNext()) {
            System.out.println(position++ + ". " + dfs.next());
        }
        
        // BFS обход
        System.out.println("\n--- " + orgChart.createBFSIterator().getTraversalType() + " ---");
        TreeIterator<String> bfs = orgChart.createBFSIterator();
        position = 1;
        while (bfs.hasNext()) {
            System.out.println(position++ + ". " + bfs.next());
        }
        
        // Использование стандартного for-each (использует DFS)
        System.out.println("\n--- Using for-each (default DFS) ---");
        position = 1;
        for (String role : orgChart) {
            System.out.println(position++ + ". " + role);
        }
    }
}
```

### Пример 3: Итератор для Composite (обход каталогов)

Обход файловой системы с использованием паттерна Composite.

```java
// ============= Файловая система =============

// Базовый компонент
interface FileSystemComponent {
    String getName();
    long getSize();
    void display(String indent);
    boolean isDirectory();
}

// Файл (листовой элемент)
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
    public void display(String indent) {
        System.out.println(indent + "📄 " + name + " (" + size + " bytes)");
    }
    
    @Override
    public boolean isDirectory() {
        return false;
    }
}

// Директория (композитный элемент)
class Directory implements FileSystemComponent, Iterable<FileSystemComponent> {
    private String name;
    private List<FileSystemComponent> children = new ArrayList<>();
    
    public Directory(String name) {
        this.name = name;
    }
    
    public void add(FileSystemComponent component) {
        children.add(component);
    }
    
    public void remove(FileSystemComponent component) {
        children.remove(component);
    }
    
    public List<FileSystemComponent> getChildren() {
        return new ArrayList<>(children); // Защитная копия
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
    public void display(String indent) {
        System.out.println(indent + "📁 " + name + "/ (" + getSize() + " bytes total)");
        for (FileSystemComponent child : children) {
            child.display(indent + "  ");
        }
    }
    
    @Override
    public boolean isDirectory() {
        return true;
    }
    
    @Override
    public java.util.Iterator<FileSystemComponent> iterator() {
        return new FileSystemIterator(this);
    }
    
    // Рекурсивный итератор для обхода всех файлов
    public java.util.Iterator<FileSystemComponent> deepIterator() {
        return new DeepFileSystemIterator(this);
    }
}

// ============= Итератор для одного уровня =============

class FileSystemIterator implements java.util.Iterator<FileSystemComponent> {
    private List<FileSystemComponent> components;
    private int position = 0;
    
    public FileSystemIterator(Directory directory) {
        this.components = directory.getChildren();
    }
    
    @Override
    public boolean hasNext() {
        return position < components.size();
    }
    
    @Override
    public FileSystemComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return components.get(position++);
    }
}

// ============= Глубокий итератор (обход всех подкаталогов) =============

class DeepFileSystemIterator implements java.util.Iterator<FileSystemComponent> {
    private Stack<java.util.Iterator<FileSystemComponent>> stack = new Stack<>();
    private FileSystemComponent next;
    
    public DeepFileSystemIterator(Directory root) {
        stack.push(Collections.singletonList((FileSystemComponent) root).iterator());
        advance();
    }
    
    private void advance() {
        next = null;
        
        while (!stack.isEmpty() && next == null) {
            java.util.Iterator<FileSystemComponent> current = stack.peek();
            
            if (current.hasNext()) {
                FileSystemComponent component = current.next();
                next = component;
                
                // Если это директория, добавляем её детей в стек
                if (component.isDirectory()) {
                    Directory dir = (Directory) component;
                    if (!dir.getChildren().isEmpty()) {
                        stack.push(dir.getChildren().iterator());
                    }
                }
            } else {
                stack.pop();
            }
        }
    }
    
    @Override
    public boolean hasNext() {
        return next != null;
    }
    
    @Override
    public FileSystemComponent next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        
        FileSystemComponent result = next;
        advance();
        return result;
    }
}

// ============= Использование =============

class FileSystemIteratorDemo {
    public static void main(String[] args) {
        // Создаем файловую структуру
        Directory root = new Directory("project");
        
        Directory src = new Directory("src");
        Directory main = new Directory("main");
        Directory java = new Directory("java");
        Directory resources = new Directory("resources");
        
        root.add(src);
        src.add(main);
        main.add(java);
        main.add(resources);
        
        java.add(new File("Main.java", 1024));
        java.add(new File("Controller.java", 2048));
        java.add(new File("Service.java", 3072));
        
        resources.add(new File("application.properties", 512));
        resources.add(new File("logback.xml", 768));
        
        Directory test = new Directory("test");
        Directory testJava = new Directory("java");
        test.add(testJava);
        src.add(test);
        
        testJava.add(new File("MainTest.java", 1536));
        testJava.add(new File("ServiceTest.java", 2560));
        
        root.add(new File("pom.xml", 4096));
        root.add(new File("README.md", 2048));
        
        System.out.println("=== File System Structure ===\n");
        root.display("");
        
        // Итерация по содержимому корневой директории (один уровень)
        System.out.println("\n=== Root Directory Contents (shallow) ===");
        int count = 1;
        for (FileSystemComponent component : root) {
            System.out.println(count++ + ". " + component.getName() + 
                (component.isDirectory() ? "/" : ""));
        }
        
        // Глубокая итерация (все файлы и папки)
        System.out.println("\n=== All Files and Directories (deep) ===");
        java.util.Iterator<FileSystemComponent> deepIterator = root.deepIterator();
        count = 1;
        while (deepIterator.hasNext()) {
            FileSystemComponent component = deepIterator.next();
            String type = component.isDirectory() ? "DIR" : "FILE";
            System.out.println(count++ + ". [" + type + "] " + component.getName() + 
                " (" + component.getSize() + " bytes)");
        }
        
        // Подсчет статистики
        System.out.println("\n=== Statistics ===");
        deepIterator = root.deepIterator();
        long totalFiles = 0;
        long totalDirs = 0;
        long totalSize = 0;
        
        while (deepIterator.hasNext()) {
            FileSystemComponent component = deepIterator.next();
            if (component.isDirectory()) {
                totalDirs++;
            } else {
                totalFiles++;
                totalSize += component.getSize();
            }
        }
        
        System.out.println("Total directories: " + totalDirs);
        System.out.println("Total files: " + totalFiles);
        System.out.println("Total size: " + totalSize + " bytes");
    }
}
```

### Пример 4: Фильтрующий итератор

Итератор с возможностью фильтрации элементов.

```java
// ============= Модель данных =============

class Employee {
    private String name;
    private String department;
    private int salary;
    private int yearsOfService;
    
    public Employee(String name, String department, int salary, int yearsOfService) {
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.yearsOfService = yearsOfService;
    }
    
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public int getSalary() { return salary; }
    public int getYearsOfService() { return yearsOfService; }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - $%,d, %d years", 
            name, department, salary, yearsOfService);
    }
}

// ============= Предикаты для фильтрации =============

interface EmployeePredicate {
    boolean test(Employee employee);
}

class DepartmentPredicate implements EmployeePredicate {
    private String department;
    
    public DepartmentPredicate(String department) {
        this.department = department;
    }
    
    @Override
    public boolean test(Employee employee) {
        return employee.getDepartment().equals(department);
    }
}

class SalaryRangePredicate implements EmployeePredicate {
    private int minSalary;
    private int maxSalary;
    
    public SalaryRangePredicate(int minSalary, int maxSalary) {
        this.minSalary = minSalary;
        this.maxSalary = maxSalary;
    }
    
    @Override
    public boolean test(Employee employee) {
        int salary = employee.getSalary();
        return salary >= minSalary && salary <= maxSalary;
    }
}

class ExperiencePredicate implements EmployeePredicate {
    private int minYears;
    
    public ExperiencePredicate(int minYears) {
        this.minYears = minYears;
    }
    
    @Override
    public boolean test(Employee employee) {
        return employee.getYearsOfService() >= minYears;
    }
}

// Комбинированный предикат
class CompositePredicate implements EmployeePredicate {
    private List<EmployeePredicate> predicates;
    
    public CompositePredicate(EmployeePredicate... predicates) {
        this.predicates = Arrays.asList(predicates);
    }
    
    @Override
    public boolean test(Employee employee) {
        return predicates.stream().allMatch(p -> p.test(employee));
    }
}

// ============= Фильтрующий итератор =============

class FilteringIterator implements java.util.Iterator<Employee> {
    private java.util.Iterator<Employee> iterator;
    private EmployeePredicate predicate;
    private Employee nextElement;
    private boolean hasNext;
    
    public FilteringIterator(List<Employee> employees, EmployeePredicate predicate) {
        this.iterator = employees.iterator();
        this.predicate = predicate;
        advance();
    }
    
    private void advance() {
        hasNext = false;
        
        while (iterator.hasNext()) {
            Employee employee = iterator.next();
            if (predicate.test(employee)) {
                nextElement = employee;
                hasNext = true;
                return;
            }
        }
    }
    
    @Override
    public boolean hasNext() {
        return hasNext;
    }
    
    @Override
    public Employee next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        
        Employee result = nextElement;
        advance();
        return result;
    }
}

// ============= Коллекция сотрудников =============

class EmployeeCollection implements Iterable<Employee> {
    private List<Employee> employees = new ArrayList<>();
    
    public void addEmployee(Employee employee) {
        employees.add(employee);
    }
    
    public int size() {
        return employees.size();
    }
    
    @Override
    public java.util.Iterator<Employee> iterator() {
        return employees.iterator();
    }
    
    // Итератор с фильтром
    public java.util.Iterator<Employee> iterator(EmployeePredicate predicate) {
        return new FilteringIterator(employees, predicate);
    }
    
    // Удобные методы для создания фильтрованных итераторов
    public java.util.Iterator<Employee> byDepartment(String department) {
        return iterator(new DepartmentPredicate(department));
    }
    
    public java.util.Iterator<Employee> bySalaryRange(int min, int max) {
        return iterator(new SalaryRangePredicate(min, max));
    }
    
    public java.util.Iterator<Employee> byExperience(int minYears) {
        return iterator(new ExperiencePredicate(minYears));
    }
}

// ============= Использование =============

class FilteringIteratorDemo {
    public static void main(String[] args) {
        EmployeeCollection company = new EmployeeCollection();
        
        company.addEmployee(new Employee("Alice Johnson", "Engineering", 120000, 5));
        company.addEmployee(new Employee("Bob Smith", "Engineering", 95000, 3));
        company.addEmployee(new Employee("Carol White", "Sales", 80000, 7));
        company.addEmployee(new Employee("David Brown", "Sales", 70000, 2));
        company.addEmployee(new Employee("Eve Davis", "Engineering", 110000, 8));
        company.addEmployee(new Employee("Frank Miller", "HR", 65000, 4));
        company.addEmployee(new Employee("Grace Wilson", "Engineering", 85000, 1));
        company.addEmployee(new Employee("Henry Moore", "Sales", 90000, 6));
        
        System.out.println("=== Company Employees (" + company.size() + " total) ===\n");
        
        // Все сотрудники
        System.out.println("--- All Employees ---");
        printEmployees(company.iterator());
        
        // Только инженеры
        System.out.println("\n--- Engineering Department ---");
        printEmployees(company.byDepartment("Engineering"));
        
        // Зарплата от $80k до $100k
        System.out.println("\n--- Salary Range: $80,000 - $100,000 ---");
        printEmployees(company.bySalaryRange(80000, 100000));
        
        // Опыт 5+ лет
        System.out.println("\n--- 5+ Years of Experience ---");
        printEmployees(company.byExperience(5));
        
        // Комбинированный фильтр: инженеры с зарплатой $100k+ и опытом 5+ лет
        System.out.println("\n--- Senior Engineers (Salary $100k+, 5+ years) ---");
        EmployeePredicate seniorEngineer = new CompositePredicate(
            new DepartmentPredicate("Engineering"),
            new SalaryRangePredicate(100000, Integer.MAX_VALUE),
            new ExperiencePredicate(5)
        );
        printEmployees(company.iterator(seniorEngineer));
    }
    
    private static void printEmployees(java.util.Iterator<Employee> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            System.out.println(++count + ". " + iterator.next());
        }
        if (count == 0) {
            System.out.println("No employees match the criteria");
        }
    }
}
```

## Внутренние vs Внешние итераторы

### Внешний итератор (External Iterator)

Клиент контролирует итерацию — классический подход в Java.

```java
// Клиент управляет итерацией
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    String item = iterator.next();
    System.out.println(item);
}
```

**Преимущества:**
- Полный контроль над процессом итерации
- Можно прервать итерацию в любой момент
- Можно иметь несколько одновременных итераций
- Гибкость в обработке элементов

**Недостатки:**
- Более многословный код
- Клиент должен управлять состоянием итерации

### Внутренний итератор (Internal Iterator)

Коллекция сама управляет итерацией — подход функционального программирования.

```java
// Коллекция управляет итерацией
list.forEach(item -> System.out.println(item));

// Или с Stream API
list.stream()
    .filter(s -> s.length() > 5)
    .map(String::toUpperCase)
    .forEach(System.out::println);
```

**Преимущества:**
- Более лаконичный код
- Лучше для параллелизма
- Декларативный стиль
- Меньше вероятность ошибок

**Недостатки:**
- Меньше контроля над процессом
- Сложнее прервать итерацию
- Только одна операция за раз

### Сравнение

```java
class IteratorComparisonDemo {
    public static void main(String[] args) {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        // Внешний итератор: найти первое четное число
        System.out.println("=== External Iterator ===");
        Iterator<Integer> iterator = numbers.iterator();
        while (iterator.hasNext()) {
            Integer num = iterator.next();
            if (num % 2 == 0) {
                System.out.println("First even number: " + num);
                break; // Можем прервать
            }
        }
        
        // Внутренний итератор: вывести все четные числа
        System.out.println("\n=== Internal Iterator ===");
        numbers.stream()
            .filter(n -> n % 2 == 0)
            .forEach(n -> System.out.println("Even number: " + n));
        
        // Внешний: можем иметь несколько одновременных итераций
        System.out.println("\n=== Multiple Concurrent Iterations ===");
        Iterator<Integer> iter1 = numbers.iterator();
        Iterator<Integer> iter2 = numbers.iterator();
        
        System.out.print("Iter1: ");
        for (int i = 0; i < 3 && iter1.hasNext(); i++) {
            System.out.print(iter1.next() + " ");
        }
        
        System.out.print("\nIter2: ");
        for (int i = 0; i < 5 && iter2.hasNext(); i++) {
            System.out.print(iter2.next() + " ");
        }
        
        System.out.print("\nIter1 continued: ");
        while (iter1.hasNext()) {
            System.out.print(iter1.next() + " ");
        }
        System.out.println();
    }
}
```

## Примеры из JDK

### 1. java.util.Iterator

```java
// Стандартный итератор для коллекций
List<String> list = Arrays.asList("A", "B", "C");
Iterator<String> iterator = list.iterator();

while (iterator.hasNext()) {
    String element = iterator.next();
    System.out.println(element);
}

// Удаление во время итерации
Iterator<String> iter = list.iterator();
while (iter.hasNext()) {
    String elem = iter.next();
    if (elem.equals("B")) {
        iter.remove(); // Безопасное удаление
    }
}
```

### 2. java.lang.Iterable и for-each

```java
// Iterable позволяет использовать for-each
class MyCollection<T> implements Iterable<T> {
    private List<T> items = new ArrayList<>();
    
    @Override
    public Iterator<T> iterator() {
        return items.iterator();
    }
}

MyCollection<String> collection = new MyCollection<>();
for (String item : collection) { // for-each использует iterator()
    System.out.println(item);
}
```

### 3. Stream API

```java
// Stream API - внутренний итератор
List<String> names = Arrays.asList("Alice", "Bob", "Charlie");

names.stream()
    .filter(name -> name.length() > 3)
    .map(String::toUpperCase)
    .forEach(System.out::println);

// Spliterator - для параллельных стримов
Spliterator<String> spliterator = names.spliterator();
```

### 4. ListIterator

```java
// Двунаправленный итератор для списков
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
ListIterator<String> listIter = list.listIterator();

// Вперед
while (listIter.hasNext()) {
    System.out.println(listIter.next());
}

// Назад
while (listIter.hasPrevious()) {
    System.out.println(listIter.previous());
}

// Модификация во время итерации
listIter = list.listIterator();
while (listIter.hasNext()) {
    String elem = listIter.next();
    if (elem.equals("B")) {
        listIter.set("B_MODIFIED"); // Изменение элемента
        listIter.add("B_NEW");      // Добавление элемента
    }
}
```

### 5. Scanner

```java
// Scanner - итератор для ввода
Scanner scanner = new Scanner("one two three");

while (scanner.hasNext()) {
    String word = scanner.next();
    System.out.println(word);
}
```

## Преимущества и недостатки

### Преимущества

| Преимущество | Описание |
|--------------|----------|
| **Инкапсуляция** | Скрывает внутреннюю структуру коллекции |
| **Единый интерфейс** | Одинаковый способ обхода для разных коллекций |
| **Множественные обходы** | Возможность нескольких одновременных обходов одной коллекции |
| **Различные стратегии** | Можно реализовать разные способы обхода (DFS, BFS, фильтрация) |
| **Безопасность** | Контролируемый доступ к элементам коллекции |

### Недостатки

| Недостаток | Описание |
|------------|----------|
| **Производительность** | Дополнительный уровень абстракции может снизить производительность |
| **Сложность** | Увеличивает количество классов в системе |
| **Модификация** | Проблемы при модификации коллекции во время итерации (ConcurrentModificationException) |
| **Состояние** | Итератор хранит состояние, что требует памяти |

## Вопросы на собеседовании

### 1. Что такое паттерн Iterator?

**Ответ:** Iterator — поведенческий паттерн проектирования, который предоставляет способ последовательного доступа ко всем элементам составного объекта, не раскрывая его внутреннего представления.

Основные компоненты:
- **Iterator**: интерфейс с методами hasNext(), next(), remove()
- **ConcreteIterator**: реализация итератора для конкретной коллекции
- **Aggregate**: интерфейс коллекции с методом createIterator()
- **ConcreteAggregate**: конкретная коллекция

### 2. В чем разница между Iterator и Iterable?

**Ответ:**
- **Iterable**: Интерфейс, который говорит, что объект можно итерировать. Содержит метод `iterator()`, возвращающий итератор. Позволяет использовать for-each цикл.
- **Iterator**: Интерфейс для самого процесса итерации. Содержит методы `hasNext()`, `next()`, `remove()`.

```java
// Iterable - "может быть проитерирован"
public interface Iterable<T> {
    Iterator<T> iterator();
}

// Iterator - "выполняет итерацию"
public interface Iterator<T> {
    boolean hasNext();
    T next();
    void remove();
}

// Использование
List<String> list = Arrays.asList("A", "B", "C");
// list - Iterable
// list.iterator() - Iterator

for (String item : list) { // работает, т.к. List implements Iterable
    System.out.println(item);
}
```

### 3. Что такое fail-fast и fail-safe итераторы?

**Ответ:**

**Fail-fast итератор:**
- Выбрасывает `ConcurrentModificationException` при модификации коллекции во время итерации
- Используется в большинстве коллекций из `java.util` (ArrayList, HashMap)
- Работает через счетчик модификаций (`modCount`)

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
Iterator<String> iter = list.iterator();

while (iter.hasNext()) {
    String item = iter.next();
    list.remove(item); // ConcurrentModificationException!
}

// Правильно: использовать iterator.remove()
while (iter.hasNext()) {
    String item = iter.next();
    iter.remove(); // OK
}
```

**Fail-safe итератор:**
- Работает с копией коллекции
- Не выбрасывает исключение при модификации
- Используется в concurrent коллекциях (`CopyOnWriteArrayList`, `ConcurrentHashMap`)
- Может не видеть последние изменения

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(
    Arrays.asList("A", "B", "C")
);

for (String item : list) {
    list.remove(item); // OK, работает с копией
}
```

### 4. Как реализовать свой итератор?

**Ответ:** Необходимо реализовать интерфейс `Iterator` и `Iterable`:

```java
class MyCollection<T> implements Iterable<T> {
    private T[] items;
    private int size;
    
    @SuppressWarnings("unchecked")
    public MyCollection(int capacity) {
        items = (T[]) new Object[capacity];
        size = 0;
    }
    
    public void add(T item) {
        items[size++] = item;
    }
    
    @Override
    public Iterator<T> iterator() {
        return new MyIterator();
    }
    
    // Вложенный класс итератора
    private class MyIterator implements Iterator<T> {
        private int currentIndex = 0;
        
        @Override
        public boolean hasNext() {
            return currentIndex < size;
        }
        
        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return items[currentIndex++];
        }
        
        @Override
        public void remove() {
            if (currentIndex <= 0) {
                throw new IllegalStateException();
            }
            
            // Сдвигаем элементы влево
            for (int i = currentIndex - 1; i < size - 1; i++) {
                items[i] = items[i + 1];
            }
            size--;
            currentIndex--;
        }
    }
}
```

### 5. В чем разница между внешним и внутренним итератором?

**Ответ:**

**Внешний итератор (External):**
- Клиент контролирует итерацию
- Явное управление через hasNext()/next()
- Можно прервать в любой момент
- Традиционный подход в Java

```java
Iterator<String> iter = list.iterator();
while (iter.hasNext()) {
    String item = iter.next();
    if (item.equals("stop")) {
        break; // Можем прервать
    }
    process(item);
}
```

**Внутренний итератор (Internal):**
- Коллекция контролирует итерацию
- Используется через forEach() или Stream API
- Более функциональный стиль
- Лучше для параллелизма

```java
list.forEach(item -> process(item));

// Или
list.stream()
    .filter(item -> !item.equals("stop"))
    .forEach(this::process);
```

### 6. Можно ли модифицировать коллекцию во время итерации?

**Ответ:** Зависит от способа модификации:

**Безопасно через iterator.remove():**
```java
Iterator<String> iter = list.iterator();
while (iter.hasNext()) {
    String item = iter.next();
    if (shouldRemove(item)) {
        iter.remove(); // OK
    }
}
```

**Небезопасно через методы коллекции:**
```java
for (String item : list) {
    if (shouldRemove(item)) {
        list.remove(item); // ConcurrentModificationException!
    }
}
```

**Решения:**
1. Использовать `iterator.remove()`
2. Собрать элементы для удаления отдельно:
```java
List<String> toRemove = new ArrayList<>();
for (String item : list) {
    if (shouldRemove(item)) {
        toRemove.add(item);
    }
}
list.removeAll(toRemove);
```

3. Использовать `removeIf()`:
```java
list.removeIf(item -> shouldRemove(item));
```

4. Использовать concurrent коллекции

### 7. Как работает двунаправленный итератор (ListIterator)?

**Ответ:** `ListIterator` расширяет `Iterator` и добавляет возможность двунаправленного обхода:

```java
List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C", "D"));
ListIterator<String> iter = list.listIterator();

// Вперед
while (iter.hasNext()) {
    System.out.println("Forward: " + iter.next());
    System.out.println("Current index: " + iter.nextIndex());
}

// Назад
while (iter.hasPrevious()) {
    System.out.println("Backward: " + iter.previous());
    System.out.println("Current index: " + iter.previousIndex());
}

// Модификация
iter = list.listIterator();
while (iter.hasNext()) {
    String item = iter.next();
    if (item.equals("B")) {
        iter.set("B_MODIFIED");  // Изменить текущий
        iter.add("B_INSERTED");  // Вставить после текущего
    }
}
```

Методы ListIterator:
- `hasNext()`, `next()`, `hasPrevious()`, `previous()` - навигация
- `nextIndex()`, `previousIndex()` - индексы
- `set(E e)` - изменить текущий элемент
- `add(E e)` - вставить элемент
- `remove()` - удалить текущий элемент

### 8. Как реализовать фильтрующий итератор?

**Ответ:** Создать итератор-обертку с предикатом:

```java
class FilterIterator<T> implements Iterator<T> {
    private Iterator<T> iterator;
    private Predicate<T> predicate;
    private T nextElement;
    private boolean hasNext;
    
    public FilterIterator(Iterator<T> iterator, Predicate<T> predicate) {
        this.iterator = iterator;
        this.predicate = predicate;
        findNext();
    }
    
    private void findNext() {
        hasNext = false;
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (predicate.test(element)) {
                nextElement = element;
                hasNext = true;
                return;
            }
        }
    }
    
    @Override
    public boolean hasNext() {
        return hasNext;
    }
    
    @Override
    public T next() {
        if (!hasNext) {
            throw new NoSuchElementException();
        }
        T result = nextElement;
        findNext();
        return result;
    }
}

// Использование
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
Iterator<Integer> evenNumbers = new FilterIterator<>(
    numbers.iterator(),
    n -> n % 2 == 0
);

while (evenNumbers.hasNext()) {
    System.out.println(evenNumbers.next()); // 2, 4, 6, 8, 10
}
```

### 9. Как итератор используется в паттерне Composite?

**Ответ:** Iterator позволяет единообразно обходить составные структуры:

```java
interface Component {
    String getName();
    Iterator<Component> createIterator();
}

class Leaf implements Component {
    private String name;
    
    public Leaf(String name) {
        this.name = name;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Iterator<Component> createIterator() {
        return Collections.emptyIterator(); // Нет детей
    }
}

class Composite implements Component {
    private String name;
    private List<Component> children = new ArrayList<>();
    
    public Composite(String name) {
        this.name = name;
    }
    
    public void add(Component component) {
        children.add(component);
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public Iterator<Component> createIterator() {
        return new CompositeIterator(children.iterator());
    }
    
    // Рекурсивный итератор
    private class CompositeIterator implements Iterator<Component> {
        private Stack<Iterator<Component>> stack = new Stack<>();
        
        public CompositeIterator(Iterator<Component> iterator) {
            stack.push(iterator);
        }
        
        @Override
        public boolean hasNext() {
            if (stack.isEmpty()) {
                return false;
            }
            
            Iterator<Component> iterator = stack.peek();
            if (!iterator.hasNext()) {
                stack.pop();
                return hasNext(); // Рекурсивная проверка
            }
            
            return true;
        }
        
        @Override
        public Component next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            
            Iterator<Component> iterator = stack.peek();
            Component component = iterator.next();
            
            // Если компонент составной, добавляем его итератор в стек
            Iterator<Component> childIterator = component.createIterator();
            if (childIterator.hasNext()) {
                stack.push(childIterator);
            }
            
            return component;
        }
    }
}
```

### 10. Как тестировать итераторы?

**Ответ:** Основные тестовые сценарии:

```java
@Test
public void testBasicIteration() {
    MyCollection<String> collection = new MyCollection<>();
    collection.add("A");
    collection.add("B");
    collection.add("C");
    
    Iterator<String> iter = collection.iterator();
    
    assertTrue(iter.hasNext());
    assertEquals("A", iter.next());
    assertTrue(iter.hasNext());
    assertEquals("B", iter.next());
    assertTrue(iter.hasNext());
    assertEquals("C", iter.next());
    assertFalse(iter.hasNext());
}

@Test(expected = NoSuchElementException.class)
public void testNextOnEmptyIterator() {
    MyCollection<String> collection = new MyCollection<>();
    Iterator<String> iter = collection.iterator();
    iter.next(); // Должно выбросить исключение
}

@Test
public void testRemove() {
    List<String> list = new ArrayList<>(Arrays.asList("A", "B", "C"));
    Iterator<String> iter = list.iterator();
    
    iter.next(); // A
    iter.next(); // B
    iter.remove(); // Удаляем B
    
    assertEquals(2, list.size());
    assertFalse(list.contains("B"));
}

@Test
public void testMultipleConcurrentIterators() {
    MyCollection<String> collection = new MyCollection<>();
    collection.add("A");
    collection.add("B");
    
    Iterator<String> iter1 = collection.iterator();
    Iterator<String> iter2 = collection.iterator();
    
    assertEquals("A", iter1.next());
    assertEquals("A", iter2.next());
    assertEquals("B", iter1.next());
    assertEquals("B", iter2.next());
}

@Test
public void testFilteringIterator() {
    List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
    Iterator<Integer> evenIter = new FilterIterator<>(
        numbers.iterator(),
        n -> n % 2 == 0
    );
    
    List<Integer> result = new ArrayList<>();
    evenIter.forEachRemaining(result::add);
    
    assertEquals(Arrays.asList(2, 4), result);
}
```

Важно тестировать:
- Базовую итерацию (hasNext/next)
- Пустые коллекции
- Удаление элементов
- Множественные итераторы
- Граничные условия
- Фильтрацию и специальную логику
