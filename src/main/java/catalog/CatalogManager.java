package catalog;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CatalogManager {
    
    public static class ColumnSchema {
        public String name;
        public String type;
        public ColumnSchema(String name, String type) { this.name = name; this.type = type; }
    }

    public static class TableSchema {
        public String name;
        public List<ColumnSchema> columns = new ArrayList<>();
        public String primaryKey;
        public List<String> uniqueColumns = new ArrayList<>();
        public List<ForeignKeySchema> foreignKeys = new ArrayList<>();
        public TableSchema(String name) { this.name = name; }
    }

    public static class ForeignKeySchema {
        public String column, refTable, refColumn;
        public ForeignKeySchema(String col, String rt, String rc) {
            this.column = col; this.refTable = rt; this.refColumn = rc;
        }
    }

    public static class DatabaseSchema {
        public String name;
        public Map<String, TableSchema> tables = new HashMap<>();
        public DatabaseSchema(String name) { this.name = name; }
    }

    private Map<String, DatabaseSchema> databases = new HashMap<>();
    private String catalogPath = "data/catalog.json";
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public CatalogManager() {
        loadCatalog();
    }

    public void createDatabase(String name) {
        rwLock.writeLock().lock();
        try {
            if (!databases.containsKey(name)) {
                databases.put(name, new DatabaseSchema(name));
                saveCatalog();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void dropDatabase(String name) {
        rwLock.writeLock().lock();
        try {
            databases.remove(name);
            saveCatalog();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void createTable(String dbName, TableSchema table) {
        rwLock.writeLock().lock();
        try {
            if (!databases.containsKey(dbName)) {
                databases.put(dbName, new DatabaseSchema(dbName));
            }
            databases.get(dbName).tables.put(table.name, table);
            saveCatalog();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void dropTable(String dbName, String tableName) {
        rwLock.writeLock().lock();
        try {
            DatabaseSchema db = databases.get(dbName);
            if (db != null) {
                db.tables.remove(tableName);
                saveCatalog();
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public TableSchema getTableSchema(String dbName, String tableName) {
        rwLock.readLock().lock();
        try {
            DatabaseSchema db = databases.get(dbName);
            return (db == null) ? null : db.tables.get(tableName);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<String> getDatabases() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(databases.keySet());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<String> getTables(String dbName) {
        rwLock.readLock().lock();
        try {
            DatabaseSchema db = databases.get(dbName);
            if (db == null) return Collections.emptyList();
            return new ArrayList<>(db.tables.keySet());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public DatabaseSchema getDatabaseSchema(String name) {
        rwLock.readLock().lock();
        try {
            return databases.get(name);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void saveCatalog() {
        rwLock.writeLock().lock();
        try {
            try (PrintWriter out = new PrintWriter(new FileWriter(catalogPath))) {
                for (DatabaseSchema db : databases.values()) {
                    for (TableSchema table : db.tables.values()) {
                        out.println("TABLE|" + db.name + "|" + table.name + "|" + (table.primaryKey == null ? "id" : table.primaryKey));
                        for (ColumnSchema col : table.columns) {
                            out.println("COL|" + db.name + "|" + table.name + "|" + col.name + "|" + col.type);
                        }
                        for (String uniqueCol : table.uniqueColumns) {
                            out.println("UNIQUE|" + db.name + "|" + table.name + "|" + uniqueCol);
                        }
                        for (ForeignKeySchema fk : table.foreignKeys) {
                            out.println("FK|" + db.name + "|" + table.name + "|" + fk.column + "|" + fk.refTable + "|" + fk.refColumn);
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void loadCatalog() {
        rwLock.writeLock().lock();
        try {
            File file = new File(catalogPath);
            if (!file.exists()) return;
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] p = line.split("\\|");
                    if (p.length < 4 && !p[0].equals("FK")) continue;
                    String dbName = p[1];
                    String tblName = p[2];

                    if (p[0].equals("TABLE")) {
                        if (!databases.containsKey(dbName)) databases.put(dbName, new DatabaseSchema(dbName));
                        TableSchema ts = new TableSchema(tblName);
                        ts.primaryKey = p[3];
                        databases.get(dbName).tables.put(tblName, ts);
                    } else if (p[0].equals("COL")) {
                        TableSchema ts = getTableSchema(dbName, tblName);
                        if (ts != null) ts.columns.add(new ColumnSchema(p[3], p[4]));
                    } else if (p[0].equals("UNIQUE")) {
                        TableSchema ts = getTableSchema(dbName, tblName);
                        if (ts != null) ts.uniqueColumns.add(p[3]);
                    } else if (p[0].equals("FK")) {
                        TableSchema ts = getTableSchema(dbName, tblName);
                        if (ts != null) ts.foreignKeys.add(new ForeignKeySchema(p[3], p[4], p[5]));
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
