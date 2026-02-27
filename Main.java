import java.sql.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Connection conn = null;
        Scanner scanner = new Scanner(System.in);

        try {
            Class.forName("org.sqlite.JDBC");

            conn = DriverManager.getConnection("jdbc:sqlite:test.db");

            Statement stmt = conn.createStatement();

            // Create Table
            stmt.executeUpdate(
                "CREATE tABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT)"
            );


            
             // ✅ PreparedStatement for batch insert
             PreparedStatement ps =
                conn.prepareStatement(
                    "INSERT INTO users(name) VALUES(?)"
                );

            System.out.println("MiniRelDB Started");
            System.out.println("Commands: ");
            System.out.println("INSERT <name1> <name2> ...");
            System.out.println("SELECT");
            System.out.println("Clear");
            System.out.println("EXIT");


            // ================ Console Loop ============================
            while(true){
                System.out.print("\nMiniRelDB > ");
                String input = scanner.nextLine().trim();

                if (input.equalsIgnoreCase("EXIT"))
                    break;

                // ======================== INSET ===========================
                else if (input.toUpperCase().startsWith("INSERT")) {
                    String[] parts = input.split(" ");

                    if (parts.length < 2){
                        System.out.println("Usage: INSERT <name>");
                        continue;
                    }

                    conn.setAutoCommit(false);

                    for(int i = 1; i < parts.length; i++){
                        ps.setString(1, parts[i]);
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    conn.commit();

                    System.out.println(
                        (parts.length - 1) + "record(s) inserted."
                    );
                }

                // =================== SELECT ==============================
                else if (input.equalsIgnoreCase("SELECT")) {

                    ResultSet rs =  stmt.executeQuery("SELECT * FROM users");

                    System.out.println("\nID | NAME");

                    while(rs.next()) {
                        System.out.println(
                            rs.getInt("id") + " | " +
                            rs .getString("name")
                        );
                    }
                }


                // ========================= CLEAR ==========================
                else if (input.equalsIgnoreCase("CLEAR")) {
                    
                    stmt.executeUpdate("DELETE FROM users");
                    System.out.println("Table cleared.");
                }

                else {
                    System.out.println("Unknown command.");
                }
            }

            conn.close();
            scanner.close();


            System.out.println("MiniRelDB Closed");
        } catch (Exception e) {
            e.printStackTrace();
        }


            // // Insert Data
            // // stmt.executeUpdate("INSERT INTO users(name) VALUES('Ronak')");
            // // stmt.executeUpdate("INSERT INTO users(name) VALUES('Bhavya')");
            // // stmt.executeUpdate("INSERT INTO users(name) VALUES('Utsav')");
            // PreparedStatement ps =
            //     conn.prepareStatement(
            //         "INSERT INTO users(name) VALUES(?)"
            //     );

            // ps.setString(1, "Ronak");
            // ps.addBatch();

            // ps.setString(1, "Bhavya");
            // ps.addBatch();

            // ps.setString(1, "Utsav");
            // ps.addBatch();

            // ps.executeBatch();

            // System.out.println("Data inserted.");


            // //Read Data
            // ResultSet rs =
            //     stmt.executeQuery("SELECT * FROM users");

            // System.out.println("\nUsers Table: ");


            // while(rs.next()){
            //     int id = rs.getInt("id");
            //     String name = rs.getString("name");

            //     System.out.println(id + " | " + name);
            // }

            // conn.close();
    }
}


// import java.sql.*;

// public class Main {
//     public static void main(String[] args) {
//         try {

//             // ⭐ ADD THIS LINE
//             Class.forName("org.sqlite.JDBC");

//             Connection conn =
//                 DriverManager.getConnection("jdbc:sqlite:test.db");

//             System.out.println("Connected to SQLite!");

//             conn.close();

//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }
// }
