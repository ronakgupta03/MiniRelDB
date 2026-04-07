# MiniRelDB Technical Reference

This document provides a detailed, file-by-file technical breakdown of the MiniRelDB source code. It is intended for developers who want to understand the internal implementation of the database engine.

## Table of Contents
1.  [Storage Layer (`src/main/java/storage/`)](#1-storage-layer)
2.  [Index Layer (`src/main/java/index/`)](#2-index-layer)
3.  [Query Layer (`src/main/java/query/`)](#3-query-layer)
4.  [Database Layer (`src/main/java/database/`)](#4-database-layer)
5.  [Buffer Layer (`src/main/java/buffer/`)](#5-buffer-layer)
6.  [Catalog Layer (`src/main/java/catalog/`)](#6-catalog-layer)

---

## 1. Storage Layer
The storage layer is responsible for the physical management of data on disk.

### `storage/DiskManager.java`

-   **Purpose**: To provide a low-level API for reading and writing fixed-size pages to the database file on disk. It abstracts the direct file I/O operations.
-   **Implementation Details**: Uses `java.io.RandomAccessFile` to perform direct read/write operations at specific byte offsets within a file. Each page is located by calculating `pageId * PAGE_SIZE`. It also interacts with a `BufferManager` to cache pages in memory, reducing disk I/O.
-   **Key Methods**:
    -   `writePage(Page page)`: Writes a `Page` object's byte data to the corresponding offset in the database file. It also places the page in the buffer pool.
    -   `readPage(int pageId)`: Reads a 4KB block of data from the file at the calculated offset. Before reading from disk, it checks the buffer pool for a cached version of the page.
    -   `getPageCount()`: Calculates the total number of pages in the database file based on its total size.
-   **Data Structures**: Interacts with the `BufferManager`'s cache (`LinkedHashMap`).
-   **Dependencies**:
    -   `storage.Page`: Represents the unit of data being read or written.
    -   `buffer.BufferManager`: Used to cache pages and reduce disk access.

### `storage/HeapFile.java`

-   **Purpose**: To manage a collection of pages as a simple heap file structure. It provides a higher-level abstraction for inserting and retrieving records without needing to know the specific page details.
-   **Implementation Details**: Maintains a reference to a `currentPage` which is an in-memory buffer for new records. When this page becomes full, `insertRecord` writes it to disk via the `DiskManager` and allocates a new `currentPage`.
-   **Key Methods**:
    -   `insertRecord(DBRecord record)`: Serializes the record and tries to fit it into the `currentPage`. If the page is full, it flushes the current page to disk and places the record in a new one.
    -   `getAllRecords()`: Performs a full table scan by iterating through every page on disk (via `DiskManager`) and deserializing all records within them.
    -   `deleteRecord(int id)`: Scans all pages to find the record with the matching ID, marks its `deleted` flag as true, and overwrites the record in place on disk.
    -   `flush()`: Explicitly writes the `currentPage` to disk. This is crucial for ensuring data persistence on shutdown.
-   **Data Structures**: Holds a `Page` object as `currentPage`.
-   **Dependencies**:
    -   `storage.DiskManager`: To read and write pages from/to the disk.
    -   `storage.Page`: To hold records in memory before flushing.
    -   `storage.DBRecord`: To define the records being stored.

### `storage/Page.java`

-   **Purpose**: Represents a single, fixed-size (4KB) page, which is the fundamental unit of storage.
-   **Implementation Details**: A page is essentially a `byte[4096]` array. The first 4 bytes are reserved to store the `freeSpaceOffset`, an integer that indicates the end of the used space within the page. This header is critical for persistence, allowing the engine to know where to write new data when a page is read from disk.
-   **Key Methods**:
    -   `insertRecord(byte[] recordBytes)`: Copies the given record's bytes into the `data` array at the current `freeSpaceOffset` and then updates the offset.
    -   `getAllRecords()`: Iterates through the byte data from the 4-byte header up to the `freeSpaceOffset`, deserializing each record it finds using `DBRecord.fromBytes`.
    -   `setData(byte[] data)`: Populates the page's data and, importantly, reads the first 4 bytes to initialize its `freeSpaceOffset` member variable.
-   **Data Structures**: A `byte[]` array named `data`.
-   **Dependencies**: `storage.DBRecord`: For deserializing raw bytes into record objects.

### `storage/DBRecord.java`

-   **Purpose**: To define the structure of a single database record and handle its serialization and deserialization.
-   **Implementation Details**: A record consists of a deleted flag, an ID, and a name. The `toBytes()` method converts these fields into a byte array with a specific format: `[deleted_flag (1 byte)] [id (4 bytes)] [name_length (4 bytes)] [name_bytes (variable)]`. The `fromBytes()` static method performs the reverse operation.
-   **Key Methods**:
    -   `toBytes()`: Serializes the record object into a `byte[]`.
    -   `fromBytes(byte[] data)`: A static factory method that deserializes a `byte[]` into a new `DBRecord` object.
    -   `setDeleted(boolean deleted)`: Marks the record for soft deletion.
-   **Data Structures**: None.
-   **Dependencies**: `java.nio.ByteBuffer` for easier serialization/deserialization.

### `storage/MemTable.java`

-   **Purpose**: An in-memory table (cache) for recent database writes (inserts, updates, deletes). It follows a Log-Structured Merge-Tree (LSM) architecture principle.
-   **Implementation Details**: Uses a `java.util.TreeMap` to store records, which keeps them sorted by their primary key (the integer `id`). This allows for efficient, ordered retrieval.
-   **Key Methods**:
    -   `put(DBRecord record)`: Adds or overwrites a record in the `TreeMap`.
    -   `get(int id)`: Retrieves a single record by its ID.
    -   `delete(int id)`: Performs a soft delete by setting the record's `deleted` flag to true.
-   **Data Structures**: A `TreeMap<Integer, DBRecord>` to store records sorted by key.
-   **Dependencies**: `storage.DBRecord`.

### `storage/WriteAheadLog.java`

-   **Purpose**: To provide durability for writes. Before a write operation is applied to the `MemTable`, it is first recorded in the WAL. If the system crashes, the WAL can be replayed to restore the `MemTable` to its state before the crash.
-   **Implementation Details**: It opens a file and appends log entries using `java.io.DataOutputStream`. Each entry consists of an operation type (e.g., insert/delete), the length of the record data, and the record data itself.
-   **Key Methods**:
    -   `append(byte opType, DBRecord record)`: Writes a new log entry to the WAL file and flushes it to disk to guarantee it is written.
    -   `recover()`: Reads the entire WAL file from the beginning, deserializes each log entry, and returns them as a list. This is used on startup to rebuild the `MemTable`.
    -   `clear()`: Deletes and recreates the log file, typically after a `MemTable` has been successfully flushed to a persistent SSTable.
-   **Data Structures**: `WriteAheadLog.LogEntry` is a static inner class used to represent a recovered log entry.
-   **Dependencies**: `storage.DBRecord`.

---

## 2. Index Layer
The index layer provides data structures for fast data retrieval, avoiding full table scans.

### `index/BPlusTree.java`

-   **Purpose**: An in-memory B+ Tree implementation that maps a primary key (`id`) to the `pageId` where the corresponding record is stored. This is the primary mechanism for fast `SELECT ... WHERE id=` lookups.
-   **Implementation Details**: The tree is composed of `InternalNode` and `LeafNode` objects. Internal nodes contain keys and pointers to child nodes, while leaf nodes contain keys and the actual data values (in this case, `pageId`s). Leaf nodes are also linked together in a sequence to allow for efficient range scans. The tree has a fixed `ORDER` which determines its width.
-   **Key Methods**:
    -   `insert(int key, int value)`: Traverses the tree to find the correct leaf node and inserts the key-value pair. If a node becomes full, it is split, and a key is promoted to the parent, which may cause further splits up to the root.
    -   `search(int key)`: Traverses the tree to a leaf node to find the value associated with a given key.
    -   `delete(int key)`: Finds the key in a leaf node and removes it. (Note: This implementation does not handle node merging if they become under-full, which is a feature of a complete B+ Tree).
-   **Data Structures**:
    -   `Node`, `InternalNode`, `LeafNode`: Abstract and concrete classes representing the tree structure.
    -   `Split`: A private static class used to pass information about a split operation up the recursion stack.
-   **Dependencies**: None.

### `index/BloomFilter.java`

-   **Purpose**: A probabilistic data structure used to quickly test whether an element is a member of a set. In MiniRelDB, it's used by SSTables to quickly determine if a record ID *might* be in that table file, avoiding a disk read if it's definitely not present.
-   **Implementation Details**: Uses a `java.util.BitSet` as the underlying bit array. It employs two different hash functions (`hash1`, `hash2`) to set bits in the array.
-   **Key Methods**:
    -   `add(int id)`: Hashes the ID using both hash functions and sets the corresponding bits in the `BitSet`.
    -   `mightContain(int id)`: Hashes the ID and checks if *both* corresponding bits are set. If not, the ID is definitely not in the set. If they are, it *might* be.
-   **Data Structures**: `java.util.BitSet`.
-   **Dependencies**: None.

---

## 3. Query Layer
The query layer is responsible for parsing and executing SQL commands.

### `query/sqlParser.java`

-   **Purpose**: A hand-written, top-down parser for a small subset of SQL. It converts a raw SQL string into a structured query object.
-   **Implementation Details**: It works by checking for keywords (`insert`, `select`, etc.) at the start of the query string (case-insensitively). Based on the keyword, it calls a corresponding private parse method (e.g., `parseInsert()`). These methods use string manipulation and splitting to extract components like table names, values, and `WHERE` clause conditions.
-   **Key Methods**:
    -   `parse()`: The main entry point. It inspects the query and delegates to the appropriate specialized parsing method.
    -   `parseInsert()`, `parseSelect()`, `parseUpdate()`, `parseDelete()`: Private methods that handle the specific syntax of each DML statement.
-   **Data Structures**: None.
-   **Dependencies**: All the specific query classes (`InsertQuery`, `SelectQuery`, etc.).

### `query/Executor.java`

-   **Purpose**: The central "brain" of the database. It takes a parsed query object from `sqlParser` and orchestrates the necessary operations across the storage, index, and catalog layers to fulfill the query.
-   **Implementation Details**: The executor manages the active database instance, including its associated `HeapFile`, `BPlusTree` index, `MemTable`, and `WriteAheadLog` for each table. It handles the logic for both reading and writing data, including the LSM tree architecture (MemTable flushing to SSTables).
-   **Key Methods**:
    -   `execute<Command>(...Query query)`: A series of methods (e.g., `executeInsert`, `executeSelect`) that contain the logic for handling each query type.
    -   `rebuildIndex(String tableName)`: Called on startup to scan the entire heap file and populate the in-memory B+ Tree index.
    -   `recoverFromWal(String tableName)`: Replays the Write-Ahead Log on startup to restore the MemTable.
    -   `flushMemTable(String tableName)`: The core of the LSM-style persistence. It writes the contents of a `MemTable` to a new, immutable, versioned SSTable file on disk and then clears the `MemTable` and WAL.
    -   `executePlan(PlanNode plan)`: Executes a query execution plan by iterating through the plan tree.
-   **Data Structures**:
    -   `Map<String, HeapFile>`, `Map<String, BPlusTree>`, etc. to manage components per table.
    -   `List<SSTableMetadata>` to manage the sorted list of SSTables.
-   **Dependencies**: Nearly all other packages: `storage`, `index`, `catalog`, and other `query` classes.

### `query/CommandParser.java`

-   **Purpose**: An older, simpler command parser. It appears to be superseded by `sqlParser.java`, which handles more complex SQL syntax. This class likely remains for legacy or testing purposes.
-   **Implementation Details**: Uses a simple `split(" ")` approach to parse commands.
-   **Key Methods**: `parse(String input)`.
-   **Dependencies**: `InsertQuery`, `SelectQuery`.

### `query/PlanNode.java`

-   **Purpose**: Defines a query execution plan tree using the Volcano iterator model. Each node in the tree is an operator (like Scan, Filter, Join) that can be opened, iterated through (`next()`), and closed.
-   **Implementation Details**: It's an interface with several nested static classes that implement it:
    -   `ScanNode`: The leaf of a plan, responsible for scanning all records from a table.
    -   `FilterNode`: A node that wraps another node and applies a filtering condition.
    -   `JoinNode`: A node that performs a join between its child's output and another table. It implements a basic index-nested loop join.
    -   `SubqueryNode`: A node that executes a subquery and provides its results to a parent node (like a `FilterNode`).
-   **Key Methods**: `open()`, `next()`, `close()`.
-   **Dependencies**: `storage.DBRecord`, `query.Executor`.

### Other Query Classes
Classes like `InsertQuery`, `SelectQuery`, `UpdateQuery`, `DeleteQuery`, `CreateTableQuery`, etc., are simple Plain Old Java Objects (POJOs) that act as data containers. They are the output of the `sqlParser` and the input to the `Executor`. They hold the structured information extracted from the raw SQL string.

---

## 4. Database Layer
The database layer contains the main entry point for the application.

### `database/Main.java`

-   **Purpose**: The main entry point for the interactive database console (REPL).
-   **Implementation Details**: It initializes the `CatalogManager` and the `Executor`. It then enters an infinite loop, reading user input line by line using `java.util.Scanner`. Each line is passed to the `sqlParser`, and the resulting query object is passed to the `Executor`. It handles special commands like `exit` and database switching (`USE`).
-   **Key Methods**: `main(String[] args)`.
-   **Data Structures**: None.
--  **Dependencies**: `query.*`, `storage.*`, `catalog.CatalogManager`.

### `database/Database.java`

-   **Purpose**: Appears to be an obsolete or placeholder entry point.
-   **Implementation Details**: Contains a single `main` method that prints a startup message. The actual application logic resides in `database.Main`.
-   **Dependencies**: None.

---

## 5. Buffer Layer
The buffer layer is intended to manage memory by caching disk pages.

### `buffer/BufferManager.java`

-   **Purpose**: To provide an in-memory, page-level cache to reduce disk I/O. When a page is requested, the buffer manager is checked first.
-   **Implementation Details**: It uses a `java.util.LinkedHashMap` configured as an LRU (Least Recently Used) cache. When the cache exceeds its capacity, the oldest-accessed entry is automatically evicted.
-   **Key Methods**:
    -   `getPage(String key)`: Retrieves a page from the cache.
    -   `putPage(String key, Page page)`: Inserts a page into the cache.
-   **Data Structures**: A `LinkedHashMap<String, Page>` to implement the LRU cache.
-   **Dependencies**: `storage.Page`.

---

## 6. Catalog Layer
The catalog layer is responsible for managing database metadata (schemas).

### `catalog/CatalogManager.java`

-   **Purpose**: Manages all metadata about databases, tables, columns, and other schema information.
-   **Implementation Details**: It maintains a map of database schemas in memory. This catalog is persisted to a simple text file (`data/catalog.json`) in a custom pipe-delimited format. The `saveCatalog` and `loadCatalog` methods handle the serialization and deserialization of this metadata to and from the file.
-   **Key Methods**:
    -   `createDatabase(String name)`, `createTable(...)`: Methods to add new schema elements to the catalog.
    -   `getTableSchema(...)`: Retrieves the schema for a specific table.
    -   `saveCatalog()`: Writes the in-memory schema representation to the flat file.
    -   `loadCatalog()`: Parses the catalog file on startup to populate the in-memory schema maps.
-   **Data Structures**:
    -   `DatabaseSchema`, `TableSchema`, `ColumnSchema`, `ForeignKeySchema`: Nested static classes that model the schema hierarchy.
    -   `Map<String, DatabaseSchema>`: The main in-memory store for the catalog.
-   **Dependencies**: None.
