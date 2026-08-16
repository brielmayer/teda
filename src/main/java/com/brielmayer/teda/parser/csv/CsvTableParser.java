package com.brielmayer.teda.parser.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.model.Table;
import com.brielmayer.teda.parser.Coord;
import com.brielmayer.teda.parser.Parser;

public class CsvTableParser {

    public static Map<String, Table> parseTable(final String sheetName, final List<List<String>> grid) {
        final Stream<Coord> tableStream = findCells(Parser.TABLE, grid).stream();
        final Stream<Coord> tedaStream = findCells(Parser.TEDA, grid).stream();
        return Stream.concat(tableStream, tedaStream)
                .map(coord -> parseTable(sheetName, grid, coord))
                .collect(HashMap::new, (map, table) -> map.put(table.getName(), table), HashMap::putAll);
    }

    private static Table parseTable(final String sheetName, final List<List<String>> grid, final Coord coord) {
        final String tableName = parseTableName(grid, coord);
        final List<Header> headers = CsvHeaderParser.parseHeader(grid, coord);
        final List<Map<String, Object>> data = CsvDataParser.parseData(grid, coord);
        return Table.builder()
                .sheetName(sheetName)
                .name(tableName)
                .headers(headers)
                .data(data)
                .build();
    }

    private static String parseTableName(final List<List<String>> grid, final Coord coord) {
        final String value = getCell(grid, coord.row, coord.col + 1);
        return value == null ? "" : value;
    }

    private static List<Coord> findCells(final String needle, final List<List<String>> grid) {
        final List<Coord> coords = new ArrayList<>();
        // only search first 100 rows/columns
        final int maxRows = Math.min(100, grid.size());
        for (int r = 0; r < maxRows; r++) {
            final List<String> row = grid.get(r);
            if (row == null) {
                continue;
            }
            final int maxCols = Math.min(100, row.size());
            for (int c = 0; c < maxCols; c++) {
                if (needle.equals(row.get(c))) {
                    coords.add(new Coord(r, c));
                }
            }
        }
        return coords;
    }

    static String getCell(final List<List<String>> grid, final int rowIdx, final int colIdx) {
        if (rowIdx < 0 || rowIdx >= grid.size()) {
            return null;
        }
        final List<String> row = grid.get(rowIdx);
        if (row == null || colIdx < 0 || colIdx >= row.size()) {
            return null;
        }
        return row.get(colIdx);
    }
}
