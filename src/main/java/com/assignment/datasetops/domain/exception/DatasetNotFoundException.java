package com.assignment.datasetops.domain.exception;

/** Raised when a dataset has no records, i.e. it does not exist. Maps to HTTP 404. */
public class DatasetNotFoundException extends DatasetOpsException {

    public DatasetNotFoundException(String datasetName) {
        super("Dataset '" + datasetName + "' not found");
    }
}
