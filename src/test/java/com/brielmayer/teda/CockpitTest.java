package com.brielmayer.teda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Action;

class CockpitTest {

    @Test
    void stepsFollowCallOrder() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("STUDENT")
                .load("STUDENT_IN")
                .execute("job")
                .test("STUDENT_OUT")
                .build();

        final List<Cockpit.Step> steps = cockpit.getSteps();
        assertEquals(4, steps.size());
        assertStep(steps.get(0), Action.TRUNCATE, "STUDENT");
        assertStep(steps.get(1), Action.LOAD, "STUDENT_IN");
        assertStep(steps.get(2), Action.EXECUTE, "job");
        assertStep(steps.get(3), Action.TEST, "STUDENT_OUT");
    }

    @Test
    void varargsExpandInCallOrder() {
        final Cockpit cockpit = Cockpit.builder()
                .truncate("A", "B", "C")
                .load("A_IN", "B_IN")
                .build();

        final List<Cockpit.Step> steps = cockpit.getSteps();
        assertEquals(5, steps.size());
        assertStep(steps.get(0), Action.TRUNCATE, "A");
        assertStep(steps.get(1), Action.TRUNCATE, "B");
        assertStep(steps.get(2), Action.TRUNCATE, "C");
        assertStep(steps.get(3), Action.LOAD, "A_IN");
        assertStep(steps.get(4), Action.LOAD, "B_IN");
    }

    @Test
    void multipleCyclesAreAppendedInOrder() {
        final Cockpit cockpit = Cockpit.builder()
                .load("A_IN")
                .execute("first")
                .test("A_OUT")
                .load("B_IN")
                .execute("second")
                .test("B_OUT")
                .build();

        final List<Cockpit.Step> steps = cockpit.getSteps();
        assertEquals(6, steps.size());
        assertStep(steps.get(3), Action.LOAD, "B_IN");
        assertStep(steps.get(4), Action.EXECUTE, "second");
        assertStep(steps.get(5), Action.TEST, "B_OUT");
    }

    @Test
    void nameIsOptional() {
        final Cockpit named = Cockpit.builder().name("Scenario").truncate("A").build();
        final Cockpit unnamed = Cockpit.builder().truncate("A").build();

        assertEquals("Scenario", named.getName());
        assertNull(unnamed.getName());
    }

    @Test
    void stepsListIsImmutable() {
        final Cockpit cockpit = Cockpit.builder().truncate("A").build();
        assertThrows(UnsupportedOperationException.class,
                () -> cockpit.getSteps().add(new Cockpit.Step(Action.LOAD, "X")));
    }

    @Test
    void buildRejectsEmptyCockpit() {
        final TedaException e = assertThrows(TedaException.class,
                () -> Cockpit.builder().build());
        assertTrue(e.getMessage().contains("no steps"), e.getMessage());
    }

    @Test
    void actionMethodRejectsEmptyVarargs() {
        final TedaException e = assertThrows(TedaException.class,
                () -> Cockpit.builder().load());
        assertTrue(e.getMessage().contains("at least one value"), e.getMessage());
    }

    @Test
    void actionMethodRejectsNullValue() {
        assertThrows(TedaException.class,
                () -> Cockpit.builder().truncate("A", null, "B"));
    }

    @Test
    void actionMethodRejectsEmptyValue() {
        assertThrows(TedaException.class,
                () -> Cockpit.builder().load(""));
    }

    private static void assertStep(final Cockpit.Step step, final Action action, final String value) {
        assertEquals(action, step.getAction());
        assertEquals(value, step.getValue());
    }
}
