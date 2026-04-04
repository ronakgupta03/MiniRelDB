package storage;

import java.io.IOException;

public class HeapFile {

    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.nextPageId = diskManager.getPageCount();
    }

    public int insertRecord(DBRecord record) throws IOException {

        byte[] recordBytes = record.toBytes();

        boolean success = currentPage.insertRecord(recordBytes);

        if(!success) {
            // Page full -> write it to disk
            diskManager.writePage(currentPage);
            System.out.println("Page " + currentPage.getPageId() + " full, writing to disk");

            // Create new page
            nextPageId++;
            currentPage = new Page(nextPageId);

        int writtenPageId = nextPageId;
        // Move to next page
        nextPageId++;

        return writtenPageId;

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

    public DBRecord getRecordByPageId(int pageId) throws IOException {
        Page page = diskManager.readPage(pageId);
        byte[] data = page.getData();
        if (data.length == 0 || isPageEmpty(data)) {
            return null;
        }
        DBRecord record = DBRecord.fromBytes(data);
        if (record.isDeleted()) {
            return null;
        }
        return record;
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

    public void flush() throws IOException {
        diskManager.writePage(currentPage);
        System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
    }

    public Page getCurrentPage() {
        return currentPage;
    }
}