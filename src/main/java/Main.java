import database.Database;
import java.util.*;

public class Main {

    public static void main(String[] args) throws Exception {

        Database db = new Database();

        Map<String,Object> user = new HashMap<>();
        user.put("id", 1);
        user.put("name", "Ronak");
        user.put("age", 22);

        db.insert("users", user);

        db.display("users");
    }
}