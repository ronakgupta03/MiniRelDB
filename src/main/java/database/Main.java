import java.util.List;

import storage.*;

public class Main {

    public static void main(String[] args) {

        try {
            DiskManager dm = new DiskManager("data/database.db");
            
            // //  - Test Page (Phase 1)
            // // Step 1: Create a page
            // Page page = new Page(0);

            // // Step 2: Put some data
            // byte[] data = page.getData();
            // String text = "Hello MiniRelDB!";
            // byte[] textBytes = text.getBytes();

            // System.arraycopy(textBytes, 0, data, 0, textBytes.length);

            // // Step 3: Write to disk
            // dm.writePage(page);
            // System.out.println("Page written!");

           


            // // Step 5: Convert bytes back to string
            // String result = new String(readPage.getData()).trim();
            // System.out.println("Read from DB: " + result);


          // 🔹 HeapFile test (REAL DB behavior)
            HeapFile heapFile = new HeapFile(dm);

            Executor executor = new Executor(heapFile, dm);

            

            heapFile.insertRecord(new DBRecord(1, "Alice"));
            heapFile.insertRecord(new DBRecord(2, "Bob"));
            heapFile.insertRecord(new DBRecord(3, "Charlie"));          
            
            heapFile.flush(); // VERY IMPORTANT to flush remaining data to disk!




             // // Step 4: Read from disk
            Page readPage = dm.readPage(0);


            List<DBRecord> records = readPage.getAllRecords();

            System.out.println("Records in page 0:");

            for (DBRecord record : records) {
                System.out.println(record);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    
    
    
}