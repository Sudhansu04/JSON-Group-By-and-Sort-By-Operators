package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.assignment.datasetops.query.JsonFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;

class JsonValueComparatorTest {

    private final JsonValueComparator comparator = new JsonValueComparator();

    @Test
    void comparesIntegersNumerically() {
        assertThat(comparator.compare(json("7"), json("30"))).isNegative();
        assertThat(comparator.compare(json("30"), json("7"))).isPositive();
        assertThat(comparator.compare(json("30"), json("30"))).isZero();
    }

    @Test
    void comparesMixedNumericTypesNumerically() {
        assertThat(comparator.compare(json("30"), json("30.5"))).isNegative();
        assertThat(comparator.compare(json("30.5"), json("7"))).isPositive();
        assertThat(comparator.compare(json("30"), json("30.0"))).isZero();
        assertThat(comparator.compare(json("9007199254740993"), json("9007199254740992.0"))).isPositive();
    }

    @Test
    void comparesTextLexicographically() {
        assertThat(comparator.compare(json("\"apple\""), json("\"banana\""))).isNegative();
        assertThat(comparator.compare(json("\"Zoe\""), json("\"adam\""))).isNegative();
        assertThat(comparator.compare(json("\"same\""), json("\"same\""))).isZero();
    }

    @Test
    void textIsNotComparedAsNumber() {
        assertThat(comparator.compare(json("\"10\""), json("\"9\""))).isNegative();
    }

    @Test
    void comparesBooleansFalseBeforeTrue() {
        assertThat(comparator.compare(json("false"), json("true"))).isNegative();
        assertThat(comparator.compare(json("true"), json("false"))).isPositive();
        assertThat(comparator.compare(json("true"), json("true"))).isZero();
    }

    @Test
    void comparesContainersByStringForm() {
        assertThat(comparator.compare(json("{\"a\":1}"), json("{\"a\":2}"))).isNegative();
        assertThat(comparator.compare(json("[1]"), json("[2]"))).isNegative();
        assertThat(comparator.compare(json("{\"a\":1}"), json("{\"a\":1}"))).isZero();
    }

    @Test
    void ordersMixedTypesByRank() {
        List<JsonNode> shuffled = new ArrayList<>(List.of(
                json("{\"k\":1}"), json("true"), json("\"text\""), json("[1]"), json("false"), json("42")));

        shuffled.sort(comparator);

        assertThat(shuffled).extracting(JsonNode::toString)
                .containsExactly("42", "\"text\"", "false", "true", "[1]", "{\"k\":1}");
    }

    @Test
    void isConsistentWithReversal() {
        JsonNode number = json("1");
        JsonNode text = json("\"a\"");

        assertThat(comparator.compare(number, text)).isEqualTo(-comparator.compare(text, number));
    }
}
