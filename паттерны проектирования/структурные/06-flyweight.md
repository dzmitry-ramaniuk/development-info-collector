# Flyweight (Приспособленец, Легковес)

Flyweight — структурный паттерн проектирования, который позволяет вмещать большее количество объектов в отведённую оперативную память за счёт экономного разделения общего состояния объектов между собой, вместо хранения одинаковых данных в каждом объекте.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Текстовый редактор с рендерингом символов](#текстовый-редактор-с-рендерингом-символов)
   - [Лес с деревьями](#лес-с-деревьями)
   - [Система частиц в игре](#система-частиц-в-игре)
   - [Фабрика иконок](#фабрика-иконок)
5. [Примеры в JDK](#примеры-в-jdk)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Flyweight используется когда:
- Приложение использует большое количество объектов
- Расходы на хранение объектов высоки из-за большого количества
- Большую часть состояния объектов можно вынести за пределы объекта
- Множество объектов можно заменить относительно небольшим количеством разделяемых объектов

**Типичные примеры использования:**
- Рендеринг текста (символы в документе)
- Игровые объекты (деревья, частицы, юниты)
- Графические примитивы в редакторах
- Кеширование иммутабельных объектов
- Пулы объектов

## Проблема, которую решает

### Проблема: Расточительное использование памяти

```java
// Без Flyweight - каждое дерево хранит все данные

class Tree {
    private String name;           // "Сосна" - 10 байт
    private String color;          // "Зеленый" - 14 байт
    private String texture;        // Изображение - 100KB
    private int x;                 // 4 байта
    private int y;                 // 4 байта
    
    public Tree(String name, String color, String texture, int x, int y) {
        this.name = name;
        this.color = color;
        this.texture = texture;
        this.x = x;
        this.y = y;
    }
    
    public void draw() {
        System.out.println("Рисую " + name + " в позиции (" + x + ", " + y + ")");
    }
}

class Forest {
    private List<Tree> trees = new ArrayList<>();
    
    public void plantTree(int x, int y) {
        // Каждое дерево создает копию name, color, texture!
        Tree tree = new Tree("Сосна", "Зеленый", loadTexture("pine.png"), x, y);
        trees.add(tree);
    }
}

// Проблема: для 1,000,000 деревьев:
// Память = 1,000,000 * (100KB + 28 байт) ≈ 100GB!
// Хотя у всех сосен одинаковые name, color, texture
```

**Проблемы:**
- Огромное потребление памяти
- Дублирование одинаковых данных
- Медленное создание объектов
- Проблемы с производительностью

### Решение: Flyweight

Разделить состояние на внутреннее (intrinsic) и внешнее (extrinsic).

```java
// Flyweight - хранит только внутреннее состояние (общее для всех)
class TreeType {
    private String name;      // Внутреннее состояние
    private String color;     // (intrinsic)
    private String texture;   // Разделяется между объектами
    
    public TreeType(String name, String color, String texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
    }
    
    public void draw(int x, int y) {  // Внешнее состояние передается извне
        System.out.println("Рисую " + name + " в позиции (" + x + ", " + y + ")");
    }
}

// Контекст - хранит внешнее состояние (уникальное для каждого объекта)
class Tree {
    private int x;              // Внешнее состояние (extrinsic)
    private int y;              // Уникально для каждого дерева
    private TreeType type;      // Ссылка на разделяемый Flyweight
    
    public Tree(int x, int y, TreeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public void draw() {
        type.draw(x, y);
    }
}

// Фабрика Flyweight - управляет разделяемыми объектами
class TreeFactory {
    private static Map<String, TreeType> treeTypes = new HashMap<>();
    
    public static TreeType getTreeType(String name, String color, String texture) {
        String key = name + "_" + color;
        TreeType type = treeTypes.get(key);
        
        if (type == null) {
            type = new TreeType(name, color, texture);
            treeTypes.put(key, type);
            System.out.println("Создан новый тип дерева: " + name);
        }
        return type;
    }
}

// Теперь для 1,000,000 сосен:
// Память = 1 * 100KB + 1,000,000 * (8 + 8 + 8) = 100KB + 24MB ≈ 24MB
// Экономия: ~4000 раз!
```

## Структура паттерна

```java
// Flyweight - интерфейс приспособленца
interface Flyweight {
    void operation(String extrinsicState);
}

// ConcreteFlyweight - конкретный приспособленец
class ConcreteFlyweight implements Flyweight {
    private String intrinsicState;  // Внутреннее состояние (разделяемое)
    
    public ConcreteFlyweight(String intrinsicState) {
        this.intrinsicState = intrinsicState;
    }
    
    @Override
    public void operation(String extrinsicState) {
        System.out.println("Intrinsic: " + intrinsicState + 
                         ", Extrinsic: " + extrinsicState);
    }
}

// FlyweightFactory - фабрика приспособленцев
class FlyweightFactory {
    private Map<String, Flyweight> flyweights = new HashMap<>();
    
    public Flyweight getFlyweight(String key) {
        Flyweight flyweight = flyweights.get(key);
        
        if (flyweight == null) {
            flyweight = new ConcreteFlyweight(key);
            flyweights.put(key, flyweight);
        }
        
        return flyweight;
    }
    
    public int getTotalFlyweights() {
        return flyweights.size();
    }
}

// Client
class FlyweightDemo {
    public static void main(String[] args) {
        FlyweightFactory factory = new FlyweightFactory();
        
        // Получаем приспособленцы
        Flyweight flyweight1 = factory.getFlyweight("A");
        Flyweight flyweight2 = factory.getFlyweight("B");
        Flyweight flyweight3 = factory.getFlyweight("A");  // Повторное использование
        
        // flyweight1 и flyweight3 - это один и тот же объект!
        System.out.println("flyweight1 == flyweight3: " + (flyweight1 == flyweight3));
        
        // Используем с внешним состоянием
        flyweight1.operation("External State 1");
        flyweight3.operation("External State 2");
        
        System.out.println("Всего создано flyweight: " + factory.getTotalFlyweights());
    }
}
```

## Реализация

### Текстовый редактор с рендерингом символов

Пример показывает, как Flyweight экономит память при рендеринге текста.

```java
import java.util.*;

// Flyweight - символ (внутреннее состояние)
class CharacterStyle {
    private final char character;
    private final String fontFamily;
    private final int fontSize;
    private final String color;
    
    public CharacterStyle(char character, String fontFamily, int fontSize, String color) {
        this.character = character;
        this.fontFamily = fontFamily;
        this.fontSize = fontSize;
        this.color = color;
    }
    
    public void render(int x, int y) {
        System.out.println("Рисую '" + character + "' в позиции (" + x + ", " + y + ") " +
                         "шрифтом " + fontFamily + ", размер " + fontSize + ", цвет " + color);
    }
    
    public char getCharacter() {
        return character;
    }
}

// Фабрика символов
class CharacterStyleFactory {
    private static final Map<String, CharacterStyle> styles = new HashMap<>();
    
    public static CharacterStyle getCharacterStyle(char character, String fontFamily, 
                                                   int fontSize, String color) {
        String key = character + "_" + fontFamily + "_" + fontSize + "_" + color;
        CharacterStyle style = styles.get(key);
        
        if (style == null) {
            style = new CharacterStyle(character, fontFamily, fontSize, color);
            styles.put(key, style);
            System.out.println("Создан новый стиль для '" + character + "'");
        }
        
        return style;
    }
    
    public static int getTotalStyles() {
        return styles.size();
    }
    
    public static void printStatistics() {
        System.out.println("\n=== Статистика стилей символов ===");
        System.out.println("Всего уникальных стилей: " + styles.size());
        System.out.println("Занято памяти: ~" + (styles.size() * 64) + " байт");
    }
}

// Контекст - позиционированный символ (внешнее состояние)
class PositionedCharacter {
    private final int x;  // Внешнее состояние
    private final int y;
    private final CharacterStyle style;  // Ссылка на Flyweight
    
    public PositionedCharacter(int x, int y, CharacterStyle style) {
        this.x = x;
        this.y = y;
        this.style = style;
    }
    
    public void render() {
        style.render(x, y);
    }
}

// Текстовый редактор
class TextEditor {
    private List<PositionedCharacter> characters = new ArrayList<>();
    
    public void insertCharacter(char c, int x, int y, String font, int size, String color) {
        CharacterStyle style = CharacterStyleFactory.getCharacterStyle(c, font, size, color);
        PositionedCharacter posChar = new PositionedCharacter(x, y, style);
        characters.add(posChar);
    }
    
    public void insertText(String text, int startX, int startY, String font, 
                          int size, String color) {
        int x = startX;
        int y = startY;
        
        for (char c : text.toCharArray()) {
            if (c == '\n') {
                x = startX;
                y += size;
            } else if (c == ' ') {
                x += size / 2;
            } else {
                insertCharacter(c, x, y, font, size, color);
                x += size;
            }
        }
    }
    
    public void render() {
        System.out.println("\n=== Рендеринг документа ===");
        for (PositionedCharacter character : characters) {
            character.render();
        }
    }
    
    public void printMemoryUsage() {
        long charactersMemory = characters.size() * 24; // x, y, ссылка на style
        System.out.println("\n=== Использование памяти ===");
        System.out.println("Символов в документе: " + characters.size());
        System.out.println("Память для позиций: " + charactersMemory + " байт");
        System.out.println("Без Flyweight потребовалось бы: " + (characters.size() * 64) + " байт");
        System.out.println("Экономия: " + ((characters.size() * 64 - charactersMemory) / 1024.0) + " KB");
    }
}

// Демонстрация
class TextEditorDemo {
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        
        // Вставляем текст
        editor.insertText("Hello", 0, 0, "Arial", 12, "Black");
        editor.insertText("World", 100, 0, "Arial", 12, "Black");
        editor.insertText("Java", 0, 20, "Times New Roman", 14, "Blue");
        
        // Рендерим (только первые 3 символа для краткости)
        System.out.println("\n=== Первые символы ===");
        editor.insertCharacter('H', 0, 0, "Arial", 12, "Black");
        editor.insertCharacter('e', 12, 0, "Arial", 12, "Black");
        editor.insertCharacter('l', 24, 0, "Arial", 12, "Black");
        
        // Статистика
        editor.printMemoryUsage();
        CharacterStyleFactory.printStatistics();
    }
}
```

### Лес с деревьями

Классический пример Flyweight для игры или симуляции леса.

```java
import java.util.*;

// Flyweight - тип дерева (внутреннее состояние)
class TreeType {
    private final String name;
    private final String color;
    private final String texture;
    
    public TreeType(String name, String color, String texture) {
        this.name = name;
        this.color = color;
        this.texture = texture;
        System.out.println("Создание нового типа дерева: " + name + " (" + color + ")");
    }
    
    public void draw(int x, int y) {
        System.out.println("Рисую " + name + " (" + color + ") в позиции (" + x + ", " + y + ")");
    }
    
    public String getName() {
        return name;
    }
}

// Фабрика типов деревьев
class TreeFactory {
    private static final Map<String, TreeType> treeTypes = new HashMap<>();
    
    public static TreeType getTreeType(String name, String color, String texture) {
        String key = name + "_" + color;
        TreeType type = treeTypes.get(key);
        
        if (type == null) {
            type = new TreeType(name, color, texture);
            treeTypes.put(key, type);
        }
        
        return type;
    }
    
    public static int getTreeTypeCount() {
        return treeTypes.size();
    }
}

// Контекст - дерево в конкретной позиции (внешнее состояние)
class Tree {
    private final int x;
    private final int y;
    private final TreeType type;
    
    public Tree(int x, int y, TreeType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
    
    public void draw() {
        type.draw(x, y);
    }
}

// Лес - управляет деревьями
class Forest {
    private List<Tree> trees = new ArrayList<>();
    
    public void plantTree(int x, int y, String name, String color, String texture) {
        TreeType type = TreeFactory.getTreeType(name, color, texture);
        Tree tree = new Tree(x, y, type);
        trees.add(tree);
    }
    
    public void draw() {
        System.out.println("\n=== Рисование леса ===");
        for (Tree tree : trees) {
            tree.draw();
        }
    }
    
    public void printStatistics() {
        System.out.println("\n=== Статистика леса ===");
        System.out.println("Всего деревьев: " + trees.size());
        System.out.println("Уникальных типов деревьев: " + TreeFactory.getTreeTypeCount());
        
        long withoutFlyweight = trees.size() * 1024;  // 1KB на дерево с текстурой
        long withFlyweight = TreeFactory.getTreeTypeCount() * 1024 + trees.size() * 24;
        
        System.out.println("Память без Flyweight: " + withoutFlyweight / 1024 + " KB");
        System.out.println("Память с Flyweight: " + withFlyweight / 1024 + " KB");
        System.out.println("Экономия: " + ((withoutFlyweight - withFlyweight) * 100 / withoutFlyweight) + "%");
    }
}

// Демонстрация
class ForestDemo {
    public static void main(String[] args) {
        Forest forest = new Forest();
        
        // Сажаем много деревьев одного типа
        System.out.println("=== Посадка деревьев ===");
        for (int i = 0; i < 1000; i++) {
            int x = (int) (Math.random() * 10000);
            int y = (int) (Math.random() * 10000);
            
            if (i % 3 == 0) {
                forest.plantTree(x, y, "Сосна", "Зеленая", "pine.png");
            } else if (i % 3 == 1) {
                forest.plantTree(x, y, "Береза", "Белая", "birch.png");
            } else {
                forest.plantTree(x, y, "Дуб", "Коричневый", "oak.png");
            }
        }
        
        // Статистика
        forest.printStatistics();
        
        // Рисуем первые 5 деревьев для демонстрации
        System.out.println("\n=== Первые 5 деревьев ===");
        Forest smallForest = new Forest();
        smallForest.plantTree(10, 20, "Сосна", "Зеленая", "pine.png");
        smallForest.plantTree(50, 30, "Береза", "Белая", "birch.png");
        smallForest.plantTree(100, 40, "Дуб", "Коричневый", "oak.png");
        smallForest.plantTree(150, 50, "Сосна", "Зеленая", "pine.png");
        smallForest.plantTree(200, 60, "Береза", "Белая", "birch.png");
        smallForest.draw();
    }
}
```

### Система частиц в игре

Пример использования Flyweight для эффективного рендеринга тысяч частиц.

```java
import java.util.*;

// Flyweight - тип частицы (внутреннее состояние)
class ParticleType {
    private final String name;
    private final String color;
    private final String sprite;      // Изображение частицы
    private final double mass;
    private final double friction;
    
    public ParticleType(String name, String color, String sprite, 
                       double mass, double friction) {
        this.name = name;
        this.color = color;
        this.sprite = sprite;
        this.mass = mass;
        this.friction = friction;
    }
    
    public void draw(double x, double y, double velocityX, double velocityY) {
        System.out.printf("%s частица в (%.1f, %.1f), скорость (%.1f, %.1f)%n",
                         name, x, y, velocityX, velocityY);
    }
    
    public void move(Particle particle, double deltaTime) {
        // Физика частицы с учетом массы и трения
        particle.velocityX *= (1 - friction * deltaTime);
        particle.velocityY *= (1 - friction * deltaTime);
        
        particle.x += particle.velocityX * deltaTime;
        particle.y += particle.velocityY * deltaTime;
    }
}

// Фабрика типов частиц
class ParticleTypeFactory {
    private static final Map<String, ParticleType> particleTypes = new HashMap<>();
    
    static {
        // Предопределенные типы частиц
        particleTypes.put("fire", new ParticleType(
            "Огонь", "Оранжевый", "fire.png", 0.5, 0.1));
        particleTypes.put("smoke", new ParticleType(
            "Дым", "Серый", "smoke.png", 0.3, 0.05));
        particleTypes.put("spark", new ParticleType(
            "Искра", "Желтый", "spark.png", 0.2, 0.15));
        particleTypes.put("water", new ParticleType(
            "Вода", "Синий", "water.png", 1.0, 0.02));
    }
    
    public static ParticleType getParticleType(String type) {
        return particleTypes.get(type);
    }
    
    public static int getTypeCount() {
        return particleTypes.size();
    }
}

// Контекст - частица (внешнее состояние)
class Particle {
    double x, y;              // Позиция (внешнее состояние)
    double velocityX, velocityY;  // Скорость
    private final ParticleType type;  // Ссылка на Flyweight
    
    public Particle(double x, double y, double velocityX, double velocityY, 
                   ParticleType type) {
        this.x = x;
        this.y = y;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.type = type;
    }
    
    public void update(double deltaTime) {
        type.move(this, deltaTime);
    }
    
    public void draw() {
        type.draw(x, y, velocityX, velocityY);
    }
}

// Система частиц
class ParticleSystem {
    private List<Particle> particles = new ArrayList<>();
    
    public void createExplosion(double x, double y, int particleCount) {
        System.out.println("Создание взрыва в позиции (" + x + ", " + y + ")");
        
        for (int i = 0; i < particleCount; i++) {
            // Случайное направление
            double angle = Math.random() * 2 * Math.PI;
            double speed = 50 + Math.random() * 100;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            
            // Выбираем тип частицы
            String particleType;
            double rand = Math.random();
            if (rand < 0.5) {
                particleType = "fire";
            } else if (rand < 0.8) {
                particleType = "smoke";
            } else {
                particleType = "spark";
            }
            
            ParticleType type = ParticleTypeFactory.getParticleType(particleType);
            particles.add(new Particle(x, y, vx, vy, type));
        }
    }
    
    public void update(double deltaTime) {
        for (Particle particle : particles) {
            particle.update(deltaTime);
        }
        
        // Удаляем частицы, вышедшие за границы
        particles.removeIf(p -> p.x < 0 || p.x > 1000 || p.y < 0 || p.y > 1000);
    }
    
    public void draw() {
        System.out.println("\n=== Рендеринг частиц ===");
        int count = Math.min(5, particles.size());
        for (int i = 0; i < count; i++) {
            particles.get(i).draw();
        }
        if (particles.size() > 5) {
            System.out.println("... и еще " + (particles.size() - 5) + " частиц");
        }
    }
    
    public void printStatistics() {
        System.out.println("\n=== Статистика системы частиц ===");
        System.out.println("Активных частиц: " + particles.size());
        System.out.println("Типов частиц: " + ParticleTypeFactory.getTypeCount());
        
        long memoryPerParticle = 40; // x, y, vx, vy, ссылка на type
        long memoryWithFlyweight = particles.size() * memoryPerParticle + 
                                  ParticleTypeFactory.getTypeCount() * 200;
        long memoryWithoutFlyweight = particles.size() * 240; // все данные в каждой частице
        
        System.out.println("Память с Flyweight: " + memoryWithFlyweight / 1024 + " KB");
        System.out.println("Память без Flyweight: " + memoryWithoutFlyweight / 1024 + " KB");
        System.out.println("Экономия: " + ((memoryWithoutFlyweight - memoryWithFlyweight) * 100 / memoryWithoutFlyweight) + "%");
    }
}

// Демонстрация
class ParticleSystemDemo {
    public static void main(String[] args) {
        ParticleSystem system = new ParticleSystem();
        
        // Создаем несколько взрывов
        system.createExplosion(100, 100, 500);
        system.createExplosion(200, 150, 300);
        system.createExplosion(300, 200, 200);
        
        // Обновляем и рисуем
        system.update(0.016); // 60 FPS
        system.draw();
        
        // Статистика
        system.printStatistics();
    }
}
```

### Фабрика иконок

Пример кеширования иконок для GUI приложения.

```java
import java.util.*;

// Flyweight - иконка (внутреннее состояние)
class Icon {
    private final String name;
    private final String imagePath;
    private final byte[] imageData;  // Симуляция данных изображения
    
    public Icon(String name, String imagePath) {
        this.name = name;
        this.imagePath = imagePath;
        this.imageData = loadImage(imagePath);
        System.out.println("Загружена иконка: " + name + " из " + imagePath);
    }
    
    private byte[] loadImage(String path) {
        // Симуляция загрузки изображения
        return new byte[1024]; // 1KB
    }
    
    public void render(int x, int y, int size) {
        System.out.println("Рисую иконку '" + name + "' в позиции (" + x + ", " + y + 
                         "), размер " + size + "x" + size);
    }
    
    public String getName() {
        return name;
    }
}

// Фабрика иконок
class IconFactory {
    private static final Map<String, Icon> icons = new HashMap<>();
    private static int loadCount = 0;
    
    public static Icon getIcon(String name, String imagePath) {
        Icon icon = icons.get(name);
        
        if (icon == null) {
            icon = new Icon(name, imagePath);
            icons.put(name, icon);
            loadCount++;
        }
        
        return icon;
    }
    
    public static void printStatistics() {
        System.out.println("\n=== Статистика иконок ===");
        System.out.println("Уникальных иконок загружено: " + icons.size());
        System.out.println("Всего загрузок с диска: " + loadCount);
        System.out.println("Занято памяти: " + (icons.size() * 1024 / 1024.0) + " MB");
    }
}

// Кнопка с иконкой
class Button {
    private final String text;
    private final int x, y;
    private final int width, height;
    private final Icon icon;
    
    public Button(String text, int x, int y, int width, int height, Icon icon) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.icon = icon;
    }
    
    public void render() {
        System.out.println("Кнопка '" + text + "' в (" + x + ", " + y + ")");
        icon.render(x + 5, y + 5, 16);
    }
}

// Пункт меню с иконкой
class MenuItem {
    private final String text;
    private final Icon icon;
    
    public MenuItem(String text, Icon icon) {
        this.text = text;
        this.icon = icon;
    }
    
    public void render(int x, int y) {
        icon.render(x, y, 16);
        System.out.println("  Текст меню: " + text);
    }
}

// GUI приложение
class Application {
    private List<Button> buttons = new ArrayList<>();
    private List<MenuItem> menuItems = new ArrayList<>();
    
    public void createUI() {
        System.out.println("=== Создание UI ===\n");
        
        // Создаем иконки (будут переиспользованы)
        Icon saveIcon = IconFactory.getIcon("save", "icons/save.png");
        Icon openIcon = IconFactory.getIcon("open", "icons/open.png");
        Icon closeIcon = IconFactory.getIcon("close", "icons/close.png");
        Icon copyIcon = IconFactory.getIcon("copy", "icons/copy.png");
        Icon pasteIcon = IconFactory.getIcon("paste", "icons/paste.png");
        
        // Создаем кнопки на панели инструментов
        buttons.add(new Button("Сохранить", 10, 10, 80, 30, saveIcon));
        buttons.add(new Button("Открыть", 100, 10, 80, 30, openIcon));
        buttons.add(new Button("Закрыть", 190, 10, 80, 30, closeIcon));
        buttons.add(new Button("Копировать", 280, 10, 80, 30, copyIcon));
        buttons.add(new Button("Вставить", 370, 10, 80, 30, pasteIcon));
        
        // Создаем пункты меню (переиспользуем те же иконки!)
        menuItems.add(new MenuItem("Сохранить файл", saveIcon));
        menuItems.add(new MenuItem("Открыть файл", openIcon));
        menuItems.add(new MenuItem("Закрыть файл", closeIcon));
        menuItems.add(new MenuItem("Копировать текст", copyIcon));
        menuItems.add(new MenuItem("Вставить текст", pasteIcon));
        
        // Повторное использование иконок
        System.out.println("\nСоздание дополнительных элементов с теми же иконками:");
        Icon saveIconAgain = IconFactory.getIcon("save", "icons/save.png");
        buttons.add(new Button("Сохранить как", 460, 10, 100, 30, saveIconAgain));
        // Иконка не загружается повторно!
    }
    
    public void render() {
        System.out.println("\n=== Рендеринг UI ===\n");
        
        System.out.println("Панель инструментов:");
        for (Button button : buttons) {
            button.render();
        }
        
        System.out.println("\nМеню:");
        int y = 50;
        for (MenuItem item : menuItems) {
            item.render(10, y);
            y += 25;
        }
    }
}

// Демонстрация
class IconFactoryDemo {
    public static void main(String[] args) {
        Application app = new Application();
        app.createUI();
        app.render();
        
        IconFactory.printStatistics();
        
        System.out.println("\nБез Flyweight потребовалось бы загрузить каждую иконку отдельно:");
        System.out.println("6 кнопок + 5 пунктов меню = 11 загрузок");
        System.out.println("С Flyweight: только 5 загрузок (экономия >50%)");
    }
}
```

## Примеры в JDK

### Integer.valueOf() - кеш значений

Java кеширует Integer объекты в диапазоне -128 до 127.

```java
public class IntegerCacheExample {
    public static void main(String[] args) {
        // Integer.valueOf использует Flyweight для часто используемых значений
        
        Integer a1 = Integer.valueOf(100);
        Integer a2 = Integer.valueOf(100);
        System.out.println("a1 == a2: " + (a1 == a2)); // true - тот же объект!
        
        Integer b1 = Integer.valueOf(1000);
        Integer b2 = Integer.valueOf(1000);
        System.out.println("b1 == b2: " + (b1 == b2)); // false - разные объекты
        
        // Посмотрим на реализацию Integer.valueOf():
        /*
        public static Integer valueOf(int i) {
            if (i >= IntegerCache.low && i <= IntegerCache.high)
                return IntegerCache.cache[i + (-IntegerCache.low)];
            return new Integer(i);
        }
        
        // IntegerCache - это Flyweight фабрика!
        private static class IntegerCache {
            static final int low = -128;
            static final int high = 127;
            static final Integer cache[];
            
            static {
                cache = new Integer[(high - low) + 1];
                int j = low;
                for(int k = 0; k < cache.length; k++)
                    cache[k] = new Integer(j++);
            }
        }
        */
        
        // Это экономит память при работе с небольшими числами
        List<Integer> numbers = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            numbers.add(Integer.valueOf(i % 200 - 100));
        }
        // Благодаря Flyweight, в памяти только 200 объектов вместо 1000!
    }
}
```

### String pool

String pool в Java - классический пример Flyweight.

```java
public class StringPoolExample {
    public static void main(String[] args) {
        // String literals автоматически помещаются в String pool (Flyweight)
        String s1 = "Hello";
        String s2 = "Hello";
        System.out.println("s1 == s2: " + (s1 == s2)); // true - тот же объект!
        
        // new String() создает новый объект
        String s3 = new String("Hello");
        System.out.println("s1 == s3: " + (s1 == s3)); // false - разные объекты
        
        // intern() явно помещает строку в pool
        String s4 = new String("Hello").intern();
        System.out.println("s1 == s4: " + (s1 == s4)); // true - тот же объект!
        
        // String pool экономит память
        demonstrateMemorySavings();
    }
    
    private static void demonstrateMemorySavings() {
        // Без String pool
        List<String> withoutPool = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            withoutPool.add(new String("Java"));  // 10000 объектов
        }
        
        // С String pool
        List<String> withPool = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            withPool.add("Java");  // Только 1 объект!
        }
        
        System.out.println("\nБез pool: 10000 объектов String");
        System.out.println("С pool: 1 объект String (переиспользован 10000 раз)");
    }
}
```

### Collections (Boolean, Byte)

```java
public class CollectionsCacheExample {
    public static void main(String[] args) {
        // Boolean - всего 2 объекта для true и false
        Boolean b1 = Boolean.valueOf(true);
        Boolean b2 = Boolean.valueOf(true);
        System.out.println("b1 == b2: " + (b1 == b2)); // true
        
        // Byte - кеш для всех 256 значений (-128 до 127)
        Byte byte1 = Byte.valueOf((byte) 50);
        Byte byte2 = Byte.valueOf((byte) 50);
        System.out.println("byte1 == byte2: " + (byte1 == byte2)); // true
        
        // Long - кеш для -128 до 127
        Long l1 = Long.valueOf(100);
        Long l2 = Long.valueOf(100);
        System.out.println("l1 == l2: " + (l1 == l2)); // true
        
        Long l3 = Long.valueOf(1000);
        Long l4 = Long.valueOf(1000);
        System.out.println("l3 == l4: " + (l3 == l4)); // false
    }
}
```

### Character cache

```java
public class CharacterCacheExample {
    public static void main(String[] args) {
        // Character кеширует символы от 0 до 127 (ASCII)
        Character c1 = Character.valueOf('A');
        Character c2 = Character.valueOf('A');
        System.out.println("c1 == c2: " + (c1 == c2)); // true
        
        // Символы вне ASCII не кешируются
        Character c3 = Character.valueOf('Я');
        Character c4 = Character.valueOf('Я');
        System.out.println("c3 == c4: " + (c3 == c4)); // false (может быть true в некоторых JVM)
        
        // Реализация в JDK:
        /*
        public static Character valueOf(char c) {
            if (c <= 127) { // must cache
                return CharacterCache.cache[(int)c];
            }
            return new Character(c);
        }
        
        private static class CharacterCache {
            private CharacterCache(){}
            
            static final Character cache[] = new Character[127 + 1];
            
            static {
                for (int i = 0; i < cache.length; i++)
                    cache[i] = new Character((char)i);
            }
        }
        */
    }
}
```

## Преимущества и недостатки

### Преимущества

✅ **Экономия памяти**

```java
// Без Flyweight
class Document {
    List<Character> chars = new ArrayList<>();
    
    void addChar(char c, String font, int size, String color) {
        // Каждый символ хранит font, size, color
        chars.add(new Character(c, font, size, color)); // ~100 байт
    }
}
// 100,000 символов = 10 MB

// С Flyweight
class EfficientDocument {
    List<StyledChar> chars = new ArrayList<>();
    
    void addChar(char c, String font, int size, String color) {
        CharStyle style = StyleFactory.getStyle(font, size, color);
        chars.add(new StyledChar(c, style)); // ~16 байт (char + ссылка)
    }
}
// 100,000 символов + 10 стилей = ~1.6 MB (экономия 84%)
```

✅ **Уменьшение количества объектов**

```java
// Flyweight резко сокращает количество объектов
class Game {
    void spawnEnemies() {
        // Без Flyweight: 10,000 объектов
        for (int i = 0; i < 10000; i++) {
            enemies.add(new Enemy("Orc", texture, stats));
        }
        
        // С Flyweight: 10,000 ссылок + 1 общий тип
        EnemyType orcType = EnemyFactory.getType("Orc");
        for (int i = 0; i < 10000; i++) {
            enemies.add(new Enemy(x, y, orcType));
        }
    }
}
```

✅ **Производительность (меньше garbage collection)**

```java
// Меньше объектов = меньше работы для GC
public class PerformanceComparison {
    // Без Flyweight - создаем миллионы объектов
    void withoutFlyweight() {
        List<Tree> trees = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            trees.add(new Tree("Pine", texture, x, y)); // Новый объект каждый раз
        }
        // GC постоянно работает
    }
    
    // С Flyweight - переиспользуем объекты
    void withFlyweight() {
        TreeType pineType = TreeFactory.getType("Pine");
        List<TreeInstance> trees = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            trees.add(new TreeInstance(x, y, pineType)); // Только позиция
        }
        // GC работает гораздо реже
    }
}
```

✅ **Централизованное управление**

```java
// Все общие данные в одном месте
class FontManager {
    private Map<String, Font> fonts = new HashMap<>();
    
    Font getFont(String name, int size) {
        String key = name + "_" + size;
        Font font = fonts.get(key);
        
        if (font == null) {
            font = loadFont(name, size); // Дорогая операция
            fonts.put(key, font);
        }
        
        return font;
    }
    
    // Легко очистить весь кеш
    void clearCache() {
        fonts.clear();
    }
    
    // Легко получить статистику
    int getCachedFontsCount() {
        return fonts.size();
    }
}
```

### Недостатки

❌ **Усложнение кода**

```java
// Без Flyweight - просто
class Tree {
    String type;
    int x, y;
    
    void draw() {
        System.out.println(type + " at " + x + ", " + y);
    }
}

// С Flyweight - сложнее
class TreeType { /* Flyweight */ }
class TreeFactory { /* Фабрика */ }
class Tree {
    int x, y;
    TreeType type;
}
// Больше классов, больше кода
```

❌ **Необходимость различать внутреннее и внешнее состояние**

```java
// Нужно тщательно разделять состояние
class Character {
    // Что внутреннее, а что внешнее?
    char symbol;      // Внутреннее (общее)
    String font;      // Внутреннее (общее)
    int size;         // Внутреннее (общее)
    int x, y;         // Внешнее (уникальное)
    boolean selected; // Внешнее (уникальное)
    
    // Ошибка в разделении может привести к багам!
}
```

❌ **Потокобезопасность**

```java
// Разделяемые объекты должны быть неизменяемыми или потокобезопасными
class TreeType {
    private String name;
    private String color;
    
    // ПЛОХО - изменяемое состояние в многопоточной среде!
    private int renderCount;
    
    void draw(int x, int y) {
        renderCount++; // Race condition!
        // ...
    }
}

// ПРАВИЛЬНО - неизменяемый Flyweight
class ImmutableTreeType {
    private final String name;
    private final String color;
    
    // Счетчик вне Flyweight или через AtomicInteger
    void draw(int x, int y) {
        // Только чтение внутреннего состояния
    }
}
```

❌ **Накладные расходы на поиск в фабрике**

```java
class FlyweightFactory {
    private Map<String, Flyweight> cache = new HashMap<>();
    
    Flyweight getFlyweight(String key) {
        // Каждый раз поиск в HashMap
        Flyweight fw = cache.get(key); // O(1), но накладные расходы
        
        if (fw == null) {
            fw = new ConcreteFlyweight(key);
            cache.put(key, fw);
        }
        
        return fw;
    }
}

// Для часто создаваемых объектов это может быть узким местом
```

❌ **Сложность отладки**

```java
// Трудно понять, какие объекты разделяются
void debugExample() {
    TreeType type1 = factory.getType("Pine");
    TreeType type2 = factory.getType("Pine");
    
    // type1 и type2 - это один объект!
    // При изменении type1 изменится и type2 (если изменяемый)
    // Это может быть неочевидно при отладке
}
```

## Вопросы на собеседовании

### Базовые вопросы

**1. Что такое паттерн Flyweight и когда его использовать?**

*Ответ:* Flyweight — это структурный паттерн, который позволяет вмещать большее количество объектов в память за счет разделения общего состояния между ними. Используется когда:
- Приложение создает большое количество похожих объектов
- Хранение всех объектов требует слишком много памяти
- Большую часть состояния объектов можно сделать общей (внутреннее состояние)
- Приложение не зависит от идентичности объектов

**2. В чем разница между внутренним и внешним состоянием?**

*Ответ:*
- **Внутреннее состояние (intrinsic)** — хранится в Flyweight, не зависит от контекста, является общим и неизменяемым. Пример: тип дерева, текстура.
- **Внешнее состояние (extrinsic)** — передается в методы Flyweight, зависит от контекста, является уникальным для каждого использования. Пример: позиция дерева, размер символа на экране.

```java
class TreeType {
    private String name;    // Внутреннее - общее для всех сосен
    private String texture; // Внутреннее - одна текстура на все
    
    void draw(int x, int y) {  // x, y - внешнее, уникально для каждого дерева
        // ...
    }
}
```

**3. Приведите примеры Flyweight из JDK**

*Ответ:*
- **Integer.valueOf()** — кеширует значения от -128 до 127
- **String pool** — все строковые литералы хранятся в пуле
- **Boolean.valueOf()** — только 2 объекта (TRUE и FALSE)
- **Byte.valueOf()** — кеш всех 256 значений
- **Character.valueOf()** — кеширует ASCII символы (0-127)

**4. Какова структура паттерна Flyweight?**

*Ответ:* Основные компоненты:
- **Flyweight** — интерфейс приспособленца с методами, принимающими внешнее состояние
- **ConcreteFlyweight** — конкретный приспособленец, хранит внутреннее состояние
- **FlyweightFactory** — создает и управляет Flyweight объектами, обеспечивает их переиспользование
- **Client** — хранит внешнее состояние и вызывает методы Flyweight с этим состоянием

### Продвинутые вопросы

**5. Почему Flyweight объекты должны быть неизменяемыми?**

*Ответ:* Flyweight объекты разделяются между множеством контекстов. Если Flyweight изменяемый, то изменение в одном месте повлияет на все остальные места, где он используется, что приведет к трудноуловимым багам:

```java
// ПЛОХО - изменяемый Flyweight
class MutableTreeType {
    private String name;
    
    public void setName(String name) {
        this.name = name; // Изменит имя для ВСЕХ деревьев этого типа!
    }
}

// ХОРОШО - неизменяемый Flyweight
class ImmutableTreeType {
    private final String name;
    
    public ImmutableTreeType(String name) {
        this.name = name;
    }
    
    // Только геттеры, нет сеттеров
    public String getName() {
        return name;
    }
}
```

**6. В чем разница между Flyweight и Object Pool?**

*Ответ:*

| Критерий | Flyweight | Object Pool |
|----------|-----------|-------------|
| **Цель** | Экономия памяти через разделение | Переиспользование дорогих объектов |
| **Количество экземпляров** | Фиксированное (один на тип) | Переменное (пул объектов) |
| **Изменяемость** | Неизменяемые | Часто изменяемые |
| **Возврат в пул** | Не нужен | Объекты возвращаются в пул |
| **Идентичность** | Один объект используется везде | Разные объекты из пула |

```java
// Flyweight - один объект для всех
TreeType pine = factory.getType("Pine");
Tree t1 = new Tree(10, 20, pine);
Tree t2 = new Tree(30, 40, pine); // Та же ссылка на pine

// Object Pool - берем и возвращаем объекты
Connection conn1 = pool.getConnection();
// используем conn1
pool.returnConnection(conn1);
Connection conn2 = pool.getConnection(); // Может быть тот же conn1
```

**7. Как тестировать код с Flyweight?**

*Ответ:* Стратегии тестирования:

```java
@Test
public void testFlyweightSharing() {
    TreeFactory factory = new TreeFactory();
    
    // Проверяем, что объекты переиспользуются
    TreeType type1 = factory.getType("Pine", "Green", "texture");
    TreeType type2 = factory.getType("Pine", "Green", "texture");
    
    assertSame(type1, type2); // Должны быть одним объектом
    assertEquals(1, factory.getTypeCount()); // Только один тип создан
}

@Test
public void testDifferentFlyweights() {
    TreeFactory factory = new TreeFactory();
    
    TreeType pine = factory.getType("Pine", "Green", "pine.png");
    TreeType oak = factory.getType("Oak", "Brown", "oak.png");
    
    assertNotSame(pine, oak); // Разные типы
    assertEquals(2, factory.getTypeCount());
}

@Test
public void testMemoryEfficiency() {
    Forest forest = new Forest();
    
    // Сажаем много деревьев
    for (int i = 0; i < 10000; i++) {
        forest.plantTree(i, i, "Pine", "Green", "pine.png");
    }
    
    // Должен быть только один TreeType
    assertEquals(1, TreeFactory.getTypeCount());
}
```

**8. Какие проблемы могут возникнуть при использовании Flyweight?**

*Ответ:*
1. **Сложность кода** — нужно разделять состояние, управлять фабрикой
2. **Потокобезопасность** — разделяемые объекты должны быть thread-safe
3. **Утечки памяти** — если фабрика хранит Flyweight бесконечно
4. **Производительность поиска** — поиск в HashMap при каждом запросе

```java
// Проблема: утечка памяти
class LeakyFactory {
    private Map<String, Flyweight> cache = new HashMap<>();
    
    Flyweight get(String key) {
        Flyweight fw = cache.get(key);
        if (fw == null) {
            fw = new ConcreteFlyweight(key);
            cache.put(key, fw); // Никогда не удаляется!
        }
        return fw;
    }
}

// Решение: ограничение размера или WeakHashMap
class SafeFactory {
    private Map<String, Flyweight> cache = new WeakHashMap<>();
    // Или LRU cache с ограничением размера
}
```

**9. Можно ли комбинировать Flyweight с другими паттернами?**

*Ответ:* Да, часто комбинируется:

**Flyweight + Factory Method:**
```java
class FlyweightFactory {
    // Factory Method создает Flyweight
    protected Flyweight createFlyweight(String key) {
        return new ConcreteFlyweight(key);
    }
}
```

**Flyweight + Singleton:**
```java
// Фабрика Flyweight часто Singleton
class TreeFactory {
    private static TreeFactory instance;
    
    public static TreeFactory getInstance() {
        if (instance == null) {
            instance = new TreeFactory();
        }
        return instance;
    }
}
```

**Flyweight + Composite:**
```java
// Листья Composite могут быть Flyweight
class TextCharacter implements TextComponent {
    private CharacterStyle style; // Flyweight
    private int position; // Внешнее состояние
}
```

**Flyweight + State:**
```java
// Состояния могут быть Flyweight
class TCPConnection {
    private ConnectionState state; // Flyweight состояние
    
    void setState(ConnectionState state) {
        this.state = StateFactory.getState(state.getName());
    }
}
```

**10. Как реализовать потокобезопасный Flyweight?**

*Ответ:* Несколько подходов:

**1. Неизменяемый Flyweight (лучший вариант):**
```java
class ImmutableFlyweight {
    private final String data;
    
    public ImmutableFlyweight(String data) {
        this.data = data;
    }
    
    public String getData() {
        return data; // Безопасно в любом потоке
    }
}
```

**2. Потокобезопасная фабрика:**
```java
class ThreadSafeFactory {
    private final ConcurrentHashMap<String, Flyweight> cache = new ConcurrentHashMap<>();
    
    public Flyweight getFlyweight(String key) {
        return cache.computeIfAbsent(key, k -> new ConcreteFlyweight(k));
    }
}
```

**3. Double-checked locking (если нужна ленивая инициализация):**
```java
class LazyFactory {
    private volatile Map<String, Flyweight> cache;
    
    public Flyweight getFlyweight(String key) {
        if (cache == null) {
            synchronized (this) {
                if (cache == null) {
                    cache = new HashMap<>();
                }
            }
        }
        
        Flyweight fw = cache.get(key);
        if (fw == null) {
            synchronized (this) {
                fw = cache.get(key);
                if (fw == null) {
                    fw = new ConcreteFlyweight(key);
                    cache.put(key, fw);
                }
            }
        }
        return fw;
    }
}
```

---

[← Назад к разделу Структурные паттерны](README.md)
