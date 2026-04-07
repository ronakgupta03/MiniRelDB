package buffer;

import storage.Page;
import java.util.LinkedHashMap;
import java.util.Map;

public class BufferManager {
    private final int capacity;
    private final Map<String, Page> cache;

    public BufferManager(int capacity) {
        this.capacity = capacity;
        // LinkedHashMap with accessOrder=true for LRU behavior
        this.cache = new LinkedHashMap<String, Page>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Page> eldest) {
                return size() > BufferManager.this.capacity;
            }
        };
    }

    public Page getPage(String key) {
        return cache.get(key);
    }

    public void putPage(String key, Page page) {
        cache.put(key, page);
    }

    public void evict(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}
