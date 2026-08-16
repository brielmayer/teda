package com.brielmayer.teda.comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Header;

class SortComparatorTest {

    /** What the comparator names in its error messages; irrelevant for the ordering tests. */
    private static final String SOURCE = "the test data";

    private static SortComparator comparatorFor(final String... primaryKeys) {
        final List<Header> headers = new ArrayList<>();
        for (final String primaryKey : primaryKeys) {
            headers.add(Header.fromName(primaryKey));
        }
        return new SortComparator(headers, SOURCE);
    }

    private static Map<String, Object> row(final Object... keyValues) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            row.put((String) keyValues[i], keyValues[i + 1]);
        }
        return row;
    }

    private static List<Object> idColumn(final List<Map<String, Object>> rows) {
        final List<Object> ids = new ArrayList<>();
        rows.forEach(r -> ids.add(r.get("id")));
        return ids;
    }

    @Test
    void sortsNumericPrimaryKeyNumericallyNotLexicographically() {
        // the old comparator compared the whole row's toString(), so "10" sorted before "2"
        final List<Map<String, Object>> rows =
                new ArrayList<>(Arrays.asList(row("id", 10L), row("id", 2L), row("id", 1L)));

        rows.sort(comparatorFor("#id"));

        assertEquals(Arrays.asList(1L, 2L, 10L), idColumn(rows));
    }

    @Test
    void sortsExpectedLongAndActualBigDecimalIntoTheSameOrder() {
        // expected values come from the spreadsheet (Long), actual values from the DB (BigDecimal);
        // both must end up in the same logical order so rows line up positionally
        final List<Map<String, Object>> expected =
                new ArrayList<>(Arrays.asList(row("id", 10L), row("id", 1L), row("id", 2L)));
        final List<Map<String, Object>> actual = new ArrayList<>(Arrays.asList(
                row("id", new BigDecimal("2")), row("id", new BigDecimal("10")), row("id", new BigDecimal("1"))));

        final SortComparator comparator = comparatorFor("#id");
        expected.sort(comparator);
        actual.sort(comparator);

        assertEquals(Arrays.asList(1L, 2L, 10L), idColumn(expected));
        assertEquals(Arrays.asList(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("10")), idColumn(actual));
    }

    @Test
    void sortsByMultiplePrimaryKeysInOrder() {
        final List<Map<String, Object>> rows = new ArrayList<>(
                Arrays.asList(row("group", "A", "id", 2L), row("group", "B", "id", 1L), row("group", "A", "id", 1L)));

        rows.sort(comparatorFor("#group", "#id"));

        assertEquals(
                Arrays.asList("A", "A", "B"),
                rows.stream().map(r -> r.get("group")).collect(java.util.stream.Collectors.toList()));
        assertEquals(Arrays.asList(1L, 2L, 1L), idColumn(rows));
    }

    @Test
    void throwsWhenPrimaryKeyTypesAreInconsistentWithinAList() {
        final List<Map<String, Object>> rows = new ArrayList<>(Arrays.asList(row("id", 1L), row("id", "not-a-number")));

        assertThrows(TedaException.class, () -> rows.sort(comparatorFor("#id")));
    }
}
