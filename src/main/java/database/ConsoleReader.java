package database;

import java.io.*;
import java.util.*;

/**
 * A "from-scratch" console reader that supports:
 * - Up/Down for history
 * - Left/Right for cursor movement
 * - Backspace/Delete
 * - Home/End
 */
public class ConsoleReader {
    private final List<String> history;
    private int historyIndex = -1;
    private String currentBuffer = "";
    private int cursorPosition = 0;
    private final PrintStream out = System.out;
    private final InputStream in = System.in;

    public ConsoleReader(List<String> history) {
        this.history = history;
    }

    public String readLine(String prompt) throws IOException {
        setRawMode(true);
        out.print(prompt);
        out.flush();

        StringBuilder line = new StringBuilder();
        cursorPosition = 0;
        historyIndex = history.size();

        try {
            while (true) {
                int c = in.read();
                if (c == -1 || c == 4) { // EOF or Ctrl+D
                    return null;
                }
                if (c == 13 || c == 10) { // Enter
                    String result = line.toString();
                    out.println();
                    return result;
                }
                if (c == 27) { // Escape sequence
                    handleEscapeSequence(line);
                } else if (c == 127 || c == 8) { // Backspace
                    handleBackspace(line);
                } else if (c >= 32 && c <= 126) { // Printable characters
                    line.insert(cursorPosition, (char) c);
                    cursorPosition++;
                    refreshLine(prompt, line);
                }
            }
        } finally {
            setRawMode(false);
        }
    }

    private void handleEscapeSequence(StringBuilder line) throws IOException {
        int next = in.read();
        if (next == '[') {
            int code = in.read();
            switch (code) {
                case 'A': // Up
                    navigateHistory(line, -1);
                    break;
                case 'B': // Down
                    navigateHistory(line, 1);
                    break;
                case 'C': // Right
                    if (cursorPosition < line.length()) {
                        cursorPosition++;
                        out.print("\033[C");
                    }
                    break;
                case 'D': // Left
                    if (cursorPosition > 0) {
                        cursorPosition--;
                        out.print("\033[D");
                    }
                    break;
                case 'H': // Home
                    cursorPosition = 0;
                    refreshLine("", line); // Use empty prompt to just move cursor
                    break;
                case 'F': // End
                    cursorPosition = line.length();
                    refreshLine("", line);
                    break;
            }
        }
    }

    private void navigateHistory(StringBuilder line, int direction) {
        int newIndex = historyIndex + direction;
        if (newIndex >= 0 && newIndex <= history.size()) {
            if (historyIndex == history.size()) {
                currentBuffer = line.toString();
            }
            historyIndex = newIndex;
            line.setLength(0);
            if (historyIndex == history.size()) {
                line.append(currentBuffer);
            } else {
                line.append(history.get(historyIndex));
            }
            cursorPosition = line.length();
            refreshLine("", line);
        }
    }

    private void handleBackspace(StringBuilder line) {
        if (cursorPosition > 0) {
            line.deleteCharAt(cursorPosition - 1);
            cursorPosition--;
            refreshLine("", line);
        }
    }

    private void refreshLine(String prompt, StringBuilder line) {
        // Move to start of line, clear to end, reprint
        out.print("\r\033[K" + prompt + line.toString());
        // Move cursor back to correct position
        if (cursorPosition < line.length()) {
            out.print("\033[" + (line.length() - cursorPosition) + "D");
        }
        out.flush();
    }

    private void setRawMode(boolean raw) {
        try {
            String[] cmd = {"/bin/sh", "-c", raw ? "stty raw -echo < /dev/tty" : "stty cooked echo < /dev/tty"};
            Runtime.getRuntime().exec(cmd).waitFor();
        } catch (Exception e) {
            // Ignore if stty fails (e.g. not a TTY)
        }
    }
}
