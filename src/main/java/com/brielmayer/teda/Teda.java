package com.brielmayer.teda;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.brielmayer.teda.configuration.TedaConfiguration;
import com.brielmayer.teda.database.DatabaseFactory;
import com.brielmayer.teda.executor.TedaExecutor;
import com.brielmayer.teda.model.Document;
import com.brielmayer.teda.model.DocumentType;
import com.brielmayer.teda.parser.Parser;
import com.brielmayer.teda.parser.ParserFactory;

public class Teda {

    private final TedaConfiguration configuration;

    public Teda(final TedaConfiguration configuration) {
        this.configuration = configuration;
    }

    public void execute(final String filePath, final DocumentType documentType) {
        execute(Paths.get(filePath), documentType, null);
    }

    public void execute(final String filePath, final DocumentType documentType, final Cockpit cockpit) {
        execute(Paths.get(filePath), documentType, cockpit);
    }

    public void execute(final Path filePath, final DocumentType documentType) {
        execute(filePath, documentType, null);
    }

    public void execute(final Path filePath, final DocumentType documentType, final Cockpit cockpit) {
        final Parser parser = ParserFactory.getParser(documentType);
        run(parser.parse(filePath), cockpit);
    }

    public void execute(final InputStream inputStream, final DocumentType documentType) {
        execute(inputStream, documentType, null);
    }

    public void execute(final InputStream inputStream, final DocumentType documentType, final Cockpit cockpit) {
        final Parser parser = ParserFactory.getParser(documentType);
        run(parser.parse(inputStream), cockpit);
    }

    private void run(final Document document, final Cockpit cockpit) {
        final TedaExecutor executor = TedaExecutor.builder()
                .loadDatabase(DatabaseFactory.createDatabase(configuration.getLoadDatabase()))
                .testDatabase(DatabaseFactory.createDatabase(configuration.getTestDatabase()))
                .truncateHandler(configuration.getTruncateHandler())
                .loadHandler(configuration.getLoadHandler())
                .executionHandler(configuration.getExecutionHandler())
                .testHandler(configuration.getTestHandler())
                .build();
        executor.execute(document, cockpit);
    }
}
