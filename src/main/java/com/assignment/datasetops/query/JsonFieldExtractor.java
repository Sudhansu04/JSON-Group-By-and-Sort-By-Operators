package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Resolves a dot-separated field path (for example {@code address.city}) inside a JSON record.
 *
 * <p>Resolution walks object members only: each path segment is looked up as a property name of
 * the current node. Array indexes are <em>not</em> supported; a segment applied to an array (or to
 * any non-object node) is treated as missing.
 *
 * <p>A field is considered absent when any segment on the path cannot be resolved, when the
 * resolved value is JSON {@code null}, or when the path itself is {@code null} or blank.
 */
@Component
public class JsonFieldExtractor {

    private static final Pattern PATH_SEPARATOR = Pattern.compile("\\.");

    /**
     * Resolves {@code fieldPath} against {@code record}.
     *
     * @param record    the JSON record to inspect, may be {@code null}
     * @param fieldPath dot-separated path, may be {@code null} or blank
     * @return the resolved value, or {@link Optional#empty()} when missing, JSON null or unresolvable
     */
    public Optional<JsonNode> extract(JsonNode record, String fieldPath) {
        if (record == null || fieldPath == null || fieldPath.isBlank()) {
            return Optional.empty();
        }
        JsonNode current = record;
        for (String segment : PATH_SEPARATOR.split(fieldPath.trim())) {
            current = current.get(segment);
            if (current == null || current.isMissingNode()) {
                return Optional.empty();
            }
        }
        return current.isNull() ? Optional.empty() : Optional.of(current);
    }
}
