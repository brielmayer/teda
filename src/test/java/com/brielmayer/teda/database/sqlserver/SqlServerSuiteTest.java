package com.brielmayer.teda.database.sqlserver;

import javax.sql.DataSource;

import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

@Testcontainers
public class SqlServerSuiteTest extends AbstractDatabaseContractTest {

    @Container
    public static MSSQLServerContainer<?> mssqlServerContainer =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2017-CU12").acceptLicense();

    @Override
    protected DataSource dataSource() {
        final SQLServerDataSource dataSource = new SQLServerDataSource();
        dataSource.setURL(mssqlServerContainer.getJdbcUrl());
        dataSource.setUser(mssqlServerContainer.getUsername());
        dataSource.setPassword(mssqlServerContainer.getPassword());
        return dataSource;
    }

    @Override
    protected String scriptDirectory() {
        return "database/sqlserver";
    }
}
