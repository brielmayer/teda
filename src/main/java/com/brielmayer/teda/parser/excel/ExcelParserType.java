package com.brielmayer.teda.parser.excel;

import com.brielmayer.teda.model.DocumentType;
import com.brielmayer.teda.parser.Parser;
import com.brielmayer.teda.parser.ParserType;

public final class ExcelParserType implements ParserType {

    @Override
    public boolean handles(final DocumentType documentType) {
        return documentType == DocumentType.EXCEL;
    }

    @Override
    public Parser createParser() {
        return new ExcelDocumentParser();
    }
}
