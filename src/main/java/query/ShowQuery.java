package query;

public class ShowQuery {
    private String type; // "DATABASES" or "TABLES"
    public ShowQuery(String type) { this.type = type.toUpperCase(); }
    public String getType() { return type; }
}
