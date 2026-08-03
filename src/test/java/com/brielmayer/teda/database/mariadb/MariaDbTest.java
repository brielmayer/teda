package com.brielmayer.teda.database.mariadb;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.mariadb.jdbc.MariaDbDataSource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;

@Testcontainers
public class MariaDbTest extends AbstractDatabaseContractTest {

    @Container
    public static MariaDBContainer<?> mariaDbContainer = new MariaDBContainer<>("mariadb:10.9.4");

    @Override
    protected DataSource dataSource() {
        try {
            final MariaDbDataSource dataSource = new MariaDbDataSource(mariaDbContainer.getJdbcUrl());
            dataSource.setUser(mariaDbContainer.getUsername());
            dataSource.setPassword(mariaDbContainer.getPassword());
            return dataSource;
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to create the MariaDB DataSource", e);
        }
    }

    @Override
    protected String scriptDirectory() {
        return "database/mariadb";
    }

    @Override
    protected boolean supportsSchemas() {
        // Same as MySQL: a schema is a database, and the container's application
        // user may not create one. See MySqlSuiteTest.
        return false;
    }
}
