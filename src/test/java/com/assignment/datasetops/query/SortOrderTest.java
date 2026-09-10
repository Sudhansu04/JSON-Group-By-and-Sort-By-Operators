package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortOrderTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void blankParameterFallsBackToDefault(String raw) {
        assertThat(SortOrder.fromParameter(raw)).isEqualTo(SortOrder.DEFAULT).isEqualTo(SortOrder.ASC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "ASC", "aSc", " asc "})
    void parsesAscendingCaseInsensitively(String raw) {
        assertThat(SortOrder.fromParameter(raw)).isEqualTo(SortOrder.ASC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"desc", "DESC", "dEsC", " desc "})
    void parsesDescendingCaseInsensitively(String raw) {
        assertThat(SortOrder.fromParameter(raw)).isEqualTo(SortOrder.DESC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ascending", "down", "1", "asc,desc"})
    void rejectsUnknownValues(String raw) {
        assertThatThrownBy(() -> SortOrder.fromParameter(raw))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining(raw)
                .hasMessageContaining("asc, desc");
    }

    @Test
    void isDescendingOnlyForDesc() {
        assertThat(SortOrder.DESC.isDescending()).isTrue();
        assertThat(SortOrder.ASC.isDescending()).isFalse();
    }

    @Test
    void toParameterIsLowerCase() {
        assertThat(SortOrder.ASC.toParameter()).isEqualTo("asc");
        assertThat(SortOrder.DESC.toParameter()).isEqualTo("desc");
    }
}
