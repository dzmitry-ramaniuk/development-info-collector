package learning;

import java.io.ByteArrayOutputStream;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AvroCompatibilityTest {
    private static Schema record(String fields) {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"demo\",\"fields\":[" + fields + "]}");
    }
    private static final String ID = "{\"name\":\"id\",\"type\":\"string\"}";
    private static final String NAME = "{\"name\":\"name\",\"type\":\"string\"}";

    private static boolean canRead(Schema reader, Schema writer) {
        return SchemaCompatibility.checkReaderWriterCompatibility(reader, writer).getType()
            == SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;
    }

    @Test
    void backwardAndForwardHaveOppositeReaderWriterDirections() {
        Schema oldSchema = record(ID + "," + NAME);
        Schema removedRequired = record(ID);
        assertTrue(canRead(removedRequired, oldSchema), "BACKWARD: newReader-oldWriter");
        assertFalse(canRead(oldSchema, removedRequired), "FORWARD: oldReader требует name без default");

        Schema addedRequired = record(ID + "," + NAME + ", {\"name\":\"email\",\"type\":\"string\"}");
        assertTrue(canRead(oldSchema, addedRequired), "FORWARD: oldReader игнорирует новое поле");
        assertFalse(canRead(addedRequired, oldSchema), "BACKWARD: newReader неоткуда взять email");
    }

    @Test
    void addedDefaultReadsRealOldPayload() throws Exception {
        Schema oldSchema = record(ID);
        Schema newSchema = record(ID + ", {\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}");
        assertTrue(canRead(newSchema, oldSchema));
        assertTrue(canRead(oldSchema, newSchema));
        var oldRecord = new GenericData.Record(oldSchema);
        oldRecord.put("id", "42");
        var bytes = new ByteArrayOutputStream();
        var encoder = EncoderFactory.get().binaryEncoder(bytes, null);
        new GenericDatumWriter<GenericData.Record>(oldSchema).write(oldRecord, encoder);
        encoder.flush();
        var decoder = DecoderFactory.get().binaryDecoder(bytes.toByteArray(), null);
        GenericData.Record result = new GenericDatumReader<GenericData.Record>(oldSchema, newSchema).read(null, decoder);
        assertEquals("42", result.get("id").toString());
        assertNull(result.get("email"));
    }
}
