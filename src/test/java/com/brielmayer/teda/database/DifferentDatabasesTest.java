package com.brielmayer.teda.database;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.brielmayer.teda.Teda;
import com.brielmayer.teda.configuration.TedaConfiguration;
import com.brielmayer.teda.handler.IExecutionHandler;
import com.brielmayer.teda.model.DocumentType;
import com.brielmayer.teda.util.ResourceReader;
import com.mysql.cj.jdbc.MysqlDataSource;

@Testcontainers
public class DifferentDatabasesTest {

    @Container
    public static MySQLContainer<?> mySqlContainer = new MySQLContainer<>("mysql:8.0.31");

    @Container
    public static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15.1");

    private BaseDatabase mysqlDatabase;
    private BaseDatabase postgresDatabase;

    @BeforeEach
    void setup() {
        // Setup MySQL database
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setPassword(mySqlContainer.getPassword());
        mysqlDataSource.setUrl(mySqlContainer.getJdbcUrl());
        mysqlDataSource.setUser(mySqlContainer.getUsername());
        mysqlDatabase = DatabaseFactory.createDatabase(mysqlDataSource);
        mysqlDatabase.executeQuery(ResourceReader.asString("database/mysql/DROP_TEST_TABLE.sql"));
        mysqlDatabase.executeQuery(ResourceReader.asString("database/mysql/CREATE_TEST_TABLE.sql"));

        // Setup PostgreSQL database
        PGSimpleDataSource postgresDataSource = new PGSimpleDataSource();
        postgresDataSource.setPassword(postgreSQLContainer.getPassword());
        postgresDataSource.setUrl(postgreSQLContainer.getJdbcUrl());
        postgresDataSource.setUser(postgreSQLContainer.getUsername());
        postgresDatabase = DatabaseFactory.createDatabase(postgresDataSource);
        postgresDatabase.executeQuery(ResourceReader.asString("database/postgres/DROP_TEST_TABLE.sql"));
        postgresDatabase.executeQuery(ResourceReader.asString("database/postgres/CREATE_TEST_TABLE.sql"));
    }

    @Test
    void testWithTwoDifferentDatabaseTypes() {
        TedaConfiguration tedaConfiguration = TedaConfiguration.builder()
                .withLoadDatabase(mysqlDatabase.getDataSource())
                .withTestDatabase(postgresDatabase.getDataSource())
                .withExecutionHandler(new CopyTableExecutionHandler(mysqlDatabase, postgresDatabase))
                .build();

        new Teda(tedaConfiguration)
                .execute(ResourceReader.asInputStream("teda/xlsx/DIFFERENT_DATABASE_TEST.xlsx"), DocumentType.EXCEL);
    }

    /**
     * Copies a table from the load database to the test database, standing in for
     * the ETL process a real project would trigger from the {@code EXECUTE} action.
     */
    private static final class CopyTableExecutionHandler implements IExecutionHandler {

        private final BaseDatabase source;
        private final BaseDatabase target;

        CopyTableExecutionHandler(final BaseDatabase source, final BaseDatabase target) {
            this.source = source;
            this.target = target;
        }

        @Override
        public void execute(final String tableName) {
            final List<Map<String, Object>> rows = source.queryForList("SELECT * FROM " + tableName);
            for (final Map<String, Object> row : rows) {
                try {
                    target.insertRow(tableName, row);
                } catch (final SQLException e) {
                    throw new IllegalStateException(
                            "Unable to copy a row of " + tableName + " into the test database", e);
                }
            }
        }
    }
}
