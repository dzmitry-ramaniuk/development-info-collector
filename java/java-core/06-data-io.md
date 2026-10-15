---
---

# Работа с данными и ввод/вывод

## Содержание

1. [Актуальность материала](#актуальность-материала)
2. [Эволюция I/O API](#эволюция-io-api)
3. [Работа с файлами](#работа-с-файлами)
4. [Буферы и каналы](#буферы-и-каналы)
5. [Работа с данными](#работа-с-данными)
6. [Работа с сетевыми данными](#работа-с-сетевыми-данными)
7. [Производительность и безопасность](#производительность-и-безопасность)
8. [Практические упражнения](#практические-упражнения)
   - [Упражнение 1: посчитать имена из UTF-8 файла](#упражнение-1-посчитать-имена-из-utf-8-файла)
   - [Упражнение 2: собрать int из частичных чтений](#упражнение-2-собрать-int-из-частичных-чтений)
   - [Дальнейшая практика](#дальнейшая-практика)
9. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Актуальность материала

- **Дата проверки:** 2026-10-15 — точечная сверка bulk I/O, закрытия Files streams, selectable channels и двух новых упражнений.
- **Целевая версия или диапазон:** Java SE 17–21.
- **Статус примеров:** `current`; оба упражнения и bulk I/O проверены в CoreExamples с --release 17 на JDK 21.0.12.1; сетевой сервер не запускался.
- **Первичные источники:** [InputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/InputStream.html); [Files](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html); [ByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html); [SelectableChannel](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/SelectableChannel.html).

## Эволюция I/O API
- **`java.io`** — потоковая модель (InputStream/OutputStream, Reader/Writer), блокирующий ввод-вывод.
- **NIO (New I/O)** — буферно-ориентированный подход, каналы (`FileChannel`, `SocketChannel`), селекторы для неблокирующей работы.
- **NIO.2 (Java 7+)** — пакет `java.nio.file`, поддержка работы с файловой системой, символьными ссылками, атрибутами файлов.
- **Асинхронный I/O** — `AsynchronousFileChannel`, `AsynchronousSocketChannel`, позволяющие выполнять операции без блокировки
  вызывающего потока на время ожидания результата. Реализация может выполнять I/O через системные механизмы или рабочие потоки; «асинхронный» не означает отсутствие потоков вообще.

## Работа с файлами
Используйте `Paths.get` и `Files` для операций: чтение (`Files.readAllLines`, `Files.lines`), запись (`Files.write`), копирование
(`Files.copy`), управление атрибутами (`Files.getAttribute`). Следите за кодировкой: задавайте `Charset` явно. Для обхода дерева
применяйте `Files.walkFileTree` и реализуйте `SimpleFileVisitor`. Потоки из `Files.lines`, `Files.list` и `Files.walk` закрывайте через `try-with-resources`; терминальная операция stream сама их не закрывает.

## Буферы и каналы
- **Buffer**: содержит позицию (`position`), лимит (`limit`), ёмкость (`capacity`). После записи вызывайте `flip`, после чтения —
  `clear` или `compact`.
- **Channel**: абстракция соединения с источником или получателем данных; направления и возможности зависят от конкретного интерфейса. `FileChannel` поддерживает `transferTo/transferFrom` (zero-copy), `map`
  (memory-mapped files).
- **Selector**: мультиплексирует операции над каналами. Используйте `SelectionKey` и регистрируйте интересующие события
  (`OP_READ`, `OP_WRITE`). Регистрируются `SelectableChannel` в неблокирующем режиме; `FileChannel` не является selectable и не регистрируется в `Selector`.

## Работа с данными
- **Сериализация**: стандартный механизм (`Serializable`) сохраняет граф объектов. Контролируйте `serialVersionUID`, избегайте
  сериализации больших графов. Рассмотрите альтернативы: JSON (Jackson), Protobuf, Avro.
- **JSON/XML**: используйте популярные библиотеки (Jackson, Gson, JAXB). Настраивайте ObjectMapper, включайте валидацию схемы.
- **Дата и время**: пакет `java.time` (Java 8+) предлагает неизменяемые типы `LocalDate`, `ZonedDateTime`, `Duration`.

## Работа с сетевыми данными
- **HTTP**: `HttpClient` (Java 11) поддерживает синхронные и асинхронные запросы, HTTP/2, WebSocket.
- **Sockets**: `ServerSocket`, `Socket` для блокирующих сценариев; `SocketChannel` по умолчанию блокирующий, а для `Selector` вызывают `configureBlocking(false)`.
- **gRPC/REST**: используйте DTO, валидацию (`jakarta.validation`, baseline Spring Boot 3) и логирование запросов.

## Производительность и безопасность
- Используйте `BufferedInputStream`/`BufferedOutputStream` для уменьшения количества системных вызовов.
- Для больших файлов сравнивайте потоковое чтение буферами и memory-mapped I/O (`FileChannel.map`) на своей нагрузке: отображение не является универсальным ускорением и имеет отдельный жизненный цикл.
- Следите за закрытием ресурсов: применяйте `try-with-resources`.
- При работе с внешними данными проверяйте размер и формат, чтобы избежать DoS и инъекций.
- Для шифрования используйте `CipherInputStream`, `CipherOutputStream` и безопасно храните ключи.

## Практические упражнения

### Упражнение 1: посчитать имена из UTF-8 файла

**Задание:** прочитайте файл построчно, уберите пробелы по краям, пропустите пустые строки и посчитайте повторы.
Не загружайте сначала весь файл в список. Результат отсортируйте по имени; файл принадлежит методу и должен быть закрыт.

**Вход:** `" apple \n\npear\napple\n"`. **Ожидается:** `{apple=2, pear=1}`.

**Решение — метод класса:**

```java
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

static Map<String, Long> countNames(Path file) throws IOException {
    try (Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
        return lines.map(String::strip)
            .filter(line -> !line.isEmpty())
            .collect(Collectors.groupingBy(
                name -> name, TreeMap::new, Collectors.counting()));
    }
}
```

`Files.lines` открывает ресурс; `collect` не заменяет `close`. Чтение ленивое, но map результата занимает память,
пропорциональную числу различных имён. I/O-ошибка во время обхода может прийти как `UncheckedIOException`.
См. [контракт Files.lines](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/file/Files.html#lines(java.nio.file.Path,java.nio.charset.Charset)).

**Критерий решения:** исходный пример и пустой файл дают ожидаемые результаты; есть явная кодировка и `try-with-resources`;
ошибка чтения не заменяется молча пустой map.

### Упражнение 2: собрать int из частичных чтений

**Задание:** вход содержит последовательность 32-битных чисел big-endian. Одно чтение даёт не более трёх байт;
нельзя считать, что чтение совпадает с границей записи. Сохраните неполный хвост через `compact()`.
Входной `InputStream` принадлежит вызывающему коду, поэтому метод его не закрывает.

**Вход:** восемь байт для `42` и `99`. **Ожидается:** `[42, 99]`; пустой вход даёт `[]`, оборванная запись — `EOFException`.

**Решение — метод класса:**

```java
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

static List<Integer> decodeInts(InputStream input) throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(6); // Big-endian по умолчанию.
    List<Integer> result = new ArrayList<>();
    while (true) {
        int read = input.read(buffer.array(), buffer.position(),
            Math.min(3, buffer.remaining()));
        if (read == -1) break;
        buffer.position(buffer.position() + read);
        buffer.flip();
        while (buffer.remaining() >= Integer.BYTES) result.add(buffer.getInt());
        buffer.compact();
    }
    if (buffer.position() != 0) throw new EOFException("incomplete int");
    return result;
}
```

`flip` переключает границы с записи на чтение, `compact` переносит неразобранные байты в начало и готовит остаток буфера
к записи. `clear` на том же месте потерял бы логическую границу хвоста. Для `SocketChannel` дополнительно нужны обработка
результата `read()==0`, readiness-событий и частичных `write()`. Это не готовая реализация сетевого сервера.
См. [ByteBuffer.compact](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/ByteBuffer.html#compact()).

**Критерий решения:** три сценария завершаются указанным результатом; читатель может показать значения `position`/`limit`
после каждого чтения и объяснить сохранение хвоста.

Оба решения и bulk I/O проверяются в [CoreExamples.java](../../examples/java-core/CoreExamples.java);
команды запуска — в [README примеров](../../examples/java-core/README.html).

### Дальнейшая практика

1. Скопируйте файл через `Files.copy` и через bulk-операции потоков, измерив время на одинаковых данных.
2. Дополните echo-сервер на `Selector` обработкой частичной записи и отключением `OP_WRITE`, когда выходной буфер пуст.
3. Сохраните DTO в JSON и проверьте размер входа, схему и некорректную кодировку отдельно от синтаксиса JSON.

## Вопросы на собеседовании
1. **Чем отличается потоковая модель `java.io` от буферной `java.nio`?**
   *Ответ:* `java.io` предоставляет byte/character streams, включая bulk-операции `read(byte[])` и `write(byte[])`. `java.nio` добавляет buffers и channels. Неблокирующий режим и selectors доступны для selectable сетевых каналов; не каждый NIO channel неблокирующий, в частности `FileChannel` не selectable.
2. **Что такое memory-mapped файлы и когда их использовать?**
   *Ответ:* Это отображение файла в память процесса через `FileChannel.map`. Позволяет работать с файлами как с массивом байтов,
   снижает количество копирований. Полезно при обработке больших файлов и межпроцессном взаимодействии.
3. **Почему `ObjectOutputStream` не подходит для публичных API?**
   *Ответ:* Стандартная сериализация небезопасна: формат нестабилен, легко привести к атакам десериализации. Лучше использовать
   явные форматы (JSON, Protobuf) и валидировать входные данные.
4. **Как работать с датами и часовыми поясами?**
   *Ответ:* Использовать `java.time`: `Instant` для машинного времени, `ZonedDateTime` для времени с поясом, `DateTimeFormatter`
   для форматирования. Не храните `java.util.Date`/`Calendar`, переводите их в новые типы.
5. **Как реализовать асинхронный HTTP-запрос?**
   *Ответ:* Через `HttpClient.newHttpClient().sendAsync(request, BodyHandlers.ofString())`. Полученный `CompletableFuture`
   позволяет цеплять обработчики (`thenApply`, `exceptionally`).
