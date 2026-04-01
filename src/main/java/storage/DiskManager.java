package storage;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.io.IOException;

public class DiskManager {

    private RandomAccessFile dbFile;
    private String filePath;

    public DiskManager(String filePath) throws IOException {
        this.filePath = filePath;
        dbFile = new RandomAccessFile(filePath, "rw");
    }

    public void writePage(Page page) throws IOException {

        long offset = page.getPageId() * Page.PAGE_SIZE;

        dbFile.seek(offset);

        dbFile.write(page.getData());
    }

    public Page readPage(int pageId) throws IOException {

        long offset = pageId * Page.PAGE_SIZE;

        dbFile.seek(offset);

        byte[] buffer = new byte[Page.PAGE_SIZE];

        int bytesRead = dbFile.read(buffer);

        if (bytesRead == -1) {
            return new Page(pageId); // empty page
        }

        Page page = new Page(pageId);
        page.setData(buffer);

        return page;
    }

    public int getTotalPages() throws IOException {

        java.io.File file = new java.io.File(filePath);

        long fileSize = file.length();

        return (int) (fileSize / Page.PAGE_SIZE);
    }
}