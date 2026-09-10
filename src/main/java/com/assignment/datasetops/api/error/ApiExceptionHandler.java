package com.assignment.datasetops.api.error;

import com.assignment.datasetops.domain.exception.DatasetNotFoundException;
import com.assignment.datasetops.domain.exception.DuplicateRecordException;
import com.assignment.datasetops.domain.exception.InvalidQueryException;
import com.assignment.datasetops.domain.exception.InvalidRecordException;
import com.assignment.datasetops.domain.exception.RecordNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Translates every exception escaping a controller into an RFC 7807 {@link ProblemDetail}.
 *
 * <p>Business exceptions are mapped explicitly below. Framework exceptions (malformed JSON, type
 * mismatches, unknown routes, unsupported media types, ...) are handled by the
 * {@link ResponseEntityExceptionHandler} superclass; {@link #createResponseEntity} is overridden so
 * those bodies carry the same extra properties as ours. Anything else becomes a 500 whose message is
 * logged but never exposed.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DatasetNotFoundException.class)
    ProblemDetail handleDatasetNotFound(DatasetNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Dataset not found", ex.getMessage());
    }

    @ExceptionHandler(RecordNotFoundException.class)
    ProblemDetail handleRecordNotFound(RecordNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Record not found", ex.getMessage());
    }

    @ExceptionHandler(DuplicateRecordException.class)
    ProblemDetail handleDuplicateRecord(DuplicateRecordException ex) {
        return problem(HttpStatus.CONFLICT, "Duplicate record", ex.getMessage());
    }

    @ExceptionHandler(InvalidRecordException.class)
    ProblemDetail handleInvalidRecord(InvalidRecordException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid record", ex.getMessage());
    }

    @ExceptionHandler(InvalidQueryException.class)
    ProblemDetail handleInvalidQuery(InvalidQueryException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid query", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .sorted()
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Invalid request parameter", detail);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception while processing request", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", "An unexpected error occurred");
    }

    /** Adds the common properties to problem bodies produced by the framework-level handlers. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problemDetail) {
            stamp(problemDetail);
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        return stamp(problemDetail);
    }

    private static ProblemDetail stamp(ProblemDetail problemDetail) {
        problemDetail.setProperty(ErrorProblemProperties.TIMESTAMP, Instant.now());
        return problemDetail;
    }
}
