package com.brielmayer.teda.database.postgres;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.apache.commons.dbutils.QueryRunner;

/**
 * Binds String and {@code null} parameters as {@link Types#OTHER}, which makes the
 * PostgreSQL driver send them with an unspecified type so the server derives the
 * type from the target column.
 * <p>
 * Without this, the driver types each parameter after its Java class. CSV cells
 * always reach us as Strings, and PostgreSQL is the only supported database that
 * refuses to cast {@code varchar} to {@code integer} on insert, so such a row fails
 * with "column ... is of type integer but expression is of type character varying".
 * This is the per-parameter equivalent of the driver's {@code stringtype=unspecified}
 * connection property, applied without requiring anything of the user's DataSource.
 * <p>
 * Values that already carry a type are bound normally. That covers the
 * {@code Double}, {@code BigDecimal} and {@code LocalDateTime} an XLSX or ODS cell
 * yields. Sending those as unspecified would stringify them first, and the server
 * cannot cast the result back: a {@code Double} of {@code 22.0} would arrive as
 * {@code "22.0"} and be rejected for an {@code integer} column, whereas the
 * {@code float8} the driver sends is cast without complaint.
 */
final class UnspecifiedTypeQueryRunner extends QueryRunner {

    UnspecifiedTypeQueryRunner(final DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public void fillStatement(final PreparedStatement stmt, final Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            final Object param = params[i];
            if (param == null || param instanceof String) {
                stmt.setObject(i + 1, param, Types.OTHER);
            } else {
                stmt.setObject(i + 1, param);
            }
        }
    }
}
