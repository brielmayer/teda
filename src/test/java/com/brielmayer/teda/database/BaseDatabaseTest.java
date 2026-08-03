package com.brielmayer.teda.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.brielmayer.teda.exception.TedaException;

public class BaseDatabaseTest {

    @Test
    public void testPlainTableName() {
        assertDoesNotThrow(() -> BaseDatabase.validateTableName("STUDENT"));
        assertDoesNotThrow(() -> BaseDatabase.validateTableName("_student_2"));
    }

    @Test
    public void testQualifiedTableName() {
        assertDoesNotThrow(() -> BaseDatabase.validateTableName("TEDA_SCHEMA.STUDENT"));
        assertDoesNotThrow(() -> BaseDatabase.validateTableName("TEDA_CATALOG.TEDA_SCHEMA.STUDENT"));
    }

    @Test
    public void testTooManyQualifiers() {
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("a.b.c.d"));
    }

    @Test
    public void testEmptyQualifierPart() {
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName(".STUDENT"));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("STUDENT."));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("SCHEMA..STUDENT"));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName(""));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName(null));
    }

    @Test
    public void testInjectionThroughTableName() {
        // the dot separates identifiers; it must not open the door for anything else
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("STUDENT; DROP TABLE STUDENT"));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("SCHEMA.STUDENT; DROP TABLE STUDENT"));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("SCHEMA.\"STUDENT\""));
        assertThrows(TedaException.class, () -> BaseDatabase.validateTableName("SCHEMA.STUDENT WHERE 1=1"));
    }

    @Test
    public void testColumnNamesStayUnqualified() {
        // only table names may be qualified; a dotted column name is still rejected
        assertThrows(TedaException.class, () -> BaseDatabase.validateIdentifier("STUDENT.NAME"));
    }
}
