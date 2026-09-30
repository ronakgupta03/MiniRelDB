package storage;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MemTable {
    private TreeMap<Integer, DBRecord> records = new TreeMap<>();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public void put(DBRecord record) {
        rwLock.writeLock().lock();
        try {
            records.put(record.getId(), record);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public DBRecord get(int id) {
        rwLock.readLock().lock();
        try {
            return records.get(id);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void delete(int id) {
        rwLock.writeLock().lock();
        try {
            // Soft delete for consistency or hard delete in memory
            DBRecord record = records.get(id);
            if (record != null) {
                record.setDeleted(true);
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Collection<DBRecord> getAll() {
        rwLock.readLock().lock();
        try {
            return records.values();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void clear() {
        rwLock.writeLock().lock();
        try {
            records.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int size() {
        rwLock.readLock().lock();
        try {
            int totalSize = 0;
            for (DBRecord record : records.values()) {
                totalSize += record.toBytes().length;
            }
            return totalSize;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
