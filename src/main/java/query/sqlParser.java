package query;

public class sqlParser {
    
    private String query;
    
    public sqlParser(String query) {
        this.query = query.trim();
    }

    public Object parse() {
        // Normalize query to lowercase for keyword matching
        String lowerQuery = query.toLowerCase();
        
        if (lowerQuery.startsWith("insert")) {
            return parseInsert();
        } else if (lowerQuery.startsWith("select")) {
            return parseSelect();
        } else if (lowerQuery.startsWith("update")) {
            return parseUpdate();
        } else if (lowerQuery.startsWith("delete")) {
            return parseDelete();
        } else {
            throw new IllegalArgumentException("Unsupported query type: " + query);
        }
    }
    
    private InsertQuery parseInsert() {
        // Expected format: INSERT INTO table VALUES (id, 'name') or VALUES (id, "name")
        try {
            String lowerQuery = query.toLowerCase();
            int valuesIndex = lowerQuery.indexOf("values");
            if (valuesIndex == -1) {
                throw new IllegalArgumentException("Invalid INSERT syntax");
            }
            String valuesPart = query.substring(valuesIndex + "values".length()).trim();
            // Allow optional trailing semicolon
            valuesPart = valuesPart.replaceAll(";+$", "").trim();
            if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
                throw new IllegalArgumentException("Invalid VALUES format");
            }
            valuesPart = valuesPart.substring(1, valuesPart.length() - 1); // Remove parentheses
            
            // Parse id and name, handling quoted strings properly
            String[] parts = new String[2];
            int commaIndex = valuesPart.indexOf(',');
            if (commaIndex == -1) {
                throw new IllegalArgumentException("Expected 2 values: id and name");
            }
            
            parts[0] = valuesPart.substring(0, commaIndex).trim();
            parts[1] = valuesPart.substring(commaIndex + 1).trim();
            
            int id = Integer.parseInt(parts[0]);
            
            // Extract name, removing surrounding quotes
            String name = parts[1];
            if ((name.startsWith("'") && name.endsWith("'")) || (name.startsWith("\"") && name.endsWith("\""))) {
                name = name.substring(1, name.length() - 1);
            }
            
            return new InsertQuery(id, name);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing INSERT query: " + e.getMessage());
        }
    }
    
    private SelectQuery parseSelect() {
        // Expected format: SELECT * FROM table [WHERE id=1]
        // For now, support optional WHERE id filter
        String lower = query.toLowerCase();
        int where = lower.indexOf("where id=");
        if (where == -1) {
            return new SelectQuery();
        }

        String cond = query.substring(where + "where id=".length()).trim();
        // Remove trailing semicolon if present
        cond = cond.replaceAll(";+$", "").trim();
        if (cond.contains(" ")) {
            cond = cond.substring(0, cond.indexOf(" ")).trim();
        }

        try {
            int id = Integer.parseInt(cond);
            return new SelectQuery(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid SELECT WHERE id value: " + cond);
        }
    }
    
    private UpdateQuery parseUpdate() {
        // Expected format: UPDATE table SET name='newname' WHERE id=1 or SET name="newname"
        try {
            // Simple parsing: find id and newname
            int idIndex = query.toLowerCase().indexOf("where id=");
            if (idIndex == -1) throw new IllegalArgumentException("Missing WHERE id=");
            String idStr = query.substring(idIndex + 9).trim();
            // Remove trailing semicolon if present
            idStr = idStr.replaceAll(";+$", "").trim();
            if (idStr.contains(" ")) {
                idStr = idStr.substring(0, idStr.indexOf(" ")).trim();
            }
            int id = Integer.parseInt(idStr);
            
            int setIndex = query.toLowerCase().indexOf("set name=");
            if (setIndex == -1) throw new IllegalArgumentException("Missing SET name=");
            String namePart = query.substring(setIndex + 9).trim();
            
            // Find the closing quote (either single or double)
            int quoteEnd = -1;
            char quoteChar = ' ';
            if (namePart.length() > 0) {
                quoteChar = namePart.charAt(0);
                if (quoteChar == '\'' || quoteChar == '"') {
                    quoteEnd = namePart.indexOf(quoteChar, 1);
                    if (quoteEnd == -1) {
                        throw new IllegalArgumentException("Unterminated string in SET name");
                    }
                } else {
                    quoteEnd = namePart.indexOf(" ");
                    if (quoteEnd == -1) {
                        quoteEnd = namePart.length();
                    }
                }
            }
            
            String newName;
            if (quoteChar == '\'' || quoteChar == '"') {
                newName = namePart.substring(1, quoteEnd);
            } else {
                newName = namePart.substring(0, quoteEnd);
            }
            
            return new UpdateQuery(id, newName);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing UPDATE query: " + e.getMessage());
        }
    }
    
    private DeleteQuery parseDelete() {
        // Expected format: DELETE FROM table WHERE id=1
        try {
            int idIndex = query.toLowerCase().indexOf("where id=");
            if (idIndex == -1) throw new IllegalArgumentException("Missing WHERE id=");
            String idStr = query.substring(idIndex + 9).trim();
            // Remove trailing semicolon if present
            idStr = idStr.replaceAll(";+$", "").trim();
            if (idStr.contains(" ")) {
                idStr = idStr.substring(0, idStr.indexOf(" ")).trim();
            }
            int id = Integer.parseInt(idStr);
            return new DeleteQuery(id);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing DELETE query: " + e.getMessage());
        }
    }
}