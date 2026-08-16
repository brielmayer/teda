package com.brielmayer.teda.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.brielmayer.teda.exception.TedaException;
import com.brielmayer.teda.model.Action;
import com.brielmayer.teda.model.Document;
import com.brielmayer.teda.model.Sheet;
import com.brielmayer.teda.model.Table;
import com.brielmayer.teda.parser.Parser;

/**
 * Extracts the cockpit table from a {@link Document} and turns its cells into
 * an ordered list of {@link Command}s. Empty cells are skipped. Unknown action
 * names throw a {@link TedaException} with the list of valid actions.
 */
final class CockpitReader {

    List<Command> read(final Document document) {
        final Table cockpit = extractCockpit(document);

        final List<Command> commands = new ArrayList<>();
        for (final Map<String, Object> row : cockpit.getData()) {
            for (final Map.Entry<String, Object> entry : row.entrySet()) {
                final String header = entry.getKey();
                final Object rawValue = entry.getValue();

                // if cell is empty, no action will be performed
                if (rawValue == null) {
                    continue;
                }
                if (!(rawValue instanceof String)) {
                    throw TedaException.builder()
                            .appendMessage("Invalid cell in sheet \"%s\", column %s", Parser.COCKPIT, header)
                            .appendMessage(
                                    "Expected a sheet or table name as text, but found %s (%s)",
                                    rawValue.getClass().getSimpleName(), rawValue)
                            .build();
                }
                final String value = (String) rawValue;
                if (value.isEmpty()) {
                    continue;
                }

                commands.add(new Command(parseAction(header), value));
            }
        }

        // an empty cockpit would let the whole run pass without doing anything
        if (commands.isEmpty()) {
            throw TedaException.builder()
                    .appendMessage("Sheet \"%s\" contains no actions", Parser.COCKPIT)
                    .appendMessage("The #Teda table needs at least one non-empty cell below a "
                            + "TRUNCATE, LOAD, EXECUTE or TEST column.")
                    .build();
        }
        return commands;
    }

    private static Table extractCockpit(final Document document) {
        final Sheet cockpitSheet = document.getSheets().get(Parser.COCKPIT);
        if (cockpitSheet == null) {
            throw TedaException.builder()
                    .appendMessage("Missing sheet \"%s\"", Parser.COCKPIT)
                    .appendMessage(
                            "The first sheet in the workbook must be named \"%s\" and contain a #Teda table.",
                            Parser.COCKPIT)
                    .build();
        }
        if (cockpitSheet.getTables() == null || cockpitSheet.getTables().isEmpty()) {
            throw TedaException.builder()
                    .appendMessage("Sheet \"%s\" contains no #Teda table", Parser.COCKPIT)
                    .build();
        }
        return cockpitSheet.getTables().values().iterator().next();
    }

    private static Action parseAction(final String header) {
        try {
            return Action.valueOf(header);
        } catch (final IllegalArgumentException e) {
            throw TedaException.builder()
                    .appendMessage("Unknown action \"%s\" in sheet \"%s\"", header, Parser.COCKPIT)
                    .appendMessage(
                            "The column headers of the #Teda table must be one of: %s",
                            Arrays.toString(Action.values()))
                    .cause(e)
                    .build();
        }
    }
}
