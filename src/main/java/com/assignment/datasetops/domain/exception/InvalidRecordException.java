package com.assignment.datasetops.domain.exception;

/** Raised when the submitted JSON record is structurally unacceptable. Maps to HTTP 400. */
public class InvalidRecordException extends DatasetOpsException {

    public InvalidRecordException(String message) {
        super(message);
    }
}
