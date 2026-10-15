---
---

# Memento (Хранитель)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Идея паттерна](#идея-паттерна)
3. [Участники](#участники)
4. [Пример на Java](#пример-на-java)
5. [Практические советы](#практические-советы)
6. [Практическое упражнение](#практическое-упражнение)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15
- **Целевая версия или диапазон:** концепция версионно-независима; пример использует Java 17, компиляция с `--release 17` и запуск на JDK 21
- **Статус примеров:** `current`
- **Первичные источники:** [Java Language Specification, classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html); [адресный API/контракт](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/undo/UndoableEdit.html)

## Идея паттерна

**Memento** позволяет сохранять и восстанавливать состояние объекта, не раскрывая внутреннюю структуру.

## Участники

- **Originator** — объект, состояние которого сохраняется.
- **Memento** — снимок состояния.
- **Caretaker** — хранит историю снимков.

## Пример на Java

<!-- runnable: MementoDemo.java -->
```java
import java.util.ArrayDeque;
import java.util.Deque;

public class MementoDemo {
    static final class TextEditor {
        interface Memento {}
        private record Snapshot(String text) implements Memento {}
        private String text = "";
        void write(String value) { text += value; }
        Memento save() { return new Snapshot(text); }
        void restore(Memento memento) {
            if (!(memento instanceof Snapshot snapshot)) {
                throw new IllegalArgumentException("Unknown memento");
            }
            text = snapshot.text();
        }
        String text() { return text; }
    }
    public static void main(String[] args) {
        TextEditor editor = new TextEditor();
        Deque<TextEditor.Memento> history = new ArrayDeque<>();
        editor.write("A");
        history.push(editor.save());
        editor.write("B");
        editor.restore(history.pop());
        if (!editor.text().equals("A")) throw new AssertionError("Undo failed");
        System.out.println(editor.text());
    }
}
```

Сохраните код как `MementoDemo.java`, выполните `java MementoDemo.java` на JDK 17+. Ожидаемый вывод: `A`. Автоматическая проверка из корня: `python3 scripts/check-java-examples.py`.


## Практические советы

- ограничивайте глубину истории (например, ring buffer);
- для больших состояний храните дельты, а не полный снимок;
- следите за безопасностью, если состояние содержит чувствительные данные.

`TextEditor` — Originator, закрытый `Snapshot` — Memento, а стек в `main` — Caretaker. История хранит непрозрачный `Memento` и не читает состояние снимка. `String` неизменяем, поэтому снимок сохраняет прежний текст без глубокого копирования. Для `List` или изменяемого графа объектов понадобилась бы явно выбранная стратегия копирования; одного `record` недостаточно.

## Практическое упражнение

Реализуйте два последовательных undo и ограничьте историю последними десятью снимками. Задайте поведение undo при пустой истории. После undo и нового редактирования очистите redo-ветку, если добавили redo.

**Критерий:** после ввода `A`, `B`, `C` два undo возвращают `A`; лимит истории соблюдается, пустая история не вызывает случайного изменения текста.

**Разбор:** сохраняйте снимок перед изменением. Memento восстанавливает локальное состояние, но не отменяет уже отправленное письмо или платёж. Для внешних эффектов требуется отдельная компенсирующая операция.

## Вопросы для самопроверки

1. **Зачем непрозрачный снимок?** Чтобы история не зависела от внутреннего устройства редактора и не изменяла его поля напрямую.
2. **Почему снимок может оказаться дорогим?** Полная копия большого состояния на каждый шаг расходует память; помогают лимит истории, структурное разделение или дельты.
3. **Как сочетать Memento и Command?** Команда хранит снимок до выполнения и использует его при локальном undo, если операция допускает такое восстановление.
