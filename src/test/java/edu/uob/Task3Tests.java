package edu.uob;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class Task3Tests {
    private DBServer server;

    @BeforeEach
    public void setup() {
        server = new DBServer();
    }

    @Test
    public void testReadingPeopleFile() {
        String response = server.handleCommand("hello");
        assertTrue(response.contains("Bob"), "Response should contain content from the file");
        assertTrue(response.contains("id"), "Response should contain table headers");
    }
    @Test
    public void testEmptyCommand() {
        String response = server.handleCommand("");
        assertTrue(response.length() > 0, "Server should return something even with empty command");
    }
}
