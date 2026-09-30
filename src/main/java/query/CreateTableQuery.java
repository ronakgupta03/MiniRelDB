package query;

import java.util.ArrayList;
import java.util.List;
import catalog.CatalogManager.ColumnSchema;
import catalog.CatalogManager.ForeignKeySchema;

public class CreateTableQuery {
    private String tableName;
    private List<ColumnSchema> columns;
    private String primaryKey;
    private List<String> uniqueColumns = new ArrayList<>();
    private List<ForeignKeySchema> foreignKeys = new ArrayList<>();

    public CreateTableQuery(String tableName, List<ColumnSchema> columns, String primaryKey) {
        this.tableName = tableName;
        this.columns = columns;
        this.primaryKey = primaryKey;
    }

    public String getTableName() { return tableName; }
    public List<ColumnSchema> getColumns() { return columns; }
    public String getPrimaryKey() { return primaryKey; }
    
    public void addUniqueColumn(String col) { uniqueColumns.add(col); }
    public List<String> getUniqueColumns() { return uniqueColumns; }
    
    public void addForeignKey(ForeignKeySchema fk) { foreignKeys.add(fk); }
    public List<ForeignKeySchema> getForeignKeys() { return foreignKeys; }
}
