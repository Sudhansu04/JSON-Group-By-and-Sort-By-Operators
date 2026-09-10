package com.assignment.datasetops.domain.exception;

/**
 * Base class for all business exceptions raised by the application.
 * Each subclass maps to a single HTTP status in the API error handler.
 */
public abstract class DatasetOpsException extends RuntimeException {

    protected DatasetOpsException(String message) {
        super(message);
    }
}
