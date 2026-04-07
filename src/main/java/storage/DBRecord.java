package storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import catalog.CatalogManager.TableSchema;
import catalog.CatalogManager.ColumnSchema;

public class DBRecord {

    private Map<String, Object> values = new LinkedHashMap<>();
    private boolean deleted;
    private int id; // Primary key is still important for indexing

    public DBRecord(int id, Map<String, Object> values) {
        this.id = id;
        this.values = values;
        this.deleted = false;
    }

    public DBRecord(int id, Map<String, Object> values, boolean deleted) {
        this.id = id;
        this.values = values;
        this.deleted = deleted;
    }

    public void validate() throws Exception {
        // Basic validation for now, could be expanded per type
        int size = toBytes().length;
        if (size > 4096 - 17) {
            throw new Exception("Record too large: " + size + " bytes. Max allowed is " + (4096 - 17) + " bytes.");
        }
    }

    public byte[] toBytes() {
        List<byte[]> serializedValues = new ArrayList<>();
        int totalSize = 1 + 4 + 4; // deleted + id + num_cols

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object val = entry.getValue();
            if (val instanceof Integer) {
                totalSize += 1 + 4;
                ByteBuffer b = ByteBuffer.allocate(5);
                b.put((byte) 0);
                b.putInt((Integer) val);
                serializedValues.add(b.array());
            } else if (val instanceof String) {
                byte[] bytes = ((String) val).getBytes(StandardCharsets.UTF_8);
                totalSize += 1 + 4 + bytes.length;
                ByteBuffer b = ByteBuffer.allocate(1 + 4 + bytes.length);
                b.put((byte) 1);
                b.putInt(bytes.length);
                b.put(bytes);
                serializedValues.add(b.array());
            }
        }

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.put((byte) (deleted ? 1 : 0));
        buffer.putInt(id);
        buffer.putInt(values.size());
        for (byte[] b : serializedValues) {
            buffer.put(b);
        }
        // return buffer.array();
        return buffer.array();
    }

    public static DBRecord fromBytes(byte[] data) {
        if (data.length < 9) return new DBRecord(-1, new HashMap<>(), true);
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte delByte = buffer.get();
        boolean deleted = delByte == 1;
        int id = buffer.getInt();
        int numCols = buffer.getInt();
        Map<String, Object> values = new LinkedHashMap<>();

        for (int i = 0; i < numCols; i++) {
            if (buffer.remaining() < 1) break;
            byte type = buffer.get();
            Object val = null;
            if (type == 0) { // INT
                if (buffer.remaining() < 4) break;
                val = buffer.getInt();
            } else if (type == 1) { // STRING
                if (buffer.remaining() < 4) break;
                int len = buffer.getInt();
                if (buffer.remaining() < len) break;
                byte[] bytes = new byte[len];
                buffer.get(bytes);
                val = new String(bytes, StandardCharsets.UTF_8);
            }
            values.put("col" + i, val);
        }
        return new DBRecord(id, values, deleted);
    }

    // Helper to restore column names from schema
    public void applySchema(TableSchema schema) {
        if (schema == null) return;
        Map<String, Object> newValues = new LinkedHashMap<>();
        for (int i = 0; i < schema.columns.size(); i++) {
            ColumnSchema col = schema.columns.get(i);
            String rawKey = "col" + i;
            if (values.containsKey(col.name)) {
                newValues.put(col.name, values.get(col.name));
            } else if (values.containsKey(rawKey)) {
                newValues.put(col.name, values.get(rawKey));
            }
        }
        // Always ensure 'id' key exists for indexing/search consistency
        if (!newValues.containsKey("id")) {
            newValues.put("id", this.id);
        }
        this.values = newValues;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        if (values.containsKey("name") && values.get("name") != null) return values.get("name").toString();
        return "";
    }

    public Object getValue(String colName) {
        return values.get(colName);
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Record{id=" + id + ", values=" + values + (deleted ? ", DELETED" : "") + "}";
    }
}
