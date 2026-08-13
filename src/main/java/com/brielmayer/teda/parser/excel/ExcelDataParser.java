package com.brielmayer.teda.parser.excel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.CellValue;
import com.brielmayer.teda.parser.Coord;

public class ExcelDataParser {

    public static List<Map<String, Object>> parseData(Sheet excelSheet, Coord coord) {
        final List<Map<String, Object>> data = new ArrayList<>();
        final List<Header> headers = ExcelHeaderParser.parseHeader(excelSheet, coord);
        final int lastRow = excelSheet.getLastRowNum();

        for (int r = coord.row + 2; r <= lastRow; r++) {
            final Row row = excelSheet.getRow(r);
            if (row == null) {
                // end of table reached
                break;
            }

            final Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                final Cell cell = ExcelTableParser.getCell(excelSheet, r, coord.col + c);
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
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        switch (type) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue();
                }
                return numericValue(cell.getNumericCellValue());
            case BOOLEAN:
                return cell.getBooleanCellValue();
                // BLANK, ERROR
            default:
                return "";
        }
    }

    private static Object numericValue(final double d) {
        if (isMathematicalInteger(d)) {
            return (long) d;
        }
        return BigDecimal.valueOf(d);
    }

    private static boolean isMathematicalInteger(final double x) {
        return !Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x);
    }
}
