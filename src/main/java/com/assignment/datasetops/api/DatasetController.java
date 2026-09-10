package com.assignment.datasetops.api;

import com.assignment.datasetops.api.dto.InsertRecordResponse;
import com.assignment.datasetops.query.QueryRequest;
import com.assignment.datasetops.query.QueryResult;
import com.assignment.datasetops.service.DatasetQueryService;
import com.assignment.datasetops.service.DatasetRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Objects;

/** HTTP boundary for storing dataset records and running group-by / sort-by queries over them. */
@RestController
@RequestMapping("/api/dataset")
@Validated
@Tag(name = "Dataset", description = "Insert JSON records into named datasets and query them")
public class DatasetController {

    private static final String DATASET_NAME_REGEX = "^[A-Za-z0-9_-]{1,100}$";
    private static final String DATASET_NAME_MESSAGE = "datasetName must match " + DATASET_NAME_REGEX;
    private static final String DATASET_NAME_DESCRIPTION =
            "Dataset name: 1-100 letters, digits, '_' or '-'. Created implicitly on first insert.";

    private final DatasetRecordService recordService;
    private final DatasetQueryService queryService;

    public DatasetController(DatasetRecordService recordService, DatasetQueryService queryService) {
        this.recordService = Objects.requireNonNull(recordService, "recordService must not be null");
        this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    }

    @Operation(summary = "Insert a record",
            description = "Stores a JSON object in the dataset. The object must contain a non-negative integral "
                    + "'id' field that is unique within the dataset.")
    @ApiResponse(responseCode = "201", description = "Record stored; Location points at the new record",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = InsertRecordResponse.class),
                    examples = @ExampleObject(
                            value = "{\"message\":\"Record added successfully\",\"dataset\":\"employee_dataset\",\"recordId\":1}")))
    @ApiResponse(responseCode = "400", description = "Malformed JSON, invalid record or invalid dataset name",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A record with the same id already exists in the dataset",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{datasetName}/record",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InsertRecordResponse> insertRecord(
            @Parameter(description = DATASET_NAME_DESCRIPTION, example = "employee_dataset")
            @PathVariable @Pattern(regexp = DATASET_NAME_REGEX, message = DATASET_NAME_MESSAGE) String datasetName,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Arbitrary JSON object with an 'id'",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}")))
            @RequestBody JsonNode payload) {
        long recordId = recordService.insert(datasetName, payload);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{recordId}")
                .buildAndExpand(recordId)
                .toUri();
        return ResponseEntity.created(location).body(InsertRecordResponse.success(datasetName, recordId));
    }

    @Operation(summary = "Fetch a single record", description = "Returns the JSON object exactly as it was stored.")
    @ApiResponse(responseCode = "200", description = "The stored record")
    @ApiResponse(responseCode = "404", description = "No such record in the dataset",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = "/{datasetName}/record/{recordId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode getRecord(
            @Parameter(description = DATASET_NAME_DESCRIPTION, example = "employee_dataset")
            @PathVariable @Pattern(regexp = DATASET_NAME_REGEX, message = DATASET_NAME_MESSAGE) String datasetName,
            @Parameter(description = "Value of the record's 'id' field", example = "1")
            @PathVariable long recordId) {
        return recordService.get(datasetName, recordId);
    }

    @Operation(summary = "Group or sort the records of a dataset",
            description = "At least one of groupBy / sortBy is required. With groupBy the response is "
                    + "{\"groupedRecords\": {...}}; with sortBy alone it is {\"sortedRecords\": [...]}. "
                    + "When both are given, the records inside each group are sorted. Fields may be dot-paths.")
    @ApiResponse(responseCode = "200", description = "Grouped or sorted records",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, examples = {
                    @ExampleObject(name = "groupBy=department", value = "{\"groupedRecords\":{"
                            + "\"Engineering\":[{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"},"
                            + "{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"}],"
                            + "\"Marketing\":[{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"}]}}"),
                    @ExampleObject(name = "sortBy=age&order=asc", value = "{\"sortedRecords\":["
                            + "{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"},"
                            + "{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"},"
                            + "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}]}"),
                    @ExampleObject(name = "sortBy=age&order=desc", value = "{\"sortedRecords\":["
                            + "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"},"
                            + "{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"},"
                            + "{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"}]}")}))
    @ApiResponse(responseCode = "400", description = "Invalid combination of query parameters or invalid dataset name",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The dataset has no records",
            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = "/{datasetName}/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public QueryResult query(
            @Parameter(description = DATASET_NAME_DESCRIPTION, example = "employee_dataset")
            @PathVariable @Pattern(regexp = DATASET_NAME_REGEX, message = DATASET_NAME_MESSAGE) String datasetName,
            @Parameter(description = "Field to group by (dot-path allowed)", example = "department")
            @RequestParam(required = false) String groupBy,
            @Parameter(description = "Field to sort by (dot-path allowed)", example = "age")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "Sort direction, 'asc' (default) or 'desc'; requires sortBy", example = "asc")
            @RequestParam(required = false) String order) {
        return queryService.query(datasetName, QueryRequest.of(groupBy, sortBy, order));
    }
}
