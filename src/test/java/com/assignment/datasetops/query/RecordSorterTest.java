package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.assignment.datasetops.query.JsonFixtures.assignmentRecords;
import static com.assignment.datasetops.query.JsonFixtures.ids;
import static com.assignment.datasetops.query.JsonFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordSorterTest {

    private final RecordSorter sorter = new RecordSorter(new JsonFieldExtractor(), new JsonValueComparator());

    @Test
    void sortsAssignmentExampleByAgeAscending() {
        List<JsonNode> sorted = sorter.sort(assignmentRecords(), "age", SortOrder.ASC);

        assertThat(ids(sorted)).containsExactly(2, 3, 1);
    }

    @Test
    void sortsDescending() {
        List<JsonNode> sorted = sorter.sort(assignmentRecords(), "age", SortOrder.DESC);

        assertThat(ids(sorted)).containsExactly(1, 3, 2);
    }

    @Test
    void sortsTextFields() {
        List<JsonNode> sorted = sorter.sort(assignmentRecords(), "name", SortOrder.ASC);

        assertThat(ids(sorted)).containsExactly(3, 2, 1);
    }

    @Test
    void isStableForEqualKeys() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"grade\":\"B\"}"),
                json("{\"id\":2,\"grade\":\"A\"}"),
                json("{\"id\":3,\"grade\":\"B\"}"),
                json("{\"id\":4,\"grade\":\"A\"}"),
                json("{\"id\":5,\"grade\":\"B\"}"));

        assertThat(ids(sorter.sort(records, "grade", SortOrder.ASC))).containsExactly(2, 4, 1, 3, 5);
        assertThat(ids(sorter.sort(records, "grade", SortOrder.DESC))).containsExactly(1, 3, 5, 2, 4);
    }

    @Test
    void placesMissingAndNullValuesLastInBothDirections() {
        List<JsonNode> records = List.of(
                json("{\"id\":1}"),
                json("{\"id\":2,\"age\":40}"),
                json("{\"id\":3,\"age\":null}"),
                json("{\"id\":4,\"age\":20}"));

        assertThat(ids(sorter.sort(records, "age", SortOrder.ASC))).containsExactly(4, 2, 1, 3);
        assertThat(ids(sorter.sort(records, "age", SortOrder.DESC))).containsExactly(2, 4, 1, 3);
    }

    @Test
    void sortsByNestedDotPath() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"address\":{\"city\":\"Munich\"}}"),
                json("{\"id\":2,\"address\":{\"city\":\"Berlin\"}}"),
                json("{\"id\":3,\"address\":{\"city\":\"Cologne\"}}"));

        assertThat(ids(sorter.sort(records, "address.city", SortOrder.ASC))).containsExactly(2, 3, 1);
    }

    @Test
    void sortsMixedNumericTypesNumerically() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"score\":30}"),
                json("{\"id\":2,\"score\":30.5}"),
                json("{\"id\":3,\"score\":7}"));

        assertThat(ids(sorter.sort(records, "score", SortOrder.ASC))).containsExactly(3, 1, 2);
    }

    @Test
    void ordersMixedTypesByRankBeforeMissing() {
        List<JsonNode> records = List.of(
                json("{\"id\":1,\"v\":true}"),
                json("{\"id\":2,\"v\":\"b\"}"),
                json("{\"id\":3}"),
                json("{\"id\":4,\"v\":{\"x\":1}}"),
                json("{\"id\":5,\"v\":10}"));

        assertThat(ids(sorter.sort(records, "v", SortOrder.ASC))).containsExactly(5, 2, 1, 4, 3);
        assertThat(ids(sorter.sort(records, "v", SortOrder.DESC))).containsExactly(4, 1, 2, 5, 3);
    }

    @Test
    void doesNotMutateInputAndReturnsUnmodifiableList() {
        List<JsonNode> input = new ArrayList<>(assignmentRecords());
        List<JsonNode> snapshot = List.copyOf(input);

        List<JsonNode> sorted = sorter.sort(input, "age", SortOrder.ASC);

        assertThat(input).containsExactlyElementsOf(snapshot);
        assertThat(sorted).isNotSameAs(input);
        assertThatThrownBy(() -> sorted.add(json("{}"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void handlesEmptyInput() {
        assertThat(sorter.sort(List.of(), "age", SortOrder.ASC)).isEmpty();
    }

    @Test
    void unknownFieldKeepsInputOrder() {
        assertThat(ids(sorter.sort(assignmentRecords(), "salary", SortOrder.DESC))).containsExactly(1, 2, 3);
    }
}
