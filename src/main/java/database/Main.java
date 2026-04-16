package database;

import java.util.*;
import query.*;
import storage.*;
import catalog.CatalogManager;

public class Main {
    private static final List<String> history = new ArrayList<>();

    public static void main(String[] args) {
        try {
            CatalogManager catalogManager = new CatalogManager();
            String currentDatabase = "main";
            if (catalogManager.getDatabases().isEmpty()) {
                catalogManager.createDatabase("main");
            }
            
            Executor executor = new Executor(currentDatabase, catalogManager);
            
            // Check if we have a TTY for rich features
            boolean isTty = System.console() != null;
            ConsoleReader reader = isTty ? new ConsoleReader(history) : null;
            Scanner sc = isTty ? null : new Scanner(System.in);
            
            StringBuilder buffer = new StringBuilder();
            
            System.out.println("MiniRelDB Console - Connected to '" + currentDatabase + "'");
            System.out.println("Commands must end with a semicolon (;). Type 'exit' to quit.");
            if (isTty) {
                System.out.println("TUI Mode Enabled: Use Up/Down for history, Left/Right to edit.");
            }
            
            while (true) {
                String prompt = (buffer.length() == 0) ? "[" + currentDatabase + "]> " : "      -> ";
                String line;
                
                if (isTty) {
                    line = reader.readLine(prompt);
                } else {
                    System.out.print(prompt);
                    if (!sc.hasNextLine()) break;
                    line = sc.nextLine();
                }
                
                if (line == null) break;
                String trimmed = line.trim();
                
                if (trimmed.equalsIgnoreCase("exit")) {
                    executor.flushAll();
                    System.out.println("Exiting...");
                    break;
                }
                if (trimmed.equalsIgnoreCase("clear")) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    continue;
                }
                if (trimmed.equalsIgnoreCase("history")) {
                    for (int i = 0; i < history.size(); i++) {
                        System.out.println((i + 1) + ": " + history.get(i));
                    }
                    continue;
                }

                buffer.append(line).append(" ");
                
                if (trimmed.endsWith(";")) {
                    String fullQuery = buffer.toString().trim();
                    buffer.setLength(0);
                    
                    if (fullQuery.startsWith("--") || fullQuery.isEmpty()) continue;
                    
                    if (history.isEmpty() || !history.get(history.size() - 1).equals(fullQuery)) {
                        history.add(fullQuery);
                    }
                    
                    try {
                        sqlParser parser = new sqlParser(fullQuery);
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
                            // executor.executeCreateIndex((CreateIndexQuery) parsedQuery);
                            System.out.println("Index created.");
                        } else if (parsedQuery instanceof ShowQuery) {
                            List<String> results = executor.executeShow((ShowQuery) parsedQuery);
                            System.out.println(((ShowQuery) parsedQuery).getType() + ":");
                            for (String s : results) System.out.println(" - " + s);
                        } else if (parsedQuery instanceof DropQuery) {
                            executor.executeDrop((DropQuery) parsedQuery);
                            System.out.println("Dropped " + ((DropQuery) parsedQuery).getType().toLowerCase());
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
                            List<Map<String, Object>> rowMaps = new ArrayList<>();
                            for (DBRecord r : results) {
                                rowMaps.add(r.getValues());
                            }
                            TableRenderer.render(rowMaps);
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
            }
            if (sc != null) sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
