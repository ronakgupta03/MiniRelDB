# MiniRelDB

# MiniRelDB — Java + SQLite Mini Database Console

## 📌 Overview

**MiniRelDB** is a lightweight relational database console built using **Java JDBC** and **SQLite**.
The goal of this project is to understand how real database systems internally work by implementing:

* Database connection handling
* Query execution
* Prepared statements
* Batch processing
* Transactions
* Interactive database console

Instead of directly executing SQL manually, this project builds a **custom command-line database interface**, similar to MySQL or SQLite terminals.

---

## 🎯 Project Goals

This project demonstrates:

✅ JDBC database connectivity
✅ Safe query execution
✅ Transaction management
✅ Batch inserts
✅ Command parsing
✅ Persistent storage

It is designed as a **learning-level DBMS architecture foundation**.

---

## ⚙️ Technologies Used

* **Java**
* **JDBC (Java Database Connectivity)**
* **SQLite**
* **PreparedStatement API**
* **GitHub Codespaces**

---

## 📂 Project Structure

```
MiniRelDB/
│
├── Main.java        → Database console implementation
├── .gitignore       → Ignore compiled/database files
└── README.md
```

---

## ▶️ How To Run

### Run

```
./compile.sh
```

---

## 🧠 Program Flow (High Level)

```
User Command
     ↓
Console Input
     ↓
Command Parser
     ↓
JDBC Execution
     ↓
SQLite Database
```

---

# 🔎 Code Explanation (Line-by-Line Concepts)

---

## 1️⃣ Loading JDBC Driver

```java
Class.forName("org.sqlite.JDBC");
```

### Why used?

This explicitly loads the SQLite JDBC driver into JVM memory.

### Benefit

* Registers driver with `DriverManager`
* Ensures compatibility across Java versions

Without this step, Java may fail to locate the database driver.

---

## 2️⃣ Database Connection

```java
Connection conn =
    DriverManager.getConnection("jdbc:sqlite:test.db");
```

### Purpose

Creates a connection between Java application and SQLite database.

### Why SQLite?

* Serverless database
* Automatically creates database file
* Perfect for embedded DB systems

### Benefit

No external database installation required.

---

## 3️⃣ Creating Table

```java
stmt.executeUpdate(
 "CREATE TABLE IF NOT EXISTS users (...)"
);
```

### Why `executeUpdate()`?

Used for SQL operations that modify database state:

* CREATE
* INSERT
* UPDATE
* DELETE

### Benefit over `executeQuery()`

`executeQuery()` only works for SELECT statements.

---

## 4️⃣ Why PreparedStatement Instead of Statement?

```java
PreparedStatement ps =
    conn.prepareStatement(
        "INSERT INTO users(name) VALUES(?)"
    );
```

### Problem with Statement

```java
"INSERT INTO users VALUES('" + name + "')"
```

❌ Vulnerable to SQL Injection
❌ Slow for repeated execution

---

### PreparedStatement Advantages

✅ Prevents SQL injection
✅ Precompiled SQL execution
✅ Faster repeated inserts
✅ Cleaner parameter binding

Database parses SQL **once**, then reuses it.

---

## 5️⃣ Parameter Binding

```java
ps.setString(1, name);
```

`?` placeholder gets replaced safely.

Index starts from **1**, not 0.

---

## 6️⃣ Batch Processing

```java
ps.addBatch();
ps.executeBatch();
```

### Why Batch?

Instead of:

```
Insert → Send to DB
Insert → Send to DB
Insert → Send to DB
```

Batch does:

```
Collect Queries
        ↓
Send Once
```

### Benefits

✅ Faster execution
✅ Reduced DB communication
✅ Used in production systems

---

## 7️⃣ Transaction Management

```java
conn.setAutoCommit(false);
conn.commit();
```

### Default Behavior

Every SQL statement commits automatically.

### Transaction Mode

Groups operations together.

```
All succeed → COMMIT
Any fail → ROLLBACK
```

### Benefit

Maintains database consistency.

Real databases follow **ACID properties**.

---

## 8️⃣ Interactive Console

```java
Scanner scanner = new Scanner(System.in);
```

Creates continuous user interaction.

Loop:

```java
while(true)
```

Simulates real DB shells:

```
mysql>
sqlite>
MiniRelDB>
```

---

## 9️⃣ Command Parsing

Example:

```
INSERT Ronak Bhavya
```

Parsed using:

```java
input.split(" ");
```

This converts user input into executable database actions.

---

## 🔟 SELECT Query Execution

```java
ResultSet rs =
    stmt.executeQuery("SELECT * FROM users");
```

### Why ResultSet?

Represents table output row-by-row.

Iteration:

```java
while(rs.next())
```

Moves cursor through rows sequentially.

---

## 1️⃣1️⃣ CLEAR Command

```java
DELETE FROM users
```

Chosen instead of:

```
DROP TABLE
```

### Reason

Keeps schema intact while removing data.

---

## 🖥 Supported Commands

| Command      | Description            |
| ------------ | ---------------------- |
| INSERT A B C | Insert multiple users  |
| SELECT       | Display table contents |
| CLEAR        | Remove all records     |
| EXIT         | Close console          |

---

## 🧱 Architecture Concepts Learned

MiniRelDB mimics real database layers:

```
Console Layer
Parser Layer
Execution Layer
Transaction Layer
Storage Layer
```

---

## ✅ Why This Design?

| Feature           | Reason                 |
| ----------------- | ---------------------- |
| PreparedStatement | Security + performance |
| Batch Execution   | Efficient inserts      |
| Transactions      | Data safety            |
| SQLite            | Embedded storage       |
| Console Loop      | DB shell simulation    |

---

## 🚀 Future Improvements

* Multiple table support
* WHERE conditions
* UPDATE / DELETE commands
* Query parser engine
* Metadata manager
* Custom SQL interpreter

---

## 🎓 Learning Outcome

After understanding this codebase, a reader should know:

* How Java communicates with databases
* How database consoles work
* Why prepared statements matter
* How transactions ensure safety
* How DBMS execution pipelines operate

---

## 📜 License

Educational / Learning Project



---

## 🚀 Why This Next Step Exists

Until now, the project relied on external databases like **SQLite** through JDBC.

That means:

```
Your program → SQLite → Disk Storage
```

SQLite was doing all the real database work internally.

However, the goal of this project is **not to use a database**, but to **understand how databases are built internally**.

So the next step is:

> ✅ Become the database engine instead of a database user.

---

## 🧠 Core Learning Objective

### Understand **Logical Storage vs Physical Storage**

Every real DBMS (like MySQL, PostgreSQL, Oracle) separates data into two layers:

---

## ✅ 1. Logical Layer (User View)

This is what users think happens.

Example commands:

```
CREATE TABLE users
INSERT INTO users VALUES Ronak
SELECT * FROM users
```

From the user’s perspective:

* Tables exist
* Rows are inserted
* Queries return results

Users never see **how data is stored**.

---

## ✅ 2. Physical Layer (Disk Reality)

Internally, databases store data as files.

Example:

```
data/users.csv
```

Actual stored data:

```
id,name
1,Ronak
2,Bhavya
```

Meaning:

| Logical Concept | Physical Representation |
| --------------- | ----------------------- |
| Table           | File                    |
| Row             | Line in file            |
| Column          | Comma-separated value   |
| Insert          | File append             |
| Select          | File read               |

This separation is the **foundation of DBMS design**.

---

## ❗ Why Remove SQLite?

SQLite already provides:

* Storage management
* File handling
* Query execution
* Data retrieval

If SQLite remains:

👉 You never learn how databases actually work.

Removing SQLite forces you to implement:

✅ Disk storage
✅ Record persistence
✅ Table mapping
✅ Data retrieval

This is how real database engines begin.

---

## 🗂️ New System Architecture

The project now moves toward a **mini relational database engine**.

Recommended structure:

```
MiniRelDB/
│
├── data/                 ← Physical storage
│
├── src/
│   ├── Main.java
│   ├── Table.java
│   ├── StorageManager.java
│   └── Record.java
```

---

## 📁 Why CSV Storage?

For Week-1, CSV is preferred over JSON.

| CSV                       | JSON                   |
| ------------------------- | ---------------------- |
| Very simple               | Structurally complex   |
| Row-based                 | Object-based           |
| Fast to implement         | Requires parsing logic |
| Matches relational tables | Better for nested data |

CSV directly represents relational rows.

---

## ⚙️ How the System Works

---

### ✅ CREATE TABLE

#### Logical Command

```
CREATE users
```

#### Engine Operation

* Create file inside `/data`
* Initialize table header

Example:

```
data/users.csv
```

Code concept:

```java
new File("data/users.csv").createNewFile();
```

---

### ✅ INSERT RECORD

#### Logical Command

```
INSERT users Ronak
```

#### Engine Operation

* Append a new line to file

Example result:

```
1,Ronak
2,Bhavya
```

Why append?

✅ Faster
✅ Mimics real DB write operations
✅ Avoids rewriting entire file

Implementation idea:

```java
FileWriter(file, true);
```

---

### ✅ DISPLAY RECORDS (SELECT)

#### Logical Command

```
SELECT users
```

#### Engine Operation

* Read CSV file line-by-line
* Display records

Implementation idea:

```java
BufferedReader
```

Why line-by-line reading?

✅ Memory efficient
✅ Works for large datasets
✅ Similar to database scanning

---

## 🧩 Internal Flow

```
User Command
      ↓
Command Parser
      ↓
Storage Manager
      ↓
CSV File (Disk)
```

You are now implementing:

> **Storage Layer of a Database**

---

## 🎯 Purpose of This Step

This phase teaches:

✅ How tables map to files
✅ How records persist after program shutdown
✅ How databases store rows physically
✅ Difference between abstraction and implementation

Without this step, advanced DBMS topics cannot be understood.

---


```

