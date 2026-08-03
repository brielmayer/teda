package com.brielmayer.teda.parser.xlsx;

import java.util.ArrayList;
import java.util.List;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.CellType;
import org.dhatim.fastexcel.reader.Row;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.Coord;

public class XlsxHeaderParser {

    public static List<Header> parseHeader(List<Row> rows, Coord coord) {
        final List<Header> headers = new ArrayList<>();
        final int headerRow = coord.row + 1;

        for (int c = 0; ; c++) {
            final String name = cellText(XlsxTableParser.getCell(rows, headerRow, coord.col + c));
            if (name.isEmpty()) {
                // if empty column is reached, break
                break;
            }
            headers.add(Header.fromName(name));
        }
        return headers;
    }

    private static String cellText(final Cell cell) {
        if (cell == null || cell.getType() == CellType.EMPTY) {
            return "";
        }
        return cell.asString();
    }
}
