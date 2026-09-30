package storage;

import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Page {
    public static final int PAGE_SIZE = 4096;

    private int pageId;
    private byte[] data;
    private int freeSpaceOffset;
    private byte pageType; // 0: Internal, 1: Leaf, 2: Metadata
    private int parentPageId = -1;
    private int nextPageId = -1;
    private int checksum = 0;

    public Page(int pageId, byte pageType) {
        this.pageId = pageId;
        this.pageType = pageType;
        this.data = new byte[PAGE_SIZE];
        this.data[0] = pageType;
        this.freeSpaceOffset = 0;
        ByteBuffer.wrap(data, 1, 4).putInt(0);
        ByteBuffer.wrap(data, 5, 4).putInt(-1);
        ByteBuffer.wrap(data, 9, 4).putInt(-1);
        ByteBuffer.wrap(data, 13, 4).putInt(0);
    }

    public int getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }

    public byte getPageType() {
        return pageType;
    }

    public int getParentPageId() {
        return parentPageId;
    }

    public void setParentPageId(int parentPageId) {
        this.parentPageId = parentPageId;
        ByteBuffer.wrap(data, 5, 4).putInt(parentPageId);
    }

    public int getNextPageId() {
        return nextPageId;
    }

    public void setNextPageId(int nextPageId) {
        this.nextPageId = nextPageId;
        ByteBuffer.wrap(data, 9, 4).putInt(nextPageId);
    }

    public void setFreeSpaceOffset(int offset) {
        this.freeSpaceOffset = offset;
        ByteBuffer.wrap(data, 1, 4).putInt(offset);
    }

    public void setData(byte[] data) {
        this.data = data;
        this.pageType = data[0];
        ByteBuffer buffer = ByteBuffer.wrap(data, 1, 16);
        this.freeSpaceOffset = buffer.getInt();
        this.parentPageId = buffer.getInt();
        this.nextPageId = buffer.getInt();
        this.checksum = buffer.getInt();
    }

    public int getfreeSpaceOffset() {
        return freeSpaceOffset;
    }

    public void updateChecksum() {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        // Calculate CRC for all data except the checksum field itself (bytes 13-16)
        crc.update(data, 0, 13);
        crc.update(data, 17, PAGE_SIZE - 17);
        this.checksum = (int) crc.getValue();
        ByteBuffer.wrap(data, 13, 4).putInt(this.checksum);
    }

    public boolean verifyChecksum() {
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(data, 0, 13);
        crc.update(data, 17, PAGE_SIZE - 17);
        return this.checksum == (int) crc.getValue();
    }

    public boolean insertRecord(byte[] recordBytes) {
        // Header: 17 bytes. Max record size is PAGE_SIZE - 17 - 4 (for length prefix).
        if (recordBytes.length > PAGE_SIZE - 17 - 4) {
            return false;
        }
        if (17 + freeSpaceOffset + recordBytes.length + 4 > PAGE_SIZE) {
            return false;
        }

        int writePos = 17 + freeSpaceOffset;
        // Write record length (4 bytes)
        ByteBuffer.wrap(data, writePos, 4).putInt(recordBytes.length);
        // Write record data
        System.arraycopy(recordBytes, 0, data, writePos + 4, recordBytes.length);
        freeSpaceOffset += recordBytes.length + 4;

        ByteBuffer.wrap(data, 1, 4).putInt(freeSpaceOffset);
        return true;
    }

    public static class RecordWithOffset {
        public DBRecord record;
        public int offset;
        public int length;
        public RecordWithOffset(DBRecord record, int offset, int length) {
            this.record = record;
            this.offset = offset;
            this.length = length;
        }
    }

    public List<DBRecord> getAllRecords() {
        List<DBRecord> records = new ArrayList<>();
        for (RecordWithOffset r : getAllRecordsWithOffsets()) {
            if (!r.record.isDeleted()) records.add(r.record);
        }
        return records;
    }

    public List<RecordWithOffset> getAllRecordsWithOffsets() {
        List<RecordWithOffset> records = new ArrayList<>();
        int offset = 17; // skip header
        int end = 17 + freeSpaceOffset;

        while (offset + 4 <= end) {
            int recordLen = ByteBuffer.wrap(data, offset, 4).getInt();
            if (recordLen <= 0 || offset + 4 + recordLen > end) break;

            byte[] recordData = new byte[recordLen];
            System.arraycopy(data, offset + 4, recordData, 0, recordLen);

            records.add(new RecordWithOffset(DBRecord.fromBytes(recordData), offset, recordLen));
            offset += 4 + recordLen;
        }
        return records;
    }

    public List<DBRecord> getAllRecordsWithTombstones() {
        List<DBRecord> records = new ArrayList<>();
        for (RecordWithOffset r : getAllRecordsWithOffsets()) {
            records.add(r.record);
        }
        return records;
    }
}