package com.assignment.datasetops.query;

import com.assignment.datasetops.domain.exception.InvalidQueryException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Selects the {@link QueryOperation} strategy that handles a given {@link QueryRequest}.
 *
 * <p>All {@code QueryOperation} beans are injected as a list; the first one whose
 * {@link QueryOperation#supports(QueryRequest)} returns {@code true} wins. Adding a new operator
 * therefore only requires a new bean, not a change to this class.
 */
@Component
public class QueryOperationResolver {

    private final List<QueryOperation> operations;

    public QueryOperationResolver(List<QueryOperation> operations) {
        this.operations = List.copyOf(Objects.requireNonNull(operations, "operations"));
    }

    /**
     * @param request the validated request to serve
     * @return the first operation supporting {@code request}
     * @throws InvalidQueryException when no registered operation supports the request
     */
    public QueryOperation resolve(QueryRequest request) {
        return operations.stream()
                .filter(operation -> operation.supports(request))
                .findFirst()
                .orElseThrow(() -> new InvalidQueryException(
                        "No query operation supports the given parameters"));
    }
}
