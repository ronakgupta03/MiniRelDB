package storage;

import java.util.ArrayList;
import java.util.List;
import java.nio.ByteBuffer;

public class Page {
    public static final int PAGE_SIZE = 4096;

    private int pageId;
    private byte[] data;
    private int freeSpaceOffset;

    public Page(int pageId) {
        this.pageId = pageId;
        this.data = new byte[PAGE_SIZE];
        this.freeSpaceOffset = 0;
    }

    public int getPageId() {
        return pageId;
    }

    public byte[] getData() {
        return data;
    }

    // public void setData(byte[] data) {
    //     if (data.length != PAGE_SIZE) {
    //         throw new IllegalArgumentException("Data must be exactly " + PAGE_SIZE + " bytes");
    //     }
    //     this.data = data;
    // }

    public void setFreeSpaceOffset(int offset) {
        this.freeSpaceOffset = offset;
    }

    public void setData(byte[] data) {
        this.data = data;

        // 🔥 Read freeSpaceOffset from first 4 bytes
        this.freeSpaceOffset = ByteBuffer.wrap(data, 0, 4).getInt();
    }

    public int getfreeSpaceOffset() {
        return freeSpaceOffset;
    }

    public boolean insertRecord(byte[] recordBytes) {

        // First 4 bytes reserved for freeSpaceOffset
        if (freeSpaceOffset + recordBytes.length + 4 > PAGE_SIZE) {
            return false;
        }

        // Write record after header (offset starts after 4 bytes)
        int writePos = 4 + freeSpaceOffset;

        System.arraycopy(recordBytes, 0, data, writePos, recordBytes.length);

        freeSpaceOffset += recordBytes.length;

        // Store freeSpaceOffset at beginning of page
        ByteBuffer.wrap(data, 0, 4).putInt(freeSpaceOffset);

        return true;
    }

    public List<DBRecord> getAllRecords() {

        List<DBRecord> records = new ArrayList<>();

        int offset = 4; // skip header
        int end = 4 + freeSpaceOffset;

        while (offset + 8 <= end) { // ensure id + length exist

            // Read id (4 bytes)
            int id = ByteBuffer.wrap(data, offset, 4).getInt();
            offset += 4;

            // Read name length (4 bytes)
            int nameLength = ByteBuffer.wrap(data, offset, 4).getInt();
            offset += 4;

            // SAFETY CHECK: Ensure name length is valid
            if (nameLength <= 0 || offset + nameLength > end) {
                break;
            }

            // Read name bytes
            byte[] nameBytes = new byte[nameLength];
            System.arraycopy(data, offset, nameBytes, 0, nameLength);
            offset += nameLength;

            String name = new String(nameBytes);

            records.add(new DBRecord(id, name));
        }

        return records;
    }
}