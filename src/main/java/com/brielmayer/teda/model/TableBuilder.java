package com.brielmayer.teda.model;

import java.util.List;
import java.util.Map;

import com.brielmayer.teda.exception.TedaException;

public final class TableBuilder {

    private String sheetName;
    private String name;
    private List<Header> headers;
    private List<Map<String, Object>> data;

    TableBuilder() {}

    public TableBuilder sheetName(final String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    public TableBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public TableBuilder headers(final List<Header> headers) {
        this.headers = headers;
        return this;
    }

    public TableBuilder data(final List<Map<String, Object>> data) {
        this.data = data;
        return this;
    }

    String getSheetName() {
        return sheetName;
    }

    String getName() {
        return name;
    }

    List<Header> getHeaders() {
        return headers;
    }

    List<Map<String, Object>> getData() {
        return data;
    }

    public Table build() {
        final Table table = new Table(this);
        if (headers == null || headers.isEmpty()) {
            throw TedaException.builder()
                    .appendMessage("No columns found for %s", table.describe())
                    .appendMessage("The header row must start in the same column as the #Table or "
                            + "#Teda marker, in the row directly below it.")
                    .build();
        }
        return table;
    }
}
