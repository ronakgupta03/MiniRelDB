package query;

public class CreateIndexQuery {
    private String tableName;
    private String columnName;

    public CreateIndexQuery(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getColumnName() {
        return columnName;
    }
}
