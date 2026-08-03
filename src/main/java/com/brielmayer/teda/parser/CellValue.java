package com.brielmayer.teda.parser;

import java.util.Map;

/**
 * Shared handling of raw spreadsheet cell values, independent of the document
 * format (XLSX, ODS, CSV).
 * <p>
 * A cell containing {@value #NULL_TOKEN} (case-insensitive, surrounding
 * whitespace ignored) stands for a SQL {@code NULL} and is turned into
 * {@code null}. Everything else keeps its value; a missing or empty cell stays
 * the empty String, which is what gets inserted into the database and compared
 * against it. The distinction matters: an empty cell means "empty string",
 * {@value #NULL_TOKEN} means "no value at all".
 * <p>
 * Note that Oracle treats the empty String as {@code NULL}, so on Oracle an
 * empty cell and {@value #NULL_TOKEN} end up indistinguishable in the database.
 */
public final class CellValue {

    /** Cell content that stands for a SQL {@code NULL} value. */
    public static final String NULL_TOKEN = "[NULL]";

    private CellValue() {}

    /**
     * Replaces every {@value #NULL_TOKEN} cell of the given row with
     * {@code null}, in place.
     * <p>
     * Must be called <em>after</em> {@link #isEmptyRow(Map)}: the null token is
     * row content and must not let the row count as the empty row that
     * terminates a table.
     */
    public static void resolveNullTokens(final Map<String, Object> row) {
        for (final Map.Entry<String, Object> entry : row.entrySet()) {
            if (isNullToken(entry.getValue())) {
                entry.setValue(null);
            }
        }
    }

    /**
     * Returns whether the row holds no content at all. Parsers use this to
     * detect the end of a table. Expects raw cell values, i.e. before
     * {@link #resolveNullTokens(Map)} has been applied.
     */
    public static boolean isEmptyRow(final Map<String, Object> row) {
        for (final Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNullToken(final Object value) {
        return value instanceof String && NULL_TOKEN.equalsIgnoreCase(((String) value).trim());
    }
}
