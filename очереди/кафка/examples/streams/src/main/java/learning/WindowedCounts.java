package learning;

import java.time.Duration;
import java.util.Map;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.RocksDBConfigSetter;
import org.apache.kafka.streams.state.WindowStore;
import org.rocksdb.Options;

public final class WindowedCounts {
    private WindowedCounts() { }

    public static Topology topology() {
        StreamsBuilder builder = new StreamsBuilder();
        builder.stream("events", Consumed.with(Serdes.String(), Serdes.String()))
            .groupByKey(Grouped.with(Serdes.String(), Serdes.String()))
            .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofMinutes(5), Duration.ofMinutes(1)))
            .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("counts")
                .withKeySerde(Serdes.String()).withValueSerde(Serdes.Long())
                .withRetention(Duration.ofHours(1)))
            .toStream()
            .map((window, count) -> KeyValue.pair(window.key() + "@" + window.window().start(), count))
            .to("counts", Produced.with(Serdes.String(), Serdes.Long()));
        return builder.build();
    }

    public static void stopOnFailure(KafkaStreams streams) {
        streams.setUncaughtExceptionHandler(error -> {
            System.err.println("Streams остановлен: " + error);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.SHUTDOWN_CLIENT;
        });
    }

    public static final class BoundedRocksDBConfig implements RocksDBConfigSetter {
        @Override
        public void setConfig(String name, Options options, Map<String, Object> configs) {
            options.setMaxWriteBufferNumber(3);
            options.setWriteBufferSize(16 * 1024 * 1024L);
        }

        @Override
        public void close(String name, Options options) {
            // Собственные RocksObject не создавались; options принадлежит Streams.
        }
    }

    public static Map<String, Object> stateSettings() {
        return Map.of(StreamsConfig.STATESTORE_CACHE_MAX_BYTES_CONFIG, 0L,
            StreamsConfig.ROCKSDB_CONFIG_SETTER_CLASS_CONFIG, BoundedRocksDBConfig.class);
    }
}
