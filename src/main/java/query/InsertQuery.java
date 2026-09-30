package query;

import java.util.LinkedHashMap;
import java.util.Map;

public class InsertQuery {

    private Map<String, Object> values = new LinkedHashMap<>();
    private String tableName;

    public InsertQuery(String tableName, Map<String, Object> values) {
        this.tableName = tableName;
        this.values = values;
    }

    // Legacy constructor for backward compatibility
    public InsertQuery(String tableName, int id, String name) {
        this.tableName = tableName;
        this.values.put("col0", id);
        this.values.put("col1", name);
    }

    public int getId() {
        Object id = values.get("id");
        if (id == null) id = values.get("col0");
        if (id == null) return -1;
        if (id instanceof Integer) return (Integer) id;
        try {
            return Integer.parseInt(id.toString());
        } catch (Exception e) {
            return -1;
        }
    }

    public String getName() {
        Object name = values.get("name");
        return name != null ? name.toString() : "";
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, Object> getValues() {
        return values;
    }
}
