package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.InvalidRecordException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

/**
 * Structural validation of a submitted JSON record.
 *
 * <p>A record is acceptable when it is a non-empty JSON object whose {@code id} field is an integral,
 * non-negative number that fits in a {@code long}. Floating point values such as {@code 1.0} are
 * rejected even though they are numerically integral, because the id is meant to be an identifier
 * rather than a measurement.
 */
@Component
class RecordPayloadValidator {

    static final String ID_FIELD = "id";

    /**
     * Validates the payload and returns its record id.
     *
     * @throws InvalidRecordException if the payload violates any structural rule
     */
    long requireRecordId(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new InvalidRecordException("Record must be a JSON object");
        }
        if (payload.isEmpty()) {
            throw new InvalidRecordException("Record must not be empty");
        }
        JsonNode id = payload.get(ID_FIELD);
        if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.asLong() < 0) {
            throw new InvalidRecordException("Record must contain a non-negative integral 'id' field");
        }
        return id.asLong();
    }
}
