package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryOperationResolverTest {

    private final QueryOperation sortBy = operationSupporting(request -> request.hasSortBy() && !request.hasGroupBy());
    private final QueryOperation groupBy = operationSupporting(QueryRequest::hasGroupBy);

    private final QueryOperationResolver resolver = new QueryOperationResolver(List.of(sortBy, groupBy));

    @Test
    void resolvesSortOnlyRequestToSortOperation() {
        assertThat(resolver.resolve(QueryRequest.of(null, "age", null))).isSameAs(sortBy);
    }

    @Test
    void resolvesGroupRequestsToGroupOperation() {
        assertThat(resolver.resolve(QueryRequest.of("department", null, null))).isSameAs(groupBy);
        assertThat(resolver.resolve(QueryRequest.of("department", "age", "asc"))).isSameAs(groupBy);
    }

    @Test
    void returnsFirstSupportingOperationInRegistrationOrder() {
        QueryOperation acceptsAll = operationSupporting(request -> true);
        QueryOperationResolver ordered = new QueryOperationResolver(List.of(acceptsAll, groupBy));

        assertThat(ordered.resolve(QueryRequest.of("department", null, null))).isSameAs(acceptsAll);
    }

    @Test
    void throwsWhenNoOperationSupportsRequest() {
        QueryOperationResolver empty = new QueryOperationResolver(List.of());

        assertThatThrownBy(() -> empty.resolve(QueryRequest.of(null, "age", null)))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessage("No query operation supports the given parameters");
    }

    @Test
    void rejectsNullOperationList() {
        assertThatThrownBy(() -> new QueryOperationResolver(null)).isInstanceOf(NullPointerException.class);
    }

    private static QueryOperation operationSupporting(Predicate<QueryRequest> predicate) {
        return new QueryOperation() {
            @Override
            public boolean supports(QueryRequest request) {
                return predicate.test(request);
            }

            @Override
            public QueryResult execute(List<JsonNode> records, QueryRequest request) {
                return new QueryResult.Sorted(records);
            }
        };
    }
}
