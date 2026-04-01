package query;

import storage.*;
import java.util.List;

public class Executor {

    private HeapFile heapFile;
    private DiskManager diskManager;

    public Executor(HeapFile heapFile, DiskManager diskManager) {
        this.heapFile = heapFile;
        this.diskManager = diskManager;
    }

    public void execute(Object query) throws Exception {

        // INSERT
        if (query instanceof InsertQuery) {

            InsertQuery iq = (InsertQuery) query;

            DBRecord record = new DBRecord(iq.getId(), iq.getName());
            heapFile.insertRecord(record);
        }

        // SELECT
        else if (query instanceof SelectQuery) {

            System.out.println("---- ALL RECORDS ----");

            List<DBRecord> allRecords = new java.util.ArrayList<>();

            int totalPages = diskManager.getTotalPages();

            int currentPageId = heapFile.getCurrentPage().getPageId();

            // 🔹 Read pages from disk
            for (int i = 0; i < totalPages; i++) {

                // ❗ Skip current page if not flushed yet
                if (i == currentPageId) continue;

                Page page = diskManager.readPage(i);
                allRecords.addAll(page.getAllRecords());
            }

            // 🔹 Add current page (memory)
            Page currentPage = heapFile.getCurrentPage();
            allRecords.addAll(currentPage.getAllRecords());

            // 🔥 SORT by ID
            allRecords.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

            // 🔹 Print
            for (DBRecord r : allRecords) {
                System.out.println(r);
            }
        }
    }
}