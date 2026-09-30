# MiniRelDB Technical Documentation

## 1. Project Overview

**MiniRelDB** is a from-scratch relational database engine implemented in Java. It serves as an educational tool to demonstrate the core internal components of a database management system (DBMS). The project focuses on the fundamental layers, including disk-based storage, B+ Tree indexing, a custom SQL query processor, and data persistence across sessions.

- **Core Technology**: Java
- **Key Features**:
    - Disk-based, paged storage model with data persistence.
    - In-memory B+ Tree for primary key indexing, rebuilt on startup.
    - Hand-written, case-insensitive SQL parser for CRUD operations.
    - Functional UPDATE and DELETE operations.
    - Interactive command-line (REPL) interface.

---

## 2. System Architecture

MiniRelDB is designed with a layered architecture, separating concerns to mimic the design of production-grade database systems.

```
+---------------------+
|   Console (REPL)    |  <-- src/main/java/database/
+---------------------+
|    Query Layer      |  <-- src/main/java/query/
| (Parser & Executor) |
+---------------------+
|     Index Layer     |  <-- src/main/java/index/
|    (B+ Tree)        |
+---------------------+
|    Storage Layer    |  <-- src/main/java/storage/
| (Disk, Pages, Heap) |
+---------------------+
```

### 2.1. Storage Layer (`src/main/java/storage/`)

This layer is responsible for all physical data management on disk.

- **`DiskManager.java`**: The lowest-level component. It directly interacts with the database file (`data/database.db`) using `RandomAccessFile`. It is responsible for reading and writing fixed-size blocks of data called "pages".
- **`Page.java`**: Represents a single 4KB page. Its structure is now persistent, with the first 4 bytes of the page data reserved for the `freeSpaceOffset`. This allows the engine to track available space within a page even after a restart.
- **`HeapFile.java`**: Provides a higher-level abstraction over a collection of pages. It manages an in-memory `currentPage` buffer for new records. When this page is full, or when the system shuts down, its `flush()` method is called by the `Main` loop to write the buffer to disk, ensuring data persistence.
- **`DBRecord.java`**: Defines the structure and serialization of a data record. Records support soft deletion via a `deleted` flag.
    - **Serialization Format**: `[deleted_flag (1 byte)] [id (4 bytes)] [name_length (4 bytes)] [name_bytes (variable)]`

### 2.2. Index Layer (`src/main/java/index/`)

This layer provides fast, indexed access to data, avoiding costly full-table scans.

- **`BPlusTree.java`**: Implements an in-memory B+ Tree.
    - It maps a record's integer `id` (the key) to the `pageId` (the value) where that record is stored.
    - To ensure consistency across restarts, the index is **rebuilt on startup** by the `Executor`, which scans all records from the database file.
    - When a `SELECT ... WHERE id=` query is executed, the executor uses this tree to find the exact page to read, dramatically improving lookup performance.

### 2.3. Query Layer (`src/main/java/query/`)

This layer processes user-submitted SQL strings, executes them, and returns results.

- **`sqlParser.java`**: A hand-written parser for a small subset of SQL. It has been enhanced to be more user-friendly:
    - **Case-Insensitive Keywords**: SQL keywords (`SELECT`, `INSERT`, `UPDATE`, `DELETE`, etc.) are processed regardless of their case.
    - **Case-Preserved Literals**: String literals (e.g., `'Alice'`) are stored with their original case intact.
- **`Executor.java`**: The brain of the query layer. It takes a parsed query object and coordinates actions between the storage and index layers.
    - **Index Rebuilding**: Upon initialization, the Executor immediately calls its `rebuildIndex()` method. This process iterates through every page on disk, reads every record, and repopulates the B+ Tree index, ensuring the index is always in sync with the persisted data.
    - **`INSERT`**: Stores the record via the `HeapFile` and updates the `BPlusTree` with the new `id -> pageId` mapping.
    - **`DELETE`**: Performs a soft-delete by instructing the `HeapFile` to mark the record as deleted on disk. It then removes the corresponding entry from the `BPlusTree` to prevent it from being found in future lookups.
    - **`UPDATE`**: Finds the target record's page via the index, overwrites its data on the page with the new values, and writes the modified page back to disk.

### 2.4. Buffer & Catalog Layers (`src/main/java/{buffer,catalog}/`)

These layers are included in the project structure but are currently unimplemented.

- **`BufferManager.java`**: A placeholder for a future buffer pool that would cache frequently accessed pages in memory to reduce disk I/O.
- **`CatalogManager.java`**: A placeholder for a future system catalog that would manage metadata about tables, schemas, and indexes.

### 2.5. Console Layer (`src/main/java/database/`)

This is the user-facing entry point to the database.

- **`Main.java`**: Contains the `main` method. It initializes all core components and runs a REPL. Upon receiving the `exit` command, it calls `heapFile.flush()` to ensure any unwritten data in the `currentPage` buffer is persisted to disk before termination.

---

## 3. Setup and Execution

### Prerequisites
- Java Development Kit (JDK) 8 or higher.

### Build and Run Instructions

The project includes a `compile.sh` shell script that automates compilation and execution.

1.  **Navigate to the project root directory.**
2.  **Run the script:**
    ```bash
    ./compile.sh
    ```

This command compiles all `.java` files into the `out/` directory and then runs the `Main` class, starting the interactive console.

- **Compilation command**: `javac -d out src/main/java/**/*.java`
- **Execution command**: `java -cp out Main`

---

## 4. Supported SQL Commands

The custom SQL parser is case-insensitive for keywords but preserves the case of string literals. Table names are ignored but expected for syntax.

| Command           | Example Syntax                                | Description                                                                                                    |
| ----------------- | --------------------------------------------- | -------------------------------------------------------------------------------------------------------------- |
| **INSERT**        | `INSERT INTO users VALUES (1, 'Alice')`       | Inserts a new record. The entry is added to the B+ Tree index.                                                 |
| **SELECT (All)**  | `SELECT * FROM users`                         | Performs a full scan of the heap file and returns all non-deleted records.                                     |
| **SELECT (Filter)**| `SELECT * FROM users WHERE id=1`              | Uses the B+ Tree index to perform a fast lookup by ID.                                                         |
| **UPDATE**        | `UPDATE users SET name='Bob' WHERE id=1`      | Finds a record by ID using the index and overwrites its name on disk.                                          |
| **DELETE**        | `DELETE FROM users WHERE id=1`                | Performs a soft-delete by marking the record as deleted on disk and removes its entry from the B+ Tree index. |
| **EXIT**          | `exit`                                        | Terminates the session and flushes any pending data changes to disk.                                           |

---

## 5. How to Extend the Database

To add new features, follow the existing layered architecture.

**Example: Adding a new data type to records.**

1.  **`DBRecord.java`**: Modify the class to include the new field. Update `toBytes()` and `fromBytes()` for serialization.
2.  **`sqlParser.java`**: Extend `parseInsert()` to handle the new data type in the `INSERT` syntax.
3.  **`Executor.java`**: No changes should be needed if the `DBRecord` and `InsertQuery` objects correctly encapsulate the new data.
4.  **`Main.java`**: Update the `SELECT` printout to display the new field.
