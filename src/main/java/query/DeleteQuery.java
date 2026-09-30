package query;

public class DeleteQuery {
    private int id;
    private String tableName;

    public DeleteQuery(int id) {
        this.id = id;
    }

    public DeleteQuery(String tableName, int id) {
        this.tableName = tableName;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTableName() {
        return tableName;
    }
}