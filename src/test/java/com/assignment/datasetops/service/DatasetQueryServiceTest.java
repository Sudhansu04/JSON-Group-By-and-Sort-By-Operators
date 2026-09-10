package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.DatasetNotFoundException;
import com.assignment.datasetops.persistence.DatasetRecordEntity;
import com.assignment.datasetops.persistence.DatasetRecordRepository;
import com.assignment.datasetops.query.QueryOperation;
import com.assignment.datasetops.query.QueryOperationResolver;
import com.assignment.datasetops.query.QueryRequest;
import com.assignment.datasetops.query.QueryResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetQueryServiceTest {

    private static final String DATASET = "employees";

    @Mock
    private DatasetRecordRepository repository;
    @Mock
    private QueryOperationResolver resolver;
    @Mock
    private QueryOperation operation;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DatasetQueryService service;

    @BeforeEach
    void setUp() {
        service = new DatasetQueryService(repository, resolver);
    }

    @Test
    void emptyDatasetIsReportedAsNotFound() {
        QueryRequest request = QueryRequest.of("department", null, null);
        when(repository.findByDatasetNameOrderByIdAsc(DATASET)).thenReturn(List.of());

        assertThatThrownBy(() -> service.query(DATASET, request))
                .isInstanceOf(DatasetNotFoundException.class)
                .hasMessage("Dataset 'employees' not found");

        verify(resolver, never()).resolve(any());
    }

    @Test
    void passesPayloadsInStoredOrderToResolvedOperationAndReturnsItsResult() throws Exception {
        JsonNode first = objectMapper.readTree("{\"id\":1,\"age\":30}");
        JsonNode second = objectMapper.readTree("{\"id\":2,\"age\":25}");
        QueryRequest request = QueryRequest.of(null, "age", "asc");
        QueryResult expected = new QueryResult.Sorted(List.of(second, first));
        when(repository.findByDatasetNameOrderByIdAsc(DATASET)).thenReturn(List.of(
                new DatasetRecordEntity(DATASET, 1L, first),
                new DatasetRecordEntity(DATASET, 2L, second)));
        when(resolver.resolve(request)).thenReturn(operation);
        when(operation.execute(List.of(first, second), request)).thenReturn(expected);

        QueryResult result = service.query(DATASET, request);

        assertThat(result).isSameAs(expected);
    }
}
