package query;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import catalog.CatalogManager;
import storage.*;

public class Executor {
    private String dbName;
    private CatalogManager catalogManager;
    private Map<String, HeapFile> heapFiles = new ConcurrentHashMap<>();
    private Map<String, index.DiskBPlusTree> indexes = new ConcurrentHashMap<>();
    private Map<String, storage.MemTable> memTables = new ConcurrentHashMap<>();
    private Map<String, storage.WriteAheadLog> wals = new ConcurrentHashMap<>();
    private final ReadWriteLock tableLock = new ReentrantReadWriteLock();
    
    // Adaptive Hash Index (AHI)
    private Map<String, Map<Integer, DBRecord>> ahi = new ConcurrentHashMap<>();
    
    // Secondary Indexes (column -> index)
    private Map<String, Map<String, index.DiskBPlusTree>> secondaryIndexes = new ConcurrentHashMap<>();

    private Map<Integer, DBRecord> createAhiMap() {
        return new LinkedHashMap<Integer, DBRecord>(1000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, DBRecord> eldest) {
                return size() > 1000;
            }
        };
    }

    private void updateSecondaryIndexes(String tableName, DBRecord record, boolean isDelete) throws Exception {
        Map<String, index.DiskBPlusTree> tableSecIdx = secondaryIndexes.get(tableName);
        if (tableSecIdx == null) return;

        for (Map.Entry<String, index.DiskBPlusTree> entry : tableSecIdx.entrySet()) {
            String colName = entry.getKey();
            index.DiskBPlusTree sIdx = entry.getValue();
            Object val = record.getValue(colName);
            if (val != null) {
                int secKey = secondaryIndexKey(val);
                if (isDelete) {
                    sIdx.delete(secKey); 
                } else {
                    sIdx.insert(secKey, record.getId());
                }
            }
        }
    }

    static class SSTableMetadata {
        public String tableName;
        public int version;
        public DiskManager dm;
        public index.BloomFilter filter;
        public index.BPlusTree index;

        public SSTableMetadata(String tableName, int version, DiskManager dm) {
            this.tableName = tableName;
            this.version = version;
            this.dm = dm;
            this.filter = new index.BloomFilter(1000);
            this.index = new index.BPlusTree();
        }
    }

    private List<SSTableMetadata> sstables = new ArrayList<>();

    public Executor(String dbName, CatalogManager catalogManager) throws Exception {
        this.dbName = dbName;
        this.catalogManager = catalogManager;
        
        CatalogManager.DatabaseSchema dbSchema = catalogManager.getDatabaseSchema(dbName);
        if (dbSchema != null) {
            for (String tableName : dbSchema.tables.keySet()) {
                initTable(tableName);
            }
        }
        loadSSTables();
    }

    private void initTable(String tableName) throws Exception {
        tableLock.writeLock().lock();
        try {
            if (memTables.containsKey(tableName)) return;

            DiskManager dm = new DiskManager(dbName, tableName);
            HeapFile hf = new HeapFile(dm);
            heapFiles.put(tableName, hf);

            DiskManager idxDm = new DiskManager(dbName, tableName + ".idx");
            index.DiskBPlusTree idx = new index.DiskBPlusTree(idxDm);
            indexes.put(tableName, idx);

            memTables.put(tableName, new storage.MemTable());
            wals.put(tableName, new storage.WriteAheadLog(dbName + "/" + tableName));
            recoverFromWal(tableName);
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    private void recoverFromWal(String tableName) throws Exception {
        storage.WriteAheadLog wal = wals.get(tableName);
        storage.MemTable memTable = memTables.get(tableName);
        List<storage.WriteAheadLog.LogEntry> entries = wal.recover();
        for (storage.WriteAheadLog.LogEntry entry : entries) {
            if (entry.opType == 0) memTable.put(entry.record);
            else if (entry.opType == 1) memTable.delete(entry.record.getId());
        }
    }

    private void loadSSTables() throws Exception {
        File dbDir = new File("data/" + dbName);
        if (!dbDir.exists()) return;
        File[] files = dbDir.listFiles((dir, name) -> name.endsWith(".db") && name.contains("_v"));
        if (files == null) return;
        for (File f : files) {
            String name = f.getName().replace(".db", "");
            int lastV = name.lastIndexOf("_v");
            if (lastV == -1 || lastV + 2 >= name.length()) continue;
            String versionStr = name.substring(lastV + 2);
            if (!versionStr.matches("\\d+")) continue;
            String tableName = name.substring(0, lastV);
            int version = Integer.parseInt(versionStr);
            initTable(tableName);
            DiskManager dm = new DiskManager(dbName, name);
            SSTableMetadata meta = new SSTableMetadata(tableName, version, dm);
            for (int i = 0; i < dm.getPageCount(); i++) {
                Page page = dm.readPage(i);
                for (DBRecord record : page.getAllRecords()) {
                    record.applySchema(catalogManager.getTableSchema(dbName, tableName));
                    meta.index.insert(record.getId(), i);
                    meta.filter.add(record.getId());
                }
            }
            sstables.add(meta);
        }
        sstables.sort((a, b) -> b.version - a.version);
    }

    public void executeCreateTable(CreateTableQuery q) throws Exception {
        tableLock.writeLock().lock();
        try {
            CatalogManager.TableSchema ts = new CatalogManager.TableSchema(q.getTableName());
            ts.columns = q.getColumns();
            ts.primaryKey = q.getPrimaryKey();
            catalogManager.createTable(dbName, ts);
            initTable(q.getTableName());
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public void executeAlterTable(AlterTableQuery q) throws Exception {
        tableLock.writeLock().lock();
        try {
            String tableName = q.getTableName();
            initTable(tableName);
            CatalogManager.TableSchema ts = catalogManager.getTableSchema(dbName, tableName);
            if (ts == null) throw new Exception("Table not found: " + tableName);

            if (q.getOperation().equals("ADD")) {
                ts.columns.add(new CatalogManager.ColumnSchema(q.getColumnName(), q.getColumnType()));
            } else if (q.getOperation().equals("DROP")) {
                ts.columns.removeIf(c -> c.name.equals(q.getColumnName()));
            } else if (q.getOperation().equals("MODIFY")) {
                for (CatalogManager.ColumnSchema c : ts.columns) {
                    if (c.name.equals(q.getColumnName())) {
                        c.type = q.getColumnType();
                    }
                }
            }
            catalogManager.saveCatalog();
            if (ahi.containsKey(tableName)) ahi.get(tableName).clear();
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public void executeDrop(DropQuery q) throws Exception {
        tableLock.writeLock().lock();
        try {
            if (q.getType().equals("DATABASE")) {
                catalogManager.dropDatabase(q.getName());
            } else {
                catalogManager.dropTable(dbName, q.getName());
                heapFiles.remove(q.getName());
                indexes.remove(q.getName());
                memTables.remove(q.getName());
                wals.remove(q.getName());
            }
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public List<String> executeShow(ShowQuery q) {
        if (q.getType().equals("DATABASES")) {
            return catalogManager.getDatabases();
        } else if (q.getType().equals("TABLES")) {
            return catalogManager.getTables(dbName);
        }
        return new ArrayList<>();
    }

    private String getDefaultTable() {
        if (heapFiles.isEmpty()) return null;
        return heapFiles.keySet().iterator().next();
    }

    public void executeInsertBatch(List<InsertQuery> queries) throws Exception {
        if (queries == null || queries.isEmpty()) return;
        String tableName = queries.get(0).getTableName();
        initTable(tableName);
        tableLock.writeLock().lock();
        try {
            CatalogManager.TableSchema schema = catalogManager.getTableSchema(dbName, tableName);
            for (InsertQuery query : queries) {
                int targetId = query.getId();
                DBRecord record = new DBRecord(targetId, query.getValues());
                if (schema != null) record.applySchema(schema);
                
                memTables.get(tableName).put(record);
                int pageId = heapFiles.get(tableName).insertRecord(record);
                indexes.get(tableName).insert(record.getId(), pageId);
            }
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public void executeInsert(InsertQuery query) throws Exception {
        tableLock.writeLock().lock();
        try {
            String tableName = query.getTableName();
            initTable(tableName);
            CatalogManager.TableSchema schema = catalogManager.getTableSchema(dbName, tableName);

            int targetId = query.getId();
            DBRecord record = new DBRecord(targetId, query.getValues());
            if (schema != null) record.applySchema(schema);

            memTables.get(tableName).put(record);
            int pageId = heapFiles.get(tableName).insertRecord(record);
            indexes.get(tableName).insert(record.getId(), pageId);
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    private int secondaryIndexKey(Object val) {
        if (val == null) return 0;
        if (val instanceof Integer) return (Integer) val;
        return val.toString().hashCode();
    }

    private List<DBRecord> filterColumns(List<DBRecord> records, List<String> requestedCols) {
        if (requestedCols == null || requestedCols.isEmpty() || requestedCols.get(0).equals("*")) return records;
        List<DBRecord> projected = new ArrayList<>();
        for (DBRecord r : records) {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (String col : requestedCols) {
                filtered.put(col, r.getValue(col));
            }
            projected.add(new DBRecord(r.getId(), filtered, r.isDeleted()));
        }
        return projected;
    }

    public List<DBRecord> executeSelect(SelectQuery query) throws Exception {
        String tableName = query.getBaseTable();
        if (tableName == null) tableName = getDefaultTable();
        if (tableName == null) return Collections.emptyList();
        initTable(tableName);
        tableLock.readLock().lock();
        try {
            return executeSelectInternal(query);
        } finally {
            tableLock.readLock().unlock();
        }
    }

    private List<DBRecord> executeSelectInternal(SelectQuery query) throws Exception {
        String tableName = query.getBaseTable();
        List<DBRecord> results = new ArrayList<>();
        
        storage.MemTable memTable = memTables.get(tableName);
        if (memTable != null) {
            for (DBRecord r : memTable.getAll()) {
                if (!r.isDeleted()) results.add(r);
            }
        }
        HeapFile hf = heapFiles.get(tableName);
        if (hf != null) {
            for (DBRecord r : hf.getAllRecords()) {
                r.applySchema(catalogManager.getTableSchema(dbName, tableName));
                if (!r.isDeleted()) results.add(r);
            }
        }

        List<DBRecord> filtered = new ArrayList<>();
        for (DBRecord r : results) {
            if (query.matches(r)) filtered.add(r);
        }
        return filtered;
    }

    public void executeUpdate(UpdateQuery query) throws Exception {
        tableLock.writeLock().lock();
        try {
            String tableName = query.getTableName();
            initTable(tableName);
            SelectQuery sq = new SelectQuery(query.getId());
            sq.setBaseTable(tableName);
            List<DBRecord> toUpdate = executeSelectInternal(sq);
            for (DBRecord record : toUpdate) {
                // record.setValue(query.getColumnName(), query.getNewValue());
                // memTables.get(tableName).put(record);
                // heapFiles.get(tableName).updateRecord(record);
            }
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public void executeDelete(DeleteQuery query) throws Exception {
        tableLock.writeLock().lock();
        try {
            String tableName = query.getTableName();
            initTable(tableName);
            SelectQuery sq = new SelectQuery(query.getId());
            sq.setBaseTable(tableName);
            List<DBRecord> toDelete = executeSelectInternal(sq);
            for (DBRecord r : toDelete) {
                r.setDeleted(true);
                memTables.get(tableName).delete(r.getId());
                // heapFiles.get(tableName).deleteRecord(r.getId());
            }
        } finally {
            tableLock.writeLock().unlock();
        }
    }

    public void flushAll() throws Exception {
        for (HeapFile hf : heapFiles.values()) hf.flush();
    }
    
    /*
    public List<DBRecord> executePlan(PlanNode plan) throws Exception {
        List<DBRecord> results = new ArrayList<>();
        plan.open();
        while (true) {
            DBRecord r = plan.next();
            if (r == null) break;
            results.add(r);
        }
        plan.close();
        return results;
    }
    */
}
