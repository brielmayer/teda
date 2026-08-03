package com.brielmayer.teda.database.oracle;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;

import oracle.jdbc.datasource.impl.OracleDataSource;

@Testcontainers
public class OracleSuiteTest extends AbstractDatabaseContractTest {

    @Container
    public static OracleContainer oracleContainer = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            .withDatabaseName("testDB")
            .withUsername("testUser")
            .withPassword("testPassword");

    @Override
    protected DataSource dataSource() {
        try {
            final OracleDataSource dataSource = new OracleDataSource();
            dataSource.setURL(oracleContainer.getJdbcUrl());
            dataSource.setUser(oracleContainer.getUsername());
            dataSource.setPassword(oracleContainer.getPassword());
            return dataSource;
        } catch (final SQLException e) {
            throw new IllegalStateException("Unable to create the Oracle DataSource", e);
        }
    }

    @Override
    protected String scriptDirectory() {
        return "database/oracle";
    }

    @Override
    protected boolean emptyStringIsNull() {
        // Oracle stores '' as NULL, so an empty cell and [NULL] are indistinguishable
        // once the data is in the database.
        return true;
    }

    @Override
    protected boolean supportsSchemas() {
        // In Oracle a schema is a user, and creating one needs the CREATE USER
        // privilege that the container's application user does not have. This is a
        // limitation of the test setup, not of Teda. Qualified names themselves are
        // dialect-independent and covered by BaseDatabaseTest.
        return false;
    }
}
