package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.InvalidRecordException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordPayloadValidatorTest {

    private static final String ID_MESSAGE = "Record must contain a non-negative integral 'id' field";

    private final RecordPayloadValidator validator = new RecordPayloadValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"[{\"id\":1}]", "\"text\"", "42", "null"})
    void rejectsNonObjectPayloads(String json) {
        assertInvalid(json, "Record must be a JSON object");
    }

    @Test
    void rejectsNullPayload() {
        assertThatThrownBy(() -> validator.requireRecordId(null))
                .isInstanceOf(InvalidRecordException.class)
                .hasMessage("Record must be a JSON object");
    }

    @Test
    void rejectsEmptyObject() {
        assertInvalid("{}", "Record must not be empty");
    }

    @Test
    void rejectsMissingId() {
        assertInvalid("{\"name\":\"John\"}", ID_MESSAGE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"id\":\"1\"}",                      // string
            "{\"id\":1.0}",                        // float, even if integral
            "{\"id\":-1}",                         // negative
            "{\"id\":null}",                       // JSON null
            "{\"id\":true}",                       // boolean
            "{\"id\":9223372036854775808}"         // Long.MAX_VALUE + 1
    })
    void rejectsIdsThatAreNotNonNegativeLongs(String json) {
        assertInvalid(json, ID_MESSAGE);
    }

    @Test
    void acceptsZeroAndLargeIds() {
        assertThat(validator.requireRecordId(read("{\"id\":0}"))).isZero();
        assertThat(validator.requireRecordId(read("{\"id\":9223372036854775807}"))).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void returnsIdOfValidRecord() {
        assertThat(validator.requireRecordId(read("{\"id\":42,\"name\":\"John\"}"))).isEqualTo(42L);
    }

    private void assertInvalid(String json, String message) {
        JsonNode payload = read(json);
        assertThatThrownBy(() -> validator.requireRecordId(payload))
                .isInstanceOf(InvalidRecordException.class)
                .hasMessage(message);
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
