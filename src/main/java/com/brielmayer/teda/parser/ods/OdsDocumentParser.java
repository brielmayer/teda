package com.brielmayer.teda.parser.ods;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Document;
import com.brielmayer.teda.model.Sheet;
import com.brielmayer.teda.parser.Parser;
import com.github.miachm.sods.SpreadSheet;

public class OdsDocumentParser implements Parser {

    @Override
    public Document parse(InputStream inputStream) {
        final SpreadSheet spreadSheet;
        try {
            File file = getFileFromInputStream(inputStream);
            spreadSheet = new SpreadSheet(file);
            file.deleteOnExit();
        } catch (final IOException e) {
            throw TedaException.builder()
                    .appendMessage("Unable to read the ODS spreadsheet from the given InputStream")
                    .cause(e)
                    .build();
        }

        Map<String, Sheet> sheets = new HashMap<>();
        for (int i = 0; i < spreadSheet.getNumSheets(); i++) {
            final com.github.miachm.sods.Sheet odsSheet = spreadSheet.getSheet(i);
            sheets.put(odsSheet.getName(), OdsSheetParser.parseSheet(odsSheet));
        }
        return new Document(sheets);
    }

    File getFileFromInputStream(InputStream inputStream) throws IOException {
        File file = new File("tmp.ods");
        try (OutputStream output = new FileOutputStream(file, false)) {
            inputStream.transferTo(output);
        }
        return file;
    }
}
