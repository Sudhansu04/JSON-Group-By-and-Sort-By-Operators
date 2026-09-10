package com.assignment.datasetops.persistence;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

/**
 * One JSON record belonging to a named dataset.
 *
 * <p>Datasets are implicit: a dataset exists as soon as it has at least one record. The pair
 * {@code (datasetName, recordId)} is unique, where {@code recordId} is the {@code id} field of the
 * submitted JSON and {@link #getId()} is the database surrogate key.
 */
@Entity
@Table(name = "dataset_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_dataset_record", columnNames = {"dataset_name", "record_id"}),
        indexes = @Index(name = "idx_dataset_name", columnList = "dataset_name"))
public class DatasetRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_name", nullable = false, length = 100)
    private String datasetName;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Convert(converter = JsonNodeAttributeConverter.class)
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "payload", nullable = false)
    private JsonNode payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected DatasetRecordEntity() {
    }

    public DatasetRecordEntity(String datasetName, Long recordId, JsonNode payload) {
        this.datasetName = Objects.requireNonNull(datasetName, "datasetName must not be null");
        this.recordId = Objects.requireNonNull(recordId, "recordId must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
    }

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public Long getRecordId() {
        return recordId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
