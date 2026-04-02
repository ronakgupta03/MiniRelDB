// We need: DiskManager dm;  Because HeapFile does NOT directly talk to disk — it uses DiskManager.

package storage;

import java.io.IOException;

public class HeapFile {
    
    private DiskManager diskManager;
    private int nextPageId;

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.nextPageId = diskManager.getPageCount();
    }

    public void insertRecord(DBRecord record) throws IOException {

        // Create a new page for the record
        Page page = new Page(nextPageId);

        // Serialize the record (convert it to bytes)
        byte[] recordBytes = record.toBytes();

        // Copy record into page
        System.arraycopy(recordBytes, 0, page.getData(), 0, recordBytes.length);

        // Write page to disk
        diskManager.writePage(page);

        System.out.println("Inserted record into page " + nextPageId);

        // Move to next page
        nextPageId++;

    }

    public java.util.List<DBRecord> getAllRecords() throws IOException {
        java.util.List<DBRecord> records = new java.util.ArrayList<>();
        int pageCount = diskManager.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            byte[] data = page.getData();
            if (data.length == 0 || isPageEmpty(data)) {
                continue;
            }
            DBRecord record = DBRecord.fromBytes(data);
            if (!record.isDeleted()) {
                records.add(record);
            }
        }
        return records;
    }

    public void deleteRecord(int id) throws IOException {
        int pageCount = diskManager.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            byte[] data = page.getData();
            if (data.length == 0 || isPageEmpty(data)) {
                continue;
            }
            DBRecord record = DBRecord.fromBytes(data);
            if (record.getId() == id && !record.isDeleted()) {
                // Mark record as deleted and write back to disk
                record.setDeleted(true);
                byte[] updatedBytes = record.toBytes();
                byte[] pageData = page.getData();
                System.arraycopy(updatedBytes, 0, pageData, 0, updatedBytes.length);
                page.setData(pageData);
                diskManager.writePage(page);
                System.out.println("Deleted record with ID " + id);
                return;
            }
        }
        System.out.println("Record with ID " + id + " not found.");
    }

    private boolean isPageEmpty(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}


