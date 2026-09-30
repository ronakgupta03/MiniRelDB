package query;

import java.util.List;
import java.util.Map;

public class MultiInsertQuery {
    private String tableName;
    private List<Map<String, Object>> rows;

    public MultiInsertQuery(String tableName, List<Map<String, Object>> rows) {
        this.tableName = tableName;
        this.rows = rows;
    }

    public String getTableName() {
        return tableName;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }
}
