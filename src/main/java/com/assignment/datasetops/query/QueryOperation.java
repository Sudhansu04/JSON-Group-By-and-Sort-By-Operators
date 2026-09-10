package com.assignment.datasetops.query;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 * Strategy for executing one kind of query over an in-memory list of JSON records.
 *
 * <p>Implementations are Spring beans discovered by {@link QueryOperationResolver}. Exactly one
 * implementation must {@link #supports(QueryRequest) support} any valid {@link QueryRequest}.
 * Implementations must be stateless and must not mutate the input list.
 */
public interface QueryOperation {

    /** @return {@code true} if this strategy handles the given request */
    boolean supports(QueryRequest request);

    /**
     * Executes the operation.
     *
     * @param records records of the dataset, never {@code null}, never mutated
     * @param request a request for which {@link #supports(QueryRequest)} returned {@code true}
     * @return the result, never {@code null}
     */
    QueryResult execute(List<JsonNode> records, QueryRequest request);
}
