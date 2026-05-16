package work.pollochang.app.sqlconsole.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * 智慧型 SQL 分隔器，支援 ; 與 / 分隔符號，並處理字串引號問題。
 */
public class SqlSplitter {

    public List<String> split(String input) {
        List<String> statements = new ArrayList<>();
        if (input == null || input.isBlank()) {
            return statements;
        }

        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        String[] lines = input.split("\\r?\\n", -1);
        for (String line : lines) {
            String trimmedLine = line.trim();

            // Oracle：行首且整行僅為 / 時結束一段 PL/SQL 區塊
            if (trimmedLine.equals("/") && !inString) {
                emitStatement(statements, sb);
                continue;
            }

            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);

                if (c == '\'' || c == '"') {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (stringChar == c) {
                        inString = false;
                    }
                }

                if (c == ';' && !inString) {
                    emitStatement(statements, sb);
                } else {
                    sb.append(c);
                }
            }

            // 僅在「仍在累積一條跨行語句」時保留換行，避免上一句結束後把 \n 帶進下一句開頭
            if (sb.length() > 0) {
                sb.append('\n');
            }
        }

        emitStatement(statements, sb);
        return statements;
    }

    /**
     * 將累積緩衝 trim 後寫入清單；若語句以分號結尾則移除（分號僅作批次分隔符）。
     */
    private static void emitStatement(List<String> statements, StringBuilder sb) {
        String raw = sb.toString().trim();
        sb.setLength(0);
        if (raw.isEmpty()) {
            return;
        }
        String sql = raw;
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (!sql.isEmpty()) {
            statements.add(sql);
        }
    }
}
