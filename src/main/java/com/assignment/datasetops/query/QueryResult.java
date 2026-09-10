package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * Outcome of a {@link QueryOperation}. Sealed so that the API layer can map every
 * variant to its documented response shape exhaustively.
 */
public sealed interface QueryResult permits QueryResult.Grouped, QueryResult.Sorted {

    /**
     * Records bucketed by the value of the group-by field. Insertion order of the map is the
     * order in which groups were first encountered; values inside a group keep their input
     * order unless a sort-by was also requested.
     */
    record Grouped(Map<String, List<JsonNode>> groupedRecords) implements QueryResult {
    }

    /** Records ordered by the sort-by field. */
    record Sorted(List<JsonNode> sortedRecords) implements QueryResult {
    }
}
