package com.brielmayer.teda.database.h2;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;

public class H2Test extends AbstractDatabaseContractTest {

    @Override
    protected DataSource dataSource() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        // DB_CLOSE_DELAY=-1 keeps the in-memory database alive for the lifetime of the
        // JVM. Without it the database is dropped as soon as no connection is open, which
        // happens between statements because connections are closed properly.
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Override
    protected String scriptDirectory() {
        return "database/h2";
    }
}
