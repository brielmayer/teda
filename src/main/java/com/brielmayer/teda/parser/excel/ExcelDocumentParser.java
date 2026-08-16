package com.brielmayer.teda.parser.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Document;
import com.brielmayer.teda.parser.Parser;

/**
 * Reads Excel workbooks via Apache POI. {@link WorkbookFactory#create(InputStream)}
 * transparently picks {@code HSSFWorkbook} for the legacy BIFF format (which
 * Excel on macOS still writes even when the file carries an {@code .xlsx}
 * extension) or {@code XSSFWorkbook} for the modern OOXML format.
 */
public class ExcelDocumentParser implements Parser {

    @Override
    public Document parse(InputStream inputStream) {
        Map<String, com.brielmayer.teda.model.Sheet> sheets = new HashMap<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                final Sheet excelSheet = workbook.getSheetAt(i);
                sheets.put(excelSheet.getSheetName(), ExcelSheetParser.parseSheet(excelSheet));
            }
        } catch (final IOException e) {
            throw TedaException.builder()
                    .appendMessage("Unable to read the Excel workbook from the given InputStream")
                    .cause(e)
                    .build();
        }
        return new Document(sheets);
    }
}
