# MiniRelDB — From-Scratch Relational Database Engine

## Overview

MiniRelDB is a **custom relational database engine** implemented entirely in Java. Instead of using an existing database like SQLite or MySQL, this project implements the low-level storage, parsing, and execution layers directly — mimicking how production database engines like PostgreSQL and SQLite actually work.

The project evolved through three distinct phases:

1. **Phase 1** — Console over SQLite via JDBC (learned query execution and transactions)
2. **Phase 2** — Removed SQLite, moved to file-based storage (learned logical vs. physical storage)
3. **Phase 3** — Binary page-based storage engine with fixed-size 4096-byte pages, HeapFile management, multi-page persistence, and an interactive REPL console

The current codebase represents Phase 3 — a working database engine that stores records in binary pages on disk.

---

## Architecture Diagram

```
User Input (REPL Console)
    │
    ▼
┌─────────────────────────────────┐
│            Main.java            │  REPL loop: reads input, handles EXIT/QUIT/EOF
│   (database/Main.java)          │  Coordinates DiskManager → HeapFile → Executor
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│        CommandParser.java       │  Converts raw text → structured query objects
│     (query/CommandParser.java)  │  e.g. "INSERT 1 Alice" → InsertQuery(1, "Alice")
└──────────────┬──────────────────┘
               │
    ┌──────────┴──────────┐
    ▼                      ▼
┌──────────────┐    ┌──────────────┐
│ InsertQuery  │    │ SelectQuery  │  Data objects representing parsed commands
│ (query/)     │    │ (query/)     │
└──────┬───────┘    └──────┬───────┘
       │                   │
       ▼                   ▼
┌─────────────────────────────────┐
│          Executor.java          │  Dispatches queries to storage layer
│        (query/Executor.java)    │  Routes INSERT → HeapFile, SELECT → page scan
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│          HeapFile.java          │  Manages a sequence of pages for a "table"
│       (storage/HeapFile.java)   │  Auto-spills to new page when current fills up
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│            Page.java            │  Fixed 4096-byte block with 4-byte header
│         (storage/Page.java)     │  Sequential writes, offset-based reads
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│         DiskManager.java        │  RandomAccessFile wrapper
│      (storage/DiskManager.java) │  Reads/writes pages by numeric ID
└──────────────┬──────────────────┘
               │
               ▼
    ┌──────────────────────┐
    │  data/database.db    │  Single binary file of concatenated pages
    └──────────────────────┘
```

---

## Layer-by-Layer Deep Dive

### 1. REPL Entry Point (`src/main/java/database/Main.java`)

**Lines:** 11 lines of active code (87 lines of commented-out test code)

**Purpose:** The console application loop. This is the entry point where the database starts.

**How it works:**
1. Creates a `DiskManager` pointed at `data/database.db`
2. Creates a `HeapFile` (which loads or creates pages)
3. Creates an `Executor` to handle queries
4. Enters a `while(true)` loop, reading user input via `Scanner.nextLine()`
5. Parses input with `CommandParser.parse()` → gets a query object
6. Passes the query to `Executor.execute()`
7. On `EXIT`/`QUIT`/EOF, calls `heapFile.flush()` to persist the current page

**Why two catch blocks for `nextLine()`?** The inner `try/catch` catches `NoSuchElementException` from `scanner.nextLine()`, which happens when input is piped (e.g., `echo "SELECT" | java Main`). The outer `catch` handles all other exceptions like IO errors from the `DiskManager`.

**Current Status:** Functional for INSERT and SELECT-all commands. No table name awareness — all data goes to a single HeapFile.

---

### 2. Query Parser (`src/main/java/query/CommandParser.java`)

**Lines:** 29

**Purpose:** Transforms raw string input into typed query objects.

**How it works:**
- Takes a string like `"INSERT 1 Alice"`, splits by whitespace
- `parts[0]` determines the command type
- For `INSERT`: validates at least 3 parts, parses `parts[1]` as `int` (the ID), uses `parts[2]` as the name string
- For `SELECT`: returns an empty `SelectQuery` object as a marker
- Throws `IllegalArgumentException` for unrecognized commands

**Design Choice — returning `Object` instead of a typed enum:** The parser returns a plain `Object`, and the `Executor` uses `instanceof` to dispatch. This mirrors how real query parsers produce an Abstract Syntax Tree (AST), which the execution engine then pattern-matches against. In production databases like PostgreSQL, the parser generates a full tree of nodes; here we use simple data objects as the primitive equivalent.

---

### 3. Insert Query Data Object (`src/main/java/query/InsertQuery.java`)

**Lines:** 19

**Purpose:** Simple immutable data container holding the parsed INSERT values.

**Fields:**
- `int id` — the record's numeric identifier
- `String name` — the record's name string

This class has no logic other than construction and getters. It exists purely to carry parsed data from the parser to the executor, preventing the executor from needing to re-parse the raw input.

---

### 4. Select Query Marker (`src/main/java/query/SelectQuery.java`)

**Lines:** 5

**Purpose:** Marker class to distinguish a SELECT command from an INSERT command.

**Current Status:** Empty shell. No fields for projections, filters, or table names. All SELECT logic currently lives in `Executor.execute()` — it scans all pages, collects records, sorts, and prints directly.

---

### 5. Executor (`src/main/java/query/Executor.java`)

**Lines:** 66

**Purpose:** The command dispatcher and SELECT implementation. Converts query objects into database operations.

**INSERT path (lines 18-25):**
1. Casts the query to `InsertQuery`
2. Creates a `DBRecord(id, name)`
3. Delegates to `heapFile.insertRecord(record)`

**SELECT path (lines 28-63):**
1. Gets the total page count from `DiskManager`
2. Gets the current page ID from `HeapFile` (this page is still in memory, not yet flushed)
3. Iterates through all page IDs, reads each from disk **except** the current page
4. Merges the current page's records (from memory) with the disk records
5. Sorts all records by ID ascending
6. Prints a formatted ASCII table with column headers
7. Shows the total record count

**Design Decision — why skip the current page during the disk scan?** The `HeapFile` keeps one page in memory as the "current page" until it fills up or the program exits. Flushing it to disk happens only on EXIT or page overflow. So during SELECT, the newest records exist only in memory. The executor avoids reading stale data by skipping that page ID when scanning disk.

---

### 6. DBRecord (`src/main/java/storage/DBRecord.java`)

**Lines:** 76 (excluding comments showing the wire format)

**Purpose:** In-memory representation of a database row, with binary serialization.

**Wire Format (how a record looks as bytes on disk):**
```
[id: 4 bytes] [name_length: 4 bytes] [name_bytes: variable]
Example: 1 "Alice" → [00 00 00 01] [00 00 00 05] [41 6C 69 63 65]
```

**Serialization (`toBytes()`, lines 17-33):**
1. Converts the name string to UTF-8 bytes
2. Allocates a `ByteBuffer` of size `4 + 4 + name.length`
3. Writes id (4 bytes), name length (4 bytes), name bytes (variable)
4. Returns the underlying byte array

**Size calculation (`size()`, line 43-45):** Returns the exact byte size of the serialized record. Used by `Page.insertRecord()` to check if a record fits in available space.

**Deserialization (`fromBytes()`, lines 48-65):** This method is commented out. It exists but isn't used — instead, `Page.getAllRecords()` performs the deserialization manually by walking the byte stream. Both approaches work; the commented version was likely an early attempt that got replaced.

---

### 7. Page (`src/main/java/storage/Page.java`)

**Lines:** 104

**Purpose:** Represents a single fixed-size 4096-byte database page. This is the fundamental unit of storage — every read and write operation works in pages, not individual records.

**Page Layout — how bytes are organized:**
```
Byte 0-3:     freeSpaceOffset (how many bytes of record data are stored)
Byte 4-N:     record1, record2, record3, ... (sequential packed records)
Byte N-4095:  unused free space
```

**Header (`freeSpaceOffset`):** The first 4 bytes store how many bytes of record data have been written. This acts as both a "how full is this page" indicator and a cursor for where the next record should be appended.

**Writing records (`insertRecord()`, lines 50-68):`**
1. Checks if `freeSpaceOffset + record.length + 4 > 4096` (leaves room for the header)
2. If it fits, copies record bytes into position `4 + freeSpaceOffset` in the byte array
3. Updates `freeSpaceOffset` by adding the record's byte length
4. Writes the new `freeSpaceOffset` back to the first 4 bytes of the page
5. Returns `true` on success, `false` if the page is too full

**Reading records (`getAllRecords()`, lines 70-103):**
1. Starts at offset 4 (skipping the header)
2. Loops while there are at least 8 bytes remaining (minimum record: 4 for id + 4 for length)
3. Reads int id (4 bytes), int nameLength (4 bytes)
4. Safety check: rejects if `nameLength <= 0` or if the name would extend past the data boundary
5. Reads name bytes and constructs a `DBRecord`
6. Advances the offset past the name and continues

**Why manual deserialization instead of `DBRecord.fromBytes()`?** Because records are sequential in the page, we can walk forward with offsets, extracting one record at a time without knowing boundaries ahead of time. This is more efficient than slicing the page into individual byte arrays just to deserialize them.

`setData()` bound check (lines 28-33, commented out):** An earlier version validated that incoming data was exactly `PAGE_SIZE` bytes. It was commented out because `DiskManager.readPage()` already guarantees reading exactly 4096 bytes.

---

### 8. DiskManager (`src/main/java/storage/DiskManager.java`)

**Lines:** 60

**Purpose:** Low-level binary file I/O. Wraps a `RandomAccessFile` to read and write 4096-byte pages at known byte offsets.

**File operations:**
- `__writePage(Page)`: Calculates byte offset as `pageId * 4096`, seeks there, writes the page's full 4096-byte array
- `readPage(pageId)`: Validates the page ID is within file bounds, seeks to the offset, reads exactly 4096 bytes into a new Page
- `getTotalPages()`: Divides file size by `Page.PAGE_SIZE` (4096) to determine how many pages exist

**Boundary safety:** If the page_id is past the end of the file or EOF is reached, `readPage()` returns an empty `Page` instead of throwing an exception. This handles the case where the HeapFile expects a page to exist but the file has been truncated or is fresh.

**Why `RandomAccessFile`?** It provides `seek()` for positioning at exact byte offsets, which is necessary because page N is at offset N * 4096. With sequential file APIs like `BufferedReader`, you cannot jump directly to a random position in the file.

---

### 9. HeapFile (`src/main/java/storage/HeapFile.java`)

**Lines:** 59

**Purpose:** Manages a collection of pages as a single heap-organized storage structure. This is the "table" abstraction — it orchestrates when to write a full page to disk, create a new page, and insert records.

**Constructor logic:**
1. Checks `diskManager.getTotalPages()`
2. If 0 (fresh database), creates `Page(0)` as an empty page, sets `nextPageId = 0`
3. If > 0 (existing database), reads the last page from disk, sets `nextPageId = totalPages - 1`

**Insert logic (`insertRecord()`, lines 29-49):**
1. Serializes the record to bytes via `DBRecord.toBytes()`
2. Tries to insert into `currentPage` via `Page.insertRecord()`
3. If the page is too full:
   - Writes the full page to disk via `diskManager.writePage()`
   - Creates a new page with `nextPageId + 1`
   - Inserts the record into the fresh page
4. Always prints the target page ID for debugging

**Flush (`flush()`, lines 51-54):** Writes the current in-memory page to disk. Called on EXIT/QUIT to persist the most recent records that haven't triggered a page overflow.

**Design Decision — only tracking the last page:** The `HeapFile` only knows about the "current" page, not the complete page list. This works for sequential inserts but means SELECT must scan the entire file to read older pages from disk. A buffer pool manager would later improve this by caching frequently-accessed pages.

---

### 10. Stub/Placeholder Classes

These classes exist as empty shells for future phases:

| File | Purpose | Status |
|------|---------|--------|
| `database/Database.java` | High-level database facade | Stub — only prints "MiniRelDB started" |
| `buffer/BufferManager.java` | Buffer pool / LRU cache | Empty file |
| `catalog/CatalogManager.java` | Table metadata management | Empty file |
| `index/BPlusTree.java` | Index implementation | Empty file |

---

## On-Disk Binary Format

### File Level
`data/database.db` is a single binary file of concatenated 4096-byte pages. Page 0 starts at byte 0, Page 1 at byte 4096, Page 2 at byte 8192, etc.

### Page Layout (4096 bytes)
```
┌────────────────────┬────────────────────────────────────────┬─────────────────┐
│ freeSpaceOffset    │ record1 | record2 | record3 | ...     │    Free Space   │
│ 4 bytes (int)      │ variable-length packed                │    padded zeros │
└────────────────────┴────────────────────────────────────────┴─────────────────┘
Byte 0───────────────┘                                                           4095
```

### Record Wire Format
```
┌─────────────┬──────────────────┬───────────────────────┐
│ id (4 bytes)│ name_length (4B) │ name_bytes (variable) │
└─────────────┴──────────────────┴───────────────────────┘

Example:  id=1, name="Alice"
[00 00 00 01] [00 00 00 05] [41 6C 69 63 65]
```

---

## Current Capabilities

| Feature | Status | Notes |
|---------|--------|-------|
| INSERT with id + name | Working | Parses `INSERT <id> <name>` |
| SELECT all records | Working | Scans all pages, merges with memory, sorts by ID |
| Multi-page storage | Working | Auto-creates new pages at 4096-byte boundary |
| Persistence to disk | Working | Flushes on EXIT/QUIT and page overflow |
| EOF handling | Working | Catches `NoSuchElementException` for piped input |
| Boundary checks | Working | Validates page offsets, record sizes, name lengths |
| Formatted table output | Working | ASCII columns with column headers and row count |
| REPL with EXIT/QUIT | Working | Two synonyms for graceful shutdown |

## Not Yet Implemented

| Feature | Planned In |
|---------|-----------|
| Table names, multiple tables | CatalogManager |
| CREATE TABLE / dynamic schemas | CatalogManager |
| WHERE clause filtering | Executor + SelectQuery |
| Column projections (SELECT col1, col2) | SelectQuery |
| UPDATE / DELETE commands | Executor |
| Buffer pool with LRU caching | BufferManager |
| B+ Tree indexing | BPlusTree |
| Transaction support (ACID) | Future phase |
| Deserialization in `DBRecord.fromBytes()` | DBRecord |

---

## How to Build and Run

```
# Compile
javac -d out -cp ".:*" src/main/java/**/*.java src/main/java/**/**/*.java

# Or compile individual files:
javac -d out -cp ".:*" src/main/java/storage/Page.java
javac -d out -cp ".:*" src/main/java/storage/DBRecord.java
javac -d out -cp ".:*" src/main/java/storage/DiskManager.java
javac -d out -cp ".:*" src/main/java/storage/HeapFile.java
javac -d out -cp ".:*" src/main/java/query/InsertQuery.java
javac -d out -cp ".:*" src/main/java/query/SelectQuery.java
javac -d out -cp ".:*" src/main/java/query/CommandParser.java
javac -d out -cp ".:*" src/main/java/query/Executor.java
javac -d out -cp ".:*" src/main/java/database/Main.java

# Run
java -cp out database.Main
```

```
# Interactive Console Demo
MiniRelDB started. Type commands (EXIT to quit)
> INSERT 1 Alice
Inserted record into page 0
> INSERT 2 Bob
Inserted record into page 0
> SELECT
| ID     | NAME                 |
|--------|----------------------|
| 1      | Alice                |
| 2      | Bob                  |
(2 record(s))
> EXIT
Database saved. Exiting...
```

---

"`★ Insight ─────────────────────────────────────`
- **Pages are the unit of transfer, not records:** Every database — from SQLite to PostgreSQL — reads and writes in fixed-size blocks called pages. Your `Page` class at 4096 bytes matches exactly what production databases use (PostgreSQL defaults to 8KB pages, SQLite to 4KB). The key realization is that a database never reads "one record from disk" — it always reads an entire page, even if it only needs one row inside it.
- **The wire format is type-aware:** Notice how the record serialization doesn't just dump the Java object — it encodes the integer as 4 big-endian bytes and prefixs the string with its length. This is exactly how real databases serialize tuples. The length prefix is critical: without it, there would be no way to know where one record ends and the next begins inside a page.
- **Heap file organization is the simplest viable storage:** Records are appended sequentially and never reordered or deleted. Production databases use this same strategy — they just add free space maps, slot arrays, and page directories on top. The `freeSpaceOffset` at byte 0 is a primitive version of a free space map.
`─────────────────────────────────────────────────`"
