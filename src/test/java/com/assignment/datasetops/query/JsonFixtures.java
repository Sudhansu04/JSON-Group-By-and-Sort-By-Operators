package com.assignment.datasetops.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** Shared JSON test fixtures for the {@code query} package unit tests. */
final class JsonFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonFixtures() {
    }

    /** Parses a JSON literal into a tree; test fixtures are constants so parse errors are bugs. */
    static JsonNode json(String literal) {
        try {
            return MAPPER.readTree(literal);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid test fixture: " + literal, e);
        }
    }

    /** The three records used as examples in the assignment. */
    static List<JsonNode> assignmentRecords() {
        return List.of(
                json("{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}"),
                json("{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"}"),
                json("{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"}"));
    }

    /** Extracts the {@code id} of every record, in order, to make ordering assertions readable. */
    static List<Integer> ids(List<JsonNode> records) {
        return records.stream().map(record -> record.get("id").intValue()).toList();
    }
}
