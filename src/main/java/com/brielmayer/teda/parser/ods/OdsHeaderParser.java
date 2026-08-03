package com.brielmayer.teda.parser.ods;

import java.util.ArrayList;
import java.util.List;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.Coord;
import com.github.miachm.sods.Sheet;

public class OdsHeaderParser {

    public static List<Header> parseHeader(Sheet odsSheet, Coord coord) {
        final List<Header> headers = new ArrayList<>();
        final int maxCols = odsSheet.getMaxColumns();
        final int headerRow = coord.row + 1;

        for (int c = 0; coord.col + c < maxCols; c++) {
            final Object value = odsSheet.getRange(headerRow, coord.col + c).getValue();
            if (value == null || value.toString().isEmpty()) {
                // if empty column is reached, break
                break;
            }
            headers.add(Header.fromName(value.toString()));
        }
        return headers;
    }
}
