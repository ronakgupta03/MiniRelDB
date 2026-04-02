package storage;

import java.io.RandomAccessFile;
import java.io.IOException;

public class DiskManager {

    private RandomAccessFile dbFile;

    public DiskManager(String filePath) throws IOException {
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

        dbFile.read(buffer);

        Page page = new Page(pageId);

        page.setData(buffer);

        return page;
    }

    public int getPageCount() throws IOException {
        long length = dbFile.length();
        return (int) (length / Page.PAGE_SIZE);
    }
}