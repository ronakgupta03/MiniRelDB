package query;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import catalog.CatalogManager.ColumnSchema;

public class sqlParser {
    
    private String query;
    
    public sqlParser(String query) {
        this.query = query.trim();
    }

    public Object parse() {
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.startsWith("insert")) {
            return parseInsert();
        } else if (lowerQuery.startsWith("select")) {
            return parseSelect();
        } else if (lowerQuery.startsWith("update")) {
            return parseUpdate();
        } else if (lowerQuery.startsWith("delete")) {
            return parseDelete();
        } else if (lowerQuery.startsWith("create database")) {
            return new CreateDatabaseQuery(query.substring(15).trim().replace(";", ""));
        } else if (lowerQuery.startsWith("use ")) {
            return new UseDatabaseQuery(query.substring(4).trim().replace(";", ""));
        } else if (lowerQuery.startsWith("create table")) {
            return parseCreateTable();
        } else if (lowerQuery.startsWith("alter table")) {
            return parseAlterTable();
        } else if (lowerQuery.startsWith("create index")) {
            return parseCreateIndex();
        } else if (lowerQuery.startsWith("show ")) {
            String type = lowerQuery.substring(5).trim().replace(";", "");
            if (type.equals("database") || type.equals("databases")) {
                return new ShowQuery("DATABASES");
            }
            if (type.equals("table") || type.equals("tables")) {
                return new ShowQuery("TABLES");
            }
            throw new IllegalArgumentException("Unknown SHOW type: " + type);
        } else if (lowerQuery.startsWith("drop ")) {
            return parseDrop();
        } else {
            throw new IllegalArgumentException("Unsupported query type: " + query);
        }
    }

    // Helper to split by comma while respecting parentheses and quotes
    private List<String> smartSplit(String input, char delimiter) {
        List<String> results = new ArrayList<>();
        int depth = 0;
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\'' || c == '\"') inQuotes = !inQuotes;
            if (!inQuotes) {
                if (c == '(') depth++;
                if (c == ')') depth--;
            }
            if (c == delimiter && depth == 0 && !inQuotes) {
                results.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) results.add(current.toString().trim());
        return results;
    }

    private Object parseDrop() {
        String lower = query.toLowerCase();
        String type = lower.startsWith("drop database") ? "DATABASE" : "TABLE";
        String name = query.substring(lower.indexOf(type.toLowerCase()) + type.length()).trim()
                           .replace("if exists", "")
                           .replace(";", "").trim();
        return new DropQuery(type, name);
    }

    private AlterTableQuery parseAlterTable() {
        try {
            String lower = query.toLowerCase();
            String[] parts = query.split("\\s+");
            String tableName = parts[2];
            String op = parts[3].toUpperCase();
            String colName = "";
            String colType = "";

            if (op.equals("ADD")) {
                colName = parts[4];
                colType = parts[5].replace(";", "");
            } else if (op.equals("DROP")) {
                // ALTER TABLE x DROP COLUMN col
                colName = parts[parts.length - 1].replace(";", "");
            } else if (op.equals("MODIFY")) {
                colName = parts[parts.length - 2];
                colType = parts[parts.length - 1].replace(";", "");
            }
            
            return new AlterTableQuery(tableName, op, colName, colType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing ALTER TABLE: " + e.getMessage());
        }
    }

    private CreateIndexQuery parseCreateIndex() {
        try {
            String lower = query.toLowerCase();
            int onIdx = lower.indexOf("on");
            int openParen = query.indexOf("(");
            int closeParen = query.lastIndexOf(")");
            String tableName = query.substring(onIdx + 2, openParen).trim();
            String colName = query.substring(openParen + 1, closeParen).trim();
            return new CreateIndexQuery(tableName, colName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing CREATE INDEX: " + e.getMessage());
        }
    }

    private CreateTableQuery parseCreateTable() {
        try {
            int openParen = query.indexOf("(");
            int closeParen = query.lastIndexOf(")");
            String tableName = query.substring(12, openParen).trim();
            String colsPart = query.substring(openParen + 1, closeParen).trim();
            
            List<String> colDefs = smartSplit(colsPart, ',');
            List<ColumnSchema> columns = new ArrayList<>();
            String pk = "id";
            List<String> uniques = new ArrayList<>();
            List<catalog.CatalogManager.ForeignKeySchema> fks = new ArrayList<>();

            for (String col : colDefs) {
                String lower = col.toLowerCase();
                if (lower.startsWith("primary key")) {
                    int p1 = col.indexOf("(");
                    int p2 = col.indexOf(")");
                    if (p1 != -1 && p2 != -1) pk = col.substring(p1 + 1, p2).trim();
                    continue;
                }
                if (lower.startsWith("unique")) {
                    int p1 = col.indexOf("(");
                    int p2 = col.indexOf(")");
                    if (p1 != -1 && p2 != -1) uniques.add(col.substring(p1 + 1, p2).trim());
                    continue;
                }
                if (lower.startsWith("foreign key")) {
                    // FOREIGN KEY(essn) references employee(ssn)
                    int p1 = col.indexOf("(");
                    int p2 = col.indexOf(")");
                    String localCol = col.substring(p1 + 1, p2).trim();
                    int refIdx = lower.indexOf("references");
                    int p3 = col.indexOf("(", refIdx);
                    int p4 = col.indexOf(")", p3);
                    String refTable = col.substring(refIdx + 10, p3).trim();
                    String refCol = col.substring(p3 + 1, p4).trim();
                    fks.add(new catalog.CatalogManager.ForeignKeySchema(localCol, refTable, refCol));
                    continue;
                }
                
                String[] p = col.trim().split("\\s+");
                if (p.length >= 2) {
                    columns.add(new ColumnSchema(p[0], p[1]));
                    if (lower.contains("primary key") && !lower.startsWith("primary key")) pk = p[0];
                    if (lower.contains("unique") && !lower.startsWith("unique")) uniques.add(p[0]);
                }
            }
            CreateTableQuery q = new CreateTableQuery(tableName, columns, pk);
            for (String u : uniques) q.addUniqueColumn(u);
            for (catalog.CatalogManager.ForeignKeySchema fk : fks) q.addForeignKey(fk);
            return q;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing CREATE TABLE: " + e.getMessage());
        }
    }
    
    private Object parseInsert() {
        try {
            String lower = query.toLowerCase();
            int valuesIdx = lower.indexOf("values");
            String tablePart = query.substring(12, valuesIdx).trim();
            String tableName = tablePart;
            
            if (tablePart.contains("(")) {
                tableName = tablePart.substring(0, tablePart.indexOf("(")).trim();
            }

            int firstParen = query.indexOf("(", valuesIdx);
            int lastParen = query.lastIndexOf(")");
            String valPart = query.substring(firstParen + 1, lastParen).trim();
            
            // Handle multi-row: split by "), (" or "),\n("
            // Using a simple split with regex for whitespace flexibility
            String[] rows = valPart.split("\\)\\s*,\\s*\\(");
            List<Map<String, Object>> allRows = new ArrayList<>();
            for (String row : rows) {
                // Remove any leading '(' or trailing ')' that might remain from the split
                String cleanRow = row.trim();
                if (cleanRow.startsWith("(")) cleanRow = cleanRow.substring(1);
                if (cleanRow.endsWith(")")) cleanRow = cleanRow.substring(0, cleanRow.length() - 1);
                
                List<String> rowVals = smartSplit(cleanRow, ',');
                Map<String, Object> map = new LinkedHashMap<>();
                for (int i = 0; i < rowVals.size(); i++) {
                    String v = rowVals.get(i).trim();
                    if ((v.startsWith("'") && v.endsWith("'")) || (v.startsWith("\"") && v.endsWith("\""))) {
                        map.put("col" + i, v.substring(1, v.length() - 1));
                    } else {
                        try {
                            map.put("col" + i, Integer.parseInt(v));
                        } catch (NumberFormatException e) {
                            map.put("col" + i, v);
                        }
                    }
                }
                allRows.add(map);
            }
            return new MultiInsertQuery(tableName, allRows);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing INSERT: " + e.getMessage());
        }
    }
    
    private SelectQuery parseSelect() {
        try {
            String lower = query.toLowerCase();
            int fromIdx = lower.indexOf("from");
            String colPart = query.substring(7, fromIdx).trim();
            SelectQuery sq = new SelectQuery();
            
            if (!colPart.equals("*")) {
                List<String> cols = smartSplit(colPart, ',');
                for (String c : cols) sq.addColumn(c.trim());
            }

            int whereIdx = lower.indexOf("where");
            int joinIdx = lower.indexOf("join");

            if (joinIdx != -1) {
                int onIdx = lower.indexOf("on");
                String joinTable = query.substring(joinIdx + 4, onIdx).trim();
                sq.setJoinTable(joinTable);
                String baseTable = query.substring(fromIdx + 4, joinIdx).trim();
                sq.setBaseTable(baseTable);
            } else {
                String baseTable = (whereIdx != -1) 
                    ? query.substring(fromIdx + 4, whereIdx).trim() 
                    : query.substring(fromIdx + 4).trim().replace(";", "");
                sq.setBaseTable(baseTable);
            }

            if (whereIdx != -1) {
                String whereClause = query.substring(whereIdx + 5).trim();
                String lowerWhere = whereClause.toLowerCase();

                if (lowerWhere.contains("in")) {
                    int inPos = lowerWhere.indexOf("in");
                    int openParen = whereClause.indexOf("(", inPos);
                    int closeParen = whereClause.lastIndexOf(")");
                    if (openParen != -1 && closeParen != -1) {
                        String subQueryStr = whereClause.substring(openParen + 1, closeParen).trim();
                        sqlParser subParser = new sqlParser(subQueryStr);
                        sq.setSubquery((SelectQuery) subParser.parse());
                    }
                } else {
                    int eqIdx = lowerWhere.indexOf("=");
                    if (eqIdx != -1) {
                        String filterCol = whereClause.substring(0, eqIdx).trim();
                        String filterVal = whereClause.substring(eqIdx + 1).trim().replace(";", "").replaceAll("^['\"]|['\"]$", "");
                        
                        if (filterCol.toLowerCase().contains("id")) {
                            try {
                                int id = Integer.parseInt(filterVal);
                                sq = new SelectQuery(id);
                                sq.setBaseTable(query.substring(fromIdx + 4, whereIdx).trim());
                                if (!colPart.equals("*")) {
                                    List<String> cols = smartSplit(colPart, ',');
                                    for (String c : cols) sq.addColumn(c.trim());
                                }
                            } catch (NumberFormatException e) {
                                sq.setFilter(filterCol, filterVal);
                            }
                        } else {
                            sq.setFilter(filterCol, filterVal);
                        }
                    }
                }
            }
            return sq;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing SELECT: " + e.getMessage());
        }
    }
    
    private UpdateQuery parseUpdate() {
        try {
            String lower = query.toLowerCase();
            int setIdx = lower.indexOf("set");
            String tableName = query.substring(7, setIdx).trim();
            int whereIdx = lower.indexOf("where");
            
            String setClause = query.substring(setIdx + 3, whereIdx).trim();
            int eqIdxSet = setClause.indexOf("=");
            String newVal = setClause.substring(eqIdxSet + 1).trim().replaceAll("^['\"]|['\"]$", "");
            
            String whereClause = query.substring(whereIdx + 5).trim();
            int eqIdxWhere = whereClause.indexOf("=");
            String filterCol = whereClause.substring(0, eqIdxWhere).trim();
            String filterVal = whereClause.substring(eqIdxWhere + 1).trim().replace(";", "").replaceAll("^['\"]|['\"]$", "");
            
            int id = -1;
            if (filterCol.toLowerCase().contains("id")) {
                try { id = Integer.parseInt(filterVal); } catch (Exception e) {}
            }
            
            UpdateQuery uq = new UpdateQuery(tableName, id, newVal);
            uq.setFilter(filterCol, filterVal);
            return uq;
        } catch (Exception e) { throw new IllegalArgumentException("Error parsing UPDATE query: " + e.getMessage()); }
    }
    
    private DeleteQuery parseDelete() {
        try {
            int fromIdx = query.toLowerCase().indexOf("from") + 4;
            int whereIdx = query.toLowerCase().indexOf("where");
            String tableName = query.substring(fromIdx, whereIdx).trim();
            String whereClause = query.substring(whereIdx + 5).trim();
            int eqIdx = whereClause.indexOf("=");
            String idStr = whereClause.substring(eqIdx + 1).trim().replace(";", "");
            return new DeleteQuery(tableName, Integer.parseInt(idStr));
        } catch (Exception e) { throw new IllegalArgumentException("Error parsing DELETE query: " + e.getMessage()); }
    }
}
