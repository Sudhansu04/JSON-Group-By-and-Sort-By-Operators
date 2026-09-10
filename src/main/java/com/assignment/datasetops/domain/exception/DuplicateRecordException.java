package com.assignment.datasetops.domain.exception;

/** Raised when a record with the same id already exists in the dataset. Maps to HTTP 409. */
public class DuplicateRecordException extends DatasetOpsException {

    public DuplicateRecordException(String datasetName, long recordId) {
        super("Record " + recordId + " already exists in dataset '" + datasetName + "'");
    }
}
