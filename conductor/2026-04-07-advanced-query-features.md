# Implementation Plan: Advanced Query & Schema Features

This plan transitions MiniRelDB to a schema-flexible engine with professional-grade join algorithms and index optimizations.

## 1. Objective
Enable arbitrary column support, implement $O(N+M)$ Hash Joins, and optimize read paths with Covering Indexes.

## 2. Phase 1: Schema Flexibility (The Generic Record)
Currently, `DBRecord` and the parser are hardcoded to `(id, name)`. We need to make them dynamic.

### Changes:
- **`storage/DBRecord.java`**:
    - Replace `int id, String name` with `Map<String, Object> values`.
    - Update `toBytes()` and `fromBytes()` to use `CatalogManager` schema info to determine how many bytes to read/write for each column.
- **`query/sqlParser.java`**:
    - Refactor `parseInsert` to support `INSERT INTO table (cols...) VALUES (vals...)`.
    - Support specific columns in `SELECT` (e.g., `SELECT id, age FROM users`).
- **`query/Executor.java`**:
    - Update `executeInsert` to fetch the table's `TableSchema` before creating the `DBRecord`.

### Verification:
- Success: `INSERT INTO orders VALUES (1, 500, 'Pending')` works correctly for a 3-column table.

---

## 3. Phase 2: Proper Join Algorithms (Hash Join)
Replace the $O(N \times M)$ nested-loop join with a Hash Join.

### Changes:
- **`query/PlanNode.java`**:
    - Implement `HashJoinNode`. 
    - **Open()**: Read the entire "Build" side (smaller table) into an in-memory `HashMap<Object, List<DBRecord>>` keyed by the join column.
    - **Next()**: Stream the "Probe" side (larger table) and look up matches in the hash map.
- **`query/Executor.java`**:
    - Update the plan generator to use `HashJoinNode` instead of the old `JoinNode`.

### Verification:
- Benchmark: Join two 5,000-row tables. Hash Join should be significantly faster than Nested-Loop.

---

## 4. Phase 3: Covering Index Optimization
Minimize Disk I/O by fulfilling queries using only index data.

### Changes:
- **`query/SelectQuery.java`**: Track the list of requested columns.
- **`query/Executor.java`**:
    - In `executeSelect`, compare `requestedColumns` with the primary key and secondary indexes.
    - If the query only requests the `id` (Primary Key) and we are searching by `id`, return the data directly from the `DiskBPlusTree` result without calling `HeapFile.getRecordByPageId`.

### Verification:
- Performance: `SELECT id FROM users WHERE id = 5` should show 0 reads from `users.db` (HeapFile) and only reads from `users.idx`.

---

## 5. Summary of Key Files Affected
- `src/main/java/storage/DBRecord.java` (Serialization refactor)
- `src/main/java/query/sqlParser.java` (Generic parsing)
- `src/main/java/query/PlanNode.java` (Hash Join logic)
- `src/main/java/query/Executor.java` (Optimization logic)
- `src/main/java/catalog/CatalogManager.java` (Schema metadata provider)
