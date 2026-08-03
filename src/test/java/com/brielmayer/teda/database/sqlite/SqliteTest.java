package com.brielmayer.teda.database.sqlite;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;

public class SqliteTest extends AbstractDatabaseContractTest {

    private Connection keepAlive;

    @Override
    protected DataSource dataSource() {
        final SQLiteConfig config = new SQLiteConfig();
        config.setSharedCache(true);

        final SQLiteDataSource dataSource = new SQLiteDataSource(config);
        dataSource.setUrl("jdbc:sqlite:file:teda-sqlite-test?mode=memory&cache=shared");

        // Xerial's SQLite driver destroys the shared-cache in-memory DB as soon as its
        // last connection closes. We hold one connection open for the lifetime of the
        // test so all connections opened later (by DbUtils' QueryRunner during
        // executeQuery / insertRow / select) see the same tables.
        try {
            keepAlive = dataSource.getConnection();
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to open the keep-alive connection", e);
        }
        return dataSource;
    }

    @AfterEach
    void closeKeepAliveConnection() throws SQLException {
        if (keepAlive != null && !keepAlive.isClosed()) {
            keepAlive.close();
        }
    }

    @Override
    protected String scriptDirectory() {
        return "database/sqlite";
    }

    @Override
    protected boolean supportsSchemas() {
        // SQLite has no CREATE SCHEMA; the only qualifiers are ATTACHed databases.
        return false;
    }
}
