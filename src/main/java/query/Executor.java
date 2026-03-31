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

        //INSERT
        if (query instanceof InsertQuery) {

            InsertQuery iq = (InsertQuery) query;

            DBRecord record = new DBRecord(iq.getId(), iq.getName());
            heapFile.insertRecord(record);
        }
    }
}