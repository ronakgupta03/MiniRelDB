package database;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.*;

public class Database {

    private ObjectMapper mapper = new ObjectMapper();

    // INSERT RECORD
    public void insert(String table, Map<String, Object> record)
            throws Exception {

        File file = new File(table + ".json");

        List<Map<String, Object>> data;

        if (file.exists()) {
            data = mapper.readValue(
                    file,
                    List.class
            );
        } else {
            data = new ArrayList<>();
        }

        data.add(record);

        mapper.writerWithDefaultPrettyPrinter()
              .writeValue(file, data);
    }

    // DISPLAY RECORDS
    public void display(String table)
            throws Exception {

        File file = new File(table + ".json");

        if (!file.exists()) {
            System.out.println("Table not found");
            return;
        }

        List<Map<String, Object>> data =
                mapper.readValue(file, List.class);

        for (Map<String, Object> row : data) {
            System.out.println(row);
        }
    }
}