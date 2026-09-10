package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryRequestTest {

    @Test
    void buildsSortOnlyRequestWithDefaultOrder() {
        QueryRequest request = QueryRequest.of(null, "age", null);

        assertThat(request.hasSortBy()).isTrue();
        assertThat(request.hasGroupBy()).isFalse();
        assertThat(request.sortByField()).contains("age");
        assertThat(request.groupByField()).isEmpty();
        assertThat(request.order()).isEqualTo(SortOrder.ASC);
    }

    @Test
    void buildsGroupOnlyRequest() {
        QueryRequest request = QueryRequest.of("department", null, null);

        assertThat(request.hasGroupBy()).isTrue();
        assertThat(request.hasSortBy()).isFalse();
        assertThat(request.groupByField()).contains("department");
        assertThat(request.sortByField()).isEmpty();
    }

    @Test
    void buildsCombinedRequest() {
        QueryRequest request = QueryRequest.of("department", "age", "desc");

        assertThat(request.hasGroupBy()).isTrue();
        assertThat(request.hasSortBy()).isTrue();
        assertThat(request.order()).isEqualTo(SortOrder.DESC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"asc", "ASC", "Asc", "desc", "DESC", "DeSc", " desc "})
    void parsesOrderCaseInsensitively(String order) {
        QueryRequest request = QueryRequest.of(null, "age", order);

        assertThat(request.order().name()).isEqualToIgnoringCase(order.trim());
    }

    @Test
    void rejectsRequestWithNeitherParameter() {
        assertThatThrownBy(() -> QueryRequest.of(null, null, null))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("groupBy")
                .hasMessageContaining("sortBy");
    }

    @Test
    void treatsBlankParametersAsAbsent() {
        assertThatThrownBy(() -> QueryRequest.of("  ", "", null)).isInstanceOf(InvalidQueryException.class);

        QueryRequest request = QueryRequest.of(" department ", "   ", null);
        assertThat(request.groupBy()).isEqualTo("department");
        assertThat(request.hasSortBy()).isFalse();
    }

    @Test
    void rejectsOrderWithoutSortBy() {
        assertThatThrownBy(() -> QueryRequest.of("department", null, "asc"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("'order'");
    }

    @Test
    void allowsBlankOrderWithoutSortBy() {
        assertThat(QueryRequest.of("department", null, "  ").order()).isEqualTo(SortOrder.DEFAULT);
    }

    @Test
    void rejectsUnknownOrderValue() {
        assertThatThrownBy(() -> QueryRequest.of(null, "age", "sideways"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("sideways");
    }

    @Test
    void canonicalConstructorDefaultsNullOrder() {
        assertThat(new QueryRequest(null, "age", null).order()).isEqualTo(SortOrder.DEFAULT);
    }
}
