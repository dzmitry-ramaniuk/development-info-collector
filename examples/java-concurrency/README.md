---
---

# Проверяемые примеры многопоточности

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Запуск](#запуск)
3. [Что проверяет пример](#что-проверяет-пример)
4. [Границы проверки](#границы-проверки)

## Актуальность материала

- **Дата проверки:** 2026-10-15.
- **Целевая версия:** Java SE 17; проверка компиляции с `--release 17` на JDK 21 и запуск на JDK 21.
- **Статус примера:** `current`; без внешних зависимостей и preview API.
- **Первичные источники:** [AtomicReference](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/atomic/AtomicReference.html), [JLS 17.4.5](https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4.5), [ExecutorService](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ExecutorService.html), [CompletableFuture](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/CompletableFuture.html), [Future](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Future.html).

## Запуск

Из корня репозитория:

```bash
mkdir -p work/java-concurrency-classes
javac --release 17 -d work/java-concurrency-classes examples/java-concurrency/ConcurrencyExamples.java
java -cp work/java-concurrency-classes ConcurrencyExamples
```

Ожидаемый вывод: `OK: CAS, shared lock, shutdown, timeout/cancellation`. Проверки бросают `AssertionError` без флага `-ea`, поэтому ошибка даёт ненулевой код завершения.

## Что проверяет пример

- **CAS-стек:** LIFO, конкурентные push/pop, отсутствие потерь и дубликатов среди 4000 значений. На каждой повторной попытке создаётся новый узел с `final` полями.
- **Кэш с общим монитором:** параллельная запись разных ключей, чтение своих записей, итоговый размер.
- **Graceful shutdown:** завершение пустого пула и обработка всех принятых задач при полной очереди; отказ при переполнении и после shutdown. `CountDownLatch` задаёт нужные состояния без задержек для угадывания порядка.
- **Таймаут и отмена:** future результата получает `TimeoutException`, пока операция остаётся заблокированной; отмена сохранённого underlying `Future<?>` затем вызывает interruption этой операции.

Разбор: [атомарные типы](../../java/multithreading/04-synchronizers.html#атомарные-типы), [Worker Pool](../../java/multithreading/07-patterns.html#worker-pool-pattern), [таймаут результата и отмена](../../java/multithreading/03-async-coordination.html#таймаут-результата-и-отмена-операции).

## Границы проверки

Это конечные проверки конкретных сценариев, а не доказательство всех допустимых межпоточных исполнений и не нагрузочный бенчмарк. CAS-операции линеаризуются в успешном `compareAndSet`; кэш защищён общим монитором; приём задач и shutdown сериализованы одним монитором жизненного цикла. Эти свойства нужно обосновывать независимо от удачного запуска.

Пул — учебная реализация: задачи должны завершаться, немедленная отмена кооперативна, восстановление после `Error` не реализовано. HTTP-сервер не используется; проверяется interruption блокирующей операции с latch. Для сетевого клиента отдельно проверяйте таймаут запроса и его контракт отмены. Запуск на настоящем JDK 17 и jcstress не входит в проверку этого файла.
