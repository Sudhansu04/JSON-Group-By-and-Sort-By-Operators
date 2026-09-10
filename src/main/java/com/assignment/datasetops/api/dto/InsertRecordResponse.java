package com.assignment.datasetops.api.dto;

/**
 * Response body of {@code POST /api/dataset/{datasetName}/record}.
 *
 * @param message  human readable confirmation
 * @param dataset  name of the dataset the record was stored in
 * @param recordId the record's id (taken from the {@code id} field of the submitted JSON)
 */
public record InsertRecordResponse(String message, String dataset, long recordId) {

    public static final String SUCCESS_MESSAGE = "Record added successfully";

    public static InsertRecordResponse success(String dataset, long recordId) {
        return new InsertRecordResponse(SUCCESS_MESSAGE, dataset, recordId);
    }
}
