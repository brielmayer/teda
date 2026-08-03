package com.brielmayer.teda.database;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Header;

public abstract class BaseDatabase {

    // Only plain identifier chars allowed: letters, digits, underscore, starting with
    // a letter or underscore. This blocks SQL-injection through table/column names
    // sourced from spreadsheet cells without requiring per-dialect quoting.
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    // Table names may be qualified: "schema.table", or "catalog.schema.table" on
    // SQL Server. Every part is matched against SAFE_IDENTIFIER on its own, so the
    // dot only separates identifiers and cannot smuggle anything into the query.
    private static final int MAX_TABLE_NAME_PARTS = 3;

    // maps a ResultSet to a list of case-insensitive row maps (DbUtils' default
    // BasicRowProcessor lower-cases column keys). DbUtils manages the
    // connection/statement/resultset lifecycle around this handler.
    private static final ResultSetHandler<List<Map<String, Object>>> MAP_LIST_HANDLER = new MapListHandler();

    private final DataSource dataSource;
    private final QueryRunner queryRunner;

    protected BaseDatabase(final DataSource dataSource) {
        this(dataSource, new QueryRunner(dataSource));
    }

    /**
     * For dialects that need custom parameter binding. See
     * {@code UnspecifiedTypeQueryRunner} in the postgres package.
     */
    protected BaseDatabase(final DataSource dataSource, final QueryRunner queryRunner) {
        this.dataSource = dataSource;
        this.queryRunner = queryRunner;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public int executeQuery(final String query) {
        try {
            return queryRunner.update(query);
        } catch (final SQLException e) {
            throw TedaException.builder()
                    .appendMessage("Failed to execute query: %s", query)
                    .cause(e)
                    .build();
        }
    }

    public List<Map<String, Object>> queryForList(final String query) {
        try {
            return queryRunner.query(query, MAP_LIST_HANDLER);
        } catch (final SQLException e) {
            throw TedaException.builder()
                    .appendMessage("Failed to execute query: %s", query)
                    .cause(e)
                    .build();
        }
    }

    protected void executeRow(final String query, final Map<String, Object> row) throws SQLException {
        queryRunner.update(query, row.values().toArray());
    }

    /**
     * Rejects any identifier that would allow SQL injection when concatenated
     * into a query. Subclasses must call this on every table or column name that
     * originates from user-controlled input (i.e. spreadsheet cells).
     */
    protected static void validateIdentifier(final String identifier) {
        if (identifier == null || !SAFE_IDENTIFIER.matcher(identifier).matches()) {
            throw TedaException.builder()
                    .appendMessage("Invalid SQL identifier: \"%s\"", identifier)
                    .appendMessage(
                            "Only letters, digits, and underscore are allowed; must start with a letter or underscore.")
                    .build();
        }
    }

    protected static void validateIdentifiers(final Collection<String> identifiers) {
        for (final String identifier : identifiers) {
            validateIdentifier(identifier);
        }
    }

    /**
     * Rejects any table name that would allow SQL injection when concatenated
     * into a query. Unlike {@link #validateIdentifier(String)} this accepts a
     * qualified name ({@code schema.table}, or {@code catalog.schema.table} on
     * SQL Server) by validating each dot-separated part individually.
     */
    protected static void validateTableName(final String tableName) {
        final String[] parts = tableName == null ? null : tableName.split("\\.", -1);
        if (parts == null || parts.length > MAX_TABLE_NAME_PARTS || !allSafeIdentifiers(parts)) {
            throw TedaException.builder()
                    .appendMessage("Invalid table name: \"%s\"", tableName)
                    .appendMessage("Expected [catalog.][schema.]table, where every part contains only letters, "
                            + "digits, and underscore and starts with a letter or underscore.")
                    .build();
        }
    }

    private static boolean allSafeIdentifiers(final String[] identifiers) {
        for (final String identifier : identifiers) {
            if (!SAFE_IDENTIFIER.matcher(identifier).matches()) {
                return false;
            }
        }
        return true;
    }

    public abstract void truncateTable(String tableName);

    public abstract void dropTable(String tableName);

    public abstract void insertRow(String tableName, Map<String, Object> row) throws SQLException;

    public abstract List<Map<String, Object>> select(String tableName, List<Header> headers);
}
