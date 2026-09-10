package com.assignment.datasetops.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonNodeAttributeConverterTest {

    private final JsonNodeAttributeConverter converter = new JsonNodeAttributeConverter();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void roundTripsNestedJson() throws Exception {
        JsonNode original = objectMapper.readTree(
                "{\"id\":1,\"name\":\"John\",\"address\":{\"city\":\"Delhi\"},\"tags\":[\"a\",\"b\"]}");

        String column = converter.convertToDatabaseColumn(original);
        JsonNode restored = converter.convertToEntityAttribute(column);

        assertThat(column).isEqualTo("{\"id\":1,\"name\":\"John\",\"address\":{\"city\":\"Delhi\"},\"tags\":[\"a\",\"b\"]}");
        assertThat(restored).isEqualTo(original);
    }

    @Test
    void nullAttributeMapsToNullColumn() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void nullColumnMapsToNullAttribute() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void corruptStoredJsonIsReportedAsIllegalState() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("{not json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Stored payload is not valid JSON");
    }
}
