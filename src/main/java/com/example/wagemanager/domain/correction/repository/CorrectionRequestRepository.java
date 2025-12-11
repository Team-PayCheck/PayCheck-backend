package com.example.wagemanager.domain.correction.repository;

import com.example.wagemanager.domain.correction.entity.CorrectionRequest;
import com.example.wagemanager.domain.correction.enums.CorrectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, Long> {

    // 근로자 본인의 정정요청 목록 조회
    @Query("SELECT cr FROM CorrectionRequest cr " +
            "JOIN FETCH cr.workRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "WHERE cr.requester.id = :requesterId " +
            "ORDER BY cr.createdAt DESC")
    List<CorrectionRequest> findByRequesterId(@Param("requesterId") Long requesterId);

    // 근로자 본인의 정정요청 상태별 조회
    @Query("SELECT cr FROM CorrectionRequest cr " +
            "JOIN FETCH cr.workRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "WHERE cr.requester.id = :requesterId " +
            "AND cr.status = :status " +
            "ORDER BY cr.createdAt DESC")
    List<CorrectionRequest> findByRequesterIdAndStatus(
            @Param("requesterId") Long requesterId,
            @Param("status") CorrectionStatus status
    );

    // 사업장별 정정요청 목록 조회 (고용주용)
    @Query("SELECT cr FROM CorrectionRequest cr " +
            "JOIN FETCH cr.workRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "JOIN FETCH cr.requester r " +
            "WHERE w.id = :workplaceId " +
            "ORDER BY cr.createdAt DESC")
    List<CorrectionRequest> findByWorkplaceId(@Param("workplaceId") Long workplaceId);

    // 사업장별 + 상태별 정정요청 목록 조회 (고용주용)
    @Query("SELECT cr FROM CorrectionRequest cr " +
            "JOIN FETCH cr.workRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "JOIN FETCH cr.requester r " +
            "WHERE w.id = :workplaceId " +
            "AND cr.status = :status " +
            "ORDER BY cr.createdAt DESC")
    List<CorrectionRequest> findByWorkplaceIdAndStatus(
            @Param("workplaceId") Long workplaceId,
            @Param("status") CorrectionStatus status
    );

    // 정정요청 상세 조회 (fetch join)
    @Query("SELECT cr FROM CorrectionRequest cr " +
            "JOIN FETCH cr.workRecord wr " +
            "JOIN FETCH wr.contract c " +
            "JOIN FETCH c.workplace w " +
            "JOIN FETCH cr.requester r " +
            "WHERE cr.id = :id")
    Optional<CorrectionRequest> findByIdWithDetails(@Param("id") Long id);

    // 특정 근무기록에 대한 대기중인 정정요청 존재 여부 확인
    @Query("SELECT COUNT(cr) > 0 FROM CorrectionRequest cr " +
            "WHERE cr.workRecord.id = :workRecordId " +
            "AND cr.status = :status")
    boolean existsByWorkRecordIdAndStatus(
            @Param("workRecordId") Long workRecordId,
            @Param("status") CorrectionStatus status
    );
}
