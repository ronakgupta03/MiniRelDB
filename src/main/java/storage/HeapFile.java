package storage;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HeapFile {

    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.nextPageId = diskManager.getPageCount();
        // If no pages exist yet, create current page at page 0
        if (this.nextPageId == 0) {
            this.currentPage = new Page(0, (byte) 1);
            this.nextPageId = 1;
        }
    }

    public int insertRecord(DBRecord record) throws IOException {
        rwLock.writeLock().lock();
        try {
            if (currentPage == null) {
                currentPage = new Page(nextPageId, (byte) 1);
                nextPageId++;
            }

            byte[] recordBytes = record.toBytes();
            boolean success = currentPage.insertRecord(recordBytes);

            if (!success) {
                // Page full -> write it to disk
                diskManager.writePage(currentPage);
                // System.out.println("Page " + currentPage.getPageId() + " full, writing to disk");

                // Create new page
                currentPage = new Page(nextPageId, (byte) 1);
                nextPageId++;

                // Insert into the new page
                currentPage.insertRecord(recordBytes);
            }

            return currentPage.getPageId();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public java.util.List<DBRecord> getAllRecords() throws IOException {
        rwLock.readLock().lock();
        try {
            java.util.List<DBRecord> records = new java.util.ArrayList<>();
            int pageCount = diskManager.getPageCount();

            // PHASE 2.3 OPTIMIZATION: Read all pages sequentially, skip tombstoned records
            // The buffer pool (now 1000 pages) should cache all pages for small tables
            for (int i = 0; i < pageCount; i++) {
                Page page = diskManager.readPage(i);
                List<DBRecord> pageRecords = page.getAllRecords();
                for (DBRecord r : pageRecords) {
                    if (!r.isDeleted()) {
                        records.add(r);
                    }
                }
            }

            if (currentPage != null) {
                List<DBRecord> currentRecords = currentPage.getAllRecords();
                for (DBRecord r : currentRecords) {
                    if (!r.isDeleted()) {
                        records.add(r);
                    }
                }
            }

            return records;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void updateRecordInPlace(int pageId, DBRecord record) throws IOException {
        rwLock.writeLock().lock();
        try {
            Page page = diskManager.readPage(pageId);
            List<Page.RecordWithOffset> records = page.getAllRecordsWithOffsets();
            for (Page.RecordWithOffset rO : records) {
                if (rO.record.getId() == record.getId()) {
                    byte[] updatedBytes = record.toBytes();
                    byte[] pageData = page.getData();
                    int copyLen = Math.min(updatedBytes.length, rO.length);
                    System.arraycopy(updatedBytes, 0, pageData, rO.offset + 4, copyLen);
                    diskManager.writePage(page);
                    return;
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public DBRecord getRecordByPageId(int pageId, int id) throws IOException {
        rwLock.readLock().lock();
        try {
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
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void deleteRecord(int id) throws IOException {
        rwLock.writeLock().lock();
        try {
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
                        // System.out.println("Deleted record with ID " + id + " from page " + i);
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
                        // System.out.println("Deleted record with ID " + id + " from current page.");
                        return;
                    }
                }
            }

            // System.out.println("Record with ID " + id + " not found.");
        } finally {
            rwLock.writeLock().unlock();
        }
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
        rwLock.writeLock().lock();
        try {
            if (currentPage != null) {
                diskManager.writePage(currentPage);
                // System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
            }
            diskManager.flush();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public Page getCurrentPage() {
        rwLock.readLock().lock();
        try {
            return currentPage;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public DiskManager getDiskManager() {
        rwLock.readLock().lock();
        try {
            return diskManager;
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
