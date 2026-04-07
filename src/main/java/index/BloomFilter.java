package index;

import java.util.BitSet;

public class BloomFilter {
    private BitSet bits;
    private int size;

    public BloomFilter(int size) {
        this.size = size;
        this.bits = new BitSet(size);
    }

    public void add(int id) {
        bits.set(hash1(id));
        bits.set(hash2(id));
    }

    public boolean mightContain(int id) {
        return bits.get(hash1(id)) && bits.get(hash2(id));
    }

    private int hash1(int id) {
        return Math.abs(Integer.hashCode(id)) % size;
    }

    private int hash2(int id) {
        // Use a different seed/strategy for the second hash
        return Math.abs((Integer.hashCode(id) ^ 0x55555555)) % size;
    }
}
