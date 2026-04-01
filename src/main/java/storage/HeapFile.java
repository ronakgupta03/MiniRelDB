package storage;

import java.io.IOException;

public class HeapFile {

    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;

        int totalPages = diskManager.getTotalPages();

        if (totalPages == 0) {
            // Fresh DB
            this.nextPageId = 0;
            this.currentPage = new Page(0);
        } else {
            // Existing DB
            this.nextPageId = totalPages - 1;

            // Load last page from disk
            this.currentPage = diskManager.readPage(nextPageId);
        }
    }

    public void insertRecord(DBRecord record) throws IOException {

        byte[] recordBytes = record.toBytes();

        boolean success = currentPage.insertRecord(recordBytes);

        if(!success) {
            // Page full -> write it to disk
            diskManager.writePage(currentPage);
            System.out.println("Page " + currentPage.getPageId() + " full, writing to disk");

            // Create new page
            nextPageId++;
            currentPage = new Page(nextPageId);

            // Insert into new page
            currentPage.insertRecord(recordBytes);
        }

        System.out.println("Inserted record into page " + currentPage.getPageId());
    }

    public void flush() throws IOException {
        diskManager.writePage(currentPage);
        System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
    }

    public Page getCurrentPage() {
        return currentPage;
    }
}