package work.pollochang.app.sqlconsole.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SqlSplitterTest {

    private final SqlSplitter splitter = new SqlSplitter();

    @Test
    void splitsConsecutiveStatementsWithoutLeadingNewlineOnSecond() {
        List<String> out = splitter.split("SELECT 1;\nSELECT 2;");
        assertEquals(List.of("SELECT 1", "SELECT 2"), out);
    }

    @Test
    void preservesNewlinesInsideMultiLineStatement() {
        List<String> out = splitter.split("CREATE TABLE t (\nid int\n);\nSELECT 1;");
        assertEquals(2, out.size());
        assertEquals("CREATE TABLE t (\nid int\n)", out.get(0));
        assertEquals("SELECT 1", out.get(1));
    }

    @Test
    void ignoresSemicolonInsideQuotedString() {
        List<String> out = splitter.split("SELECT 'a;b' AS x;");
        assertEquals(List.of("SELECT 'a;b' AS x"), out);
    }

    @Test
    void blankLinesBetweenStatementsDoNotProduceEmptyChunks() {
        List<String> out = splitter.split("SELECT 1;\n\nSELECT 2;");
        assertEquals(List.of("SELECT 1", "SELECT 2"), out);
    }

    @Test
    void oracleSlashTerminatesWhenLineIsOnlySlash() {
        // 行首獨立 /：結束前一段（此處不含區塊內分號，避免與 PL/SQL 內 ; 混淆）
        List<String> out = splitter.split("SELECT 1\n/\nSELECT 2;");
        assertEquals(List.of("SELECT 1", "SELECT 2"), out);
    }
}
