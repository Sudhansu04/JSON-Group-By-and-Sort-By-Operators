package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;

import java.util.Arrays;
import java.util.Locale;

/** Direction of a sort-by operation. Parsed case-insensitively from the {@code order} query parameter. */
public enum SortOrder {
    ASC,
    DESC;

    public static final SortOrder DEFAULT = ASC;

    /**
     * @param raw value of the {@code order} query parameter, may be {@code null} or blank
     * @return the matching order, or {@link #DEFAULT} when {@code raw} is null/blank
     * @throws InvalidQueryException when the value is neither {@code asc} nor {@code desc}
     */
    public static SortOrder fromParameter(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .filter(order -> order.name().equalsIgnoreCase(raw.trim()))
                .findFirst()
                .orElseThrow(() -> new InvalidQueryException(
                        "Invalid order '" + raw + "'. Allowed values: asc, desc"));
    }

    public boolean isDescending() {
        return this == DESC;
    }

    public String toParameter() {
        return name().toLowerCase(Locale.ROOT);
    }
}
