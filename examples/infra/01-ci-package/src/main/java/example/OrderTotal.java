package example;

/** Денежные значения — целые minor units, без округления double. */
public final class OrderTotal {
    private OrderTotal() {}

    public static long calculate(long unitPrice, int quantity) {
        if (unitPrice < 0 || quantity <= 0) {
            throw new IllegalArgumentException("price >= 0 and quantity > 0 required");
        }
        return Math.multiplyExact(unitPrice, quantity);
    }

    public static void main(String[] args) {
        try {
            if (args.length != 2) {
                throw new IllegalArgumentException("usage: app <unit-price-minor> <quantity>");
            }
            System.out.println(calculate(Long.parseLong(args[0]), Integer.parseInt(args[1])));
        } catch (IllegalArgumentException | ArithmeticException ex) {
            System.err.println(ex.getMessage());
            System.exit(2);
        }
    }
}
