package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * {@link QueryOperation} that orders the whole dataset by a single field.
 *
 * <p>Handles requests carrying a {@code sortBy} but no {@code groupBy}; requests that combine
 * both are served by {@link GroupByOperation}.
 */
@Component
public class SortByOperation implements QueryOperation {

    private final RecordSorter recordSorter;

    public SortByOperation(RecordSorter recordSorter) {
        this.recordSorter = Objects.requireNonNull(recordSorter, "recordSorter");
    }

    @Override
    public boolean supports(QueryRequest request) {
        return request.hasSortBy() && !request.hasGroupBy();
    }

    @Override
    public QueryResult execute(List<JsonNode> records, QueryRequest request) {
        String sortField = request.sortByField()
                .orElseThrow(() -> new IllegalArgumentException("Request has no sortBy field"));
        return new QueryResult.Sorted(recordSorter.sort(records, sortField, request.order()));
    }
}
