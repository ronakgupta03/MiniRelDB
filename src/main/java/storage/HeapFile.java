package storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class HeapFile {

    private DiskManager diskManager;
    private int nextPageId;
    private Page currentPage;

    public HeapFile(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        this.nextPageId = diskManager.getPageCount();
        this.currentPage = new Page(nextPageId);
    }

    public int insertRecord(DBRecord record) throws IOException {

        byte[] recordBytes = record.toBytes();

        boolean success = currentPage.insertRecord(recordBytes);

        if (!success) {
            // Page full -> write it to disk
            diskManager.writePage(currentPage);
            System.out.println("Page " + currentPage.getPageId() + " full, writing to disk");

            // Create new page and insert there
            nextPageId++;
            currentPage = new Page(nextPageId);
            success = currentPage.insertRecord(recordBytes);
            if (!success) {
                throw new IOException("Record is too large to fit in a single page");
            }
        }

        return currentPage.getPageId();
    }

    public java.util.List<DBRecord> getAllRecords() throws IOException {
        java.util.List<DBRecord> records = new java.util.ArrayList<>();
        int pageCount = diskManager.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            records.addAll(readRecordsFromPage(page));
        }

        if (currentPage != null && !isPageEmpty(currentPage.getData()) && currentPage.getPageId() >= pageCount) {
            records.addAll(readRecordsFromPage(currentPage));
        }

        return records;
    }

    public DBRecord getRecordByPageId(int pageId) throws IOException {
        Page page = diskManager.readPage(pageId);
        java.util.List<DBRecord> records = readRecordsFromPage(page);
        for (DBRecord record : records) {
            if (!record.isDeleted()) {
                return record;
            }
        }
        return null;
    }

    public void deleteRecord(int id) throws IOException {
        int pageCount = diskManager.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            byte[] data = page.getData();
            if (data.length == 0 || isPageEmpty(data)) {
                continue;
            }

            int freeSpaceOffset = ByteBuffer.wrap(data, 0, 4).getInt();
            int offset = 4;
            int end = 4 + freeSpaceOffset;

            while (offset + 9 <= end) {
                boolean deleted = data[offset] == 1;
                int recordId = ByteBuffer.wrap(data, offset + 1, 4).getInt();
                int nameLength = ByteBuffer.wrap(data, offset + 5, 4).getInt();

                if (recordId == id && !deleted) {
                    data[offset] = 1;
                    page.setData(data);
                    diskManager.writePage(page);
                    System.out.println("Deleted record with ID " + id);
                    return;
                }

                offset += 1 + 4 + 4 + nameLength;
            }
        }
        System.out.println("Record with ID " + id + " not found.");
    }

    private java.util.List<DBRecord> readRecordsFromPage(Page page) {
        java.util.List<DBRecord> records = new java.util.ArrayList<>();
        byte[] data = page.getData();
        if (isPageEmpty(data)) {
            return records;
        }

        int freeSpaceOffset = ByteBuffer.wrap(data, 0, 4).getInt();
        int offset = 4;
        int end = 4 + freeSpaceOffset;

        while (offset + 9 <= end) {
            boolean deleted = data[offset] == 1;
            offset += 1;
            int recordId = ByteBuffer.wrap(data, offset, 4).getInt();
            offset += 4;
            int nameLength = ByteBuffer.wrap(data, offset, 4).getInt();
            offset += 4;

            if (nameLength <= 0 || offset + nameLength > end) {
                break;
            }

            byte[] nameBytes = new byte[nameLength];
            System.arraycopy(data, offset, nameBytes, 0, nameLength);
            offset += nameLength;

            String name = new String(nameBytes, StandardCharsets.UTF_8);
            if (!deleted) {
                records.add(new DBRecord(recordId, name));
            }
        }

        return records;
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
        if (currentPage != null && !isPageEmpty(currentPage.getData())) {
            diskManager.writePage(currentPage);
            System.out.println("Flushed page " + currentPage.getPageId() + " to disk");
        }
    }

    public void updateRecord(int id, String newName) throws IOException {
        int pageCount = diskManager.getPageCount();
        for (int i = 0; i < pageCount; i++) {
            Page page = diskManager.readPage(i);
            byte[] data = page.getData();
            if (isPageEmpty(data)) {
                continue;
            }

            if (updateRecordInPage(page, id, newName)) {
                diskManager.writePage(page);
                System.out.println("Updated record with ID " + id);
                return;
            }
        }

        if (currentPage != null && updateRecordInPage(currentPage, id, newName)) {
            diskManager.writePage(currentPage);
            System.out.println("Updated record with ID " + id);
            return;
        }

        System.out.println("Record with ID " + id + " not found.");
    }

    private boolean updateRecordInPage(Page page, int id, String newName) throws IOException {
        byte[] data = page.getData();
        if (isPageEmpty(data)) {
            return false;
        }

        int freeSpaceOffset = ByteBuffer.wrap(data, 0, 4).getInt();
        int offset = 4;
        int end = 4 + freeSpaceOffset;

        while (offset + 9 <= end) {
            boolean deleted = data[offset] == 1;
            int recordId = ByteBuffer.wrap(data, offset + 1, 4).getInt();
            int nameLength = ByteBuffer.wrap(data, offset + 5, 4).getInt();

            if (!deleted && recordId == id) {
                byte[] newNameBytes = newName.getBytes(StandardCharsets.UTF_8);
                if (newNameBytes.length == nameLength) {
                    System.arraycopy(newNameBytes, 0, data, offset + 9, nameLength);
                    page.setData(data);
                    return true;
                } else {
                    data[offset] = 1;
                    page.setData(data);
                    diskManager.writePage(page);

                    DBRecord newRecord = new DBRecord(id, newName);
                    insertRecord(newRecord);
                    return true;
                }
            }

            offset += 1 + 4 + 4 + nameLength;
        }

        return false;
    }

    public Page getCurrentPage() {
        return currentPage;
    }
}