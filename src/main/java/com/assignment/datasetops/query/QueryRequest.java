package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;

import java.util.Optional;

/**
 * Validated, immutable description of a query against a dataset.
 *
 * <p>Invariants enforced by {@link #of(String, String, String)}:
 * <ul>
 *   <li>At least one of {@code groupBy} / {@code sortBy} is present and non-blank.</li>
 *   <li>{@code order} is only meaningful together with {@code sortBy}.</li>
 *   <li>Field names are trimmed; a blank field name is treated as absent.</li>
 * </ul>
 *
 * @param groupBy field (dot-path allowed, e.g. {@code address.city}) to group by, or {@code null}
 * @param sortBy  field (dot-path allowed) to sort by, or {@code null}
 * @param order   sort direction, never {@code null} (defaults to {@link SortOrder#DEFAULT})
 */
public record QueryRequest(String groupBy, String sortBy, SortOrder order) {

    public QueryRequest {
        if (order == null) {
            order = SortOrder.DEFAULT;
        }
    }

    /**
     * Builds a request from raw query-string parameters, applying validation.
     *
     * @throws InvalidQueryException if the combination of parameters is invalid
     */
    public static QueryRequest of(String groupBy, String sortBy, String order) {
        String group = normalise(groupBy);
        String sort = normalise(sortBy);

        if (group == null && sort == null) {
            throw new InvalidQueryException(
                    "At least one of 'groupBy' or 'sortBy' query parameters must be provided");
        }
        if (sort == null && order != null && !order.isBlank()) {
            throw new InvalidQueryException("'order' can only be used together with 'sortBy'");
        }
        return new QueryRequest(group, sort, SortOrder.fromParameter(order));
    }

    public boolean hasGroupBy() {
        return groupBy != null;
    }

    public boolean hasSortBy() {
        return sortBy != null;
    }

    public Optional<String> groupByField() {
        return Optional.ofNullable(groupBy);
    }

    public Optional<String> sortByField() {
        return Optional.ofNullable(sortBy);
    }

    private static String normalise(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
