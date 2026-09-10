package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Total ordering over non-null {@link JsonNode} values used by {@link RecordSorter}.
 *
 * <p>Ordering rules:
 * <ul>
 *   <li>Numbers compare numerically via {@link JsonNode#decimalValue()}, so integral and
 *       floating-point values are mutually comparable ({@code 7 < 30 < 30.5}).</li>
 *   <li>Text compares lexicographically with {@link String#compareTo(String)}.</li>
 *   <li>Booleans compare {@code false < true}.</li>
 *   <li>Every other node type (objects, arrays, binary, POJO, ...) compares by its
 *       {@link JsonNode#toString()} representation.</li>
 *   <li>Values of different kinds are ordered by kind:
 *       {@code NUMBER < TEXT < BOOLEAN < OTHER}.</li>
 * </ul>
 *
 * <p>This comparator is stateless and never receives {@code null}; callers are responsible for
 * handling missing or JSON-null values (see {@link RecordSorter}).
 */
@Component
public class JsonValueComparator implements Comparator<JsonNode> {

    /** Coarse kind of a JSON value; declaration order defines the cross-kind ordering. */
    private enum ValueKind {
        NUMBER,
        TEXT,
        BOOLEAN,
        OTHER;

        static ValueKind of(JsonNode node) {
            if (node.isNumber()) {
                return NUMBER;
            }
            if (node.isTextual()) {
                return TEXT;
            }
            if (node.isBoolean()) {
                return BOOLEAN;
            }
            return OTHER;
        }
    }

    @Override
    public int compare(JsonNode left, JsonNode right) {
        ValueKind leftKind = ValueKind.of(left);
        ValueKind rightKind = ValueKind.of(right);
        if (leftKind != rightKind) {
            return leftKind.compareTo(rightKind);
        }
        return switch (leftKind) {
            case NUMBER -> left.decimalValue().compareTo(right.decimalValue());
            case TEXT -> left.textValue().compareTo(right.textValue());
            case BOOLEAN -> Boolean.compare(left.booleanValue(), right.booleanValue());
            case OTHER -> left.toString().compareTo(right.toString());
        };
    }
}
