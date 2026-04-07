package query;

public class AlterTableQuery {
    private String tableName;
    private String operation; // ADD, DROP, MODIFY
    private String columnName;
    private String columnType;

    public AlterTableQuery(String tableName, String operation, String columnName, String columnType) {
        this.tableName = tableName;
        this.operation = operation;
        this.columnName = columnName;
        this.columnType = columnType;
    }

    public String getTableName() {
        return tableName;
    }

    public String getOperation() {
        return operation;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getColumnType() {
        return columnType;
    }
}
