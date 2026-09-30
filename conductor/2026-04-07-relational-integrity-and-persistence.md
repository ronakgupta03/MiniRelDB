# Implementation Plan: Relational Integrity & Persistence Hardening

This plan fixes critical bugs discovered during the "Death-March" testing phase, focusing on data loss during schema application, missing relational constraints, and persistence consistency.

## 1. Objective
Fix data loss in `applySchema`, enforce Primary Key uniqueness, support generic secondary indexes, and ensure full durability across restarts.

## 2. Phase 1: Fix Data Loss in `applySchema` (Critical)
The current `applySchema` overwrites the `values` map and loses any data not keyed as `colN`.

### Changes:
- **`storage/DBRecord.java`**:
    - Update `applySchema(TableSchema schema)`:
        - Check if `values` already contains the column name from the schema.
        - If so, keep it.
        - If not, try to map from `colN`.
        - This preserves data during the `executeInsert` -> `applySchema` flow.

### Verification:
- Run `IntenseTest` and verify STAGE 4 (Londoners) now returns ~5,000 results.

---

## 3. Phase 2: Relational Integrity (PK Uniqueness)
Enforce that no two records can have the same Primary Key.

### Changes:
- **`query/Executor.java`**:
    - In `executeInsert`:
        - Before performing the insert, check `indexes.get(tableName).search(query.getId())`.
        - If a value is returned, throw an `Exception("Duplicate entry for primary key")`.
        - Also check the `MemTable` for that ID.

### Verification:
- Run `IntenseTest` and verify STAGE 5 now reports "SUCCESS: Duplicate PK rejected".

---

## 4. Phase 3: Generic Secondary Indexes
Support `CREATE INDEX` on any column, not just `id` or `name`.

### Changes:
- **`query/Executor.java`**:
    - In `executeCreateIndex`:
        - Use `record.getValue(colName)` to get the value for indexing.
        - Map the `hashCode()` of the value to the Primary ID.

### Verification:
- Run `IntenseTest` and verify secondary index creation works for 'city'.

---

## 5. Phase 4: Persistence Hardening
Ensure all data is physically on disk before the process exits.

### Changes:
- **`storage/DiskManager.java`**:
    - Add a `flush()` method that calls `mappedBuffer.force()`.
- **`storage/HeapFile.java`**:
    - Update `flush()` to call `diskManager.flush()`.
- **`query/Executor.java`**:
    - Update `flushAll()` to ensure all `DiskManager` instances (including SSTables) are flushed and closed if possible.
    - Ensure `DiskBPlusTree` root updates are durable.

### Verification:
- Run `IntenseTest` recovery phase and verify the marker record 99999 is found.

---

## 6. Summary of Key Files Affected
- `src/main/java/storage/DBRecord.java` (applySchema fix)
- `src/main/java/query/Executor.java` (PK check, generic index, flush hardening)
- `src/main/java/storage/DiskManager.java` (force() call)
- `src/main/java/storage/HeapFile.java` (flush propagation)
