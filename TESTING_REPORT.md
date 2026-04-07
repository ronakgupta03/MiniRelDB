# MiniRelDB Aggressive Testing Report

## 1. Executive Summary
The aggressive testing phase pushed MiniRelDB to its limits using high-volume inserts (10,000+), edge-case data, and persistence cycles. While the engine successfully implemented Zero-Copy I/O and persistent indexing, several critical architectural flaws were uncovered that prevent it from being production-ready.

---

## 2. Discovered Flaws & Bugs

### 🚨 Critical: Data Corruption via Oversized Records
- **Issue**: `Page.insertRecord` did not originally check if a record exceeded the total available space of a fresh page (PAGE_SIZE - Header).
- **Impact**: Oversized records would overwrite memory or return `true` while being truncated, leading to `Negative pageId` or `BufferUnderflow` errors during B+ Tree splits.
- **Status**: Partially fixed in `Page.java`, but the system lacks a "Large Object" (LOB) storage strategy for records > 4KB.

### 🚨 Critical: Delete Operation is Non-Functional
- **Issue**: Stress tests revealed that 1,000 records deleted from the database **still existed** in subsequent `SELECT` queries.
- **Root Cause**: 
    1. `Executor.executeDelete` marks records as deleted in the `MemTable`, but the `HeapFile` scan or `SSTable` scan doesn't correctly filter them out.
    2. The `ahi` (Adaptive Hash Index) is not properly invalidated upon deletion.
- **Evidence**: `ERROR: Record 5000 still exists after deletion` repeated for all deleted ranges.

### 🚨 High: B+ Tree Split Logic Instability
- **Issue**: During 10,000 sequential inserts, the `DiskBPlusTree` frequently encountered `Negative pageId` errors.
- **Root Cause**: Indexing logic was reading from incorrect offsets in the 17-byte header and misinterpreting metadata as leaf data.
- **Impact**: Index corruption occurs under heavy write pressure.

### 🟡 Medium: SQL Parser Limitations
- **Issue**: `sqlParser.parseInsert()` is hardcoded to expect exactly two values: `(id, name)`.
- **Impact**: Cannot insert into tables with more than 2 columns or different column names (e.g., `orders` table with `user_id`).
- **Evidence**: `RegressionTest` failed to populate complex tables due to parser constraints.

### 🟡 Medium: Adaptive Hash Index (AHI) Staleness
- **Issue**: The AHI provides $O(1)$ speed but does not have a TTL or eviction policy.
- **Impact**: Memory usage grows linearly with the number of "hot" records found, and it can serve stale data if a record is updated on disk but not in the AHI map.

---

## 3. Performance Metrics
- **Ingestion Speed**: 10,000 inserts in **364ms** (approx. 27,000 rows/sec). This is excellent and attributed to `mmap` and `MemTable` buffering.
- **Point Lookup (Indexed)**: 1,000 lookups in **6ms** ($0.006ms$ per lookup). This is competitive with MySQL for in-memory hot data.
- **Persistence**: Verified. Data for ID 777 survived a full process kill and restart.

---

## 4. Recommendations for Next Phase
1.  **Implement Tombstones**: To fix the Delete bug, ensure that both `HeapFile.getAllRecords()` and `SSTable` scans strictly honor the `deleted` flag.
2.  **Fix AHI Invalidation**: Every `UPDATE` or `DELETE` must explicitly remove the entry from the `ahi` map.
3.  **Grammar-based Parser**: Replace `sqlParser.java` with ANTLR to support multi-column inserts and complex JOIN conditions.
4.  **Robust B+ Tree Testing**: Implement a dedicated unit test for `DiskBPlusTree` that verifies tree invariants (sorting, pointer validity) after every split.
5.  **Checksum Hardening**: Update `DiskManager` to throw a `RuntimeException` instead of just printing to `stderr` when a checksum fails, preventing the database from operating on corrupt data.
