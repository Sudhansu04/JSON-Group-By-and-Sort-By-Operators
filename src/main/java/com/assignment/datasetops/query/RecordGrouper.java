package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Buckets JSON records by the value of a (possibly nested) field.
 *
 * <p>Group keys are derived from the field value: {@link JsonNode#asText()} for value nodes
 * (text, numbers, booleans) and {@link JsonNode#toString()} for containers (objects, arrays).
 * Records whose field is missing or JSON {@code null} land in the {@link #MISSING_GROUP_KEY}
 * bucket. Groups appear in the order their key was first encountered and records inside a group
 * keep their input order. The input list is never mutated.
 */
@Component
public class RecordGrouper {

    /** Key of the bucket that receives records without a value for the group-by field. */
    public static final String MISSING_GROUP_KEY = "null";

    private final JsonFieldExtractor fieldExtractor;

    public RecordGrouper(JsonFieldExtractor fieldExtractor) {
        this.fieldExtractor = Objects.requireNonNull(fieldExtractor, "fieldExtractor");
    }

    /**
     * Groups {@code records} by {@code fieldPath}.
     *
     * @param records   records to group, never mutated
     * @param fieldPath dot-path of the group key
     * @return a {@link LinkedHashMap} from group key to the unmodifiable list of its records
     */
    public Map<String, List<JsonNode>> group(List<JsonNode> records, String fieldPath) {
        return records.stream().collect(Collectors.groupingBy(
                record -> groupKey(record, fieldPath),
                LinkedHashMap::new,
                Collectors.toUnmodifiableList()));
    }

    private String groupKey(JsonNode record, String fieldPath) {
        return fieldExtractor.extract(record, fieldPath)
                .map(RecordGrouper::keyOf)
                .orElse(MISSING_GROUP_KEY);
    }

    private static String keyOf(JsonNode value) {
        return value.isContainerNode() ? value.toString() : value.asText();
    }
}
