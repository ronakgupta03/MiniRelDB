package storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DBRecord {

    private int id;
    private String name;
    private boolean deleted;

    public DBRecord(int id, String name) {
        this.id = id;
        this.name = name;
        this.deleted = false;
    }

    public DBRecord(int id, String name, boolean deleted) {
        this.id = id;
        this.name = name;
        this.deleted = deleted;
    }

    // 🔹 Serialization  (Coverting an object -> bytes)
    public byte[] toBytes() {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);   // name -> bytes 

        int totalSize = 1 + 4 + 4 + nameBytes.length;
        // [deleted flag (1 byte)]
        // [id (4 bytes)]
        // [name length (4 bytes)]
        // [name bytes]


        ByteBuffer buffer = ByteBuffer.allocate(totalSize);  //Create a byte container of required size
 
        buffer.put((byte) (deleted ? 1 : 0));  // deleted flag → 1 byte
        buffer.putInt(id);  // int → 4 bytes // 1 → [0, 0, 0, 1]
        buffer.putInt(nameBytes.length); // "Alice" → variable size
        buffer.put(nameBytes); // Add actual string bytes // "Alice" → [65, 108, 105, 99, 101]

        return buffer.array();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int size() {
        return 4 + 4 + name.getBytes(StandardCharsets.UTF_8).length;
    }

    // 🔹 Deserialization (Converting bytes -> object)
    public static DBRecord fromBytes(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);  // Wrap byte array for reading

        boolean deleted = buffer.get() == 1; // Reads first byte → deleted flag
        int id = buffer.getInt(); // Reads next 4 bytes → converts to int
        int nameLength = buffer.getInt(); // Reads next 4 bytes → string length

        byte[] nameBytes = new byte[nameLength];
        buffer.get(nameBytes);

        String name = new String(nameBytes, StandardCharsets.UTF_8);
        return new DBRecord(id, name, deleted);
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    // 🔹 (Optional but useful for testing)
    @Override
    public String toString() {
        return "Record{id=" + id + ", name='" + name + "'}";
    }
}


// Remember how data is stored:

// [id][length][string bytes]

// Example:

// [00000001][00000005][A][l][i][c][e]