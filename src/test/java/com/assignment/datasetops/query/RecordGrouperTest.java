package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.assignment.datasetops.query.JsonFixtures.assignmentRecords;
import static com.assignment.datasetops.query.JsonFixtures.ids;
import static com.assignment.datasetops.query.JsonFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;

class RecordGrouperTest {

    private final RecordGrouper grouper = new RecordGrouper(new JsonFieldExtractor());

    @Test
    void groupsAssignmentExampleByDepartment() {
        Map<String, List<JsonNode>> groups = grouper.group(assignmentRecords(), "department");

        assertThat(groups.keySet()).containsExactly("Engineering", "Marketing");
        assertThat(ids(groups.get("Engineering"))).containsExactly(1, 2);
        assertThat(ids(groups.get("Marketing"))).containsExactly(3);
    }

    @Test
    void preservesFirstSeenOrderOfGroups() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"team\":\"zeta\"}"),
                json("{\"id\":2,\"team\":\"alpha\"}"),
                json("{\"id\":3,\"team\":\"zeta\"}"),
                json("{\"id\":4,\"team\":\"mid\"}"));

        Map<String, List<JsonNode>> groups = grouper.group(records, "team");

        assertThat(groups).isInstanceOf(LinkedHashMap.class);
        assertThat(groups.keySet()).containsExactly("zeta", "alpha", "mid");
        assertThat(ids(groups.get("zeta"))).containsExactly(1, 3);
    }

    @Test
    void missingAndNullValuesShareTheNullBucket() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"dept\":\"A\"}"),
                json("{\"id\":2}"),
                json("{\"id\":3,\"dept\":null}"),
                json("{\"id\":4,\"dept\":\"A\"}"));

        Map<String, List<JsonNode>> groups = grouper.group(records, "dept");

        assertThat(groups.keySet()).containsExactly("A", RecordGrouper.MISSING_GROUP_KEY);
        assertThat(RecordGrouper.MISSING_GROUP_KEY).isEqualTo("null");
        assertThat(ids(groups.get("null"))).containsExactly(2, 3);
    }

    @Test
    void groupsByNestedDotPath() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"address\":{\"city\":\"Berlin\"}}"),
                json("{\"id\":2,\"address\":{\"city\":\"Munich\"}}"),
                json("{\"id\":3,\"address\":{\"city\":\"Berlin\"}}"));

        Map<String, List<JsonNode>> groups = grouper.group(records, "address.city");

        assertThat(groups.keySet()).containsExactly("Berlin", "Munich");
        assertThat(ids(groups.get("Berlin"))).containsExactly(1, 3);
    }

    @Test
    void usesAsTextForScalarKeys() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"v\":30}"),
                json("{\"id\":2,\"v\":true}"),
                json("{\"id\":3,\"v\":2.5}"),
                json("{\"id\":4,\"v\":\"30\"}"));

        Map<String, List<JsonNode>> groups = grouper.group(records, "v");

        assertThat(groups.keySet()).containsExactly("30", "true", "2.5");
        assertThat(ids(groups.get("30"))).containsExactly(1, 4);
    }

    @Test
    void usesToStringForContainerKeys() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"v\":{\"a\":1}}"),
                json("{\"id\":2,\"v\":[1,2]}"),
                json("{\"id\":3,\"v\":{\"a\":1}}"));

        Map<String, List<JsonNode>> groups = grouper.group(records, "v");

        assertThat(groups.keySet()).containsExactly("{\"a\":1}", "[1,2]");
        assertThat(ids(groups.get("{\"a\":1}"))).containsExactly(1, 3);
    }

    @Test
    void handlesEmptyInput() {
        assertThat(grouper.group(List.of(), "department")).isEmpty();
    }
}
