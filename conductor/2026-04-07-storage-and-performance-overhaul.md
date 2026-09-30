# Implementation Plan: Storage, Indexing, and Performance Overhaul

This plan outlines the steps to transition MiniRelDB from an educational prototype to a high-performance, MySQL-competitive database engine.

## 1. Objective
Implement a disk-persistent Clustered B+ Tree storage engine, enhanced indexing capabilities (secondary indexes, node merging), and advanced speed optimizations (mmap, adaptive indexing).

## 2. Phase 1: Disk-Persistent Clustered B+ Tree (Category 1)
Currently, the B+ Tree is in-memory and the data is in a Heap File. We will merge these into a **Clustered Index** where the B+ Tree *is* the storage.

### Changes:
- **`storage/Page.java`**: Add a `pageType` byte to the header (0: Internal Node, 1: Leaf Node, 2: Metadata).
- **`index/DiskBPlusTree.java` (New)**: 
    - Implement a B+ Tree where every "Node" is a `Page`.
    - **Internal Nodes**: Store `(Key, ChildPageId)` pairs.
    - **Leaf Nodes**: Store `(Key, DBRecord)` pairs (Clustered).
- **`storage/DiskManager.java`**: Update to manage a "Free Page List" (linked list of deleted pages) to reuse space.
- **`query/Executor.java`**: Remove `HeapFile` dependency. All CRUD operations will now go through `DiskBPlusTree`.

### Verification:
- Unit test: Insert 10,000 records, restart the DB, and verify all 10,000 are retrievable via the tree without a "rebuild" step.

---

## 3. Phase 2: Indexing Enhancements & Reliability (Category 1 & 4)
Improving the efficiency and durability of the indexing layer.

### Changes:
- **B+ Tree Node Merging**: Update the delete logic in `DiskBPlusTree` to handle "Underflow". If a node is less than 50% full, merge it with a sibling or redistribute keys.
- **Secondary Indexes**:
    - Create a new file structure `table_name.idx` for non-primary keys.
    - These indexes map `(SecondaryKey, PrimaryKey)`.
- **Page Checksums**:
    - Add a 4-byte CRC32 checksum to the `Page` header.
    - `DiskManager` will verify the checksum on every `readPage()` and update it on `writePage()`.

### Verification:
- Stress test: Delete 50% of a large table and verify the file size (or page count) decreases or stays stable due to merging/reuse.
- Corruption test: Manually flip a bit in the `.db` file and verify the engine throws a "Checksum Mismatch" error.

---

## 4. Phase 3: High-Performance "Speed Boosters" (Category 5)
Implementing features to outpace traditional database I/O overhead.

### Changes:
- **Zero-Copy I/O (mmap)**:
    - In `DiskManager.java`, use `java.nio.MappedByteBuffer` to map the entire database file into the process's virtual memory address space.
    - This allows the OS to handle page caching and reduces the overhead of copying data between the kernel and JVM.
- **Adaptive Hash Index (AHI)**:
    - In the `Executor`, maintain a small, in-memory `HashMap<Integer, RecordReference>`.
    - If a specific `id` is queried frequently (e.g., > 10 times), cache its direct memory/page location in the AHI to bypass B+ Tree traversal ($O(1)$ instead of $O(\log N)$).
- **Covering Index Optimization**:
    - Update `PlanNode` logic. If a `SELECT col1 FROM table WHERE col1 = X` is called and `col1` is indexed, the executor should return the value directly from the index without fetching the full record.

### Verification:
- Benchmark: Compare `SELECT * WHERE id=X` latency before and after AHI implementation.
- Benchmark: Compare bulk-insert speed with `RandomAccessFile` vs `MappedByteBuffer`.

---

## 5. Summary of Key Files Affected
- `src/main/java/storage/DiskManager.java` (mmap, checksums, free list)
- `src/main/java/storage/Page.java` (node headers, checksums)
- `src/main/java/index/DiskBPlusTree.java` (New core logic)
- `src/main/java/query/Executor.java` (AHI, secondary index coordination)
- `src/main/java/query/PlanNode.java` (Covering index logic)
