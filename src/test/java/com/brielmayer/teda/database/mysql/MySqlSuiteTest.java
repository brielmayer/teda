package com.brielmayer.teda.database.mysql;

import javax.sql.DataSource;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brielmayer.teda.database.AbstractDatabaseContractTest;
import com.mysql.cj.jdbc.MysqlDataSource;

@Testcontainers
public class MySqlSuiteTest extends AbstractDatabaseContractTest {

    @Container
    public static MySQLContainer<?> mySqlContainer8_0_31 = new MySQLContainer<>("mysql:8.0.31");

    @Override
    protected DataSource dataSource() {
        final MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setUrl(mySqlContainer8_0_31.getJdbcUrl());
        dataSource.setUser(mySqlContainer8_0_31.getUsername());
        dataSource.setPassword(mySqlContainer8_0_31.getPassword());
        return dataSource;
    }

    @Override
    protected String scriptDirectory() {
        return "database/mysql";
    }

    @Override
    protected boolean supportsSchemas() {
        // In MySQL a schema is a database, and the container's application user is
        // only granted rights on its own. Creating another one fails with
        // "Access denied for user 'test'@'%' to database 'TEDA_SCHEMA'". That is a
        // limitation of the test environment, not of Teda. Qualified names are
        // handled in dialect-independent code and covered by BaseDatabaseTest.
        return false;
    }
}
