package com.assignment.datasetops.api.error;

/** Names of the non-standard members added to every RFC 7807 problem body produced by the API. */
final class ErrorProblemProperties {

    /** ISO-8601 instant at which the error response was created. */
    static final String TIMESTAMP = "timestamp";

    private ErrorProblemProperties() {
    }
}
