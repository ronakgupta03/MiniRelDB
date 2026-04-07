# Phase 3: SSTable (Persistent B+ Tree) & Bloom Filter Implementation Plan (COMPLETE)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement persistent SSTables (Sorted String Tables) to store MemTable flushes and use Bloom Filters to optimize reads across multiple files.

**Architecture:** 
1. **Flush**: When the MemTable exceeds a size threshold (e.g., 2MB or a manual `flush` command), it is written to a new `.db` file in sorted order. (DONE)
2. **Bloom Filter**: For each `.db` file, an in-memory bitset is maintained to quickly skip files that definitely don't contain a specific ID. (DONE)
3. **Multi-File Search**: The `Executor` searches the `MemTable`, then checks `.db` files in reverse-chronological order (newest first). (DONE)

**Tech Stack:** Java (Standard Library).

---

### Task 1: Implement `BloomFilter.java` (DONE)

**Files:**
- Create: `src/main/java/index/BloomFilter.java`

- [x] **Step 1: Implement the Bloom Filter class**
Implement a simple bitset-based filter with 2-3 hash functions.

- [x] **Step 2: Add methods**
`add(int id)` and `mightContain(int id)`.

### Task 2: Implement SSTable Flush logic (DONE)

**Files:**
- Modify: `src/main/java/query/Executor.java`

- [x] **Step 1: Implement `flushMemTable` in `Executor`**
Add logic to take all `MemTable` records, sort them, and write to a new versioned file (e.g., `table_v1.db`, `table_v2.db`).

- [x] **Step 2: Clear WAL on Flush**
Once a MemTable is safely on disk, the WAL should be cleared/truncated.

### Task 3: Implement Unified Multi-File Search (DONE)

**Files:**
- Modify: `src/main/java/query/Executor.java`

- [x] **Step 1: Track multiple SSTables per table**
Keep a list of `SSTableMetadata` instances (one per `.db` file) and their associated `BloomFilters`.

- [x] **Step 2: Update `executeSelect`**
Implement the search sequence: MemTable -> newest SSTable -> ... -> oldest SSTable. Use Bloom Filters to skip files.
