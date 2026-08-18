package com.brielmayer.teda;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.brielmayer.teda.configuration.TedaConfiguration;
import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.database.DatabaseFactory;
import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.DocumentType;
import com.brielmayer.teda.util.ResourceReader;

/**
 * Exercises the programmatic {@link Cockpit} against a real H2 database, covering
 * the executor wiring and the collision/validation guards. Dialect specifics stay
 * with {@code AbstractDatabaseContractTest}.
 */
class TedaCockpitIntegrationTest {

    private BaseDatabase database;

    @BeforeEach
    void setupDatabase() {
        database = DatabaseFactory.createDatabase(dataSource());
        runScript("DROP_TEST_TABLE.sql");
        runScript("CREATE_TEST_TABLE.sql");
        runScript("DROP_TEST_TABLE_2.sql");
        runScript("CREATE_TEST_TABLE_2.sql");
    }

    @Test
    void csvDirectoryWithProgrammaticCockpit() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("STUDENT_IN")
                .test("STUDENT_OUT")
                .build();

        teda().execute(ResourceReader.asPath("teda/csv/programmatic"), DocumentType.CSV, cockpit);
    }

    @Test
    void multipleTruncatesLoadsAndTests() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT", "STUDENT_2")
                .load("STUDENT_IN", "STUDENT_2_IN")
                .test("STUDENT_OUT", "STUDENT_2_OUT")
                .build();

        teda().execute(ResourceReader.asPath("teda/csv/programmatic"), DocumentType.CSV, cockpit);
    }

    @Test
    void secondLoadAndTestCycle() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("STUDENT_IN")
                .test("STUDENT_OUT")
                .truncate("STUDENT_2")
                .load("STUDENT_2_IN")
                .test("STUDENT_2_OUT")
                .build();

        teda().execute(ResourceReader.asPath("teda/csv/programmatic"), DocumentType.CSV, cockpit);
    }

    @Test
    void collisionWithFileCockpitFails() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("STUDENT_IN")
                .test("STUDENT_OUT")
                .build();

        final TedaException e = assertThrows(TedaException.class, () -> teda().execute(
                        ResourceReader.asPath("teda/csv/loadTest"), DocumentType.CSV, cockpit));
        assertTrue(e.getMessage().contains("Cockpit"), e.getMessage());
        assertTrue(e.getMessage().contains("also contains"), e.getMessage());
    }

    @Test
    void unknownSheetIsRejectedBeforeDispatch() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("DOES_NOT_EXIST")
                .test("STUDENT_OUT")
                .build();

        final TedaException e = assertThrows(TedaException.class, () -> teda().execute(
                        ResourceReader.asPath("teda/csv/programmatic"), DocumentType.CSV, cockpit));
        assertTrue(e.getMessage().contains("DOES_NOT_EXIST"), e.getMessage());
        assertTrue(e.getMessage().contains("not in the document"), e.getMessage());
    }

    @Test
    void xlsxWithProgrammaticCockpitRejectsBundledCockpitSheet() {
        // The XLSX fixture ships a Cockpit sheet, so passing a programmatic Cockpit
        // must fail as a collision. Confirms the guard fires for XLSX just like CSV.
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("STUDENT_IN")
                .test("STUDENT_OUT")
                .build();

        final TedaException e = assertThrows(TedaException.class, () -> teda().execute(
                        ResourceReader.asInputStream("teda/xlsx/LOAD_TEST.xlsx"), DocumentType.EXCEL, cockpit));
        assertTrue(e.getMessage().contains("also contains"), e.getMessage());
    }

    private Teda teda() {
        final TedaConfiguration configuration = TedaConfiguration.builder()
                .withDatabase(database.getDataSource())
                .build();
        return new Teda(configuration);
    }

    private void runScript(final String fileName) {
        database.executeQuery(ResourceReader.asString("database/h2/" + fileName));
    }

    private static DataSource dataSource() {
        final JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:cockpitTest;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }
}
