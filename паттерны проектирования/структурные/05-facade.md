# Facade (Фасад)

Facade — структурный паттерн проектирования, который предоставляет простой интерфейс к сложной системе классов, библиотеке или фреймворку.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Домашний кинотеатр](#домашний-кинотеатр)
   - [Система онлайн-заказов](#система-онлайн-заказов)
   - [Компилятор](#компилятор)
5. [Примеры в JDK и фреймворках](#примеры-в-jdk-и-фреймворках)
6. [Преимущества и недостатки](#преимущества-и-недостатки)
7. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Facade используется когда:
- Необходимо предоставить простой интерфейс к сложной подсистеме
- Нужно уменьшить зависимости между клиентом и реализацией подсистемы
- Требуется разбить подсистему на слои
- Нужен единый интерфейс для работы с набором интерфейсов

**Типичные примеры использования:**
- Упрощение работы со сложными библиотеками
- API обертки над legacy системами
- Единая точка входа в подсистему
- Слои в многоуровневой архитектуре
- Клиентские библиотеки для REST API

## Проблема, которую решает

### Проблема: Сложное взаимодействие с подсистемой

```java
// Без Facade - клиент должен знать о множестве классов

public class Client {
    public void watchMovie(String movie) {
        // Включаем все устройства
        Amplifier amp = new Amplifier();
        DvdPlayer dvd = new DvdPlayer();
        Projector projector = new Projector();
        Screen screen = new Screen();
        TheaterLights lights = new TheaterLights();
        PopcornPopper popper = new PopcornPopper();
        
        // Настраиваем каждое устройство
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.down();
        projector.on();
        projector.setInput(dvd);
        projector.wideScreenMode();
        amp.on();
        amp.setDvd(dvd);
        amp.setSurroundSound();
        amp.setVolume(5);
        dvd.on();
        dvd.play(movie);
        
        // Клиент должен знать о 6+ классах и порядке вызовов!
    }
}
```

**Проблемы:**
- Клиент сильно связан с множеством классов подсистемы
- Сложный и подверженный ошибкам код
- Дублирование логики настройки
- Изменения в подсистеме влияют на всех клиентов

### Решение: Facade

Создать простой интерфейс для сложных операций.

```java
// С Facade - простой интерфейс
public class HomeTheaterFacade {
    private Amplifier amp;
    private DvdPlayer dvd;
    private Projector projector;
    private Screen screen;
    private TheaterLights lights;
    private PopcornPopper popper;
    
    public void watchMovie(String movie) {
        System.out.println("Приготовьтесь смотреть фильм...");
        popper.on();
        popper.pop();
        lights.dim(10);
        screen.down();
        projector.on();
        projector.wideScreenMode();
        amp.on();
        amp.setDvd(dvd);
        amp.setSurroundSound();
        amp.setVolume(5);
        dvd.on();
        dvd.play(movie);
    }
    
    public void endMovie() {
        System.out.println("Выключаем кинотеатр...");
        // упрощенная логика выключения
    }
}

// Клиент
HomeTheaterFacade homeTheater = new HomeTheaterFacade(...);
homeTheater.watchMovie("Матрица");
homeTheater.endMovie();
```

## Структура паттерна

```java
// Сложная подсистема
class SubsystemA {
    public void operationA1() {
        System.out.println("Subsystem A: operation A1");
    }
    
    public void operationA2() {
        System.out.println("Subsystem A: operation A2");
    }
}

class SubsystemB {
    public void operationB1() {
        System.out.println("Subsystem B: operation B1");
    }
}

class SubsystemC {
    public void operationC1() {
        System.out.println("Subsystem C: operation C1");
    }
}

// Facade - упрощенный интерфейс
class Facade {
    private SubsystemA subsystemA;
    private SubsystemB subsystemB;
    private SubsystemC subsystemC;
    
    public Facade() {
        this.subsystemA = new SubsystemA();
        this.subsystemB = new SubsystemB();
        this.subsystemC = new SubsystemC();
    }
    
    // Высокоуровневая операция
    public void operation1() {
        System.out.println("Facade operation 1:");
        subsystemA.operationA1();
        subsystemB.operationB1();
    }
    
    public void operation2() {
        System.out.println("Facade operation 2:");
        subsystemA.operationA2();
        subsystemC.operationC1();
    }
}

// Клиент работает только с Facade
class Client {
    public static void main(String[] args) {
        Facade facade = new Facade();
        facade.operation1();
        facade.operation2();
    }
}
```

## Реализация

### Домашний кинотеатр

Полный пример с кинотеатром показывает, как Facade упрощает управление множеством устройств.

