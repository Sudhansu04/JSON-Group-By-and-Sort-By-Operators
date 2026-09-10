package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.DatasetNotFoundException;
import com.assignment.datasetops.persistence.DatasetRecordEntity;
import com.assignment.datasetops.persistence.DatasetRecordRepository;
import com.assignment.datasetops.query.QueryOperationResolver;
import com.assignment.datasetops.query.QueryRequest;
import com.assignment.datasetops.query.QueryResult;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Executes group-by / sort-by queries over all records of a dataset.
 *
 * <p>Records are loaded as {@link JsonNode}s and processed in memory by the resolved
 * {@link com.assignment.datasetops.query.QueryOperation}, so behaviour is identical on every database.
 */
@Service
public class DatasetQueryService {

    private final DatasetRecordRepository repository;
    private final QueryOperationResolver resolver;

    public DatasetQueryService(DatasetRecordRepository repository, QueryOperationResolver resolver) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    /**
     * @throws DatasetNotFoundException if the dataset holds no records
     * @throws com.assignment.datasetops.domain.exception.InvalidQueryException if no operation supports the request
     */
    @Transactional(readOnly = true)
    public QueryResult query(String datasetName, QueryRequest request) {
        List<JsonNode> records = repository.findByDatasetNameOrderByIdAsc(datasetName).stream()
                .map(DatasetRecordEntity::getPayload)
                .toList();
        if (records.isEmpty()) {
            throw new DatasetNotFoundException(datasetName);
        }
        return resolver.resolve(request).execute(records, request);
    }
}
