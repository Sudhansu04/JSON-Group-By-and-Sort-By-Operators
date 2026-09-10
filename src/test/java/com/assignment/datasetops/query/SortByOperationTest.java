package com.assignment.datasetops.query;

import org.junit.jupiter.api.Test;

import static com.assignment.datasetops.query.JsonFixtures.assignmentRecords;
import static com.assignment.datasetops.query.JsonFixtures.ids;
import static org.assertj.core.api.Assertions.assertThat;

class SortByOperationTest {

    private final SortByOperation operation =
            new SortByOperation(new RecordSorter(new JsonFieldExtractor(), new JsonValueComparator()));

    @Test
    void supportsSortOnlyRequests() {
        assertThat(operation.supports(QueryRequest.of(null, "age", null))).isTrue();
        assertThat(operation.supports(QueryRequest.of(null, "age", "desc"))).isTrue();
    }

    @Test
    void doesNotSupportGroupRequests() {
        assertThat(operation.supports(QueryRequest.of("department", null, null))).isFalse();
        assertThat(operation.supports(QueryRequest.of("department", "age", null))).isFalse();
    }

    @Test
    void returnsSortedResultAscendingByDefault() {
        QueryResult result = operation.execute(assignmentRecords(), QueryRequest.of(null, "age", null));

        assertThat(result).isInstanceOfSatisfying(QueryResult.Sorted.class,
                sorted -> assertThat(ids(sorted.sortedRecords())).containsExactly(2, 3, 1));
    }

    @Test
    void honoursDescendingOrder() {
        QueryResult result = operation.execute(assignmentRecords(), QueryRequest.of(null, "age", "DESC"));

        assertThat(result).isInstanceOfSatisfying(QueryResult.Sorted.class,
                sorted -> assertThat(ids(sorted.sortedRecords())).containsExactly(1, 3, 2));
    }
}
