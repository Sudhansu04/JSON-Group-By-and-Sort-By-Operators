package com.assignment.datasetops.api;

import com.assignment.datasetops.domain.exception.DatasetNotFoundException;
import com.assignment.datasetops.domain.exception.DuplicateRecordException;
import com.assignment.datasetops.domain.exception.InvalidRecordException;
import com.assignment.datasetops.domain.exception.RecordNotFoundException;
import com.assignment.datasetops.query.QueryRequest;
import com.assignment.datasetops.query.QueryResult;
import com.assignment.datasetops.service.DatasetQueryService;
import com.assignment.datasetops.service.DatasetRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DatasetController.class)
class DatasetControllerTest {

    private static final String DATASET = "employees";
    private static final String RECORD_URL = "/api/dataset/{datasetName}/record";
    private static final String QUERY_URL = "/api/dataset/{datasetName}/query";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DatasetRecordService recordService;
    @MockitoBean
    private DatasetQueryService queryService;

    // ---- POST /record -------------------------------------------------------------------------

    @Test
    void insertReturns201WithLocationAndBody() throws Exception {
        when(recordService.insert(eq(DATASET), any())).thenReturn(1L);

        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"name\":\"John Doe\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/dataset/employees/record/1")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Record added successfully"))
                .andExpect(jsonPath("$.dataset").value(DATASET))
                .andExpect(jsonPath("$.recordId").value(1));
    }

    @Test
    void insertPassesParsedJsonToService() throws Exception {
        when(recordService.insert(eq(DATASET), any())).thenReturn(1L);

        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"address\":{\"city\":\"Pune\"}}"))
                .andExpect(status().isCreated());

        verify(recordService).insert(DATASET, objectMapper.readTree("{\"id\":1,\"address\":{\"city\":\"Pune\"}}"));
    }

    @Test
    void malformedJsonBodyIsBadRequest() throws Exception {
        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,"))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400))
                .andExpect(jsonPath("$.detail").value("Failed to read request"))
                .andExpect(jsonPath("$.instance").value("/api/dataset/employees/record"));

        verify(recordService, never()).insert(any(), any());
    }

    @Test
    void unsupportedMediaTypeIs415() throws Exception {
        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("id=1"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(problemWithStatus(415));
    }

    @Test
    void invalidDatasetNameIsBadRequest() throws Exception {
        mockMvc.perform(post(RECORD_URL, "bad name!")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400))
                .andExpect(jsonPath("$.title").value("Invalid request parameter"))
                .andExpect(jsonPath("$.detail").value("datasetName must match ^[A-Za-z0-9_-]{1,100}$"));

        verify(recordService, never()).insert(any(), any());
    }

    @Test
    void invalidRecordIsBadRequest() throws Exception {
        when(recordService.insert(eq(DATASET), any()))
                .thenThrow(new InvalidRecordException("Record must contain a non-negative integral 'id' field"));

        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"no id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400))
                .andExpect(jsonPath("$.title").value("Invalid record"))
                .andExpect(jsonPath("$.detail").value("Record must contain a non-negative integral 'id' field"));
    }

    @Test
    void duplicateRecordIsConflict() throws Exception {
        when(recordService.insert(eq(DATASET), any())).thenThrow(new DuplicateRecordException(DATASET, 1L));

        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isConflict())
                .andExpect(problemWithStatus(409))
                .andExpect(jsonPath("$.title").value("Duplicate record"))
                .andExpect(jsonPath("$.detail").value("Record 1 already exists in dataset 'employees'"));
    }

    @Test
    void unexpectedFailureIsHiddenBehind500() throws Exception {
        when(recordService.insert(eq(DATASET), any())).thenThrow(new IllegalStateException("db down: secret"));

        mockMvc.perform(post(RECORD_URL, DATASET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1}"))
                .andExpect(status().isInternalServerError())
                .andExpect(problemWithStatus(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    // ---- GET /record/{id} ---------------------------------------------------------------------

    @Test
    void getRecordReturnsStoredJson() throws Exception {
        when(recordService.get(DATASET, 1L)).thenReturn(objectMapper.readTree("{\"id\":1,\"name\":\"John\"}"));

        mockMvc.perform(get(RECORD_URL + "/{recordId}", DATASET, 1))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void getMissingRecordIs404() throws Exception {
        when(recordService.get(DATASET, 9L)).thenThrow(new RecordNotFoundException(DATASET, 9L));

        mockMvc.perform(get(RECORD_URL + "/{recordId}", DATASET, 9))
                .andExpect(status().isNotFound())
                .andExpect(problemWithStatus(404))
                .andExpect(jsonPath("$.title").value("Record not found"))
                .andExpect(jsonPath("$.detail").value("Record 9 not found in dataset 'employees'"));
    }

    @Test
    void nonNumericRecordIdIsBadRequest() throws Exception {
        mockMvc.perform(get(RECORD_URL + "/{recordId}", DATASET, "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400));
    }

    @Test
    void unknownRouteIs404Problem() throws Exception {
        mockMvc.perform(get("/api/nothing-here"))
                .andExpect(status().isNotFound())
                .andExpect(problemWithStatus(404));
    }

    // ---- GET /query ---------------------------------------------------------------------------

    @Test
    void groupByReturnsGroupedRecordsShape() throws Exception {
        JsonNode john = objectMapper.readTree("{\"id\":1,\"department\":\"Engineering\"}");
        JsonNode alice = objectMapper.readTree("{\"id\":3,\"department\":\"Marketing\"}");
        Map<String, List<JsonNode>> groups = new LinkedHashMap<>();
        groups.put("Engineering", List.of(john));
        groups.put("Marketing", List.of(alice));
        when(queryService.query(DATASET, QueryRequest.of("department", null, null)))
                .thenReturn(new QueryResult.Grouped(groups));

        mockMvc.perform(get(QUERY_URL, DATASET).queryParam("groupBy", "department"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groupedRecords.Engineering[0].id").value(1))
                .andExpect(jsonPath("$.groupedRecords.Marketing[0].id").value(3))
                .andExpect(jsonPath("$.sortedRecords").doesNotExist());
    }

    @Test
    void sortByReturnsSortedRecordsShape() throws Exception {
        JsonNode jane = objectMapper.readTree("{\"id\":2,\"age\":25}");
        JsonNode john = objectMapper.readTree("{\"id\":1,\"age\":30}");
        when(queryService.query(DATASET, QueryRequest.of(null, "age", "desc")))
                .thenReturn(new QueryResult.Sorted(List.of(john, jane)));

        mockMvc.perform(get(QUERY_URL, DATASET).queryParam("sortBy", "age").queryParam("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[0].id").value(1))
                .andExpect(jsonPath("$.sortedRecords[1].id").value(2))
                .andExpect(jsonPath("$.groupedRecords").doesNotExist());
    }

    @Test
    void queryWithoutOperatorIsBadRequest() throws Exception {
        mockMvc.perform(get(QUERY_URL, DATASET))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400))
                .andExpect(jsonPath("$.title").value("Invalid query"))
                .andExpect(jsonPath("$.detail")
                        .value("At least one of 'groupBy' or 'sortBy' query parameters must be provided"));

        verify(queryService, never()).query(any(), any());
    }

    @Test
    void unknownOrderIsBadRequest() throws Exception {
        mockMvc.perform(get(QUERY_URL, DATASET).queryParam("sortBy", "age").queryParam("order", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(problemWithStatus(400))
                .andExpect(jsonPath("$.title").value("Invalid query"))
                .andExpect(jsonPath("$.detail").value("Invalid order 'sideways'. Allowed values: asc, desc"));
    }

    @Test
    void orderWithoutSortByIsBadRequest() throws Exception {
        mockMvc.perform(get(QUERY_URL, DATASET).queryParam("groupBy", "department").queryParam("order", "asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid query"));
    }

    @Test
    void unknownDatasetIs404() throws Exception {
        when(queryService.query(eq("ghost"), any())).thenThrow(new DatasetNotFoundException("ghost"));

        mockMvc.perform(get(QUERY_URL, "ghost").queryParam("groupBy", "department"))
                .andExpect(status().isNotFound())
                .andExpect(problemWithStatus(404))
                .andExpect(jsonPath("$.title").value("Dataset not found"))
                .andExpect(jsonPath("$.detail").value("Dataset 'ghost' not found"))
                .andExpect(jsonPath("$.instance").value("/api/dataset/ghost/query"));
    }

    /** Asserts the RFC 7807 envelope every error shares, including the custom timestamp property. */
    private static ResultMatcher problemWithStatus(int status) {
        return result -> {
            content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON).match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.title").isString().match(result);
            jsonPath("$.timestamp").isString().match(result);
        };
    }
}
