---
---

# Управление памятью и сборка мусора

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Архитектура памяти JVM](#архитектура-памяти-jvm)
   - [Heap (Куча)](#heap-куча)
   - [Stack (Стек)](#stack-стек)
   - [Metaspace](#metaspace)
   - [PC Register](#pc-register)
   - [Native Method Stack](#native-method-stack)
   - [Code Cache](#code-cache)
   - [Direct Memory](#direct-memory)
3. [Структура Heap подробно](#структура-heap-подробно)
   - [Young Generation](#young-generation)
   - [Old Generation (Tenured)](#old-generation-tenured)
   - [TLAB (Thread Local Allocation Buffer)](#tlab-thread-local-allocation-buffer)
4. [Работа сборки мусора](#работа-сборки-мусора)
   - [Minor GC](#minor-gc)
   - [Old-generation и Full GC](#old-generation-и-full-gc)
   - [Алгоритмы сборки мусора](#алгоритмы-сборки-мусора)
   - [Stop-the-World](#stop-the-world)
5. [Модель памяти JVM](#модель-памяти-jvm)
6. [Типы сборщиков мусора](#типы-сборщиков-мусора)
   - [Serial GC](#serial-gc)
   - [Parallel GC](#parallel-gc)
   - [CMS (Concurrent Mark-Sweep)](#cms-concurrent-mark-sweep)
   - [G1 (Garbage First)](#g1-garbage-first)
   - [ZGC](#zgc)
   - [Shenandoah](#shenandoah)
   - [Сравнение сборщиков мусора](#сравнение-сборщиков-мусора)
7. [Жизненный цикл объекта](#жизненный-цикл-объекта)
8. [Настройка памяти и диагностика](#настройка-памяти-и-диагностика)
9. [Частые проблемы и подходы](#частые-проблемы-и-подходы)
10. [Практические советы](#практические-советы)
11. [Практические упражнения](#практические-упражнения)
12. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Актуальность материала

- **Дата проверки:** 2026-10-15 — точечная сверка различия JMM/runtime, поколений и Full GC, ограничений рекомендаций по G1 и диагностики; полная матрица всех GC-флагов не проверялась.
- **Целевая версия или диапазон:** HotSpot 21 для основных рекомендаций; CMS и старый формат логов — исторический материал.
- **Статус примеров:** `current` для рекомендаций; `legacy` для CMS/старых логов. Нагрузочные измерения и все приведённые GC-флаги не запускались.
- **Первичные источники:** [G1 tuning JDK 21](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-garbage-collector-tuning.html); [java command JDK 21](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html); [System.gc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/System.html#gc()).

## Архитектура памяти JVM

JVM управляет несколькими областями памяти, каждая из которых имеет своё назначение и характеристики.
Ниже схемы Eden/S0/S1 и алгоритм копирования иллюстрируют поколенческие Serial/Parallel в HotSpot.
G1 использует регионы, а устройство поколений ZGC зависит от версии и режима. Не переносите параметры одного GC на другой.
Для G1 начните с размеров heap и цели пауз: фиксированный young через `-Xmn`/`NewRatio` мешает его адаптивному управлению.
См. [G1 tuning JDK 21](https://docs.oracle.com/en/java/javase/21/gctuning/garbage-first-garbage-collector-tuning.html).

### Heap (Куча)

**Heap** — основная область памяти для размещения объектов. Все объекты и массивы создаются в куче и управляются сборщиком мусора.

**Характеристики:**
- Разделяется между всеми потоками приложения
- Размер настраивается параметрами `-Xms` (начальный) и `-Xmx` (максимальный)
- Может делиться на поколения: конкретная структура зависит от сборщика и версии
- Управляется Garbage Collector

**Структура Heap:**
```text
┌─────────────────────────────────────────────────────────┐
│                        HEAP                              │
├───────────────────────────┬─────────────────────────────┤
│   Young Generation        │   Old Generation (Tenured)  │
│  ┌──────┬──────┬──────┐  │                             │
│  │ Eden │ S0   │ S1   │  │                             │
│  └──────┴──────┴──────┘  │                             │
└───────────────────────────┴─────────────────────────────┘
```

**Параметры настройки (первые два общие; young/survivor — для подходящего поколенческого GC):**
```bash
# Начальный и максимальный размер heap
-Xms2g -Xmx4g

# Размер Young Generation
-Xmn1g
-XX:NewSize=512m
-XX:MaxNewSize=1g

# Соотношение Young к Old
-XX:NewRatio=2  # Old = 2 * Young

# Размер Survivor spaces
-XX:SurvivorRatio=8  # Eden = 8 * Survivor
```

### Stack (Стек)

**Stack** — область памяти для каждого потока, хранящая локальные переменные, параметры методов и адреса возврата.

**Характеристики:**
- Каждый поток имеет свой собственный стек
- Создаётся при запуске потока, удаляется при его завершении
- Размер фиксирован и настраивается параметром `-Xss`
- Работает по принципу LIFO (Last In, First Out)
- Не управляется GC — память освобождается автоматически при выходе из метода

**Структура Stack Frame:**
```text
┌─────────────────────────┐
│   Thread Stack          │
├─────────────────────────┤
│  Frame N (current)      │
│  ┌───────────────────┐  │
│  │ Local Variables   │  │  <- Локальные переменные и параметры метода
│  ├───────────────────┤  │
│  │ Operand Stack     │  │  <- Стек операндов для вычислений
│  ├───────────────────┤  │
│  │ Frame Data        │  │  <- Ссылка на constant pool, return address
│  └───────────────────┘  │
├─────────────────────────┤
│  Frame N-1              │
├─────────────────────────┤
│  Frame N-2              │
└─────────────────────────┘
```

**Пример содержимого Stack Frame:**
```java
public class StackExample {
    public int calculate(int a, int b) {
        int sum = a + b;
        int result = multiply(sum, 2);
        return result;
    }

    private int multiply(int x, int y) {
        return x * y;
    }
}

// Stack для метода calculate(5, 3):
// Frame: calculate
//   Local Variables: [this, a=5, b=3, sum=8, result=16]
//   Operand Stack: [...промежуточные вычисления...]
//   Frame Data: [return address, constant pool reference]
//
// Frame: multiply (вложенный вызов)
//   Local Variables: [this, x=8, y=2]
//   Operand Stack: [16]
//   Frame Data: [return address to calculate]
```

**Параметры настройки:**
```bash
# Размер стека для каждого потока (по умолчанию ~1MB)
-Xss512k    # Уменьшить для экономии памяти
-Xss2m      # Увеличить для глубокой рекурсии
```

**StackOverflowError:**
```java
// Возникает при превышении размера стека (обычно из-за бесконечной рекурсии)
public void recursiveMethod() {
    recursiveMethod();  // Бесконечная рекурсия
}
// Exception in thread "main" java.lang.StackOverflowError
```

**Что хранится в Stack:**
- Примитивные локальные переменные (`int`, `long`, `boolean`, etc.)
- Ссылки на объекты в Heap
- Промежуточные результаты вычислений
- Адреса возврата из методов

**Что НЕ хранится в Stack:**
- Объекты (они всегда в Heap)
- Статические переменные (они в Metaspace)

### Metaspace

**Metaspace** (ранее Permanent Generation в Java 7 и ниже) — область памяти для метаданных классов.

**Характеристики:**
- Появился в Java 8, заменил PermGen
- Использует native memory (не часть Heap)
- Автоматически расширяется по мере необходимости
- Выделяется native-аллокатором; освобождение метаданных связано с выгрузкой классов и работой GC

**Что хранится в Metaspace:**
- Метаданные классов (структура классов, методы, поля)
- Метаданные runtime constant pool (ссылки на объекты не делают сами объекты частью metaspace)
- Аннотации runtime

**Параметры настройки:**
```bash
# Начальный порог для GC по использованию metaspace, а не заранее выделяемый объём
-XX:MetaspaceSize=128m

# Максимальный размер Metaspace
-XX:MaxMetaspaceSize=512m

# Минимальная ёмкость для классов
-XX:MinMetaspaceExpansion=1m
```

**Сравнение с PermGen:**

| Характеристика | PermGen (до Java 8) | Metaspace (Java 8+) |
|----------------|---------------------|---------------------|
| Тип памяти | Heap memory | Native memory |
| Размер по умолчанию | Фиксированный (~64-85MB) | Неограничен (зависит от ОС) |
| Настройка | `-XX:PermSize`, `-XX:MaxPermSize` | `-XX:MetaspaceSize`, `-XX:MaxMetaspaceSize` |
| Автоматическое расширение | Ограничено | Да |
| OutOfMemoryError | `PermGen space` | `Metaspace` |

**OutOfMemoryError: Metaspace:**
```bash
# Причины: удержание classloader, слишком много классов, runtime-генерация классов.
# Для VM.native_memory включите NMT заранее: -XX:NativeMemoryTracking=summary
# Диагностика:
jcmd <pid> VM.native_memory summary
jcmd <pid> GC.class_histogram
```

### PC Register

**PC Register** (Program Counter Register) — область памяти, хранящая адрес текущей исполняемой инструкции JVM.

**Характеристики:**
- Каждый поток имеет свой PC Register
- Очень маленький размер (один machine word)
- Содержит адрес bytecode инструкции
- Для native методов содержит `undefined` value

**Назначение:**
- Отслеживание текущей позиции выполнения в потоке
- Переключение контекста между потоками
- Возврат из методов

### Native Method Stack

**Native Method Stack** — стек для выполнения native (JNI) методов.

**Характеристики:**
- Создаётся для каждого потока, вызывающего native методы
- Используется при вызове C/C++ кода через JNI
- Размер и структура зависят от реализации JVM
- Управляется native кодом, а не JVM

**Пример использования:**
```java
public class NativeExample {
    // Native метод (реализован в C/C++)
    public native void nativeMethod();

    static {
        System.loadLibrary("native-lib");
    }
}
```

**StackOverflowError в Native Stack:**
```java
// Может возникнуть при глубокой рекурсии в native коде
// или при большом количестве вложенных JNI вызовов
```

### Code Cache

**Code Cache** — область памяти для хранения скомпилированного машинного кода (JIT-компиляция).

**Характеристики:**
- Хранит native код, созданный JIT-компиляторами (C1, C2)
- Размер ограничен и не расширяется динамически
- При заполнении JVM прекращает компиляцию (падает производительность)

**Параметры настройки:**
```bash
# Размер Code Cache (по умолчанию ~240MB)
-XX:ReservedCodeCacheSize=512m

# Начальный размер
-XX:InitialCodeCacheSize=128m

# Информация о Code Cache
-XX:+PrintCodeCache
```

**Мониторинг:**
```bash
# Просмотр использования Code Cache
jcmd <pid> Compiler.codecache

# В JConsole/VisualVM: Memory -> Code Cache
```

### Direct Memory

**Direct Memory** — область памяти вне Heap, используемая для NIO buffers.

**Характеристики:**
- Выделяется через `ByteBuffer.allocateDirect()`
- Находится вне Heap, не управляется GC
- Быстрее для I/O операций (избегает копирования)
- Освобождается через механизм Cleaner/PhantomReference

**Параметры настройки:**
```bash
# Максимальный размер Direct Memory
-XX:MaxDirectMemorySize=1g
```

**Пример использования:**
```java
// Heap-based buffer (медленнее для I/O)
ByteBuffer heapBuffer = ByteBuffer.allocate(1024);

// Direct buffer (быстрее для I/O)
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);

// Использование в NIO
FileChannel channel = new FileInputStream("file.txt").getChannel();
channel.read(directBuffer);  // Эффективное чтение без промежуточного копирования
```

**Преимущества Direct Memory:**
- Избегает копирования данных между Java Heap и native memory
- Производительнее для сетевых и файловых операций
- Память может быть доступна нативным библиотекам

**Недостатки:**
- Выделение и освобождение медленнее, чем для Heap
- Не управляется GC напрямую
- Потенциальные утечки памяти при неправильном использовании

## Структура Heap подробно

### Young Generation

**Young Generation** — область для новых объектов, состоит из Eden и двух Survivor spaces.

**Структура:**
```text
Young Generation
├── Eden Space (80% от Young by default)
│   └── Новые объекты создаются здесь
├── Survivor Space 0 (S0) (10%)
│   └── Выжившие объекты после GC
└── Survivor Space 1 (S1) (10%)
    └── Выжившие объекты копируются между S0/S1
```

**Процесс работы:**

1. **Выделение объектов:**
```java
// Новые объекты создаются в Eden
Object obj1 = new Object();  // Создаётся в Eden
Object obj2 = new Object();  // Создаётся в Eden
```

2. **Minor GC (когда Eden заполняется):**
```text
Before Minor GC:
┌─────────────┬─────┬─────┐
│ Eden (FULL) │ S0  │ S1  │
└─────────────┴─────┴─────┘

After Minor GC:
┌─────────────┬─────────┬─────┐
│ Eden (empty)│ S0      │ S1  │ <- Выжившие объекты скопированы в S0
└─────────────┴─────────┴─────┘
```

3. **Копирование между Survivor spaces:**
```text
GC #1: Eden -> S0
GC #2: Eden + S0 -> S1
GC #3: Eden + S1 -> S0
...
```

**Зачем два Survivor spaces?**
- Избегают фрагментации памяти
- Всегда одна из spaces пуста и готова принять выживших объектов
- Реализуют copying algorithm эффективно

**Age (возраст объекта):**
```java
// Каждый объект имеет счётчик возраста (age)
// При каждом Minor GC, который переживает объект, age++
// Когда age достигает MaxTenuringThreshold, объект перемещается в Old Generation

// Default: -XX:MaxTenuringThreshold=15
```

**Параметры настройки:**
```bash
# Размер Young Generation
-Xmn512m

# Соотношение Eden к Survivor
-XX:SurvivorRatio=8  # Eden = 8 * (S0 + S1)

# Порог для продвижения в Old Gen
-XX:MaxTenuringThreshold=15

# Целевое использование Survivor
-XX:TargetSurvivorRatio=50
```

### Old Generation (Tenured)

**Old Generation** — область для долгоживущих объектов, которые пережили несколько Minor GC.

**Характеристики:**
- Размер и политика сбора зависят от GC и эргономики
- В Serial/Parallel полная сборка затрагивает old generation; в G1 old-регионы также очищаются mixed-сборками
- Длительность зависит от live set и алгоритма; old-generation collection нельзя автоматически считать синонимом Full GC

**Когда объекты попадают в Old Generation:**

1. **Promotion по возрасту:**
```java
// Объект пережил MaxTenuringThreshold Minor GC
Object longLiving = new Object();
// Возраст и заполнение survivor влияют на продвижение; значение 15 не гарантирует точный срок.
```

2. **Большие объекты:**
```bash
# Историческая/collector-specific настройка; не общий порог крупных объектов для всех GC.
# Перед использованием проверьте поддержку выбранным сборщиком и JDK:
-XX:PretenureSizeThreshold=1m
```

3. **Переполнение Survivor:**
```java
// Если Survivor переполнен, объекты сразу идут в Old Gen
```

**Схема уплотнения old generation в полном STW-цикле:**
```text
Before Major GC:
┌─────────────────────────────────┐
│ Old Generation (85% full)        │
│ ████████████████████░░░░░░░     │
└─────────────────────────────────┘

After Major GC:
┌─────────────────────────────────┐
│ Old Generation (40% full)        │
│ ███████░░░░░░░░░░░░░░░░░░░░░    │ <- Мёртвые объекты удалены, живые уплотнены
└─────────────────────────────────┘
```

**Проблемы с Old Generation:**
- **Фрагментация:** Память разбита на мелкие куски
- **Длинные паузы:** Full GC может занимать секунды
- **Promotion failure:** Не хватает места для продвижения объектов

### TLAB (Thread Local Allocation Buffer)

**TLAB** — небольшой буфер в Eden для каждого потока, позволяющий быстро выделять объекты без синхронизации.

**Принцип работы:**
```text
┌────────────────────────────────────────────┐
│              Eden Space                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ TLAB     │  │ TLAB     │  │ TLAB     │ │
│  │ Thread 1 │  │ Thread 2 │  │ Thread 3 │ │
│  └──────────┘  └──────────┘  └──────────┘ │
│  ┌──────────────────────────────────────┐  │
│  │ Shared Eden (для больших объектов)   │  │
│  └──────────────────────────────────────┘  │
└────────────────────────────────────────────┘
```

**Алгоритм выделения:**
```java
Object obj = new MyObject();

// 1. Попытка выделить в TLAB текущего потока (fast path)
if (TLAB.hasSpace(objectSize)) {
    return TLAB.allocate(objectSize);  // Без синхронизации!
}

// 2. TLAB заполнен -> запросить новый TLAB
if (TLAB.requestNew()) {
    return TLAB.allocate(objectSize);
}

// 3. Объект слишком большой -> выделить в shared Eden (slow path)
return Eden.allocateShared(objectSize);  // С синхронизацией
```

**Преимущества TLAB:**
- Быстрое выделение без блокировок (lock-free)
- Уменьшение конкуренции между потоками
- Лучшая locality (объекты одного потока рядом в памяти)

**Параметры настройки:**
```bash
# Включить/выключить TLAB (включен по умолчанию)
-XX:+UseTLAB
-XX:-UseTLAB

# Размер TLAB
-XX:TLABSize=256k

# Процент Eden для TLAB
-XX:TLABRefillWasteFraction=64  # 1/64 от Eden
```

**Статистика TLAB:**
```bash
# Вывод статистики TLAB
-XX:+PrintTLAB

# Пример вывода:
# TLAB: gc thread: 0x00007f8c4c001000 [id=12345] desired_size: 131KB slow allocs: 0  refill waste: 0KB alloc: 0.98 128KB refills: 1 waste  0.7% gc: 0B slow: 0B fast: 0B
```

## Работа сборки мусора

### Minor GC

**Minor GC** — сборка мусора в Young Generation, происходит часто и быстро.

**Типичный триггер Minor/Young GC:** недостаток места для выделения в молодом поколении.
`System.gc()` запрашивает работу GC, но не обещает именно young-сборку или завершение сборки к возврату метода.

**Алгоритм (Copying):**
```text
1. Stop-the-World (остановка всех потоков приложения)
2. Поиск живых объектов в Eden и S0 (mark)
3. Копирование живых объектов в S1 (copy)
4. Очистка Eden и S0 (становятся пустыми)
5. Увеличение age выживших объектов
6. Promotion объектов со старым age в Old Generation
7. Resume (возобновление работы приложения)
```

**Пример Minor GC:**
```text
Before Minor GC #1:
Eden: [obj1, obj2, obj3, obj4]  <- obj2 и obj4 мёртвые
S0:   []
S1:   []

After Minor GC #1:
Eden: []
S0:   [obj1(age=1), obj3(age=1)]  <- Живые объекты скопированы
S1:   []

Before Minor GC #2:
Eden: [obj5, obj6, obj7]  <- obj6 мёртвый
S0:   [obj1(age=1), obj3(age=1)]
S1:   []

After Minor GC #2:
Eden: []
S0:   []
S1:   [obj1(age=2), obj3(age=2), obj5(age=1), obj7(age=1)]  <- Копирование в другой Survivor
```

**Временные характеристики:**
- Длительность: зависит от живых данных, скорости CPU, сборщика и нагрузки; фиксированного диапазона нет
- Частота: зависит от скорости создания объектов (секунды/минуты)

**Параметры мониторинга:**
```bash
# Логи Minor GC
-Xlog:gc*=info

# Пример вывода:
# [0.234s][info][gc] GC(0) Pause Young (Normal) (G1 Evacuation Pause) 24M->8M(64M) 2.123ms
```

### Old-generation и Full GC

**Сборка старшего поколения** и **Full GC** — не синонимы. Например, G1 может очищать old-регионы в mixed-сборках,
а Full GC в HotSpot означает полный цикл, который может затронуть всю кучу. Термин «major GC» неоднозначен: уточняйте сборщик
и конкретное событие в логах. Далее показан схематический полный STW mark/sweep/compact цикл, а не универсальный алгоритм всех GC.

**Возможные причины полного цикла (зависят от GC и конфигурации):**
- Old Generation заполнен
- Metaspace заполнен
- Явный вызов `System.gc()`
- Promotion failure (нет места для объектов из Young Gen)
- CMS failure (для CMS GC)

**Алгоритм (Mark-Sweep-Compact):**
```text
1. Stop-the-World
2. Mark: Пометить все живые объекты
   ├── Поиск GC roots (stack, static variables, JNI handles)
   ├── Обход графа объектов
   └── Пометка достижимых объектов
3. Sweep: Удалить мёртвые объекты
   └── Освобождение памяти мёртвых объектов
4. Compact: Уплотнение памяти (optional, зависит от GC)
   ├── Перемещение живых объектов к началу памяти
   └── Обновление ссылок
5. Resume
```

**Иллюстрация полного цикла:**
```text
Before Major GC:
Old Gen: [objA, DEAD, objB, DEAD, DEAD, objC, DEAD, objD]
         ████░░░░██░░░░░░░░███░░░░███     (фрагментирована)

After Mark:
         [objA, DEAD, objB, DEAD, DEAD, objC, DEAD, objD]
          ^^^^         ^^^^               ^^^^        ^^^^  <- Помечены как живые

After Sweep:
         [objA, ____, objB, ____, ____, objC, ____, objD]
          ^^^^         ^^^^               ^^^^        ^^^^

After Compact:
         [objA, objB, objC, objD, ____, ____, ____, ____]
          ████████████████████░░░░░░░░░░░░░░░░░░░░  <- Уплотнено, без фрагментации
```

**Временные характеристики:**
- Длительность: может существенно влиять на latency; измеряйте на конкретном heap и нагрузке
- Частота: минуты/часы (зависит от объёма памяти и нагрузки)

**Возможные издержки полного STW-цикла:**
- Длинные паузы приложения (stop-the-world)
- Влияние на latency критичных приложений
- Фрагментация памяти

### Алгоритмы сборки мусора

#### 1. Mark-and-Sweep

**Описание:** Помечает живые объекты и удаляет остальные.

**Фазы:**
1. Mark: Обход графа объектов от GC roots
2. Sweep: Проход по памяти и удаление непомеченных

**Проблемы:**
- Фрагментация памяти
- Необходимость stop-the-world

#### 2. Copying (Копирование)

**Описание:** Копирует живые объекты в новую область, старая очищается полностью.

**Используется в:** Young Generation (Minor GC)

**Преимущества:**
- Нет фрагментации
- Быстрая очистка (просто сбросить указатель)
- Живые объекты компактны в памяти

**Недостатки:**
- Требует дополнительное пространство (Survivor)
- Копирование объектов затратно

#### 3. Mark-and-Compact

**Описание:** Помечает живые объекты и перемещает их к началу памяти.

**Используется в:** некоторых вариантах обработки old generation; конкретные фазы зависят от GC

**Фазы:**
1. Mark: Пометка живых объектов
2. Compact: Перемещение живых объектов к началу
3. Update references: Обновление всех ссылок

**Преимущества:**
- Нет фрагментации
- Не требует дополнительное пространство

**Недостатки:**
- Медленнее copying (требуется несколько проходов)
- Stop-the-world

### Stop-the-World

**Stop-the-World (STW)** — пауза приложения, когда все прикладные потоки останавливаются для выполнения GC.

**Причины STW:**
- Безопасность: предотвращение изменения графа объектов во время GC
- Согласованность: гарантия корректности ссылок
- Простота: упрощение реализации GC

**Типы STW пауз:**

1. **Young GC pause:** 10-50 мс
2. **Full GC pause:** 100-5000+ мс (зависит от размера Heap)
3. **Remark pause (CMS/G1):** 10-100 мс
4. **Final mark pause:** варьируется

**Safepoint:**
```java
// Точки в коде, где JVM может безопасно остановить поток:
// - Вызовы методов
// - Обратные ветви циклов
// - Возврат из native методов

for (int i = 0; i < 1000000; i++) {
    // JVM может вставить safepoint poll здесь
    doWork();
}
```

**Минимизация STW:**
- Использование concurrent GC (G1, ZGC, Shenandoah)
- Настройка размера Heap
- Оптимизация создания объектов
- Tuning GC параметров

## Модель памяти JVM

**Java Memory Model (JMM)** задаёт допустимую видимость изменений, порядок операций и happens-before между потоками.
Это не карта областей памяти процесса: heap, stacks, metaspace и code cache описывают устройство runtime.
Подробный разбор JMM находится в [главе о видимости](../multithreading/01-jmm-visibility.html).

## Типы сборщиков мусора

JVM предлагает несколько сборщиков мусора, каждый оптимизирован для различных сценариев использования.

### Serial GC

**Serial GC** — простейший однопоточный сборщик мусора.

**Характеристики:**
- Использует один поток для всех операций GC
- Stop-the-world для всех типов сборки
- Минимальное потребление памяти

**Активация:**
```bash
-XX:+UseSerialGC
```

**Алгоритмы:**
- Young Generation: Copying
- Old Generation: Mark-Sweep-Compact

**Когда использовать:**
- Однопроцессорные системы
- Небольшие приложения (heap < 100 MB)
- Клиентские приложения
- Встроенные системы с ограниченными ресурсами

**Иллюстрация старого формата логов (legacy, не вывод JDK 21 с `-Xlog`):**
```text
[GC (Allocation Failure) [DefNew: 8192K->512K(9216K), 0.0123456 secs]
    8192K->3072K(19456K), 0.0124567 secs]
```

### Parallel GC

**Parallel GC** (Throughput Collector) — многопоточный сборщик для максимальной пропускной способности.

**Характеристики:**
- Использует несколько потоков для Young и Old GC
- Stop-the-world, но параллельная обработка
- Оптимизирован для throughput (общее время выполнения)
- По умолчанию в Java 8

**Активация:**
```bash
-XX:+UseParallelGC
```

**Параметры настройки:**
```bash
# Количество потоков GC; значение по умолчанию выбирает эргономика JVM
-XX:ParallelGCThreads=4

# Целевая пауза (миллисекунды)
-XX:MaxGCPauseMillis=200

# Отношение времени приложения ко времени GC: целевая доля GC = 1 / (1 + ratio)
-XX:GCTimeRatio=99  # 1% времени на GC, 99% на приложение

# Адаптивная настройка размеров
-XX:+UseAdaptiveSizePolicy
```

**Алгоритмы:**
- Young Generation: Parallel Copying
- Old Generation: Parallel Mark-Compact

**Когда использовать:**
- Batch обработка
- Научные вычисления
- Приложения, где важна пропускная способность, а не задержки
- Многопроцессорные системы

**Иллюстрация старого формата логов (legacy, не вывод JDK 21 с `-Xlog`):**
```text
[GC (Allocation Failure) [PSYoungGen: 65536K->8192K(76288K)]
    65536K->16384K(251392K), 0.0234567 secs]
[Full GC (Ergonomics) [PSYoungGen: 8192K->0K(76288K)]
    [ParOldGen: 131072K->65536K(175104K)] 139264K->65536K(251392K),
    [Metaspace: 8192K->8192K(1056768K)], 0.1234567 secs]
```

### CMS (Concurrent Mark-Sweep)

**CMS** — низкопаузный concurrent сборщик для Old Generation.

> **Важно**: Устарел (deprecated) с Java 9 и удалён с Java 14. Заменён на G1 GC.

**Характеристики:**
- Concurrent: большая часть работы происходит параллельно с приложением
- Минимизирует паузы
- Не выполняет compaction (может привести к фрагментации)
- Требует больше CPU и памяти

**Активация:**
```bash
-XX:+UseConcMarkSweepGC
```

**Фазы CMS:**
```text
1. Initial Mark (STW):           ~50ms   <- Короткая пауза
2. Concurrent Mark:               ~2s     <- Параллельно с приложением
3. Concurrent Preclean:           ~100ms  <- Параллельно с приложением
4. Remark (STW):                  ~100ms  <- Короткая пауза
5. Concurrent Sweep:              ~1s     <- Параллельно с приложением
6. Concurrent Reset:              ~50ms   <- Параллельно с приложением
```

**Параметры настройки:**
```bash
# Порог для запуска CMS (% Old Gen)
-XX:CMSInitiatingOccupancyFraction=70

# Количество concurrent GC потоков
-XX:ConcGCThreads=2

# Включить compaction после N full GC
-XX:CMSFullGCsBeforeCompaction=5
```

**Проблемы CMS:**
- **Concurrent Mode Failure:** Old Gen заполнился до окончания CMS → Full GC
- **Promotion Failure:** Нет места для продвижения из Young Gen → Full GC
- **Фрагментация памяти:** Отсутствие compaction

**Когда использовал(ся):**
- Низколатентные приложения
- Веб-серверы
- Системы реального времени

### G1 (Garbage First)

**G1** — региональный сборщик, баланс между throughput и паузами. По умолчанию в серверных конфигурациях HotSpot начиная с Java 9; фактический выбор проверьте в своей JVM.

**Характеристики:**
- Делит heap на регионы (~2048 регионов)
- Выбирает регионы с наибольшим мусором (garbage first)
- Мягкая цель пауз; `MaxGCPauseMillis` не является жёсткой гарантией latency
- Concurrent и incremental сборка

**Активация:**
```bash
-XX:+UseG1GC  # По умолчанию в Java 9+
```

**Структура Heap в G1:**
```text
┌─────────────────────────────────────────────────────────────┐
│                         G1 Heap                              │
│ ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐ │
│ │ E │ E │ E │ S │ S │ O │ O │ O │ O │ H │ H │ E │ E │ F │ │
│ └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘ │
│   E = Eden                                                   │
│   S = Survivor                                               │
│   O = Old                                                    │
│   H = Humongous (большие объекты, > 50% от размера региона) │
│   F = Free                                                   │
└─────────────────────────────────────────────────────────────┘
```

**Параметры настройки:**
```bash
# Целевая пауза (миллисекунды)
-XX:MaxGCPauseMillis=200

# Размер региона (автоматически вычисляется, но можно задать)
-XX:G1HeapRegionSize=4m  # 1m-32m, должно быть степенью 2

# Порог для инициации Concurrent Mark
-XX:InitiatingHeapOccupancyPercent=45

# Количество concurrent marking потоков
-XX:ConcGCThreads=2

# Количество parallel GC потоков
-XX:ParallelGCThreads=8
```

**Типы GC в G1 (ниже legacy-формат иллюстраций; в JDK 21 используйте `-Xlog:gc*`):**

1. **Young GC (Evacuation Pause):**
```text
[GC pause (G1 Evacuation Pause) (young), 0.0234567 secs]
   [Parallel Time: 20.1 ms, GC Workers: 8]
   [Eden: 512M(512M)->0B(480M) Survivors: 32M->64M Heap: 1024M(2048M)->544M(2048M)]
```

2. **Mixed GC:**
```text
[GC pause (G1 Evacuation Pause) (mixed), 0.0456789 secs]
   # Очищает Eden + часть Old regions
```

3. **Full GC (редко, при ошибках):**
```text
[Full GC (Allocation Failure) 2047M->1024M(2048M), 1.2345678 secs]
```

**Фазы Concurrent Cycle:**
```text
1. Initial Mark (STW, piggyback on Young GC)
2. Root Region Scan (concurrent)
3. Concurrent Mark (concurrent)
4. Remark (STW, короткая)
5. Cleanup (частично STW, частично concurrent)
6. Copying/Cleanup (STW, в Mixed GC)
```

**Когда использовать:**
- Нагрузки, где нужен баланс пауз и throughput; нижняя граница heap не является обязательным требованием
- Нужен баланс между throughput и latency
- Важен контроль пауз, подтверждённый измерениями
- Универсальное решение для большинства приложений

**Преимущества:**
- Настраиваемая мягкая цель пауз через `MaxGCPauseMillis`
- Хорошая производительность на больших heap
- Автоматическая адаптация к нагрузке
- Меньше фрагментации

**Недостатки:**
- Больше CPU overhead (concurrent работа)
- Сложнее в tuning
- Full GC всё ещё возможен (но редок)

### ZGC

**ZGC** — concurrent сборщик с целью коротких пауз; конкретная задержка требует измерений и не гарантирована API. В JDK 21 есть поколенческий и непоколенческий режимы; перед обновлением сверяйте режим и флаги с целевым JDK.

**Характеристики:**
- Основная работа выполняется конкурентно, что уменьшает зависимость STW-пауз от размера heap
- Concurrent compaction
- Colored pointers (использует биты указателя для метаданных)
- NUMA-aware
- Поддерживает Heap до 16TB

**Активация:**
```bash
-XX:+UseZGC
-Xmx16g  # Размер Heap
```

**Параметры настройки:**
```bash
# Количество concurrent threads
-XX:ConcGCThreads=4

# Включить возврат памяти ОС
-XX:+ZUncommit
-XX:ZUncommitDelay=300  # Секунды перед возвратом памяти

# NUMA support
-XX:+UseNUMA
```

**Colored Pointers:**
```text
64-bit указатель:
┌────────────┬──────┬────────────────────────────────────────┐
│ Unused (4) │Marks │ Object Address (44 bits)               │
│    bits    │(4bit)│                                        │
└────────────┴──────┴────────────────────────────────────────┘

Marks биты используются для:
- Finalizabled
- Remapped
- Marked 0/1 (для фаз marking)
```

**Фазы ZGC:**
```text
1. Pause Mark Start (STW):         <1ms
2. Concurrent Mark                  ~concurrent
3. Pause Mark End (STW):            <1ms
4. Concurrent Prepare for Relocate  ~concurrent
5. Pause Relocate Start (STW):      <1ms
6. Concurrent Relocate              ~concurrent
```

**Иллюстрация старого формата логов (legacy, не вывод JDK 21 с `-Xlog`):**
```text
[0.123s][info][gc] GC(0) Pause Mark Start 0.012ms
[0.234s][info][gc] GC(0) Concurrent Mark 123.456ms
[0.235s][info][gc] GC(0) Pause Mark End 0.008ms
[0.345s][info][gc] GC(0) Concurrent Relocate 234.567ms
```

**Когда использовать:**
- Требования к низким задержкам (< 10ms)
- Большие Heap (> 8GB)
- Cloud native приложения
- Микросервисы с SLA по latency

**Преимущества:**
- Предсказуемо низкие паузы
- Масштабируется до очень больших Heap
- Concurrent compaction (нет фрагментации)
- Возврат неиспользуемой памяти ОС

**Недостатки:**
- Требует Java 11+ (production Java 15+)
- Больше CPU overhead
- Больше memory overhead (colored pointers, metadata)
- Linux/macOS only (Windows с Java 14+, production Java 15+)

### Shenandoah

**Shenandoah** — еще один low-latency сборщик с паузами < 10ms (Java 12+).

**Характеристики:**
- Concurrent evacuation
- Паузы не зависят от размера Heap
- Brooks pointers (forwarding pointer)
- Работает на любом размере Heap

**Активация:**
```bash
-XX:+UseShenandoahGC
```

**Параметры настройки:**
```bash
# Эвристика для запуска GC
-XX:ShenandoahGCHeuristics=adaptive  # adaptive, static, compact, aggressive

# Включить возврат памяти ОС
-XX:+ShenandoahUncommit
-XX:ShenandoahUncommitDelay=5000  # Миллисекунды

# Количество concurrent потоков
-XX:ConcGCThreads=4
```

**Brooks Pointers:**
```text
Каждый объект имеет forwarding pointer в начале:
┌──────────────────┬──────────────────┐
│ Forwarding Ptr   │ Object Data      │
└──────────────────┴──────────────────┘

Используется для перемещения объектов concurrent:
- Во время evacuation обновляется forwarding pointer
- Чтения проходят через forwarding pointer (read barrier)
```

**Фазы Shenandoah:**
```text
1. Init Mark (STW):                <1ms
2. Concurrent Marking               ~concurrent
3. Final Mark (STW):                <1ms
4. Concurrent Evacuation            ~concurrent
5. Init Update Refs (STW):          <1ms
6. Concurrent Update References     ~concurrent
7. Final Update Refs (STW):         <1ms
8. Concurrent Cleanup               ~concurrent
```

**Сравнение Shenandoah и ZGC:**

| Характеристика | Shenandoah | ZGC |
|----------------|------------|-----|
| Доступность | Java 12+ | Java 11+ (prod Java 15+) |
| Паузы | Цель — короткие паузы, измерять | Цель — короткие паузы, измерять |
| Механизм | Brooks pointers | Colored pointers |
| Heap размер | Любой | До 16TB |
| CPU overhead | Средний | Средний-Высокий |
| Memory overhead | Средний (forwarding ptrs) | Высокий (colored ptrs) |
| Платформы | Linux, Windows | Linux, macOS, Windows |

**Когда использовать Shenandoah:**
- Низкие паузы на небольших-средних Heap
- Когда выбранный дистрибутив поддерживает Shenandoah и измерения подтверждают нужный баланс CPU, памяти и задержек

### Сравнение сборщиков мусора

| GC | Throughput | Latency | Heap Size | CPU | Memory | Use Case |
|----|------------|---------|-----------|-----|--------|----------|
| Serial | Зависит от нагрузки | Обычно длиннее на крупном heap | Малый heap — типичный сценарий | Низкий | Низкий | Embedded, ограниченные ресурсы |
| Parallel | Ориентирован на throughput | STW-паузы | Размер не ограничен этой таблицей | Средний | Низкий | Batch, compute |
| CMS | Средний | Низкий | < 4GB | Высокий | Средний | Deprecated |
| G1 | Баланс | Мягкая цель пауз | Зависит от конфигурации | Средний | Средний | Типичный server-side выбор |
| ZGC | Средний | Очень низкий | > 8GB, до 16TB | Высокий | Высокий | Low-latency, large heap |
| Shenandoah | Средний | Очень низкий | Любой | Высокий | Средний | Low-latency, any size |

**Ориентир для сравнения кандидатов, не пороги применимости:**

```text
Мало ресурсов, небольшой heap → проверить Serial GC
Batch приложения             → Parallel GC
Баланс throughput и пауз      → проверить G1 GC
Низкие паузы + большой Heap  → ZGC
Низкие паузы + любой Heap    → Shenandoah
```

## Жизненный цикл объекта
1. **Выделение** — для подходящих объектов HotSpot обычно использует TLAB; при его заполнении возможны новый TLAB или выделение вне него. Крупные объекты и оптимизации JIT требуют отдельного рассмотрения.
2. **Promotion** — объект, переживший несколько minor-сборок, продвигается в старшее поколение. Настройка `-XX:MaxTenuringThreshold`
   помогает контролировать скорость продвижения.
3. **Сбор** — в рассмотренной поколенческой модели young GC очищает молодое поколение; G1 выполняет mixed-сборки и может выбирать
   регионы с наибольшим эффектом очистки.

## Настройка памяти и диагностика
- **Размеры кучи**: `-Xms` (начальный) и `-Xmx` (максимальный). Хорошая практика — задавать их равными, чтобы избежать
  изменения размера heap, но это увеличивает требования к памяти с самого запуска. Выбирайте по бюджету процесса и контейнера.
- **Metaspace**: лимит `-XX:MaxMetaspaceSize`; `jcmd <pid> VM.native_memory summary` требует заранее включённого `-XX:NativeMemoryTracking=summary`. Объекты, на которые указывают static-поля, находятся в heap, а не становятся метаданными классов.
- **Direct memory**: лимит `-XX:MaxDirectMemorySize`, полезно при активном использовании NIO.
- **GC-логи**: включайте `-Xlog:gc*` (Java 11+) или `-XX:+PrintGCDetails` (Java 8) для анализа пауз. Передайте логи в GC Easy или
  GarbageCat для визуализации.
- **Heap dump**: `jcmd GC.heap_dump` позволяет исследовать удерживаемые объёмы, деревья dominator и потенциальные утечки.

## Частые проблемы и подходы
- **OutOfMemoryError: Java heap space** — пересмотрите размеры кучи, найдите утечку (например, через кэш без ограничений).
- **OutOfMemoryError: Metaspace** — утечки классов из-за пользовательских class loader (часто в application servers). Нужно
  корректно освобождать class loader при hot deploy.
- **GC-паузы слишком длинные** — сопоставьте типы пауз, live set и allocation rate. Проверьте цель пауз и бюджет CPU; сравните другой GC на том же workload. Произвольное уменьшение old generation способно участить полные сборки.
- **Фрагментация/неудачная эвакуация** — исследуйте причины в GC-логах и запас свободных регионов, в G1 также humongous allocations. String deduplication уменьшает повторяющиеся данные строк и не является общим средством дефрагментации. Не добавляйте периодический Full GC без измерений.

## Практические советы
- Измеряйте, а не угадывайте: включайте GC-логи на staging, смотрите распределение времени между young и mixed GC.
- Используйте профилировщики (VisualVM, YourKit, async-profiler) с режимом `alloc` для поиска горячих мест выделения.
- Ограничивайте коллекции, кэшируйте только необходимое, применяйте слабые/мягкие ссылки (`WeakReference`, `SoftReference`).
- При работе с большими буферами сравните heap и direct memory с учётом жизненного цикла и native memory. `allocateDirect` не заменяет произвольный Java-массив и не устраняет ограничения памяти процесса.

## Практические упражнения
1. Напишите программу, которая создаёт много временных объектов, и сравните GC-логи для Parallel GC и G1.
2. Соберите heap dump после нагрузки, определите доминантные цепочки удержания и предложите оптимизации.
3. Попробуйте настроить `-XX:MaxGCPauseMillis` и посмотрите, как изменится поведение G1.

## Вопросы на собеседовании
1. **В чём отличие young и old поколения, и зачем нужны survivor-области?**
   *Ответ:* Молодое поколение обслуживает короткоживущие объекты; после каждой minor-сборки выжившие копируются в survivor-области.
   Объекты, пережившие несколько циклов, продвигаются в старшее поколение. Такое разделение уменьшает стоимость GC, потому что
   большинство объектов умирает рано.
2. **Как выбрать сборщик мусора для сервиса с низкими задержками?**
   *Ответ:* Рассмотреть G1 с настройкой пауз (`-XX:MaxGCPauseMillis`) или современные ZGC/Shenandoah, которые собирают кучу почти
   полностью в фоновом режиме и дают паузы порядка миллисекунд. Решение зависит от версии JDK и объёма кучи.
3. **Что такое stop-the-world и можно ли его избежать?**
   *Ответ:* stop-the-world — момент, когда JVM останавливает все прикладные потоки для выполнения операций GC или других задач
   (профилирование, safepoint). Полностью избежать нельзя, но можно сократить длительность, используя concurrent-алгоритмы и
   оптимизируя размер кучи.
4. **Как диагностировать утечку памяти в продакшене?**
   *Ответ:* Включить `-XX:+HeapDumpOnOutOfMemoryError`, снять heap dump, проанализировать его в MAT/VisualVM, найти цепочки
   удержания. Дополнительно снять серию JFR-трейсов, посмотреть аллокации и удерживаемые объёмы.
5. **Почему неконтролируемое создание потоков приводит к утечке памяти?**
   *Ответ:* Для каждого потока JVM выделяет стек (по умолчанию 1–2 МБ). Большое количество потоков увеличивает потребление памяти
   даже без работы GC. Решение — использовать пулы потоков и асинхронные библиотеки.
