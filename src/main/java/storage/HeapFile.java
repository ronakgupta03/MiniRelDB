package storage;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;

public class HeapFile {

    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.nextPageId = diskManager.getPageCount();
    }

    public int insertRecord(DBRecord record) throws IOException {
        if (currentPage == null) {
            currentPage = new Page(nextPageId, (byte) 1);
            nextPageId++;
        }

        byte[] recordBytes = record.toBytes();
        boolean success = currentPage.insertRecord(recordBytes);

        if (!success) {
            // Page full -> write it to disk
            diskManager.writePage(currentPage);
            System.out.println("Page " + currentPage.getPageId() + " full, writing to disk");

            // Create new page
            currentPage = new Page(nextPageId, (byte) 1);
            nextPageId++;

            // Insert into the new page
            currentPage.insertRecord(recordBytes);
        }

        return currentPage.getPageId();
    }

    public java.util.List<DBRecord> getAllRecords() throws IOException {
        java.util.List<DBRecord> records = new java.util.ArrayList<>();
        int pageCount = diskManager.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            records.addAll(page.getAllRecords());
        }
        
        if (currentPage != null) {
            records.addAll(currentPage.getAllRecords());
        }
        
        return records;
    }

    public DBRecord getRecordByPageId(int pageId, int id) throws IOException {
        Page page = diskManager.readPage(pageId);
        List<DBRecord> records = page.getAllRecordsWithTombstones();
        for (DBRecord record : records) {
            if (record.getId() == id) {
                return record;
            }
        }
        
        // Also check current page if pageId matches
        if (currentPage != null && currentPage.getPageId() == pageId) {
            List<DBRecord> currentRecords = currentPage.getAllRecordsWithTombstones();
            for (DBRecord record : currentRecords) {
                if (record.getId() == id) {
                    return record;
                }
            }
        }
        
        return null;
    }

    public void deleteRecord(int id) throws IOException {
        int pageCount = diskManager.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            List<Page.RecordWithOffset> records = page.getAllRecordsWithOffsets();
            
            for (Page.RecordWithOffset rO : records) {
                DBRecord record = rO.record;
                if (record.getId() == id && !record.isDeleted()) {
                    record.setDeleted(true);
                    byte[] updatedBytes = record.toBytes();
                    byte[] pageData = page.getData();
                    // length prefix at 'rO.offset', record at 'rO.offset + 4'
                    System.arraycopy(updatedBytes, 0, pageData, rO.offset + 4, Math.min(updatedBytes.length, rO.length));
                    diskManager.writePage(page);
                    System.out.println("Deleted record with ID " + id + " from page " + i);
                    return;
                }
            }
        }
        
        if (currentPage != null) {
            List<Page.RecordWithOffset> records = currentPage.getAllRecordsWithOffsets();
            for (Page.RecordWithOffset rO : records) {
                DBRecord record = rO.record;
                if (record.getId() == id && !record.isDeleted()) {
                    record.setDeleted(true);
                    byte[] updatedBytes = record.toBytes();
                    System.arraycopy(updatedBytes, 0, currentPage.getData(), rO.offset + 4, Math.min(updatedBytes.length, rO.length));
                    System.out.println("Deleted record with ID " + id + " from current page.");
                    return;
                }
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
        if (currentPage != null) {
            diskManager.writePage(currentPage);
            System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
        }
        diskManager.flush();
    }

    public Page getCurrentPage() {
        return currentPage;
    }

    public DiskManager getDiskManager() {
        return diskManager;
    }
}
