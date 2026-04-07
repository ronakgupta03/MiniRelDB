# Phase 1: Storage & Metadata Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish the multi-database directory structure and a JSON-based CatalogManager to track schemas and constraints.

**Architecture:** A new `CatalogManager` class will handle all metadata operations, reading from and writing to `data/catalog.json`. The `DiskManager` and `HeapFile` will be updated to support database-specific paths.

**Tech Stack:** Java (Standard Library).

---

### Task 1: Refactor CatalogManager for Multi-Database support

**Files:**
- Modify: `src/main/java/catalog/CatalogManager.java`

- [ ] **Step 1: Define schema classes**
Implement internal classes for `DatabaseSchema`, `TableSchema`, and `ColumnSchema` within `CatalogManager.java`.

- [ ] **Step 2: Implement basic JSON-like persistence**
Implement a simple manual parser to read and write `data/catalog.json` using the new format.

- [ ] **Step 3: Add methods to manage databases and tables**
Add `createDatabase(String name)`, `createTable(String db, TableSchema table)`, and `getTableSchema(String db, String table)`.

### Task 2: Database-Specific Storage Isolation

**Files:**
- Modify: `src/main/java/storage/DiskManager.java`
- Modify: `src/main/java/storage/HeapFile.java`

- [ ] **Step 1: Update `DiskManager` constructor**
Modify `DiskManager` to take `dbName` and `tableName`. It should ensure the directory `data/{dbName}/` exists and use `data/{dbName}/{tableName}.db`.

- [ ] **Step 2: Update `HeapFile` to use the new `DiskManager` initialization**
Ensure it passes the database and table names correctly from the query context.

### Task 3: Update `Executor` to use Metadata

**Files:**
- Modify: `src/main/java/query/Executor.java`
- Modify: `src/main/java/database/Main.java`

- [ ] **Step 1: Integrate `CatalogManager` into `Executor`**
Ensure the `Executor` can look up schemas to validate queries.

- [ ] **Step 2: Default to a "main" database in `Main.java`**
Initialize the system by selecting or creating a default database.
