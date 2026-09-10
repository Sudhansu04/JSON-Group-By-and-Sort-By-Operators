package com.assignment.datasetops;

import com.assignment.datasetops.persistence.DatasetRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end replay of the assignment examples against the real stack (controller, services, query
 * operators, JPA on in-memory H2).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DatasetApiIntegrationTest {

    private static final String DATASET = "employee_dataset";
    private static final String JOHN = "{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"department\":\"Engineering\"}";
    private static final String JANE = "{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"department\":\"Engineering\"}";
    private static final String ALICE = "{\"id\":3,\"name\":\"Alice Brown\",\"age\":28,\"department\":\"Marketing\"}";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private DatasetRecordRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void insertReturnsCreatedWithLocationThatResolves() throws Exception {
        mockMvc.perform(insert(DATASET, JOHN))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/dataset/employee_dataset/record/1")))
                .andExpect(jsonPath("$.message").value("Record added successfully"))
                .andExpect(jsonPath("$.dataset").value(DATASET))
                .andExpect(jsonPath("$.recordId").value(1));

        mockMvc.perform(get("/api/dataset/{d}/record/{id}", DATASET, 1))
                .andExpect(status().isOk())
                .andExpect(content().json(JOHN));
    }

    @Test
    void groupByDepartmentMatchesAssignmentExample() throws Exception {
        insertEmployees();

        MvcResult result = mockMvc.perform(get("/api/dataset/{d}/query", DATASET).queryParam("groupBy", "department"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.groupedRecords.Engineering[*].id").value(contains(1, 2)))
                .andExpect(jsonPath("$.groupedRecords.Marketing[*].id").value(contains(3)))
                .andReturn();

        JsonNode grouped = objectMapper.readTree(result.getResponse().getContentAsString()).get("groupedRecords");
        List<String> groupOrder = new ArrayList<>();
        grouped.fieldNames().forEachRemaining(groupOrder::add);
        assertThat(groupOrder).containsExactly("Engineering", "Marketing");
        assertThat(grouped.get("Engineering").get(0)).isEqualTo(objectMapper.readTree(JOHN));
    }

    @Test
    void sortByAgeAscendingMatchesAssignmentExample() throws Exception {
        insertEmployees();

        mockMvc.perform(get("/api/dataset/{d}/query", DATASET).queryParam("sortBy", "age").queryParam("order", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords", hasSize(3)))
                .andExpect(jsonPath("$.sortedRecords[*].id").value(contains(2, 3, 1)))
                .andExpect(jsonPath("$.sortedRecords[*].age").value(contains(25, 28, 30)));
    }

    @Test
    void sortByAgeDescendingMatchesAssignmentExample() throws Exception {
        insertEmployees();

        mockMvc.perform(get("/api/dataset/{d}/query", DATASET).queryParam("sortBy", "age").queryParam("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[*].id").value(contains(1, 3, 2)));
    }

    @Test
    void sortDefaultsToAscendingAndIsCaseInsensitive() throws Exception {
        insertEmployees();

        mockMvc.perform(get("/api/dataset/{d}/query", DATASET).queryParam("sortBy", "age"))
                .andExpect(jsonPath("$.sortedRecords[*].id").value(contains(2, 3, 1)));
        mockMvc.perform(get("/api/dataset/{d}/query", DATASET).queryParam("sortBy", "age").queryParam("order", "DESC"))
                .andExpect(jsonPath("$.sortedRecords[*].id").value(contains(1, 3, 2)));
    }

    @Test
    void groupByAndSortByTogetherSortInsideEachGroup() throws Exception {
        insertEmployees();

        mockMvc.perform(get("/api/dataset/{d}/query", DATASET)
                        .queryParam("groupBy", "department").queryParam("sortBy", "age"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords.Engineering[*].id").value(contains(2, 1)))
                .andExpect(jsonPath("$.groupedRecords.Marketing[*].id").value(contains(3)));
    }

    @Test
    void duplicateIdInSameDatasetIsConflictButAllowedAcrossDatasets() throws Exception {
        mockMvc.perform(insert(DATASET, JOHN)).andExpect(status().isCreated());

        mockMvc.perform(insert(DATASET, JOHN))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Duplicate record"))
                .andExpect(jsonPath("$.timestamp").isString());

        mockMvc.perform(insert("another_dataset", JOHN)).andExpect(status().isCreated());
    }

    @Test
    void nestedFieldsAreAddressedByDotPathAndMissingValuesAreBucketedAsNull() throws Exception {
        String dataset = "customers";
        mockMvc.perform(insert(dataset, "{\"id\":1,\"address\":{\"city\":\"Pune\"}}")).andExpect(status().isCreated());
        mockMvc.perform(insert(dataset, "{\"id\":2,\"address\":{\"city\":\"Delhi\"}}")).andExpect(status().isCreated());
        mockMvc.perform(insert(dataset, "{\"id\":3,\"address\":{\"city\":\"Pune\"}}")).andExpect(status().isCreated());
        mockMvc.perform(insert(dataset, "{\"id\":4,\"name\":\"no address\"}")).andExpect(status().isCreated());

        mockMvc.perform(get("/api/dataset/{d}/query", dataset).queryParam("groupBy", "address.city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groupedRecords.Pune[*].id").value(contains(1, 3)))
                .andExpect(jsonPath("$.groupedRecords.Delhi[*].id").value(contains(2)))
                .andExpect(jsonPath("$.groupedRecords.null[*].id").value(contains(4)));

        mockMvc.perform(get("/api/dataset/{d}/query", dataset).queryParam("sortBy", "address.city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sortedRecords[*].id").value(contains(2, 1, 3, 4)));
    }

    @Test
    void invalidRequestsProduceProblemDetails() throws Exception {
        mockMvc.perform(get("/api/dataset/{d}/query", "unknown_dataset").queryParam("groupBy", "department"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Dataset not found"))
                .andExpect(jsonPath("$.detail").value("Dataset 'unknown_dataset' not found"));

        mockMvc.perform(insert(DATASET, "{\"name\":\"no id\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid record"));

        mockMvc.perform(insert(DATASET, "[1,2,3]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid record"))
                .andExpect(jsonPath("$.detail").value("Record must be a JSON object"));

        mockMvc.perform(insert(DATASET, "{\"id\":1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isString());

        mockMvc.perform(insert("bad name", JOHN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request parameter"));
    }

    private void insertEmployees() throws Exception {
        mockMvc.perform(insert(DATASET, JOHN)).andExpect(status().isCreated());
        mockMvc.perform(insert(DATASET, JANE)).andExpect(status().isCreated());
        mockMvc.perform(insert(DATASET, ALICE)).andExpect(status().isCreated());
    }

    private static MockHttpServletRequestBuilder insert(String dataset, String body) {
        return post("/api/dataset/{d}/record", dataset).contentType(MediaType.APPLICATION_JSON).content(body);
    }
}
