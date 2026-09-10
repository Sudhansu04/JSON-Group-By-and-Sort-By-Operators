package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link QueryOperation} that buckets the dataset by one field and, when the request also carries
 * a {@code sortBy}, orders the records inside every bucket by that second field.
 *
 * <p>Composes {@link RecordGrouper} and {@link RecordSorter} rather than re-implementing either.
 */
@Component
public class GroupByOperation implements QueryOperation {

    private final RecordGrouper recordGrouper;
    private final RecordSorter recordSorter;

    public GroupByOperation(RecordGrouper recordGrouper, RecordSorter recordSorter) {
        this.recordGrouper = Objects.requireNonNull(recordGrouper, "recordGrouper");
        this.recordSorter = Objects.requireNonNull(recordSorter, "recordSorter");
    }

    @Override
    public boolean supports(QueryRequest request) {
        return request.hasGroupBy();
    }

    @Override
    public QueryResult execute(List<JsonNode> records, QueryRequest request) {
        String groupField = request.groupByField()
                .orElseThrow(() -> new IllegalArgumentException("Request has no groupBy field"));
        Map<String, List<JsonNode>> groups = recordGrouper.group(records, groupField);

        return new QueryResult.Grouped(request.sortByField()
                .map(sortField -> sortEachGroup(groups, sortField, request.order()))
                .orElse(groups));
    }

    private Map<String, List<JsonNode>> sortEachGroup(
            Map<String, List<JsonNode>> groups, String sortField, SortOrder order) {
        Map<String, List<JsonNode>> sorted = new LinkedHashMap<>();
        groups.forEach((key, members) -> sorted.put(key, recordSorter.sort(members, sortField, order)));
        return sorted;
    }
}
