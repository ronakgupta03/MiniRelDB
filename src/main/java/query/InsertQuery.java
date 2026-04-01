package query;

public class InsertQuery {

    private int id;
    private String name;

    public InsertQuery(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}