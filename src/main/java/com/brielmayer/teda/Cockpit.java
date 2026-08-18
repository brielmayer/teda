package com.brielmayer.teda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Action;

/**
 * A programmatic Cockpit built in Java code instead of read from a spreadsheet.
 * Steps run in the exact order the builder methods were called. Passing a
 * {@code Cockpit} to {@link Teda#execute} suppresses the file based Cockpit
 * sheet; combining both is rejected as a collision.
 */
public final class Cockpit {

    private final String name;
    private final List<Step> steps;

    private Cockpit(final String name, final List<Step> steps) {
        this.name = name;
        this.steps = Collections.unmodifiableList(steps);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public static final class Step {

        private final Action action;
        private final String value;

        Step(final Action action, final String value) {
            this.action = action;
            this.value = value;
        }

        public Action getAction() {
            return action;
        }

        public String getValue() {
            return value;
        }
    }

    public static final class Builder {

        private String name;
        private final List<Step> steps = new ArrayList<>();

        Builder() {}

        public Builder name(final String scenarioName) {
            this.name = scenarioName;
            return this;
        }

        public Builder truncate(final String... tables) {
            return append(Action.TRUNCATE, tables);
        }

        public Builder load(final String... tables) {
            return append(Action.LOAD, tables);
        }

        public Builder execute(final String... commands) {
            return append(Action.EXECUTE, commands);
        }

        public Builder test(final String... tables) {
            return append(Action.TEST, tables);
        }

        public Cockpit build() {
            if (steps.isEmpty()) {
                throw TedaException.builder()
                        .appendMessage("Cockpit contains no steps")
                        .appendMessage("Add at least one truncate/load/execute/test call before build().")
                        .build();
            }
            return new Cockpit(name, new ArrayList<>(steps));
        }

        private Builder append(final Action action, final String[] values) {
            if (values == null || values.length == 0) {
                throw TedaException.builder()
                        .appendMessage(
                                "Cockpit.Builder.%s requires at least one value",
                                action.name().toLowerCase())
                        .build();
            }
            for (final String value : values) {
                if (value == null || value.isEmpty()) {
                    throw TedaException.builder()
                            .appendMessage(
                                    "Cockpit.Builder.%s does not accept null or empty values",
                                    action.name().toLowerCase())
                            .build();
                }
                steps.add(new Step(action, value));
            }
            return this;
        }
    }
}
