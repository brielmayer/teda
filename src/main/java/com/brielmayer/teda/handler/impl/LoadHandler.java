package com.brielmayer.teda.handler.impl;

import java.sql.SQLException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.handler.ILoadHandler;
import com.brielmayer.teda.model.Table;

public final class LoadHandler implements ILoadHandler {

    private static final Logger log = LoggerFactory.getLogger(LoadHandler.class);

    public void load(final BaseDatabase database, final Table table) {
        log.info("Load {}", table.describe());

        int rowCount = 1;
        for (final Map<String, Object> row : table.getData()) {
            try {
                database.insertRow(table.getName(), row);
            } catch (final SQLException e) {
                throw TedaException.builder()
                        .appendMessage("Failed to insert row %d into %s", rowCount, table.describe())
                        .appendMessage("Row %d: %s", rowCount, row)
                        .cause(e)
                        .build();
            }
            rowCount++;
        }
    }
}
