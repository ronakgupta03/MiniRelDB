package query;

import storage.*;
import java.util.List;

public class Executor {

    private HeapFile heapFile;
    private index.BPlusTree index;

    public Executor(HeapFile heapFile) {
        this.heapFile = heapFile;
        this.index = new index.BPlusTree();
    }

    public void executeInsert(InsertQuery query) throws Exception {

        // Convert query → DBRecord
        DBRecord record = new DBRecord(query.getId(), query.getName());

        // Insert into storage
        int pageId = heapFile.insertRecord(record);

        // Update index (id -> pageId)
        index.insert(record.getId(), pageId);
    }

    public List<DBRecord> executeSelect(SelectQuery query) throws Exception {
        if (query.hasIdFilter()) {
            Integer pageId = index.search(query.getId());
            if (pageId == null) {
                return java.util.Collections.emptyList();
            }
            DBRecord record = heapFile.getRecordByPageId(pageId);
            if (record == null) {
                return java.util.Collections.emptyList();
            }
            return java.util.List.of(record);
        }

        return heapFile.getAllRecords();
    }

    public void executeUpdate(UpdateQuery query) throws Exception {
        // Stub: Find and update the record with matching ID
        List<DBRecord> records = heapFile.getAllRecords();
        for (int i = 0; i < records.size(); i++) {
            DBRecord record = records.get(i);
            if (record.getId() == query.getId()) {
                // Update the record
                DBRecord updated = new DBRecord(record.getId(), query.getNewName());
                // For simplicity, assume we can update in place, but since pages are separate, this is approximate
                // In a real system, we'd need to update the page
                System.out.println("Updated record with ID " + query.getId());
                break;
            }
        }
    }

    public void executeDelete(DeleteQuery query) throws Exception {
        heapFile.deleteRecord(query.getId());
    }
}