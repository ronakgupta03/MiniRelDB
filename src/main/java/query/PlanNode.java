package query;

import storage.DBRecord;
import java.util.*;

public interface PlanNode {
    void open() throws Exception;
    DBRecord next() throws Exception;
    void close() throws Exception;

    class ScanNode implements PlanNode {
        private String tableName;
        private Executor executor;
        private Iterator<DBRecord> iterator;

        public ScanNode(String tableName, Executor executor) {
            this.tableName = tableName;
            this.executor = executor;
        }

        @Override
        public void open() throws Exception {
            // Use the executor's logic to get all records (unified view)
            List<DBRecord> records = executor.getTableRecords(tableName);
            this.iterator = records.iterator();
        }

        @Override
        public DBRecord next() {
            return (iterator != null && iterator.hasNext()) ? iterator.next() : null;
        }

        @Override
        public void close() {}
    }

    class FilterNode implements PlanNode {
        private PlanNode child;
        private String conditionColumn;
        private String conditionValue;
        private PlanNode subqueryChild;
        private Set<Integer> allowedIds = new HashSet<>();

        public FilterNode(PlanNode child, String col, String val) {
            this.child = child;
            this.conditionColumn = col;
            this.conditionValue = val;
        }

        public FilterNode(PlanNode child, String col, PlanNode subquery) {
            this.child = child;
            this.conditionColumn = col;
            this.subqueryChild = subquery;
        }

        @Override
        public void open() throws Exception {
            child.open();
            if (subqueryChild != null) {
                subqueryChild.open();
                DBRecord r;
                while ((r = subqueryChild.next()) != null) {
                    allowedIds.add(r.getId());
                }
                subqueryChild.close();
            }
        }

        @Override
        public DBRecord next() throws Exception {
            DBRecord record;
            while ((record = child.next()) != null) {
                if (subqueryChild != null) {
                    if (allowedIds.contains(record.getId())) return record;
                    continue;
                }
                
                if (conditionColumn.equalsIgnoreCase("id")) {
                    if (String.valueOf(record.getId()).equals(conditionValue)) return record;
                } else if (conditionColumn.equalsIgnoreCase("name")) {
                    if (record.getName().equals(conditionValue)) return record;
                }
            }
            return null;
        }

        @Override
        public void close() throws Exception {
            child.close();
        }
    }

    class JoinNode implements PlanNode {
        private PlanNode leftChild;
        private String rightTableName;
        private Executor executor;
        private DBRecord currentLeft;
        private Iterator<DBRecord> rightMatches;

        public JoinNode(PlanNode left, String rightTable, Executor executor) {
            this.leftChild = left;
            this.rightTableName = rightTable;
            this.executor = executor;
        }

        @Override
        public void open() throws Exception {
            leftChild.open();
        }

        @Override
        public DBRecord next() throws Exception {
            while (true) {
                if (rightMatches == null || !rightMatches.hasNext()) {
                    currentLeft = leftChild.next();
                    if (currentLeft == null) return null;
                    
                    // Index-Nested Loop Join: Search right table by ID from left row
                    // (Assuming left.id joins with right.id for now)
                    List<DBRecord> matches = executor.getTableRecordsById(rightTableName, currentLeft.getId());
                    rightMatches = matches.iterator();
                }
                
                if (rightMatches.hasNext()) {
                    DBRecord right = rightMatches.next();
                    Map<String, Object> joinedVals = new LinkedHashMap<>(currentLeft.getValues());
                    for (Map.Entry<String, Object> entry : right.getValues().entrySet()) {
                        joinedVals.put(rightTableName + "." + entry.getKey(), entry.getValue());
                    }
                    return new DBRecord(currentLeft.getId(), joinedVals);
                }
            }
        }

        @Override
        public void close() throws Exception {
            leftChild.close();
        }
    }

    class HashJoinNode implements PlanNode {
        private PlanNode leftChild;
        private String rightTableName;
        private Executor executor;
        private Map<Integer, List<DBRecord>> hashTable = new HashMap<>();
        private DBRecord currentLeft;
        private Iterator<DBRecord> matchIterator;

        public HashJoinNode(PlanNode left, String rightTable, Executor executor) {
            this.leftChild = left;
            this.rightTableName = rightTable;
            this.executor = executor;
        }

        @Override
        public void open() throws Exception {
            leftChild.open();
            // Build Phase: Read entire right table into hash map
            List<DBRecord> rightRecords = executor.getTableRecords(rightTableName);
            for (DBRecord r : rightRecords) {
                hashTable.computeIfAbsent(r.getId(), k -> new ArrayList<>()).add(r);
            }
        }

        @Override
        public DBRecord next() throws Exception {
            while (true) {
                if (matchIterator == null || !matchIterator.hasNext()) {
                    currentLeft = leftChild.next();
                    if (currentLeft == null) return null;
                    
                    List<DBRecord> matches = hashTable.get(currentLeft.getId());
                    if (matches != null) {
                        matchIterator = matches.iterator();
                    } else {
                        continue;
                    }
                }

                if (matchIterator.hasNext()) {
                    DBRecord right = matchIterator.next();
                    Map<String, Object> joinedVals = new LinkedHashMap<>(currentLeft.getValues());
                    // Prefix right columns to avoid collision
                    for (Map.Entry<String, Object> entry : right.getValues().entrySet()) {
                        joinedVals.put(rightTableName + "." + entry.getKey(), entry.getValue());
                    }
                    return new DBRecord(currentLeft.getId(), joinedVals);
                }
            }
        }

        @Override
        public void close() throws Exception {
            leftChild.close();
            hashTable.clear();
        }
    }

    class SubqueryNode implements PlanNode {
        private SelectQuery subquery;
        private Executor executor;
        private List<DBRecord> results;
        private Iterator<DBRecord> iterator;

        public SubqueryNode(SelectQuery subquery, Executor executor) {
            this.subquery = subquery;
            this.executor = executor;
        }

        @Override
        public void open() throws Exception {
            this.results = executor.executeSelect(subquery);
            this.iterator = results.iterator();
        }

        @Override
        public DBRecord next() throws Exception {
            return (iterator != null && iterator.hasNext()) ? iterator.next() : null;
        }

        @Override
        public void close() throws Exception {}
    }
}
