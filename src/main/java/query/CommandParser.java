package query;

public abstract class CommandParser {
    
    public static Object parse(String input) {
        
        String[] parts = input.trim().split(" ");
        String command = parts[0].toUpperCase();

        if (command.equals("INSERT")) {

            if (parts.length < 3) {
                throw new IllegalArgumentException("Usage: INSERT <id> <name>");
            }

            int id = Integer.parseInt(parts[1]);
            String name = parts[2];

            return new InsertQuery(id, name);
        }

        if (command.equals("SELECT")) {
            return new SelectQuery();
        }

        throw new IllegalArgumentException("Unknown command: " + input);
    }
}
