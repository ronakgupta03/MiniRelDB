package query;

import java.util.ArrayList;
import java.util.List;

public class SelectQuery {
    private Integer id;
    private String baseTable;
    private String joinTable;
    private SelectQuery subquery;
    private List<String> columns = new ArrayList<>();
    private String filterColumn;
    private Object filterValue;

    public SelectQuery() {
    }

    public SelectQuery(Integer id) {
        this.id = id;
        this.columns.add("id");
        this.columns.add("name");
    }

    public void setFilter(String col, Object val) {
        this.filterColumn = col;
        this.filterValue = val;
    }

    public String getFilterColumn() { return filterColumn; }
    public Object getFilterValue() { return filterValue; }

    public List<String> getColumns() {
        return columns;
    }

    public void addColumn(String col) {
        columns.add(col);
    }

    public Integer getId() {
        return id;
    }

    public boolean hasIdFilter() {
        return id != null;
    }

    public String getBaseTable() {
        return baseTable;
    }

    public void setBaseTable(String baseTable) {
        this.baseTable = baseTable;
    }

    public String getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(String joinTable) {
        this.joinTable = joinTable;
    }

    public SelectQuery getSubquery() {
        return subquery;
    }

    public void setSubquery(SelectQuery subquery) {
        this.subquery = subquery;
    }
    
    public boolean matches(storage.DBRecord r) {
        return true;
    }
}