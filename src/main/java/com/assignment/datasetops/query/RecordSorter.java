package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Sorts JSON records by the value of a (possibly nested) field.
 *
 * <p>The sort is stable: records with equal keys keep their relative input order. Records whose
 * field is missing or JSON {@code null} are always placed after all records that have a value,
 * regardless of the requested {@link SortOrder}, and also keep their relative input order.
 * The input list is never mutated.
 */
@Component
public class RecordSorter {

    private final JsonFieldExtractor fieldExtractor;
    private final JsonValueComparator valueComparator;

    public RecordSorter(JsonFieldExtractor fieldExtractor, JsonValueComparator valueComparator) {
        this.fieldExtractor = Objects.requireNonNull(fieldExtractor, "fieldExtractor");
        this.valueComparator = Objects.requireNonNull(valueComparator, "valueComparator");
    }

    /**
     * Sorts {@code records} by {@code fieldPath}.
     *
     * @param records   records to sort, never mutated
     * @param fieldPath dot-path of the sort key
     * @param order     sort direction
     * @return a new unmodifiable list containing every input record exactly once
     */
    public List<JsonNode> sort(List<JsonNode> records, String fieldPath, SortOrder order) {
        List<KeyedRecord> keyed = new ArrayList<>(records.size());
        List<JsonNode> withoutKey = new ArrayList<>();
        for (JsonNode record : records) {
            Optional<JsonNode> key = fieldExtractor.extract(record, fieldPath);
            if (key.isPresent()) {
                keyed.add(new KeyedRecord(record, key.get()));
            } else {
                withoutKey.add(record);
            }
        }
        keyed.sort(Comparator.comparing(KeyedRecord::key, directed(order)));

        return Stream.concat(keyed.stream().map(KeyedRecord::record), withoutKey.stream()).toList();
    }

    private Comparator<JsonNode> directed(SortOrder order) {
        return order.isDescending() ? valueComparator.reversed() : valueComparator;
    }

    /** A record paired with its already-extracted sort key, so the key is resolved only once. */
    private record KeyedRecord(JsonNode record, JsonNode key) {
    }
}
