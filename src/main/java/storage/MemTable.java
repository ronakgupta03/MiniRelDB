package storage;

import java.util.*;

public class MemTable {
    private TreeMap<Integer, DBRecord> records = new TreeMap<>();

    public void put(DBRecord record) {
        records.put(record.getId(), record);
    }

    public DBRecord get(int id) {
        return records.get(id);
    }

    public void delete(int id) {
        // Soft delete for consistency or hard delete in memory
        DBRecord record = records.get(id);
        if (record != null) {
            record.setDeleted(true);
        }
    }

    public Collection<DBRecord> getAll() {
        return records.values();
    }

    public void clear() {
        records.clear();
    }

    public int size() {
        int totalSize = 0;
        for (DBRecord record : records.values()) {
            totalSize += record.toBytes().length;
        }
        return totalSize;
    }
}
