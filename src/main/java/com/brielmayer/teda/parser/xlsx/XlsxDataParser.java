package com.brielmayer.teda.parser.xlsx;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.CellValue;
import com.brielmayer.teda.parser.Coord;

public class XlsxDataParser {

    public static List<Map<String, Object>> parseData(List<Row> rows, Coord coord) {
        final List<Map<String, Object>> data = new ArrayList<>();
        final List<Header> headers = XlsxHeaderParser.parseHeader(rows, coord);

        for (int r = coord.row + 2; r < rows.size(); r++) {
            final Row row = rows.get(r);
            if (row == null) {
                // end of table reached
                break;
            }

            final Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                final Cell cell = XlsxTableParser.getCell(rows, r, coord.col + c);
                rowMap.put(headers.get(c).getName(), getCellValue(cell));
            }

            if (CellValue.isEmptyRow(rowMap)) {
                // end of table reached
                break;
            }
            CellValue.resolveNullTokens(rowMap);
            data.add(rowMap);
        }
        return data;
    }

    private static Object getCellValue(final Cell cell) {
        if (cell == null) {
            return "";
        }
        final CellType type = cell.getType();
        if (type == CellType.FORMULA) {
            return unwrap(cell.getValue());
        }
        switch (type) {
            case STRING:
                return cell.asString();
            case NUMBER:
                return unwrap(cell.getValue());
            case BOOLEAN:
                return cell.asBoolean();
                // EMPTY, ERROR
            default:
                return "";
        }
    }

    private static Object unwrap(final Object value) {
        if (value == null) {
            return "";
        }
        // Date-formatted numeric cells surface as LocalDateTime
        if (value instanceof LocalDateTime) {
            return value;
        }
        // Round-trip through double to strip Excel's storage precision
        // artifacts (e.g. 4.393949494 stored as 4.3939494940000001), matching
        // POI's Double.toString-based behavior.
        if (value instanceof BigDecimal) {
            final double d = ((BigDecimal) value).doubleValue();
            if (isMathematicalInteger(d)) {
                return (long) d;
            }
            return BigDecimal.valueOf(d);
        }
        return value;
    }

    private static boolean isMathematicalInteger(final double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x);
    }
}
