package com.brielmayer.teda.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.brielmayer.teda.Cockpit;
import com.brielmayer.teda.database.BaseDatabase;
import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.handler.IExecutionHandler;
import com.brielmayer.teda.handler.ILoadHandler;
import com.brielmayer.teda.handler.ITestHandler;
import com.brielmayer.teda.handler.ITruncateHandler;
import com.brielmayer.teda.model.Action;
import com.brielmayer.teda.model.Document;
import com.brielmayer.teda.model.Sheet;
import com.brielmayer.teda.parser.Parser;

public class TedaExecutor {

    private final BaseDatabase loadDatabase;
    private final BaseDatabase testDatabase;
    private final ITruncateHandler truncateHandler;
    private final ILoadHandler loadHandler;
    private final IExecutionHandler executionHandler;
    private final ITestHandler testHandler;
    private final CockpitReader cockpitReader = new CockpitReader();

    TedaExecutor(final TedaExecutorBuilder builder) {
        this.loadDatabase = builder.getLoadDatabase();
        this.testDatabase = builder.getTestDatabase();
        this.truncateHandler = builder.getTruncateHandler();
        this.loadHandler = builder.getLoadHandler();
        this.executionHandler = builder.getExecutionHandler();
        this.testHandler = builder.getTestHandler();
    }

    public static TedaExecutorBuilder builder() {
        return new TedaExecutorBuilder();
    }

    public void execute(final Document document) {
        execute(document, null);
    }

    public void execute(final Document document, final Cockpit cockpit) {
        final List<Command> commands = commandsFor(document, cockpit);
        for (final Command command : commands) {
            dispatch(command, document);
        }
    }

    private List<Command> commandsFor(final Document document, final Cockpit cockpit) {
        if (cockpit == null) {
            return cockpitReader.read(document);
        }
        if (document.getSheets().containsKey(Parser.COCKPIT)) {
            throw TedaException.builder()
                    .appendMessage("Cockpit was provided programmatically, but the document also contains a \"%s\" sheet",
                            Parser.COCKPIT)
                    .appendMessage("Pick one: remove the sheet from the file, or drop the Cockpit argument.")
                    .build();
        }
        validateReferences(document, cockpit);
        final List<Command> commands = new ArrayList<>(cockpit.getSteps().size());
        for (final Cockpit.Step step : cockpit.getSteps()) {
            commands.add(new Command(step.getAction(), step.getValue()));
        }
        return commands;
    }

    private static void validateReferences(final Document document, final Cockpit cockpit) {
        final List<String> missing = new ArrayList<>();
        for (final Cockpit.Step step : cockpit.getSteps()) {
            final Action action = step.getAction();
            if (action != Action.LOAD && action != Action.TEST) {
                continue;
            }
            final String sheetName = step.getValue();
            if (!document.getSheets().containsKey(sheetName) && !missing.contains(sheetName)) {
                missing.add(sheetName);
            }
        }
        if (!missing.isEmpty()) {
            throw TedaException.builder()
                    .appendMessage("Cockpit references sheets that are not in the document: %s", missing)
                    .appendMessage("Available sheets: %s", new TreeSet<>(document.getSheets().keySet()))
                    .build();
        }
    }

    private void dispatch(final Command command, final Document document) {
        final String value = command.getValue();
        switch (command.getAction()) {
            case TRUNCATE:
                truncateHandler.truncate(testDatabase, value);
                break;
            case LOAD:
                requireSheet(document, value, Action.LOAD)
                        .getTables()
                        .values()
                        .forEach(table -> loadHandler.load(loadDatabase, table));
                break;
            case EXECUTE:
                executionHandler.execute(value);
                break;
            case TEST:
                requireSheet(document, value, Action.TEST)
                        .getTables()
                        .values()
                        .forEach(table -> testHandler.test(testDatabase, table));
                break;
            default:
                throw TedaException.builder()
                        .appendMessage("Unhandled action: %s", command.getAction())
                        .build();
        }
    }

    private static Sheet requireSheet(final Document document, final String name, final Action action) {
        final Sheet sheet = document.getSheets().get(name);
        if (sheet == null) {
            throw TedaException.builder()
                    .appendMessage("The %s action references the unknown sheet \"%s\"", action, name)
                    .appendMessage(
                            "Available sheets: %s",
                            new TreeSet<>(document.getSheets().keySet()))
                    .build();
        }
        return sheet;
    }
}
