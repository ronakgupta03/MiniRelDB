package query;

public class UpdateQuery {
    private int id;
    private String newName;

    public UpdateQuery(int id, String newName) {
        this.id = id;
        this.newName = newName;
    }

    public int getId() {
        return id;
    }

    public String getNewName() {
        return newName;
    }
}