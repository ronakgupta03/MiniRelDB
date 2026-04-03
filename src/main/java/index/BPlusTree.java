package index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Simple B+ Tree for integer key -> integer pageId mapping.
 */
public class BPlusTree {
    private static final int ORDER = 4; // max number of child pointers in internal nodes

    private Node root;

    public BPlusTree() {
        root = new LeafNode();
    }

    public void insert(int key, int value) {
        Split split = root.insert(key, value);
        if (split != null) {
            InternalNode newRoot = new InternalNode();
            newRoot.keys.add(split.key);
            newRoot.children.add(split.left);
            newRoot.children.add(split.right);
            root = newRoot;
        }
    }

    public Integer search(int key) {
        return root.search(key);
    }

    public List<Integer> rangeSearch(int low, int high) {
        List<Integer> results = new ArrayList<>();
        LeafNode node = root.getFirstLeaf();
        while (node != null) {
            for (int i = 0; i < node.keys.size(); i++) {
                int k = node.keys.get(i);
                if (k >= low && k <= high) {
                    results.add(node.values.get(i));
                }
            }
            node = node.next;
        }
        return results;
    }

    private abstract class Node {
        List<Integer> keys = new ArrayList<>();

        abstract Split insert(int key, int value);
        abstract Integer search(int key);
        abstract LeafNode getFirstLeaf();
    }

    private class InternalNode extends Node {
        List<Node> children = new ArrayList<>();

        @Override
        Split insert(int key, int value) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) {
                idx++;
            } else {
                idx = -idx - 1;
            }

            Split split = children.get(idx).insert(key, value);
            if (split == null) {
                return null;
            }

            int pos = Collections.binarySearch(keys, split.key);
            if (pos >= 0) {
                pos++;
            } else {
                pos = -pos - 1;
            }

            keys.add(pos, split.key);
            children.set(pos, split.left);
            children.add(pos + 1, split.right);

            if (keys.size() >= ORDER) {
                int mid = keys.size() / 2;
                InternalNode newNode = new InternalNode();

                newNode.keys.addAll(keys.subList(mid + 1, keys.size()));
                newNode.children.addAll(children.subList(mid + 1, children.size()));

                int promoteKey = keys.get(mid);

                keys.subList(mid, keys.size()).clear();
                children.subList(mid + 1, children.size()).clear();

                return new Split(promoteKey, this, newNode);
            }

            return null;
        }

        @Override
        Integer search(int key) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) {
                idx++;
            } else {
                idx = -idx - 1;
            }
            return children.get(idx).search(key);
        }

        @Override
        LeafNode getFirstLeaf() {
            return children.get(0).getFirstLeaf();
        }
    }

    private class LeafNode extends Node {
        List<Integer> values = new ArrayList<>();
        LeafNode next;

        @Override
        Split insert(int key, int value) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) {
                values.set(idx, value);
                return null;
            }
            idx = -idx - 1;

            keys.add(idx, key);
            values.add(idx, value);

            if (keys.size() >= ORDER) {
                int mid = keys.size() / 2;
                LeafNode newLeaf = new LeafNode();
                newLeaf.keys.addAll(keys.subList(mid, keys.size()));
                newLeaf.values.addAll(values.subList(mid, values.size()));
                newLeaf.next = this.next;
                this.next = newLeaf;

                keys.subList(mid, keys.size()).clear();
                values.subList(mid, values.size()).clear();

                return new Split(newLeaf.keys.get(0), this, newLeaf);
            }

            return null;
        }

        @Override
        Integer search(int key) {
            int idx = Collections.binarySearch(keys, key);
            if (idx >= 0) {
                return values.get(idx);
            }
            return null;
        }

        @Override
        LeafNode getFirstLeaf() {
            return this;
        }
    }

    private static class Split {
        int key;
        Node left;
        Node right;

        Split(int key, Node left, Node right) {
            this.key = key;
            this.left = left;
            this.right = right;
        }
    }
}
