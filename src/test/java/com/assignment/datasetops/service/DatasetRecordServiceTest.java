package com.assignment.datasetops.service;

import com.assignment.datasetops.domain.exception.DuplicateRecordException;
import com.assignment.datasetops.domain.exception.InvalidRecordException;
import com.assignment.datasetops.domain.exception.RecordNotFoundException;
import com.assignment.datasetops.persistence.DatasetRecordEntity;
import com.assignment.datasetops.persistence.DatasetRecordRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasetRecordServiceTest {

    private static final String DATASET = "employees";

    @Mock
    private DatasetRecordRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DatasetRecordService service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new DatasetRecordService(repository, new RecordPayloadValidator());
    }

    @Test
    void insertPersistsEntityAndReturnsRecordId() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"id\":5,\"name\":\"John\"}");
        when(repository.existsByDatasetNameAndRecordId(DATASET, 5L)).thenReturn(false);

        long recordId = service.insert(DATASET, payload);

        ArgumentCaptor<DatasetRecordEntity> captor = ArgumentCaptor.forClass(DatasetRecordEntity.class);
        verify(repository).save(captor.capture());
        DatasetRecordEntity saved = captor.getValue();
        assertThat(recordId).isEqualTo(5L);
        assertThat(saved.getDatasetName()).isEqualTo(DATASET);
        assertThat(saved.getRecordId()).isEqualTo(5L);
        assertThat(saved.getPayload()).isSameAs(payload);
    }

    @Test
    void insertRejectsInvalidPayloadBeforeTouchingRepository() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"name\":\"no id\"}");

        assertThatThrownBy(() -> service.insert(DATASET, payload)).isInstanceOf(InvalidRecordException.class);

        verify(repository, never()).existsByDatasetNameAndRecordId(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void insertRejectsDuplicateWithoutSaving() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"id\":5}");
        when(repository.existsByDatasetNameAndRecordId(DATASET, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.insert(DATASET, payload))
                .isInstanceOf(DuplicateRecordException.class)
                .hasMessage("Record 5 already exists in dataset 'employees'");

        verify(repository, never()).save(any());
    }

    @Test
    void insertTranslatesUniqueConstraintViolationIntoDuplicate() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"id\":5}");
        when(repository.existsByDatasetNameAndRecordId(DATASET, 5L)).thenReturn(false);
        when(repository.save(any())).thenThrow(new DataIntegrityViolationException("uk_dataset_record"));

        assertThatThrownBy(() -> service.insert(DATASET, payload)).isInstanceOf(DuplicateRecordException.class);
    }

    @Test
    void getReturnsStoredPayload() throws Exception {
        JsonNode payload = objectMapper.readTree("{\"id\":5}");
        when(repository.findByDatasetNameAndRecordId(DATASET, 5L))
                .thenReturn(Optional.of(new DatasetRecordEntity(DATASET, 5L, payload)));

        assertThat(service.get(DATASET, 5L)).isSameAs(payload);
    }

    @Test
    void getThrowsWhenRecordIsMissing() {
        when(repository.findByDatasetNameAndRecordId(DATASET, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(DATASET, 5L))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Record 5 not found in dataset 'employees'");
    }
}
