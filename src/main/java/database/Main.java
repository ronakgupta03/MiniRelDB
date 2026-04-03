import storage.*;
import query.*;
import java.util.Scanner;
import java.util.List;


public class Main {

    public static void main(String[] args) {

        try {
            DiskManager dm = new DiskManager("data/database.db");
            HeapFile heapFile = new HeapFile(dm);

            Executor executor = new Executor(heapFile);

            Scanner sc = new Scanner(System.in);
            String exit = "";
            
            while(!exit.equals("exit")){

                System.out.print("Enter query: ");
                String query = sc.nextLine().trim();
                query = query.toLowerCase();
                if (query.equals("exit")) {
                    exit = query;
                    System.out.println("Exiting...");
                    continue;
                }
                
                try {
                    sqlParser parser = new sqlParser(query);
                    Object parsedQuery = parser.parse();
                    
                    if (parsedQuery instanceof InsertQuery) {
                        executor.executeInsert((InsertQuery) parsedQuery);
                        System.out.println("Insert executed.");
                    } else if (parsedQuery instanceof SelectQuery) {
                        List<DBRecord> results = executor.executeSelect((SelectQuery) parsedQuery);
                        System.out.println("Select results:");
                        for (DBRecord record : results) {
                            System.out.println("ID: " + record.getId() + ", Name: " + record.getName());
                        }
                    } else if (parsedQuery instanceof UpdateQuery) {
                        executor.executeUpdate((UpdateQuery) parsedQuery);
                        System.out.println("Update executed.");
                    } else if (parsedQuery instanceof DeleteQuery) {
                        executor.executeDelete((DeleteQuery) parsedQuery);
                        System.out.println("Delete executed.");
                    }
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}