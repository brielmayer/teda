package com.brielmayer.teda.handler.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.brielmayer.teda.comparator.ObjectComparator;
import com.brielmayer.teda.comparator.SortComparator;
import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.handler.ITestHandler;
import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.model.Table;
import com.brielmayer.teda.parser.CellValue;
import com.brielmayer.teda.parser.TypeParser;

public final class TestHandler implements ITestHandler {

    private static final Logger log = LoggerFactory.getLogger(TestHandler.class);

    public void test(final BaseDatabase database, final Table expectedTable) {

        final Table actualTable = Table.builder()
                .sheetName(expectedTable.getSheetName())
                .name(expectedTable.getName())
                .headers(expectedTable.getHeaders())
                .data(database.select(expectedTable.getName(), expectedTable.getHeaders()))
                .build();

        log.info("Test {}", expectedTable.describe());

        // sort data
        final List<Header> primaryKeys =
                expectedTable.getHeaders().stream().filter(Header::isPrimaryKey).collect(Collectors.toList());
        // separate comparators so a sorting error names the side it originates from
        final SortComparator expectedComparator = new SortComparator(
                primaryKeys, String.format("the expected data (spreadsheet) of %s", expectedTable.describe()));
        final SortComparator actualComparator = new SortComparator(
                primaryKeys, String.format("the actual data (database) of %s", expectedTable.describe()));
        expectedTable.getData().sort(expectedComparator);
        actualTable.getData().sort(actualComparator);

        // rows must be uniquely identifiable, otherwise the row-by-row comparison
        // below can not reliably line expected and actual rows up
        assertPrimaryKeysAreUnique(expectedTable, primaryKeys, expectedComparator);

        // compare data
        compare(expectedTable, actualTable);
    }

    private static void assertPrimaryKeysAreUnique(
            final Table table, final List<Header> primaryKeys, final SortComparator comparator) {
        if (primaryKeys.isEmpty()) {
            return;
        }

        final List<Map<String, Object>> data = table.getData();
        for (int i = 1; i < data.size(); i++) {
            // data is already sorted, so duplicate keys are adjacent
            if (comparator.compare(data.get(i - 1), data.get(i)) == 0) {
                throw TedaException.builder()
                        .appendMessage("Duplicate primary key in %s", table.describe())
                        .appendMessage("Rows %d and %d share the same primary key %s", i, i + 1, primaryKeys)
                        .appendMessage("Row %d: %s", i, data.get(i - 1))
                        .appendMessage("Row %d: %s", i + 1, data.get(i))
                        .build();
            }
        }
    }

    private void compare(final Table expectedTable, final Table actualTable) {
        // check number of rows
        if (expectedTable.getData().size() != actualTable.getData().size()) {
            throw TedaException.builder()
                    .appendMessage("Failed to compare data for %s", expectedTable.describe())
                    .appendMessage("Number of rows are not equal")
                    .appendMessage(
                            "Expected number of rows: %d",
                            expectedTable.getData().size())
                    .appendMessage(
                            "Actual number of rows: %d", actualTable.getData().size())
                    .appendMessage()
                    .appendMessage("Expected:")
                    .appendMessage("%s", listToString(expectedTable.getData()))
                    .appendMessage("Actual:")
                    .appendMessage("%s", listToString(actualTable.getData()))
                    .build();
        }

        // compare line by line
        for (int rowCount = 0; rowCount < expectedTable.getData().size(); rowCount++) {

            final Map<String, Object> expectedRow = expectedTable.getData().get(rowCount);
            final Map<String, Object> actualRow = actualTable.getData().get(rowCount);

            for (final Map.Entry<String, Object> entry : expectedRow.entrySet()) {
                final String key = entry.getKey();

                final Object actualValue = TypeParser.parse(actualRow.get(key));
                final Object expectedValue = TypeParser.parse(expectedRow.get(key));

                if (!compareValues(expectedTable, rowCount, key, expectedValue, actualValue)) {
                    throw TedaException.builder()
                            .appendMessage("Error comparing %s in row %d", expectedTable.describe(), rowCount + 1)
                            .appendMessage(
                                    "Column %s: Expected %s != Actual %s",
                                    key, describeValue(expectedValue), describeValue(actualValue))
                            .appendMessage("Expected Row:  %s", expectedRow.toString())
                            .appendMessage("Actual Row:    %s", actualRow.toString())
                            .build();
                }
            }
        }
    }

    private static boolean compareValues(
            final Table expectedTable,
            final int rowCount,
            final String column,
            final Object expectedValue,
            final Object actualValue) {
        try {
            return ObjectComparator.compare(actualValue, expectedValue);
        } catch (final TedaException e) {
            throw TedaException.builder()
                    .appendMessage(
                            "Error comparing %s in row %d, column %s", expectedTable.describe(), rowCount + 1, column)
                    .appendMessage("%s", e.getMessage().trim())
                    .cause(e)
                    .build();
        }
    }

    /**
     * Renders a value with its type, e.g. {@code (String) "Alice"}. A missing value is
     * shown as the {@code [NULL]} token the user writes in the spreadsheet, rather than
     * as {@code (null) "null"}.
     */
    private static String describeValue(final Object value) {
        if (value == null) {
            return CellValue.NULL_TOKEN;
        }
        return String.format("(%s) \"%s\"", value.getClass().getSimpleName(), value);
    }

    // only used in case of an exception
    private String listToString(final List<Map<String, Object>> data) {
        final StringBuilder retVal = new StringBuilder();
        for (final Map<String, Object> row : data) {
            retVal.append(String.format("%s\n", row.toString()));
        }
        return retVal.toString();
    }
}
