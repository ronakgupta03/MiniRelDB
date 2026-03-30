// We need: DiskManager dm;  Because HeapFile does NOT directly talk to disk — it uses DiskManager.

package storage;

import java.io.IOException;

public class HeapFile {
    
    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;

    public HeapFile(DiskManager diskManager) {
        this.diskManager = diskManager;
        this.nextPageId = 0;
        this.currentPage = new Page(nextPageId);
    }

    public void insertRecord(DBRecord record) throws IOException {

        // // Create a new page for the record
        // Page page = new Page(nextPageId);

        // Serialize the record (convert it to bytes)
        byte[] recordBytes = record.toBytes();

        // // Copy record into page
        // System.arraycopy(recordBytes, 0, page.getData(), 0, recordBytes.length);

        // Insert record using page logic.  Try inserting into current page
        boolean success = currentPage.insertRecord(recordBytes);

        if(!success) {
            // Page full -> write it to disk
            diskManager.writePage(currentPage);
            System.out.println("Page" + currentPage.getPageId() + " full, writing to disk");

            // Create new page
            nextPageId++;
            currentPage = new Page(nextPageId);

            // Insert into new page
            currentPage.insertRecord(recordBytes);
        }

        // // Write page to disk
        // diskManager.writePage(page);
        System.out.println("Inserted record into page " + currentPage.getPageId());

        // // Move to next page
        // nextPageId++;
    }

    // Flush remaining data to disk (VERY IMPORTANT)
    public void flush() throws IOException {
        diskManager.writePage(currentPage);
        System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
    }

    //Getter (userfull for debudding/testing)
    public Page getCurrentPage() {
        return currentPage;
    }
}


