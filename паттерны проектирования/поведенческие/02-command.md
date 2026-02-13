# Command (Команда)

Command — поведенческий паттерн проектирования, который превращает запросы в объекты, позволяя передавать их как аргументы при вызове методов, ставить запросы в очередь, логировать их, а также поддерживать отмену операций.

## Содержание

1. [Назначение и применение](#назначение-и-применение)
2. [Проблема, которую решает](#проблема-которую-решает)
3. [Структура паттерна](#структура-паттерна)
4. [Реализация](#реализация)
   - [Пример 1: Умный дом](#пример-1-умный-дом)
   - [Пример 2: Текстовый редактор с undo/redo](#пример-2-текстовый-редактор-с-undoredo)
   - [Пример 3: Система транзакций с откатом](#пример-3-система-транзакций-с-откатом)
   - [Пример 4: Планировщик задач](#пример-4-планировщик-задач)
5. [Macro Commands](#macro-commands)
6. [Примеры из JDK](#примеры-из-jdk)
7. [Преимущества и недостатки](#преимущества-и-недостатки)
8. [Вопросы на собеседовании](#вопросы-на-собеседовании)

## Назначение и применение

Command используется когда:
- Нужно параметризовать объекты выполняемым действием
- Нужно ставить операции в очередь, выполнять их по расписанию или передавать по сети
- Необходима поддержка отмены операций (undo)
- Нужно логировать изменения для последующего восстановления системы
- Нужно структурировать систему на основе высокоуровневых операций

**Типичные примеры использования:**
- GUI приложения (кнопки, меню, горячие клавиши)
- Системы транзакций с откатом
- Планировщики задач и очереди
- Макросы и скрипты
- Многоуровневая отмена (undo/redo)
- Системы логирования операций

## Проблема, которую решает

### Проблема: Прямые вызовы методов

```java
public class RemoteControl {
    private Light light;
    private TV tv;
    private Thermostat thermostat;
    
    public RemoteControl(Light light, TV tv, Thermostat thermostat) {
        this.light = light;
        this.tv = tv;
        this.thermostat = thermostat;
    }
    
    public void button1Pressed() {
        light.turnOn(); // Жесткая привязка к конкретному действию
    }
    
    public void button2Pressed() {
        tv.turnOn();
        tv.setChannel(5);
    }
    
    public void button3Pressed() {
        thermostat.setTemperature(22);
    }
    
    // Проблемы:
    // 1. Невозможно изменить действие кнопки без изменения кода
    // 2. Нет возможности отменить действие
    // 3. Невозможно сохранить историю команд
    // 4. Нельзя передать действие в другой контекст
}
```

**Проблемы:**
- Жесткая связь между вызывающим кодом и получателем
- Невозможность отмены операций
- Сложность логирования и аудита
- Невозможность динамической конфигурации
- Нельзя поставить операции в очередь

### Решение: Command

Инкапсулировать запрос как объект, отделив инициатора от исполнителя.

## Структура паттерна

```java
// Интерфейс команды
interface Command {
    void execute();
    void undo(); // Опционально для отмены
}

// Получатель - объект, который выполняет реальную работу
class Receiver {
    public void action() {
        System.out.println("Receiver performing action");
    }
}

// Конкретная команда
class ConcreteCommand implements Command {
    private Receiver receiver;
    private String state; // Для сохранения состояния для undo
    
    public ConcreteCommand(Receiver receiver) {
        this.receiver = receiver;
    }
    
    @Override
    public void execute() {
        // Сохраняем состояние для возможной отмены
        state = saveState();
        receiver.action();
    }
    
    @Override
    public void undo() {
        restoreState(state);
    }
    
    private String saveState() {
        return "current state";
    }
    
    private void restoreState(String state) {
        System.out.println("Restoring to: " + state);
    }
}

// Инициатор - объект, который запускает команды
class Invoker {
    private Command command;
    
    public void setCommand(Command command) {
        this.command = command;
    }
    
    public void executeCommand() {
        command.execute();
    }
}
```

## Реализация

### Пример 1: Умный дом

Система управления умным домом с пультом дистанционного управления.

```java
// ============= Получатели (Receivers) =============

// Свет
class Light {
    private String location;
    private boolean isOn;
    private int brightness; // 0-100
    
    public Light(String location) {
        this.location = location;
        this.isOn = false;
        this.brightness = 0;
    }
    
    public void on() {
        isOn = true;
        brightness = 100;
        System.out.println(location + " light is ON at " + brightness + "%");
    }
    
    public void off() {
        isOn = false;
        brightness = 0;
        System.out.println(location + " light is OFF");
    }
    
    public void dim(int level) {
        brightness = level;
        if (level == 0) {
            isOn = false;
        } else {
            isOn = true;
        }
        System.out.println(location + " light dimmed to " + brightness + "%");
    }
    
    public int getBrightness() {
        return brightness;
    }
}

// Телевизор
class TV {
    private String location;
    private boolean isOn;
    private int channel;
    private int volume;
    
    public TV(String location) {
        this.location = location;
        this.isOn = false;
        this.channel = 1;
        this.volume = 10;
    }
    
    public void on() {
        isOn = true;
        System.out.println(location + " TV is ON (Channel: " + channel + ", Volume: " + volume + ")");
    }
    
    public void off() {
        isOn = false;
        System.out.println(location + " TV is OFF");
    }
    
    public void setChannel(int channel) {
        this.channel = channel;
        System.out.println(location + " TV channel set to " + channel);
    }
    
    public void setVolume(int volume) {
        this.volume = volume;
        System.out.println(location + " TV volume set to " + volume);
    }
    
    public boolean isOn() { return isOn; }
    public int getChannel() { return channel; }
    public int getVolume() { return volume; }
}

// Термостат
class Thermostat {
    private int temperature;
    
    public Thermostat() {
        this.temperature = 20; // По умолчанию 20°C
    }
    
    public void setTemperature(int temperature) {
        this.temperature = temperature;
        System.out.println("Thermostat set to " + temperature + "°C");
    }
    
    public int getTemperature() {
        return temperature;
    }
}

// ============= Команды (Commands) =============

// Интерфейс команды
interface Command {
    void execute();
    void undo();
}

// Команда: включить свет
class LightOnCommand implements Command {
    private Light light;
    private int previousBrightness;
    
    public LightOnCommand(Light light) {
        this.light = light;
    }
    
    @Override
    public void execute() {
        previousBrightness = light.getBrightness();
        light.on();
    }
    
    @Override
    public void undo() {
        if (previousBrightness > 0) {
            light.dim(previousBrightness);
        } else {
            light.off();
        }
    }
}

// Команда: выключить свет
class LightOffCommand implements Command {
    private Light light;
    private int previousBrightness;
    
    public LightOffCommand(Light light) {
        this.light = light;
    }
    
    @Override
    public void execute() {
        previousBrightness = light.getBrightness();
        light.off();
    }
    
    @Override
    public void undo() {
        if (previousBrightness > 0) {
            light.dim(previousBrightness);
        }
    }
}

// Команда: приглушить свет
class LightDimCommand implements Command {
    private Light light;
    private int level;
    private int previousBrightness;
    
    public LightDimCommand(Light light, int level) {
        this.light = light;
        this.level = level;
    }
    
    @Override
    public void execute() {
        previousBrightness = light.getBrightness();
        light.dim(level);
    }
    
    @Override
    public void undo() {
        light.dim(previousBrightness);
    }
}

// Команда: включить TV
class TVOnCommand implements Command {
    private TV tv;
    
    public TVOnCommand(TV tv) {
        this.tv = tv;
    }
    
    @Override
    public void execute() {
        tv.on();
    }
    
    @Override
    public void undo() {
        tv.off();
    }
}

// Команда: выключить TV
class TVOffCommand implements Command {
    private TV tv;
    
    public TVOffCommand(TV tv) {
        this.tv = tv;
    }
    
    @Override
    public void execute() {
        tv.off();
    }
    
    @Override
    public void undo() {
        tv.on();
    }
}

// Команда: установить температуру
class ThermostatCommand implements Command {
    private Thermostat thermostat;
    private int temperature;
    private int previousTemperature;
    
    public ThermostatCommand(Thermostat thermostat, int temperature) {
        this.thermostat = thermostat;
        this.temperature = temperature;
    }
    
    @Override
    public void execute() {
        previousTemperature = thermostat.getTemperature();
        thermostat.setTemperature(temperature);
    }
    
    @Override
    public void undo() {
        thermostat.setTemperature(previousTemperature);
    }
}

// Пустая команда (Null Object Pattern)
class NoCommand implements Command {
    @Override
    public void execute() {
        System.out.println("No command assigned");
    }
    
    @Override
    public void undo() {
        // Ничего не делаем
    }
}

// ============= Инициатор (Invoker) =============

// Пульт управления
class RemoteControl {
    private Command[] onCommands;
    private Command[] offCommands;
    private Command lastCommand;
    
    public RemoteControl(int slots) {
        onCommands = new Command[slots];
        offCommands = new Command[slots];
        
        Command noCommand = new NoCommand();
        for (int i = 0; i < slots; i++) {
            onCommands[i] = noCommand;
            offCommands[i] = noCommand;
        }
        lastCommand = noCommand;
    }
    
    public void setCommand(int slot, Command onCommand, Command offCommand) {
        onCommands[slot] = onCommand;
        offCommands[slot] = offCommand;
    }
    
    public void onButtonPressed(int slot) {
        onCommands[slot].execute();
        lastCommand = onCommands[slot];
    }
    
    public void offButtonPressed(int slot) {
        offCommands[slot].execute();
        lastCommand = offCommands[slot];
    }
    
    public void undoButtonPressed() {
        System.out.println("Undoing last command...");
        lastCommand.undo();
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("\n------ Remote Control ------\n");
        for (int i = 0; i < onCommands.length; i++) {
            sb.append("[slot ").append(i).append("] ")
              .append(onCommands[i].getClass().getSimpleName())
              .append("    ")
              .append(offCommands[i].getClass().getSimpleName())
              .append("\n");
        }
        return sb.toString();
    }
}

// ============= Использование =============

class SmartHomeDemo {
    public static void main(String[] args) {
        // Создаем устройства
        Light livingRoomLight = new Light("Living Room");
        Light bedroomLight = new Light("Bedroom");
        TV livingRoomTV = new TV("Living Room");
        Thermostat thermostat = new Thermostat();
        
        // Создаем команды
        Command livingRoomLightOn = new LightOnCommand(livingRoomLight);
        Command livingRoomLightOff = new LightOffCommand(livingRoomLight);
        Command bedroomLightOn = new LightOnCommand(bedroomLight);
        Command bedroomLightOff = new LightOffCommand(bedroomLight);
        Command tvOn = new TVOnCommand(livingRoomTV);
        Command tvOff = new TVOffCommand(livingRoomTV);
        Command thermostatWarm = new ThermostatCommand(thermostat, 24);
        Command thermostatCool = new ThermostatCommand(thermostat, 18);
        
        // Создаем пульт с 4 слотами
        RemoteControl remote = new RemoteControl(4);
        
        // Программируем пульт
        remote.setCommand(0, livingRoomLightOn, livingRoomLightOff);
        remote.setCommand(1, bedroomLightOn, bedroomLightOff);
        remote.setCommand(2, tvOn, tvOff);
        remote.setCommand(3, thermostatWarm, thermostatCool);
        
        System.out.println(remote);
        
        // Используем пульт
        System.out.println("\n=== Testing Remote Control ===\n");
        
        remote.onButtonPressed(0);  // Living room light on
        remote.offButtonPressed(0); // Living room light off
        remote.undoButtonPressed();  // Undo (light on again)
        
        System.out.println();
        remote.onButtonPressed(2);  // TV on
        remote.onButtonPressed(3);  // Thermostat to 24°C
        
        System.out.println();
        remote.undoButtonPressed();  // Undo thermostat
        
        System.out.println();
        remote.offButtonPressed(2); // TV off
    }
}
```

### Пример 2: Текстовый редактор с undo/redo

Редактор с поддержкой множественной отмены и повтора операций.

```java
// ============= Получатель (Receiver) =============

// Документ
class Document {
    private StringBuilder content;
    
    public Document() {
        this.content = new StringBuilder();
    }
    
    public void insertText(String text, int position) {
        content.insert(position, text);
        System.out.println("Inserted '" + text + "' at position " + position);
        displayContent();
    }
    
    public void deleteText(int position, int length) {
        content.delete(position, position + length);
        System.out.println("Deleted " + length + " characters from position " + position);
        displayContent();
    }
    
    public void replaceText(int position, int length, String newText) {
        content.replace(position, position + length, newText);
        System.out.println("Replaced text at position " + position + " with '" + newText + "'");
        displayContent();
    }
    
    public String getContent() {
        return content.toString();
    }
    
    public int getLength() {
        return content.length();
    }
    
    public void displayContent() {
        System.out.println("Content: \"" + content.toString() + "\"");
    }
}

// ============= Команды (Commands) =============

// Абстрактная команда редактора
interface EditorCommand {
    void execute();
    void undo();
}

// Команда вставки текста
class InsertTextCommand implements EditorCommand {
    private Document document;
    private String text;
    private int position;
    
    public InsertTextCommand(Document document, String text, int position) {
        this.document = document;
        this.text = text;
        this.position = position;
    }
    
    @Override
    public void execute() {
        document.insertText(text, position);
    }
    
    @Override
    public void undo() {
        document.deleteText(position, text.length());
    }
}

// Команда удаления текста
class DeleteTextCommand implements EditorCommand {
    private Document document;
    private int position;
    private int length;
    private String deletedText;
    
    public DeleteTextCommand(Document document, int position, int length) {
        this.document = document;
        this.position = position;
        this.length = length;
    }
    
    @Override
    public void execute() {
        // Сохраняем удаляемый текст для undo
        String content = document.getContent();
        deletedText = content.substring(position, Math.min(position + length, content.length()));
        document.deleteText(position, length);
    }
    
    @Override
    public void undo() {
        document.insertText(deletedText, position);
    }
}

// Команда замены текста
class ReplaceTextCommand implements EditorCommand {
    private Document document;
    private int position;
    private int length;
    private String newText;
    private String oldText;
    
    public ReplaceTextCommand(Document document, int position, int length, String newText) {
        this.document = document;
        this.position = position;
        this.length = length;
        this.newText = newText;
    }
    
    @Override
    public void execute() {
        String content = document.getContent();
        oldText = content.substring(position, Math.min(position + length, content.length()));
        document.replaceText(position, length, newText);
    }
    
    @Override
    public void undo() {
        document.replaceText(position, newText.length(), oldText);
    }
}

// ============= Инициатор с историей (Invoker) =============

// Менеджер истории команд
class CommandHistory {
    private Stack<EditorCommand> undoStack = new Stack<>();
    private Stack<EditorCommand> redoStack = new Stack<>();
    
    public void executeCommand(EditorCommand command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Очищаем redo после нового действия
    }
    
    public void undo() {
        if (undoStack.isEmpty()) {
            System.out.println("Nothing to undo!");
            return;
        }
        
        EditorCommand command = undoStack.pop();
        System.out.println("\n--- Undoing operation ---");
        command.undo();
        redoStack.push(command);
    }
    
    public void redo() {
        if (redoStack.isEmpty()) {
            System.out.println("Nothing to redo!");
            return;
        }
        
        EditorCommand command = redoStack.pop();
        System.out.println("\n--- Redoing operation ---");
        command.execute();
        undoStack.push(command);
    }
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}

// ============= Использование =============

class TextEditorDemo {
    public static void main(String[] args) {
        Document document = new Document();
        CommandHistory history = new CommandHistory();
        
        System.out.println("=== Text Editor with Undo/Redo ===\n");
        
        // Вставляем текст
        history.executeCommand(new InsertTextCommand(document, "Hello", 0));
        
        System.out.println();
        history.executeCommand(new InsertTextCommand(document, " World", 5));
        
        System.out.println();
        history.executeCommand(new InsertTextCommand(document, "!", 11));
        
        // Удаляем текст
        System.out.println();
        history.executeCommand(new DeleteTextCommand(document, 5, 6)); // Удаляем " World"
        
        // Заменяем текст
        System.out.println();
        history.executeCommand(new ReplaceTextCommand(document, 0, 5, "Hi")); // "Hello!" -> "Hi!"
        
        // Отменяем последние 2 операции
        System.out.println();
        history.undo(); // Вернет "Hello!"
        
        System.out.println();
        history.undo(); // Вернет "Hello World!"
        
        // Повторяем одну операцию
        System.out.println();
        history.redo(); // Снова удалит " World"
        
        // Делаем новую операцию (это очистит redo stack)
        System.out.println();
        history.executeCommand(new InsertTextCommand(document, " Java", 5));
        
        System.out.println();
        history.undo();
        
        System.out.println();
        history.redo();
    }
}
```

### Пример 3: Система транзакций с откатом

Банковские транзакции с возможностью отката.

```java
// ============= Получатели (Receivers) =============

// Банковский счет
class BankAccount {
    private String accountNumber;
    private double balance;
    
    public BankAccount(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }
    
    public void deposit(double amount) {
        balance += amount;
        System.out.println("Deposited $" + amount + " to " + accountNumber 
            + ". New balance: $" + balance);
    }
    
    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            System.out.println("Withdrew $" + amount + " from " + accountNumber 
                + ". New balance: $" + balance);
            return true;
        } else {
            System.out.println("Insufficient funds in " + accountNumber);
            return false;
        }
    }
    
    public double getBalance() {
        return balance;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
}

// ============= Команды-транзакции (Commands) =============

// Интерфейс транзакции
interface Transaction {
    boolean execute();
    void rollback();
    String getDescription();
}

// Транзакция пополнения
class DepositTransaction implements Transaction {
    private BankAccount account;
    private double amount;
    private boolean executed;
    
    public DepositTransaction(BankAccount account, double amount) {
        this.account = account;
        this.amount = amount;
        this.executed = false;
    }
    
    @Override
    public boolean execute() {
        account.deposit(amount);
        executed = true;
        return true;
    }
    
    @Override
    public void rollback() {
        if (executed) {
            account.withdraw(amount);
            System.out.println("Rolled back deposit of $" + amount);
        }
    }
    
    @Override
    public String getDescription() {
        return "Deposit $" + amount + " to " + account.getAccountNumber();
    }
}

// Транзакция снятия
class WithdrawTransaction implements Transaction {
    private BankAccount account;
    private double amount;
    private boolean executed;
    
    public WithdrawTransaction(BankAccount account, double amount) {
        this.account = account;
        this.amount = amount;
        this.executed = false;
    }
    
    @Override
    public boolean execute() {
        if (account.withdraw(amount)) {
            executed = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void rollback() {
        if (executed) {
            account.deposit(amount);
            System.out.println("Rolled back withdrawal of $" + amount);
        }
    }
    
    @Override
    public String getDescription() {
        return "Withdraw $" + amount + " from " + account.getAccountNumber();
    }
}

// Транзакция перевода
class TransferTransaction implements Transaction {
    private BankAccount fromAccount;
    private BankAccount toAccount;
    private double amount;
    private boolean executed;
    
    public TransferTransaction(BankAccount fromAccount, BankAccount toAccount, double amount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.executed = false;
    }
    
    @Override
    public boolean execute() {
        if (fromAccount.withdraw(amount)) {
            toAccount.deposit(amount);
            System.out.println("Transferred $" + amount + " from " 
                + fromAccount.getAccountNumber() + " to " + toAccount.getAccountNumber());
            executed = true;
            return true;
        }
        return false;
    }
    
    @Override
    public void rollback() {
        if (executed) {
            toAccount.withdraw(amount);
            fromAccount.deposit(amount);
            System.out.println("Rolled back transfer of $" + amount);
        }
    }
    
    @Override
    public String getDescription() {
        return "Transfer $" + amount + " from " + fromAccount.getAccountNumber() 
            + " to " + toAccount.getAccountNumber();
    }
}

// ============= Менеджер транзакций (Invoker) =============

class TransactionManager {
    private List<Transaction> completedTransactions = new ArrayList<>();
    
    public boolean executeTransaction(Transaction transaction) {
        System.out.println("\nExecuting: " + transaction.getDescription());
        
        if (transaction.execute()) {
            completedTransactions.add(transaction);
            System.out.println("✓ Transaction completed successfully");
            return true;
        } else {
            System.out.println("✗ Transaction failed");
            return false;
        }
    }
    
    public void rollbackLastTransaction() {
        if (completedTransactions.isEmpty()) {
            System.out.println("No transactions to rollback");
            return;
        }
        
        Transaction lastTransaction = completedTransactions.remove(completedTransactions.size() - 1);
        System.out.println("\nRolling back: " + lastTransaction.getDescription());
        lastTransaction.rollback();
    }
    
    public void rollbackAllTransactions() {
        System.out.println("\n=== Rolling back all transactions ===");
        
        // Откатываем в обратном порядке
        for (int i = completedTransactions.size() - 1; i >= 0; i--) {
            Transaction transaction = completedTransactions.get(i);
            System.out.println("\nRolling back: " + transaction.getDescription());
            transaction.rollback();
        }
        
        completedTransactions.clear();
    }
    
    public void printHistory() {
        System.out.println("\n=== Transaction History ===");
        if (completedTransactions.isEmpty()) {
            System.out.println("No transactions");
        } else {
            for (int i = 0; i < completedTransactions.size(); i++) {
                System.out.println((i + 1) + ". " + completedTransactions.get(i).getDescription());
            }
        }
    }
}

// ============= Использование =============

class BankingSystemDemo {
    public static void main(String[] args) {
        // Создаем счета
        BankAccount account1 = new BankAccount("ACC001", 1000.0);
        BankAccount account2 = new BankAccount("ACC002", 500.0);
        
        TransactionManager manager = new TransactionManager();
        
        System.out.println("=== Banking Transaction System ===");
        System.out.println("Initial balances:");
        System.out.println("Account 1: $" + account1.getBalance());
        System.out.println("Account 2: $" + account2.getBalance());
        
        // Выполняем транзакции
        manager.executeTransaction(new DepositTransaction(account1, 200.0));
        manager.executeTransaction(new WithdrawTransaction(account1, 150.0));
        manager.executeTransaction(new TransferTransaction(account1, account2, 300.0));
        
        manager.printHistory();
        
        System.out.println("\nCurrent balances:");
        System.out.println("Account 1: $" + account1.getBalance());
        System.out.println("Account 2: $" + account2.getBalance());
        
        // Откатываем последнюю транзакцию
        manager.rollbackLastTransaction();
        
        System.out.println("\nBalances after rollback:");
        System.out.println("Account 1: $" + account1.getBalance());
        System.out.println("Account 2: $" + account2.getBalance());
        
        // Откатываем все
        manager.rollbackAllTransactions();
        
        System.out.println("\nBalances after full rollback:");
        System.out.println("Account 1: $" + account1.getBalance());
        System.out.println("Account 2: $" + account2.getBalance());
    }
}
```

### Пример 4: Планировщик задач

Система очереди задач с планировщиком.

```java
// ============= Задачи (Commands) =============

// Интерфейс задачи
interface Task extends Runnable {
    String getName();
    TaskPriority getPriority();
    void execute();
    
    @Override
    default void run() {
        execute();
    }
}

enum TaskPriority {
    LOW(1), NORMAL(2), HIGH(3), CRITICAL(4);
    
    private final int value;
    
    TaskPriority(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
}

// Абстрактная задача
abstract class AbstractTask implements Task {
    protected String name;
    protected TaskPriority priority;
    
    public AbstractTask(String name, TaskPriority priority) {
        this.name = name;
        this.priority = priority;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public TaskPriority getPriority() {
        return priority;
    }
}

// Задача отправки email
class EmailTask extends AbstractTask {
    private String recipient;
    private String subject;
    private String body;
    
    public EmailTask(String recipient, String subject, String body, TaskPriority priority) {
        super("Email to " + recipient, priority);
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
    }
    
    @Override
    public void execute() {
        System.out.println("📧 Sending email to " + recipient);
        System.out.println("   Subject: " + subject);
        System.out.println("   Body: " + body);
        
        // Симуляция отправки
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Email sent successfully");
    }
}

// Задача генерации отчета
class ReportGenerationTask extends AbstractTask {
    private String reportType;
    private String outputPath;
    
    public ReportGenerationTask(String reportType, String outputPath, TaskPriority priority) {
        super("Generate " + reportType + " report", priority);
        this.reportType = reportType;
        this.outputPath = outputPath;
    }
    
    @Override
    public void execute() {
        System.out.println("📊 Generating " + reportType + " report");
        System.out.println("   Output: " + outputPath);
        
        // Симуляция генерации
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Report generated successfully");
    }
}

// Задача резервного копирования
class BackupTask extends AbstractTask {
    private String sourcePath;
    private String destinationPath;
    
    public BackupTask(String sourcePath, String destinationPath) {
        super("Backup " + sourcePath, TaskPriority.HIGH);
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }
    
    @Override
    public void execute() {
        System.out.println("💾 Backing up data");
        System.out.println("   Source: " + sourcePath);
        System.out.println("   Destination: " + destinationPath);
        
        // Симуляция backup
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Backup completed successfully");
    }
}

// Задача очистки
class CleanupTask extends AbstractTask {
    private String path;
    
    public CleanupTask(String path) {
        super("Cleanup " + path, TaskPriority.LOW);
        this.path = path;
    }
    
    @Override
    public void execute() {
        System.out.println("🧹 Cleaning up " + path);
        
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("   ✓ Cleanup completed");
    }
}

// ============= Планировщик задач (Invoker) =============

class TaskScheduler {
    private PriorityQueue<Task> taskQueue;
    private ExecutorService executor;
    private boolean isRunning;
    
    public TaskScheduler(int threadPoolSize) {
        // Сортируем по приоритету (высокий приоритет первым)
        this.taskQueue = new PriorityQueue<>(
            Comparator.comparing(Task::getPriority, 
                Comparator.comparingInt(TaskPriority::getValue).reversed())
        );
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        this.isRunning = false;
    }
    
    public synchronized void scheduleTask(Task task) {
        taskQueue.offer(task);
        System.out.println("✓ Scheduled: " + task.getName() + " [" + task.getPriority() + "]");
    }
    
    public void start() {
        if (isRunning) {
            System.out.println("Scheduler is already running");
            return;
        }
        
        isRunning = true;
        System.out.println("\n=== Task Scheduler Started ===\n");
        
        while (!taskQueue.isEmpty()) {
            Task task = taskQueue.poll();
            System.out.println("\n--- Executing: " + task.getName() + " ---");
            executor.submit(task);
        }
        
        shutdown();
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        isRunning = false;
        System.out.println("\n=== Task Scheduler Stopped ===");
    }
    
    public int getQueueSize() {
        return taskQueue.size();
    }
}

// ============= Использование =============

class TaskSchedulerDemo {
    public static void main(String[] args) {
        TaskScheduler scheduler = new TaskScheduler(2); // 2 потока
        
        System.out.println("=== Task Scheduling System ===\n");
        
        // Планируем задачи с разными приоритетами
        scheduler.scheduleTask(new EmailTask(
            "user@example.com", 
            "Welcome", 
            "Thank you for signing up!", 
            TaskPriority.NORMAL
        ));
        
        scheduler.scheduleTask(new ReportGenerationTask(
            "Sales", 
            "/reports/sales_2024.pdf", 
            TaskPriority.HIGH
        ));
        
        scheduler.scheduleTask(new CleanupTask("/tmp"));
        
        scheduler.scheduleTask(new BackupTask(
            "/data/production", 
            "/backups/prod_backup"
        ));
        
        scheduler.scheduleTask(new EmailTask(
            "admin@example.com", 
            "System Alert", 
            "Critical issue detected!", 
            TaskPriority.CRITICAL
        ));
        
        scheduler.scheduleTask(new ReportGenerationTask(
            "Inventory", 
            "/reports/inventory_2024.pdf", 
            TaskPriority.NORMAL
        ));
        
        // Запускаем планировщик
        scheduler.start();
    }
}
```

## Macro Commands

Макрокоманды позволяют объединить несколько команд в одну.

```java
// Макрокоманда
class MacroCommand implements Command {
    private Command[] commands;
    
    public MacroCommand(Command[] commands) {
        this.commands = commands;
    }
    
    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }
    
    @Override
    public void undo() {
        // Откатываем в обратном порядке
        for (int i = commands.length - 1; i >= 0; i--) {
            commands[i].undo();
        }
    }
}

// Использование макрокоманды
class MacroCommandDemo {
    public static void main(String[] args) {
        Light light = new Light("Living Room");
        TV tv = new TV("Living Room");
        Thermostat thermostat = new Thermostat();
        
        // Создаем "Party Mode" макрокоманду
        Command[] partyOn = {
            new LightDimCommand(light, 30),
            new TVOnCommand(tv),
            new ThermostatCommand(thermostat, 22)
        };
        
        Command[] partyOff = {
            new LightOffCommand(light),
            new TVOffCommand(tv),
            new ThermostatCommand(thermostat, 20)
        };
        
        MacroCommand partyOnMacro = new MacroCommand(partyOn);
        MacroCommand partyOffMacro = new MacroCommand(partyOff);
        
        RemoteControl remote = new RemoteControl(1);
        remote.setCommand(0, partyOnMacro, partyOffMacro);
        
        System.out.println("=== Party Mode ===\n");
        remote.onButtonPressed(0);  // Включаем party mode
        
        System.out.println("\n=== End Party ===\n");
        remote.offButtonPressed(0); // Выключаем party mode
        
        System.out.println("\n=== Undo ===\n");
        remote.undoButtonPressed(); // Откатываем
    }
}
```

## Примеры из JDK

### 1. java.lang.Runnable

```java
// Runnable - это классический пример паттерна Command
Runnable task = new Runnable() {
    @Override
    public void run() {
        System.out.println("Executing task");
    }
};

// Executor - это Invoker
ExecutorService executor = Executors.newSingleThreadExecutor();
executor.submit(task); // Выполнение команды
executor.shutdown();

// Лямбда-версия
executor.submit(() -> System.out.println("Executing task"));
```

### 2. javax.swing.Action

```java
// Action в Swing - это Command с дополнительными метаданными
Action saveAction = new AbstractAction("Save") {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Saving file...");
    }
};

// Можно привязать к разным UI элементам
JButton saveButton = new JButton(saveAction);
JMenuItem saveMenuItem = new JMenuItem(saveAction);
// Обе используют одну команду
```

### 3. Thread и Thread Pool

```java
// Thread pool использует паттерн Command для задач
ExecutorService pool = Executors.newFixedThreadPool(5);

// Задачи - это команды
pool.execute(() -> System.out.println("Task 1"));
pool.execute(() -> System.out.println("Task 2"));
pool.execute(() -> System.out.println("Task 3"));

pool.shutdown();
```

### 4. CompletableFuture

```java
// CompletableFuture позволяет создавать цепочки команд
CompletableFuture.supplyAsync(() -> "Hello")
    .thenApply(s -> s + " World")
    .thenApply(String::toUpperCase)
    .thenAccept(System.out::println);
```

## Преимущества и недостатки

### Преимущества

| Преимущество | Описание |
|--------------|----------|
| **Разделение ответственности** | Отделяет объект, инициирующий операцию, от объекта, который её выполняет |
| **Отмена операций** | Легко реализовать undo/redo функциональность |
| **Составные команды** | Можно собирать сложные команды из простых (макрокоманды) |
| **Очереди команд** | Команды можно ставить в очередь, планировать, передавать по сети |
| **Логирование** | Легко логировать все операции для аудита или восстановления |
| **Расширяемость** | Легко добавлять новые команды без изменения существующего кода |

### Недостатки

| Недостаток | Описание |
|------------|----------|
| **Увеличение количества классов** | Для каждой операции нужен отдельный класс команды |
| **Усложнение кода** | Добавляется дополнительный уровень абстракции |
| **Управление состоянием** | Для undo нужно хранить предыдущее состояние, что может потреблять память |
| **Сложность отладки** | Поток выполнения становится менее прямолинейным |

## Вопросы на собеседовании

### 1. Что такое паттерн Command?

**Ответ:** Command — поведенческий паттерн проектирования, который инкапсулирует запрос как объект, позволяя параметризовать клиентов с различными запросами, ставить запросы в очередь, логировать их и поддерживать отмену операций.

Основные компоненты:
- **Command**: интерфейс команды с методом execute()
- **ConcreteCommand**: конкретная реализация команды
- **Receiver**: объект, который выполняет реальную работу
- **Invoker**: объект, который запускает команды
- **Client**: создает команды и связывает их с получателями

### 2. В чем отличие Command от Strategy?

**Ответ:**
- **Command**: Инкапсулирует действие вместе с параметрами. Фокус на выполнении, отмене, логировании операций. Команда обычно содержит ссылку на receiver и вызывает его методы.
- **Strategy**: Инкапсулирует алгоритм. Фокус на выборе разных способов выполнения одной операции. Стратегия не обязательно хранит состояние.

Пример:
```java
// Command - инкапсулирует действие
Command saveCommand = new SaveCommand(document, "/path/to/file");
saveCommand.execute();

// Strategy - инкапсулирует алгоритм
SortStrategy strategy = new QuickSort();
sorter.setStrategy(strategy);
sorter.sort(array);
```

### 3. Как реализовать undo/redo функциональность?

**Ответ:** Используется два стека: один для undo, другой для redo.

```java
class CommandHistory {
    private Stack<Command> undoStack = new Stack<>();
    private Stack<Command> redoStack = new Stack<>();
    
    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        redoStack.clear(); // Очищаем redo после нового действия
    }
    
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }
    
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }
}
```

Важно:
- При выполнении новой команды redo stack очищается
- Команды выполняются в прямом порядке, откатываются в обратном
- Нужно сохранять предыдущее состояние в команде для undo

### 4. Когда следует использовать паттерн Command?

**Ответ:** Command следует использовать когда:
- Нужно параметризовать объекты выполняемыми действиями
- Требуется ставить операции в очередь или выполнять их по расписанию
- Необходима поддержка отмены операций (undo/redo)
- Нужно логировать операции или поддерживать транзакции
- Хотите отделить объект, инициирующий операцию, от объекта, выполняющего её
- Нужно поддерживать макрокоманды (композитные команды)

Типичные сценарии: GUI приложения, системы транзакций, планировщики задач, удаленное выполнение.

### 5. Что такое макрокоманды?

**Ответ:** Макрокоманда (Composite Command) — это команда, состоящая из последовательности других команд. Это применение паттерна Composite к паттерну Command.

```java
class MacroCommand implements Command {
    private List<Command> commands;
    
    public MacroCommand(Command... commands) {
        this.commands = Arrays.asList(commands);
    }
    
    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }
    
    @Override
    public void undo() {
        // Откатываем в обратном порядке
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
```

Применение: режимы работы (party mode, sleep mode), пакетная обработка, сложные операции.

### 6. Как Command используется в многопоточном окружении?

**Ответ:** Command отлично подходит для многопоточности:

```java
// 1. ExecutorService - классический пример
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> System.out.println("Task 1"));
executor.submit(() -> System.out.println("Task 2"));

// 2. Асинхронное выполнение
CompletableFuture.runAsync(() -> {
    // команда выполняется асинхронно
});

// 3. Очередь команд для межпоточного взаимодействия
BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

// Producer thread
commandQueue.put(new SomeCommand());

// Consumer thread
Command command = commandQueue.take();
command.execute();
```

Преимущества:
- Команды можно безопасно передавать между потоками
- Легко реализовать producer-consumer паттерн
- Можно контролировать выполнение через Executor

### 7. В чем разница между Command и Callback?

**Ответ:**
- **Command**: Полноценный объект с методами execute() и undo(). Может хранить состояние, историю, поддерживать отмену. Более формальный подход.
- **Callback**: Обычно простая функция/лямбда, вызываемая при наступлении события. Обычно не поддерживает undo. Более легковесный подход.

```java
// Command - объект с состоянием
Command command = new SaveCommand(document, path);
command.execute();
command.undo();

// Callback - простая функция
button.setOnClickListener(event -> {
    // обработка клика
});
```

Command более подходит для сложной бизнес-логики, callback — для простых обработчиков событий.

### 8. Как хранить историю команд с минимальным использованием памяти?

**Ответ:** Несколько стратегий:

**1. Memento паттерн**: Хранить только снимки состояния
```java
class Command {
    private Memento memento;
    
    public void execute() {
        memento = receiver.createMemento(); // Сохраняем состояние
        receiver.performAction();
    }
    
    public void undo() {
        receiver.restoreFromMemento(memento);
    }
}
```

**2. Ограничение размера истории**:
```java
class BoundedHistory {
    private static final int MAX_SIZE = 100;
    private Deque<Command> history = new ArrayDeque<>();
    
    public void add(Command command) {
        if (history.size() >= MAX_SIZE) {
            history.removeFirst(); // Удаляем самую старую
        }
        history.addLast(command);
    }
}
```

**3. Командный лог вместо объектов**:
```java
// Вместо хранения объектов команд, храним их параметры
class CommandLog {
    record LogEntry(String commandType, Map<String, Object> params) {}
    
    private List<LogEntry> log = new ArrayList<>();
    
    public void log(Command command) {
        log.add(new LogEntry(command.getType(), command.getParams()));
    }
    
    public Command recreate(LogEntry entry) {
        // Воссоздаем команду из лога при необходимости
    }
}
```

### 9. Можно ли комбинировать Command с другими паттернами?

**Ответ:** Да, Command часто комбинируется:

**Command + Composite**: Макрокоманды
```java
class CompositeCommand implements Command {
    private List<Command> children;
    
    public void execute() {
        children.forEach(Command::execute);
    }
}
```

**Command + Memento**: Для сохранения состояния при undo
```java
class CommandWithMemento implements Command {
    private Memento memento;
    
    public void execute() {
        memento = receiver.save();
        receiver.action();
    }
    
    public void undo() {
        receiver.restore(memento);
    }
}
```

**Command + Chain of Responsibility**: Валидация команд
```java
class ValidatingCommand implements Command {
    private Command command;
    private Validator validator;
    
    public void execute() {
        if (validator.validate()) {
            command.execute();
        }
    }
}
```

**Command + Factory**: Создание команд
```java
class CommandFactory {
    public Command createCommand(String type, Object... params) {
        return switch(type) {
            case "save" -> new SaveCommand(params);
            case "delete" -> new DeleteCommand(params);
            default -> new NoCommand();
        };
    }
}
```

### 10. Как тестировать команды?

**Ответ:** Стратегии тестирования:

```java
@Test
public void testCommandExecution() {
    // Arrange
    Document document = new Document();
    Command command = new InsertTextCommand(document, "Hello", 0);
    
    // Act
    command.execute();
    
    // Assert
    assertEquals("Hello", document.getContent());
}

@Test
public void testCommandUndo() {
    Document document = new Document();
    document.insertText("World", 0);
    
    Command command = new InsertTextCommand(document, "Hello ", 0);
    command.execute();
    
    assertEquals("Hello World", document.getContent());
    
    command.undo();
    
    assertEquals("World", document.getContent());
}

@Test
public void testMacroCommand() {
    Light light = mock(Light.class);
    TV tv = mock(TV.class);
    
    Command[] commands = {
        new LightOnCommand(light),
        new TVOnCommand(tv)
    };
    
    MacroCommand macro = new MacroCommand(commands);
    macro.execute();
    
    verify(light).on();
    verify(tv).on();
}

@Test
public void testCommandHistory() {
    CommandHistory history = new CommandHistory();
    Command command1 = mock(Command.class);
    Command command2 = mock(Command.class);
    
    history.execute(command1);
    history.execute(command2);
    
    history.undo();
    
    verify(command2).undo();
    verify(command1, never()).undo();
}
```

Важно тестировать:
- Правильность выполнения команды
- Корректность отмены (undo)
- Работу с историей команд
- Макрокоманды
- Граничные случаи (пустая история, невалидные параметры)
