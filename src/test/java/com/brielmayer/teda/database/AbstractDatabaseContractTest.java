package com.brielmayer.teda.database;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.brielmayer.teda.Teda;
import com.brielmayer.teda.configuration.TedaConfiguration;
import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.DocumentType;
import com.brielmayer.teda.util.ResourceReader;

/**
 * Every scenario Teda supports, run against one database. A subclass only supplies
 * the DataSource and the folder holding that dialect's DDL scripts, so a scenario
 * added here is automatically covered on every supported database.
 * <p>
 * The document format is not redundant with the database: it decides which Java
 * types reach the JDBC driver (CSV yields Strings, XLSX/ODS yield typed values),
 * and drivers differ in what they accept. Each format therefore runs everywhere.
 * <p>
 * Where dialects genuinely differ, the difference is expressed through an
 * overridable hook and asserted, rather than by leaving the test out. A missing
 * test looks the same as a passing one.
 */
public abstract class AbstractDatabaseContractTest {

    private BaseDatabase database;

    /** The database under test. */
    protected abstract DataSource dataSource();

    /** Classpath folder holding this dialect's DDL, e.g. {@code database/postgres}. */
    protected abstract String scriptDirectory();

    /**
     * Whether the dialect stores the empty String as NULL. True only for Oracle,
     * where an empty cell and {@code [NULL]} therefore end up indistinguishable.
     */
    protected boolean emptyStringIsNull() {
        return false;
    }

    /**
     * Whether a {@code TEDA_SCHEMA} schema can be created for the
     * schema-qualified table test.
     */
    protected boolean supportsSchemas() {
        return true;
    }

    @BeforeEach
    void setupDatabase() {
        database = DatabaseFactory.createDatabase(dataSource());
        runScript("DROP_TEST_TABLE.sql");
        runScript("CREATE_TEST_TABLE.sql");
        runScript("DROP_TEST_TABLE_2.sql");
        runScript("CREATE_TEST_TABLE_2.sql");
        if (supportsSchemas()) {
            runScript("CREATE_TEST_SCHEMA.sql");
            runScript("DROP_TEST_TABLE_SCHEMA.sql");
            runScript("CREATE_TEST_TABLE_SCHEMA.sql");
        }
    }

    @Test
    void loadFromExcel() {
        execute("teda/xlsx/LOAD_TEST.xlsx", DocumentType.EXCEL);
    }

    @Test
    void loadFromOpenDocumentSpreadsheet() {
        execute("teda/ods/LOAD_TEST.ods", DocumentType.OPEN_DOCUMENT_SPREADSHEET);
    }

    @Test
    void loadFromCsvDirectory() {
        execute("teda/csv/loadTest");
    }

    /**
     * CSV specifics: quoted values containing a comma, escaped double-quotes,
     * unquoted values with interior spaces, expected rows in reverse order, and
     * nine-digit decimals.
     */
    @Test
    void loadFromCsvWithQuotedValuesAndReorderedExpected() {
        execute("teda/csv/complex");
    }

    /** Two {@code #Table} blocks on a single sheet. */
    @Test
    void multipleTablesInOneSheet() {
        execute("teda/xlsx/MULTIPLE_TABLES_IN_ONE_SHEET.xlsx", DocumentType.EXCEL);
    }

    /**
     * {@code [NULL]} loads a SQL NULL and asserts against one. Covers a varchar,
     * an int and a decimal column, a row whose non-key columns are all NULL, and
     * the case-insensitive spelling.
     */
    @Test
    void nullValuesAreLoadedAndAsserted() {
        execute("teda/csv/nullValues");
    }

    /** NULL must not silently match a value: the fixture loads NULL, expects {@code Alice}. */
    @Test
    void nullDoesNotMatchAValue() {
        assertComparisonFails("teda/csv/nullMismatch");
    }

    /** An empty cell means the empty string, everywhere except Oracle. */
    @Test
    void emptyCellIsAnEmptyStringNotNull() {
        if (emptyStringIsNull()) {
            // the empty string comes back as NULL, so the expected '' cannot match
            assertComparisonFails("teda/csv/nullVsEmptyString");
        } else {
            execute("teda/csv/nullVsEmptyString");
        }
    }

    /** The flip side: a loaded empty string does not satisfy a {@code [NULL]} expectation. */
    @Test
    void emptyStringDoesNotMatchNull() {
        if (emptyStringIsNull()) {
            // the empty string was stored as NULL, so here the expectation does hold
            execute("teda/csv/emptyStringIsNotNull");
        } else {
            assertComparisonFails("teda/csv/emptyStringIsNotNull");
        }
    }

    /** Tables may be addressed as {@code SCHEMA.TABLE}. */
    @Test
    void schemaQualifiedTableName() {
        assumeTrue(supportsSchemas(), "dialect has no schema that this test can create");
        execute("teda/csv/schemaQualified");
    }

    private void assertComparisonFails(final String csvDirectory) {
        final TedaException e = assertThrows(TedaException.class, () -> execute(csvDirectory));
        assertTrue(e.getMessage().contains("Column NAME"), e.getMessage());
    }

    private void execute(final String csvDirectory) {
        new Teda(configuration()).execute(ResourceReader.asPath(csvDirectory), DocumentType.CSV);
    }

    private void execute(final String resource, final DocumentType documentType) {
        new Teda(configuration()).execute(ResourceReader.asInputStream(resource), documentType);
    }

    private TedaConfiguration configuration() {
        return TedaConfiguration.builder()
                .withDatabase(database.getDataSource())
                .build();
    }

    private void runScript(final String fileName) {
        database.executeQuery(ResourceReader.asString(scriptDirectory() + "/" + fileName));
    }
}
