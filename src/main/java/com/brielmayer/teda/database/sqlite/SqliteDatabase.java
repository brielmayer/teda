package com.brielmayer.teda.database.sqlite;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.model.Header;

public class SqliteDatabase extends BaseDatabase {

    public SqliteDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void truncateTable(String tableName) {
        validateTableName(tableName);
        // SQLite does not support TRUNCATE TABLE; DELETE FROM (without WHERE)
        // is the equivalent and is internally optimized to a truncate.
        executeQuery("DELETE FROM " + tableName);
    }

    @Override
    public void dropTable(String tableName) {
        validateTableName(tableName);
        executeQuery("DROP TABLE IF EXISTS " + tableName);
    }

    @Override
    public void insertRow(String tableName, Map<String, Object> row) throws SQLException {
        validateTableName(tableName);
        validateIdentifiers(row.keySet());
        String query = "INSERT INTO %s(%s) VALUES (%s)";

        // "?" * header size
        List<String> placeHolder = Collections.nCopies(row.keySet().size(), "?");
        query = String.format(query, tableName, String.join(",", row.keySet()), String.join(",", placeHolder));

        executeRow(query, row);
    }

    @Override
    public List<Map<String, Object>> select(String tableName, List<Header> headers) {
        validateTableName(tableName);
        List<String> headerNames = headers.stream().map(Header::getName).collect(Collectors.toList());
        validateIdentifiers(headerNames);
        String query = "SELECT %s FROM %s";
        query = String.format(query, String.join(",", headerNames), tableName);
        return queryForList(query);
    }
}
