package query;

public class UpdateQuery {
    private String tableName;
    private int id;
    private String newName;
    private String filterColumn;
    private Object filterValue;

    public UpdateQuery(String tableName, int id, String newName) {
        this.tableName = tableName;
        this.id = id;
        this.newName = newName;
    }

    public String getTableName() { return tableName; }
    public int getId() { return id; }
    public String getNewName() { return newName; }

    public void setFilter(String col, Object val) {
        this.filterColumn = col;
        this.filterValue = val;
    }

    public String getFilterColumn() { return filterColumn; }
    public Object getFilterValue() { return filterValue; }
}
