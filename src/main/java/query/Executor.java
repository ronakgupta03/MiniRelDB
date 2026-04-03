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
            if (allRecords.isEmpty()) {
                System.out.println("(no records)");
                return;
            }
            System.out.printf("| %-6s | %-20s |%n", "ID", "NAME");
            System.out.println("|--------|----------------------|");
            for (DBRecord r : allRecords) {
                System.out.printf("| %-6d | %-20s |%n", r.getId(), r.getName());
            }
            System.out.println("(" + allRecords.size() + " record(s))");
        }
    }
}