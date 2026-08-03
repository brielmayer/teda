package com.brielmayer.teda.database.mariadb;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.model.Header;

public class MariaDbDatabase extends BaseDatabase {

    public MariaDbDatabase(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void truncateTable(String tableName) {
        validateTableName(tableName);
        executeQuery("TRUNCATE TABLE " + tableName);
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
