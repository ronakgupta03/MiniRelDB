# Spec: MiniRelDB v2 - High-Performance LSM/B+ Tree Hybrid

## 1. Objective
Transform MiniRelDB into a production-grade database engine supporting multiple databases, complex queries (Joins/Subqueries), and high-performance I/O using a hybrid LSM-Tree (Write) and B+ Tree (Read) architecture.

## 2. Storage Architecture

### 2.1 Multi-Database Directory Structure
The system uses a directory-per-database approach to isolate I/O and simplify management.
- `data/catalog.json`: Central metadata repository (Human Readable).
- `data/{db_name}/`: Namespace for a specific database.
- `data/{db_name}/{table_name}.db`: Persistent SSTable (B+ Tree) for a table.
- `data/{db_name}/wal.log`: Sequential log for all pending writes.

### 2.2 System Catalog (`catalog.json`)
Stores the "Schema" of the entire system.
- **Database List**: Active databases.
- **Table Definitions**: Columns (Name, Type, Length).
- **Integrity Constraints**: Primary Keys (PK) and Foreign Keys (FK).

## 3. High-Performance I/O (LSM + B+ Tree Hybrid)

### 3.1 The Write Path (LSM Style)
1. **WAL (Write-Ahead Log)**: Every write is first appended sequentially to `wal.log`.
2. **MemTable**: Data is inserted into an in-memory `TreeMap<Integer, DBRecord>`.
3. **Flush**: When MemTable >= 2MB, it is converted into a sorted array and written to disk as a new `.db` file (SSTable).

### 3.2 The Read Path (B+ Tree Style)
1. **MemTable Search**: Check RAM first for the most recent version of a record.
2. **Bloom Filter**: Check an in-memory bitset for each `.db` file to see if the ID *might* exist there.
3. **SSTable Search**: If the Bloom Filter passes, perform a B+ Tree lookup within the `.db` file.
4. **Unified View**: Merge results from MemTable and Disk, prioritizing the MemTable version.

## 4. Query Engine (Logic Layer)

### 4.1 Relational Algebra Operators
Queries are parsed into a tree of executable operators:
- `ScanNode`: Reads rows from a table (LSM/B+ Tree lookup).
- `FilterNode`: Applies `WHERE` conditions.
- `JoinNode`: Implements **Index-Nested Loop Join**.
- `ProjectNode`: Filters columns for output.

### 4.2 Constraints & Normalization
- **Primary Key**: Ensures uniqueness during MemTable insertion.
- **Foreign Key**: Before an `INSERT` into a child table, the `Executor` performs a B+ Tree lookup on the parent table to verify the reference.

## 5. Error Handling & Robustness
- **Recovery**: On startup, if `wal.log` is non-empty, the system replays it to rebuild the MemTable.
- **Validation**: Schema-strictness for all inputs (no more silent lowercase conversions unless requested).

## 6. Phase 1 Implementation Goals
1. Implement `CatalogManager` (JSON) and multi-directory logic.
2. Implement `MemTable` and `WAL` for persistent, fast writes.
3. Implement `SSTable` (Persistent B+ Tree) format.
4. Update `Executor` to support `JOIN` via Index-Nested Loop.
