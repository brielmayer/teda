package com.brielmayer.teda.parser.excel;

import java.util.Map;

import com.brielmayer.teda.model.Sheet;
import com.brielmayer.teda.model.Table;

public class ExcelSheetParser {

    public static Sheet parseSheet(org.apache.poi.ss.usermodel.Sheet excelSheet) {
        Map<String, Table> tables = ExcelTableParser.parseTable(excelSheet);
        return Sheet.builder().name(excelSheet.getSheetName()).tables(tables).build();
    }
}
