package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static com.assignment.datasetops.query.JsonFixtures.json;
import static org.assertj.core.api.Assertions.assertThat;

class JsonFieldExtractorTest {

    private final JsonFieldExtractor extractor = new JsonFieldExtractor();

    private final JsonNode record = json("""
            {"id": 1, "age": 30, "active": true, "nickname": null,
             "address": {"city": "Berlin", "geo": {"lat": 52.5}},
             "tags": ["a", "b"]}
            """);

    @Test
    void extractsTopLevelField() {
        Optional<JsonNode> value = extractor.extract(record, "age");

        assertThat(value).isPresent();
        assertThat(value.get().intValue()).isEqualTo(30);
    }

    @Test
    void extractsNestedFieldViaDotPath() {
        assertThat(extractor.extract(record, "address.city")).map(JsonNode::textValue).contains("Berlin");
        assertThat(extractor.extract(record, "address.geo.lat")).map(JsonNode::doubleValue).contains(52.5);
    }

    @Test
    void extractsContainerNodes() {
        assertThat(extractor.extract(record, "address")).map(JsonNode::isObject).contains(true);
        assertThat(extractor.extract(record, "tags")).map(JsonNode::isArray).contains(true);
    }

    @Test
    void missingFieldIsEmpty() {
        assertThat(extractor.extract(record, "salary")).isEmpty();
        assertThat(extractor.extract(record, "address.zip")).isEmpty();
        assertThat(extractor.extract(record, "address.geo.lat.deeper")).isEmpty();
    }

    @Test
    void jsonNullIsTreatedAsMissing() {
        assertThat(extractor.extract(record, "nickname")).isEmpty();
    }

    @Test
    void arrayIndexesAreNotSupported() {
        assertThat(extractor.extract(record, "tags.0")).isEmpty();
        assertThat(extractor.extract(record, "tags[0]")).isEmpty();
    }

    @Test
    void pathThroughScalarIsMissing() {
        assertThat(extractor.extract(record, "age.value")).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void blankPathIsTreatedAsMissing(String path) {
        assertThat(extractor.extract(record, path)).isEmpty();
    }

    @Test
    void surroundingWhitespaceOnPathIsIgnored() {
        assertThat(extractor.extract(record, "  address.city ")).map(JsonNode::textValue).contains("Berlin");
    }

    @Test
    void nullRecordIsMissing() {
        assertThat(extractor.extract(null, "age")).isEmpty();
    }
}
