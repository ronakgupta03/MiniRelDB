package database;

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TableRenderer {
    private static final int MIN_COL_WIDTH = 4;
    private static final int DEFAULT_TERM_WIDTH = 120;

    public static void render(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("No results found.");
            return;
        }

        int termWidth = getTerminalWidth();
        
        Set<String> columnSet = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            columnSet.addAll(row.keySet());
        }
        List<String> columns = new ArrayList<>(columnSet);
        int numCols = columns.size();

        Map<String, Integer> idealWidths = new HashMap<>();
        for (String col : columns) {
            int maxW = col.length();
            for (Map<String, Object> row : rows) {
                Object valObj = row.get(col);
                String val = (valObj == null) ? "NULL" : valObj.toString();
                maxW = Math.max(maxW, val.length());
            }
            idealWidths.put(col, maxW);
        }

        int overhead = (numCols * 3) + 1;
        int availableWidth = Math.max(termWidth - overhead, numCols * MIN_COL_WIDTH);
        
        Map<String, Integer> actualWidths = new HashMap<>();
        int totalIdeal = idealWidths.values().stream().mapToInt(Integer::intValue).sum();

        if (totalIdeal <= availableWidth) {
            actualWidths.putAll(idealWidths);
        } else {
            for (String col : columns) {
                double weight = (double) idealWidths.get(col) / totalIdeal;
                int allocated = (int) Math.max(MIN_COL_WIDTH, Math.floor(weight * availableWidth));
                actualWidths.put(col, allocated);
            }
        }

        StringBuilder separator = new StringBuilder("+");
        for (String col : columns) {
            int w = actualWidths.get(col);
            for (int i = 0; i < w + 2; i++) separator.append("-");
            separator.append("+");
        }
        
        System.out.println(separator);
        
        Map<String, Object> headerRow = new LinkedHashMap<>();
        for (String c : columns) headerRow.put(c, c);
        renderWrappedRow(columns, actualWidths, headerRow);
        System.out.println(separator);

        for (Map<String, Object> row : rows) {
            renderWrappedRow(columns, actualWidths, row);
        }
        System.out.println(separator);
        System.out.println(rows.size() + " row(s) in set.\n");
    }

    private static void renderWrappedRow(List<String> columns, Map<String, Integer> widths, Map<String, Object> row) {
        Map<String, List<String>> cellLines = new HashMap<>();
        int maxLines = 1;

        for (String col : columns) {
            Object valObj = row.get(col);
            String val = (valObj == null) ? "NULL" : valObj.toString();
            List<String> wrapped = wrapText(val, widths.get(col));
            cellLines.put(col, wrapped);
            maxLines = Math.max(maxLines, wrapped.size());
        }

        for (int l = 0; l < maxLines; l++) {
            System.out.print("|");
            for (String col : columns) {
                List<String> lines = cellLines.get(col);
                String content = (l < lines.size()) ? lines.get(l) : "";
                System.out.printf(" %-" + widths.get(col) + "s |", content);
            }
            System.out.println();
        }
    }

    private static int getTerminalWidth() {
        try {
            String[] cmd = {"/bin/sh", "-c", "stty size < /dev/tty"};
            Process p = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) return Integer.parseInt(parts[1]);
            }
        } catch (Exception e) {
            // Fallback
        }
        return DEFAULT_TERM_WIDTH;
    }

    private static List<String> wrapText(String text, int width) {
        if (text == null || text.isEmpty()) return Collections.singletonList("");
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + width, text.length());
            result.add(text.substring(start, end));
            start = end;
        }
        return result;
    }
}