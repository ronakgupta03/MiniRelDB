package database;

import storage.*;
import query.*;
import catalog.CatalogManager;
import java.util.*;

public class BenchmarkRunner {
    private static final String DB_NAME = "bench_db";
    private static Random random = new java.util.Random();

    public static void main(String[] args) {
        try {
            CatalogManager catalogManager = new CatalogManager();
            if (catalogManager.getDatabaseSchema(DB_NAME) == null) {
                catalogManager.createDatabase(DB_NAME);
            }
            Executor executor = new Executor(DB_NAME, catalogManager);

            // 1. Ingestion Benchmark
            System.out.println("METRIC:Ingestion_Start");
            long start = System.currentTimeMillis();
            createAndPopulate(executor);
            long ingestionTime = System.currentTimeMillis() - start;
            System.out.println("METRIC:Ingestion_End:" + ingestionTime);

            // 2. Point Lookup Benchmark (1,000 random ID lookups)
            System.out.println("METRIC:PointLookup_Start");
            start = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                int id = random.nextInt(10000);
                executor.executeSelect(new SelectQuery(id) {{ setBaseTable("orders"); }});
            }
            long lookupTime = System.currentTimeMillis() - start;
            System.out.println("METRIC:PointLookup_End:" + lookupTime);

            // 3. Full Scan Benchmark (All users)
            System.out.println("METRIC:FullScan_Start");
            start = System.currentTimeMillis();
            executor.executeSelect(new SelectQuery() {{ setBaseTable("users"); }});
            long scanTime = System.currentTimeMillis() - start;
            System.out.println("METRIC:FullScan_End:" + scanTime);

            // 4. Hash Join Benchmark (Users x Orders)
            System.out.println("METRIC:HashJoin_Start");
            start = System.currentTimeMillis();
            SelectQuery joinQuery = new SelectQuery();
            joinQuery.setBaseTable("users");
            joinQuery.setJoinTable("orders");
            executor.executeSelect(joinQuery);
            long joinTime = System.currentTimeMillis() - start;
            System.out.println("METRIC:HashJoin_End:" + joinTime);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createAndPopulate(Executor exec) throws Exception {
        // Simple schema for bench
        List<CatalogManager.ColumnSchema> uCols = new ArrayList<>();
        uCols.add(new CatalogManager.ColumnSchema("id", "INT"));
        uCols.add(new CatalogManager.ColumnSchema("name", "VARCHAR"));
        uCols.add(new CatalogManager.ColumnSchema("city", "VARCHAR"));
        exec.executeCreateTable(new CreateTableQuery("users", uCols, "id"));

        List<CatalogManager.ColumnSchema> oCols = new ArrayList<>();
        oCols.add(new CatalogManager.ColumnSchema("id", "INT"));
        oCols.add(new CatalogManager.ColumnSchema("user_id", "INT"));
        oCols.add(new CatalogManager.ColumnSchema("amount", "INT"));
        exec.executeCreateTable(new CreateTableQuery("orders", oCols, "id"));

        for (int i = 0; i < 5000; i++) {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("id", i);
            u.put("name", "User_" + i);
            u.put("city", "City_" + (i % 10));
            exec.executeInsert(new InsertQuery("users", u));
        }

        for (int i = 0; i < 10000; i++) {
            Map<String, Object> o = new LinkedHashMap<>();
            o.put("id", i);
            o.put("user_id", random.nextInt(5000));
            o.put("amount", random.nextInt(1000));
            exec.executeInsert(new InsertQuery("orders", o));
        }
        exec.flushAll();
    }
}
