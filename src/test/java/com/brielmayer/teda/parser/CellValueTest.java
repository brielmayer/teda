package com.brielmayer.teda.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CellValueTest {

    @Test
    public void testNullTokenBecomesNull() {
        final Map<String, Object> row = row("a", "[NULL]");
        CellValue.resolveNullTokens(row);
        assertNull(row.get("a"));
    }

    @Test
    public void testNullTokenIsCaseInsensitive() {
        final Map<String, Object> row = row("a", "[null]", "b", "[Null]", "c", "[nUlL]");
        CellValue.resolveNullTokens(row);
        assertNull(row.get("a"));
        assertNull(row.get("b"));
        assertNull(row.get("c"));
    }

    @Test
    public void testNullTokenIgnoresSurroundingWhitespace() {
        final Map<String, Object> row = row("a", "  [NULL] ");
        CellValue.resolveNullTokens(row);
        assertNull(row.get("a"));
    }

    @Test
    public void testEmptyStringStaysEmptyString() {
        // an empty cell means "empty string", not "no value"
        final Map<String, Object> row = row("a", "");
        CellValue.resolveNullTokens(row);
        assertEquals("", row.get("a"));
    }

    @Test
    public void testOtherValuesAreUntouched() {
        final Map<String, Object> row = row("a", "NULL", "b", "[NULL] Alice", "c", 42L, "d", "null");
        CellValue.resolveNullTokens(row);
        assertEquals("NULL", row.get("a"));
        assertEquals("[NULL] Alice", row.get("b"));
        assertEquals(42L, row.get("c"));
        assertEquals("null", row.get("d"));
    }

    @Test
    public void testEmptyRowDetection() {
        assertTrue(CellValue.isEmptyRow(row("a", "", "b", "")));
        assertTrue(CellValue.isEmptyRow(row("a", null, "b", "")));
        assertFalse(CellValue.isEmptyRow(row("a", "", "b", "x")));
    }

    @Test
    public void testNullTokenIsNotAnEmptyRow() {
        // a row of null values is real data and must not terminate the table
        assertFalse(CellValue.isEmptyRow(row("a", "[NULL]", "b", "[NULL]")));
    }

    private static Map<String, Object> row(final Object... keysAndValues) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            row.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return row;
    }
}
