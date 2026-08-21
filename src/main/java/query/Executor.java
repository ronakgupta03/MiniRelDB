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
            // Search through all records for the matching ID
            List<DBRecord> allRecords = heapFile.getAllRecords();
            for (DBRecord record : allRecords) {
                if (record.getId() == query.getId()) {
                    return java.util.List.of(record);
                }
            }
            return java.util.Collections.emptyList();
        }

        return heapFile.getAllRecords();
    }

    public void executeUpdate(UpdateQuery query) throws Exception {
        heapFile.updateRecord(query.getId(), query.getNewName());
    }

    public void executeDelete(DeleteQuery query) throws Exception {
        heapFile.deleteRecord(query.getId());
    }
}