package com.assignment.datasetops.domain.exception;

/** Raised when the query parameters do not describe a valid operation. Maps to HTTP 400. */
public class InvalidQueryException extends DatasetOpsException {

    public InvalidQueryException(String message) {
        super(message);
    }
}
