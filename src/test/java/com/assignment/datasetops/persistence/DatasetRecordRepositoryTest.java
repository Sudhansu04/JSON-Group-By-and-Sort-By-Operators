package com.assignment.datasetops.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class DatasetRecordRepositoryTest {

    private static final String DATASET = "employees";

    @Autowired
    private DatasetRecordRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void persistsPayloadAsJsonAndSetsCreatedAt() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"id\":7,\"address\":{\"city\":\"Pune\"}}");

        DatasetRecordEntity saved = repository.saveAndFlush(new DatasetRecordEntity(DATASET, 7L, payload));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(repository.findByDatasetNameAndRecordId(DATASET, 7L))
                .map(DatasetRecordEntity::getPayload)
                .contains(payload);
    }

    @Test
    void findsRecordsOfOneDatasetInInsertionOrder() throws Exception {
        repository.save(new DatasetRecordEntity(DATASET, 3L, objectMapper.readTree("{\"id\":3}")));
        repository.save(new DatasetRecordEntity(DATASET, 1L, objectMapper.readTree("{\"id\":1}")));
        repository.save(new DatasetRecordEntity("other", 2L, objectMapper.readTree("{\"id\":2}")));

        List<DatasetRecordEntity> records = repository.findByDatasetNameOrderByIdAsc(DATASET);

        assertThat(records).extracting(DatasetRecordEntity::getRecordId).containsExactly(3L, 1L);
        assertThat(repository.existsByDatasetName(DATASET)).isTrue();
        assertThat(repository.existsByDatasetName("missing")).isFalse();
        assertThat(repository.existsByDatasetNameAndRecordId(DATASET, 1L)).isTrue();
        assertThat(repository.existsByDatasetNameAndRecordId("other", 1L)).isFalse();
    }

    @Test
    void enforcesUniqueRecordIdPerDataset() throws Exception {
        repository.saveAndFlush(new DatasetRecordEntity(DATASET, 1L, objectMapper.readTree("{\"id\":1}")));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new DatasetRecordEntity(DATASET, 1L, objectMapper.readTree("{\"id\":1,\"dup\":true}"))))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
