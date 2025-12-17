package com.example.wagemanager.domain.workrecord.deletion.repository;

import com.example.wagemanager.domain.workrecord.deletion.entity.WorkRecordDeletionRequest;
import com.example.wagemanager.domain.workrecord.deletion.enums.WorkRecordDeletionRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkRecordDeletionRequestRepository extends JpaRepository<WorkRecordDeletionRequest, Long> {

    boolean existsByWorkRecordIdAndStatus(Long workRecordId, WorkRecordDeletionRequestStatus status);

    Optional<WorkRecordDeletionRequest> findByWorkRecordIdAndStatus(Long workRecordId, WorkRecordDeletionRequestStatus status);
}
