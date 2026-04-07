# Implementation Plan: Post-Testing Stabilization

This plan addresses the critical architectural flaws and bugs uncovered during the aggressive testing phase.

## 1. Objective
Ensure query correctness (fixing deletions), prevent memory leaks (AHI LRU), and stabilize the B+ Tree indexing logic.

## 2. Phase 1: Query Correctness & Integrity (High Priority)
Fix the issue where deleted records still appear in results and ensure the database halts on corruption.

### Changes:
- **`storage/Page.java`**: 
    - Ensure `getAllRecords()` strictly skips records with the `deleted` flag set.
- **`storage/DiskManager.java`**:
    - Change the CRC32 mismatch from a warning to a `RuntimeException` or `IOException` to prevent data corruption spread.
- **`query/Executor.java`**:
    - **AHI Invalidation**: In `executeDelete` and `executeUpdate`, explicitly remove the ID from the `ahi` map for that table.
    - **Full Scan Logic**: Ensure full table scans across HeapFiles and SSTables honor the tombstone (deleted flag).

### Verification:
- Run `StressTest` and verify that the `ERROR: Record 5000 still exists after deletion` message no longer appears.

---

## 3. Phase 2: Memory & Resource Management (Fix 5)
Prevent the Adaptive Hash Index (AHI) from growing indefinitely.

### Changes:
- **`query/Executor.java`**:
    - Replace the inner `HashMap` in `ahi` with a `LinkedHashMap` configured for LRU eviction.
    - Set a hard limit (e.g., 10,000 entries per table) to prevent OOM errors during mass ingestion.

### Verification:
- Run a modified `StressTest` with 100,000 records and monitor memory usage; ensure it plateaus.

---

## 4. Phase 3: B+ Tree Stability & Record Safety (Fix 2, 3)
Stabilize the `DiskBPlusTree` and prevent oversized record crashes.

### Changes:
- **`index/DiskBPlusTree.java`**:
    - Implement a `verifyTree()` method to check invariants (sorted keys, valid child pointers).
    - Fix the recursive `insertInternal` logic to ensure keys are promoted correctly during root splits.
- **`storage/DBRecord.java`**:
    - Add a `validate()` method that checks if the serialized size exceeds `PAGE_SIZE - 17`.
- **`query/Executor.java`**:
    - Call `record.validate()` before any insert/update and throw a descriptive `SQLException` if it fails.

### Verification:
- Run `RegressionTest` and verify that the "Long String Test" now throws a clear "Record too large" error instead of a `Negative pageId` exception.

---

## 5. Summary of Key Files Affected
- `src/main/java/storage/Page.java` (Deletion filtering)
- `src/main/java/storage/DiskManager.java` (Hardened integrity)
- `src/main/java/storage/DBRecord.java` (Size validation)
- `src/main/java/index/DiskBPlusTree.java` (Split logic stabilization)
- `src/main/java/query/Executor.java` (AHI LRU & Invalidation)
