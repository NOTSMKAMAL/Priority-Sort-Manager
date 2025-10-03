package com.taskManagers.taskapp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandParserTest {

    private final CommandParser parser = new CommandParser();

    @Test
    void parsesSearchCommand() {
        Command command = parser.parse("search report");
        assertEquals(CommandType.SEARCH, command.type());
        assertEquals("report", command.text());
    }

    @Test
    void parsesFilterOptions() {
        Command command = parser.parse("filter priority=P1 status=open due=week");
        assertEquals(CommandType.FILTER, command.type());
        assertEquals("P1", command.options().get("priority"));
        assertEquals("open", command.options().get("status"));
        assertEquals("week", command.options().get("due"));
    }

    @Test
    void blankInputTriggersBackCommand() {
        Command command = parser.parse("   ");
        assertEquals(CommandType.BACK, command.type());
    }

    @Test
    void hotkeysAreCaseInsensitive() {
        assertEquals(CommandType.NEXT_PAGE, parser.parse("N").type());
        assertEquals(CommandType.TODAY, parser.parse("t").type());
        assertEquals(CommandType.HELP, parser.parse("?").type());
    }

    @Test
    void unknownCommandFallsBack() {
        Command command = parser.parse("launch");
        assertEquals(CommandType.UNKNOWN, command.type());
        assertEquals("launch", command.text());
    }
}
