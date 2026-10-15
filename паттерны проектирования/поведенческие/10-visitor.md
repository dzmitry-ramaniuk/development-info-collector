---
---

# Visitor (Посетитель)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Идея паттерна](#идея-паттерна)
3. [Участники](#участники)
4. [Пример на Java](#пример-на-java)
5. [Плюсы и ограничения](#плюсы-и-ограничения)
6. [Практическое упражнение](#практическое-упражнение)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15
- **Целевая версия или диапазон:** концепция версионно-независима; пример использует Java 17, компиляция с `--release 17` и запуск на JDK 21
- **Статус примеров:** `current`
- **Первичные источники:** [Java Language Specification, classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html); [адресный API/контракт](https://docs.oracle.com/en/java/javase/21/docs/api/java.compiler/javax/lang/model/element/ElementVisitor.html)

## Идея паттерна

**Visitor** отделяет операции от структуры объектов и позволяет добавлять новые операции без изменения классов элементов.

## Участники

- **Visitor** — интерфейс операций;
- **ConcreteVisitor** — конкретная операция;
- **Element** — интерфейс с методом `accept`;
- **ObjectStructure** — набор элементов.

## Пример на Java

<!-- runnable: VisitorDemo.java -->
```java
import java.util.List;

public class VisitorDemo {
    interface Shape {
        double accept(ShapeVisitor visitor);
    }
    interface ShapeVisitor {
        double visit(Circle circle);
        double visit(Rectangle rectangle);
    }
    record Circle(double radius) implements Shape {
        public double accept(ShapeVisitor visitor) { return visitor.visit(this); }
    }
    record Rectangle(double width, double height) implements Shape {
        public double accept(ShapeVisitor visitor) { return visitor.visit(this); }
    }
    static final class AreaVisitor implements ShapeVisitor {
        public double visit(Circle circle) { return Math.PI * circle.radius() * circle.radius(); }
        public double visit(Rectangle rectangle) { return rectangle.width() * rectangle.height(); }
    }
    public static void main(String[] args) {
        ShapeVisitor area = new AreaVisitor();
        List<Shape> shapes = List.of(new Circle(1), new Rectangle(2, 3));
        double total = shapes.stream().mapToDouble(shape -> shape.accept(area)).sum();
        if (Math.abs(total - (Math.PI + 6)) > 1e-9) throw new AssertionError("Wrong area");
        System.out.println("area ok");
    }
}
```

Сохраните код как `VisitorDemo.java`, выполните `java VisitorDemo.java` на JDK 17+. Ожидаемый вывод: `area ok`. Автоматическая проверка из корня: `python3 scripts/check-java-examples.py`.


## Плюсы и ограничения

- плюс: легко добавлять новые операции;
- минус: тяжело добавлять новые типы элементов;
- полезен для AST, отчётов, валидации сложных моделей.

Здесь видна **двойная диспетчеризация**: виртуальный `accept` выбирает конкретный элемент, а внутри него `visit(this)` выбирает перегрузку для `Circle` или `Rectangle`. Java не выбирает перегрузку по динамическому типу аргумента сама: прямой вызов `visit` со ссылкой типа `Shape` не заменяет этот механизм.

## Практическое упражнение

Добавьте операцию вычисления периметра отдельным visitor, сохранив классы элементов. Затем добавьте `Triangle` и перечислите, какие интерфейсы и visitor потребуется изменить.

**Критерий:** площадь круга радиуса 1 равна π с допуском, прямоугольника 2×3 — 6; периметры равны 2π и 10. Новая операция не меняет существующие элементы.

**Разбор:** Visitor удобен для стабильного набора типов и растущего числа операций, например для AST. Если типов постоянно становится больше, обычный полиморфный метод или разбор закрытой sealed-иерархии может быть проще. Результат visitor не обязан быть `double`; здесь выбран один числовой контракт для двух учебных операций.

## Вопросы для самопроверки

1. **Почему одного overload visit(Shape) недостаточно?** Выбор перегрузки зависит от статического типа выражения; `accept` передаёт `this` конкретного типа.
2. **Что дешевле расширять?** Новую операцию: добавляется visitor. Новый тип элемента требует обновить интерфейс visitor и его реализации.
3. **Где встречается похожий контракт в JDK?** `ElementVisitor` и `Element.accept` в API модели Java-кода; конкретные операции реализуются посетителями.
