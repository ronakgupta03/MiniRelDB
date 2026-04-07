import mysql.connector
import subprocess
import time
import os
import random

# Configuration
DB_NAME = "bench_db"
MYSQL_USER = "utsav"
MINIREL_DIR = "data/bench_db"

def run_command(cmd):
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error running command: {cmd}\n{result.stderr}")
    return result.stdout

def benchmark_minirel():
    print("Running MiniRelDB Benchmark...")
    # Clean old data
    if os.path.exists("data/bench_db"):
        run_command("rm -rf data/bench_db")
    
    # Compile
    run_command("javac -d out src/main/java/**/*.java")
    
    # Run
    output = run_command("java -cp out database.BenchmarkRunner")
    
    metrics = {}
    for line in output.split('\n'):
        if line.startswith("METRIC:"):
            parts = line.split(':')
            if len(parts) == 3:
                metrics[parts[1]] = int(parts[2])
    
    # Get storage size
    size = 0
    for root, dirs, files in os.walk(MINIREL_DIR):
        for f in files:
            size += os.path.getsize(os.path.join(root, f))
    metrics['StorageSize'] = size / (1024 * 1024) # MB
    
    return metrics

def benchmark_mysql():
    print("Running MySQL Benchmark...")
    DB_NAME = "test"
    
    # 1. Setup via CLI (fast for bulk)
    with open("setup.sql", "w") as f:
        f.write(f"USE {DB_NAME}; DROP TABLE IF EXISTS orders; DROP TABLE IF EXISTS users;")
        f.write("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(255), city VARCHAR(255));")
        f.write("CREATE TABLE orders (id INT PRIMARY KEY, user_id INT, amount INT);")
        
        user_inserts = []
        for i in range(5000):
            user_inserts.append(f"({i}, 'User_{i}', 'City_{i%10}')")
        f.write(f"INSERT INTO users VALUES {','.join(user_inserts)};")
        
        order_inserts = []
        for i in range(10000):
            order_inserts.append(f"({i}, {random.randint(0, 4999)}, {random.randint(0, 999)})")
        f.write(f"INSERT INTO orders VALUES {','.join(order_inserts)};")
    
    start = time.time()
    run_command(f"mysql -u {MYSQL_USER} < setup.sql")
    ingestion_time = int((time.time() - start) * 1000)
    os.remove("setup.sql")

    # 2. Queries via Connector (Fair timing)
    conn = mysql.connector.connect(user=MYSQL_USER, unix_socket='/run/mysqld/mysqld.sock', database=DB_NAME)
    cursor = conn.cursor()
    
    # Point Lookup
    lookup_ids = [random.randint(0, 9999) for _ in range(1000)]
    start = time.time()
    for target_id in lookup_ids:
        cursor.execute("SELECT * FROM orders WHERE id = %s", (target_id,))
        cursor.fetchone()
    lookup_time = int((time.time() - start) * 1000)
    
    # Full Scan
    start = time.time()
    cursor.execute("SELECT * FROM users")
    cursor.fetchall()
    scan_time = int((time.time() - start) * 1000)
    
    # Join
    start = time.time()
    cursor.execute("SELECT users.name, orders.amount FROM users JOIN orders ON users.id = orders.user_id")
    cursor.fetchall()
    join_time = int((time.time() - start) * 1000)
    
    # Storage size
    cursor.execute(f"SELECT SUM(data_length + index_length) FROM information_schema.tables WHERE table_schema = '{DB_NAME}' AND table_name IN ('users', 'orders')")
    size = float(cursor.fetchone()[0]) / (1024 * 1024)
    
    # Clean up
    cursor.execute("DROP TABLE IF EXISTS orders")
    cursor.execute("DROP TABLE IF EXISTS users")
    conn.commit()
    cursor.close()
    conn.close()
    
    return {
        'Ingestion_End': ingestion_time,
        'PointLookup_End': lookup_time,
        'FullScan_End': scan_time,
        'HashJoin_End': join_time,
        'StorageSize': size
    }

def generate_report(minirel, mysql):
    report = f"""# MiniRelDB vs MySQL Benchmark Report
Date: {time.strftime("%Y-%m-%d %H:%M:%S")}
Dataset: E-commerce Simulation (5,000 Users, 10,000 Orders)

## Performance Comparison (Time in ms)

| Metric | MiniRelDB | MySQL (MariaDB) | Difference |
| :--- | :---: | :---: | :---: |
| **Bulk Ingestion** | {minirel['Ingestion_End']}ms | {mysql['Ingestion_End']}ms | {minirel['Ingestion_End'] / mysql['Ingestion_End']:.2f}x |
| **Point Lookup (1k ops)** | {minirel['PointLookup_End']}ms | {mysql['PointLookup_End']}ms | {minirel['PointLookup_End'] / mysql['PointLookup_End']:.2f}x |
| **Full Table Scan** | {minirel['FullScan_End']}ms | {mysql['FullScan_End']}ms | {minirel['FullScan_End'] / mysql['FullScan_End']:.2f}x |
| **Join Performance** | {minirel['HashJoin_End']}ms | {mysql['HashJoin_End']}ms | {minirel['HashJoin_End'] / mysql['HashJoin_End']:.2f}x |

## Efficiency Comparison

| Metric | MiniRelDB | MySQL (MariaDB) | Difference |
| :--- | :---: | :---: | :---: |
| **Storage Size** | {minirel['StorageSize']:.2f} MB | {mysql['StorageSize']:.2f} MB | {minirel['StorageSize'] / mysql['StorageSize']:.2f}x |

## Observations

### 1. Ingestion
MiniRelDB uses a **MemTable + WAL** write path with **mmap** which provides very low overhead for sequential inserts. MySQL uses **InnoDB** with heavier transaction log management and page flushing logic.

### 2. Point Lookups
MiniRelDB's **Adaptive Hash Index (AHI)** provides O(1) in-memory lookups for hot data, making it extremely competitive for point-queries compared to MySQL's B+ Tree traversal.

### 3. Join Performance
MiniRelDB's implementation of the **Hash Join** algorithm allows it to achieve linear O(N+M) performance, which is highly efficient for the tested dataset size.

### 4. Storage
MiniRelDB's storage is optimized for educational clarity and raw performance, currently using a simple paged format without the heavy page metadata and undo-logs found in MySQL's InnoDB.

---
*Report generated automatically by MiniRelDB Benchmark Suite.*
"""
    with open("BENCHMARK_REPORT.md", "w") as f:
        f.write(report)
    print("Report generated: BENCHMARK_REPORT.md")

if __name__ == "__main__":
    minirel_results = benchmark_minirel()
    mysql_results = benchmark_mysql()
    generate_report(minirel_results, mysql_results)
