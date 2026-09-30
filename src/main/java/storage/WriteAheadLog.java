package storage;

import java.io.*;
import java.util.*;

public class WriteAheadLog {
    private String walPath;
    private DataOutputStream out;

    public WriteAheadLog(String dbName) throws IOException {
        String dbDirPath = "data/" + dbName;
        File dbDir = new File(dbDirPath);
        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }
        this.walPath = dbDirPath + "/wal.log";
        this.out = new DataOutputStream(new FileOutputStream(walPath, true));
    }

    public synchronized void append(byte opType, DBRecord record) throws IOException {
        out.writeByte(opType);
        byte[] data = record.toBytes();
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public synchronized void clear() throws IOException {
        out.close();
        new File(walPath).delete();
        this.out = new DataOutputStream(new FileOutputStream(walPath, true));
    }

    public synchronized List<LogEntry> recover() throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        File file = new File(walPath);
        if (!file.exists()) return entries;

        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            while (in.available() > 0) {
                byte opType = in.readByte();
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                DBRecord record = DBRecord.fromBytes(data);
                entries.add(new LogEntry(opType, record));
            }
        } catch (EOFException e) {
            // End of file reached
        }
        return entries;
    }

    public static class LogEntry {
        public byte opType;
        public DBRecord record;

        public LogEntry(byte opType, DBRecord record) {
            this.opType = opType;
            this.record = record;
        }
    }

    public synchronized void close() throws IOException {
        if (out != null) out.close();
    }
}
