package database;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import query.*;
import storage.*;
import catalog.CatalogManager;

public class Main {
    public static void main(String[] args) {
        try {
            CatalogManager catalogManager = new CatalogManager();
            String currentDatabase = "main";
            if (catalogManager.getDatabases().isEmpty()) {
                catalogManager.createDatabase("main");
            }
            
            Executor executor = new Executor(currentDatabase, catalogManager);
            Scanner sc = new Scanner(System.in);
            
            System.out.println("MiniRelDB Console - Connected to '" + currentDatabase + "'");
            while (true) {
                System.out.print("[" + currentDatabase + "]> ");
                String queryStr = sc.nextLine().trim();
                if (queryStr.equalsIgnoreCase("exit")) {
                    executor.flushAll();
                    System.out.println("Exiting...");
                    break;
                }
                if (queryStr.equalsIgnoreCase("clear")) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    continue;
                }
                if (queryStr.isEmpty()) continue;

                try {
                    sqlParser parser = new sqlParser(queryStr);
                    Object parsedQuery = parser.parse();

                    if (parsedQuery instanceof UseDatabaseQuery) {
                        String newDb = ((UseDatabaseQuery) parsedQuery).getDbName();
                        if (catalogManager.getDatabaseSchema(newDb) == null) {
                            System.out.println("Error: Database '" + newDb + "' does not exist.");
                        } else {
                            executor.flushAll();
                            currentDatabase = newDb;
                            executor = new Executor(currentDatabase, catalogManager);
                            System.out.println("Switched to database '" + currentDatabase + "'");
                        }
                    } else if (parsedQuery instanceof CreateDatabaseQuery) {
                        catalogManager.createDatabase(((CreateDatabaseQuery) parsedQuery).getDbName());
                        System.out.println("Database created.");
                    } else if (parsedQuery instanceof CreateTableQuery) {
                        executor.executeCreateTable((CreateTableQuery) parsedQuery);
                        System.out.println("Table created.");
                    } else if (parsedQuery instanceof AlterTableQuery) {
                        executor.executeAlterTable((AlterTableQuery) parsedQuery);
                        System.out.println("Table altered.");
                    } else if (parsedQuery instanceof CreateIndexQuery) {
                        executor.executeCreateIndex((CreateIndexQuery) parsedQuery);
                        System.out.println("Index created.");
                    } else if (parsedQuery instanceof ShowQuery) {
                        List<String> results = executor.executeShow((ShowQuery) parsedQuery);
                        System.out.println(((ShowQuery) parsedQuery).getType() + ":");
                        for (String s : results) System.out.println(" - " + s);
                    } else if (parsedQuery instanceof DropQuery) {
                        try {
                            executor.executeDrop((DropQuery) parsedQuery);
                            System.out.println("Dropped " + ((DropQuery) parsedQuery).getType().toLowerCase());
                        } catch (Exception e) {
                            System.out.println("Drop skipped (object might not exist).");
                        }
                    } else if (parsedQuery instanceof InsertQuery) {
                        executor.executeInsert((InsertQuery) parsedQuery);
                        System.out.println("Insert executed.");
                    } else if (parsedQuery instanceof MultiInsertQuery) {
                        MultiInsertQuery mq = (MultiInsertQuery) parsedQuery;
                        for (Map<String, Object> row : mq.getRows()) {
                            executor.executeInsert(new InsertQuery(mq.getTableName(), row));
                        }
                        System.out.println("Bulk insert executed (" + mq.getRows().size() + " rows).");
                    } else if (parsedQuery instanceof SelectQuery) {
                        List<DBRecord> results = executor.executeSelect((SelectQuery) parsedQuery);
                        System.out.println("Select results:");
                        for (DBRecord r : results) {
                            System.out.print(" - ");
                            Map<String, Object> vals = r.getValues();
                            for (Map.Entry<String, Object> entry : vals.entrySet()) {
                                System.out.print(entry.getKey() + ": " + entry.getValue() + " | ");
                            }
                            System.out.println();
                        }
                    } else if (parsedQuery instanceof UpdateQuery) {
                        executor.executeUpdate((UpdateQuery) parsedQuery);
                        System.out.println("Update executed.");
                    } else if (parsedQuery instanceof DeleteQuery) {
                        executor.executeDelete((DeleteQuery) parsedQuery);
                        System.out.println("Delete executed.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
