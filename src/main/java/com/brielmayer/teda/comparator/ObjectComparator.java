package com.brielmayer.teda.comparator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import com.brielmayer.teda.exception.TedaException;

public final class ObjectComparator {

    private ObjectComparator() {}

    public static boolean compare(final Object o1, final Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }

        if (!o1.getClass().equals(o2.getClass())) {
            // Different concrete types cannot be equal; let the caller report
            // the mismatch with row context instead of throwing here.
            return false;
        }

        if (o1 instanceof String) {
            return o1.equals(o2);
        }

        if (o1 instanceof Boolean) {
            return o1.equals(o2);
        }

        if (o1 instanceof LocalDate) {
            return ((LocalDate) o1).isEqual((LocalDate) o2);
        }

        if (o1 instanceof LocalDateTime) {
            return ((LocalDateTime) o1).isEqual((LocalDateTime) o2);
        }

        if (o1 instanceof LocalTime) {
            return o1.equals(o2);
        }

        if (o1 instanceof BigInteger) {
            return o1.equals(o2);
        }

        if (o1 instanceof BigDecimal) {
            return ((BigDecimal) o1).compareTo((BigDecimal) o2) == 0;
        }

        throw TedaException.builder()
                .appendMessage("Unsupported data type %s", o1.getClass().getSimpleName())
                .appendMessage("Comparable types are: String, Boolean, LocalDate, LocalDateTime, "
                        + "LocalTime, BigInteger and BigDecimal.")
                .build();
    }
}
