---
---

# State (Состояние)

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Назначение паттерна](#назначение-паттерна)
3. [Структура](#структура)
4. [Пример на Java](#пример-на-java)
5. [Сравнение со Strategy](#сравнение-со-strategy)
6. [Практическое упражнение](#практическое-упражнение)
7. [Вопросы для самопроверки](#вопросы-для-самопроверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15
- **Целевая версия или диапазон:** концепция версионно-независима; пример использует Java 17, компиляция с `--release 17` и запуск на JDK 21
- **Статус примеров:** `current`
- **Первичные источники:** [Java Language Specification, classes](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html); [адресный API/контракт](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.4.8)

## Назначение паттерна

**State** позволяет объекту менять поведение при смене внутреннего состояния без большого `if/else`.

## Структура

- **Context** — содержит текущее состояние.
- **State** — интерфейс поведения.
- **ConcreteState** — конкретная реализация состояния.

## Пример на Java

<!-- runnable: StateDemo.java -->
```java
public class StateDemo {
    interface OrderState {
        default OrderState pay() { throw new IllegalStateException("Cannot pay"); }
        default OrderState ship() { throw new IllegalStateException("Cannot ship"); }
        String name();
    }
    static final class Created implements OrderState {
        public OrderState pay() { return new Paid(); }
        public String name() { return "CREATED"; }
    }
    static final class Paid implements OrderState {
        public OrderState ship() { return new Shipped(); }
        public String name() { return "PAID"; }
    }
    static final class Shipped implements OrderState {
        public String name() { return "SHIPPED"; }
    }
    static final class Order {
        private OrderState state = new Created();
        void pay() { state = state.pay(); }
        void ship() { state = state.ship(); }
        String status() { return state.name(); }
    }
    public static void main(String[] args) {
        Order order = new Order();
        try {
            order.ship();
            throw new AssertionError("Shipping before payment was allowed");
        } catch (IllegalStateException expected) {
            if (!order.status().equals("CREATED")) throw new AssertionError("State changed");
        }
        order.pay();
        order.ship();
        if (!order.status().equals("SHIPPED")) throw new AssertionError("Wrong final state");
        System.out.println(order.status());
    }
}
```

Сохраните код как `StateDemo.java`, выполните `java StateDemo.java` на JDK 17+. Ожидаемый вывод: `SHIPPED`. Автоматическая проверка из корня: `python3 scripts/check-java-examples.py`.


## Сравнение со Strategy

State меняет поведение из-за жизненного цикла объекта, а Strategy — из-за выбранного алгоритма.

`Order` — Context; реализации `OrderState` определяют допустимые действия. Закрытое поле состояния не позволяет произвольно поставить `SHIPPED`. Ошибка перехода возникает до присваивания, поэтому состояние сохраняется. Пример моделирует локальный жизненный цикл и не выполняет реальный платёж.

## Практическое упражнение

Добавьте `CANCELLED`: отмена разрешена только из `CREATED`. Составьте таблицу для действий `pay`, `ship`, `cancel` во всех состояниях и проверьте каждый запрещённый переход.

**Критерий:** после отказа исходное состояние неизменно; оплаченный заказ нельзя отменить по правилу этого упражнения; отправленный нельзя оплатить повторно.

**Разбор:** это однопоточная модель. При конкурентных командах переходы требуют общей синхронизации либо атомарного обновления по версии в БД. State не обеспечивает транзакционность внешних эффектов сам по себе.

## Вопросы для самопроверки

1. **Чем State отличается от Strategy?** Состояние определяет разрешённое поведение на этапе жизненного цикла; стратегия выбирает алгоритм выполнения одной задачи.
2. **Всегда ли State лучше switch?** Нет: для маленькой стабильной таблицы переходов enum и явная проверка могут быть проще.
3. **Как обеспечить thread safety?** Согласовать чтение и изменение состояния одной схемой синхронизации; при хранении в БД проверять версию и транзакционную границу.
