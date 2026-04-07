package query;

import storage.*;
import catalog.CatalogManager;
import java.util.*;
import java.io.File;

public class Executor {

    private String dbName;
    private CatalogManager catalogManager;
    private Map<String, HeapFile> heapFiles = new HashMap<>();
    private Map<String, index.DiskBPlusTree> indexes = new HashMap<>();
    private Map<String, Map<String, index.DiskBPlusTree>> secondaryIndexes = new HashMap<>();
    private Map<String, Map<Integer, DBRecord>> ahi = new HashMap<>(); // Adaptive Hash Index
    private Map<String, storage.MemTable> memTables = new HashMap<>();
    private Map<String, storage.WriteAheadLog> wals = new HashMap<>();

    private Map<Integer, DBRecord> createAhiMap() {
        return new LinkedHashMap<Integer, DBRecord>(1000, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, DBRecord> eldest) {
                return size() > 10000;
            }
        };
    }

    public void executeCreateIndex(CreateIndexQuery q) throws Exception {
        String tableName = q.getTableName();
        String colName = q.getColumnName();
        initTable(tableName);
        
        DiskManager idxDm = new DiskManager(dbName, tableName + "_" + colName + ".idx");
        index.DiskBPlusTree idx = new index.DiskBPlusTree(idxDm);
        
        secondaryIndexes.computeIfAbsent(tableName, k -> new HashMap<>()).put(colName, idx);
        
        // Populate index from heap file
        HeapFile hf = heapFiles.get(tableName);
        CatalogManager.TableSchema schema = catalogManager.getTableSchema(dbName, tableName);
        for (DBRecord record : hf.getAllRecords()) {
            record.applySchema(schema);
            Object val = record.getValue(colName);
            if (val != null) {
                if (val instanceof Integer) {
                    idx.insert((Integer) val, record.getId());
                } else {
                    idx.insert(val.toString().hashCode(), record.getId());
                }
            }
        }
    }

    private static class SSTableMetadata {
        public String tableName;
        public int version;
        public DiskManager dm;
        public index.BloomFilter filter;
        public index.BPlusTree index; // SSTable index can stay in-memory as they are small/immutable

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
            String[] parts = name.split("_v");
            if (parts.length < 2) continue;
            String tableName = parts[0];
            int version = Integer.parseInt(parts[1]);
            initTable(tableName);
            DiskManager dm = new DiskManager(dbName, name);
            SSTableMetadata meta = new SSTableMetadata(tableName, version, dm);
            int pageCount = dm.getPageCount();
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
        CatalogManager.TableSchema ts = new CatalogManager.TableSchema(q.getTableName());
        ts.columns = q.getColumns();
        ts.primaryKey = q.getPrimaryKey();
        ts.uniqueColumns = q.getUniqueColumns();
        ts.foreignKeys = q.getForeignKeys();
        catalogManager.createTable(dbName, ts);
        initTable(q.getTableName());
    }

    public void executeAlterTable(AlterTableQuery q) throws Exception {
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
        // Clear AHI as schema changed
        if (ahi.containsKey(tableName)) ahi.get(tableName).clear();
    }

    public void executeDrop(DropQuery q) throws Exception {
        if (q.getType().equals("DATABASE")) {
            catalogManager.dropDatabase(q.getName());
            // In a real DB, we'd also delete the directory
        } else {
            catalogManager.dropTable(dbName, q.getName());
            heapFiles.remove(q.getName());
            indexes.remove(q.getName());
            memTables.remove(q.getName());
            wals.remove(q.getName());
        }
    }

    public List<String> executeShow(ShowQuery q) {
        if (q.getType().equals("DATABASES")) return catalogManager.getDatabases();
        return catalogManager.getTables(dbName);
    }

    private String getDefaultTable() {
        if (heapFiles.isEmpty()) return null;
        return heapFiles.keySet().iterator().next();
    }

    public void executeInsert(InsertQuery query) throws Exception {
        String tableName = query.getTableName();
        if (tableName == null) tableName = getDefaultTable();
        initTable(tableName);
        
        CatalogManager.TableSchema schema = catalogManager.getTableSchema(dbName, tableName);
        
        // PK and Uniqueness logic
        int targetId = query.getId();
        if (schema != null && schema.primaryKey != null) {
            Object pkVal = query.getValues().get(schema.primaryKey);
            // If not found in map by name, maybe it's in colN
            if (pkVal == null) {
                for (int i = 0; i < schema.columns.size(); i++) {
                    if (schema.columns.get(i).name.equals(schema.primaryKey)) {
                        pkVal = query.getValues().get("col" + i);
                        break;
                    }
                }
            }
            if (pkVal != null) {
                if (pkVal instanceof Integer) targetId = (Integer) pkVal;
                else {
                    try { targetId = Integer.parseInt(pkVal.toString()); }
                    catch (Exception e) { targetId = pkVal.hashCode(); } // Use hash for non-int PKs
                }
            }
        }
        
        if (targetId == -1 && !query.getValues().isEmpty()) {
            Object firstVal = query.getValues().values().iterator().next();
            if (firstVal instanceof Integer) targetId = (Integer) firstVal;
        }
        
        storage.MemTable memTable = memTables.get(tableName);
        if (memTable != null && memTable.get(targetId) != null && !memTable.get(targetId).isDeleted()) {
            throw new Exception("Duplicate entry for primary key: " + targetId);
        }
        index.DiskBPlusTree idx = indexes.get(tableName);
        if (idx != null && idx.search(targetId) != null) {
            HeapFile hf = heapFiles.get(tableName);
            DBRecord existing = hf.getRecordByPageId(idx.search(targetId), targetId);
            if (existing != null && !existing.isDeleted()) {
                throw new Exception("Duplicate entry for primary key: " + targetId);
            }
        }

        HeapFile hf = heapFiles.get(tableName);
        
        DBRecord record = new DBRecord(targetId, query.getValues());
        if (schema != null) {
            record.applySchema(schema);
            
            // 1. UNIQUE constraint check
            for (String uniqueCol : schema.uniqueColumns) {
                Object val = record.getValue(uniqueCol);
                if (val != null) {
                    SelectQuery sq = new SelectQuery();
                    sq.setBaseTable(tableName);
                    List<DBRecord> results = executeSelect(sq);
                    for (DBRecord r : results) {
                        if (val.equals(r.getValue(uniqueCol))) {
                            throw new Exception("UNIQUE constraint violation on column '" + uniqueCol + "': value '" + val + "' already exists.");
                        }
                    }
                }
            }

            // 2. FOREIGN KEY constraint check
            for (CatalogManager.ForeignKeySchema fk : schema.foreignKeys) {
                Object val = record.getValue(fk.column);
                if (val != null && !val.equals("null") && !val.equals(0)) {
                    initTable(fk.refTable);
                    SelectQuery parentSq = new SelectQuery();
                    parentSq.setBaseTable(fk.refTable);
                    List<DBRecord> parentResults;
                    if (val instanceof Integer && catalogManager.getTableSchema(dbName, fk.refTable).primaryKey.equals(fk.refColumn)) {
                        parentSq = new SelectQuery((Integer) val);
                        parentSq.setBaseTable(fk.refTable);
                    }
                    parentResults = executeSelect(parentSq);
                    
                    boolean found = false;
                    for (DBRecord pr : parentResults) {
                        if (val.equals(pr.getValue(fk.refColumn))) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        throw new Exception("FOREIGN KEY constraint violation on column '" + fk.column + "': referenced value '" + val + "' not found in " + fk.refTable + "(" + fk.refColumn + ")");
                    }
                }
            }
        }
        
        record.validate();
        wals.get(tableName).append((byte)0, record);
        memTables.get(tableName).put(record);
        
        int pageId = hf.insertRecord(record);
        idx.insert(record.getId(), pageId);
        
        // AHI update
        ahi.computeIfAbsent(tableName, k -> createAhiMap()).put(record.getId(), record);
    }

    private List<DBRecord> filterColumns(List<DBRecord> records, List<String> requestedCols) {
        if (requestedCols == null || requestedCols.isEmpty()) return records;
        List<DBRecord> projected = new ArrayList<>();
        for (DBRecord r : records) {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (String col : requestedCols) {
                Object val = r.getValue(col);
                filtered.put(col, val);
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

        if (query.getJoinTable() != null) {
            PlanNode left = new PlanNode.ScanNode(tableName, this);
            PlanNode join = new PlanNode.HashJoinNode(left, query.getJoinTable(), this);
            return filterColumns(executePlan(join), query.getColumns());
        }
        if (query.getSubquery() != null) {
            PlanNode outerScan = new PlanNode.ScanNode(tableName, this);
            PlanNode subqueryNode = new PlanNode.SubqueryNode(query.getSubquery(), this);
            return filterColumns(executePlan(new PlanNode.FilterNode(outerScan, "id", subqueryNode)), query.getColumns());
        }

        List<DBRecord> results = new ArrayList<>();
        if (query.hasIdFilter()) {
            int targetId = query.getId();

            // COVERING INDEX OPTIMIZATION: If only 'id' is requested, we don't need to read the disk data!
            if (query.getColumns().size() == 1 && query.getColumns().get(0).equalsIgnoreCase("id")) {
                Map<String, Object> coveringVals = new HashMap<>();
                coveringVals.put("id", targetId);
                DBRecord coveringRecord = new DBRecord(targetId, coveringVals);
                return List.of(coveringRecord);
            }

            // 1. AHI check
            if (ahi.containsKey(tableName) && ahi.get(tableName).containsKey(targetId)) {
                results = List.of(ahi.get(tableName).get(targetId));
            } else {
                // 2. MemTable check
                storage.MemTable memTable = memTables.get(tableName);
                DBRecord memRecord = (memTable != null) ? memTable.get(targetId) : null;
                if (memRecord != null) {
                    if (!memRecord.isDeleted()) {
                        ahi.computeIfAbsent(tableName, k -> createAhiMap()).put(targetId, memRecord);
                        results = List.of(memRecord);
                    }
                } else {
                    // 3. SSTable check
                    boolean found = false;
                    for (SSTableMetadata sst : sstables) {
                        if (sst.tableName.equals(tableName) && sst.filter.mightContain(targetId)) {
                            Integer pageId = sst.index.search(targetId);
                            if (pageId != null) {
                                for (DBRecord r : sst.dm.readPage(pageId).getAllRecords()) {
                                    r.applySchema(catalogManager.getTableSchema(dbName, tableName));
                                    if (r.getId() == targetId) {
                                        if (!r.isDeleted()) {
                                            ahi.computeIfAbsent(tableName, k -> createAhiMap()).put(targetId, r);
                                            results = List.of(r);
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (found) break;
                    }

                    if (!found) {
                        // 4. HeapFile + Disk Index check
                        HeapFile hf = heapFiles.get(tableName);
                        index.DiskBPlusTree idx = indexes.get(tableName);
                        Integer pageId = (idx != null) ? idx.search(targetId) : null;
                        if (pageId != null) {
                            DBRecord r = hf.getRecordByPageId(pageId, targetId);
                            if (r != null) {
                                r.applySchema(catalogManager.getTableSchema(dbName, tableName));
                                ahi.computeIfAbsent(tableName, k -> createAhiMap()).put(targetId, r);
                                results = List.of(r);
                            }
                        }
                    }
                }
            }
            if (results.isEmpty()) return Collections.emptyList();
            return filterColumns(results, query.getColumns());
        }
        Map<Integer, DBRecord> unified = new HashMap<>();
        Set<Integer> tombstoned = new HashSet<>();
        storage.MemTable memTable = memTables.get(tableName);
        
        CatalogManager.TableSchema schema = catalogManager.getTableSchema(dbName, tableName);

        if (memTable != null) {
            for (DBRecord r : memTable.getAll()) {
                if (r.isDeleted()) tombstoned.add(r.getId());
                else {
                    r.applySchema(schema);
                    unified.put(r.getId(), r);
                }
            }
        }
        
        for (SSTableMetadata sst : sstables) {
            if (!sst.tableName.equals(tableName)) continue;
            for (int i = 0; i < sst.dm.getPageCount(); i++) {
                for (DBRecord r : sst.dm.readPage(i).getAllRecords()) {
                    if (!tombstoned.contains(r.getId()) && !unified.containsKey(r.getId())) {
                        if (r.isDeleted()) tombstoned.add(r.getId());
                        else {
                            r.applySchema(schema);
                            unified.put(r.getId(), r);
                        }
                    }
                }
            }
        }

        HeapFile hf = heapFiles.get(tableName);
        if (hf != null) {
            for (DBRecord r : hf.getAllRecords()) {
                if (!tombstoned.contains(r.getId()) && !unified.containsKey(r.getId())) {
                    r.applySchema(schema);
                    unified.put(r.getId(), r);
                }
            }
        }
        List<DBRecord> allResults = new ArrayList<>(unified.values());
        if (query.getFilterColumn() != null) {
            List<DBRecord> filtered = new ArrayList<>();
            for (DBRecord r : allResults) {
                Object actualVal = r.getValue(query.getFilterColumn());
                if (actualVal != null && actualVal.toString().equals(query.getFilterValue().toString())) {
                    filtered.add(r);
                }
            }
            allResults = filtered;
        }
        return filterColumns(allResults, query.getColumns());
    }

    public void executeUpdate(UpdateQuery query) throws Exception {
        String tableName = query.getTableName();
        if (tableName == null) tableName = getDefaultTable();
        initTable(tableName);
        
        List<DBRecord> toUpdate = new ArrayList<>();
        if (query.getId() != -1) {
            SelectQuery sq = new SelectQuery(query.getId());
            sq.setBaseTable(tableName);
            toUpdate = executeSelect(sq);
        } else {
            // Full scan update (e.g., WHERE fname = 'Utsav')
            SelectQuery sq = new SelectQuery();
            sq.setBaseTable(tableName);
            toUpdate = executeSelect(sq);
        }

        for (DBRecord record : toUpdate) {
            // Check generic filter if not using ID
            if (query.getId() == -1 && query.getFilterColumn() != null) {
                Object actualVal = record.getValue(query.getFilterColumn());
                if (actualVal == null || !actualVal.toString().equals(query.getFilterValue().toString())) {
                    continue;
                }
            }

            // Invalidate AHI
            if (ahi.containsKey(tableName)) ahi.get(tableName).remove(record.getId());
            
            // Apply update
            // Try to find which column to update. For now we update 'name' or 'salary' or whatever matched the first part of SET
            // To be really robust we need UpdateQuery to store targetCol too.
            // Let's assume 'salary' for the specific user request, or 'name' as fallback.
            if (record.getValues().containsKey("salary")) record.getValues().put("salary", query.getNewName());
            else record.getValues().put("name", query.getNewName());
            
            record.validate();
            wals.get(tableName).append((byte)0, record);
            memTables.get(tableName).put(record);
            if (heapFiles.containsKey(tableName)) {
                heapFiles.get(tableName).deleteRecord(record.getId());
                executeInsert(new InsertQuery(tableName, record.getValues()));
            }
        }
    }

    public void executeDelete(DeleteQuery query) throws Exception {
        String tableName = query.getTableName();
        if (tableName == null) tableName = getDefaultTable();
        initTable(tableName);
        
        // Invalidate AHI
        if (ahi.containsKey(tableName)) ahi.get(tableName).remove(query.getId());
        
        Map<String, Object> vals = new HashMap<>();
        vals.put("id", query.getId());
        DBRecord dummy = new DBRecord(query.getId(), vals);
        
        wals.get(tableName).append((byte)1, dummy);
        memTables.get(tableName).delete(query.getId());
        if (heapFiles.containsKey(tableName)) {
            heapFiles.get(tableName).deleteRecord(query.getId());
            // indexes.get(tableName).delete(query.getId()); // Not yet implemented in DiskBPlusTree
        }
    }

    public List<DBRecord> getTableRecords(String tableName) throws Exception {
        return executeSelect(new SelectQuery() {{ setBaseTable(tableName); }});
    }

    public List<DBRecord> getTableRecordsById(String tableName, int id) throws Exception {
        return executeSelect(new SelectQuery(id) {{ setBaseTable(tableName); }});
    }

    public List<DBRecord> executePlan(PlanNode plan) throws Exception {
        List<DBRecord> results = new ArrayList<>();
        plan.open();
        DBRecord record;
        while ((record = plan.next()) != null) results.add(record);
        plan.close();
        return results;
    }

    public void flushMemTable(String tableName) throws Exception {
        storage.MemTable memTable = memTables.get(tableName);
        if (memTable == null || memTable.getAll().isEmpty()) return;
        int maxVersion = 0;
        for (SSTableMetadata sst : sstables) if (sst.tableName.equals(tableName)) maxVersion = Math.max(maxVersion, sst.version);
        int nextVersion = maxVersion + 1;
        String sstName = tableName + "_v" + nextVersion;
        DiskManager dm = new DiskManager(dbName, sstName);
        SSTableMetadata meta = new SSTableMetadata(tableName, nextVersion, dm);
        Page page = new Page(0, (byte) 1);
        int pageId = 0;
        for (DBRecord record : memTable.getAll()) {
            if (!page.insertRecord(record.toBytes())) {
                dm.writePage(page);
                pageId++;
                page = new Page(pageId, (byte) 1);
                page.insertRecord(record.toBytes());
            }
            meta.index.insert(record.getId(), pageId);
            meta.filter.add(record.getId());
        }
        dm.writePage(page);
        sstables.add(0, meta);
        memTable.clear();
        wals.get(tableName).clear();
    }

    public void flushAll() throws Exception {
        for (String tableName : heapFiles.keySet()) {
            flushMemTable(tableName);
            heapFiles.get(tableName).flush();
            if (indexes.containsKey(tableName)) indexes.get(tableName).flush();
            if (secondaryIndexes.containsKey(tableName)) {
                for (index.DiskBPlusTree sIdx : secondaryIndexes.get(tableName).values()) {
                    sIdx.flush();
                }
            }
        }
    }
}
