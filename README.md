# development-info-collector

## Обзор
`development-info-collector` — это список тематических разделов и материалов, который помогает структурировать процесс изучения технологий разработки.

## Разделы технологий

### Java

#### Java Core
- Обзор виртуальной машины Java (JVM), JRE и JDK, устройство процесса компиляции и запуска.
- Основы языка: типы данных, управление памятью, система пакетов и модульность (JPMS).
- Объектно-ориентированное программирование: классы, интерфейсы, инкапсуляция, наследование, полиморфизм.
- Коллекции Java: иерархия коллекций, отличие `List`, `Set`, `Map`, использование `Collections` и `Arrays` utility-классов.
- Потоки ввода/вывода и работа с файлами, NIO.2, сериализация.
- Обработка исключений и работа с логированием.
- Современные возможности языка: generics, lambda-выражения, Stream API, Optional, Date and Time API.

#### Multithreading
- Модель памяти Java и ключевые слова `volatile`, `synchronized`, правила happens-before.
- Создание и управление потоками: `Thread`, `Runnable`, `Callable`, пул потоков и `ExecutorService`.
- Высокоуровневые средства конкурентности: `Future`, `CompletableFuture`, `ForkJoinPool`, параллельные стримы.
- Средства синхронизации: `Lock`, `ReadWriteLock`, `ReentrantLock`, синхронизаторы (`CountDownLatch`, `CyclicBarrier`, `Semaphore`).
- Безопасные коллекции и классы пакета `java.util.concurrent` (ConcurrentHashMap, CopyOnWriteArrayList и т.п.).
- Диагностика и отладка многопоточных приложений, предотвращение deadlock, livelock, starvation.
- Паттерны многопоточности и рекомендации по тестированию конкурентного кода.

