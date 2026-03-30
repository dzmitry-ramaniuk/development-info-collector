# State (Состояние)

## Содержание

1. [Назначение паттерна](#назначение-паттерна)
2. [Структура](#структура)
3. [Пример на Java](#пример-на-java)
4. [Сравнение со Strategy](#сравнение-со-strategy)
5. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Назначение паттерна

**State** позволяет объекту менять поведение при смене внутреннего состояния без большого `if/else`.

## Структура

- **Context** — содержит текущее состояние.
- **State** — интерфейс поведения.
- **ConcreteState** — конкретная реализация состояния.

## Пример на Java

```java
interface OrderState {
    void next(OrderContext context);
    String name();
}

class CreatedState implements OrderState {
    public void next(OrderContext context) { context.setState(new PaidState()); }
    public String name() { return "CREATED"; }
}

class PaidState implements OrderState {
    public void next(OrderContext context) { context.setState(new ShippedState()); }
    public String name() { return "PAID"; }
}
```

## Сравнение со Strategy

State меняет поведение из-за жизненного цикла объекта, а Strategy — из-за выбранного алгоритма.

## Вопросы для самопроверки

1. Когда State предпочтительнее switch-case?
2. Как обеспечить thread safety при переходах?
3. Какие состояния в вашей системе естественно выделяются?
