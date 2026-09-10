package com.assignment.datasetops.domain.exception;

/** Raised when a record id does not exist inside a dataset. Maps to HTTP 404. */
public class RecordNotFoundException extends DatasetOpsException {

    public RecordNotFoundException(String datasetName, long recordId) {
        super("Record " + recordId + " not found in dataset '" + datasetName + "'");
    }
}
