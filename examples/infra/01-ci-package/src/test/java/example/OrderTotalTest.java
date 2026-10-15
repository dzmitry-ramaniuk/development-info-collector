package example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderTotalTest {
    @Test void countsMinorUnitsExactly() { assertEquals(3600L, OrderTotal.calculate(1200, 3)); }
    @Test void allowsFreeOrder() { assertEquals(0L, OrderTotal.calculate(0, 2)); }
    @Test void rejectsNegativePrice() { assertThrows(IllegalArgumentException.class, () -> OrderTotal.calculate(-1, 2)); }
    @Test void rejectsZeroQuantity() { assertThrows(IllegalArgumentException.class, () -> OrderTotal.calculate(10, 0)); }
    @Test void rejectsNegativeQuantity() { assertThrows(IllegalArgumentException.class, () -> OrderTotal.calculate(10, -1)); }
    @Test void rejectsOverflow() { assertThrows(ArithmeticException.class, () -> OrderTotal.calculate(Long.MAX_VALUE, 2)); }
}
