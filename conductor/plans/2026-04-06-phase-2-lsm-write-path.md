# Phase 2: LSM Write Path (MemTable & WAL) Implementation Plan (COMPLETE)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a Write-Ahead Log (WAL) and an in-memory MemTable to enable high-performance writes while maintaining durability.

**Architecture:** 
1. **MemTable**: A `TreeMap<Integer, DBRecord>` per table to store pending writes in memory. (DONE)
2. **WAL**: A sequential file `data/{dbName}/wal.log` where each write operation is appended before touching the MemTable. (DONE)
3. **Recovery**: On `Executor` startup, the `wal.log` is read, and all operations are re-applied to the MemTable. (DONE)

**Tech Stack:** Java (Standard Library).

---

### Task 1: Implement `MemTable.java` (DONE)

**Files:**
- Create: `src/main/java/storage/MemTable.java`

- [x] **Step 1: Implement the MemTable class**
Use `TreeMap<Integer, DBRecord>` to store records sorted by Primary Key.

- [x] **Step 2: Add methods for CRUD**
Implement `put(DBRecord record)`, `get(int id)`, `delete(int id)`, and `getAll()`.

### Task 2: Implement `WriteAheadLog.java` (DONE)

**Files:**
- Create: `src/main/java/storage/WriteAheadLog.java`

- [x] **Step 1: Implement the WAL class**
Implement a simple binary or text-based logger to `data/{dbName}/wal.log`.

- [x] **Step 2: Add `append(String operation, DBRecord record)`**
Each operation should log the type (INSERT/UPDATE/DELETE) and the record data.

### Task 3: Integrate MemTable and WAL into `Executor` (DONE)

**Files:**
- Modify: `src/main/java/query/Executor.java`

- [x] **Step 1: Update `executeInsert` and `executeDelete`**
Ensure every write first goes to the `WAL`, then to the `MemTable`.

- [x] **Step 2: Implement Startup Recovery**
In the `Executor` constructor, read `wal.log` and populate the `MemTable`.

- [x] **Step 3: Update `executeSelect`**
Ensure it checks the `MemTable` first, then the `HeapFile` (Unified View).
