package storage;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import buffer.BufferManager;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;

public class DiskManager {

    private RandomAccessFile dbFile;
    private FileChannel fileChannel;
    private MappedByteBuffer mappedBuffer;
    private String filePath;
    private static BufferManager bufferPool = new BufferManager(100);
    private long currentSize;
    private int pageCount;

    public DiskManager(String dbName, String tableName) throws IOException {
        String dbDirPath = "data/" + dbName;
        File dbDir = new File(dbDirPath);
        if (!dbDir.exists()) dbDir.mkdirs();
        
        this.filePath = dbDirPath + "/" + tableName + ".db";
        dbFile = new RandomAccessFile(filePath, "rw");
        fileChannel = dbFile.getChannel();
        this.pageCount = (int) (dbFile.length() / Page.PAGE_SIZE);
        if (dbFile.length() > 0) {
            remap(dbFile.length());
        }
    }

    private void remap(long size) throws IOException {
        size = ((size + Page.PAGE_SIZE - 1) / Page.PAGE_SIZE) * Page.PAGE_SIZE;
        if (size > dbFile.length()) dbFile.setLength(size);
        mappedBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, size);
        currentSize = size;
    }

    public void writePage(Page page) throws IOException {
        if (page.getPageId() < 0) {
            throw new IllegalArgumentException("Negative pageId: " + page.getPageId());
        }
        page.updateChecksum();
        long offset = (long) page.getPageId() * Page.PAGE_SIZE;
        
        if (mappedBuffer == null || offset + Page.PAGE_SIZE > currentSize) {
            remap(Math.max(offset + Page.PAGE_SIZE * 2, Page.PAGE_SIZE)); 
        }

        mappedBuffer.position((int) offset);
        mappedBuffer.put(page.getData());
        
        if (page.getPageId() >= pageCount) {
            pageCount = page.getPageId() + 1;
        }

        String cacheKey = filePath + ":" + page.getPageId();
        bufferPool.putPage(cacheKey, page);
    }

    public Page readPage(int pageId) throws IOException {
        String cacheKey = filePath + ":" + pageId;
        Page cachedPage = bufferPool.getPage(cacheKey);
        if (cachedPage != null) return cachedPage;

        long fileSize = dbFile.length();
        long offset = (long) pageId * Page.PAGE_SIZE;
        
        if (pageId < 0 || offset >= fileSize) {
            return new Page(pageId, (byte) (pageId == 0 ? 2 : 1)); // 2 for Meta, 1 for Leaf
        }

        if (mappedBuffer == null || offset + Page.PAGE_SIZE > currentSize) {
            remap(fileSize);
        }

        byte[] buffer = new byte[Page.PAGE_SIZE];
        mappedBuffer.position((int) offset);
        mappedBuffer.get(buffer);

        Page page = new Page(pageId, (byte) 1);
        page.setData(buffer);
        
        if (!page.verifyChecksum() && page.getfreeSpaceOffset() > 0) {
            throw new RuntimeException("CRITICAL: Checksum verification failed for page " + pageId + ". Data corruption detected!");
        }

        bufferPool.putPage(cacheKey, page);
        return page;
    }

    public int getPageCount() throws IOException {
        return pageCount;
    }

    public boolean isNewFile() throws IOException {
        return dbFile.length() == 0;
    }

    public void flush() throws IOException {
        if (mappedBuffer != null) {
            mappedBuffer.force();
        }
    }

    public void close() throws IOException {
        if (dbFile != null) {
            dbFile.close();
        }
    }
}
