package com.brielmayer.teda.parser.excel;

import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.Coord;

public class ExcelHeaderParser {

    public static List<Header> parseHeader(Sheet excelSheet, Coord coord) {
        final List<Header> headers = new ArrayList<>();
        final int headerRow = coord.row + 1;

        for (int c = 0; ; c++) {
            final String name = cellText(ExcelTableParser.getCell(excelSheet, headerRow, coord.col + c));
            if (name.isEmpty()) {
                // if empty column is reached, break
                break;
            }
            headers.add(Header.fromName(name));
        }
        return headers;
    }

    private static String cellText(final Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        return cell.toString();
    }
}
