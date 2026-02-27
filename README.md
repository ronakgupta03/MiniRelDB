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

### Compile

```
javac -cp ".:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar" Main.java
```

### Run

```
java --enable-native-access=ALL-UNNAMED \
-cp ".:sqlite-jdbc.jar:slf4j-api.jar:slf4j-simple.jar" Main
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

**MiniRelDB — Understanding Databases by Building One.**
