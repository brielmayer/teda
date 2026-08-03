package com.brielmayer.teda.parser.ods;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.brielmayer.teda.model.Header;
import com.brielmayer.teda.parser.CellValue;
import com.brielmayer.teda.parser.Coord;
import com.github.miachm.sods.Sheet;

public class OdsDataParser {

    public static List<Map<String, Object>> parseData(Sheet odsSheet, Coord coord) {
        final List<Map<String, Object>> data = new ArrayList<>();
        final List<Header> headers = OdsHeaderParser.parseHeader(odsSheet, coord);
        final int maxRows = odsSheet.getMaxRows();

        for (int r = coord.row + 2; r < maxRows; r++) {
            final Map<String, Object> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                final Object value = odsSheet.getRange(r, coord.col + c + 1).getValue();
                row.put(headers.get(c).getName(), value == null ? "" : value);
            }

            if (CellValue.isEmptyRow(row)) {
                // end of table reached
                break;
            }
            CellValue.resolveNullTokens(row);
            data.add(row);
        }
        return data;
    }
}
