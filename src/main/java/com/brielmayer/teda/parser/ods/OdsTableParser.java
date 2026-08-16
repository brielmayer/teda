package com.brielmayer.teda.parser.ods;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.model.Table;
import com.brielmayer.teda.parser.Coord;
import com.brielmayer.teda.parser.Parser;
import com.github.miachm.sods.Sheet;

public class OdsTableParser {

    public static Map<String, Table> parseTable(Sheet odsSheet) {
        Stream<Coord> tableStream = findCells(Parser.TABLE, odsSheet).stream();
        Stream<Coord> tedaStream = findCells(Parser.TEDA, odsSheet).stream();
        return Stream.concat(tableStream, tedaStream)
                .map(coord -> parseTable(odsSheet, coord))
                .collect(HashMap::new, (map, table) -> map.put(table.getName(), table), HashMap::putAll);
    }

    private static Table parseTable(Sheet odsSheet, Coord coord) {
        final String tableName = parseTableName(odsSheet, coord);
        final List<Header> headers = OdsHeaderParser.parseHeader(odsSheet, coord);
        final List<Map<String, Object>> data = OdsDataParser.parseData(odsSheet, coord);

        return Table.builder()
                .sheetName(odsSheet.getName())
                .name(tableName)
                .headers(headers)
                .data(data)
                .build();
    }

    private static String parseTableName(Sheet odsSheet, Coord coord) {
        final Object value = odsSheet.getRange(coord.row, coord.col + 1).getValue();
        return value == null ? "" : value.toString();
    }

    private static List<Coord> findCells(final String needle, final Sheet haystack) {
        final List<Coord> coords = new ArrayList<>();
        // only search first 100 rows/columns
        final int maxRows = Math.min(100, haystack.getMaxRows());
        final int maxCols = Math.min(100, haystack.getMaxColumns());

        for (int r = 0; r < maxRows; r++) {
            for (int c = 0; c < maxCols; c++) {
                final Object value = haystack.getRange(r, c).getValue();
                if (value instanceof String && value.equals(needle)) {
                    coords.add(new Coord(r, c));
                }
            }
        }
        return coords;
    }
}
