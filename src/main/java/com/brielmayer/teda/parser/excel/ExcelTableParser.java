package com.brielmayer.teda.parser.excel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.model.Table;
import com.brielmayer.teda.parser.Coord;
import com.brielmayer.teda.parser.Parser;

public class ExcelTableParser {

    public static Map<String, Table> parseTable(Sheet excelSheet) {
        Stream<Coord> tableStream = findCells(Parser.TABLE, excelSheet).stream();
        Stream<Coord> tedaStream = findCells(Parser.TEDA, excelSheet).stream();
        return Stream.concat(tableStream, tedaStream)
                .map(coord -> parseTable(excelSheet, coord))
                .collect(HashMap::new, (map, table) -> map.put(table.getName(), table), HashMap::putAll);
    }

    private static Table parseTable(Sheet excelSheet, Coord coord) {
        final String tableName = parseTableName(excelSheet, coord);
        final List<Header> headers = ExcelHeaderParser.parseHeader(excelSheet, coord);
        final List<Map<String, Object>> data = ExcelDataParser.parseData(excelSheet, coord);

        return Table.builder().name(tableName).headers(headers).data(data).build();
    }

    private static String parseTableName(Sheet excelSheet, Coord coord) {
        final Cell cell = getCell(excelSheet, coord.row, coord.col + 1);
        return cell == null ? "" : cell.getStringCellValue();
    }

    private static List<Coord> findCells(final String needle, final Sheet haystack) {
        final List<Coord> coords = new ArrayList<>();
        // only search first 100 rows/columns
        final int maxRows = Math.min(100, haystack.getLastRowNum() + 1);

        for (int r = 0; r < maxRows; r++) {
            final Row row = haystack.getRow(r);
            if (row == null) {
                continue;
            }
            final int maxCols = Math.min(100, row.getLastCellNum());
            for (int c = 0; c < maxCols; c++) {
                final Cell cell = row.getCell(c);
                if (cell == null || cell.getCellType() != CellType.STRING) {
                    continue;
                }
                if (needle.equals(cell.getStringCellValue())) {
                    coords.add(new Coord(r, c));
                }
            }
        }
        return coords;
    }

    static Cell getCell(Sheet excelSheet, int rowIdx, int colIdx) {
        if (rowIdx < 0 || rowIdx > excelSheet.getLastRowNum()) {
            return null;
        }
        final Row row = excelSheet.getRow(rowIdx);
        if (row == null || colIdx < 0 || colIdx >= row.getLastCellNum()) {
            return null;
        }
        return row.getCell(colIdx);
    }
}
