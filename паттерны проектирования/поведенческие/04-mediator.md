---
---

# Mediator (Посредник)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Зачем нужен паттерн](#зачем-нужен-паттерн)
3. [Структура и участники](#структура-и-участники)
4. [Пример на Java](#пример-на-java)
5. [Когда применять](#когда-применять)
6. [Практическое упражнение](#практическое-упражнение)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15
- **Целевая версия или диапазон:** концепция версионно-независима; пример использует Java 17, компиляция с `--release 17` и запуск на JDK 21
- **Статус примеров:** `current`
- **Первичные источники:** [Java Language Specification, classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html); [адресный API/контракт](https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/ButtonGroup.html)

## Зачем нужен паттерн

**Mediator** централизует взаимодействие между объектами и уменьшает прямые связи между ними.

> Идея: компоненты знают не друг о друге, а о посреднике.

## Структура и участники

- **Mediator** — контракт взаимодействия.
- **ConcreteMediator** — содержит правила координации.
- **Colleague** — участник, который делегирует коммуникацию посреднику.

## Пример на Java

<!-- runnable: MediatorDemo.java -->
```java
import java.util.ArrayList;
import java.util.List;

public class MediatorDemo {
    interface ChatMediator {
        void register(Member member);
        void send(String message, Member from);
    }
    static final class TeamChat implements ChatMediator {
        private final List<Member> members = new ArrayList<>();
        public void register(Member member) {
            if (members.contains(member)) throw new IllegalArgumentException("Already registered");
            members.add(member);
        }
        public void send(String message, Member from) {
            if (!members.contains(from)) throw new IllegalArgumentException("Unknown sender");
            for (Member member : List.copyOf(members)) {
                if (member != from) member.receive(from.name + ": " + message);
            }
        }
    }
    static final class Member {
        private final String name;
        private final ChatMediator mediator;
        private final List<String> inbox = new ArrayList<>();
        Member(String name, ChatMediator mediator) {
            this.name = name;
            this.mediator = mediator;
            mediator.register(this);
        }
        void send(String message) { mediator.send(message, this); }
        void receive(String message) { inbox.add(message); }
    }
    public static void main(String[] args) {
        ChatMediator chat = new TeamChat();
        Member alice = new Member("Alice", chat);
        Member bob = new Member("Bob", chat);
        alice.send("hello");
        if (!alice.inbox.isEmpty() || !bob.inbox.equals(List.of("Alice: hello"))) {
            throw new AssertionError("Incorrect routing");
        }
        System.out.println(bob.inbox.get(0));
    }
}
```

Сохраните код как `MediatorDemo.java`, выполните `java MediatorDemo.java` на JDK 17+. Ожидаемый вывод: `Alice: hello`. Автоматическая проверка из корня: `python3 scripts/check-java-examples.py`.


## Когда применять

- много связей «каждый с каждым»;
- правила коммуникации часто меняются;
- нужна единая точка для логирования и контроля сценариев.

Пример синхронный и однопоточный: `List.copyOf` защищает обход от изменения списка самим callback, но не делает чат потокобезопасным. Участник знает только посредника. Правило «доставить всем, кроме отправителя» сосредоточено в `TeamChat`; сетевой транспорт и хранение сообщений здесь отсутствуют.

## Практическое упражнение

Добавьте третьего участника и метод удаления участника. После удаления сообщение должны получить только зарегистрированные участники; отправка от удалённого участника должна завершиться отказом. Сохраните проверку отсутствия сообщения у самого отправителя.

**Критерий:** проверяются состав получателей, содержимое сообщения и отрицательный сценарий. Правила маршрутизации остаются в посреднике.

**Разбор:** для небольшой пары объектов прямая связь проще. Mediator полезен, когда меняются именно правила взаимодействия; бизнес-логику всех участников переносить в один «божественный объект» не следует.

## Вопросы для самопроверки

1. **Чем Mediator отличается от Facade?** Посредник координирует участников, которые обращаются к нему; фасад предоставляет клиенту удобный вход в подсистему.
2. **Когда лучше оставить прямые связи?** Когда участников мало и отдельные правила координации не нужны.
3. **Как избежать «толстого» посредника?** Разделять независимые процессы и оставлять локальную бизнес-логику у участников.
