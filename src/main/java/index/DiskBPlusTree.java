package index;

import storage.DiskManager;
import storage.Page;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Disk-persistent B+ Tree mapping int key -> int value (pageId).
 * Nodes are stored as Pages via DiskManager.
 */
public class DiskBPlusTree {
    private DiskManager diskManager;
    private int rootPageId;

    // Header sizes: pageType(1) + freeSpace(4) + parent(4) + next(4) + checksum(4) = 17 bytes
    private static final int HEADER_SIZE = 17;
    private static final int KEY_SIZE = 4;
    private static final int VAL_SIZE = 4;
    private static final int ORDER = (Page.PAGE_SIZE - HEADER_SIZE) / (KEY_SIZE + VAL_SIZE);

    public DiskBPlusTree(DiskManager diskManager) throws IOException {
        this.diskManager = diskManager;
        if (diskManager.isNewFile()) {
            // Page 0: Metadata
            Page meta = new Page(0, (byte) 2);
            // Page 1: Initial Leaf Root
            Page root = new Page(1, (byte) 1);
            
            this.rootPageId = 1;
            ByteBuffer.wrap(meta.getData(), 17, 4).putInt(rootPageId);
            
            diskManager.writePage(meta);
            diskManager.writePage(root);
        } else {
            Page meta = diskManager.readPage(0);
            this.rootPageId = ByteBuffer.wrap(meta.getData(), 17, 4).getInt();
        }
    }

    private void updateRoot(int newRootId) throws IOException {
        this.rootPageId = newRootId;
        Page meta = diskManager.readPage(0);
        ByteBuffer.wrap(meta.getData(), 17, 4).putInt(newRootId);
        diskManager.writePage(meta);
    }

    public void insert(int key, int value) throws IOException {
        Page rootPage = diskManager.readPage(rootPageId);
        SplitResult split = insertRecursive(rootPage, key, value);
        if (split != null) {
            int newRootId = diskManager.getPageCount();
            Page newRoot = new Page(newRootId, (byte) 0); // Internal node
            
            // Insert split key and the two children
            byte[] entry1 = new byte[8];
            ByteBuffer.wrap(entry1).putInt(split.key).putInt(split.leftId);
            newRoot.insertRecord(entry1);
            
            byte[] entry2 = new byte[8];
            ByteBuffer.wrap(entry2).putInt(Integer.MAX_VALUE).putInt(split.rightId);
            newRoot.insertRecord(entry2);

            diskManager.writePage(newRoot);
            updateRoot(newRootId);
        }
    }

    private SplitResult insertRecursive(Page node, int key, int value) throws IOException {
        if (node.getPageType() == 1) { // Leaf
            return insertLeaf(node, key, value);
        } else { // Internal
            int childId = findChild(node, key);
            Page child = diskManager.readPage(childId);
            SplitResult split = insertRecursive(child, key, value);
            if (split == null) return null;
            return insertInternal(node, split.key, split.rightId);
        }
    }

    private int findChild(Page node, int key) {
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        for (int i = 0; i < freeSpace; i += 8) {
            int k = ByteBuffer.wrap(data, HEADER_SIZE + i, 4).getInt();
            if (key <= k) {
                return ByteBuffer.wrap(data, HEADER_SIZE + i + 4, 4).getInt();
            }
        }
        // Fallback to last pointer if exists, or error
        if (freeSpace >= 8) {
            return ByteBuffer.wrap(data, HEADER_SIZE + freeSpace - 4, 4).getInt();
        }
        return -1;
    }

    private SplitResult insertLeaf(Page node, int key, int value) throws IOException {
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        
        int pos = 0;
        for (; pos < freeSpace; pos += 8) {
            int k = ByteBuffer.wrap(data, HEADER_SIZE + pos, 4).getInt();
            if (key < k) break;
            if (key == k) {
                ByteBuffer.wrap(data, HEADER_SIZE + pos + 4, 4).putInt(value);
                diskManager.writePage(node);
                return null;
            }
        }

        if (HEADER_SIZE + freeSpace + 8 <= Page.PAGE_SIZE) {
            System.arraycopy(data, HEADER_SIZE + pos, data, HEADER_SIZE + pos + 8, freeSpace - pos);
            ByteBuffer.wrap(data, HEADER_SIZE + pos, 8).putInt(key).putInt(value);
            node.setFreeSpaceOffset(freeSpace + 8);
            diskManager.writePage(node);
            return null;
        }

        // Split Leaf
        int midCount = (ORDER / 2) * 8;
        int nextId = diskManager.getPageCount();
        Page nextLeaf = new Page(nextId, (byte) 1);
        
        System.arraycopy(data, HEADER_SIZE + midCount, nextLeaf.getData(), HEADER_SIZE, freeSpace - midCount);
        nextLeaf.setFreeSpaceOffset(freeSpace - midCount);
        node.setFreeSpaceOffset(midCount);
        
        nextLeaf.setNextPageId(node.getNextPageId());
        node.setNextPageId(nextId);
        
        if (key < ByteBuffer.wrap(nextLeaf.getData(), HEADER_SIZE, 4).getInt()) insertLeaf(node, key, value);
        else insertLeaf(nextLeaf, key, value);

        diskManager.writePage(node);
        diskManager.writePage(nextLeaf);
        
        return new SplitResult(ByteBuffer.wrap(nextLeaf.getData(), HEADER_SIZE, 4).getInt(), node.getPageId(), nextId);
    }

    private SplitResult insertInternal(Page node, int key, int rightChildId) throws IOException {
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        
        int pos = 0;
        for (; pos < freeSpace; pos += 8) {
            int k = ByteBuffer.wrap(data, HEADER_SIZE + pos, 4).getInt();
            if (key < k) break;
        }

        if (HEADER_SIZE + freeSpace + 8 <= Page.PAGE_SIZE) {
            System.arraycopy(data, HEADER_SIZE + pos, data, HEADER_SIZE + pos + 8, freeSpace - pos);
            ByteBuffer.wrap(data, HEADER_SIZE + pos, 8).putInt(key).putInt(rightChildId);
            node.setFreeSpaceOffset(freeSpace + 8);
            diskManager.writePage(node);
            return null;
        }

        // Split Internal
        int midCount = (ORDER / 2) * 8;
        int nextId = diskManager.getPageCount();
        Page nextInternal = new Page(nextId, (byte) 0);
        
        System.arraycopy(data, HEADER_SIZE + midCount, nextInternal.getData(), HEADER_SIZE, freeSpace - midCount);
        nextInternal.setFreeSpaceOffset(freeSpace - midCount);
        node.setFreeSpaceOffset(midCount);
        
        if (key < ByteBuffer.wrap(nextInternal.getData(), HEADER_SIZE, 4).getInt()) insertInternal(node, key, rightChildId);
        else insertInternal(nextInternal, key, rightChildId);

        diskManager.writePage(node);
        diskManager.writePage(nextInternal);

        return new SplitResult(ByteBuffer.wrap(nextInternal.getData(), HEADER_SIZE, 4).getInt(), node.getPageId(), nextId);
    }

    public Integer search(int key) throws IOException {
        Page node = diskManager.readPage(rootPageId);
        while (node.getPageType() == 0) { // Internal
            int childId = findChild(node, key);
            node = diskManager.readPage(childId);
        }
        
        // Search in leaf
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        for (int i = 0; i < freeSpace; i += 8) {
            int k = ByteBuffer.wrap(data, HEADER_SIZE + i, 4).getInt();
            if (k == key) {
                return ByteBuffer.wrap(data, HEADER_SIZE + i + 4, 4).getInt();
            }
        }
        return null;
    }

    public void delete(int key) throws IOException {
        Page rootPage = diskManager.readPage(rootPageId);
        deleteRecursive(rootPage, key);
    }

    private boolean deleteRecursive(Page node, int key) throws IOException {
        if (node.getPageType() == 1) { // Leaf
            return deleteFromLeaf(node, key);
        } else { // Internal
            int childId = findChild(node, key);
            if (childId == -1) return false;
            Page child = diskManager.readPage(childId);
            boolean deleted = deleteRecursive(child, key);
            if (deleted) {
                // If child is now empty, remove its pointer from this node
                byte[] childData = child.getData();
                int childFreeSpace = child.getfreeSpaceOffset();
                if (childFreeSpace == 0 && child.getPageType() == 1) {
                    removeFromInternal(node, childId);
                }
            }
            return deleted;
        }
    }

    private boolean deleteFromLeaf(Page node, int key) throws IOException {
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        for (int i = 0; i < freeSpace; i += 8) {
            int k = java.nio.ByteBuffer.wrap(data, HEADER_SIZE + i, 4).getInt();
            if (k == key) {
                // Compact: shift remaining entries left to cover this one
                int remaining = freeSpace - i - 8;
                System.arraycopy(data, HEADER_SIZE + i + 8, data, HEADER_SIZE + i, remaining);
                node.setFreeSpaceOffset(freeSpace - 8);
                diskManager.writePage(node);
                return true;
            }
        }
        return false;
    }

    private void removeFromInternal(Page node, int childPageId) throws IOException {
        byte[] data = node.getData();
        int freeSpace = node.getfreeSpaceOffset();
        for (int i = 0; i < freeSpace; i += 8) {
            int cId = java.nio.ByteBuffer.wrap(data, HEADER_SIZE + i + 4, 4).getInt();
            if (cId == childPageId) {
                int remaining = freeSpace - i - 8;
                System.arraycopy(data, HEADER_SIZE + i + 8, data, HEADER_SIZE + i, remaining);
                node.setFreeSpaceOffset(freeSpace - 8);
                diskManager.writePage(node);
                return;
            }
        }
    }

    /**
     * Bulk insert from sorted key-value entries.
     * Used by Compaction to build new dense index efficiently.
     */
    public void bulkInsert(List<int[]> entries) throws IOException {
        if (entries.isEmpty()) return;

        // Create leaf pages sequentially
        int firstLeafPageId = -1;
        int lastLeafPageId = -1;
        int maxEntriesPerPage = ORDER;

        for (int i = 0; i < entries.size(); i += maxEntriesPerPage) {
            Page leaf = new Page(diskManager.getPageCount(), (byte) 1);
            int end = Math.min(i + maxEntriesPerPage, entries.size());
            for (int j = i; j < end; j++) {
                byte[] entry = new byte[8];
                java.nio.ByteBuffer.wrap(entry).putInt(entries.get(j)[0]).putInt(entries.get(j)[1]);
                leaf.insertRecord(entry);
            }
            if (firstLeafPageId == -1) firstLeafPageId = leaf.getPageId();
            if (lastLeafPageId != -1) {
                Page prevLeaf = diskManager.readPage(lastLeafPageId);
                prevLeaf.setNextPageId(leaf.getPageId());
                diskManager.writePage(prevLeaf);
            }
            lastLeafPageId = leaf.getPageId();
            diskManager.writePage(leaf);
        }

        updateRoot(firstLeafPageId);
    }

    public void flush() throws IOException {
        diskManager.flush();
    }

    private static class SplitResult {
        int key;
        int leftId;
        int rightId;

        SplitResult(int key, int leftId, int rightId) {
            this.key = key;
            this.leftId = leftId;
            this.rightId = rightId;
        }
    }
}
