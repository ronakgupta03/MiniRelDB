# Phase 4: Query Engine Operators (Joins & Subqueries) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement an operator-based query engine to support Joins (Index-Nested Loop) and Subqueries.

**Architecture:** 
1. **Nodes**: `ScanNode`, `FilterNode`, `JoinNode`, `ProjectNode`.
2. **Join**: Implement `Index-Nested Loop Join`. For each row in the outer table, perform an index lookup on the inner table.
3. **Subquery**: Support simple non-correlated subqueries in `WHERE` clauses.

**Tech Stack:** Java (Standard Library).

---

### Task 1: Implement Query Operator Nodes

**Files:**
- Create: `src/main/java/query/PlanNode.java` (Interface and Implementations)

- [ ] **Step 1: Define the `PlanNode` interface**
Implement `next()` to return the next `DBRecord` (Volcano model).

- [ ] **Step 2: Implement `ScanNode`, `FilterNode`, and `ProjectNode`**
Implement basic table scanning, filtering, and column projection.

### Task 2: Implement `JoinNode` (Index-Nested Loop)

**Files:**
- Modify: `src/main/java/query/PlanNode.java`

- [ ] **Step 1: Implement the `JoinNode` class**
Take an outer node and an inner table name. For each outer record, use the inner table's index for matching.

### Task 3: Update `sqlParser` and `Executor` for Joins

**Files:**
- Modify: `src/main/java/query/sqlParser.java`
- Modify: `src/main/java/query/Executor.java`

- [ ] **Step 1: Update `sqlParser` to handle `JOIN` syntax**
Parse `SELECT ... FROM table1 JOIN table2 ON ...`.

- [ ] **Step 2: Update `Executor` to build and execute the Plan Tree**
Convert parsed queries into a tree of `PlanNode`s and execute them.
