import java.io.ByteArrayOutputStream;
import java.util.stream.Stream;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class AvroCompatibilityTest {
    static Schema schema(String fields) {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Order\",\"fields\":[" + fields + "]}");
    }
    static final String ID = "{\"name\":\"id\",\"type\":\"long\"}";
    static Stream<Arguments> changes() {
        Schema v1 = schema(ID);
        Schema withDefault = schema(ID + ",{\"name\":\"currency\",\"type\":\"string\",\"default\":\"EUR\"}");
        Schema withoutDefault = schema(ID + ",{\"name\":\"currency\",\"type\":\"string\"}");
        Schema removed = schema("");
        Schema intId = schema("{\"name\":\"id\",\"type\":\"int\"}");
        return Stream.of(
            Arguments.of("BACKWARD add with default", withDefault, v1, true),
            Arguments.of("BACKWARD add without default", withoutDefault, v1, false),
            Arguments.of("FORWARD add without default", v1, withoutDefault, true),
            Arguments.of("BACKWARD remove field", removed, v1, true),
            Arguments.of("FORWARD remove field without old default", v1, removed, false),
            Arguments.of("BACKWARD int to long", v1, intId, true),
            Arguments.of("FORWARD int to long", intId, v1, false)
        );
    }
    @ParameterizedTest(name = "{0}") @MethodSource("changes")
    void compatibilityUsesReaderThenWriter(String name, Schema reader, Schema writer, boolean expected) {
        var result = SchemaCompatibility.checkReaderWriterCompatibility(reader, writer);
        assertEquals(expected, result.getType() == SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE, name);
    }
    @Test void defaultIsAppliedWhenReadingOldPayload() throws Exception {
        Schema writer = schema(ID);
        Schema reader = schema(ID + ",{\"name\":\"currency\",\"type\":\"string\",\"default\":\"EUR\"}");
        var record = new GenericData.Record(writer);
        record.put("id", 42L);
        var bytes = new ByteArrayOutputStream();
        var encoder = EncoderFactory.get().binaryEncoder(bytes, null);
        new GenericDatumWriter<GenericData.Record>(writer).write(record, encoder);
        encoder.flush();
        var decoded = new GenericDatumReader<GenericData.Record>(writer, reader)
            .read(null, DecoderFactory.get().binaryDecoder(bytes.toByteArray(), null));
        assertEquals(42L, decoded.get("id"));
        assertEquals("EUR", decoded.get("currency").toString());
    }
}
