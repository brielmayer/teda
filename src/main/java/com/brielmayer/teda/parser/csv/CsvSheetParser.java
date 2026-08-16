package com.brielmayer.teda.parser.csv;

import java.util.List;
import java.util.Map;

import com.brielmayer.teda.model.Sheet;
import com.brielmayer.teda.model.Table;

public class CsvSheetParser {

    public static Sheet parseSheet(final String name, final List<List<String>> grid) {
        final Map<String, Table> tables = CsvTableParser.parseTable(name, grid);
        return Sheet.builder().name(name).tables(tables).build();
    }
}
