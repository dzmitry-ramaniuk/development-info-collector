import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class RedisIT {
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.2.8-alpine").withExposedPorts(6379);
    @Test void conditionalWriteRejectsDuplicate() throws Exception {
        var first = redis.execInContainer("redis-cli", "SET", "order:42", "created", "NX");
        assertEquals(0, first.getExitCode());
        assertEquals("OK", first.getStdout().trim());
        var duplicate = redis.execInContainer("redis-cli", "SET", "order:42", "changed", "NX");
        assertEquals(0, duplicate.getExitCode());
        assertTrue(duplicate.getStdout().isBlank());
        var result = redis.execInContainer("redis-cli", "GET", "order:42");
        assertEquals(0, result.getExitCode());
        assertEquals("created", result.getStdout().trim());
    }
}
