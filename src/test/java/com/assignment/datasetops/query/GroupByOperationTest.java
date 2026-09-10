package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.assignment.datasetops.query.JsonFixtures.assignmentRecords;
import static com.assignment.datasetops.query.JsonFixtures.ids;
import static com.assignment.datasetops.query.JsonFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;

class GroupByOperationTest {

    private final GroupByOperation operation;

    GroupByOperationTest() {
        JsonFieldExtractor extractor = new JsonFieldExtractor();
        this.operation = new GroupByOperation(
                new RecordGrouper(extractor),
                new RecordSorter(extractor, new JsonValueComparator()));
    }

    @Test
    void supportsAnyRequestWithGroupBy() {
        assertThat(operation.supports(QueryRequest.of("department", null, null))).isTrue();
        assertThat(operation.supports(QueryRequest.of("department", "age", "desc"))).isTrue();
    }

    @Test
    void doesNotSupportSortOnlyRequests() {
        assertThat(operation.supports(QueryRequest.of(null, "age", null))).isFalse();
    }

    @Test
    void groupsAssignmentExampleByDepartment() {
        QueryResult result = operation.execute(assignmentRecords(), QueryRequest.of("department", null, null));

        assertThat(result).isInstanceOfSatisfying(QueryResult.Grouped.class, grouped -> {
            assertThat(grouped.groupedRecords().keySet()).containsExactly("Engineering", "Marketing");
            assertThat(ids(grouped.groupedRecords().get("Engineering"))).containsExactly(1, 2);
            assertThat(ids(grouped.groupedRecords().get("Marketing"))).containsExactly(3);
        });
    }

    @Test
    void sortsInsideEachGroupWhenSortByIsPresent() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"department\":\"Engineering\",\"age\":30}"),
                json("{\"id\":2,\"department\":\"Marketing\",\"age\":41}"),
                json("{\"id\":3,\"department\":\"Engineering\",\"age\":25}"),
                json("{\"id\":4,\"department\":\"Marketing\",\"age\":35}"),
                json("{\"id\":5,\"department\":\"Engineering\"}"));

        QueryResult result = operation.execute(records, QueryRequest.of("department", "age", "desc"));

        assertThat(result).isInstanceOfSatisfying(QueryResult.Grouped.class, grouped -> {
            assertThat(grouped.groupedRecords().keySet()).containsExactly("Engineering", "Marketing");
            assertThat(ids(grouped.groupedRecords().get("Engineering"))).containsExactly(1, 3, 5);
            assertThat(ids(grouped.groupedRecords().get("Marketing"))).containsExactly(2, 4);
        });
    }

    @Test
    void recordsWithoutGroupFieldLandInNullBucket() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"department\":\"Engineering\"}"),
                json("{\"id\":2}"));

        QueryResult result = operation.execute(records, QueryRequest.of("department", null, null));

        assertThat(result).isInstanceOfSatisfying(QueryResult.Grouped.class, grouped -> {
            assertThat(grouped.groupedRecords().keySet()).containsExactly("Engineering", "null");
            assertThat(ids(grouped.groupedRecords().get("null"))).containsExactly(2);
        });
    }
}
