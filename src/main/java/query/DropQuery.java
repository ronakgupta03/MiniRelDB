package query;

public class DropQuery {
    private String type; // "DATABASE" or "TABLE"
    private String name;
    public DropQuery(String type, String name) { 
        this.type = type.toUpperCase();
        this.name = name;
    }
    public String getType() { return type; }
    public String getName() { return name; }
}
