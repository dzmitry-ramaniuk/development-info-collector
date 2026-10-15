package learning;

import java.time.Duration;
import java.time.Instant;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WindowedCountsTest {
    @Test
    void aggregatesAndDropsEventsAfterGrace() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "window-practice");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "unused:9092");
        props.putAll(WindowedCounts.stateSettings());
        try (TopologyTestDriver driver = new TopologyTestDriver(WindowedCounts.topology(), props)) {
            var input = driver.createInputTopic("events", Serdes.String().serializer(),
                Serdes.String().serializer());
            var output = driver.createOutputTopic("counts", Serdes.String().deserializer(),
                Serdes.Long().deserializer());
            input.pipeInput("room-1", "created", Instant.EPOCH);
            input.pipeInput("room-1", "created", Instant.EPOCH.plusSeconds(30));
            assertEquals(2L, driver.getWindowStore("counts").fetch("room-1", 0L));
            assertEquals(2L, output.readKeyValuesToMap().get("room-1@0"));

            // Продвигаем stream-time за size + grace; wall-clock sleep не нужен.
            input.pipeInput("room-2", "created", Instant.EPOCH.plus(Duration.ofMinutes(7)));
            output.readKeyValuesToList();
            input.pipeInput("room-1", "late", Instant.EPOCH.plusSeconds(60));
            assertTrue(output.isEmpty(), "Закрытое окно не должно обновиться");
            assertEquals(2L, driver.getWindowStore("counts").fetch("room-1", 0L));
        }
    }
}
