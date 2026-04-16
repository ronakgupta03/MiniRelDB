package buffer;

import storage.Page;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class BufferManager {
    private final int capacity;
    private final ConcurrentHashMap<String, Page> cache = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<String> lruQueue = new LinkedBlockingQueue<>();
    private final ReentrantLock evictionLock = new ReentrantLock();

    public BufferManager(int capacity) {
        this.capacity = capacity;
    }

    public Page getPage(String key) {
        return cache.get(key);
    }

    public void putPage(String key, Page page) {
        if (!cache.containsKey(key) && cache.size() >= capacity) {
            evictOldest();
        }
        cache.put(key, page);
        lruQueue.offer(key);
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
        lruQueue.clear();
    }

    private void evictOldest() {
        evictionLock.lock();
        try {
            String evicted = lruQueue.poll();
            if (evicted != null) {
                cache.remove(evicted);
            }
        } finally {
            evictionLock.unlock();
        }
    }
}
