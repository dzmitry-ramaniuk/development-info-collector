---
---

# Template Method (Шаблонный метод)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Что решает паттерн](#что-решает-паттерн)
3. [Структура](#структура)
4. [Пример на Java](#пример-на-java)
5. [Когда использовать](#когда-использовать)
6. [Практическое упражнение](#практическое-упражнение)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15
- **Целевая версия или диапазон:** концепция версионно-независима; пример использует Java 17, компиляция с `--release 17` и запуск на JDK 21
- **Статус примеров:** `current`
- **Первичные источники:** [Java Language Specification, classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html); [адресный API/контракт](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.3.3)

## Что решает паттерн

**Template Method** фиксирует скелет алгоритма в базовом классе и делегирует отдельные шаги подклассам.

## Структура

- базовый класс с `templateMethod()`;
- обязательные абстрактные шаги;
- опциональные hook-методы.

## Пример на Java

<!-- runnable: TemplateMethodDemo.java -->
```java
import java.util.ArrayList;
import java.util.List;

public class TemplateMethodDemo {
    abstract static class DataExporter {
        public final List<String> export() {
            List<String> events = new ArrayList<>();
            read(events);
            transform(events);
            write(events);
            return List.copyOf(events);
        }
        protected abstract void read(List<String> events);
        protected abstract void write(List<String> events);
        protected void transform(List<String> events) {}
    }
    static final class CsvExporter extends DataExporter {
        protected void read(List<String> events) { events.add("read"); }
        protected void transform(List<String> events) { events.add("normalize"); }
        protected void write(List<String> events) { events.add("write"); }
    }
    public static void main(String[] args) {
        List<String> events = new CsvExporter().export();
        if (!events.equals(List.of("read", "normalize", "write"))) {
            throw new AssertionError("Wrong step order");
        }
        System.out.println(String.join(" -> ", events));
    }
}
```

Сохраните код как `TemplateMethodDemo.java`, выполните `java TemplateMethodDemo.java` на JDK 17+. Ожидаемый вывод: `read -> normalize -> write`. Автоматическая проверка из корня: `python3 scripts/check-java-examples.py`.


## Когда использовать

- когда общий алгоритм стабилен, а шаги вариативны;
- когда нужно централизованно гарантировать порядок операций.

`export()` задаёт порядок шагов и объявлен `final`, поэтому подкласс меняет детали, но не сам алгоритм. `transform()` — hook с поведением по умолчанию. Список событий служит наблюдаемым результатом учебного примера; настоящий экспорт должен определить формат данных, обработку ошибок и закрытие ресурсов.

## Практическое упражнение

Добавьте экспортёр без нормализации и экспортёр с ошибкой в `read`. Проверьте соответственно порядок `read → write` и отсутствие `write` после ошибки.

**Критерий:** обязательные шаги реализованы, hook можно не переопределять, ошибка не приводит к продолжению успешного сценария.

**Разбор:** когда алгоритм нужно собирать и менять во время выполнения, передайте отдельные стратегии через композицию. Глубокая иерархия подклассов ради всех сочетаний шагов усложнит сопровождение.

## Вопросы для самопроверки

1. **Чем Template Method отличается от Strategy?** Шаблон фиксирует алгоритм наследованием; стратегия передаёт изменяемое поведение через композицию.
2. **Когда hook лучше абстрактного метода?** Когда шаг необязателен и корректное поведение по умолчанию уже существует.
3. **Зачем final у шаблонного метода?** Он запрещает подклассу переопределять установленный порядок шагов; это ограничение расширения, а не гарантия безопасности произвольного кода подкласса.
