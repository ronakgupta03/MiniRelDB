package query;

public class SelectQuery {
    private Integer id;

    public SelectQuery() {
    }

    public SelectQuery(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public boolean hasIdFilter() {
        return id != null;
    }
}