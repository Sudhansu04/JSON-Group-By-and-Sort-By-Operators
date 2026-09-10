package com.assignment.datasetops.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Spring Data repository for {@link DatasetRecordEntity}. */
public interface DatasetRecordRepository extends JpaRepository<DatasetRecordEntity, Long> {

    /** All records of a dataset in insertion order (surrogate key ascending). */
    List<DatasetRecordEntity> findByDatasetNameOrderByIdAsc(String datasetName);

    boolean existsByDatasetName(String datasetName);

    boolean existsByDatasetNameAndRecordId(String datasetName, Long recordId);

    Optional<DatasetRecordEntity> findByDatasetNameAndRecordId(String datasetName, Long recordId);
}
