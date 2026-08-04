# Mediator (Посредник)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Зачем нужен паттерн](#зачем-нужен-паттерн)
3. [Структура и участники](#структура-и-участники)
4. [Пример на Java](#пример-на-java)
5. [Когда применять](#когда-применять)
6. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-08-04
- **Целевая версия или диапазон:** концепция версионно-независима; технические примеры проверены на Java 17–25
- **Статус примеров:** `current`
- **Первичные источники:** [Oracle Java Language Specification](https://docs.oracle.com/javase/specs/); [Oracle Java API](https://docs.oracle.com/en/java/javase/25/docs/api/)

## Зачем нужен паттерн

**Mediator** централизует взаимодействие между объектами и уменьшает прямые связи между ними.

> Идея: компоненты знают не друг о друге, а о посреднике.

## Структура и участники

- **Mediator** — контракт взаимодействия.
- **ConcreteMediator** — содержит правила координации.
- **Colleague** — участник, который делегирует коммуникацию посреднику.

## Пример на Java

```java
interface ChatMediator {
    void send(String message, User from);
    void register(User user);
}

class TeamChatMediator implements ChatMediator {
    private final List<User> users = new ArrayList<>();

    @Override
    public void send(String message, User from) {
        for (User user : users) {
            if (user != from) {
                user.receive(message, from.name());
            }
        }
    }

    @Override
    public void register(User user) {
        users.add(user);
    }
}
```

## Когда применять

- много связей «каждый с каждым»;
- правила коммуникации часто меняются;
- нужна единая точка для логирования и контроля сценариев.

## Вопросы для самопроверки

1. Чем Mediator отличается от Facade?
2. Какие риски у «толстого» посредника?
3. Когда лучше оставить прямые связи между объектами?
