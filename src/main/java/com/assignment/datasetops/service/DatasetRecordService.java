package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.DuplicateRecordException;
import com.assignment.datasetops.domain.exception.RecordNotFoundException;
import com.assignment.datasetops.persistence.DatasetRecordEntity;
import com.assignment.datasetops.persistence.DatasetRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/** Use-cases for storing and reading individual dataset records. */
@Service
public class DatasetRecordService {

    private final DatasetRecordRepository repository;
    private final RecordPayloadValidator validator;

    public DatasetRecordService(DatasetRecordRepository repository, RecordPayloadValidator validator) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
    }

    /**
     * Validates and persists a record.
     *
     * @return the record id taken from the payload's {@code id} field
     * @throws com.assignment.datasetops.domain.exception.InvalidRecordException if the payload is malformed
     * @throws DuplicateRecordException if the dataset already holds a record with that id
     */
    @Transactional
    public long insert(String datasetName, JsonNode payload) {
        long recordId = validator.requireRecordId(payload);
        if (repository.existsByDatasetNameAndRecordId(datasetName, recordId)) {
            throw new DuplicateRecordException(datasetName, recordId);
        }
        try {
            repository.save(new DatasetRecordEntity(datasetName, recordId, payload));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent inserts passed the existence check; the unique constraint is the final arbiter.
            throw new DuplicateRecordException(datasetName, recordId);
        }
        return recordId;
    }

    /**
     * @return the stored JSON of the record
     * @throws RecordNotFoundException if no such record exists in the dataset
     */
    @Transactional(readOnly = true)
    public JsonNode get(String datasetName, long recordId) {
        return repository.findByDatasetNameAndRecordId(datasetName, recordId)
                .map(DatasetRecordEntity::getPayload)
                .orElseThrow(() -> new RecordNotFoundException(datasetName, recordId));
    }
}
